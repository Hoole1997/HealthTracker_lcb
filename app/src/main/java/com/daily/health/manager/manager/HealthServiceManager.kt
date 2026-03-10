package com.daily.health.manager.manager

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.daily.health.manager.alarm.PermissionManager
import com.daily.health.manager.helper.ResidentNotificationHelper
import com.daily.health.manager.service.HTService
import com.daily.health.manager.service.HealthServiceConstants
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.hasOreo
import net.corekit.core.report.ReportDataManager

/**
 * 健康服务管理器
 * 负责健康服务的启动、停止和状态管理
 */
class HealthServiceManager(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val residentNotificationHelper: ResidentNotificationHelper
) {

    companion object {
        private const val TAG = "HealthServiceManager"
    }
    private var lastTime = 0L
    /**
     * 启动健康服务
     */
    fun startHealthService(from: String = "local_push") {

        if(System.currentTimeMillis() - lastTime < 5000L){
            return
        }

        // 检查通知权限
        if (!permissionManager.isNotificationPermissionGranted()) {
            "Cannot start health service: notification permission not granted".logw(TAG)
            return
        }

        try {
            ReportDataManager.reportData("Notific_Pull", mapOf("topic" to "permanent"))
            val intent = Intent(context, HTService::class.java).apply {
                putExtra(HTService.IS_SILENT,isServiceRunning())
            }

            // Android 8.0+ 使用 startForegroundService
            if (hasOreo()) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            // 记录用户启用了服务
            SpUtils.putBoolean(HealthServiceConstants.PREF_HEALTH_SERVICE_ENABLED, true)
            lastTime = System.currentTimeMillis()

            "Health service started successfully".logd(TAG)

        } catch (e: Exception) {
            "Failed to start health service: ${e.message}".loge(TAG)
            ReportDataManager.reportData("Notific_Show_Fail",mapOf("reason" to "alive_service_${from}_${e.message}"))
        }
    }


    /**
     * 检查健康服务是否正在运行
     */
    fun isServiceRunning(): Boolean {
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (HTService::class.java.name == service.service.className) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            "Failed to check service status: ${e.message}".loge(TAG)
            false
        }
    }

}
