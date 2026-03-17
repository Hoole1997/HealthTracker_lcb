package com.daily.health.manager.face.act

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.daily.health.manager.App
import com.daily.health.manager.BuildConfig
import com.daily.health.manager.R
import com.daily.health.manager.constants.KEY_FROM_SHORTCUT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_CONTENT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_FROM
import com.daily.health.manager.constants.LANDING_NOTIFICATION_TITLE
import com.daily.health.manager.constants.UNINSTALL
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.FcActivitySplashBinding
import com.daily.health.manager.feature.NotificationFeatureSwitch
import com.daily.health.manager.hasNewGuide
import com.daily.health.manager.receiver.NotificationActionReceiver
import com.daily.health.manager.face.history.HistoryRecordItem
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.face.viewmodel.SplashViewModel
import com.daily.health.manager.face.tracker.trackUninstallClick
import com.daily.health.manager.util.logEvent
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import net.corekit.core.report.ReportDataManager
import net.corekit.core.utils.ConfigRemoteManager
import net.corekit.monetize.ump.UmpConsentController
import com.daily.health.manager.alarm.PermissionManager
import kotlin.math.ceil
import com.healthtracker.framework.R as FrameworkR

class SplashScreen : BaseMVVMActivity<SplashViewModel, FcActivitySplashBinding>() {

    companion object {
        private const val TAG = "SplashScreen"
    }

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

    override fun createViewBinding() = FcActivitySplashBinding.inflate(layoutInflater)

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
                   finish()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            launchTime = System.currentTimeMillis()
            logEvent("loading_page_show")
            playAnimations()
            checkNotificationOpen()

            // 并行执行初始化准备，但不再展示开屏广告
            val ipJob = async {
                try {
                    UmpConsentController.prefetchCountryCode()
                } catch (e: Exception) {
                    if (BuildState.debug) "IP 预取异常: ${e.message}".loge(TAG)
                }
            }

            val permissionJob = async {
                if (NotificationFeatureSwitch.notificationPermissionPromptEnabled) {
                    checkNotificationPermissionFlow()
                }
            }

            try {
                permissionJob.await()
                ipJob.await()

                try {
                    UmpConsentController.checkAndShowConsentIfNeeded(this@SplashScreen)
                } catch (e: Exception) {
                    if (BuildState.debug) "UMP 同意检查异常: ${e.message}".loge(TAG)
                }
            } catch (e: Throwable) {
                if (BuildState.debug) "启动流程异常: ${e.message}".loge(TAG)
            } finally {
                stateMachine.onPermissionCheckCompleted()
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

            if (!(animationDone && permissionDone)) {
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
private fun SplashScreen(
    startAnimationFlow: StateFlow<Boolean>,
    recentRecordFlow: StateFlow<HistoryRecordItem?>,
    onAnimationCompleted: () -> Unit
) {
    val startAnimation by startAnimationFlow.collectAsState()
    val recentRecord by recentRecordFlow.collectAsState()
    val onAnimationCompletedState by rememberUpdatedState(onAnimationCompleted)
    val context = LocalContext.current

    val contentAlpha = remember { Animatable(0f) }
    var hasSentAnimationCompleted by remember { mutableStateOf(false) }
    var showLogoShadow by remember { mutableStateOf(false) }

    LaunchedEffect(startAnimation) {
        if (!startAnimation) {
            return@LaunchedEffect
        }
        hasSentAnimationCompleted = false
        showLogoShadow = false
        contentAlpha.snapTo(0f)

        delay(120)
        contentAlpha.animateTo(1f, animationSpec = tween(durationMillis = 1000))
        delay(120)
        showLogoShadow = true

        if (!hasSentAnimationCompleted) {
            hasSentAnimationCompleted = true
            onAnimationCompletedState()
        }
    }

    val appName = stringResource(id = R.string.app_name).replace("\n", " ")
    val accentColor = Color(0xFFFF7C3F)
    val accentTrackColor = Color(0xFFFFE8D8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFCF6),
                        Color(0xFFF8F4F6),
                    )
                )
            )
    ) {
        SplashWarmGlow()

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 132.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LauncherLogo(
                alpha = contentAlpha.value,
                showShadow = showLogoShadow
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = appName,
                color = colorResource(id = R.color.t1),
                fontSize = 24.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.inter_bold)),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp)
                    .alpha(contentAlpha.value),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 36.dp, end = 36.dp, bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (recentRecord != null) {
                RecentRecordCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(contentAlpha.value),
                    item = recentRecord
                )
                Spacer(modifier = Modifier.height(26.dp))
            }

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .alpha(contentAlpha.value),
                color = accentColor,
                trackColor = accentTrackColor,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(id = R.string.fc_loading_pure),
                color = accentColor,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.inter_medium)),
                modifier = Modifier.alpha(contentAlpha.value),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(34.dp))

            PrivacyAgreementRow(
                modifier = Modifier.alpha(contentAlpha.value),
                accentColor = accentColor,
                onClick = {
                    InnerWebAct.start(context, BuildConfig.PRIVACY_POLICY)
                }
            )
        }
    }
}

