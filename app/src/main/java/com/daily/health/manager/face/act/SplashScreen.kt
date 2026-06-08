package com.daily.health.manager.face.act

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.constants.KEY_FROM_SHORTCUT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_CONTENT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_FROM
import com.daily.health.manager.constants.LANDING_NOTIFICATION_TITLE
import com.daily.health.manager.constants.UNINSTALL
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.TrActivitySplashBinding
import com.daily.health.manager.feature.NotificationFeatureSwitch
import com.daily.health.manager.hasNewGuide
import com.daily.health.manager.receiver.NotificationActionReceiver
import com.daily.health.manager.face.history.HistoryRecordItem
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.face.viewmodel.SplashViewModel
import com.daily.health.manager.util.logEvent
import com.daily.health.manager.utils.isAdPage
import com.daily.health.manager.face.tracker.trackUninstallClick
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.SysBarUtils
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.SpUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.PreloadController
import com.android.common.bill.ads.ext.AdShowExt
import com.android.common.bill.ads.ext.CountdownConfig
import com.android.common.bill.ads.log.AdLogger
import com.android.common.bill.ads.util.GoogleMobileAdsConsentManager
import net.corekit.core.report.ReportDataManager
import net.corekit.core.utils.ConfigRemoteManager
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ads.config.AdConfigManager
import com.daily.health.manager.alarm.PermissionManager
import kotlin.math.ceil
import com.healthtracker.framework.R as FrameworkR

private val SplashDark = Color(0xFF1B1D2C)
private val SplashTitle = Color(0xFF333333)
private val SplashMuted = Color(0xFF999999)
private val SplashProgressTrack = Color(0xFFEDEFF4)
private val SplashRecordCard = Color(0xFFEBEBFF)

class SplashScreen : BaseMVVMActivity<SplashViewModel, TrActivitySplashBinding>() {

    companion object {
        private const val TAG = "SplashScreen"

    }

    private var isAdLoaded = false
    private val hasFullNativeShowing: Boolean
        get() = AdShowExt.isAnyInterstitialOrFullScreenNativeShowing()
    private val hasInterstitialShowing: Boolean
        get() = AdShowExt.isAnyInterstitialOrFullScreenNativeShowing()
    
    private val startAnimationFlow = MutableStateFlow(false)
    
    // 权限管理器
    private val permissionManager = PermissionManager()


    // 状态机负责协调动画、权限、前后台状态与跳转
    private val stateMachine by lazy {
        SplashStateMachine(
            scope = lifecycleScope,
            onNavigate = {
                if(intent.hasExtra(KEY_FROM_SHORTCUT) && intent.getStringExtra(KEY_FROM_SHORTCUT) == UNINSTALL){
                    // 跳转到卸载挽留页面
                    trackUninstallClick()
                    startActivity(Intent(this@SplashScreen, UninstallResenActivity::class.java))
                    finish()
                    return@SplashStateMachine
                }
                reportGroup()
                if (ActivityUtils.isActivityExistsInStack(MainAct::class.java)) {
                    finish()
                    return@SplashStateMachine
                }
                // 判断应该跳转到哪个页面
                val targetActivity = if (hasNewGuide()) {
                    MainAct::class.java
                } else {
                    GuideAct::class.java
                }
                // 创建Intent并传递通知参数
                val targetIntent = Intent(this, targetActivity).apply {
                    putExtras(intent)
                }

                startActivity(targetIntent)
                finish()
            }
        )
    }

    override fun createViewBinding() = TrActivitySplashBinding.inflate(layoutInflater)

