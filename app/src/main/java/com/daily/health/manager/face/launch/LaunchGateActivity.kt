package com.daily.health.manager.face.launch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.daily.health.manager.App
import com.daily.health.manager.face.compose.LaunchContent
import com.daily.health.manager.R
import com.daily.health.manager.constants.KEY_FROM_SHORTCUT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_CONTENT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_FROM
import com.daily.health.manager.constants.LANDING_NOTIFICATION_TITLE
import com.daily.health.manager.constants.UNINSTALL
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.TrActivitySplashBinding
import com.daily.health.manager.face.act.GuideAct
import com.daily.health.manager.face.act.MainAct
import com.daily.health.manager.face.act.UninstallResenActivity
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


class LaunchGateActivity : BaseMVVMActivity<SplashViewModel, TrActivitySplashBinding>() {

    companion object {
        private const val TAG = "LaunchGateActivity"

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
                    startActivity(Intent(this@LaunchGateActivity, UninstallResenActivity::class.java))
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
                    GoogleMobileAdsConsentManager.getInstance(this@LaunchGateActivity)
                        .gatherConsent(this@LaunchGateActivity)
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
                    activity = this@LaunchGateActivity,
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
