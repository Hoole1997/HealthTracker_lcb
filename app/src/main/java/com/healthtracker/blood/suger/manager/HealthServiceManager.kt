package com.healthtracker.blood.suger.manager

import android.app.ActivityManager
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.os.Build
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.service.HealthService
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.hasOreo
import dagger.hilt.android.qualifiers.ApplicationContext
import net.corekit.core.report.ReportDataManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 健康服务管理器
 * 负责健康服务的启动、停止和状态管理
 */
@Singleton
class HealthServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {

    companion object {
        private const val TAG = "HealthServiceManager"
    }

    /**
     * 启动健康服务
     */
    fun startHealthService(from: String = "local_push") {
        // 检查通知权限
        if (!permissionManager.isNotificationPermissionGranted()) {
            "Cannot start health service: notification permission not granted".logw(TAG)
            return
        }

        try {
            ReportDataManager.reportData("Notific_Pull", mapOf("topic" to "permanent"))
            val intent = Intent(context, HealthService::class.java).apply {
                putExtra(HealthService.IS_SILENT,isServiceRunning())
            }

            // Android 8.0+ 使用 startForegroundService
            if (hasOreo()) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            // 记录用户启用了服务
            SpUtils.putBoolean(HealthServiceConstants.PREF_HEALTH_SERVICE_ENABLED, true)

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
                if (HealthService::class.java.name == service.service.className) {
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
