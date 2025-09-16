package com.healthtracker.blood.suger.act

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.core.animation.addListener
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.MainActivity
import com.healthtracker.blood.suger.databinding.ActivitySplashBinding
import com.healthtracker.framework.SysBarUtils
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity: BaseMVVMActivity<BaseViewModel, ActivitySplashBinding>() {
    override fun createViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        playAnimations()
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
     * 动画完成回调
     */
    private fun onAnimationCompleted() {
        // 使用lifecycleScope确保只在Activity活跃时执行
        lifecycleScope.launch {
            delay(1000)
           startActivity<MainActivity>(isFinishSelf = true)
        }
    }

}