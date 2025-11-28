package com.healthtracker.blood.suger.ui.act

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.alarm.AlarmNotificationManager
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.constants.KEY_STEP_COUNT_GOLE
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityFsiNotificationBinding
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.service.HealthServiceConstants.EXTRA_NOTIFICATION_ACTION
import com.healthtracker.blood.suger.ui.viewmodel.FsiViewModel
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.NumberFormatter
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.hasOreo
import com.ncorti.slidetoact.SlideToActView
import dagger.hilt.android.AndroidEntryPoint
import net.corekit.core.report.ReportDataManager
import java.sql.Date
@AndroidEntryPoint
class FsiNotificationActivity: BaseMVVMActivity<FsiViewModel, ActivityFsiNotificationBinding>() {
    companion object{
        const val EXTRA_PUSH_MESSAGE = "extra_push_message"
        const val EXTRA_ALARM_RECORD = "extra_alarm_record"
        private const val TAG = "FsiNotificationActivity"
    }
    override fun createViewBinding() = ActivityFsiNotificationBinding.inflate(layoutInflater)

    override fun getVMModelClass() = FsiViewModel::class.java

    override fun isFullscreenWithNavigationBar() = true
    override fun isFullscreen() = true
    override fun initView(savedInstanceState: Bundle?) {
        ReportDataManager.reportData("full_screen_show")
        registerTimeChangeReceiver()  // 注册时间变更监听
        setupFullScreenMode()
        onTimeChanged()
        with(mViewBind){
            root.clickWithDuration {
                ReportDataManager.reportData("full_screen_click")
                handleClick()
            }
            val pushMessage = intent.getSerializableExtra(EXTRA_PUSH_MESSAGE)
            pushMessage?.let {
                if(it is PushMessage){
                    tvBtn.text = it.buttonText
                    tvTitle.text = it.title
                    tvDes.text = it.desc
                    if(it.id == AlarmNotificationManager.PUSH_ALARM){
                        ivIcon.setImageResource(R.mipmap.ic_fsi_remind)
                    }else{
                        ivIcon.setImageResource(when(it.iconType){
                            1 -> R.drawable.ic_fis_home
                            2 -> R.mipmap.ic_blood_suger
                            3 -> R.mipmap.ic_blood_pressure
                            4 -> R.mipmap.ic_cholesterol
                            5 -> R.mipmap.ic_bmi
                            6 -> R.mipmap.ic_heart
                            9 -> R.mipmap.ic_home_cup
                            10 -> R.mipmap.ic_home_step
                            else -> R.drawable.ic_fis_home
                        })
                    }

                }
            }
            slideView.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener{
                override fun onSlideComplete(view: SlideToActView) {
                    ReportDataManager.reportData("full_screen_click",mapOf("type" to "slide"))
                    handleClick()
                }

            }

            llHydrate.clickWithDuration {
                intent.putExtra(EXTRA_NOTIFICATION_ACTION,HealthServiceConstants.ACTION_VALUE_HYDRATION)
                ReportDataManager.reportData("full_screen_click",mapOf("type" to "hydrate"))
                handleClick()
            }

            llStep.clickWithDuration {
                intent.putExtra(EXTRA_NOTIFICATION_ACTION,HealthServiceConstants.ACTION_VALUE_STEPS)
                ReportDataManager.reportData("full_screen_click",mapOf("type" to "step"))
                handleClick()
            }



        }
    }

    private fun handleClick(){
        unlockAndOpen()
        finishAndRemoveTask()
    }

    override fun createObserver() {
        super.createObserver()
        this.collect(mViewModel.todayStatFlow) { stat ->
            stat?.let {
                mViewBind.tvStepCount.text = it.steps.toString()
                mViewBind.tvKcal.text =  NumberFormatter.formatNumber(it.kcal, LanguageUtils.getAppLocale(App.INSTANCE),1)
            }
        }
        
        // 观察饮水数据状态
        this.collect(mViewModel.hydrateDisplayState) { state ->
            with(mViewBind) {
                // 根据单位换算显示值
                val currentValue = when (state.cupUnit) {
                    com.healthtracker.blood.suger.config.HydrateSettingManager.CupUnit.ML -> state.currentIntakeMl
                    com.healthtracker.blood.suger.config.HydrateSettingManager.CupUnit.FL_OZ -> 
                        com.healthtracker.blood.suger.config.HydrateSettingManager.fromMl(state.currentIntakeMl, state.cupUnit)
                }
                
                val targetValue = when (state.cupUnit) {
                    com.healthtracker.blood.suger.config.HydrateSettingManager.CupUnit.ML -> state.targetIntakeMl
                    com.healthtracker.blood.suger.config.HydrateSettingManager.CupUnit.FL_OZ -> 
                        com.healthtracker.blood.suger.config.HydrateSettingManager.fromMl(state.targetIntakeMl, state.cupUnit)
                }
                
                val unitLabel = if (state.cupUnit == com.healthtracker.blood.suger.config.HydrateSettingManager.CupUnit.FL_OZ) {
                    getString(R.string.fl_oz)
                } else {
                    getString(R.string.unit_ml)
                }
                
                // 更新 UI
                tvDrinkValue.text = currentValue.toString()
                tvDrinkGoal.text = "/$targetValue$unitLabel"
                
                // 根据达标状态切换图标
                ivDrinkProgress.setImageResource(
                    if (state.isGoalReached) R.drawable.ic_fsi_hydrate_full 
                    else R.drawable.ic_fsi_hydrate_not_full
                )
                
                ivReachGoal.setImageResource(
                    if (state.isGoalReached) R.drawable.ic_fsi_reach_goal 
                    else R.drawable.ic_fsi_not_reach_goal
                )
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(timeChangeReceiver)
        } catch (e: IllegalArgumentException) {
            // 防止重复取消注册
            "取消注册时间广播失败: ${e.message}".logd(TAG)
        }
    }

    /**
     * 系统时间被手动更改
     */
    private fun onTimeChanged() {
        with(mViewBind){
            val current = DateTimeUtils.now()
            val formatDate = DateTimeUtils.formatDate(current)
            if(BuildState.debug) "当前日期：$formatDate".logd(TAG)
            tvDate.text = formatDate
            val formatTime = DateTimeUtils.formatTime(current)
            if(BuildState.debug) "当前时间：$formatTime".logd(TAG)
            tvTime.text = formatTime
        }
    }

    /**
     * 注册时间变更广播接收器
     */
    private fun registerTimeChangeReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)  // 可选：每分钟触发
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要指定 RECEIVER_NOT_EXPORTED
            registerReceiver(timeChangeReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(timeChangeReceiver, filter)
        }
    }

    // 时间变更广播接收器
    private val timeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_TIME_CHANGED -> {
                    if(BuildState.debug) "系统时间被手动更改".logd(TAG)
                    onTimeChanged()
                }
                Intent.ACTION_DATE_CHANGED -> {
                    if(BuildState.debug) "日期变更（跨天）".logd(TAG)
                    onTimeChanged()
                }
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    if(BuildState.debug) "时区变更".logd(TAG)
                    onTimeChanged()
                }
                Intent.ACTION_TIME_TICK -> {
                    if(BuildState.debug) "每分钟触发".logd(TAG)
                    onTimeChanged()
                }
            }
        }
    }

}