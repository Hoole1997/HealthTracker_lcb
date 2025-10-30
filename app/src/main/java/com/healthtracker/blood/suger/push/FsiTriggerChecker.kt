package com.healthtracker.blood.suger.push

import android.content.Context
import com.healthtracker.blood.suger.config.models.FsiConfig
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import java.util.Calendar

private const val TAG = "FsiTriggerChecker"
private const val INSTALL_COOLDOWN_HOURS = 24  // 安装后固定冷却期（小时）

/**
 * 检查是否可以升级为全屏通知
 */
fun canUpgradeToFullScreen(
    config: FsiConfig
): Boolean {
    // 1. 检查开关
    if (!checkEnabled(config)) {
        "FSI disabled".logd(TAG)
        return false
    }

    // 2. 检查冷却期
    if (!checkQuietPeriod(config)) {
        "Quiet period not met".logd(TAG)
        return false
    }

    // 3. 检查时间窗口
    if (!checkTimeWindow(config)) {
        "Outside time window".logd(TAG)
        return false
    }

    // 4. 检查触发次数
    if (!checkTriggerCount(config)) {
        "Max trigger count reached".logd(TAG)
        return false
    }

    "All FSI conditions met".logd(TAG)
    return true
}

private fun checkEnabled(config: FsiConfig): Boolean {
    return config.enabled
}

/**
 * 检查冷却期条件（两个条件都需要满足）
 *
 * 条件 1: 距离上次使用 APP（关闭首页）时间 >= quietPeriodHours（沉默时间）
 * 条件 2: 距离安装时间 >= 24 小时（固定值）
 */
private fun checkQuietPeriod(config: FsiConfig): Boolean {

    if (hasClickNotify()) {
        if (BuildState.debug)
            "user has click notification".logd(TAG)
        return false
    }

    val currentTime = System.currentTimeMillis()

    // ========== 条件 1: 检查沉默时间 ==========
    val lastActiveTime = getLastActiveTime()

    val quietPeriodMillis = config.quietPeriodHours * 3600 * 1000L
    val timeSinceActive = currentTime - lastActiveTime

    if (timeSinceActive < quietPeriodMillis) {
        if (BuildState.debug)
            "User inactive for ${timeSinceActive / 3600000}h, need ${config.quietPeriodHours}h".logd(
                TAG
            )
        return false
    }

    "✓ Quiet period met: ${timeSinceActive / 3600000}h >= ${config.quietPeriodHours}h".logd(TAG)

    // ========== 条件 2: 检查安装冷却期（固定 24h）==========
    val firstInstallTime = getFirstInstallTime()

    if (firstInstallTime == 0L) {
        if (BuildState.debug)
            "Failed to get install time".logd(TAG)
        return false
    }

    val installCooldownMillis = INSTALL_COOLDOWN_HOURS * 3600 * 1000L
    val timeSinceInstall = currentTime - firstInstallTime

    if (timeSinceInstall < installCooldownMillis) {
        if (BuildState.debug)
            "Installed ${timeSinceInstall / 3600000}h ago, need ${INSTALL_COOLDOWN_HOURS}h".logd(TAG)
        return false
    }

    if (BuildState.debug)
        "✓ Install cooldown met: ${timeSinceInstall / 3600000}h >= ${INSTALL_COOLDOWN_HOURS}h".logd(
            TAG
        )

    return true
}

private fun checkTimeWindow(config: FsiConfig): Boolean {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    return config.isInTimeWindow(currentHour)
}

private fun checkTriggerCount(config: FsiConfig): Boolean {
    val currentCount = getTriggerCount()
    return currentCount < config.maxTriggerCount
}
