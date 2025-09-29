package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.alarm.AlarmNotificationManager
import com.healthtracker.blood.suger.data.repository.MedicineReminderRepository
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 服药提醒全屏通知ViewModel
 *
 * 管理全屏服药提醒的状态和业务逻辑
 */
@HiltViewModel
class MedicationReminderFullScreenViewModel @Inject constructor(
    private val medicineReminderRepository: MedicineReminderRepository,
    private val alarmNotificationManager: AlarmNotificationManager
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

        if (currentInfo.reminderId < 0) {
            "Invalid reminder id, skip record update".logw(TAG)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                medicineReminderRepository.recordMedication(currentInfo.reminderId)
            } catch (e: Exception) {
                "Failed to record medication taken: ${e.message}".loge(TAG)
            }
        }

        alarmNotificationManager.cancelMedicationNotification(currentInfo.reminderId)
    }

    /**
     * 延迟提醒
     */
    fun scheduleSnooze(minutes: Int) {
        val currentInfo = _medicationInfo.value
        "Scheduling snooze for ${currentInfo.medicationName}: $minutes minutes".logd(TAG)

        if (minutes <= 0) {
            "Snooze duration must be positive".logw(TAG)
            return
        }

        if (currentInfo.reminderId < 0) {
            "Invalid reminder id, cannot schedule snooze".logw(TAG)
            return
        }

        alarmNotificationManager.cancelMedicationNotification(currentInfo.reminderId)
        alarmNotificationManager.scheduleMedicationSnooze(
            medicationName = currentInfo.medicationName,
            dosage = currentInfo.dosage,
            notes = currentInfo.notes,
            reminderTime = currentInfo.reminderTime,
            reminderId = currentInfo.reminderId,
            delayMinutes = minutes
        )
    }

    /**
     * 忽略提醒
     */
    fun dismissReminder() {
        val currentInfo = _medicationInfo.value
        "Dismissing medication reminder: ${currentInfo.medicationName}".logd(TAG)

        if (currentInfo.reminderId < 0) {
            return
        }

        alarmNotificationManager.cancelMedicationNotification(currentInfo.reminderId)

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
