package com.healthtracker.blood.suger.strategy

import android.content.Context
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.config.models.ChannelConfig
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.push.getFirstInstallTime
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.SpUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 推送频率控制器
 *
 * 职责：
 * 1. 执行所有频率相关的检查（权限、冷却、免打扰、每日限额、推送间隔）
 * 2. 记录推送触发历史
 * 3. 管理推送计数器（按日期自动重置）
 *
 * 规则：
 * - 付费用户：每日 999 次，无新用户冷却，10 分钟推送间隔，免打扰 02:00-07:00
 * - 自然用户：每日 3 次，24 分钟新用户冷却，10 分钟推送间隔，免打扰 02:00-08:00
 * - 免打扰时间（闹钟场景除外）
 */
@Singleton
class PushFrequencyController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {

    companion object {
        private const val TAG = "PushFrequencyController"

        // SharedPreferences Keys
        private const val KEY_DAILY_COUNT = "daily_count"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_UNLOCK_LAST_PUSH_TIME = "unlock_last_push_time"
        private const val KEY_BACKGROUND_LAST_PUSH_TIME = "background_last_push_time"
        private const val KEY_OTHER_LAST_PUSH_TIME = "other_last_push_time"
        private const val KEY_LAST_PUSH_SCENARIO = "last_push_scenario"
    }



    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 检查是否可以触发推送
     * 执行所有频率检查，任一检查失败则返回 false
     *
     * @param pushId 推送消息 ID（用于日志）
     * @param scenario 推送场景
     * @param isPaidUser 是否付费用户
     * @return 频率检查结果
     */
    fun canTriggerPush(
        pushId: String,
        scenario: PushScenario,
        config: ChannelConfig
    ): FrequencyCheckResult {
        if (BuildState.debug) {
            "Frequency check for pushId=$pushId, scenario=$scenario, config=$config".logd(PushOrchestrator.TAG)
        }

        val type = if(scenario == PushScenario.FCM) "firebase_push" else "local_push"

        if(AppLifecycleManager.isForeground()){
            val reason = "app_inner_${type}_app_in_foreground"
            if (BuildState.debug) reason.logw(PushOrchestrator.TAG)
            return FrequencyCheckResult(false,reason)
        }


        if(config.notificationEnabled == 0){
            val reason = "app_inner_${type}_notification_disabled"
            if (BuildState.debug) reason.logw(PushOrchestrator.TAG)
            return FrequencyCheckResult(false,reason)
        }

        // 检查 1: 通知权限
        if (!permissionManager.isNotificationPermissionGranted()) {
            val reason = "app_inner_${type}_no_permission"
            if (BuildState.debug) reason.logw(PushOrchestrator.TAG)
            return FrequencyCheckResult(false, reason)
        }

        // 检查 2: 新用户冷却期
        val cooldownResult = checkNewUserCooldown(config,type)
        if (!cooldownResult.canTrigger) {
            return cooldownResult
        }

        // 检查 3: 免打扰时间（闹钟场景除外）
        val dndResult = checkDoNotDisturbTime(config,type)
        if (!dndResult.canTrigger) {
            return dndResult
        }

        // 检查 4: 每日推送限额
        val dailyLimitResult = checkDailyLimit(config,type)
        if (!dailyLimitResult.canTrigger) {
            return dailyLimitResult
        }

        // 检查 5: 推送间隔
        val intervalResult = checkPushInterval(scenario,config,type)
        if (!intervalResult.canTrigger) {
            return intervalResult
        }

        if (BuildState.debug) {
            "All frequency checks passed".logd(PushOrchestrator.TAG)
        }

        return FrequencyCheckResult(canTrigger = true)
    }

