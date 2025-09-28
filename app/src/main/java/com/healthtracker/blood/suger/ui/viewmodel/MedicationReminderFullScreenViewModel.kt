package com.healthtracker.blood.suger.ui.viewmodel

import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 服药提醒全屏通知ViewModel
 *
 * 管理全屏服药提醒的状态和业务逻辑
 */
@HiltViewModel
class MedicationReminderFullScreenViewModel @Inject constructor(
    // TODO: 注入必要的Repository和Service
) : BaseViewModel() {

    companion object {
        private const val TAG = "MedicationReminderFSIViewModel"
    }

    /**
     * 药物信息数据类
     */
    data class MedicationInfo(
        val medicationName: String = "",
        val dosage: String = "",
        val notes: String = "",
        val reminderTime: String = "",
        val reminderId: Long = -1L
    )

    /**
     * 延迟选项数据类
     */
    data class SnoozeOption(
        val minutes: Int,
        val displayText: String
    )

    // 药物信息状态
    private val _medicationInfo = MutableStateFlow(MedicationInfo())
    val medicationInfo: StateFlow<MedicationInfo> = _medicationInfo.asStateFlow()

    // 延迟选项
    private val _snoozeOptions = MutableStateFlow(getDefaultSnoozeOptions())
    val snoozeOptions: StateFlow<List<SnoozeOption>> = _snoozeOptions.asStateFlow()

    /**
     * 初始化药物提醒信息
     */
    fun initializeMedicationReminder(
        medicationName: String,
        dosage: String,
        notes: String,
        reminderTime: String,
        reminderId: Long
    ) {
        "Initializing medication reminder: $medicationName".logd(TAG)

        _medicationInfo.value = MedicationInfo(
            medicationName = medicationName,
            dosage = dosage,
            notes = notes,
            reminderTime = reminderTime,
            reminderId = reminderId
        )
    }

    /**
     * 标记药物已服用
     */
    fun markMedicationTaken() {
        val currentInfo = _medicationInfo.value
        "Marking medication as taken: ${currentInfo.medicationName} (ID: ${currentInfo.reminderId})".logd(TAG)

        // TODO: 调用Repository更新服药记录
        // medicationRepository.markAsTaken(currentInfo.reminderId, System.currentTimeMillis())

        // TODO: 取消相关的通知和闹钟
        // notificationHelper.cancelMedicationNotification(currentInfo.reminderId)
    }

    /**
     * 延迟提醒
     */
    fun scheduleSnooze(minutes: Int) {
        val currentInfo = _medicationInfo.value
        "Scheduling snooze for ${currentInfo.medicationName}: $minutes minutes".logd(TAG)

        // TODO: 重新安排闹钟
        // alarmScheduler.scheduleSnooze(currentInfo.reminderId, minutes)

        // TODO: 取消当前通知
        // notificationHelper.cancelMedicationNotification(currentInfo.reminderId)
    }

    /**
     * 忽略提醒
     */
    fun dismissReminder() {
        val currentInfo = _medicationInfo.value
        "Dismissing medication reminder: ${currentInfo.medicationName}".logd(TAG)

        // TODO: 取消当前通知，但不影响后续提醒
        // notificationHelper.cancelMedicationNotification(currentInfo.reminderId)

        // TODO: 记录用户行为（可选）
        // userActionRepository.recordDismissal(currentInfo.reminderId, System.currentTimeMillis())
    }

    /**
     * 获取默认延迟选项
     */
    private fun getDefaultSnoozeOptions(): List<SnoozeOption> {
        return listOf(
            SnoozeOption(5, "5 minutes later"),
            SnoozeOption(10, "10 minutes later"),
            SnoozeOption(15, "15 minutes later"),
            SnoozeOption(30, "30 minutes later")
        )
    }
}