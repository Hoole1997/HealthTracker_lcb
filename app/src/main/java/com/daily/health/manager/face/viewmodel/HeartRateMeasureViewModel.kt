package com.daily.health.manager.face.viewmodel

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.daily.health.manager.analyzer.HeartRateAnalyzer
import com.daily.health.manager.camera.CameraXManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd

/**
 * 心率测量 ViewModel
 *
 * 采用 MVI 架构，实现新的测量流程：
 * 1. WAITING_FINGER: 等待用户放置手指
 * 2. STABILIZING: 手指已放置，稳定期 3 秒
 * 3. MEASURING: 正式测量中，显示实时心率
 * 4. COMPLETE: 测量完成
 *
 * 手指离开时：进度条 300ms 渐减动画 → 回退到 WAITING_FINGER
 */
import com.daily.health.manager.data.repository.HeartRateRepository

class HeartRateMeasureViewModel(
    private val context: Context,
    private val heartRateRepository: HeartRateRepository
) : BaseViewModel() {

    companion object {
        private const val TAG = "PPG_ViewModel"
        const val MEASUREMENT_DURATION_MS = 20_000L     // 测量时长 20 秒
        const val MIN_SAMPLES_FOR_RESULT = 300          // 最少样本数 (10秒 * 30fps)
        const val STABILIZATION_TIME_MS = 3_000L        // 稳定时间 3 秒
        const val PROGRESS_DECREASE_ANIM_MS = 300L      // 进度回退动画时长
        const val FINGER_CHECK_INTERVAL_MS = 100L       // 手指检测间隔
        const val UI_UPDATE_INTERVAL_MS = 200L          // UI 更新间隔
    }

    // ===== State =====
    private val _uiState = MutableStateFlow(MeasureUiState())
    val uiState: StateFlow<MeasureUiState> = _uiState.asStateFlow()

    // ===== Effect =====
    private val _effect = MutableSharedFlow<MeasureEffect>()
    val effect: SharedFlow<MeasureEffect> = _effect.asSharedFlow()

    // ===== Internal =====
    private var cameraManager: CameraXManager? = null
    private val analyzer = HeartRateAnalyzer()
    private var mainJob: Job? = null
    private var progressAnimJob: Job? = null
    private var previewSurfaceProvider: androidx.camera.core.Preview.SurfaceProvider? = null
    private var lifecycleOwnerRef: LifecycleOwner? = null

    /**
     * 设置预览 SurfaceProvider
     */
    fun setPreviewSurfaceProvider(provider: androidx.camera.core.Preview.SurfaceProvider) {
        this.previewSurfaceProvider = provider
    }

    /**
     * 处理用户事件
     */
    fun onEvent(event: MeasureEvent) {
        if (BuildState.debug) "用户事件: $event".logd(TAG)
        when (event) {
            is MeasureEvent.StartCamera -> startCamera(event.lifecycleOwner)
            is MeasureEvent.StopCamera -> stopCamera()
            is MeasureEvent.UseMeasurement -> useMeasurement()
        }
    }

    /**
     * 启动摄像头并进入等待手指状态
     */
    private fun startCamera(lifecycleOwner: LifecycleOwner) {
        if (cameraManager != null) {
            if (BuildState.debug) "摄像头已启动，跳过".logd(TAG)
            return
        }

        lifecycleOwnerRef = lifecycleOwner
        analyzer.reset()

        // 更新状态为等待手指
        _uiState.update {
            MeasureUiState(measureState = MeasureState.WAITING_FINGER)
        }

        // 初始化摄像头
        cameraManager = CameraXManager(context)
        cameraManager?.startCapture(lifecycleOwner, previewSurfaceProvider) { redValue, timestamp ->
            analyzer.addSample(redValue, timestamp)
        }

        if (BuildState.debug) "摄像头已启动，等待手指放置".logd(TAG)

        // 启动主流程协程
        mainJob = viewModelScope.launch {
            runMeasurementFlow()
        }
    }

    /**
     * 运行测量主流程
     */
    private suspend fun runMeasurementFlow() {
        while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
            when (_uiState.value.measureState) {
                MeasureState.WAITING_FINGER -> handleWaitingFinger()
                MeasureState.STABILIZING -> handleStabilizing()
                MeasureState.MEASURING -> handleMeasuring()
                MeasureState.COMPLETE -> break // 结束循环
            }
        }
    }

    /**
     * 等待手指放置
     */
    private suspend fun handleWaitingFinger() {
        if (BuildState.debug) "状态: WAITING_FINGER".logd(TAG)

        while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true && _uiState.value.measureState == MeasureState.WAITING_FINGER) {
            val fingerDetected = analyzer.isFingerDetected()

            _uiState.update { it.copy(isFingerDetected = fingerDetected) }

            if (fingerDetected) {
                // 检测到手指，进入稳定期
                if (BuildState.debug) "检测到手指，进入稳定期".logd(TAG)
                _uiState.update {
                    it.copy(
                        measureState = MeasureState.STABILIZING,
                        stabilizationStartTime = System.currentTimeMillis()
                    )
                }
                return
            }

            delay(FINGER_CHECK_INTERVAL_MS)
        }
    }

    /**
     * 稳定期处理 (3秒)
     * 
     * 在稳定期内使用更宽容的检测逻辑：
     * - 不会因为单次检测失败就回退
     * - 需要连续多次丢失才会回退（由 Analyzer 的防抖机制保证）
     */
    private suspend fun handleStabilizing() {
        if (BuildState.debug) "状态: STABILIZING".logd(TAG)
        val startTime = _uiState.value.stabilizationStartTime
        
        runSafeMeasurementLoop(MeasureState.STABILIZING) {
            val elapsed = System.currentTimeMillis() - startTime
            val quality = analyzer.getSignalQuality()

            _uiState.update {
                it.copy(
                    isFingerDetected = true,
                    signalQuality = mapQuality(quality)
                )
            }

            if (elapsed >= STABILIZATION_TIME_MS) {
                if (BuildState.debug) "稳定期完成，开始测量".logd(TAG)
                _uiState.update {
                    it.copy(
                        measureState = MeasureState.MEASURING,
                        measureStartTime = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    /**
     * 测量中处理
     * 
     * 直接使用 Analyzer 的防抖结果，快速响应手指离开
     */
    private suspend fun handleMeasuring() {
        if (BuildState.debug) "状态: MEASURING".logd(TAG)
        val startTime = _uiState.value.measureStartTime

        runSafeMeasurementLoop(MeasureState.MEASURING, UI_UPDATE_INTERVAL_MS) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / MEASUREMENT_DURATION_MS).coerceIn(0f, 1f)
            val instantBpm = analyzer.getInstantBpm()
            val quality = analyzer.getSignalQuality()

            _uiState.update {
                it.copy(
                    progress = progress,
                    currentBpm = instantBpm ?: it.currentBpm,
                    signalQuality = mapQuality(quality),
                    isFingerDetected = true
                )
            }

            if (elapsed >= MEASUREMENT_DURATION_MS) {
                completeMeasurement()
            }
        }
    }

    /**
     * 手指离开处理 - 进度条渐减动画后回退到初始状态
     */
    private suspend fun handleFingerLost() {
        val currentProgress = _uiState.value.progress

        // 如果已经是 0 进度，直接回退
        if (currentProgress <= 0f) {
            resetToWaitingState()
            return
        }

        // 取消之前的动画任务
        progressAnimJob?.cancel()

        // 启动进度渐减动画 (300ms)
        val animStartTime = System.currentTimeMillis()

        while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
            val elapsed = System.currentTimeMillis() - animStartTime
            val fraction = (elapsed.toFloat() / PROGRESS_DECREASE_ANIM_MS).coerceIn(0f, 1f)
            val newProgress = currentProgress * (1f - fraction)

            _uiState.update { it.copy(progress = newProgress) }

            if (fraction >= 1f) break
            delay(16) // ~60fps
        }

        // 重置到等待状态
        resetToWaitingState()
    }

    /**
     * 重置到等待手指状态
     */
    private fun resetToWaitingState() {
        if (BuildState.debug) "重置到 WAITING_FINGER 状态".logd(TAG)

        // 完全重置分析器
        analyzer.reset()

        _uiState.update {
            MeasureUiState(
                measureState = MeasureState.WAITING_FINGER,
                progress = 0f,
                currentBpm = null,
                signalQuality = SignalQuality.NO_SIGNAL,
                isFingerDetected = false
            )
        }
    }

    /**
     * 完成测量
     */
    private suspend fun completeMeasurement() {
        if (BuildState.debug) "测量完成: 样本数=${analyzer.getSampleCount()}".logd(TAG)

        // 获取最终结果
        val result = analyzer.getMeasurementResult()

        // 如果结果无效，回退重测
        if (result.bpm == null || result.quality == HeartRateAnalyzer.SignalQuality.POOR ||
            result.quality == HeartRateAnalyzer.SignalQuality.NO_SIGNAL
        ) {
            if (BuildState.debug) "测量结果无效，回退重测".logd(TAG)
            handleFingerLost()
            return
        }

        // 停止摄像头
        cameraManager?.stopCapture()

        // 保存到数据库
        val recordId = heartRateRepository.addHeartRateRecord(
            heartRateBpm = result.bpm,
            // ext1 也可以存 confidence
            ext1 = result.confidence.toString(),
            ext2 = "camera"
        )

        _uiState.update {
            it.copy(
                measureState = MeasureState.COMPLETE,
                progress = 1f,
                finalBpm = result.bpm,
                currentBpm = result.bpm,
                confidence = result.confidence,
                recordId = recordId
            )
        }

        if (BuildState.debug) "测量成功: 心率=${result.bpm} BPM, 置信度=${result.confidence}, ID=$recordId".logd(TAG)
        _effect.emit(MeasureEffect.MeasurementComplete(result.bpm, recordId))
    }

    /**
     * 停止摄像头
     */
    private fun stopCamera() {
        mainJob?.cancel()
        mainJob = null
        progressAnimJob?.cancel()
        progressAnimJob = null
        cameraManager?.stopCapture()
        cameraManager = null
        lifecycleOwnerRef = null

        _uiState.update { MeasureUiState() }
    }

    /**
     * 使用测量结果
     */
    private fun useMeasurement() {
        val bpm = _uiState.value.finalBpm ?: return
        val recordId = _uiState.value.recordId ?: return
        viewModelScope.launch {
            _effect.emit(MeasureEffect.NavigateToResult(bpm, recordId))
        }
    }

    /**
     * 通用测量循环骨架
     * 自动处理：协程活跃检查、状态检查、手指丢失检测
     */
    private suspend fun runSafeMeasurementLoop(
        targetState: MeasureState,
        checkInterval: Long = FINGER_CHECK_INTERVAL_MS,
        onTick: suspend () -> Unit
    ) {
        while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true && _uiState.value.measureState == targetState) {
            if (!analyzer.isFingerDetected()) {
                if (BuildState.debug) "状态 $targetState 下手指离开，回退".logd(TAG)
                handleFingerLost()
                return
            }
            onTick()
            delay(checkInterval)
        }
    }

    /**
     * 映射信号质量
     */
    private fun mapQuality(quality: HeartRateAnalyzer.SignalQuality): SignalQuality {
        return when (quality) {
            HeartRateAnalyzer.SignalQuality.EXCELLENT -> SignalQuality.EXCELLENT
            HeartRateAnalyzer.SignalQuality.GOOD -> SignalQuality.GOOD
            HeartRateAnalyzer.SignalQuality.FAIR -> SignalQuality.FAIR
            HeartRateAnalyzer.SignalQuality.POOR -> SignalQuality.POOR
            HeartRateAnalyzer.SignalQuality.NO_SIGNAL -> SignalQuality.NO_SIGNAL
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 先 release 释放 executor 线程，再 stopCamera 清理状态
        // stopCamera 内部会将 cameraManager 置 null，所以必须先调用 release
        cameraManager?.release()
        stopCamera()
    }
}

// ===== State =====

/**
 * UI 状态
 */
data class MeasureUiState(
    val measureState: MeasureState = MeasureState.WAITING_FINGER,
    val progress: Float = 0f,
    val currentBpm: Int? = null,
    val finalBpm: Int? = null,
    val signalQuality: SignalQuality = SignalQuality.NO_SIGNAL,
    val isFingerDetected: Boolean = false,
    val confidence: Float = 0f,
    val stabilizationStartTime: Long = 0L,
    val measureStartTime: Long = 0L,
    val recordId: Long? = null
)

/**
 * 测量状态
 *
 * WAITING_FINGER → STABILIZING → MEASURING → COMPLETE
 *       ↑________________|_____________|
 *              (手指离开时回退)
 */
enum class MeasureState {
    WAITING_FINGER, // 等待手指放置
    STABILIZING,    // 稳定期 (3秒)
    MEASURING,      // 测量中
    COMPLETE        // 完成
}

/**
 * 信号质量
 */
enum class SignalQuality {
    EXCELLENT,  // 优秀
    GOOD,       // 良好
    FAIR,       // 一般
    POOR,       // 较差
    NO_SIGNAL   // 无信号
}

// ===== Event =====

/**
 * 用户事件
 */
sealed class MeasureEvent {
    data class StartCamera(val lifecycleOwner: LifecycleOwner) : MeasureEvent()
    data object StopCamera : MeasureEvent()
    data object UseMeasurement : MeasureEvent()
}

// ===== Effect =====

/**
 * 一次性副作用
 */
sealed class MeasureEffect {
    data class MeasurementComplete(val bpm: Int, val recordId: Long) : MeasureEffect()
    data class NavigateToResult(val bpm: Int, val recordId: Long) : MeasureEffect()
}
