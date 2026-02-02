package com.daily.health.manager.face.act

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import android.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.core.graphics.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.daily.health.manager.R
import com.daily.health.manager.databinding.HtActivityLanguageSelectBinding
import com.daily.health.manager.face.dialog.SaveCompleteDialog
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.face.viewmodel.HeartRateMeasureViewModel
import com.daily.health.manager.face.viewmodel.MeasureEvent
import com.daily.health.manager.face.viewmodel.MeasureEffect
import com.daily.health.manager.face.viewmodel.MeasureUiState
import com.daily.health.manager.face.viewmodel.SignalQuality as ViewModelSignalQuality
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import org.koin.android.ext.android.inject

/**
 * 心率测量页面
 * 
 * 通过摄像头和闪光灯进行 PPG 心率测量
 */
class HeartRateMeasureScreen : BaseMVVMActivity<BaseViewModel, HtActivityLanguageSelectBinding>() {

    companion object {
        const val EXTRA_RESULT_BPM = "result_bpm"
        const val MEASURE_DURATION_SECONDS = 30

        fun start(context: Context) {
            context.startActivity(Intent(context, HeartRateMeasureScreen::class.java))
        }

        fun startForResult(context: Context): Intent {
            return Intent(context, HeartRateMeasureScreen::class.java)
        }
    }

    // 注入心率测量 ViewModel
    private val heartRateViewModel: HeartRateMeasureViewModel by inject()

    private var hasPermission by mutableStateOf(false)
    private var showPermissionDenied by mutableStateOf(false)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        showPermissionDenied = !isGranted
    }

    override fun createViewBinding() = HtActivityLanguageSelectBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        checkCameraPermission()

        mViewBind.composeView.setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            val measureUiState by heartRateViewModel.uiState.collectAsStateWithLifecycle()

            // 监听测量 Effect
            LaunchedEffect(Unit) {
                heartRateViewModel.effect.collect { effect ->
                    when (effect) {
                        is MeasureEffect.MeasurementComplete -> {
                            handleMeasureComplete(effect.bpm)
                        }
                        is MeasureEffect.NavigateToResult -> {
                            navigateToDetailAndFinish(effect.bpm)
                        }
                    }
                }
            }

            HealthTrackerTheme {
                HeartRateMeasureScreenContent(
                    hasPermission = hasPermission,
                    showPermissionDenied = showPermissionDenied,
                    showPermissionDialog = showPermissionDialog,
                    measureUiState = measureUiState,
                    onBackClick = { finish() },
                    onRequestPermission = { showPermissionDialogAction() },
                    onDismissPermissionDialog = { dismissPermissionDialog() },
                    onGrantPermission = {
                        dismissPermissionDialog()
                        requestCameraPermission()
                    },
                    onGoToSettings = {
                        dismissPermissionDialog()
                        goToAppSettings()
                    },
                    onStartCamera = {
                        heartRateViewModel.onEvent(MeasureEvent.StartCamera(lifecycleOwner))
                    },
                    onStopCamera = {
                        heartRateViewModel.onEvent(MeasureEvent.StopCamera)
                    },
                    onUseMeasurement = {
                        heartRateViewModel.onEvent(MeasureEvent.UseMeasurement)
                    },

                    onMeasureComplete = { bpm -> handleMeasureComplete(bpm) },
                    onSurfaceProviderReady = { provider ->
                        heartRateViewModel.setPreviewSurfaceProvider(provider)
                    }
                )
            }
        }
    }

    private fun checkCameraPermission() {
        hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private var showPermissionDialog by mutableStateOf(false)

    private fun showPermissionDialogAction() {
        showPermissionDialog = true
    }

    private fun dismissPermissionDialog() {
        showPermissionDialog = false
    }

    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun goToAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun handleMeasureComplete(bpm: Int) {
        // 显示保存完成动效
        SaveCompleteDialog.show(supportFragmentManager) {
            // 动效完成后跳转到详情页面
            navigateToDetailAndFinish(bpm)
        }
    }

    private fun navigateToDetailAndFinish(bpm: Int) {
        // 先保存心率记录，然后跳转到详情页面
        // 这里需要调用 ViewModel 保存记录，然后获取记录ID
        // 暂时使用模拟的记录ID
        val recordId = System.currentTimeMillis()
        
        // 使用 FLAG_ACTIVITY_CLEAR_TOP 启动首页，清除中间页面
        val mainIntent = Intent(this, MainScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(mainIntent)
        
        // 启动详情页面
         HealthDetailScreen.start(
            this,
            HealthDetailScreen.DetailType.HEART_RATE,
            recordId
        )
        finish()
    }

    override fun onDestroy() {
        // 页面销毁时停止摄像头和协程
        heartRateViewModel.onEvent(MeasureEvent.StopCamera)
        super.onDestroy()
    }
}

/**
 * UI 测量状态 - 与 ViewModel 状态对应
 */
private enum class UiMeasureState {
    WAITING_FINGER, // 等待手指放置
    STABILIZING,    // 稳定期 (3秒)
    MEASURING,      // 测量中
    COMPLETE        // 测量完成
}

@Composable
private fun HeartRateMeasureScreenContent(
    hasPermission: Boolean,
    showPermissionDenied: Boolean,
    showPermissionDialog: Boolean,
    measureUiState: MeasureUiState,
    onBackClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
    onGrantPermission: () -> Unit,
    onGoToSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onStopCamera: () -> Unit,
    onUseMeasurement: () -> Unit,
    onMeasureComplete: (Int) -> Unit,
    onSurfaceProviderReady: (androidx.camera.core.Preview.SurfaceProvider) -> Unit
) {
    // 权限状态变化时的处理
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            onRequestPermission()
        } else {
            // 有权限时自动启动摄像头
            onStartCamera()
        }
    }

    // 权限申请底部弹窗
    if (showPermissionDialog) {
        CameraPermissionBottomSheet(
            isPermissionDenied = showPermissionDenied,
            onDismiss = onDismissPermissionDialog,
            onGrantPermission = onGrantPermission,
            onGoToSettings = onGoToSettings
        )
    }

    // 背景色使用Figma设计的#F5F7FB
    val backgroundColor = Color(0xFFF5F7FB)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Top Bar - 白色背景
        TopBar(onBackClick = onBackClick)

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasPermission) {
                MeasureContent(
                    measureState = mapUiMeasureState(measureUiState.measureState),
                    progress = (measureUiState.progress * 100).toInt(),
                    measuredBpm = measureUiState.currentBpm ?: 0,
                    signalQuality = measureUiState.signalQuality,
                    isFingerDetected = measureUiState.isFingerDetected,
                    onUseMeasurement = onUseMeasurement,
                    onSurfaceProviderReady = onSurfaceProviderReady
                )
            }
            // 无权限时不显示任何内容，只显示底部弹窗
        }
    }
}

