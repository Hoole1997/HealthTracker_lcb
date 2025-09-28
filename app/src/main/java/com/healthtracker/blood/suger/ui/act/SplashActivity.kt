package com.healthtracker.blood.suger.ui.act

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.core.animation.addListener
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.databinding.ActivitySplashBinding
import com.healthtracker.framework.SysBarUtils
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity: BaseMVVMActivity<BaseViewModel, ActivitySplashBinding>() {

    companion object {
        private const val TAG = "SplashActivity"
    }

    @Inject
    lateinit var permissionManager: PermissionManager

    // 标志位：动画是否完成
    private var isAnimationCompleted = false
    // 标志位：权限检查是否完成
    private var isPermissionCheckCompleted = false

    override fun createViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        playAnimations()
        checkNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        SysBarUtils.hideNavigationBar(this)
    }

    override fun isFullscreen() = true

    /**
     * 播放所有启动动画
     */
    private fun playAnimations() {
        with(mViewBind){
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
            animatorSet.addListener(onEnd = {
                // 动画结束后标记可以进行导航
                onAnimationCompleted()
            })

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
        isPermissionCheckCompleted = true
        "Permission check completed".logd(TAG)
        checkAndNavigateToMainActivity()
    }

    /**
     * 动画完成回调
     */
    private fun onAnimationCompleted() {
        isAnimationCompleted = true
        "Animation completed".logd(TAG)
        checkAndNavigateToMainActivity()
    }

    /**
     * 检查是否可以跳转到主页面
     * 只有当动画完成且权限检查完成时才跳转
     */
    private fun checkAndNavigateToMainActivity() {
        if (isAnimationCompleted && isPermissionCheckCompleted) {
            "Both animation and permission check completed, navigating to MainActivity".logd(TAG)
            // 使用lifecycleScope确保只在Activity活跃时执行
            lifecycleScope.launch {
                delay(500) // 稍微延迟一下，提供更好的用户体验
                startActivity<MainActivity>(isFinishSelf = true)
            }
        } else {
            "Waiting for completion - Animation: $isAnimationCompleted, Permission: $isPermissionCheckCompleted".logd(TAG)
        }
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

        val handled = permissionManager.handlePermissionResult(requestCode, permissions, grantResults)
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

}