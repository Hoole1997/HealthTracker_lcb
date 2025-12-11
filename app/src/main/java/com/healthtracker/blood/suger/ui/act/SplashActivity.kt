package com.healthtracker.blood.suger.ui.act

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.animation.addListener
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.BuildConfig
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.constants.LANDING_NOTIFICATION_CONTENT
import com.healthtracker.blood.suger.constants.LANDING_NOTIFICATION_FROM
import com.healthtracker.blood.suger.constants.LANDING_NOTIFICATION_TITLE
import com.healthtracker.blood.suger.databinding.ActivitySplashBinding
import com.healthtracker.blood.suger.hasNewGuide
import com.healthtracker.blood.suger.receiver.NotificationActionReceiver
import com.healthtracker.blood.suger.util.logEvent
import com.healthtracker.blood.suger.utils.isAdPage
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.SysBarUtils
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.ext.openBrowser
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.SpUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import net.corekit.core.report.ReportDataManager
import net.corekit.core.utils.ConfigRemoteManager
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.FullNativeAds
import net.corekit.monetize.ads.InterstitialAds
import net.corekit.monetize.ads.LaunchAds
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.log.AdLogger
import javax.inject.Inject
import kotlin.math.ceil

@AndroidEntryPoint
class SplashActivity : BaseMVVMActivity<BaseViewModel, ActivitySplashBinding>() {

    companion object {
        private const val TAG = "SplashActivity"

    }

    private var isAdLoaded = false
    private val hasFullNativeShowing: Boolean
        get() = FullNativeAds.getInstance().checkAdShowing()
    private val hasInterstitialShowing: Boolean
        get() = InterstitialAds.getInstance().checkAdShowing()


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
                val targetActivity = if (LanguageUtils.getSavedLanguage().isEmpty()) {
                    LanguageActivity::class.java
                } else {
                    if (hasNewGuide() || !AdConfigManager.showNewGuide()) {
                        MainActivity::class.java
                    } else {
                        GuideActivity::class.java
                    }
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

    @Inject
    lateinit var permissionManager: PermissionManager

    override fun createViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java
    private var launchTime = 0L
    override fun initView(savedInstanceState: Bundle?) {
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
            mViewBind.tvPrivacy.click {
                openBrowser(this@SplashActivity, BuildConfig.PRIVACY_POLICY)
            }

            playAnimations()
            permissionManager.checkNotificationPermission(this@SplashActivity){
                onPermissionCheckCompleted()
            }
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
     * @return 广告是否加载成功
     */
    private suspend fun initializeAndShowAd(): Boolean {
        try {

//            // 初始化 AdMob SDK

            if (BuildState.debug) "AdMob SDK 初始化成功，准备显示开屏广告".logd(TAG)

            // 显示开屏广告
            val adResult = LaunchAds.getInstance().displayAd(
                activity = this,
                onLoaded = { isSuccess ->
                    isAdLoaded = isSuccess
                }
            )

            if (adResult is AdResult.Success) {
                if (BuildState.debug) "开屏广告关闭".logd(TAG)
                return true
            } else {
                if (BuildState.debug) "开屏广告显示失败: ${(adResult as? AdResult.Failure)?.error?.message}".logd(
                    TAG
                )
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
        with(mViewBind) {
            // 创建组合动画
            val animatorSet = AnimatorSet().apply {
                startDelay = 200 // 延迟200毫秒开始动画
                duration = 1000 // 动画持续时间350毫秒
            }

            // 创建各个视图的动画
            val logoAnimator = createAlphaAnimator(ivLogo)
            val nameAnimator = createAlphaAnimator(tvAppName)


            // 设置动画同时播放
            animatorSet.playTogether(logoAnimator, nameAnimator)

            // 添加动画监听器
            animatorSet.addListener(
                onEnd = {
                    // 动画结束后标记可以进行导航
                    onAnimationCompleted()
                },
                onCancel = {
                    // 部分设备上动画可能被系统取消，这里兜底仍然推进流程
                    onAnimationCompleted()
                }
            )

            // 开始动画
            animatorSet.start()
        }
    }

    /**
     * 创建淡入动画
     * 保持与原版本完全相同的动画参数
     */
    private fun createAlphaAnimator(view: View): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "alpha", 0f, 1.0f)
    }



    /**
     * 权限检查完成回调
     */
    private fun onPermissionCheckCompleted() {
        if(BuildState.debug) "启动页通知授权流程完成".logd(PermissionManager.TAG)
        stateMachine.onPermissionCheckCompleted()

    }

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
            if(BuildState.debug) "设置开屏拦截等待结束".logd(PermissionManager.TAG)
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
