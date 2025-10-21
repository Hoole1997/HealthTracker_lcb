package com.healthtracker.blood.suger.work

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.healthtracker.blood.suger.BuildConfig
import java.util.concurrent.TimeUnit


object HealthWorkTask {

    @Volatile
    private var fcmToken : String? = null

    @JvmOverloads
    fun start(context: Context, tag: String, repeatInterval: Long = 15, repeatIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES) {
        WorkManager.getInstance(context).apply {
            cancelAllWorkByTag(tag)
            enqueue(PeriodicWorkRequest.Builder(PeriodicScanWorker::class.java, repeatInterval, repeatIntervalTimeUnit).addTag(tag).build())
        }

//        initUser()
    }

//    private fun initUser() {
//        GlobalScope.launch(Dispatchers.IO) {
//            // 检查用户初始化
//            Server.checkUserInit {
//                // 如果有暂存的fcmToken，上报
//                fcmToken?.let {
//                    if (BuildState.debug) "[Buss] checkUserInit 初始化回调，有 fcmToken，上报".logi("Worker")
//                    if (it.isNotEmpty()) {
//                        Server.sendFCMTokenAndSave(it)
//                    }
//                    fcmToken = null
//                }
//            }
//        }
//
//        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                if (BuildState.debug) "[Buss] Fetching FCM registration token failed: ${task.exception}".logw(
//                    "Worker"
//                )
//                return@OnCompleteListener
//            }
//
//            val token = task.result
//            uploadToken(token)
//        })
//
//        if (!BuildConfig.PUB_RELEASE) {
//            uploadToken(UUID.randomUUID().toString())
//        }
//    }
//
//    fun uploadToken(token: String) {
//        if (token.isNotEmpty()) {
//            val savedToken = SpUtils.getString(Constants.KEY_FCM_TOKEN)
//            // token 不同的时候上传
//            if (token != savedToken) {
//                if (BuildState.debug) "[Buss] Upload FCM Token".logw("Worker")
//
//                GlobalScope.launch(Dispatchers.IO) {
//                    val isCalled = Server.checkUserInit({
//                        if (BuildState.debug) "[Buss] checkUserInit-回调: token = $it".logw("Worker")
//                        if (!it.isNullOrEmpty()) {
//                            Server.sendFCMTokenAndSave(token)
//                        }
//                    }, false)
//
//                    if (BuildState.debug) "[Buss] checkUserInit 请求中 = $isCalled".logi("Worker")
//
//                    // checkUserInit正在调用中 = false，缓存下 token，等下次调用
//                    if (!isCalled) {
//                        fcmToken = token
//                    }
//                }
//            }
//        }
//    }

    fun scheduleScanTask(context: Context) {
        // 定义约束条件
//        val constraints: Constraints = Constraints.Builder()
//            .setRequiresCharging(false) // 例如：不要求设备充电
//            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // 例如：不要求网络连接
//            .build()

        WorkManager.getInstance(context).apply {
            val tag = "${BuildConfig.APPLICATION_ID}_unlock"
            try {
                cancelAllWorkByTag(tag)
            } catch (e: Throwable) {

            }
            enqueue(OneTimeWorkRequest.Builder(OneTimeScanWorker::class.java).addTag(tag).build())
        }
    }

    fun registerReceiver(context: Context) {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_USER_PRESENT)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        context.registerReceiver(ScreenUnlockReceiver(), filter)
    }
}