    /**
     * 记录推送触发
     * 更新推送计数器和最后推送时间
     *
     * @param pushId 推送消息 ID
     * @param scenario 推送场景
     */
    fun recordPushTrigger(pushId: String, scenario: PushScenario) {
        val currentTime = System.currentTimeMillis()
        val currentDate = dateFormat.format(Date(currentTime))

        // 检查日期是否变更，需要重置计数器
        val lastResetDate = SpUtils.getString(KEY_LAST_RESET_DATE)
        if (lastResetDate != currentDate) {
            SpUtils.putInt(KEY_DAILY_COUNT, 1)
            SpUtils.putString(KEY_LAST_RESET_DATE, currentDate)
            if (BuildState.debug) {
                "Daily counter reset for new day: $currentDate".logd(PushOrchestrator.TAG)
            }
        } else {
            // 同一天，计数器 +1
            val currentCount = SpUtils.getInt(KEY_DAILY_COUNT, 0)
            SpUtils.putInt(KEY_DAILY_COUNT, currentCount + 1)
        }

       val key = getLastPushTimeKey(scenario)
        // 更新最后推送时间和场景
        SpUtils.putLong(key, currentTime)
        SpUtils.putString(KEY_LAST_PUSH_SCENARIO, scenario.name)


        if (BuildState.debug) {
            val count = SpUtils.getInt(KEY_DAILY_COUNT, 0)
            "Push recorded: pushId=$pushId, scenario=$scenario, dailyCount=$count".logd(PushOrchestrator.TAG)
        }
    }


    private fun getLastPushTimeKey(scenario: PushScenario) = when(scenario){
        PushScenario.UNLOCK -> KEY_UNLOCK_LAST_PUSH_TIME
        else -> KEY_BACKGROUND_LAST_PUSH_TIME
    }

    /**
     * 检查新用户冷却期
     * - 付费用户：无冷却期（0 分钟）
     * - 自然用户：24 分钟冷却期
     */
    private fun checkNewUserCooldown(config: ChannelConfig,type:String = "local_push"): FrequencyCheckResult {
        val cooldownMinutes = config.newUserCooldown

        // 付费用户无冷却期，直接通过
        if (cooldownMinutes == 0) {
            if (BuildState.debug) "new user cool time = 0,pass".logd(PushOrchestrator.TAG)
            return FrequencyCheckResult(canTrigger = true)
        }

        // 计算已过去的分钟数
        val currentTime = System.currentTimeMillis()
        val firstInstallTime = getFirstInstallTime()
        val elapsedMinutes = (currentTime - firstInstallTime) / (60 * 1000)

        if (elapsedMinutes < cooldownMinutes) {
            val remainingMinutes = cooldownMinutes - elapsedMinutes
            if (BuildState.debug) "New user cooldown: $remainingMinutes minutes remaining,installTime:${
                DateTimeUtils.formatDateTimeWithSeconds(
                    Date(firstInstallTime)
                )
            }".logd(TAG)
            val reason = "app_inner_${type}_new_user_cooldown"
            return FrequencyCheckResult(false, reason)
        }