    override fun getVMModelClass() = SplashViewModel::class.java
    private var launchTime = 0L
    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        mViewBind.composeView.setContent {
            HealthTrackerTheme {
                LaunchContent(
                    startAnimationFlow = startAnimationFlow,
                    recentRecordFlow = mViewModel.recentRecord,
                    onAnimationCompleted = ::onAnimationCompleted
                )
            }
        }
        lifecycleScope.launch {
//            try {
//                if (!isTaskRoot) {
//                   finish()
//                }
//            } catch (e: Throwable) {
//                e.printStackTrace()
//            }

            launchTime = System.currentTimeMillis()
            logEvent("loading_page_show")
            playAnimations()
            checkNotificationOpen()
            
            // ========== 并行执行：通知权限、广告加载 ==========
            val timeout = AdConfigManager.getSplashTimeout()
            AdLogger.d("[$TAG] 启动页面，超时时长：$timeout s")
            
            // 1. 通知权限检查（并行）
            val permissionJob = async {
                if (NotificationFeatureSwitch.notificationPermissionPromptEnabled) {
                    checkNotificationPermissionFlow()
                }
            }
            
            // 2. 广告加载（并行）
            val adJob = async {
                initializeAndShowAd()
            }
            
            try {
                // 3. 等待权限完成（广告加载不阻塞，继续在后台）
                permissionJob.await()
                
                // 4. UMP 同意检查
                try {
                    AdLogger.d("[$TAG] 开始 UMP 同意检查")
                    GoogleMobileAdsConsentManager.getInstance(this@SplashScreen)
                        .gatherConsent(this@SplashScreen)
                    AdLogger.d("[$TAG] UMP 同意检查完成")
                } catch (e: Exception) {
                    AdLogger.e("[$TAG] UMP 同意检查异常: ${e.message}")
                }
                
                // 5. 放开跳转状态机
                stateMachine.onPermissionCheckCompleted()
                
                // 7. 超时任务（UMP 完成后才开始计时，仅针对广告展示阶段）
                val timeoutJob = async {
                    // delay(timeout * 1000L)
                    delay(0L)
                }
                
                // 8. 等待广告完成或超时
                val timeoutTriggered = select<Boolean> {
                    adJob.onAwait { false }
                    timeoutJob.onAwait { true }
                }

                if (timeoutTriggered) {
                    "触发超时".logd(TAG)
                    val hasAdLoaded = isAdLoaded
                    if (!hasAdLoaded && !hasFullNativeShowing && !hasInterstitialShowing) {
                        if (BuildState.debug) Log.d(TAG, "${timeout}秒超时兜底：无广告，执行继续流程")
                    } else {
                        if (BuildState.debug) Log.d(
                            TAG,
                            "${timeout}秒超时兜底：有广告(loaded=$hasAdLoaded, fullNative=$hasFullNativeShowing, interstitial=$hasInterstitialShowing)，等待广告完成"
                        )
                        adJob.await()
                    }
                } else {
                    "非超时触发".logd(TAG)
                }
            } catch (e: Throwable) {
                AdLogger.e("[$TAG] 启动流程异常: ${e.message}")
            } finally {
                stateMachine.onAdCompleted()
            }




        }

    }

    private fun checkNotificationOpen() {
        try {
            val notificationId =
                intent.getIntExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, -1)
            reportOpen()

            if (notificationId == -1) {
                if (BuildState.debug) "Invalid notification ID: $notificationId".logw(TAG)
                return
            }
            reportNotificationParam()
            val actionType = intent.getStringExtra(NotificationActionReceiver.EXTRA_ACTION_VALUE)
            if (BuildState.debug) "checkNotificationOpen actionType = $actionType".logd(TAG)
            sendBroadcast(Intent(this, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_NOTIFICATION_CLICKED
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            })
        } catch (e: Throwable) {

        }


    }


    private fun reportOpen() {
        ReportDataManager.reportData(
            "app_open", mapOf(
                "type" to if (isTaskRoot) "cold_open" else "hot_open",
                "position" to if (intent.hasExtra(LANDING_NOTIFICATION_FROM)) intent.getStringExtra(
                    LANDING_NOTIFICATION_FROM
                ).orEmpty().ifBlank { "other" } else "other"
            ))
    }

    private fun reportNotificationParam() {
        val params = mutableMapOf<String, Any>(
            "Notific_Type" to when (intent.getStringExtra(LANDING_NOTIFICATION_FROM)
                .orEmpty()) {
                "firebase_push" -> 2
                "top_notification" -> 4
                else -> 1
            },
            "Notific_Position" to when (intent.getStringExtra(LANDING_NOTIFICATION_FROM)
                .orEmpty()) {
                "top_notification" -> 2
                else -> 1
            },
            "Notific_Priority" to when (intent.getStringExtra(LANDING_NOTIFICATION_FROM)
                .orEmpty()) {
                "top_notification" -> "PRIORITY_DEFAULT"
                else -> "PRIORITY_HIGH"
            },
            "event_id" to when (intent.getStringExtra(LANDING_NOTIFICATION_FROM)
                .orEmpty()) {
                "top_notification" -> "permanent"
                else -> "customer_general_style"
            },
            "title" to intent.getStringExtra(LANDING_NOTIFICATION_TITLE).orEmpty(),
            "text" to intent.getStringExtra(LANDING_NOTIFICATION_CONTENT).orEmpty()
        )

        ReportDataManager.reportData(
            "Notific_Enter", params
        )

        ReportDataManager.reportData(
            "Notific_Click", params.apply {
                put("from_background", AppLifecycleManager.isBackground())

            }
        )
    }

    /**
     * 初始化 AdMob 并显示开屏广告
     * 根据配置决定是使用竞价模式还是传统模式
     * @return 广告是否加载成功
     */
    private suspend fun initializeAndShowAd(): Boolean {
        try {
            if (BuildState.debug) "AdMob SDK 初始化成功，准备显示开屏广告".logd(TAG)

            val adResult = AdShowExt.showAppOpenAd(
                activity = this,
                onLoaded = { isSuccess ->
                    PreloadController.preloadAll(this)
                    isAdLoaded = isSuccess
                },
                countdown = CountdownConfig(seconds = 2),
                position = AdPosition.SP_APP_START
            )

            if (adResult is AdResult.Success) {
                if (BuildState.debug) "广告展示完成并关闭".logd(TAG)
                return true
            } else {
                if (BuildState.debug) "广告显示失败: ${(adResult as? AdResult.Failure)?.error?.message}".logd(TAG)
                return false
            }
        } catch (e: Exception) {
            if (BuildState.debug) "广告初始化或显示异常 e:$e".loge(TAG)
            return false
        }
    }

    override fun onResume() {
        super.onResume()
        stateMachine.onResume()
        SysBarUtils.hideNavigationBar(this)
    }

    override fun onPause() {
        super.onPause()
        stateMachine.onPause()
    }

    override fun onDestroy() {
        stateMachine.onDestroy()
        super.onDestroy()
        logEvent(
            "loading_page_end", mapOf(
                "pass_time" to ceil((System.currentTimeMillis() - launchTime) / 1000.0).toInt()
            )
        )
    }

    override fun isFullscreen() = true

    /**
     * 播放所有启动动画
     */
    private fun playAnimations() {
        if (startAnimationFlow.value) {
            return
        }
        startAnimationFlow.value = true
    }

    /**
     * 创建淡入动画
     * 保持与原版本完全相同的动画参数
     */

    /**
     * 动画完成回调
     */
    private fun onAnimationCompleted() {
        if (BuildState.debug) "动画执行完成".logd(TAG)
        stateMachine.onAnimationCompleted()
    }
    
    /**
     * 检查通知权限流程
     * 
     * 在启动页请求通知权限，不阻塞流程
     */
    private suspend fun checkNotificationPermissionFlow() {
        try {
            suspendCancellableCoroutine<Boolean> { cont ->
                permissionManager.checkNotificationPermission(
                    activity = this@SplashScreen,
                    onGoSetting = {
                        // 启动页不处理跳转设置的情况，直接完成
                    }
                ) {
                    if (cont.isActive) {
                        cont.resume(it)
                    }
                }
            }
        } catch (e: Exception) {
            if (BuildState.debug) "通知权限检查异常: ${e.message}".loge(TAG)
        }
    }

    /**
     * 启动页状态机，统一管理动画、权限与导航状态
     */
    private  class SplashStateMachine(
        private val scope: CoroutineScope,
        private val onNavigate: suspend () -> Unit
    ) {

        private var animationDone = false
        private var permissionDone = false
        private var adDone = false
        private var isForeground = true
        private var pendingForegroundNavigation = false
        private var hasNavigated = false
        private var navigationJob: Job? = null

        fun onAnimationCompleted() {
            if (animationDone) {
                return
            }
            animationDone = true
            if (BuildState.debug) "Animation completed".logd(TAG)
            tryNavigate()

        }

        fun onPermissionCheckCompleted() {
            if (permissionDone) {
                return
            }
            permissionDone = true
            if (BuildState.debug) "Permission check completed".logd(TAG)
            tryNavigate()

        }

        fun onAdCompleted() {
            if (adDone) {
                return
            }
            adDone = true
            if (BuildState.debug) "Ad completed".logd(TAG)
            tryNavigate()
        }

        fun onResume() {
            isForeground = true
            if (pendingForegroundNavigation) {
                tryNavigate()
            }
        }

        fun onPause() {
            isForeground = false
        }

        fun onDestroy() {
            navigationJob?.cancel()
            navigationJob = null
        }

        private fun tryNavigate() {
            if (hasNavigated) {
                if (BuildState.debug) "Already navigated, ignore further requests".logd(TAG)
                return
            }

            if (!(animationDone && permissionDone && adDone)) {
                if (BuildState.debug) "Waiting for completion - Animation: $animationDone, Permission: $permissionDone".logd(
                    TAG
                )
                return
            }

            if (navigationJob?.isActive == true) {
                if (BuildState.debug) "Navigation coroutine is already running".logd(TAG)
                return
            }

            navigationJob = scope.launch {
                val skipDelay = pendingForegroundNavigation
                if (!skipDelay) {
                    delay(500)
                }

                if (!isForeground) {
                    pendingForegroundNavigation = true
                    return@launch
                }

                pendingForegroundNavigation = false
                if (hasNavigated) {
                    return@launch
                }

                hasNavigated = true
                onNavigate()
            }.also { job ->
                job.invokeOnCompletion {
                    navigationJob = null
                }
            }
        }
    }

    private fun reportGroup(){
        lifecycleScope.launch {
           val group =  ConfigRemoteManager.getString("Grouping","")
            if(group.isNullOrEmpty()){
                if(BuildState.debug) "没有配置Group,不上报".logd(TAG)
                return@launch
            }
            if(SpUtils.getBoolean("has_report_group_$group",false)){
                if(BuildState.debug) "已经上报过$group,不再上报".logd(TAG)
                return@launch
            }
            SpUtils.putBoolean("has_report_group_$group",true)
            if(BuildState.debug) "上报Group,value:$group".logd(TAG)
            ReportDataManager.reportData("Grouping_$group", mapOf())
        }
    }
}

