package com.healthtracker.blood.suger.alarm

import android.Manifest
import android.app.Activity
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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限管理器
 * 负责检查和申请应用所需的权限
 *
 * 主要功能：
 * 1. 通知权限管理
 * 2. 权限状态检查
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "PermissionManager"
        
        // 权限请求码
        const val REQUEST_CODE_NOTIFICATION = 1001
        
        // 权限检查结果
        enum class PermissionStatus {
            GRANTED,        // 已授权
            DENIED,         // 被拒绝
            NOT_REQUIRED    // 不需要（版本不支持）
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
}

