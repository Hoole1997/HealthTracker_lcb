package com.daily.health.manager.face.viewmodel

import androidx.lifecycle.viewModelScope
import com.daily.health.manager.data.dao.HydrateReminderDao
import com.daily.health.manager.data.repository.HydrateRepository
import com.daily.health.manager.data.entity.HydrateReminder
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.data.repository.AlarmRepository
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.alarm.AlarmNotificationManager
import com.daily.health.manager.alarm.AlarmScheduler
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HydrateSettingViewModel(
    private val hydrateReminderDao: HydrateReminderDao,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val alarmNotificationManager: AlarmNotificationManager,
    private val hydrateRepository: HydrateRepository
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

    /** 当前所有 HydrateReminder 列表（包含 enabled 状态），用于UI开关同步 */
    val reminders = hydrateReminderDao.getAll()
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

    /** 切换提醒启用状态（持久化并联动系统闹钟） */
    fun updateReminderEnabled(hour: Int, minute: Int, enabled: Boolean) {
        viewModelScope.launch {
            // 1) 更新 HydrateReminder.enabled
            hydrateReminderDao.updateEnabledByTime(hour, minute, enabled)

            // 2) 联动 AlarmRecord：启用则调度，禁用则取消
            val records = alarmRepository.getRecordsByTypeAndTime(AlarmRecord.TYPE_HYDRATION, hour, minute)
            if (enabled) {
                if (records.isEmpty()) {
                    // 若不存在对应闹钟记录，补充创建并调度
                    val newId = alarmRepository.addHydrationReminder(hour, minute, AlarmRecord.REPEAT_DAILY)
                    val newRecord = AlarmRecord.createHydrationReminder(hour, minute, AlarmRecord.REPEAT_DAILY)
                        .copy(id = newId, isEnabled = true)
                    alarmScheduler.scheduleAlarm(newRecord)
                } else {
                    records.forEach { record ->
                        // 更新启用状态
                        alarmRepository.enableAlarm(record.id)
                        // 调度系统闹钟
                        alarmScheduler.scheduleAlarm(record.copy(isEnabled = true))
                    }
                }
            } else {
                records.forEach { record ->
                    // 取消系统闹钟与通知
                    alarmScheduler.cancelAlarm(record)
                    alarmNotificationManager.cancelAlarmNotification(record)
                    // 更新禁用状态
                    alarmRepository.disableAlarm(record.id)
                }
            }
        }
    }

    /**
     * 设置变更后触发：同步当天饮水记录的设置快照字段
     * 仅在 DailyCups 或 CupVolume 变更时调用
     */
    fun syncTodayHydrateRecordSettings() {
        viewModelScope.launch {
            hydrateRepository.syncTodayHydrateRecordSettings()
        }
    }
}