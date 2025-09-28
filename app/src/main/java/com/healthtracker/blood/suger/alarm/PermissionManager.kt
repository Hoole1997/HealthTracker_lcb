package com.healthtracker.blood.suger.alarm

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 闹钟权限管理器
 * 负责检查和申请系统级闹钟所需的各种权限
 * 
 * 主要功能：
 * 1. 精确闹钟权限检查和申请
 * 2. 通知权限管理
 * 3. 电池优化白名单管理
 * 4. 权限状态统一检查
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "PermissionManager"
        
        // 权限请求码
        const val REQUEST_CODE_NOTIFICATION = 1001
        const val REQUEST_CODE_EXACT_ALARM = 1002
        const val REQUEST_CODE_BATTERY_OPTIMIZATION = 1003
        
        // 权限检查结果
        enum class PermissionStatus {
            GRANTED,        // 已授权
            DENIED,         // 被拒绝
            NOT_REQUIRED    // 不需要（版本不支持）
        }
    }
    
    /**
     * 检查精确闹钟权限
     * Android 12+ 需要SCHEDULE_EXACT_ALARM权限
     * 
     * @return 权限状态
     */
    fun checkExactAlarmPermission(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) {
                "Exact alarm permission granted".logd(TAG)
                PermissionStatus.GRANTED
            } else {
                "Exact alarm permission denied".logw(TAG)
                PermissionStatus.DENIED
            }
        } else {
            "Exact alarm permission not required for this Android version".logd(TAG)
            PermissionStatus.NOT_REQUIRED
        }
    }
    
    /**
     * 申请精确闹钟权限
     * 跳转到系统设置页面让用户手动开启
     * 
     * @param activity 当前Activity
     */
    fun requestExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                activity.startActivityForResult(intent, REQUEST_CODE_EXACT_ALARM)
                "Requesting exact alarm permission".logd(TAG)
            } catch (e: Exception) {
                "Failed to request exact alarm permission: ${e.message}".loge(TAG)
                // 降级处理：跳转到应用详情页
                openAppSettings(activity)
            }
        }
    }
    
    /**
     * 检查通知权限
     * Android 13+ 需要POST_NOTIFICATIONS权限
     * 
     * @return 权限状态
     */
    fun checkNotificationPermission(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (granted) {
                "Notification permission granted".logd(TAG)
                PermissionStatus.GRANTED
            } else {
                "Notification permission denied".logw(TAG)
                PermissionStatus.DENIED
            }
        } else {
            "Notification permission not required for this Android version".logd(TAG)
            PermissionStatus.NOT_REQUIRED
        }
    }
    
    /**
     * 申请通知权限
     * 
     * @param activity 当前Activity
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION
            )
            "Requesting notification permission".logd(TAG)
        }
    }
    
    /**
     * 检查电池优化白名单状态
     * 
     * @return 是否在白名单中
     */
    fun checkBatteryOptimizationExemption(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            
            if (isIgnoring) {
                "App is in battery optimization whitelist".logd(TAG)
            } else {
                "App is not in battery optimization whitelist".logw(TAG)
            }
            
            isIgnoring
        } else {
            "Battery optimization not available for this Android version".logd(TAG)
            true // 低版本默认认为已豁免
        }
    }
    
    /**
     * 申请电池优化豁免
     * 
     * @param activity 当前Activity
     */
    fun requestBatteryOptimizationExemption(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                activity.startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATION)
                "Requesting battery optimization exemption".logd(TAG)
            } catch (e: Exception) {
                "Failed to request battery optimization exemption: ${e.message}".loge(TAG)
                // 降级处理：跳转到电池优化设置页
                openBatteryOptimizationSettings(activity)
            }
        }
    }
    
    /**
     * 检查所有必需权限是否已授权
     * 
     * @return 权限检查结果
     */
    fun checkAllPermissions(): AlarmPermissionResult {
        val exactAlarmStatus = checkExactAlarmPermission()
        val notificationStatus = checkNotificationPermission()
        val batteryOptimized = checkBatteryOptimizationExemption()
        
        return AlarmPermissionResult(
            exactAlarmGranted = exactAlarmStatus == PermissionStatus.GRANTED || exactAlarmStatus == PermissionStatus.NOT_REQUIRED,
            notificationGranted = notificationStatus == PermissionStatus.GRANTED || notificationStatus == PermissionStatus.NOT_REQUIRED,
            batteryOptimizationExempted = batteryOptimized,
            allGranted = (exactAlarmStatus == PermissionStatus.GRANTED || exactAlarmStatus == PermissionStatus.NOT_REQUIRED) &&
                        (notificationStatus == PermissionStatus.GRANTED || notificationStatus == PermissionStatus.NOT_REQUIRED) &&
                        batteryOptimized
        )
    }
    
    /**
     * 批量申请所有必需权限
     * 
     * @param activity 当前Activity
     * @param callback 权限申请结果回调
     */
    fun requestAllPermissions(activity: Activity, callback: (AlarmPermissionResult) -> Unit) {
        val currentResult = checkAllPermissions()
        
        when {
            currentResult.allGranted -> {
                "All permissions already granted".logd(TAG)
                callback(currentResult)
            }
            !currentResult.notificationGranted -> {
                "Requesting notification permission first".logd(TAG)
                requestNotificationPermission(activity)
            }
            !currentResult.exactAlarmGranted -> {
                "Requesting exact alarm permission".logd(TAG)
                requestExactAlarmPermission(activity)
            }
            !currentResult.batteryOptimizationExempted -> {
                "Requesting battery optimization exemption".logd(TAG)
                requestBatteryOptimizationExemption(activity)
            }
        }
    }
    
    /**
     * 处理权限申请结果
     * 
     * @param requestCode 请求码
     * @param permissions 权限数组
     * @param grantResults 授权结果
     * @return 是否处理了该请求
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        return when (requestCode) {
            REQUEST_CODE_NOTIFICATION -> {
                val granted = grantResults.isNotEmpty() && 
                             grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    "Notification permission granted by user".logd(TAG)
                } else {
                    "Notification permission denied by user".logw(TAG)
                }
                true
            }
            else -> false
        }
    }
    
    /**
     * 处理Activity结果
     * 
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @return 是否处理了该请求
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int): Boolean {
        return when (requestCode) {
            REQUEST_CODE_EXACT_ALARM -> {
                val granted = checkExactAlarmPermission() == PermissionStatus.GRANTED
                if (granted) {
                    "Exact alarm permission granted by user".logd(TAG)
                } else {
                    "Exact alarm permission still denied".logw(TAG)
                }
                true
            }
            REQUEST_CODE_BATTERY_OPTIMIZATION -> {
                val exempted = checkBatteryOptimizationExemption()
                if (exempted) {
                    "Battery optimization exemption granted by user".logd(TAG)
                } else {
                    "Battery optimization exemption denied".logw(TAG)
                }
                true
            }
            else -> false
        }
    }
    
    /**
     * 打开应用设置页面
     * 
     * @param activity 当前Activity
     */
    private fun openAppSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
            "Opening app settings".logd(TAG)
        } catch (e: Exception) {
            "Failed to open app settings: ${e.message}".loge(TAG)
        }
    }
    
    /**
     * 打开电池优化设置页面
     * 
     * @param activity 当前Activity
     */
    private fun openBatteryOptimizationSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            activity.startActivity(intent)
            "Opening battery optimization settings".logd(TAG)
        } catch (e: Exception) {
            "Failed to open battery optimization settings: ${e.message}".loge(TAG)
        }
    }
}

/**
 * 闹钟权限检查结果
 * 
 * @property exactAlarmGranted 精确闹钟权限是否已授权
 * @property notificationGranted 通知权限是否已授权
 * @property batteryOptimizationExempted 是否已豁免电池优化
 * @property allGranted 所有权限是否都已授权
 */
data class AlarmPermissionResult(
    val exactAlarmGranted: Boolean,
    val notificationGranted: Boolean,
    val batteryOptimizationExempted: Boolean,
    val allGranted: Boolean
) {
    /**
     * 获取缺失的权限描述
     */
    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (!exactAlarmGranted) missing.add("精确闹钟权限")
        if (!notificationGranted) missing.add("通知权限")
        if (!batteryOptimizationExempted) missing.add("电池优化豁免")
        return missing
    }
}