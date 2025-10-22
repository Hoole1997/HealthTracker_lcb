package com.healthtracker.blood.suger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.ui.act.BpRecordActivity
import com.healthtracker.blood.suger.ui.act.BsRecordActivity
import com.healthtracker.blood.suger.ui.act.HeartRateRecordActivity
import com.healthtracker.framework.ext.logd

/**
 * 健康通知点击接收器
 * 处理常驻通知中各个区域的点击事件
 */
class HealthNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HealthNotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            HealthServiceConstants.ACTION_BLOOD_SUGAR -> {
                "Blood sugar notification clicked".logd(TAG)
                // 启动血糖记录界面
                val bsIntent = Intent(context, BsRecordActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(bsIntent)
            }

            HealthServiceConstants.ACTION_BLOOD_PRESSURE -> {
                "Blood pressure notification clicked".logd(TAG)
                // 启动血压记录界面
                val bpIntent = Intent(context, BpRecordActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(bpIntent)
            }

            HealthServiceConstants.ACTION_HEART_RATE -> {
                "Heart rate notification clicked".logd(TAG)
                // 启动心率记录界面
                val hrIntent = Intent(context, HeartRateRecordActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(hrIntent)
            }
        }
    }
}