// 将 ViewModel MeasureState 映射到 Screen UiMeasureState
private fun mapUiMeasureState(state: com.daily.health.manager.face.viewmodel.MeasureState): UiMeasureState = when (state) {
    com.daily.health.manager.face.viewmodel.MeasureState.WAITING_FINGER -> UiMeasureState.WAITING_FINGER
    com.daily.health.manager.face.viewmodel.MeasureState.STABILIZING -> UiMeasureState.STABILIZING
    com.daily.health.manager.face.viewmodel.MeasureState.MEASURING -> UiMeasureState.MEASURING
    com.daily.health.manager.face.viewmodel.MeasureState.COMPLETE -> UiMeasureState.COMPLETE
}

@Composable
private fun TopBar(
    onBackClick: () -> Unit,
    onHelpClick: () -> Unit = {}
) {
    val bgColor = colorResource(R.color.c1)
    val titleColor = colorResource(R.color.t1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(bgColor)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ht_ic_back),
                    contentDescription = "back",
                    tint = Color.Unspecified,
                )
            }
        }

        Text(
            text = stringResource(R.string.ht_measure_title),
            color = titleColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )

        // 问号帮助图标
        IconButton(
            onClick = onHelpClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ht_ic_questing),
                contentDescription = "help",
                tint = Color.Unspecified,
            )
        }
    }
}

