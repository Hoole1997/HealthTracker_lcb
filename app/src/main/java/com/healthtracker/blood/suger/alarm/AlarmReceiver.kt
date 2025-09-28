package com.healthtracker.blood.suger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.data.repository.AlarmRepository
import com.healthtracker.blood.suger.data.repository.MedicineReminderRepository
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 闹钟广播接收器
 * 接收系统AlarmManager发送的闹钟触发广播，处理闹钟逻辑
 * 
 * 主要功能：
 * 1. 接收闹钟触发广播
 * 2. 验证闹钟有效性
 * 3. 显示通知提醒
 * 4. 处理重复闹钟的下次调度
 * 5. 更新闹钟触发记录
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
        
        // WakeLock标签
        private const val WAKE_LOCK_TAG = "HealthTracker:AlarmReceiver"
        
        // WakeLock超时时间（毫秒）
        private const val WAKE_LOCK_TIMEOUT = 30 * 1000L // 30秒
    }
    
    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var notificationManager: AlarmNotificationManager

    @Inject
    lateinit var medicineReminderRepository: MedicineReminderRepository
    
    // 协程作用域，用于异步处理
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        "Alarm broadcast received".logd(TAG)
        
        // 获取WakeLock确保处理完成前设备保持唤醒
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        )
        
        try {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT)
            
            // 获取闹钟信息
            val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
            val alarmType = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_TYPE, -1)
            
            if (alarmId == -1L) {
                "Invalid alarm ID received, ignoring broadcast".logw(TAG)
                return
            }
            
            "Processing alarm trigger: ID=$alarmId, Type=$alarmType".logd(TAG)
            
            // 异步处理闹钟逻辑
            val pendingResult = goAsync()
            coroutineScope.launch {
                try {
                    handleAlarmTrigger(context, alarmId, alarmType)
                } catch (e: Exception) {
                    "Error handling alarm trigger: ${e.message}".loge(TAG)
                } finally {
                    pendingResult.finish()
                }
            }
            
        } catch (e: Exception) {
            "Error in onReceive: ${e.message}".loge(TAG)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
    
    /**
     * 处理闹钟触发逻辑
     * 
     * @param context 上下文
     * @param alarmId 闹钟ID
     * @param alarmType 闹钟类型
     */
    private suspend fun handleAlarmTrigger(context: Context, alarmId: Long, alarmType: Int) {
        try {
            // 从数据库获取闹钟记录
            val alarmRecord = alarmRepository.getRecordById(alarmId)
            
            if (alarmRecord == null) {
                "Alarm record not found: ID=$alarmId".logw(TAG)
                return
            }
            
            if (!alarmRecord.isEnabled) {
                "Alarm is disabled, ignoring trigger: ID=$alarmId".logw(TAG)
                return
            }
            
            if (alarmRecord.isDeleted) {
                "Alarm is deleted, ignoring trigger: ID=$alarmId".logw(TAG)
                return
            }
            
            "Alarm trigger validated: ID=$alarmId, Time=${alarmRecord.getFormattedTime()}".logd(TAG)
            
            // 显示通知
            showAlarmNotification(alarmRecord)
            
            // 更新最后触发时间
            updateLastTriggerTime(alarmId)
            
            // 处理重复闹钟的下次调度
            scheduleNextRepeat(alarmRecord)
            
            "Alarm trigger processed successfully: ID=$alarmId".logd(TAG)
            
        } catch (e: Exception) {
            "Failed to handle alarm trigger: ID=$alarmId, Error=${e.message}".loge(TAG)
        }
    }
    
    /**
     * 显示闹钟通知
     * 根据闹钟类型选择不同的通知显示方式
     *
     * @param alarmRecord 闹钟记录
     */
    private fun showAlarmNotification(alarmRecord: AlarmRecord) {
        try {
            when (alarmRecord.type) {
                AlarmRecord.TYPE_MEDICATION -> {
                    // 服药提醒：显示全屏通知
                    showMedicationNotification(alarmRecord)
                    "Medication FSI notification shown: ID=${alarmRecord.id}".logd(TAG)
                }
                AlarmRecord.TYPE_BLOOD_SUGAR,
                AlarmRecord.TYPE_BLOOD_PRESSURE -> {
                    // 血糖血压测量提醒：显示普通通知
                    notificationManager.showAlarmNotification(alarmRecord)
                    "Health reminder notification shown: ID=${alarmRecord.id}".logd(TAG)
                }
                else -> {
                    // 其他类型：使用默认通知
                    notificationManager.showAlarmNotification(alarmRecord)
                    "Default alarm notification shown: ID=${alarmRecord.id}".logd(TAG)
                }
            }
        } catch (e: Exception) {
            "Failed to show alarm notification: ID=${alarmRecord.id}, Type=${alarmRecord.type}, Error=${e.message}".loge(TAG)
        }
    }

    /**
     * 显示服药提醒全屏通知
     * 通过药物ID查询数据库获取最新的药物信息
     *
     * @param alarmRecord 闹钟记录
     */
    private fun showMedicationNotification(alarmRecord: AlarmRecord) {
        try {
            if (!alarmRecord.isMedicationReminder()) {
                "Invalid medication alarm record: ID=${alarmRecord.id}".logw(TAG)
                return
            }

            val medicineId = alarmRecord.getMedicineId()
            if (medicineId == null) {
                "Invalid medicine ID in alarm record: ID=${alarmRecord.id}".logw(TAG)
                return
            }

            // 异步查询药物信息并创建通知
            coroutineScope.launch {
                try {
                    // 从数据库查询最新的药物信息
                    val medicineReminder = medicineReminderRepository.getMedicineById(medicineId)

                    if (medicineReminder == null) {
                        "Medicine not found: ID=$medicineId".logw(TAG)
                        return@launch
                    }

                    // 创建服药提醒全屏通知
                    notificationManager.createMedicationNotification(
                        medicationName = medicineReminder.medicineName,
                        dosage = "", // 简化版不显示剂量
                        notes = "", // 简化版不显示备注
                        reminderTime = alarmRecord.getFormattedTime(),
                        reminderId = medicineId
                    )

                    // 记录真实提醒时间到药物提醒记录
                    medicineReminderRepository.recordRealRemind(medicineId)

                    "Medication FSI notification created: MedicineID=$medicineId, Name=${medicineReminder.medicineName}".logd(TAG)

                } catch (e: Exception) {
                    "Failed to query medicine and show notification: MedicineID=$medicineId, Error=${e.message}".loge(TAG)
                }
            }

        } catch (e: Exception) {
            "Failed to show medication notification: ID=${alarmRecord.id}, Error=${e.message}".loge(TAG)
        }
    }
    
    /**
     * 更新最后触发时间
     * 
     * @param alarmId 闹钟ID
     */
    private suspend fun updateLastTriggerTime(alarmId: Long) {
        try {
            val success = alarmRepository.updateLastTriggerTime(alarmId)
            if (success) {
                "Last trigger time updated: ID=$alarmId".logd(TAG)
            } else {
                "Failed to update last trigger time: ID=$alarmId".logw(TAG)
            }
        } catch (e: Exception) {
            "Error updating last trigger time: ID=$alarmId, Error=${e.message}".loge(TAG)
        }
    }
    
    /**
     * 调度重复闹钟的下次触发
     * 
     * @param alarmRecord 闹钟记录
     */
    private fun scheduleNextRepeat(alarmRecord: AlarmRecord) {
        try {
            if (alarmRecord.isRepeating()) {
                val success = alarmScheduler.rescheduleRepeatingAlarm(alarmRecord)
                if (success) {
                    val nextTriggerTime = alarmScheduler.calculateNextTriggerTime(alarmRecord)
                    val calendar = java.util.Calendar.getInstance().apply { 
                        timeInMillis = nextTriggerTime 
                    }
                    "Next repeat scheduled: ID=${alarmRecord.id}, NextTime=${calendar.time}".logd(TAG)
                } else {
                    "Failed to schedule next repeat: ID=${alarmRecord.id}".logw(TAG)
                }
            } else {
                // 单次闹钟触发后自动禁用
                coroutineScope.launch {
                    try {
                        val success = alarmRepository.disableAlarm(alarmRecord.id)
                        if (success) {
                            "Single alarm disabled after trigger: ID=${alarmRecord.id}".logd(TAG)
                        } else {
                            "Failed to disable single alarm: ID=${alarmRecord.id}".logw(TAG)
                        }
                    } catch (e: Exception) {
                        "Error disabling single alarm: ID=${alarmRecord.id}, Error=${e.message}".loge(TAG)
                    }
                }
            }
        } catch (e: Exception) {
            "Error scheduling next repeat: ID=${alarmRecord.id}, Error=${e.message}".loge(TAG)
        }
    }
    
    /**
     * 验证闹钟触发的有效性
     * 检查时间是否匹配、是否在有效的重复日期等
     * 
     * @param alarmRecord 闹钟记录
     * @return 是否有效
     */
    private fun validateAlarmTrigger(alarmRecord: AlarmRecord): Boolean {
        try {
            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)
            
            // 检查时间是否匹配（允许1分钟误差）
            val timeDiff = Math.abs((currentHour * 60 + currentMinute) - (alarmRecord.hour * 60 + alarmRecord.minute))
            if (timeDiff > 1) {
                "Alarm time mismatch: Expected=${alarmRecord.getFormattedTime()}, Current=$currentHour:$currentMinute".logw(TAG)
                return false
            }
            
            // 检查重复规则
            if (alarmRecord.isRepeating()) {
                val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                val alarmDayOfWeek = convertCalendarDayToAlarmDay(dayOfWeek)
                
                if (!alarmRecord.shouldRingOnDay(alarmDayOfWeek)) {
                    "Alarm should not ring today: ID=${alarmRecord.id}, DayOfWeek=$alarmDayOfWeek".logw(TAG)
                    return false
                }
            }
            
            return true
        } catch (e: Exception) {
            "Error validating alarm trigger: ID=${alarmRecord.id}, Error=${e.message}".loge(TAG)
            return false
        }
    }
    
    /**
     * 转换Calendar的星期表示为AlarmRecord的星期表示
     * 
     * @param calendarDay Calendar的星期值
     * @return AlarmRecord的星期值
     */
    private fun convertCalendarDayToAlarmDay(calendarDay: Int): Int {
        return when (calendarDay) {
            java.util.Calendar.SUNDAY -> 7
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            else -> 1
        }
    }
    
    /**
     * 处理闹钟触发异常
     * 记录错误信息并尝试恢复
     * 
     * @param alarmId 闹钟ID
     * @param error 异常信息
     */
    private fun handleAlarmError(alarmId: Long, error: Throwable) {
        "Alarm trigger error: ID=$alarmId, Error=${error.message}".loge(TAG)
        
        // 可以在这里添加错误恢复逻辑
        // 例如：重新调度闹钟、发送错误报告等
    }
}