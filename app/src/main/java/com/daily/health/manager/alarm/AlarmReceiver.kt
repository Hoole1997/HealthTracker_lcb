package com.daily.health.manager.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.data.repository.AlarmRepository
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

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
class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
        
        // WakeLock标签
        private const val WAKE_LOCK_TAG = "HealthTracker:AlarmReceiver"
        
        // WakeLock超时时间（毫秒）
        private const val WAKE_LOCK_TIMEOUT = 30 * 1000L // 30秒
    }
    
    // 协程作用域，用于异步处理
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onReceive(context: Context, intent: Intent) {
        "Alarm broadcast received".logd(TAG)

        val koin = runCatching { GlobalContext.get() }.getOrNull()
        val alarmRepository = koin?.get<AlarmRepository>()
        val alarmScheduler = koin?.get<AlarmScheduler>()
        val notificationManager = koin?.get<AlarmNotificationManager>()

        if (alarmRepository == null || alarmScheduler == null || notificationManager == null) {
            "Koin not ready, skipping alarm handling".logw(TAG)
            return
        }
        
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
                    handleAlarmTrigger(alarmRepository, alarmScheduler, notificationManager, alarmId, alarmType)
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
    private suspend fun handleAlarmTrigger(
        alarmRepository: AlarmRepository,
        alarmScheduler: AlarmScheduler,
        notificationManager: AlarmNotificationManager,
        alarmId: Long,
        alarmType: Int
    ) {
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
            showAlarmNotification(notificationManager, alarmRecord)

            // 更新最后触发时间
            updateLastTriggerTime(alarmRepository, alarmId)
            
            // 处理重复闹钟的下次调度
            scheduleNextRepeat(alarmRepository, alarmScheduler, alarmRecord)
            
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
    private fun showAlarmNotification(notificationManager: AlarmNotificationManager, alarmRecord: AlarmRecord) {
        try {
            notificationManager.showAlarmNotification(alarmRecord)
        } catch (e: Exception) {
            "Failed to show alarm notification: ID=${alarmRecord.id}, Type=${alarmRecord.type}, Error=${e.message}".loge(TAG)
        }
    }

    
    /**
     * 更新最后触发时间
     * 
     * @param alarmId 闹钟ID
     */
    private suspend fun updateLastTriggerTime(alarmRepository: AlarmRepository, alarmId: Long) {
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
    private fun scheduleNextRepeat(
        alarmRepository: AlarmRepository,
        alarmScheduler: AlarmScheduler,
        alarmRecord: AlarmRecord
    ) {
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

}