/**
 * Compose实现的相机权限申请底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraPermissionBottomSheet(
    isPermissionDenied: Boolean,
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorResource(R.color.c1),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标
            Icon(
                painter = painterResource(R.drawable.ht_ic_measure_camera),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            Text(
                text = stringResource(
                    if (isPermissionDenied) R.string.ht_camera_permission_denied_title
                    else R.string.ht_camera_permission_title
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.t1),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 描述
            Text(
                text = stringResource(
                    if (isPermissionDenied) R.string.ht_camera_permission_denied_desc
                    else R.string.ht_camera_permission_desc
                ),
                fontSize = 14.sp,
                color = colorResource(R.color.color_666),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 主要按钮
            PrimaryButton(
                text = stringResource(
                    if (isPermissionDenied) R.string.ht_go_to_settings
                    else R.string.ht_grant_permission
                ),
                onClick = if (isPermissionDenied) onGoToSettings else onGrantPermission
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 取消按钮
            Text(
                text = stringResource(R.string.ht_cancel),
                fontSize = 14.sp,
                color = colorResource(R.color.color_666),
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun MeasureContent(
    measureState: UiMeasureState,
    progress: Int,
    measuredBpm: Int,
    signalQuality: ViewModelSignalQuality = ViewModelSignalQuality.NO_SIGNAL,
    isFingerDetected: Boolean = false,
    onUseMeasurement: () -> Unit,
    onSurfaceProviderReady: (androidx.camera.core.Preview.SurfaceProvider) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 心形图标区域（带心电图线条）- 根据Figma设计
        HeartIconArea(
            measureState = measureState,
            progress = progress,
            measuredBpm = measuredBpm,
            onSurfaceProviderReady = onSurfaceProviderReady
        )

        // 根据状态显示不同内容
        when (measureState) {
            UiMeasureState.WAITING_FINGER, UiMeasureState.STABILIZING -> {
                // 等待手指/稳定期: 显示引导图
                Spacer(modifier = Modifier.height(16.dp))

                // 免责声明文案
                Text(
                    text = stringResource(R.string.ht_measure_disclaimer),
                    fontSize = 14.sp,
                    color = colorResource(R.color.t1),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 手指引导提示文案 - 根据状态显示不同文案
                Text(
                    text = stringResource(
                        if (measureState == UiMeasureState.STABILIZING) 
                            R.string.ht_measure_stabilizing 
                        else 
                            R.string.ht_measure_finger_instruction
                    ),
                    fontSize = 14.sp,
                    color = colorResource(R.color.color_666),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                PhoneFingerGuide()
            }
            UiMeasureState.MEASURING -> {
                // 测量中: 显示信号质量指示器和进度
                Spacer(modifier = Modifier.height(8.dp))
                SignalQualityIndicator(
                    signalQuality = signalQuality,
                    isFingerDetected = isFingerDetected
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 测量进度文案
                Text(
                    text = stringResource(R.string.ht_measuring_progress, progress),
                    fontSize = 16.sp,
                    color = colorResource(R.color.t1),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))
            }
            UiMeasureState.COMPLETE -> {
                // 测量完成
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.ht_measure_complete),
                    fontSize = 18.sp,
                    color = colorResource(R.color.c5),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }


    }
}

/**
 * 信号质量指示器
 * 根据信号质量和手指检测状态显示不同的文本和颜色
 */
@Composable
private fun SignalQualityIndicator(
    signalQuality: ViewModelSignalQuality,
    isFingerDetected: Boolean
) {
    val (text, color) = when {
        !isFingerDetected -> stringResource(R.string.ht_place_finger) to Color.Red
        signalQuality == ViewModelSignalQuality.EXCELLENT -> stringResource(R.string.ht_signal_excellent) to Color(0xFF4CAF50)
        signalQuality == ViewModelSignalQuality.GOOD -> stringResource(R.string.ht_signal_good) to Color(0xFF8BC34A)
        signalQuality == ViewModelSignalQuality.FAIR -> stringResource(R.string.ht_signal_fair) to Color(0xFFFFC107)
        else -> stringResource(R.string.ht_signal_poor) to Color(0xFFFF5722)
    }
    
    Text(
        text = text,
        fontSize = 14.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/**
 * 手机+手指引导图片组合 - 根据Figma设计
 * 包含：网格背景 + 16dp圆角背景 + 手机图片 + 光圈图标 + 手指图片
 */
@Composable
private fun PhoneFingerGuide() {
    // 整体容器 - 带16dp顶部圆角的白色背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color.White),
    ) {
        // 网格背景
        Image(
            painter = painterResource(R.mipmap.bg_grid),
            contentDescription = null,
            modifier = Modifier.size(width = 315.dp,126.dp).align(Alignment.Center),
            contentScale = ContentScale.FillWidth,
            alpha = 0.5f
        )

        // 内容区域
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 手机图片
            Image(
                painter = painterResource(R.mipmap.ic_phone),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
            )

            // 大光圈图标 - 在手机闪光灯位置
            Image(
                painter = painterResource(R.mipmap.ic_flash_big),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.Center)
                    .offset(x = (-10).dp, y = (-73).dp)
            )

            // 小光圈图标 - 叠加在大光圈上
            Image(
                painter = painterResource(R.mipmap.ic_flash_small),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
                    .offset(x = (-10).dp, y = (-73).dp)
            )

            // 手指图片 - 指尖放在闪光灯位置
            Image(
                painter = painterResource(R.mipmap.ic_hand),
                contentDescription = null,
                modifier = Modifier
                    .height(180.dp)
                    .align(Alignment.Center)
                    .offset(x = 36.dp, y = 4.dp)
            )

            // 绿色勾选图标 - 底部居中
            Image(
                painter = painterResource(R.drawable.ht_ic_checked),
                contentDescription = null,
                modifier = Modifier
                    .size(66.dp).offset(y = 40.dp)
            )
        }
    }
}

