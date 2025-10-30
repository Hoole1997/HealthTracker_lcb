package com.healthtracker.blood.suger.service

import com.blankj.utilcode.util.AppUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.healthtracker.framework.ext.logd
import net.corekit.core.report.ReportDataManager

/**
 * FCM 消息处理服务
 * 处理推送通知的接收和显示
 */
class MessageService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MessageService"
        private const val VERSION_KEY = "version" // FCM消息中的version字段

        /**
         * 初始化 FCM 服务
         * 这个方法可以在应用启动时调用，确保 FCM 服务被注册
         */
        fun initialize() {
            // FCM 服务会在收到消息时自动创建，这里只是记录初始化状态
            "FCM 服务已准备就绪，等待消息".logd(TAG)
        }
        
        /**
         * 检查version字段是否匹配当前应用版本
         * @param messageVersion FCM消息中的version字段值
         * @param currentVersion 当前应用版本
         * @return true表示匹配或无需检查，false表示不匹配
         */
        private fun isVersionMatched(messageVersion: String?, currentVersion: String): Boolean {
            // version没有值的时候不判断，全量发送
            if (messageVersion.isNullOrBlank()) {
                "FCM消息无version字段，全量发送".logd("FCM消息无version字段，全量发送")
                return true
            }
            
            // 有值的时候，客户端需要判断=当前值才发送
            val isMatched = messageVersion == currentVersion
            "FCM消息version检查: 消息version=$messageVersion, 当前version=$currentVersion, 匹配结果=$isMatched".logd(TAG)
            return isMatched
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    /**
     * 当收到 FCM 消息时调用
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        ReportDataManager.reportData("Notific_Pull", mapOf("topic" to "ALL_TOKEN"))

        "收到 FCM 消息".logd(TAG)
        "消息来源: ${remoteMessage.from}".logd(TAG)
        "消息 ID: ${remoteMessage.messageId}".logd(TAG)
        "消息类型: ${remoteMessage.messageType}".logd(TAG)

        // 处理数据载荷
        if (remoteMessage.data.isNotEmpty()) {
            "消息数据载荷:".logd(TAG)
            for ((key, value) in remoteMessage.data) {
                "  $key: $value".logd(TAG)
            }
        }

        // 检查version字段
        val messageVersion = remoteMessage.data[VERSION_KEY]
        val currentVersion = AppUtils.getAppVersionName()
        
//        Logger.d("FCM消息version检查开始")
        "当前应用版本: $currentVersion".logd(TAG)
        "消息version字段: $messageVersion".logd(TAG)
        
        // 根据version字段决定是否处理消息
//        if (!isVersionMatched(messageVersion, currentVersion)) {
//            Logger.d("FCM消息version不匹配，忽略此消息")
//            return
//        }
        
//        Logger.d("FCM消息version检查通过，继续处理消息")

        // 处理真正的操作
        triggerFCMNotification()
    }

    private fun triggerFCMNotification() {
//        KeepAliveServiceManager.startKeepAliveService(context = this, from = "fcm")
//        TimingCtrl.getInstance().triggerNotificationIfAllowed(
//            CheckCtrl.NotificationType.FCM
//        )
    }

    /**
     * 当 FCM 令牌更新时调用
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        "FCM 令牌已更新: $token".logd(TAG)

    }


}
