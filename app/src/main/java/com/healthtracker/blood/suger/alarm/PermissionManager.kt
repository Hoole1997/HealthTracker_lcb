package com.healthtracker.blood.suger.alarm

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.util.SpUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限管理器
 * 负责检查和申请应用所需的权限
 *
 * 主要功能：
 * 1. 通知权限管理
 * 2. 全屏通知(FSI)权限管理
 * 3. 权限状态检查
 * 4. 智能权限请求策略
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "PermissionManager"

        // 权限请求码
        const val REQUEST_CODE_NOTIFICATION = 1001
        const val REQUEST_CODE_FSI = 1002

        // FSI权限状态存储键
        private const val PREF_FSI_TOTAL_REQUESTS = "fsi_total_requests"
        private const val PREF_FSI_SESSION_REQUESTS = "fsi_session_requests"
        private const val PREF_FSI_LAST_REQUEST_TIME = "fsi_last_request_time"
        private const val PREF_FSI_USER_DENIED = "fsi_user_permanently_denied"
        private const val PREF_APP_SESSION_ID = "app_session_id"

        // FSI权限请求限制
        private const val FSI_MAX_TOTAL_REQUESTS = 3
        private const val FSI_MAX_SESSION_REQUESTS = 1

        // 权限检查结果
        enum class PermissionStatus {
            GRANTED,        // 已授权
            DENIED,         // 被拒绝
            NOT_REQUIRED    // 不需要（版本不支持）
        }
    }

    /**
     * FSI权限状态数据类
     */
    data class FSIPermissionState(
        val totalRequestCount: Int = 0,
        val sessionRequestCount: Int = 0,
        val lastRequestTime: Long = 0,
        val userPermanentlyDenied: Boolean = false
    )
    
    
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
     * 检查通知权限是否已授权（简化版本）
     *
     * @return 是否已授权通知权限
     */
    fun isNotificationPermissionGranted(): Boolean {
        val status = checkNotificationPermission()
        return status == PermissionStatus.GRANTED || status == PermissionStatus.NOT_REQUIRED
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
     * 处理Activity返回结果（用于FSI权限）
     *
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @return 是否处理了该请求
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int): Boolean {
        return when (requestCode) {
            REQUEST_CODE_FSI -> {
                // 检查FSI权限状态
                val granted = isFSIPermissionGranted()
                recordFSIPermissionRequest(granted)

                if (granted) {
                    "FSI permission granted after settings".logd(TAG)
                } else {
                    "FSI permission still denied after settings".logw(TAG)
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
    fun openAppSettings(activity: Activity) {
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


    fun goToNotifySetting(context: Context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startActivity(Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)
            })
        }
    }

    // ==================== FSI权限管理 ====================

    /**
     * 检查全屏通知(FSI)权限
     * Android 10+ 支持FSI，Android 14+ 需要用户授权
     *
     * @return 权限状态
     */
    fun checkFSIPermission(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ 需要检查运行时权限
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val canUseFullScreenIntent = notificationManager.canUseFullScreenIntent()

                if (canUseFullScreenIntent) {
                    "FSI permission granted".logd(TAG)
                    PermissionStatus.GRANTED
                } else {
                    "FSI permission denied".logw(TAG)
                    PermissionStatus.DENIED
                }
            } else {
                // Android 10-13 只需要Manifest声明
                "FSI permission not required for runtime (Android 10-13)".logd(TAG)
                PermissionStatus.GRANTED
            }
        } else {
            "FSI not supported on this Android version".logd(TAG)
            PermissionStatus.NOT_REQUIRED
        }
    }

    /**
     * 申请全屏通知权限
     * Android 14+ 需要跳转到系统设置
     *
     * @param activity 当前Activity
     */
    fun requestFSIPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.fromParts("package",context.packageName,null)
                }
                activity.startActivityForResult(intent, REQUEST_CODE_FSI)
                "Requesting FSI permission via settings".logd(TAG)
            } catch (e: Exception) {
                "Failed to open FSI settings: ${e.message}".loge(TAG)
                // 降级到通用应用设置
                openAppSettings(activity)
            }
        } else {
            "FSI permission request not needed for this Android version".logd(TAG)
        }
    }

    /**
     * 检查FSI权限是否已授权（简化版本）
     *
     * @return 是否已授权FSI权限
     */
    fun isFSIPermissionGranted(): Boolean {
        val status = checkFSIPermission()
        return status == PermissionStatus.GRANTED || status == PermissionStatus.NOT_REQUIRED
    }

    // ==================== FSI权限智能请求策略 ====================

    /**
     * 获取当前应用Session ID
     */
    private fun getCurrentSessionId(): String {
        val existingSessionId = SpUtils.getString(PREF_APP_SESSION_ID, "")
        return existingSessionId.ifEmpty {
            val newSessionId = UUID.randomUUID().toString()
            SpUtils.putString(PREF_APP_SESSION_ID, newSessionId)
            newSessionId
        }
    }

    /**
     * 初始化新Session（应用启动时调用）
     */
    fun initializeSession() {
        val currentSessionId = getCurrentSessionId()
        val lastSessionId = SpUtils.getString("last_session_id", "")

        if (currentSessionId != lastSessionId) {
            // 新Session，重置session计数
            SpUtils.putString("last_session_id", currentSessionId)
            SpUtils.putInt(PREF_FSI_SESSION_REQUESTS, 0)
            "New session initialized: $currentSessionId".logd(TAG)
        }
    }

    /**
     * 获取FSI权限状态
     */
    fun getFSIPermissionState(): FSIPermissionState {
        return FSIPermissionState(
            totalRequestCount = SpUtils.getInt(PREF_FSI_TOTAL_REQUESTS, 0),
            sessionRequestCount = SpUtils.getInt(PREF_FSI_SESSION_REQUESTS, 0),
            lastRequestTime = SpUtils.getLong(PREF_FSI_LAST_REQUEST_TIME, 0),
            userPermanentlyDenied = SpUtils.getBoolean(PREF_FSI_USER_DENIED, false)
        )
    }

    /**
     * 是否应该请求FSI权限
     *
     * @return true if should request, false otherwise
     */
    fun shouldRequestFSIPermission(): Boolean {
        // 如果已经有权限，不需要请求
        if (isFSIPermissionGranted()) {
            return false
        }

        val state = getFSIPermissionState()

        // 检查各种限制条件
        val withinTotalLimit = state.totalRequestCount < FSI_MAX_TOTAL_REQUESTS
        val withinSessionLimit = state.sessionRequestCount < FSI_MAX_SESSION_REQUESTS
        val notPermanentlyDenied = !state.userPermanentlyDenied

        val shouldRequest = withinTotalLimit && withinSessionLimit && notPermanentlyDenied

        "FSI permission request check: total=${state.totalRequestCount}/$FSI_MAX_TOTAL_REQUESTS, " +
                "session=${state.sessionRequestCount}/$FSI_MAX_SESSION_REQUESTS, " +
                "denied=${state.userPermanentlyDenied}, shouldRequest=$shouldRequest".logd(TAG)

        return shouldRequest
    }

    /**
     * 记录FSI权限请求结果
     *
     * @param granted 用户是否授权
     */
    fun recordFSIPermissionRequest(granted: Boolean) {
        val state = getFSIPermissionState()
        val currentTime = System.currentTimeMillis()

        // 更新计数
        SpUtils.putInt(PREF_FSI_TOTAL_REQUESTS, state.totalRequestCount + 1)
        SpUtils.putInt(PREF_FSI_SESSION_REQUESTS, state.sessionRequestCount + 1)
        SpUtils.putLong(PREF_FSI_LAST_REQUEST_TIME, currentTime)

        if (!granted) {
            // 如果用户拒绝，检查是否应该标记为永久拒绝
            val newTotalCount = state.totalRequestCount + 1
            if (newTotalCount >= FSI_MAX_TOTAL_REQUESTS) {
                SpUtils.putBoolean(PREF_FSI_USER_DENIED, true)
                "User permanently denied FSI permission after $newTotalCount attempts".logw(TAG)
            }
        } else {
            // 用户授权了，清除拒绝标记
            SpUtils.putBoolean(PREF_FSI_USER_DENIED, false)
            "User granted FSI permission".logd(TAG)
        }

        "FSI permission request recorded: granted=$granted, " +
                "total=${state.totalRequestCount + 1}, session=${state.sessionRequestCount + 1}".logd(TAG)
    }

    /**
     * 检查FSI权限是否可用（权限已授权且不在限制中）
     */
    fun isFSIPermissionAvailable(): Boolean {
        return isFSIPermissionGranted() && !getFSIPermissionState().userPermanentlyDenied
    }

    /**
     * 重置FSI权限状态（用于测试或重置用户选择）
     */
    fun resetFSIPermissionState() {
        SpUtils.remove(PREF_FSI_TOTAL_REQUESTS)
        SpUtils.remove(PREF_FSI_SESSION_REQUESTS)
        SpUtils.remove(PREF_FSI_LAST_REQUEST_TIME)
        SpUtils.remove(PREF_FSI_USER_DENIED)
        "FSI permission state reset".logd(TAG)
    }
}