/**
 * 心形图标区域 - 根据Figma设计
 * 心电图线条 + 粉色背景圆 + 心形图标（用于摄像头预览）+ 环形进度条
 */
@Composable
private fun HeartIconArea(
    measureState: UiMeasureState,
    progress: Int,
    measuredBpm: Int,
    onSurfaceProviderReady: (androidx.camera.core.Preview.SurfaceProvider) -> Unit
) {
    // Figma设计中的粉色背景 #F7EAF1
    val pinkBackground = Color(0xFFF7EAF1)
    val progressColor = colorResource(R.color.c5)

    // 整体布局：使用Box让心电图可以重叠
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // 左侧心电图线条 - 使用负偏移让其与中心重叠
        Image(
            painter = painterResource(R.mipmap.ic_heartbeat_left),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-20).dp),
            contentScale = ContentScale.FillHeight
        )

        // 右侧心电图线条 - 使用负偏移让其与中心重叠
        Image(
            painter = painterResource(R.mipmap.ic_heartbeat_right),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp),
            contentScale = ContentScale.FillHeight
        )

        // 中间粉色背景圆 + 环形进度条 + 心形图标
        Box(
            modifier = Modifier.size(166.dp),
            contentAlignment = Alignment.Center
        ) {
            // 环形进度条轨道（白色）
            Canvas(modifier = Modifier.size(166.dp)) {
                drawArc(
                    color = Color.White,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 环形进度条（c5颜色）- 测量中或回退时显示
            if (progress > 0) {
                Canvas(modifier = Modifier.size(166.dp)) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress / 100f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 粉色背景圆
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(pinkBackground),
                contentAlignment = Alignment.Center
            ) {
                // 心形图标（用于显示摄像头预览或占位）
                Box(
                    modifier = Modifier
                        .size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 摄像头预览 - 替换原本的 Image 占位
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                onSurfaceProviderReady(this.getSurfaceProvider())
                            }
                        },
                        modifier = Modifier
                            .size(140.dp)
                            .offset(y = 14.dp)
                            .clip(HeartShape)
                    )

                    // 根据状态显示不同内容
                    when (measureState) {
                        UiMeasureState.WAITING_FINGER -> {
                            // 等待手指: 显示 "---"
                            Text(
                                text = "- - -",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 4.sp
                            )
                        }
                        UiMeasureState.STABILIZING -> {
                            // 稳定期: 显示稳定中提示
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "...",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        UiMeasureState.MEASURING -> {
                            // 测量中显示实时心率
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (measuredBpm > 0) {
                                    Text(
                                        text = measuredBpm.toString(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = stringResource(R.string.ht_bpm),
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        text = "...",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        UiMeasureState.COMPLETE -> {
                            // 显示BPM
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = measuredBpm.toString(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.ht_bpm),
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 心形Shape - 用于裁剪摄像头预览区域
 */
/**
 * 心形Shape - 用于裁剪摄像头预览区域
 * 使用 ic_shap_heart.xml 中的精确路径
 */
private val HeartShape = object : Shape {
    // ic_shap_heart.xml 中的 pathData
    private val pathData = "M140,44.19C140,90.26 70,125.13 70,125.13C70,125.13 0,90.4 0,44.19C0,23.65 16.65,7 37.19,7C51.04,7 63.12,14.57 69.52,25.81C69.73,26.18 70.26,26.18 70.46,25.81C76.88,14.57 88.96,7 102.81,7C123.35,7 140,23.65 140,44.19Z"

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = PathParser.createPathFromPathData(pathData)
        
        // 计算缩放比例 (XML viewport 是 140x140)
        val scaleX = size.width / 140f
        val scaleY = size.height / 140f
        
        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY)
        path.transform(matrix)
        
        return Outline.Generic(path.asComposePath())
    }
}

/**
 * 开始测量按钮 - 圆形绿色按钮带播放图标
 */
@Composable
private fun StartMeasureButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(66.dp)
            .clip(CircleShape)
            .background(colorResource(R.color.c5))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 播放图标（三角形）
        Icon(
            painter = painterResource(R.drawable.ht_ic_play),
            contentDescription = stringResource(R.string.ht_start_measure),
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colorResource(R.color.c5))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(
                width = 1.dp,
                color = colorResource(R.color.c5),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.c5)
        )
    }
}

