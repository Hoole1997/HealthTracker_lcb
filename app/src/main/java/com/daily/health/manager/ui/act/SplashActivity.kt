package com.daily.health.manager.ui.act

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
import androidx.compose.material3.ProgressIndicatorDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.constants.LANDING_NOTIFICATION_CONTENT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_FROM
import com.daily.health.manager.constants.LANDING_NOTIFICATION_TITLE
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.HtActivitySplashBinding
import com.daily.health.manager.hasNewGuide
import com.daily.health.manager.receiver.NotificationActionReceiver
import com.daily.health.manager.ui.history.HistoryRecordItem
import com.daily.health.manager.ui.theme.HealthTrackerTheme
import com.daily.health.manager.ui.viewmodel.SplashViewModel
import com.daily.health.manager.util.logEvent
import com.daily.health.manager.utils.isAdPage
import com.healthtracker.framework.R as FrameworkR
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
import net.corekit.core.report.ReportDataManager
import net.corekit.core.utils.ConfigRemoteManager
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.FullNativeAds
import net.corekit.monetize.ads.InterstitialAds
import net.corekit.monetize.ads.LaunchAds
import net.corekit.monetize.ads.SplashBiddingManager
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.log.AdLogger
import org.koin.android.ext.android.inject
import kotlin.math.ceil

class SplashActivity : BaseMVVMActivity<SplashViewModel, HtActivitySplashBinding>() {

    companion object {
        private const val TAG = "SplashActivity"

    }

    private var isAdLoaded = false
    private val hasFullNativeShowing: Boolean
        get() = FullNativeAds.getInstance().checkAdShowing()
    private val hasInterstitialShowing: Boolean
        get() = InterstitialAds.getInstance().checkAdShowing()
    
    private val startAnimationFlow = MutableStateFlow(false)


