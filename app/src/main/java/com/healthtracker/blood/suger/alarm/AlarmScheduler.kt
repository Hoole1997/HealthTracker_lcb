package com.healthtracker.blood.suger.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.util.AlarmRepeatHelper
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统级闹钟调度器
 * 负责与Android系统AlarmManager交互，管理系统级闹钟的创建、更新、删除
 * 
 * 主要功能：
 * 1. 注册系统级闹钟
 * 2. 取消系统级闹钟
 * 3. 处理重复闹钟逻辑
 * 4. 计算下次触发时间
 * 5. 系统重启后恢复闹钟
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "AlarmScheduler"
        
        // PendingIntent请求码基础值
        private const val REQUEST_CODE_BASE = 20000
        
        // Intent额外数据键
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_TYPE = "alarm_type"
    }
    
    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
    
    /**
     * 调度闹钟
     * 根据闹钟记录创建系统级闹钟
     * 
     * @param alarmRecord 闹钟记录
     * @return 是否调度成功
     */
    fun scheduleAlarm(alarmRecord: AlarmRecord): Boolean {
        return try {
            if (!alarmRecord.isEnabled) {
                "Alarm is disabled, skipping schedule: ID=${alarmRecord.id}".logw(TAG)
                return false
            }
            
            val triggerTime = calculateNextTriggerTime(alarmRecord)
            if (triggerTime <= System.currentTimeMillis()) {
                "Trigger time is in the past, skipping schedule: ID=${alarmRecord.id}".logw(TAG)
                return false
            }
            
            val pendingIntent = createPendingIntent(alarmRecord)
            
            // 使用非精确闹钟API，适用于所有版本且不需要特殊权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ 使用setAndAllowWhileIdle确保休眠唤醒
                // 这个方法不需要精确闹钟权限，但仍能在Doze模式下工作
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // 低版本使用普通set方法
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            val calendar = Calendar.getInstance().apply { timeInMillis = triggerTime }
            "Alarm scheduled successfully: ID=${alarmRecord.id}, Time=${calendar.time}".logd(TAG)
            true
        } catch (e: Exception) {
            "Failed to schedule alarm: ID=${alarmRecord.id}, Error=${e.message}".loge(TAG)
            false
        }
    }
    
    /**
     * 取消闹钟
     * 
     * @param alarmRecord 闹钟记录
     * @return 是否取消成功
     */
    fun cancelAlarm(alarmRecord: AlarmRecord): Boolean {
        return try {
            val pendingIntent = createPendingIntent(alarmRecord)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            
            "Alarm cancelled successfully: ID=${alarmRecord.id}".logd(TAG)
            true
        } catch (e: Exception) {
            "Failed to cancel alarm: ID=${alarmRecord.id}, Error=${e.message}".loge(TAG)
            false
        }
    }
    
    /**
     * 更新闹钟
     * 先取消旧闹钟，再创建新闹钟
     * 
     * @param alarmRecord 闹钟记录
     * @return 是否更新成功
     */
    fun updateAlarm(alarmRecord: AlarmRecord): Boolean {
        return try {
            // 先取消现有闹钟
            cancelAlarm(alarmRecord)
            
            // 如果闹钟已启用，重新调度
            if (alarmRecord.isEnabled) {
                scheduleAlarm(alarmRecord)
            } else {
                "Alarm is disabled, not rescheduling: ID=${alarmRecord.id}".logd(TAG)
                true
            }
        } catch (e: Exception) {
            "Failed to update alarm: ID=${alarmRecord.id}, Error=${e.message}".loge(TAG)
            false
        }
    }
    
    /**
     * 批量调度闹钟
     * 
     * @param alarmRecords 闹钟记录列表
     * @return 成功调度的数量
     */
    fun scheduleAlarms(alarmRecords: List<AlarmRecord>): Int {
        var successCount = 0
        
        alarmRecords.forEach { alarmRecord ->
            if (scheduleAlarm(alarmRecord)) {
                successCount++
            }
        }
        
        "Batch schedule completed: ${successCount}/${alarmRecords.size} alarms scheduled".logd(TAG)
        return successCount
    }
    
    /**
     * 批量取消闹钟
     * 
     * @param alarmRecords 闹钟记录列表
     * @return 成功取消的数量
     */
    fun cancelAlarms(alarmRecords: List<AlarmRecord>): Int {
        var successCount = 0
        
        alarmRecords.forEach { alarmRecord ->
            if (cancelAlarm(alarmRecord)) {
                successCount++
            }
        }
        
        "Batch cancel completed: ${successCount}/${alarmRecords.size} alarms cancelled".logd(TAG)
        return successCount
    }
    
    /**
     * 重新调度下次重复闹钟
     * 用于处理重复闹钟触发后的下次调度
     * 
     * @param alarmRecord 闹钟记录
     * @return 是否调度成功
     */
    fun rescheduleRepeatingAlarm(alarmRecord: AlarmRecord): Boolean {
        return if (alarmRecord.isRepeating()) {
            scheduleAlarm(alarmRecord)
        } else {
            "Alarm is not repeating, no need to reschedule: ID=${alarmRecord.id}".logd(TAG)
            true
        }
    }
    
    /**
     * 计算下次触发时间
     * 
     * @param alarmRecord 闹钟记录
     * @return 下次触发时间戳
     */
    fun calculateNextTriggerTime(alarmRecord: AlarmRecord): Long {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis
        
        // 设置闹钟时间
        calendar.set(Calendar.HOUR_OF_DAY, alarmRecord.hour)
        calendar.set(Calendar.MINUTE, alarmRecord.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        var triggerTime = calendar.timeInMillis
        
        // 如果是重复闹钟，需要找到下一个有效的触发时间
        if (alarmRecord.isRepeating()) {
            // 如果今天的时间已过，从明天开始查找
            if (triggerTime <= currentTime) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                triggerTime = calendar.timeInMillis
            }
            
            // 查找下一个符合重复规则的日期
            var attempts = 0
            while (attempts < 7) { // 最多查找7天
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val dayOfWeekForAlarm = convertCalendarDayToAlarmDay(dayOfWeek)
                
                if (alarmRecord.shouldRingOnDay(dayOfWeekForAlarm)) {
                    break
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                triggerTime = calendar.timeInMillis
                attempts++
            }
        } else {
            // 单次闹钟，如果时间已过，设置为明天同一时间
            if (triggerTime <= currentTime) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                triggerTime = calendar.timeInMillis
            }
        }
        
        return triggerTime
    }
    
    /**
     * 检查闹钟是否可以调度
     *
     * @return 是否可以调度闹钟（使用非精确API，始终可用）
     */
    fun canScheduleAlarms(): Boolean {
        // 使用非精确闹钟API，不需要特殊权限，始终可用
        return true
    }
    
    /**
     * 创建PendingIntent
     * 
     * @param alarmRecord 闹钟记录
     * @return PendingIntent
     */
    private fun createPendingIntent(alarmRecord: AlarmRecord): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmRecord.id)
            putExtra(EXTRA_ALARM_TYPE, alarmRecord.type)
        }
        
        val requestCode = REQUEST_CODE_BASE + alarmRecord.id.toInt()
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
    
    /**
     * 转换Calendar的星期表示为AlarmRecord的星期表示
     * Calendar: 1=周日, 2=周一, ..., 7=周六
     * AlarmRecord: 1=周一, 2=周二, ..., 7=周日
     * 
     * @param calendarDay Calendar的星期值
     * @return AlarmRecord的星期值
     */
    private fun convertCalendarDayToAlarmDay(calendarDay: Int): Int {
        return when (calendarDay) {
            Calendar.SUNDAY -> 7
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 1
        }
    }
    
    /**
     * 获取调度器状态信息
     *
     * @return 调度器状态
     */
    fun getSchedulerStatus(): AlarmSchedulerStatus {
        return AlarmSchedulerStatus(
            canScheduleAlarms = canScheduleAlarms(),
            systemAlarmManagerAvailable = true // 直接返回true，因为alarmManager已初始化
        )
    }
}

/**
 * 闹钟调度器状态
 *
 * @property canScheduleAlarms 是否可以调度闹钟
 * @property systemAlarmManagerAvailable 系统AlarmManager是否可用
 */
data class AlarmSchedulerStatus(
    val canScheduleAlarms: Boolean,
    val systemAlarmManagerAvailable: Boolean
) {
    /**
     * 调度器是否完全可用
     */
    val fullyAvailable: Boolean
        get() = canScheduleAlarms && systemAlarmManagerAvailable
}