package com.daily.health.manager.camera

import android.content.Context
import android.graphics.ImageFormat
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge

/**
 * CameraX 管理器
 *
 * 封装 CameraX 用于 PPG 心率测量，提供以下功能：
 * 1. 摄像头初始化和帧捕获
 * 2. 闪光灯 (Torch) 控制
 * 3. 帧数据回调用于信号分析
 *
 * 使用方式：
 * ```kotlin
 * val cameraManager = CameraXManager(context)
 * cameraManager.startCapture(lifecycleOwner) { redValue, timestamp ->
 *     analyzer.addSample(redValue, timestamp)
 * }
 * cameraManager.setTorchEnabled(true)
 * // 测量完成后
 * cameraManager.stopCapture()
 * ```
 */
class CameraXManager(private val context: Context) {

    companion object {
        private const val TAG = "PPG_Camera"
    }

    /**
     * 摄像头状态
     */
    enum class CameraState {
        IDLE,           // 未初始化
        INITIALIZING,   // 初始化中
        RUNNING,        // 运行中
        STOPPING,       // 停止中
        ERROR           // 错误
    }

    /**
     * 帧数据回调
     * @param redValue 红色通道平均亮度 (0-255)
     * @param timestamp 帧时间戳 (纳秒)
     */
    fun interface FrameCallback {
        fun onFrame(redValue: Double, timestamp: Long)
    }

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val _cameraState = MutableStateFlow(CameraState.IDLE)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private var frameCallback: FrameCallback? = null

    /**
     * 开始摄像头捕获
     *
     * @param lifecycleOwner 生命周期拥有者 (通常是 Activity/Fragment)
     * @param surfaceProvider 预览 Surface 提供者 (可选)
     * @param callback 帧数据回调
     */
    fun startCapture(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: androidx.camera.core.Preview.SurfaceProvider? = null,
        callback: FrameCallback
    ) {
        if (_cameraState.value == CameraState.RUNNING) {
            if (BuildState.debug) "摄像头已在运行中，跳过初始化".logd(TAG)
            return
        }

        if (BuildState.debug) "正在初始化摄像头".logd(TAG)
        _cameraState.value = CameraState.INITIALIZING
        frameCallback = callback

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                if (BuildState.debug) "摄像头 Provider 获取成功".logd(TAG)
                bindCamera(lifecycleOwner, surfaceProvider)
            } catch (e: Exception) {
                if (BuildState.debug) "摄像头初始化失败: ${e.message}".loge(TAG)
                _cameraState.value = CameraState.ERROR
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 绑定摄像头用例
     */
    private fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: androidx.camera.core.Preview.SurfaceProvider?
    ) {
        val provider = cameraProvider ?: return

        // 使用后置摄像头
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        // 配置图像分析器
        // 使用较低分辨率以获得更高帧率和更低功耗
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(320, 240))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }

        // 配置预览
        val preview = if (surfaceProvider != null) {
            androidx.camera.core.Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(surfaceProvider) }
        } else null

        try {
            // 解绑之前的用例
            provider.unbindAll()

            // 绑定新用例
            camera = if (preview != null) {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis,
                    preview
                )
            } else {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
            }

            _cameraState.value = CameraState.RUNNING
            if (BuildState.debug) "摄像头绑定成功, 预览=${preview != null}, 闪光灯可用=${camera?.cameraInfo?.hasFlashUnit()}".logd(TAG)

            // 自动开启闪光灯用于 PPG 测量
            setTorchEnabled(true)

        } catch (e: Exception) {
            if (BuildState.debug) "摄像头绑定失败: ${e.message}".loge(TAG)
            _cameraState.value = CameraState.ERROR
        }
    }

    /**
     * 处理图像帧
     *
     * 从 YUV_420_888 格式图像中提取红色通道信息。
     * 实际上我们使用 Y 通道亮度值作为信号源，因为在闪光灯照射下，
     * 亮度变化与血液脉动高度相关。
     */
    private fun processImage(imageProxy: ImageProxy) {
        try {
            // 获取时间戳 (纳秒)
            val timestamp = imageProxy.imageInfo.timestamp

            // 获取 Y 平面 (亮度)
            val yPlane = imageProxy.planes[0]
            val yBuffer = yPlane.buffer

            // 计算中心区域的平均亮度
            // 只处理图像中心部分，因为手指应该覆盖在镜头中心
            val width = imageProxy.width
            val height = imageProxy.height
            val rowStride = yPlane.rowStride

            // 定义采样区域 (中心 50%)
            val startX = width / 4
            val endX = width * 3 / 4
            val startY = height / 4
            val endY = height * 3 / 4

            var sum = 0L
            var count = 0

            for (y in startY until endY step 2) {  // 步长为 2 以减少计算量
                for (x in startX until endX step 2) {
                    val index = y * rowStride + x
                    if (index < yBuffer.capacity()) {
                        // 将字节转换为无符号值 (0-255)
                        val yValue = yBuffer.get(index).toInt() and 0xFF
                        sum += yValue
                        count++
                    }
                }
            }

            if (count > 0) {
                val averageBrightness = sum.toDouble() / count
                frameCallback?.onFrame(averageBrightness, timestamp)
            }

        } finally {
            imageProxy.close()
        }
    }

    /**
     * 控制闪光灯
     *
     * @param enabled 是否开启
     */
    fun setTorchEnabled(enabled: Boolean) {
        if (BuildState.debug) "闪光灯设置: ${if (enabled) "开启" else "关闭"}".logd(TAG)
        camera?.cameraControl?.enableTorch(enabled)
    }

    /**
     * 检查闪光灯是否可用
     */
    fun isTorchAvailable(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() == true
    }

    /**
     * 停止摄像头捕获
     */
    fun stopCapture() {
        if (BuildState.debug) "正在停止摄像头".logd(TAG)
        _cameraState.value = CameraState.STOPPING

        // 关闭闪光灯
        setTorchEnabled(false)

        // 解绑摄像头
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            if (BuildState.debug) "解绑摄像头失败: ${e.message}".loge(TAG)
        }

        camera = null
        imageAnalysis = null
        frameCallback = null

        _cameraState.value = CameraState.IDLE
        if (BuildState.debug) "摄像头已停止".logd(TAG)
    }

    /**
     * 释放资源
     *
     * 在不再需要摄像头时调用
     */
    fun release() {
        stopCapture()
        cameraExecutor.shutdown()
    }
}