    // 状态机负责协调动画、权限、前后台状态与跳转
    private val stateMachine by lazy {
        SplashStateMachine(
            scope = lifecycleScope,
            onNavigate = {
                reportGroup()
                if (ActivityUtils.isActivityExistsInStack(MainActivity::class.java)) {
                    finish()
                    return@SplashStateMachine
                }
                // 判断应该跳转到哪个页面
                val targetActivity = if (hasNewGuide() || !AdConfigManager.showNewGuide()) {
                    MainActivity::class.java
                } else {
                    GuideActivity::class.java
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

    override fun createViewBinding() = HtActivitySplashBinding.inflate(layoutInflater)

    override fun getVMModelClass() = SplashViewModel::class.java
    private var launchTime = 0L
    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        mViewBind.composeView.setContent {
            HealthTrackerTheme {
                SplashScreen(
                    startAnimationFlow = startAnimationFlow,
                    recentRecordFlow = mViewModel.recentRecord,
                    onAnimationCompleted = ::onAnimationCompleted
                )
            }
        }
        lifecycleScope.launch {
            try {
                if (!isTaskRoot) {
                    val activityList = ActivityUtils.getActivityList()
                    if (isAdPage(activityList[1])) {
                        "当前是广告页面或引导页面，直接关闭启动页".logd(TAG)
                        finish()
                        return@launch
                    }

                    if(!App.INSTANCE.isLongLeaveApp() && (App.INSTANCE.isClickAdLeave || App.INSTANCE.isFeatureLeave || App.INSTANCE.isGoSetting)){
                        "用户点击广告，业务操作，请求权限短时间离开应用，直接关闭启动页".logd(TAG)
                        finish()
                        return@launch
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            launchTime = System.currentTimeMillis()
            logEvent("loading_page_show")
            playAnimations()
            // 启动页不再承载权限流程：直接标记权限流程完成，避免状态机卡住。
            stateMachine.onPermissionCheckCompleted()
            checkNotificationOpen()

            try {
                val adJob = async {
                    initializeAndShowAd()
                }

                val timeout = AdConfigManager.getSplashTimeout()
                AdLogger.d("启动页面，超时时长：$timeout s")
                val timeoutJob = async {
                    delay(timeout * 1000L)
                }
                val timeoutTriggered = select<Boolean> {
                    adJob.onAwait {
                        false
                    }
                    timeoutJob.onAwait {
                        true
                    }
                }

                if (timeoutTriggered) {
                    "触发超时".logd(TAG)
                    val hasAdLoaded = isAdLoaded
                    if (!hasAdLoaded && !hasFullNativeShowing && !hasInterstitialShowing) {
                        // 没有任何广告，执行继续流程
                        if (BuildState.debug) Log.d(TAG, "${timeout}秒超时兜底：无广告，执行继续流程")
                    } else {
                        // 有广告加载或显示，继续等待广告完成
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

            // 检查是否启用竞价模式
            val useBidding = SplashBiddingManager.shouldUseBidding()
            AdLogger.d("[SplashActivity] 竞价模式: %s", if (useBidding) "启用" else "禁用")

            val adResult = if (useBidding) {
                // 竞价模式：同时请求开屏和插屏，展示eCPM更高的
                if (BuildState.debug) "使用竞价模式加载广告".logd(TAG)
                SplashBiddingManager.bidAndShow(
                    activity = this,
                    onAdLoaded = { isSuccess ->
                        isAdLoaded = isSuccess
                    }
                )
            } else {
                // 传统模式：只请求开屏广告
                if (BuildState.debug) "使用传统模式加载开屏广告".logd(TAG)
                LaunchAds.getInstance().displayAd(
                    activity = this,
                    onLoaded = { isSuccess ->
                        isAdLoaded = isSuccess
                    }
                )
            }

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
            if(BuildState.debug) "设置开屏拦截等待结束".logd(TAG)
            LaunchAds.getInstance().cancelInterceptor()
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
            ReportDataManager.reportData("Grouping_$group")
        }
    }
}

@Composable
private fun AutoSizeSingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    maxFontSize: androidx.compose.ui.unit.TextUnit,
    minFontSize: androidx.compose.ui.unit.TextUnit,
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
private fun SplashScreen(
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
            .background(colorResource(id = R.color.c1))
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ht_bg_splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(id = R.mipmap.ht_bg_splash_top_start),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(id = R.mipmap.ht_bg_splash_top_end),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (logo, appName, loadingBar, loadingText, recentCard) = createRefs()
            val centerGuideline = createGuidelineFromTop(0.22f)
            val bottomGuideline = createGuidelineFromTop(0.88f)

            Image(
                painter = painterResource(id = R.drawable.ht_ic_logo_sq),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .constrainAs(logo) {
                        top.linkTo(centerGuideline)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                alpha = contentAlpha.value
            )

            Text(
                text = stringResource(id = R.string.app_name),
                color = colorResource(id = R.color.c5).copy(alpha = contentAlpha.value),
                fontSize = 22.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.roboto_bold)),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(appName) {
                    top.linkTo(logo.bottom, margin = 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
                maxLines = 1,
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
                        top.linkTo(bottomGuideline)
                        bottom.linkTo(bottomGuideline)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.percent(0.8f)
                    },
                color = colorResource(id = R.color.c5),
                trackColor = Color(0xFFB1EDD6),
                strokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap
            )

            Text(
                text = stringResource(id = R.string.ht_loading),
                color = colorResource(id = R.color.color_666),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.roboto_regular)),
                modifier = Modifier.constrainAs(loadingText) {
                    top.linkTo(loadingBar.bottom, margin = 10.dp)
                    start.linkTo(loadingBar.start)
                    end.linkTo(loadingBar.end)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentRecordCard(
    modifier: Modifier = Modifier,
    item: HistoryRecordItem
) {
    val context = LocalContext.current

    val typeName = when (item.getRecordType()) {
        HistoryRecordItem.RecordType.BLOOD_PRESSURE -> stringResource(id = R.string.ht_blood_pressure)
        HistoryRecordItem.RecordType.BLOOD_SUGAR -> stringResource(id = R.string.ht_blood_suger)
        HistoryRecordItem.RecordType.HEART_RATE -> stringResource(id = R.string.ht_heart_rate)
        HistoryRecordItem.RecordType.BMI_RECORD -> stringResource(id = R.string.ht_bmi)
        else -> ""
    }

    val title = stringResource(id = R.string.ht_last_measurement, typeName)
    val levelText = item.getLevel(context)
    val timeText = DateTimeUtils.formatDateTime(item.getRecordTime())
    val statusValue = item.getStatus(context)

    val statusText = when {
        statusValue.isNullOrBlank() -> null
        item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_PRESSURE -> {
            "${stringResource(id = R.string.ht_pulse)}:$statusValue"
        }
        item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_SUGAR -> {
            "${stringResource(id = R.string.ht_status)}:$statusValue"
        }
        else -> statusValue
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_F1F8F6))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = title,
                color = colorResource(id = R.color.t1),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.roboto_black)),
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.getPrimaryValue(),
                        color = colorResource(id = R.color.t1),
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.roboto_black)),
                        fontWeight = FontWeight.Black
                    )
                    val secondary = item.getSecondaryValue()
                    if (!secondary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = secondary,
                            color = colorResource(id = R.color.t1),
                            fontSize = 18.sp,
                            fontFamily = FontFamily(Font(FrameworkR.font.roboto_black)),
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.getUnit(),
                        color = colorResource(id = R.color.color_999),
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.roboto_regular))
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colorResource(id = item.getLeveColorRes()))
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = levelText,
                        color = colorResource(id = R.color.t1),
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.roboto_bold)),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!statusText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AutoSizeSingleLineText(
                            text = statusText,
                            modifier = Modifier.fillMaxWidth(),
                            color = colorResource(id = R.color.t1),
                            maxFontSize = 14.sp,
                            minFontSize = 6.sp,
                            fontFamily = FontFamily(Font(FrameworkR.font.roboto_regular))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeText,
                        color = colorResource(id = R.color.color_999),
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.roboto_regular)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
