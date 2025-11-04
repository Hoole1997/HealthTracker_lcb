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
import com.healthtracker.blood.suger.BuildConfig
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.databinding.ActivitySplashBinding
import com.healthtracker.blood.suger.hasNewGuide
import com.healthtracker.blood.suger.receiver.NotificationActionReceiver
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.SysBarUtils
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.ext.openBrowser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import net.corekit.core.utils.ConfigRemoteManager
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.FullNativeAds
import net.corekit.monetize.ads.InterstitialAds
import net.corekit.monetize.ads.LaunchAds
import net.corekit.monetize.ads.log.AdLogger
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseMVVMActivity<BaseViewModel, ActivitySplashBinding>() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    private var isAdLoaded = false
    private val hasFullNativeShowing : Boolean
        get() = FullNativeAds.getInstance().checkAdShowing()
    private val hasInterstitialShowing : Boolean
        get() = InterstitialAds.getInstance().checkAdShowing()
    // 从通知传递过来的动作参数
    private val notificationAction: String? by lazy {
        intent.getStringExtra(com.healthtracker.blood.suger.service.HealthServiceConstants.EXTRA_NOTIFICATION_ACTION)
    }

    // 状态机负责协调动画、权限、前后台状态与跳转
    private val stateMachine by lazy {
        SplashStateMachine(
            scope = lifecycleScope,
            onNavigate = {

                if(ActivityUtils.isActivityExistsInStack(MainActivity::class.java)){
                    finish()
                    return@SplashStateMachine
                }
                // 判断应该跳转到哪个页面
                val targetActivity = if(hasNewGuide()){
                    MainActivity::class.java
                } else {
                    GuideActivity::class.java
                }

                // 创建Intent并传递通知参数
                val targetIntent = Intent(this, targetActivity).apply {
                    notificationAction?.let { action ->
                        putExtra(HealthServiceConstants.EXTRA_NOTIFICATION_ACTION, action)
                        if(BuildState.debug) "Passing notification action to ${targetActivity.simpleName}: $action".logd(TAG)
                    }
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

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.tvPrivacy.clickWithDuration {
            openBrowser(this, BuildConfig.PRIVACY_POLICY)
        }
        lifecycleScope.launch {
            val adJob = async {
                initializeAndShowAd()
            }

            val timeout = ConfigRemoteManager.getInt("splash_time_out",10)
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

            if(timeoutTriggered){
                "触发超时".logd(TAG)
                val hasAdLoaded = isAdLoaded
                if (!hasAdLoaded && !hasFullNativeShowing && !hasInterstitialShowing) {
                    // 没有任何广告，执行继续流程
                    if(BuildState.debug)  Log.d(TAG, "${timeout}秒超时兜底：无广告，执行继续流程")
                } else {
                    // 有广告加载或显示，继续等待广告完成
                    if(BuildState.debug)  Log.d(TAG, "${timeout}秒超时兜底：有广告(loaded=$hasAdLoaded, fullNative=$hasFullNativeShowing, interstitial=$hasInterstitialShowing)，等待广告完成")
                    adJob.await()
                }
            }else{
                "非超时触发".logd(TAG)
            }

            stateMachine.onAdCompleted()
        }
        playAnimations()
        checkNotificationPermission()
        checkNotificationOpen()
    }

    private fun checkNotificationOpen() {
       val notificationId = intent.getIntExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID,-1)
        if (notificationId == -1) {
            if(BuildState.debug) "Invalid notification ID: $notificationId".logw(TAG)
            return
        }
        val actionType = intent.getStringExtra(NotificationActionReceiver.EXTRA_ACTION_VALUE)
        if(BuildState.debug) "checkNotificationOpen actionType = $actionType".logd(TAG)
        sendBroadcast(Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_CLICKED
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID,notificationId)
        })


    }

    /**
     * 初始化 AdMob 并显示开屏广告
     * @return 广告是否加载成功
     */
    private suspend fun initializeAndShowAd(): Boolean {
        try {

//            // 初始化 AdMob SDK

            if(BuildState.debug) "AdMob SDK 初始化成功，准备显示开屏广告".logd(TAG)

            // 显示开屏广告
            val adResult = LaunchAds.getInstance().displayAd(
                activity = this,
                onLoaded = { isSuccess ->
                    isAdLoaded = isSuccess
                }
            )

            if (adResult is AdResult.Success) {
                if(BuildState.debug) "开屏广告关闭".logd(TAG)
                return true
            } else {
                if(BuildState.debug)  "开屏广告显示失败: ${(adResult as? AdResult.Failure)?.error?.message}".logd(TAG)
                return false
            }
        } catch (e: Exception) {
            if(BuildState.debug) "广告初始化或显示异常 e:$e".loge(TAG)
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
     * 检查通知权限
     */
    private  fun checkNotificationPermission() {
        "Checking notification permission...".logd(TAG)

        val notificationStatus = permissionManager.checkNotificationPermission()

        when (notificationStatus) {
            PermissionManager.Companion.PermissionStatus.GRANTED,
            PermissionManager.Companion.PermissionStatus.NOT_REQUIRED -> {
                if(BuildState.debug) "Notification permission already granted or not required".logd(TAG)
                onPermissionCheckCompleted()
            }

            PermissionManager.Companion.PermissionStatus.DENIED -> {
                if(BuildState.debug) "Notification permission denied, requesting...".logd(TAG)
                permissionManager.requestNotificationPermission(this)
            }
        }
    }

    /**
     * 权限检查完成回调
     */
    private fun onPermissionCheckCompleted() {
        stateMachine.onPermissionCheckCompleted()
    }

    /**
     * 动画完成回调
     */
    private fun onAnimationCompleted() {
        if(BuildState.debug)  "动画执行完成".logd(TAG)
        stateMachine.onAnimationCompleted()
    }

    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val handled =
            permissionManager.handlePermissionResult(requestCode, permissions, grantResults)
        if (handled && requestCode == PermissionManager.REQUEST_CODE_NOTIFICATION) {
            // 无论用户是否授权，都完成权限检查流程
            val currentStatus = permissionManager.checkNotificationPermission()
            if (currentStatus == PermissionManager.Companion.PermissionStatus.GRANTED) {
                if(BuildState.debug) "Notification permission granted by user".logd(TAG)
            } else {
                if(BuildState.debug) "Notification permission denied by user, but continue to main activity".logd(TAG)
            }
            onPermissionCheckCompleted()
        }
    }

    /**
     * 启动页状态机，统一管理动画、权限与导航状态
     */
    private inner class SplashStateMachine(
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
            if(BuildState.debug) "Animation completed".logd(TAG)
            tryNavigate()

        }

        fun onPermissionCheckCompleted() {
            if (permissionDone) {
                return
            }
            permissionDone = true
            if(BuildState.debug) "Permission check completed".logd(TAG)
            tryNavigate()

        }

        fun onAdCompleted() {
            if (adDone) {
                return
            }
            adDone = true
            if(BuildState.debug) "Ad completed".logd(TAG)
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

      private  fun tryNavigate() {
            if (hasNavigated) {
                if(BuildState.debug)  "Already navigated, ignore further requests".logd(TAG)
                return
            }

            if (!(animationDone && permissionDone && adDone )) {
                if(BuildState.debug)  "Waiting for completion - Animation: $animationDone, Permission: $permissionDone".logd(
                    TAG
                )
                return
            }

            if (navigationJob?.isActive == true) {
                if(BuildState.debug)  "Navigation coroutine is already running".logd(TAG)
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
}
