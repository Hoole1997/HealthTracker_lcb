package com.healthtracker.blood.suger.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthtracker.earthquake.push.EarthquakePushInitializer
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.data.repository.AlarmRepository
import com.healthtracker.blood.suger.manager.HealthServiceManager
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * 系统启动广播接收器
 * 监听系统启动、应用更新等事件，恢复系统级闹钟
 * 
 * 主要功能：
 * 1. 监听系统启动广播
 * 2. 监听应用更新广播
 * 3. 从数据库恢复启用的闹钟
 * 4. 重新注册系统级闹钟
 * 5. 处理恢复过程中的异常
 */
class SystemBootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SystemBootReceiver"
    }
    
    // 协程作用域，用于异步处理
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 延迟初始化的依赖
    private val alarmRepository: AlarmRepository? by lazy {
        runCatching { GlobalContext.get().get<AlarmRepository>() }.getOrNull()
    }
    private val alarmScheduler: AlarmScheduler? by lazy {
        runCatching { GlobalContext.get().get<AlarmScheduler>() }.getOrNull()
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        "System broadcast received: $action".logd(TAG)

        val koin = runCatching { GlobalContext.get() }.getOrNull()
        val alarmRepository = koin?.get<AlarmRepository>()
        val alarmScheduler = koin?.get<AlarmScheduler>()
        val serviceManager = koin?.get<HealthServiceManager>()

        if (alarmRepository == null || alarmScheduler == null) {
            "Koin not ready, skipping broadcast handling".logw(TAG)
            return
        }
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                "System boot completed, restoring alarms".logd(TAG)
                restoreAlarms(context, "BOOT_COMPLETED", alarmRepository, alarmScheduler)
                tryShowResident(serviceManager)
                // 初始化地震推送每日调度
                EarthquakePushInitializer.init(context.applicationContext)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                "App package replaced, restoring alarms".logd(TAG)
                restoreAlarms(context, "PACKAGE_REPLACED", alarmRepository, alarmScheduler)
                // 应用更新后重新初始化地震推送每日调度
                EarthquakePushInitializer.init(context.applicationContext)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                // 检查是否是当前应用的包被替换
                val packageName = intent.dataString?.removePrefix("package:")
                if (packageName == context.packageName) {
                    "Current app package replaced, restoring alarms".logd(TAG)
                    restoreAlarms(context, "PACKAGE_REPLACED", alarmRepository, alarmScheduler)
                    // 当前应用替换后重新初始化地震推送每日调度
                    EarthquakePushInitializer.init(context.applicationContext)
                }
            }
            else -> {
                "Unhandled broadcast action: $action".logw(TAG)
            }
        }
    }
    
    /**
     * 恢复闹钟
     * 
     * @param context 上下文
     * @param reason 恢复原因
     * @param alarmRepository 闹钟仓库
     * @param alarmScheduler 闹钟调度器
     */
    private fun restoreAlarms(
        context: Context,
        reason: String,
        alarmRepository: AlarmRepository,
        alarmScheduler: AlarmScheduler
    ) {
        val pendingResult = goAsync()
        
        coroutineScope.launch {
            try {
                "Starting alarm restoration, reason: $reason".logd(TAG)
                
                // 检查调度器状态
                val schedulerStatus = alarmScheduler.getSchedulerStatus()
                if (!schedulerStatus.fullyAvailable) {
                    "Alarm scheduler not fully available, skipping restoration".logw(TAG)
                    "Scheduler status: canScheduleAlarms=${schedulerStatus.canScheduleAlarms}, systemAvailable=${schedulerStatus.systemAlarmManagerAvailable}".logw(TAG)
                    return@launch
                }
                
                // 获取所有启用的闹钟
                val enabledAlarms = alarmRepository.getAllRecordsSync()
                    .filter { it.isEnabled && !it.isDeleted }
                
                if (enabledAlarms.isEmpty()) {
                    "No enabled alarms found, nothing to restore".logd(TAG)
                    return@launch
                }
                
                "Found ${enabledAlarms.size} enabled alarms to restore".logd(TAG)
                
                // 批量恢复闹钟
                val restoredCount = restoreAlarmsInBatch(enabledAlarms, alarmScheduler)
                
                "Alarm restoration completed: $restoredCount/${enabledAlarms.size} alarms restored, reason: $reason".logd(TAG)
                
                // 记录恢复统计信息
                logRestorationStatistics(enabledAlarms, restoredCount, reason)
                
            } catch (e: Exception) {
                "Failed to restore alarms: ${e.message}".loge(TAG)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    /**
     * 批量恢复闹钟
     * 
     * @param alarms 要恢复的闹钟列表
     * @param scheduler 闹钟调度器
     * @return 成功恢复的数量
     */
    private suspend fun restoreAlarmsInBatch(alarms: List<AlarmRecord>, scheduler: AlarmScheduler): Int {
        var successCount = 0
        var failureCount = 0
        
        alarms.forEach { alarm ->
            try {
                val success = scheduler.scheduleAlarm(alarm)
                if (success) {
                    successCount++
                    "Alarm restored: ID=${alarm.id}, Time=${alarm.getFormattedTime()}, Type=${alarm.type}".logd(TAG)
                } else {
                    failureCount++
                    "Failed to restore alarm: ID=${alarm.id}".logw(TAG)
                }
            } catch (e: Exception) {
                failureCount++
                "Error restoring alarm: ID=${alarm.id}, Error=${e.message}".loge(TAG)
            }
        }
        
        "Batch restoration result: Success=$successCount, Failure=$failureCount".logd(TAG)
        return successCount
    }
    
    /**
     * 记录恢复统计信息
     * 
     * @param allAlarms 所有闹钟
     * @param restoredCount 恢复成功的数量
     * @param reason 恢复原因
     */
    private fun logRestorationStatistics(
        allAlarms: List<AlarmRecord>, 
        restoredCount: Int, 
        reason: String
    ) {
        try {
            val bloodSugarCount = allAlarms.count { it.type == AlarmRecord.TYPE_BLOOD_SUGAR }
            val bloodPressureCount = allAlarms.count { it.type == AlarmRecord.TYPE_BLOOD_PRESSURE }
            val repeatingCount = allAlarms.count { it.isRepeating() }
            val onceCount = allAlarms.count { !it.isRepeating() }
            
            "Restoration statistics:".logd(TAG)
            "  Reason: $reason".logd(TAG)
            "  Total alarms: ${allAlarms.size}".logd(TAG)
            "  Restored: $restoredCount".logd(TAG)
            "  Blood sugar alarms: $bloodSugarCount".logd(TAG)
            "  Blood pressure alarms: $bloodPressureCount".logd(TAG)
            "  Repeating alarms: $repeatingCount".logd(TAG)
            "  Once alarms: $onceCount".logd(TAG)
            
        } catch (e: Exception) {
            "Error logging restoration statistics: ${e.message}".loge(TAG)
        }
    }
    
    /**
     * 验证闹钟恢复的必要性
     * 检查是否真的需要恢复闹钟
     * 
     * @param context 上下文
     * @param scheduler 闹钟调度器
     * @return 是否需要恢复
     */
    private fun shouldRestoreAlarms(context: Context, scheduler: AlarmScheduler): Boolean {
        return try {
            // 检查调度器是否可用
            val schedulerStatus = scheduler.getSchedulerStatus()
            if (!schedulerStatus.fullyAvailable) {
                "Scheduler not available, skipping restoration".logw(TAG)
                return false
            }
            
            // 可以添加其他检查条件
            // 例如：检查上次恢复时间、检查系统状态等
            
            true
        } catch (e: Exception) {
            "Error checking restoration necessity: ${e.message}".loge(TAG)
            false
        }
    }
    
    /**
     * 处理恢复失败的闹钟
     * 对于恢复失败的闹钟，可以尝试其他恢复策略
     * 
     * @param failedAlarms 恢复失败的闹钟列表
     */
    private suspend fun handleFailedRestorations(failedAlarms: List<AlarmRecord>) {
        if (failedAlarms.isEmpty()) return
        
        "Handling ${failedAlarms.size} failed alarm restorations".logw(TAG)
        
        failedAlarms.forEach { alarm ->
            try {
                // 可以尝试延迟恢复、降级处理等策略
                "Attempting recovery for failed alarm: ID=${alarm.id}".logd(TAG)
                
                // 例如：禁用有问题的闹钟
                // alarmRepository.disableAlarm(alarm.id)
                
            } catch (e: Exception) {
                "Error handling failed restoration: ID=${alarm.id}, Error=${e.message}".loge(TAG)
            }
        }
    }
    
    /**
     * 获取恢复状态信息
     * 
     * @return 恢复状态
     */
    suspend fun getRestorationStatus(): AlarmRestorationStatus {
        return try {
            val repo = alarmRepository ?: return AlarmRestorationStatus(0, 0, false, false)
            val scheduler = alarmScheduler ?: return AlarmRestorationStatus(0, 0, false, false)
            
            val allAlarms = repo.getAllRecordsSync()
            val enabledAlarms = allAlarms.filter { it.isEnabled && !it.isDeleted }
            val schedulerStatus = scheduler.getSchedulerStatus()
            
            AlarmRestorationStatus(
                totalAlarms = allAlarms.size,
                enabledAlarms = enabledAlarms.size,
                schedulerAvailable = schedulerStatus.fullyAvailable,
                canScheduleAlarms = schedulerStatus.canScheduleAlarms
            )
        } catch (e: Exception) {
            "Error getting restoration status: ${e.message}".loge(TAG)
            AlarmRestorationStatus(0, 0, false, false)
        }
    }

    private fun tryShowResident(serviceManager: HealthServiceManager?) {
        serviceManager?.startHealthService()
    }
}

/**
 * 闹钟恢复状态
 * 
 * @property totalAlarms 总闹钟数量
 * @property enabledAlarms 启用的闹钟数量
 * @property schedulerAvailable 调度器是否可用
 * @property canScheduleAlarms 是否可以调度闹钟
 */
data class AlarmRestorationStatus(
    val totalAlarms: Int,
    val enabledAlarms: Int,
    val schedulerAvailable: Boolean,
    val canScheduleAlarms: Boolean
) {
    /**
     * 是否准备好进行恢复
     */
    val readyForRestoration: Boolean
        get() = schedulerAvailable && canScheduleAlarms && enabledAlarms > 0
}