@Composable
private fun SplashWarmGlow() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val glowSpecs = listOf(
            Triple(Offset(size.width * 0.12f, size.height * 0.10f), size.width * 0.26f, 0.30f),
            Triple(Offset(size.width * 0.28f, size.height * 0.20f), size.width * 0.22f, 0.12f),
            Triple(Offset(size.width * 0.52f, size.height * 0.08f), size.width * 0.34f, 0.14f),
            Triple(Offset(size.width * 0.82f, size.height * 0.11f), size.width * 0.28f, 0.28f),
            Triple(Offset(size.width * 0.92f, size.height * 0.23f), size.width * 0.18f, 0.10f),
        )
        glowSpecs.forEach { (center, radius, alpha) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD58C).copy(alpha = alpha),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    }
}

@Composable
private fun LauncherLogo(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    showShadow: Boolean = true,
) {
    val shadowElevation by animateDpAsState(
        targetValue = if (showShadow) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "launcher_logo_shadow"
    )
    Image(
        painter = painterResource(id = R.mipmap.fc_ic_logo_sq),
        contentDescription = null,
        modifier = modifier
            .size(110.dp)
            .shadow(elevation = shadowElevation, shape = RoundedCornerShape(24.dp)),
        contentScale = ContentScale.Fit,
        alpha = alpha
    )
}

@Composable
private fun PrivacyAgreementRow(
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val policyText = stringResource(id = R.string.fc_privacy_policy)
    val description = buildAnnotatedString {
        append(stringResource(id = R.string.fc_splash_privacy_prefix))
        append(" ")
        withStyle(style = SpanStyle(color = accentColor)) {
            append(policyText)
        }
    }

    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        PrivacyCheckedIcon(accentColor = accentColor)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = description,
            color = colorResource(id = R.color.t1),
            fontSize = 12.sp,
            fontFamily = FontFamily(Font(FrameworkR.font.inter_regular)),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PrivacyCheckedIcon(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.size(14.dp)
    ) {
        val strokeWidth = 1.6.dp.toPx()
        drawCircle(color = accentColor)
        val checkPath = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.69f)
            lineTo(size.width * 0.74f, size.height * 0.34f)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
        )
    }
}

@Composable
private fun RecentRecordCard(
    modifier: Modifier = Modifier,
    item: HistoryRecordItem?
) {
    if (item == null) {
        return
    }
    val context = LocalContext.current

    val typeName = when (item.getRecordType()) {
        HistoryRecordItem.RecordType.BLOOD_PRESSURE -> stringResource(id = R.string.fc_blood_pressure)
        HistoryRecordItem.RecordType.BLOOD_SUGAR -> stringResource(id = R.string.fc_blood_suger)
        HistoryRecordItem.RecordType.HEART_RATE -> stringResource(id = R.string.fc_heart_rate)
        HistoryRecordItem.RecordType.BMI_RECORD -> stringResource(id = R.string.fc_bmi)
        else -> ""
    }

    val title = stringResource(id = R.string.fc_last_measurement, typeName)
    val levelText = item.getLevel(context)
    val timeText = DateTimeUtils.formatDateTime(item.getRecordTime())
    val statusValue = item.getStatus(context)

    val statusText = when {
        statusValue.isNullOrBlank() -> null
        item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_PRESSURE -> {
            "${stringResource(id = R.string.fc_pulse)}:$statusValue"
        }
        item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_SUGAR -> {
            "${stringResource(id = R.string.fc_status)}:$statusValue"
        }
        else -> statusValue
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2E9))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = title,
                color = colorResource(id = R.color.t1),
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.getPrimaryValue(),
                        color = colorResource(id = R.color.t1),
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_black)),
                        fontWeight = FontWeight.Black
                    )
                    val secondary = item.getSecondaryValue()
                    if (!secondary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = secondary,
                            color = colorResource(id = R.color.t1),
                            fontSize = 18.sp,
                            fontFamily = FontFamily(Font(FrameworkR.font.inter_black)),
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.getUnit(),
                        color = colorResource(id = R.color.color_999),
                        fontSize = 14.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_regular))
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFFF7C3F))
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = levelText,
                        color = colorResource(id = R.color.t1),
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
                            color = colorResource(id = R.color.t1),
                            maxFontSize = 14.sp,
                            minFontSize = 6.sp,
                            fontFamily = FontFamily(Font(FrameworkR.font.inter_regular))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeText,
                        color = colorResource(id = R.color.color_999),
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
