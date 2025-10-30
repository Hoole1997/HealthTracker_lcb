package com.healthtracker.blood.suger.ui.act

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.os.bundleOf
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.databinding.ActivityFsiNotificationBinding
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.util.hasOreo

class FsiNotificationActivity: BaseMVVMActivity<BaseViewModel, ActivityFsiNotificationBinding>() {
    companion object{
        const val EXTRA_PUSH_MESSAGE = "extra_push_message"
    }
    override fun createViewBinding() = ActivityFsiNotificationBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun isFullscreenWithNavigationBar() = true
    override fun isFullscreen() = true
    override fun initView(savedInstanceState: Bundle?) {
        setupFullScreenMode()
        with(mViewBind){
            tvBtn.clickWithDuration {
                unlockAndOpen()
                finishAndRemoveTask()
            }
            val pushMessage = intent.getSerializableExtra(EXTRA_PUSH_MESSAGE)
            pushMessage?.let {
                if(it is PushMessage){
                    tvBtn.text = it.buttonText
                    tvTitle.text = it.title
                    tvDes.text = it.desc
                    ivIcon.setImageResource(when(it.iconType){
                        1 -> R.drawable.ic_fis_home
                        2 -> R.drawable.ic_fsi_bs
                        3 -> R.drawable.ic_fsi_bp
                        4 -> R.drawable.ic_fsi_cholesterol
                        5 -> R.drawable.ic_fsi_bmi
                        6 -> R.drawable.ic_fsi_hr
                        else -> R.drawable.ic_fis_home
                    })
                }
            }

        }

    }

    /**
     * 设置全屏模式和锁屏显示
     */
    private fun setupFullScreenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // 全屏显示
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun unlockAndOpen(vararg params: Pair<String, Any?>) {
        // 1. 解锁屏幕
        if (hasOreo()) {
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            if (keyguardManager?.isKeyguardLocked == true) {
                keyguardManager.requestDismissKeyguard(this, null)
            }
        }

        // 2. 跳转到 SplashActivity
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.extras?.let { putExtras(it) }
        }
        startActivity(intent)
    }

    override fun getStatusBarColor() = com.healthtracker.framework.R.color.transparent
}