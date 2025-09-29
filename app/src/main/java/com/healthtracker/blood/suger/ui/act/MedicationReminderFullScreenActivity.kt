package com.healthtracker.blood.suger.ui.act

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.os.bundleOf
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityMedicationReminderFullscreenBinding
import com.healthtracker.blood.suger.ui.viewmodel.MedicationReminderFullScreenViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.visible
import com.healthtracker.framework.util.hasOreo
import dagger.hilt.android.AndroidEntryPoint

/**
 * 服药提醒全屏通知Activity
 *
 * 用于显示全屏的服药提醒，确保用户不会错过重要的服药时间
 * 支持锁屏状态下的显示和用户交互
 */
@AndroidEntryPoint
class MedicationReminderFullScreenActivity : BaseMVVMActivity<MedicationReminderFullScreenViewModel, ActivityMedicationReminderFullscreenBinding>() {


    companion object {
        private const val TAG = "MedicationReminderFullScreenActivity"
        private const val DEFAULT_SNOOZE_MINUTES = 5

        // Intent extras
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_NOTES = "notes"
        const val EXTRA_REMINDER_TIME = "reminder_time"
        const val EXTRA_REMINDER_ID = "reminder_id"

        /**
         * 启动全屏服药提醒Activity
         */
        fun start(
            context: Context,
            medicationName: String,
            dosage: String = "",
            notes: String = "",
            reminderTime: String = "",
            reminderId: Long = -1L
        ) {
            val intent = Intent(context, MedicationReminderFullScreenActivity::class.java).apply {
                putExtra(EXTRA_MEDICATION_NAME, medicationName)
                putExtra(EXTRA_DOSAGE, dosage)
                putExtra(EXTRA_NOTES, notes)
                putExtra(EXTRA_REMINDER_TIME, reminderTime)
                putExtra(EXTRA_REMINDER_ID, reminderId)

                // 全屏Intent标记
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }
    }

    override fun createViewBinding() = ActivityMedicationReminderFullscreenBinding.inflate(layoutInflater)

    override fun getVMModelClass() = MedicationReminderFullScreenViewModel::class.java


    override fun isFullscreenWithNavigationBar() = true
    override fun isFullscreen() = true

    override fun initView(savedInstanceState: Bundle?) {
        setupFullScreenMode()
        initializeFromIntent()
        setupViews()
        observeViewModel()
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

    /**
     * 从Intent初始化数据
     */
    private fun initializeFromIntent() {
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: ""
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
        val notes = intent.getStringExtra(EXTRA_NOTES) ?: ""
        val reminderTime = intent.getStringExtra(EXTRA_REMINDER_TIME) ?: ""
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

        "Initializing FSI reminder: $medicationName at $reminderTime,$notes".logd(TAG)

        mViewModel.initializeMedicationReminder(
            medicationName = medicationName,
            dosage = dosage,
            notes = notes,
            reminderTime = reminderTime,
            reminderId = reminderId
        )
    }

    /**
     * 设置视图和点击事件
     */
    private fun setupViews() {
        mViewBind.apply {
            // 当前时间显示
            val currentTime = DateTimeUtils.formatTime(DateTimeUtils.now())
            tvCurrentTime.text = getString(R.string.now_time_temp,currentTime)

            // 按钮点击事件
            btnTaken.clickWithDuration {
                "User marked medication as taken".logd(TAG)
                unlockAndOpen("FROM_FSI" to true)
                finishAndRemoveTask()
            }

            btnClose.clickWithDuration {
                "User chose to snooze medication reminder".logd(TAG)
                finishAndRemoveTask()
            }
        }
    }

    /**
     * 观察ViewModel状态变化
     */
    private fun observeViewModel() {
        collectLatest(mViewModel.medicationInfo) { info ->
            with(mViewBind) {
                if (info.notes.isBlank()) {
                    tvNotes.gone()
                } else {
                    tvNotes.visible()
                    tvNotes.text = getString(R.string.meds_notes_temp, info.notes)
                }

                if (info.reminderTime.isBlank()) {
                    tvReminderTime.gone()
                } else {
                    tvReminderTime.visible()
                    tvReminderTime.text = getString(R.string.every_day_temp, info.reminderTime)
                }

                if (info.medicationName.isBlank()) {
                    tvMedicationName.gone()
                } else {
                    tvMedicationName.visible()
                    tvMedicationName.text = getString(R.string.medication_name_temp, info.medicationName)
                }
            }

        }

        collectLatest(mViewModel.snoozeOptions) { options ->
            // 处理延迟选项更新
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        initializeFromIntent()
    }

    override fun shouldDisableBackPressed() = true

    /**
     * Activity生命周期管理
     */
    override fun onDestroy() {
        super.onDestroy()
        "FSI reminder activity destroyed".logd(TAG)
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
            putExtras(bundleOf(*params))
        }
        startActivity(intent)
    }

    override fun getStatusBarColor() = com.healthtracker.framework.R.color.transparent
}
