package com.daily.health.manager.face.launch

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ActivityUtils
import com.daily.health.manager.constants.KEY_FROM_SHORTCUT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_CONTENT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_FROM
import com.daily.health.manager.constants.LANDING_NOTIFICATION_TITLE
import com.daily.health.manager.constants.UNINSTALL
import com.daily.health.manager.face.act.GuideAct
import com.daily.health.manager.face.act.MainAct
import com.daily.health.manager.face.act.UninstallResenActivity
import com.daily.health.manager.face.tracker.trackUninstallClick
import com.daily.health.manager.hasNewGuide
import com.daily.health.manager.receiver.NotificationActionReceiver
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.SpUtils
import net.corekit.core.report.ReportDataManager
import net.corekit.core.utils.ConfigRemoteManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 无界面的启动路由器。
 *
 * 渠道 SDK、通知和桌面快捷入口都依赖一个统一 Activity 入口，因此保留该类；它不再
 * inflate 布局、展示启动内容或请求开屏广告，只负责保持原有 Intent 分发行为。
 */
class LaunchGateActivity : AppCompatActivity() {

    private var hasRouted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        route(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        hasRouted = false
        route(intent)
    }

    private fun route(sourceIntent: Intent) {
        if (hasRouted || isFinishing) return
        hasRouted = true

        try {
            reportOpen(sourceIntent)
            dispatchNotificationClick(sourceIntent)
        } catch (error: Throwable) {
            // 埋点或通知分发失败不能阻断应用启动。
            if (BuildState.debug) "startup reporting failed: ${error.message}".logw(TAG)
        }

        if (sourceIntent.getStringExtra(KEY_FROM_SHORTCUT) == UNINSTALL) {
            trackUninstallClick()
            startActivity(Intent(this, UninstallResenActivity::class.java))
            finish()
            return
        }

        // 分组上报不应阻塞首个业务页面；作用域不捕获 Activity。
        reportingScope.launch {
            runCatching { reportGroup() }
                .onFailure { error ->
                    if (BuildState.debug) "group reporting failed: ${error.message}".logw(TAG)
                }
        }
        navigateToApplication(sourceIntent)
    }

    private fun navigateToApplication(sourceIntent: Intent) {
        if (ActivityUtils.isActivityExistsInStack(MainAct::class.java)) {
            finish()
            return
        }
        val targetActivity = if (hasNewGuide()) MainAct::class.java else GuideAct::class.java
        startActivity(Intent(this, targetActivity).apply { putExtras(sourceIntent) })
        finish()
    }

    private fun dispatchNotificationClick(sourceIntent: Intent) {
        val notificationId = sourceIntent.getIntExtra(
            NotificationActionReceiver.EXTRA_NOTIFICATION_ID,
            -1,
        )
        if (notificationId == -1) {
            if (BuildState.debug) "Invalid notification ID: $notificationId".logw(TAG)
            return
        }

        reportNotificationParam(sourceIntent)
        val actionType = sourceIntent.getStringExtra(NotificationActionReceiver.EXTRA_ACTION_VALUE)
        if (BuildState.debug) "notification actionType=$actionType".logd(TAG)
        sendBroadcast(Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_CLICKED
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        })
    }

    private fun reportOpen(sourceIntent: Intent) {
        ReportDataManager.reportData(
            "app_open",
            mapOf(
                "type" to if (isTaskRoot) "cold_open" else "hot_open",
                "position" to sourceIntent.getStringExtra(LANDING_NOTIFICATION_FROM)
                    .orEmpty()
                    .ifBlank { "other" },
            ),
        )
    }

    private fun reportNotificationParam(sourceIntent: Intent) {
        val source = sourceIntent.getStringExtra(LANDING_NOTIFICATION_FROM).orEmpty()
        val params = mutableMapOf<String, Any>(
            "Notific_Type" to when (source) {
                "firebase_push" -> 2
                "top_notification" -> 4
                else -> 1
            },
            "Notific_Position" to if (source == "top_notification") 2 else 1,
            "Notific_Priority" to if (source == "top_notification") {
                "PRIORITY_DEFAULT"
            } else {
                "PRIORITY_HIGH"
            },
            "event_id" to if (source == "top_notification") {
                "permanent"
            } else {
                "customer_general_style"
            },
            "title" to sourceIntent.getStringExtra(LANDING_NOTIFICATION_TITLE).orEmpty(),
            "text" to sourceIntent.getStringExtra(LANDING_NOTIFICATION_CONTENT).orEmpty(),
        )
        ReportDataManager.reportData("Notific_Enter", params)
        ReportDataManager.reportData(
            "Notific_Click",
            params.apply { put("from_background", AppLifecycleManager.isBackground()) },
        )
    }

    private companion object {
        const val TAG = "LaunchGateActivity"
        val reportingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun reportGroup() {
            val group = ConfigRemoteManager.getString("Grouping", "")
            if (group.isNullOrEmpty() || SpUtils.getBoolean("has_report_group_$group", false)) return

            SpUtils.putBoolean("has_report_group_$group", true)
            ReportDataManager.reportData("Grouping_$group", emptyMap())
        }
    }
}
