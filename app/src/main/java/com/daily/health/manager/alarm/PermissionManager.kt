package com.daily.health.manager.alarm

import androidx.fragment.app.FragmentActivity
import com.daily.health.manager.App
import com.daily.health.manager.constants.HAS_NOTIFICATION_PERMISSION
import com.daily.health.manager.constants.HAS_REPORT_NOTIFICATION_REVOKED
import com.daily.health.manager.face.act.AlarmManageScreen
import com.daily.health.manager.face.act.MainAct
import com.daily.health.manager.face.act.SplashScreen
import com.daily.health.manager.face.dialog.NotificationPermissionDialog
import com.daily.health.manager.feature.NotificationFeatureSwitch
import com.daily.health.manager.util.pushRequest
import com.daily.health.manager.util.pushResult
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.util.PermissionUtils
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.isLeast13
import com.hjq.permissions.permission.PermissionLists
import net.corekit.core.report.ReportDataManager

/**
 * 权限管理器
 * 负责检查和申请应用所需的权限
 *
 * 主要功能：
 * 1. 通知权限管理
 * 2. 权限状态检查
 */
class PermissionManager {

    companion object {
        const val TAG = "PermissionManager"

        // 权限请求码
        const val REQUEST_CODE_NOTIFICATION = 1001
        private const val CUSTOM_NOTIFICATION_PERMISSION_REQUEST_TIMES = "custom_notification_permission_request_times"
        private const val MAX_CUSTOM_NOTIFICATION_PERMISSION_REQUEST = 2
    }

    private var hasShowCustomNotificationRequest = false

    /**
     * 检查通知权限是否已授权（简化版本）
     *
     * @return 是否已授权通知权限
     */
    fun isNotificationPermissionGranted(): Boolean {
        if (!NotificationFeatureSwitch.notificationsEnabled) return false
        return PermissionUtils.hasPermission(
            App.INSTANCE,
            PermissionLists.getPostNotificationsPermission()
        )
    }

    /**
     * 检查通知权限是否被撤销
     */
    private fun checkRevoked(){
        if(SpUtils.getBoolean(HAS_NOTIFICATION_PERMISSION,false) && SpUtils.getBoolean(
                HAS_REPORT_NOTIFICATION_REVOKED,false
            )){
            if(BuildState.debug) "notification permission is revoked".logd(TAG)
            SpUtils.putBoolean(HAS_REPORT_NOTIFICATION_REVOKED,true)
            ReportDataManager.reportData("notify_permission_revoked", mapOf())
        }
    }

    /**
     * 检查通知权限
     */
    fun checkNotificationPermission(activity: FragmentActivity, onGoSetting:(() -> Unit)? = null, onComplete:(Boolean) -> Unit) {
        if (!NotificationFeatureSwitch.notificationPermissionPromptEnabled) {
            onComplete(true)
            return
        }

        val position = when (activity) {
            is SplashScreen -> "AppStart"
            is MainAct -> "Home"
            is AlarmManageScreen -> "alarm"
            else -> "other"
        }

        val permissions = PermissionLists.getPostNotificationsPermission()
        if(!PermissionUtils.hasPermission(activity, permissions)){
            if(BuildState.debug) "没有通知权限".logd(TAG)
            checkRevoked()
            if(isLeast13()){
                if(BuildState.debug) "13及以上设备，尝试请求通知权限".logd(TAG)
                pushRequest("Appstart")
                PermissionUtils.requestNotificationPermission(activity){ isGrand, isDoNotAsk ->
                    if(isGrand){
                        if(BuildState.debug) "Notification permission granted by user".logd(TAG)
                        SpUtils.putBoolean(HAS_NOTIFICATION_PERMISSION,true)
                        pushResult("allow",position)
                        onComplete.invoke(true)
                    }else{
                        if (BuildState.debug) "Notification permission denied by user, is forever:$isDoNotAsk".logd(
                            TAG
                        )
                        pushResult(if (isDoNotAsk) "denied_forever" else "denied", position)
                        if (isDoNotAsk && activity !is SplashScreen) {
                            if(BuildState.debug) "非启动页，永久拒绝，尝试自定义弹窗请求通知权限".logd(TAG)
                            showCustomNotificationRequest(activity,onGoSetting,onComplete)
                        } else {
                            onComplete.invoke(false)
                        }
                    }
                }
            }else{
                if(BuildState.debug) "13以下设备，直接完成通知权限流程".logd(TAG)
                onComplete.invoke(false)
            }

        }else{
            if(BuildState.debug) "有通知权限".logd(TAG)
            val keyFlag = "has_send_def_allow"
            if (!isLeast13()) {
                if (BuildState.debug) "13以下设备".logd(TAG)
                if(!SpUtils.getBoolean(keyFlag, false)){
                    if (BuildState.debug) "上报默认授权通知权限".logd(TAG)
                    SpUtils.putBoolean(keyFlag, true)
                    pushResult("allow1", position)
                    SpUtils.putBoolean(HAS_NOTIFICATION_PERMISSION,true)
                }else{
                    if (BuildState.debug) "已经上报默认授权通知权限".logd(TAG)
                }
            }
            onComplete.invoke(true)
        }
    }

    private fun showCustomNotificationRequest(activity: FragmentActivity, onGoSetting:(() -> Unit)? = null, onComplete: (Boolean) -> Unit){
        if (activity is MainAct) {
            if (hasShowCustomNotificationRequest) {
                if (BuildState.debug) "本次启动，用户已请求过通知权限，不再请求".logd(
                    TAG
                )
                onComplete.invoke(false)
                return
            }
            val count = SpUtils.getInt(CUSTOM_NOTIFICATION_PERMISSION_REQUEST_TIMES, 0)
            if (count >= MAX_CUSTOM_NOTIFICATION_PERMISSION_REQUEST) {
                if (BuildState.debug) "首页，自定义通知权限请求弹窗，达到最大请求次数($count/$MAX_CUSTOM_NOTIFICATION_PERMISSION_REQUEST)，不再请求".logd(
                    TAG
                )
                onComplete.invoke(false)
                return
            }
            SpUtils.putInt(CUSTOM_NOTIFICATION_PERMISSION_REQUEST_TIMES, count + 1)
            hasShowCustomNotificationRequest = true
        } else {
            if (BuildState.debug) "非首页(闹钟设置页面)，自定义通知权限请求弹窗，不受限制".logd(TAG)
        }
        NotificationPermissionDialog.show(activity.supportFragmentManager, onGoToSettings = {
            PermissionUtils.openPermissionSettings(activity, arrayOf(PermissionLists.getPostNotificationsPermission()))
            onGoSetting?.invoke()
            App.INSTANCE.isGoSetting = true
        }) {
            onComplete.invoke(false)
        }
    }
}
