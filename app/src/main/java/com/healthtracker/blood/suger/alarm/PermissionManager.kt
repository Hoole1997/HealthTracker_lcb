package com.healthtracker.blood.suger.alarm

import ads_mobile_sdk.ac
import android.R.attr.data
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import java.lang.ref.WeakReference
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.constants.FSI_PERMISSION_POSITION
import com.healthtracker.blood.suger.constants.HAS_NOTIFICATION_PERMISSION
import com.healthtracker.blood.suger.constants.HAS_REPORT_NOTIFICATION_REVOKED
import com.healthtracker.blood.suger.ui.act.AlarmManageActivity
import com.healthtracker.blood.suger.ui.act.MainActivity
import com.healthtracker.blood.suger.ui.act.SplashActivity
import com.healthtracker.blood.suger.ui.dialog.FSIPermissionDialog
import com.healthtracker.blood.suger.ui.dialog.NotificationPermissionDialog
import com.healthtracker.blood.suger.util.pushRequest
import com.healthtracker.blood.suger.util.pushResult
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.util.PermissionUtils
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.isLeast13
import com.hjq.permissions.permission.PermissionLists
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager
import net.corekit.core.utils.ConfigRemoteManager
import java.lang.ref.SoftReference
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
    
    // FSI 权限 Launcher（使用弱引用避免内存泄漏）
    private var fsiLauncher: SoftReference<ActivityResultLauncher<Intent>>? = null
    
    companion object {
        const val TAG = "PermissionManager"

        // 权限请求码
        const val REQUEST_CODE_NOTIFICATION = 1001
        private const val SPLASH_HAS_SHOW_FSI_REQUEST = "splash_has_show_fsi_request"
        private const val CUSTOM_NOTIFICATION_PERMISSION_REQUEST_TIMES = "custom_notification_permission_request_times"
        // FSI权限状态存储键
        private const val PREF_FSI_TOTAL_REQUESTS = "fsi_total_requests"
        private const val PREF_FSI_SESSION_REQUESTS = "fsi_session_requests"
        private const val PREF_APP_SESSION_ID = "app_session_id"


        // FSI权限请求限制
        private const val FSI_MAX_TOTAL_REQUESTS = 3
        private const val FSI_MAX_SESSION_REQUESTS = 1
        private const val MAX_CUSTOM_NOTIFICATION_PERMISSION_REQUEST = 2

        // 权限检查结果
        enum class PermissionStatus {
            GRANTED,        // 已授权
            DENIED,         // 被拒绝
            NOT_REQUIRED    // 不需要（版本不支持）
        }
    }

    private var hasShowCustomNotificationRequest = false
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
     * 初始化 FSI 权限 Launcher
     * 必须在 Activity.onCreate() 中调用
     * 智能判断：基于完整的业务规则判断是否需要注册（版本、权限状态、请求次数等）
     * 
     * 注意：因为 registerForActivityResult() 必须同步调用，所以先注册 Launcher，
     * 然后在协程中异步检查业务规则，如果不需要则清空
     * 
     * @param activity FragmentActivity 实例
     * @param onResult 权限结果回调，参数为是否授予权限
     */
    fun initFSILauncher(
        activity: FragmentActivity, 
        onResult: (Boolean) -> Unit
    ) {
        activity.lifecycleScope.launch {
            if (shouldRequestFSIPermission(activity)) {
                // 先注册 Launcher（必须同步进行）
                val launcher = activity.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    // 检查FSI权限状态
                    val granted = isFSIPermissionGranted()

                    if (granted) {
                        if(BuildState.debug) "FSI permission granted after settings".logd(TAG)
                        // TODO: 暂不上报 FSI 权限埋点
                        // ReportDataManager.reportData("permission_full_screen_result", mapOf("result" to "allow"))
                    } else {
                        if(BuildState.debug) "FSI permission still denied after settings".logw(TAG)
                    }

                    onResult(granted)
                    // 使用后立即清空引用，帮助 GC 回收
                    cleanFSILauncher()
                }

                // 使用弱引用存储
                if(BuildState.debug) "注册,FSI授权回调".logw(TAG)
                fsiLauncher = SoftReference(launcher)
                if(BuildState.debug) "FSI launcher initialized and ready".logd(TAG)

            } else {
                if(BuildState.debug) "不需要请求FSI权限，注销授权结果回调".logd(TAG)
                onResult.invoke(true)
            }
        }

        

    }


    private fun cleanFSILauncher(){
        if(fsiLauncher != null){
            if(BuildState.debug) "注销,FSI授权回调".logw(TAG)
            fsiLauncher?.clear()
            fsiLauncher = null
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
     * 使用前必须先调用 initFSILauncher() 初始化
     */
    fun requestFSIPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                
                // 从弱引用中获取 Launcher
                val launcher = fsiLauncher?.get()
                if (launcher == null) {
                    if(BuildState.debug) "FSI launcher not initialized or has been garbage collected".loge(TAG)
                    return false
                }
                
                launcher.launch(intent)
                App.INSTANCE.isGoSetting = true
                if(BuildState.debug) "Requesting FSI permission via settings".logd(TAG)
                return true
            } catch (e: Exception) {
                if(BuildState.debug) "Failed to open FSI settings: ${e.message}".loge(TAG)
                return false
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
    suspend fun shouldRequestFSIPermission(activity: FragmentActivity): Boolean {
        // 如果已经有权限，不需要请求
        if (isFSIPermissionGranted()) {
            return false
        }

       val result = if(activity is SplashActivity || activity is MainActivity){
            if(BuildState.debug) "首页或启动页，检查FSI请求弹窗次数限制".logd(TAG)
            val state = getFSIPermissionState()

            val sessionLimit = when{
                isSplashCheckFsi() && !SpUtils.getBoolean(SPLASH_HAS_SHOW_FSI_REQUEST,false) -> FSI_MAX_SESSION_REQUESTS + 1
                else -> FSI_MAX_SESSION_REQUESTS
            }
            // 检查各种限制条件
            val withinTotalLimit = state.totalRequestCount < FSI_MAX_TOTAL_REQUESTS
            val withinSessionLimit = state.sessionRequestCount < sessionLimit

           val shouldRequest = withinTotalLimit && withinSessionLimit
            if (BuildState.debug) ("FSI permission request check: total=${state.totalRequestCount}/$FSI_MAX_TOTAL_REQUESTS, session=${state.sessionRequestCount}/$sessionLimit, shouldRequest=$shouldRequest").logd(TAG)
            shouldRequest
        }else{
           if(BuildState.debug) "非首页或启动页，不检查FSI请求弹窗次数限制".logd(TAG)
           true
        }
        return result
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

    private suspend fun isSplashCheckFsi() = ConfigRemoteManager.getInt(FSI_PERMISSION_POSITION,0) == 0

    /**
     * 重置FSI权限状态（用于测试或重置用户选择）
     */
    fun resetFSIPermissionState() {
        SpUtils.remove(PREF_FSI_TOTAL_REQUESTS)
        SpUtils.remove(PREF_FSI_SESSION_REQUESTS)
        if(BuildState.debug) "FSI permission state reset".logd(TAG)
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
   fun checkNotificationPermission(activity: FragmentActivity,onGoSetting:(() -> Unit)? = null, onComplete:(Boolean) -> Unit) {

        val position = when (activity) {
            is SplashActivity -> "AppStart"
            is MainActivity -> "Home"
            is AlarmManageActivity -> "alarm"
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
                        activity.lifecycleScope.launch {
                            checkFSIPermission(activity,onComplete)
                        }

                    }else{
                        if (BuildState.debug) "Notification permission denied by user, is forever:$isDoNotAsk".logd(
                            TAG
                        )
                        pushResult(if (isDoNotAsk) "denied_forever" else "denied", position)
                        if (isDoNotAsk && activity !is SplashActivity) {
                            if(BuildState.debug) "非启动页，永久拒绝，尝试自定义弹窗请求通知权限".logd(TAG)
                            showCustomNotificationRequest(activity,onGoSetting,onComplete)
                        } else {
                            cleanFSILauncher()
                            onComplete.invoke(false)
                        }

                    }

                }
            }else{
                if(BuildState.debug) "13以下设备，直接完成通知权限流程".logd(TAG)
                cleanFSILauncher()
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
                onComplete.invoke(true)
            } else {
                if (BuildState.debug) "13及以上设备".logd(TAG)
                activity.lifecycleScope.launch {
                    checkFSIPermission(activity,onComplete)
                }
            }

        }
    }


    private suspend fun checkFSIPermission(activity: FragmentActivity,onComplete:(Boolean) -> Unit){
        val isSplash = activity is SplashActivity

        if(isSplash){
            if(!isSplashCheckFsi()){
                if(BuildState.debug) "FSI_permission_position = 1,启动页不需要检查FSI权限".logd(TAG)
                cleanFSILauncher()
                onComplete.invoke(true)
                return
            }else{
                if(BuildState.debug) "FSI_permission_position = 0,启动页需要检查FSI权限".logd(TAG)
            }
            if(SpUtils.getBoolean(SPLASH_HAS_SHOW_FSI_REQUEST,false)){
                if(BuildState.debug) "启动页页已请求过FSI权限，不再请求".logd(TAG)
                cleanFSILauncher()
                onComplete.invoke(true)
                return
            }
        }else{
            if(BuildState.debug) "非启动页页检查FSI权限".logd(TAG)
        }
        if(shouldRequestFSIPermission(activity)){
            showFSIPermissionExplanationDialog(activity,onComplete)
            SpUtils.putBoolean(SPLASH_HAS_SHOW_FSI_REQUEST,true)
        }else{
            if(BuildState.debug) "有全屏通知权限或系统版本不支持全屏通知".logd(TAG)
            cleanFSILauncher()
            onComplete.invoke(true)

        }
    }

    /**
     * 显示FSI权限说明对话框
     */
    private fun showFSIPermissionExplanationDialog(activity: FragmentActivity,onComplete:(Boolean) -> Unit) {
        if(BuildState.debug) "显示FSI权限说明对话框".logd(TAG)
        if (activity is SplashActivity || activity is MainActivity) {
            if (BuildState.debug) "首页或启动页，记录FSI权限请求次数".logd(TAG)
            recordFSIPermissionRequest()
        } else {
            if (BuildState.debug) "非首页或启动页，不记录FSI权限请求次数".logd(TAG)
        }
        FSIPermissionDialog.show(
            activity.supportFragmentManager,
            onAllowPermission = {
                if (BuildState.debug) "前往FSI授权页面".logd(TAG)
                if (!requestFSIPermission()) {
                    if (BuildState.debug) "跳转FSI授权页面失败".logd(TAG)
                    cleanFSILauncher()
                    onComplete.invoke(true)
                }
            },
            onDenyPermission = {
                if (BuildState.debug) "用户拒绝授权FSI权限".logd(TAG)
                cleanFSILauncher()
                onComplete.invoke(true)
            }
        )
    }

    private fun showCustomNotificationRequest(activity: FragmentActivity,onGoSetting:(() -> Unit)? = null, onComplete: (Boolean) -> Unit){
        if (activity is MainActivity) {
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
