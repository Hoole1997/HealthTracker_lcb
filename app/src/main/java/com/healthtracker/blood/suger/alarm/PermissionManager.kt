package com.healthtracker.blood.suger.alarm

import ads_mobile_sdk.f71
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.constants.FSI_PERMISSION_POSITION
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.util.PermissionUtils
import com.healthtracker.framework.util.SpUtils
import com.hjq.permissions.permission.PermissionLists
import net.corekit.core.report.ReportDataManager
import net.corekit.core.utils.ConfigRemoteManager
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

) {

    private val context = App.INSTANCE
    companion object {
        const val TAG = "PermissionManager"

        // 权限请求码
        const val REQUEST_CODE_NOTIFICATION = 1001
        const val REQUEST_CODE_FSI = 1002

        // FSI权限状态存储键
        private const val PREF_FSI_TOTAL_REQUESTS = "fsi_total_requests"
        private const val PREF_FSI_SESSION_REQUESTS = "fsi_session_requests"
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
        val sessionRequestCount: Int = 0
    )

    /**
     * 检查通知权限是否已授权（简化版本）
     *
     * @return 是否已授权通知权限
     */
    fun isNotificationPermissionGranted() = PermissionUtils.hasPermission(App.INSTANCE,
        PermissionLists.getPostNotificationsPermission())
    


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

                if (granted) {
                    if(BuildState.debug) "FSI permission granted after settings".logd(TAG)
                    ReportDataManager.reportData("permission_full_screen_result",mapOf("result" to "allow"))
                } else {
                    if(BuildState.debug)  "FSI permission still denied after settings".logw(TAG)
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
    fun openAppSettings(activity: Activity): Boolean {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
            if(BuildState.debug) "Opening app settings".logd(TAG)
            return true
        } catch (e: Exception) {
            if(BuildState.debug) "Failed to open app settings: ${e.message}".loge(TAG)
            return false
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
                try {
                    // Android 14+ 需要检查运行时权限
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val canUseFullScreenIntent = notificationManager.canUseFullScreenIntent()

                    if (canUseFullScreenIntent) {
                        if(BuildState.debug) "FSI permission granted".logd(TAG)
                        PermissionStatus.GRANTED
                    } else {
                        if(BuildState.debug) "FSI permission denied".logw(TAG)
                        PermissionStatus.DENIED
                    }
                }catch (_: Throwable){
                    PermissionStatus.DENIED
                }
            } else {
                // Android 10-13 只需要Manifest声明
                if(BuildState.debug)  "FSI permission not required for runtime (Android 10-13)".logd(TAG)
                PermissionStatus.GRANTED
            }
        } else {
            if(BuildState.debug) "FSI not supported on this Android version".logd(TAG)
            PermissionStatus.NOT_REQUIRED
        }
    }

    /**
     * 申请全屏通知权限
     * Android 14+ 需要跳转到系统设置
     *
     * @param activity 当前Activity
     */
    fun requestFSIPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.fromParts("package",context.packageName,null)
                }
                activity.startActivityForResult(intent, REQUEST_CODE_FSI)
                App.INSTANCE.isGoSetting = true
                if(BuildState.debug) "Requesting FSI permission via settings".logd(TAG)
                return true
            } catch (e: Exception) {
                if(BuildState.debug) "Failed to open FSI settings: ${e.message}".loge(TAG)
                // 降级到通用应用设置
               return openAppSettings(activity)
            }
        } else {
            if(BuildState.debug) "FSI permission request not needed for this Android version".logd(TAG)
            return false
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
     * 初始化新Session（应用启动时调用），确保一次冷启动只会执行一次
     */
    fun initializeSession() {
        val newSessionId = UUID.randomUUID().toString()
        if(BuildState.debug) "New session initialized: $newSessionId".logd(TAG)
        SpUtils.putString(PREF_APP_SESSION_ID, newSessionId)
        SpUtils.putInt(PREF_FSI_SESSION_REQUESTS, 0)
    }

    /**
     * 获取FSI权限状态
     */
    fun getFSIPermissionState(): FSIPermissionState {
        return FSIPermissionState(
            totalRequestCount = SpUtils.getInt(PREF_FSI_TOTAL_REQUESTS, 0),
            sessionRequestCount = SpUtils.getInt(PREF_FSI_SESSION_REQUESTS, 0)
        )
    }

    /**
     * 是否应该请求FSI权限
     *
     * @return true if should request, false otherwise
     */
    suspend fun shouldRequestFSIPermission(): Boolean {
        // 如果已经有权限，不需要请求
        if (isFSIPermissionGranted()) {
            return false
        }

        val state = getFSIPermissionState()

        val sessionLimit = when{
            isSplashCheckFsi() -> FSI_MAX_SESSION_REQUESTS + 1
            else -> FSI_MAX_SESSION_REQUESTS
        }
        // 检查各种限制条件
        val withinTotalLimit = state.totalRequestCount < FSI_MAX_TOTAL_REQUESTS
        val withinSessionLimit = state.sessionRequestCount < sessionLimit

        val shouldRequest = withinTotalLimit && withinSessionLimit

        if (BuildState.debug) ("FSI permission request check: total=${state.totalRequestCount}/$FSI_MAX_TOTAL_REQUESTS, " +
                "session=${state.sessionRequestCount}/$sessionLimit, " +
                "shouldRequest=$shouldRequest").logd(TAG)

        return shouldRequest
    }

    /**
     * 记录FSI权限请求结果
     *
     */
    fun recordFSIPermissionRequest() {
        val state = getFSIPermissionState()

        // 更新计数
        SpUtils.putInt(PREF_FSI_TOTAL_REQUESTS, state.totalRequestCount + 1)
        SpUtils.putInt(PREF_FSI_SESSION_REQUESTS, state.sessionRequestCount + 1)

        if(BuildState.debug) "FSI permission request recorded: total=${state.totalRequestCount + 1}, session=${state.sessionRequestCount + 1}".logd(TAG)

    }

    /**
     * 检查FSI权限是否可用（权限已授权）
     */
    fun isFSIPermissionAvailable(): Boolean {
        return isFSIPermissionGranted()
    }

    suspend fun isSplashCheckFsi() = ConfigRemoteManager.getInt(FSI_PERMISSION_POSITION,0) == 0

    /**
     * 重置FSI权限状态（用于测试或重置用户选择）
     */
    fun resetFSIPermissionState() {
        SpUtils.remove(PREF_FSI_TOTAL_REQUESTS)
        SpUtils.remove(PREF_FSI_SESSION_REQUESTS)
        if(BuildState.debug) "FSI permission state reset".logd(TAG)
    }
}