        return FrequencyCheckResult(canTrigger = true)
    }

    /**
     * 检查免打扰时间
     * - 付费用户：02:00-07:00 不推送
     * - 自然用户：02:00-08:00 不推送
     * （闹钟场景在外部已排除）
     */
    private fun checkDoNotDisturbTime(config: ChannelConfig,type:String): FrequencyCheckResult {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // 根据用户类型确定免打扰结束时间
        val dndEnd = config.doNotDisturbEnd
        val dndStart = config.doNotDisturbStart

        // 将时间字符串转换为小时数进行比较（兼容API 24）
        val startHour = parseTimeStringToHour(dndStart)
        val endHour = parseTimeStringToHour(dndEnd)

        // 检查是否在免打扰时段内
        val isInDoNotDisturbPeriod = if (startHour < endHour) {
            // 正常时段，如 02:00-08:00
            currentHour >= startHour && currentHour < endHour
        } else {
            // 跨越午夜的时段，如 22:00-07:00
            currentHour >= startHour || currentHour < endHour
        }

        if (isInDoNotDisturbPeriod) {
             "Do-not-disturb time: $currentHour:00 (blocked between " +
                    "$startHour:00-$endHour:00)".logd(TAG)
            val reason = "app_inner_${type}_do_not_disturb"
            return FrequencyCheckResult(false, reason)
        }

        return FrequencyCheckResult(canTrigger = true)
    }

    /**
     * 检查每日推送限额
     * - 付费用户：999 次/天
     * - 自然用户：3 次/天
     */
    private fun checkDailyLimit(config: ChannelConfig,type:String): FrequencyCheckResult {
        val dailyLimit = config.totalPushCount

        // 检查日期是否变更
        val currentDate = dateFormat.format(Date())
        val lastResetDate = SpUtils.getString(KEY_LAST_RESET_DATE)

        val dailyCount = if (lastResetDate != currentDate) {
            // 新的一天，计数器重置为 0
            if (BuildState.debug) {
                "New day detected, daily count reset".logd(PushOrchestrator.TAG)
            }
            0
        } else {
            // 同一天，读取当前计数
            SpUtils.getInt(KEY_DAILY_COUNT, 0)
        }

        if (dailyCount >= dailyLimit) {
             "Daily limit reached: $dailyCount/$dailyLimit".logd(TAG)
            val reason = "app_inner_${type}_daily_limit_reached"
            return FrequencyCheckResult(false, reason)
        }

        return FrequencyCheckResult(canTrigger = true)
    }

    /**
     * 检查推送间隔
     * - 解锁/后台场景：10 分钟间隔
     * - 闹钟场景：无间隔限制（直接通过）
     * - FCM 场景：10 分钟间隔
     */
    private fun checkPushInterval(scenario: PushScenario,config: ChannelConfig,type:String): FrequencyCheckResult {
        // 闹钟场景无间隔限制
        if (scenario == PushScenario.FCM || scenario == PushScenario.KEEPALIVE) {
            if (BuildState.debug) "scenario = $scenario,no interval limit".logd(TAG)
            return FrequencyCheckResult(canTrigger = true)
        }

        val lastPushTime = SpUtils.getLong(getLastPushTimeKey(scenario), 0L)
        if (lastPushTime == 0L) {
            // 首次推送，直接通过
            return FrequencyCheckResult(canTrigger = true)
        }

        val currentTime = System.currentTimeMillis()
        val elapsedMinutes = (currentTime - lastPushTime) / (60 * 1000)

        val interval = if(scenario == PushScenario.UNLOCK) config.unlockPushInterval else config.backgroundPushInterval

        if (elapsedMinutes < interval) {
            val remainingMinutes = interval - elapsedMinutes
            "Push interval not met: $elapsedMinutes/$interval minutes " +
                    "(remaining: $remainingMinutes minutes)".logd(TAG)
            val reason = "app_inner_${type}_trigger_interval_not_met "
            return FrequencyCheckResult(false, reason)
        }

        return FrequencyCheckResult(canTrigger = true)
    }

    /**
     * 获取当前每日推送计数（调试用）
     */
    fun getDailyPushCount(): Int {
        val currentDate = dateFormat.format(Date())
        val lastResetDate = SpUtils.getString(KEY_LAST_RESET_DATE)

        return if (lastResetDate != currentDate) {
            0
        } else {
            SpUtils.getInt(KEY_DAILY_COUNT, 0)
        }
    }

    /**
     * 重置所有频率控制数据（调试/测试用）
     */
    fun resetAll() {
        if (BuildState.debug) {
            "All frequency control data reset".logd(PushOrchestrator.TAG)
        }
    }

    /**
     * 解析时间字符串为小时数（兼容API 24）
     * 
     * @param timeString 时间字符串，格式为 "HH:mm"（如 "08:00"）
     * @return 小时数（0-23）
     */
    private fun parseTimeStringToHour(timeString: String): Int {
        return try {
            // 使用字符串分割的方式提取小时部分，兼容API 24
            val parts = timeString.split(":")
            if (parts.size >= 2) {
                parts[0].toIntOrNull() ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            if (BuildState.debug) {
                "Failed to parse time string: $timeString, error: ${e.message}".logw(PushOrchestrator.TAG)
            }
            0
        }
    }
}