@Composable
private fun AutoSizeSingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        modifier = modifier,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (!result.didOverflowWidth || fontSize <= minFontSize) {
                return@Text
            }

            val availableWidth = result.size.width.toFloat()
            val lineRight = try {
                result.getLineRight(0)
            } catch (_: Throwable) {
                0f
            }
            if (availableWidth <= 0f || lineRight <= 0f) {
                return@Text
            }

            val ratio = (availableWidth / lineRight).coerceIn(0f, 1f)
            val next = (fontSize.value * ratio).coerceAtLeast(minFontSize.value)
            if (next < fontSize.value - 0.1f) {
                fontSize = next.sp
            }
        }
    )
}

@Composable
private fun LaunchContent(
    startAnimationFlow: StateFlow<Boolean>,
    recentRecordFlow: StateFlow<HistoryRecordItem?>,
    onAnimationCompleted: () -> Unit
) {
    val startAnimation by startAnimationFlow.collectAsState()
    val recentRecord by recentRecordFlow.collectAsState()
    val onAnimationCompletedState by rememberUpdatedState(onAnimationCompleted)

    val contentAlpha = remember { Animatable(0f) }
    var hasSentAnimationCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(startAnimation) {
        if (!startAnimation) {
            return@LaunchedEffect
        }
        hasSentAnimationCompleted = false
        contentAlpha.snapTo(0f)

        delay(200)
        contentAlpha.animateTo(1f, animationSpec = tween(durationMillis = 1000))

        if (!hasSentAnimationCompleted) {
            hasSentAnimationCompleted = true
            onAnimationCompletedState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .splashFigmaBackground()
    ) {

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (logo, appName, loadingBar, loadingText, recentCard) = createRefs()
            val logoTopGuideline = createGuidelineFromTop(0.198f)
            val bottomGuideline = createGuidelineFromBottom(0.12f)

            Image(
                painter = painterResource(id = R.mipmap.tr_ic_logo_sq),
                contentDescription = null,
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .constrainAs(logo) {
                        top.linkTo(logoTopGuideline)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                alpha = contentAlpha.value
            )

            Text(
                text = stringResource(id = R.string.app_name),
                color = SplashTitle,
                fontSize = 24.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.inter_bold)),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.constrainAs(appName) {
                    top.linkTo(logo.bottom, margin = 18.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.percent(0.78f)
                }.alpha(contentAlpha.value),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            recentRecord?.let {
                RecentRecordCard(
                    modifier = Modifier.constrainAs(recentCard) {
                        bottom.linkTo(loadingBar.top, margin = 26.dp)
                        start.linkTo(loadingBar.start)
                        end.linkTo(loadingBar.end)
                        width = Dimension.fillToConstraints
                    },
                    item = it
                )
            }

            LinearProgressIndicator(
                modifier = Modifier
                    .height(6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .constrainAs(loadingBar) {
                        bottom.linkTo(loadingText.top, margin = 14.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.percent(0.81f)
                    },
                color = SplashDark,
                trackColor = SplashProgressTrack
            )

            Text(
                text = stringResource(id = R.string.tr_loading_pure),
                color = SplashDark,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.inter_medium)),
                modifier = Modifier.constrainAs(loadingText) {
                    bottom.linkTo(bottomGuideline)
                    start.linkTo(loadingBar.start)
                    end.linkTo(loadingBar.end)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun Modifier.splashFigmaBackground(): Modifier = drawBehind {
    drawRect(Color.White)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFEEF3).copy(alpha = 0.78f), Color.Transparent),
            center = Offset(size.width * 1.02f, size.height * 0.12f),
            radius = size.width * 0.55f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFEEF3).copy(alpha = 0.82f), Color.Transparent),
            center = Offset(size.width * 0.98f, size.height * 0.96f),
            radius = size.width * 0.48f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFEBEBFF).copy(alpha = 0.82f), Color.Transparent),
            center = Offset(-size.width * 0.08f, size.height * 0.47f),
            radius = size.width * 0.42f
        )
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFF2F2FF).copy(alpha = 0.72f), Color.Transparent),
            center = Offset(size.width * 0.08f, -size.height * 0.03f),
            radius = size.width * 0.62f
        )
    )
}

