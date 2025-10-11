package com.healthtracker.blood.suger.ui.act

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.core.animation.addListener
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.BuildConfig
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.databinding.ActivitySplashBinding
import com.healthtracker.blood.suger.isNewUser
import com.healthtracker.framework.SysBarUtils
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.openBrowser
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseMVVMActivity<BaseViewModel, ActivitySplashBinding>() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    // 状态机负责协调动画、权限、前后台状态与跳转
    private val stateMachine by lazy {
        SplashStateMachine(
            scope = lifecycleScope,
            onNavigate = {
                startActivity<MainActivity>(isFinishSelf = true)
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
        playAnimations()
        checkNotificationPermission()
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
    private fun checkNotificationPermission() {
        "Checking notification permission...".logd(TAG)

        val notificationStatus = permissionManager.checkNotificationPermission()

        when (notificationStatus) {
            PermissionManager.Companion.PermissionStatus.GRANTED,
            PermissionManager.Companion.PermissionStatus.NOT_REQUIRED -> {
                "Notification permission already granted or not required".logd(TAG)
                onPermissionCheckCompleted()
            }

            PermissionManager.Companion.PermissionStatus.DENIED -> {
                "Notification permission denied, requesting...".logd(TAG)
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
                "Notification permission granted by user".logd(TAG)
            } else {
                "Notification permission denied by user, but continue to main activity".logd(TAG)
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
        private var isForeground = true
        private var pendingForegroundNavigation = false
        private var hasNavigated = false
        private var navigationJob: Job? = null

        fun onAnimationCompleted() {
            if (animationDone) {
                return
            }
            animationDone = true
            "Animation completed".logd(TAG)
            tryNavigate()
        }

        fun onPermissionCheckCompleted() {
            if (permissionDone) {
                return
            }
            permissionDone = true
            "Permission check completed".logd(TAG)
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
                "Already navigated, ignore further requests".logd(TAG)
                return
            }

            if (!(animationDone && permissionDone)) {
                "Waiting for completion - Animation: $animationDone, Permission: $permissionDone".logd(
                    TAG
                )
                return
            }

            if (navigationJob?.isActive == true) {
                "Navigation coroutine is already running".logd(TAG)
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
