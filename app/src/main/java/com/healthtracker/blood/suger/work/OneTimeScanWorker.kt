package com.healthtracker.blood.suger.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd

class OneTimeScanWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (BuildState.debug) "OneTimeScanWorker Run".logd("Worker")

//        EventUtil.prepareScanFiles()

        val context = applicationContext

//        if (Notifications.isCanPush(context) && !AppUpgradeUtil.checkMigrateUpgrade(context)) {
//            val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
//
//            if (hour in 9..11) {
//                if (Notifications.isCanDontForgot()) {
//                    Notifications.markDontForgot()
//                    if (BuildState.debug) "[日常]早晨 提醒 SHOW".logi("Worker")
//                    Notifications.showDontForgotNotification(context)
//                } else {
//                    if (BuildState.debug) "[日常]早晨 提醒 未符合条件".logi("Worker")
//                }
//            } else {
//                if (BuildState.debug) "[日常]早晨  提醒 不在 9 ~ 11".logi("Worker")
//            }
//
//            if (hour in 21..23) {
//                if (Notifications.isCanGotoBed()) {
//                    Notifications.markGotoBed()
//                    if (BuildState.debug) "[日常]夜晚 提醒 SHOW".logi("Worker")
//                    Notifications.showGotoBedNotification(context)
//                } else {
//                    if (BuildState.debug) "[日常]夜晚 提醒 未符合条件".logi("Worker")
//                }
//            } else {
//                if (BuildState.debug) "[日常]夜晚 提醒 不在 21 ~ 23".logi("Worker")
//            }
//
//            if (PermissionsUtils.hasStoragePermission(context)) {
//                // 检查是否在6到24点之间
//                if (hour >= 6) {
//
//                    if (BuildState.debug) "Scan [New Files]".logd("Worker")
//
//                    DocScanner.discoverNewDocs(context).also { newDocList ->
//                        if (!newDocList.isNullOrEmpty()) {
//                            if (BuildState.debug) "Scan [New Files] size = ${newDocList.size}".loge("Worker")
//                            Notifications.showNewDocsNotification(context)
//                        } else {
//                            if (BuildState.debug) "Scan [New Files] size = 0".loge("Worker")
//                        }
//                    }
//
//                    if (BuildState.debug) "Scan [New Images]".logd("Worker")
//                    if (DocScanner.detectNewImages(context)) {
//                        if (BuildState.debug) "Scan [New Images] has New Images".logi("Worker")
//                        if (Notifications.showNewPhotoNotification(context)) {
//                            Notifications.setNotificationTime(Constants.KEY_DETECTED_IMAGES_SHOW_TIME)
//                        }
//                    } else {
//                        if (BuildState.debug) "Scan [New Images] no New Image".logi("Worker")
//                    }
//                }
//            } else {
//                if (BuildState.debug) "Scan [New Files] No Permission".loge("Worker")
//            }
//
//            if (BuildState.debug) "query [Not Finished]".logd("Worker")
//
//            DbManager.queryFirstNotFinished()?.also {
//                if (Notifications.isCanReminderNotFinished()) {
//                    if (BuildState.debug) "query [Not Finished]: show Notification".logd("Worker")
//                    Notifications.showNotFinishedNotification(context)
//
//                    SpUtils.putLong(Constants.KEY_NOT_FINISHED_REMINDER_TIME, System.currentTimeMillis())
//                } else {
//                    if (BuildState.debug) "query [Not Finished]: < 24H".loge("Worker")
//                }
//            }
//
//        } else {
//            if (BuildState.debug) "OneTimeScanWorker: Notification Disable OR App Foreground".loge("Worker")
//        }
//
//        SensorsDataAPI.getInstance().flush()

        if (BuildState.debug) "OneTimeScanWorker End".logd("Worker")

        return Result.success()
    }
}