@Composable
private fun RecentRecordCard(
    modifier: Modifier = Modifier,
    item: HistoryRecordItem
) {
    val context = LocalContext.current

    val typeName = when (item.getRecordType()) {
        HistoryRecordItem.RecordType.BLOOD_PRESSURE -> stringResource(id = R.string.tr_blood_pressure)
        HistoryRecordItem.RecordType.BLOOD_SUGAR -> stringResource(id = R.string.tr_blood_suger)
        HistoryRecordItem.RecordType.HEART_RATE -> stringResource(id = R.string.tr_heart_rate)
        HistoryRecordItem.RecordType.BMI_RECORD -> stringResource(id = R.string.tr_bmi)
        else -> ""
    }

    val title = stringResource(id = R.string.tr_last_measurement, typeName)
    val levelText = item.getLevel(context)
    val timeText = DateTimeUtils.formatDateTime(item.getRecordTime())
    val statusValue = item.getStatus(context)

    val statusText = when {
        statusValue.isNullOrBlank() -> null
        item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_PRESSURE -> {
            "${stringResource(id = R.string.tr_pulse)}:$statusValue"
        }
        item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_SUGAR -> {
            "${stringResource(id = R.string.tr_status)}:$statusValue"
        }
        else -> statusValue
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SplashRecordCard)
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, top = 10.dp, end = 16.dp, bottom = 12.dp)) {
            Text(
                text = title,
                color = SplashTitle,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.inter_black)),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(45.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.getPrimaryValue(),
                        color = SplashTitle,
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_black)),
                        fontWeight = FontWeight.Black
                    )
                    val secondary = item.getSecondaryValue()
                    if (!secondary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = secondary,
                            color = SplashTitle,
                            fontSize = 18.sp,
                            fontFamily = FontFamily(Font(FrameworkR.font.inter_black)),
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.getUnit(),
                        color = SplashMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_regular))
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(SplashDark)
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = levelText,
                        color = SplashTitle,
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_bold)),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!statusText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AutoSizeSingleLineText(
                            text = statusText,
                            modifier = Modifier.fillMaxWidth(),
                            color = SplashTitle,
                            maxFontSize = 14.sp,
                            minFontSize = 6.sp,
                            fontFamily = FontFamily(Font(FrameworkR.font.inter_regular))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeText,
                        color = SplashMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_regular)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
