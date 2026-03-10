package com.daily.health.manager.face.act

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import com.daily.health.manager.R
import com.daily.health.manager.databinding.TrActivityLanguageSelectBinding
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
import com.daily.health.manager.utils.loadNative
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle
import com.healthtracker.framework.util.PermissionUtils
import com.hjq.permissions.permission.PermissionLists
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Brush
import com.daily.health.manager.App
import com.daily.health.manager.face.card.PpgExplainBottomSheet


/**
 * 心率测量页面
 * 
 * 通过摄像头和闪光灯进行 PPG 心率测量
 */
class HeartRateMeasureScreen : BaseMVVMActivity<BaseViewModel, TrActivityLanguageSelectBinding>() {

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
    private var showPermissionDialog by mutableStateOf(false)

    override fun createViewBinding() = TrActivityLanguageSelectBinding.inflate(layoutInflater)

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
                            handleMeasureComplete(effect.bpm, effect.recordId)
                        }
                        is MeasureEffect.NavigateToResult -> {
                            navigateToDetailAndFinish(effect.bpm, effect.recordId)
                        }
                    }
                }
            }

            HealthTrackerTheme {
                var showExplainSheet by remember { mutableStateOf(false) }

                HeartRateMeasureScreenContent(
                    hasPermission = hasPermission,
                    showPermissionDenied = showPermissionDenied,
                    showPermissionDialog = showPermissionDialog,
                    measureUiState = measureUiState,
                    onBackClick = { finish() },
                    onHelpClick = { showExplainSheet = true },
                    onRequestPermission = { showPermissionDialogAction() },
                    onDismissPermissionDialog = { dismissPermissionDialog() },
                    onGrantPermission = {
                        requestCameraPermission()
                    },
                    onGoToSettings = {
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
                    onResumeFlashlight = {
                        heartRateViewModel.onEvent(MeasureEvent.ResumeFlashlight)
                    },
                    onMeasureComplete = { bpm -> handleMeasureComplete(bpm, 0L) },
                    onSurfaceProviderReady = { provider ->
                        heartRateViewModel.setPreviewSurfaceProvider(provider)
                    }
                )

                if (showExplainSheet) {
                    PpgExplainBottomSheet(
                        onDismiss = { showExplainSheet = false }
                    )
                }
            }
        }

        loadNative(mViewBind.adContainer, AdPosition.NA_HEART_RATE_MEASURE_BOTTOM, NativeAdStyle.STANDARD)
    }

    override fun onResume() {
        super.onResume()
        // 用户从设置页回来可能已经授权，这里需要自查
        if (!hasPermission) {
            val nowHas = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (nowHas) {
                hasPermission = true
                showPermissionDialog = false
            }
        }
    }

    private fun checkCameraPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        hasPermission = granted
        if (!granted) {
            // 预检查：如果用户之前已经选择了“不再询问”，则直接进入“拒绝”状态以显示“去设置”
            showPermissionDenied = com.hjq.permissions.XXPermissions.isDoNotAskAgainPermissions(this, listOf(PermissionLists.getCameraPermission()))
        }
    }

    private fun showPermissionDialogAction() {
        showPermissionDialog = true
    }

    private fun dismissPermissionDialog() {
        showPermissionDialog = false
        // 用户手动关闭弹窗或点击取消，说明不想测了，直接退出
        if (!hasPermission) {
            finish()
        }
    }

    private fun requestCameraPermission() {
        PermissionUtils.requestCameraPermission(this) { granted, isDoNotAsk ->
            hasPermission = granted
            if (granted) {
                showPermissionDialog = false
                showPermissionDenied = false
            } else {
                if (isDoNotAsk) {
                    // 永久拒绝，此时需要显示“去设置”
                    showPermissionDenied = true
                } else {
                    // 普通拒绝，直接退出页面
                    finish()
                }
            }
        }
    }

    private fun goToAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
        App.INSTANCE.isGoSetting = true
    }

    private fun handleMeasureComplete(bpm: Int, recordId: Long) {
        // 显示保存完成动效
        SaveCompleteDialog.show(supportFragmentManager) {
            // 动效完成后跳转到详情页面
            navigateToDetailAndFinish(bpm, recordId)
        }
    }

    private fun navigateToDetailAndFinish(bpm: Int, recordId: Long) {
        // 先保存心率记录，然后跳转到详情页面
        
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
    onHelpClick: () -> Unit,
    onRequestPermission: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
    onGrantPermission: () -> Unit,
    onGoToSettings: () -> Unit,
    onStartCamera: () -> Unit,
    onStopCamera: () -> Unit,
    onUseMeasurement: () -> Unit,
    onResumeFlashlight: () -> Unit, // 新增：恢复闪光灯回调
    onMeasureComplete: (Int) -> Unit,
    onSurfaceProviderReady: (androidx.camera.core.Preview.SurfaceProvider) -> Unit
) {
    // 监听生命周期，从后台返回时恢复闪光灯
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, hasPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasPermission) {
                onResumeFlashlight()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
        TopBar(onBackClick = onBackClick, onHelpClick = onHelpClick)

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
                    painter = painterResource(R.drawable.tr_ic_back),
                    contentDescription = "back",
                    tint = Color.Unspecified,
                )
            }
        }

        Text(
            text = stringResource(R.string.tr_measure_title),
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
                painter = painterResource(R.drawable.tr_ic_questing),
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
                painter = painterResource(R.drawable.tr_ic_measure_camera),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            Text(
                text = stringResource(
                    if (isPermissionDenied) R.string.tr_camera_permission_denied_title
                    else R.string.tr_camera_permission_title
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
                    if (isPermissionDenied) R.string.tr_camera_permission_denied_desc
                    else R.string.tr_camera_permission_desc
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
                    if (isPermissionDenied) R.string.tr_go_to_settings
                    else R.string.tr_grant_permission
                ),
                onClick = if (isPermissionDenied) onGoToSettings else onGrantPermission
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 取消按钮
            Text(
                text = stringResource(R.string.tr_cancel),
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
    // 获取 Context 用于震动
    val context = LocalContext.current
    
    // 震动器
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    // 测量状态变化时控制震动
    LaunchedEffect(measureState) {
        if (measureState == UiMeasureState.MEASURING) {
            // 开始心跳震动 (每秒一次轻微震动)
            while (true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
                delay(1000L)
            }
        }
    }
    
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
            UiMeasureState.WAITING_FINGER -> {
                // 等待手指: 显示引导图
                Spacer(modifier = Modifier.height(16.dp))

                // 免责声明文案
                Text(
                    text = stringResource(R.string.tr_measure_disclaimer),
                    fontSize = 14.sp,
                    color = colorResource(R.color.t1),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 手指引导提示文案
                Text(
                    text = stringResource(R.string.tr_measure_finger_instruction),
                    fontSize = 14.sp,
                    color = colorResource(R.color.color_666),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                MeasureBottomArea(
                    modifier = Modifier.weight(1f)
                ) {
                    // 手机图片
                    Image(
                        painter = painterResource(R.mipmap.ic_phone),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
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
                            .offset(x = 16.dp, y = 20.dp)
                    )

                    // 绿色勾选图标 - 底部居中
                    Image(
                        painter = painterResource(R.drawable.tr_ic_checked),
                        contentDescription = null,
                        modifier = Modifier
                            .size(66.dp).offset(y = 40.dp)
                    )
                }
            }
            UiMeasureState.STABILIZING -> {
                // 测量状态标头 (标题 + 副标题)
                MeasureStatusHeader()
                
                MeasureBottomArea(
                    modifier = Modifier.weight(1f),
                    showBackground = false
                ) {
                    // 底部提示区域
                    Row(
                        modifier = Modifier.padding(horizontal = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // 心形图标
                        Image(
                            painter = painterResource(R.drawable.ic_heart_shap_red),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.tr_stabilizing_tip),
                            fontSize = 13.sp,
                            color = colorResource(R.color.color_666),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            UiMeasureState.MEASURING -> {
                // 测量状态标头 (标题 + 副标题)
                MeasureStatusHeader()
                
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.heart_rate_volatility)
                )

                // 调整播放速度，使动画与测量时长同步 (20s)
                val speed = remember(composition) {
                    if (composition == null) 1f else {
                        val animDuration = composition!!.duration
                        val totalDuration = 20_000f
                        if (animDuration <= 0f) 1f else {
                            val ratio = totalDuration / animDuration
                            val cycles = kotlin.math.round(ratio.toDouble()).toFloat().coerceAtLeast(1f)
                            (cycles * animDuration) / totalDuration
                        }
                    }
                }

                val animationProgress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    speed = speed,
                    isPlaying = true
                )

                MeasureBottomArea(
                    modifier = Modifier.weight(1f),
                    showBackground = false
                ) {
                    // 恢复底部 Lottie 心电图动画
                    LottieAnimation(
                        composition = composition,
                        progress = { animationProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            UiMeasureState.COMPLETE -> {
                // 测量完成
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.tr_measure_complete),
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
    // 震动器
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Figma设计中的粉色背景 #F7EAF1
    val pinkBackground = Color(0xFFF7EAF1)
    // 根据设计图更新进度条颜色 #FB4248
    val progressColor = Color(0xFFFB4248)
    val isMeasuring = measureState == UiMeasureState.MEASURING

    // 监听测量状态，驱动震动 (这里简单保留，后续可根据动画同步)
    LaunchedEffect(isMeasuring) {
        if (isMeasuring) {
             // 简单模拟心跳震动
             while (true) {
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                     vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                 } else {
                     vibrator.vibrate(50)
                 }
                 delay(1000L)
             }
        }
    }

    // 整体布局：使用Box让心电图可以重叠
    // clipToBounds = false 允许 Lottie 动画扩散时溢出
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .graphicsLayer { clip = false },
        contentAlignment = Alignment.Center
    ) {
        // 1. 最底层：自定义纯 Compose 脉动波纹 (呼吸感粉色方案 - 增强版)
        if (isMeasuring) {
            // 使用比背景稍深、更饱和的粉色增加可见度 #F4D7E5
            val optimizedPink = Color(0xFFF4D7E5) 
            PulseRippleBackground(
                modifier = Modifier.size(400.dp),
                color = optimizedPink
            )
        }

        // 左侧心电图线条
        Image(
            painter = painterResource(R.mipmap.ic_heartbeat_left),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-20).dp),
            contentScale = ContentScale.FillHeight
        )

        // 右侧心电图线条
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
            
            // 静态背景粉色圆 (160dp)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(pinkBackground)
            )

            // 环形进度条 (轨道 + 进度)
            MeasureRingProgress(
                progress = progress,
                activeColor = progressColor,
                modifier = Modifier.size(166.dp)
            )

            // 5. 顶层：心形图标 + 摄像头预览
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // 摄像头预览
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

                // 状态文字显示
                when (measureState) {
                    UiMeasureState.WAITING_FINGER, UiMeasureState.STABILIZING -> {
                        // 等待手指/稳定期: 统一显示 "- - -"
                        BpmValueText(text = "- - -", fontSize = 24.sp, letterSpacing = 4.sp)
                    }
                    UiMeasureState.MEASURING -> {
                        // 测量中显示实时心率 (无单位)
                        val displayText = if (measuredBpm > 0) measuredBpm.toString() else "- -"
                        BpmValueText(text = displayText)
                    }
                    UiMeasureState.COMPLETE -> {
                        // 完成时显示最终 BPM (带单位)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BpmValueText(text = measuredBpm.toString())
                            Text(
                                text = stringResource(R.string.tr_bpm),
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
            painter = painterResource(R.drawable.tr_ic_play),
            contentDescription = stringResource(R.string.tr_start_measure),
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


/**
 * 底部通用测量区域 - 带网格背景
 * @param showBackground 是否显示白色背景和圆角 (Wait Finger状态需要，其他状态不需要)
 */
@Composable
private fun MeasureBottomArea(
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showBackground) {
                    Modifier
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color.White)
                } else {
                    Modifier
                }
            )
    ) {
        // 网格背景 - 居中显示
        Image(
            painter = painterResource(R.mipmap.bg_grid),
            contentDescription = null,
            modifier = Modifier
                .size(width = 315.dp, height = 126.dp)
                .align(Alignment.Center),
            contentScale = ContentScale.FillWidth,
        )

        // 内容区域
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * 自定义脉动波纹背景 (纯 Compose Canvas 版)
 * 特点：心跳脉冲曲线 + 三层同步爆发 + 柔焦边缘
 */
@Composable
private fun PulseRippleBackground(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFF7EAF1)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // 心跳周期：1000ms (1秒一次)
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = modifier) {
        val center = center
        val baseRadius = 80.dp.toPx()
        
        // 循环绘制三层波纹，减少冗余代码
        val layers = listOf(
            1.2f to 0.8f,  // scale range end to alpha start
            1.45f to 0.6f,
            1.7f to 0.4f
        )
        
        layers.forEach { (maxScale, startAlpha) ->
            drawRipple(center, baseRadius, pulseProgress, color, 1.0f..maxScale, startAlpha..0.0f)
        }
    }
}

/**
 * 绘制单个波纹环，带柔焦渐变效果
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRipple(
    center: androidx.compose.ui.geometry.Offset,
    baseRadius: Float,
    progress: Float,
    color: Color,
    scaleRange: ClosedRange<Float>,
    alphaRange: ClosedRange<Float>
) {
    val currentScale = scaleRange.start + (scaleRange.endInclusive - scaleRange.start) * progress
    val currentAlpha = alphaRange.start + (alphaRange.endInclusive - alphaRange.start) * (1f - progress) // 随扩散变淡
    val currentRadius = baseRadius * currentScale
    
    if (currentAlpha <= 0.05f) return

    // 使用径向渐变实现柔焦边缘 (Soft Glow - 增强版)
    // colors: 通过增加中间断点，让颜色在扩散半径内保留得更久，看起来更“厚实”
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to color.copy(alpha = 0f),               // 内部透明，不遮挡心形
            0.5f to color.copy(alpha = currentAlpha * 0.6f), // 中间开始爆发颜色
            0.85f to color.copy(alpha = currentAlpha * 0.9f), // 边缘处强度最大
            1.0f to Color.Transparent,                    // 极速消散，呈现毛玻璃边缘感
            center = center,
            radius = currentRadius
        ),
        radius = currentRadius,
        center = center
    )
}

/**
 * 统一度量环绘制 (背景轨道 + 进度)
 */
@Composable
private fun MeasureRingProgress(
    progress: Int,
    activeColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: androidx.compose.ui.unit.Dp = 6.dp
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        // 1. 轨道 (Track)
        drawArc(color = Color.White, 0f, 360f, false, style = stroke)
        // 2. 进度 (Progress)
        if (progress > 0) {
            drawArc(color = activeColor, -90f, 360f * progress / 100f, false, style = stroke)
        }
    }
}

/**
 * 统一心率数字样式
 */
@Composable
private fun BpmValueText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 32.sp,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        letterSpacing = letterSpacing
    )
}

/**
 * 测量状态标头 (标题 + 副标题)
 */
@Composable
private fun MeasureStatusHeader() {
    Spacer(modifier = Modifier.height(40.dp))
    
    // "Measuring..."
    Text(
        text = stringResource(R.string.tr_stabilizing_title),
        fontSize = 16.sp,
        color = colorResource(R.color.t1),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(22.dp))
    
    // "Great! Waiting for the reading..."
    Text(
        text = stringResource(R.string.tr_stabilizing_subtitle),
        fontSize = 14.sp,
        color = colorResource(R.color.color_666),
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}
