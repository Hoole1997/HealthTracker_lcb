package com.healthtracker.blood.suger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.healthtracker.blood.suger.service.HealthService
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.SpUtils

/**
 * 系统启动接收器
 * 监听系统启动完成事件，自动启动健康服务
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                "System boot completed".logd(TAG)

                // 检查用户是否启用了健康服务
                val enabled = SpUtils.getBoolean(
                    HealthServiceConstants.PREF_HEALTH_SERVICE_ENABLED,
                    false
                )

                if (enabled) {
                    try {
                        val serviceIntent = Intent(context, HealthService::class.java)

                        // Android 8.0+ 使用 startForegroundService
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }

                        "Health service auto-started after boot".logd(TAG)

                    } catch (e: Exception) {
                        "Failed to auto-start health service: ${e.message}".loge(TAG)
                    }
                } else {
                    "Health service not enabled, skip auto-start".logd(TAG)
                }
            }
        }
    }
}
