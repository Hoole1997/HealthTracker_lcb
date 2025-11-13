package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.dao.HydrateReminderDao
import com.healthtracker.blood.suger.data.entity.HydrateReminder
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.data.repository.AlarmRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.alarm.AlarmNotificationManager
import com.healthtracker.blood.suger.alarm.AlarmScheduler
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HydrateSettingViewModel @Inject constructor(
    private val hydrateReminderDao: HydrateReminderDao,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val alarmNotificationManager: AlarmNotificationManager
) : BaseViewModel() {

    /**
     * 当前所有提醒时间（"HH:MM" 字符串列表），按小时与分钟升序
     */
    val reminderTimes = hydrateReminderDao.getAll()
        .map { list ->
            list.map { DateTimeUtils.formatTimeComponents(it.hour, it.minute) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** 添加提醒时间 */
    fun addReminder(hour: Int, minute: Int) {
        viewModelScope.launch {
            // 先插入 HydrateReminder（忽略重复）
            val insertedId = hydrateReminderDao.insert(HydrateReminder(hour = hour, minute = minute))

            // 仅当成功插入时，新增 AlarmRecord 并调度系统闹钟
            if (insertedId > 0) {
                val record = AlarmRecord.createHydrationReminder(hour, minute, AlarmRecord.REPEAT_DAILY)
                val alarmId = alarmRepository.insertAlarmRecord(record)
                val insertedRecord = record.copy(id = alarmId)
                alarmScheduler.scheduleAlarm(insertedRecord)
            }
        }
    }

    /** 删除提醒时间 */
    fun deleteReminder(hour: Int, minute: Int) {
        viewModelScope.launch {
            // 删除 HydrateReminder 本身
            hydrateReminderDao.deleteByTime(hour, minute)

            // 查找对应的饮水 AlarmRecord，取消系统闹钟并软删除记录
            val records = alarmRepository.getRecordsByTypeAndTime(AlarmRecord.TYPE_HYDRATION, hour, minute)
            records.forEach { record ->
                // 取消系统级闹钟
                alarmScheduler.cancelAlarm(record)
                // 取消已展示的通知（如果有）
                alarmNotificationManager.cancelAlarmNotification(record)
                // 软删除记录以维持一致性
                alarmRepository.softDeleteRecord(record.id)
            }
        }
    }
}