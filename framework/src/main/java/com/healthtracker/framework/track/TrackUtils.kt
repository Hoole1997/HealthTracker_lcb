//package com.healthtracker.framework.track
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.os.Bundle
//import androidx.collection.ArraySet
//import com.google.firebase.analytics.FirebaseAnalytics
//import com.google.gson.JsonObject
//import com.healthtracker.framework.BuildState
//import com.healthtracker.framework.ext.logd
//import com.healthtracker.framework.ext.loge
//
//import com.healthtracker.framework.util.SpUtils
//import com.healthtracker.framework.util.isDaySinceInstall
//import com.healthtracker.framework.util.isInstall24Hour
//import com.healthtracker.framework.util.logEvent
//
//import java.util.Locale
//import kotlin.math.round
//
//@SuppressLint("StaticFieldLeak")
//object TrackUtils {
//    private const val TAG = "TrackUtils"
//    private var isTest = false
//    private lateinit var mContext: Context
//    private var eventWhiteList: ArraySet<String>? = null
//    fun init(context: Context, whiteList: Array<String>, language: String, classNames: List<String?>) {
//        mContext = context
//        setWhiteList(whiteList)
//    }
//
//    fun setWhiteList(whiteList: Array<String>) {
//        eventWhiteList = ArraySet<String>().apply { addAll(whiteList) }
//    }
//
//    fun send(event: String, sid: String? = null) {
//        try {
//
//        } catch (_: Throwable) {
//
//        }
//    }
//
//    fun send(jsonObject: JsonObject) {
//        try {
//
//        } catch (_: Throwable) {
//
//        }
//    }
//
//    private fun sendAFEvent(event: String, params: Map<String, Any>? = null) {
//        if (BuildState.debug) "sendAFEvent:$event : $params".logd(TAG)
//        try {
//
//        } catch (_: Throwable) {
//
//        }
//    }
//
//    private fun sendFBEvent(event: String, params: Bundle? = null) {
//        if (BuildState.debug) "sendFBEvent:$event : $params".logd(TAG)
//        try {
//            logEvent(mContext, event, params)
//        } catch (_: Throwable) {
//
//        }
//    }
//
//    fun sendException(event: String, params: Bundle? = null) {
//        try {
//
//        } catch (_: Throwable) {
//
//        }
//    }
//
//
//    @Synchronized
//    fun onUserValue(ec: Double) {
//        if (!isTest && ec == 0.0) {
//            return
//        }
//        val old = SpUtils.getDouble("ad_total_value", 0.0)
//        if (isInstall24Hour(mContext) && old < valueNewUser.last()) {
//
//            val new = round6(old + ec)
//            SpUtils.putDouble("ad_total_value", new)
//            logDebug("total = $new", "onUserValue25")
//            for (d in valueNewUser) {
//                checkUserValue(old, new, d)
//            }
//            if (new >= valueNewUser.last()) {
//                logDebug("------ onUserValue25 value report END -------", "onUserValue25")
//            }
//        } else {
//            "is out of 24h".logd(TAG)
//        }
//        onValue30(ec)
//        sendAFEvent("ad_for_fb_purchase", mapOf("af_revenue" to ec, "af_currency" to "USD"))
//    }
//
//    fun sendImpression(ec: Double, adid: String?, platformId: Int, adType: Int) {
//        sendFBEvent(FirebaseAnalytics.Event.AD_IMPRESSION, Bundle().apply {
//            putString(FirebaseAnalytics.Param.AD_PLATFORM, "In-house")
////            putString(
////                FirebaseAnalytics.Param.AD_SOURCE,
////                getPlatformName(platformId)
////            )
////            putString(
////                FirebaseAnalytics.Param.AD_FORMAT,
////                getTypeName(adType)
////            )
//            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adid)
//            putString(FirebaseAnalytics.Param.CURRENCY, "USD")
//            putDouble(FirebaseAnalytics.Param.VALUE, ec)
//        })
//    }
//
//    private val valueNewUser = arrayOf(0.01,0.02,0.03,0.04, 0.05, 0.06, 0.08, 0.1, 0.15, 0.2, 0.25, 0.3, 0.4, 0.5, 0.6, 0.8, 1.0)
//
//    private fun checkUserValue(old: Double, new: Double, d: Double): Boolean {
//        if (old < d && new >= d) {
//            val sValue = formatSValue(d)
//            return if (SpUtils.getBoolean(sValue, false)) {
//                logDebug("Already reported: $sValue [Old: $old, New: $new, Threshold: $d]","onUserValue25")
//                false
//            } else {
//                logDebug("Reporting: $sValue [Old: $old, New: $new, Threshold: $d]","onUserValue25")
//                sendFBAndAF(sValue)
//                SpUtils.putBoolean(sValue, true)
//                true
//            }
//        }
//        return false
//    }
//
//    private fun formatSValue(d: Double): String {
//        return "Ad_Revenue_%03d0".format(Locale.ENGLISH, (d * 100).toInt())
//    }
//
//    @SuppressLint("DefaultLocale")
//    private fun onValue30(ec: Double) {
//        if (BuildState.debug) logDebug("input -> ${String.format("%.6f", ec)}","onUserValue_30")
//
//        val thresholds = listOf(0.005,0.01, 0.02, 0.05)
//        thresholds.forEach { threshold ->
//            checkAndProcessUserValue(ec, threshold)
//        }
//    }
//
//    @Synchronized
//    private fun checkAndProcessUserValue(ec: Double, threshold: Double) {
//        val sValue = formatThresholdValue(threshold)
//        val sKey = "key_user_mone_3_values_$sValue"
//
//        val currentValue = SpUtils.getDouble(sKey, 0.0)
//        val newValue = currentValue + ec
//
//        if (newValue >= threshold) {
//            logDebug("Threshold reached: $sValue [Current: $currentValue, New: $newValue, Threshold: $threshold]. Reporting event.","onUserValue_30")
//            sendFBEvent(sValue, createEventBundle(newValue))
//            SpUtils.putDouble(sKey, 0.0)
//        } else {
//            SpUtils.putDouble(sKey, newValue)
//            logDebug("Threshold not reached: $sValue [Current: $currentValue, New: $newValue, Threshold: $threshold]. Accumulating value.","onUserValue_30")
//        }
//    }
//
//    private fun formatThresholdValue(threshold: Double): String {
//        return "%s%03d".format(Locale.ENGLISH, "troas", (threshold * 100).toInt())
//    }
//
//    private fun createEventBundle(value: Double): Bundle {
//        return Bundle().apply {
//            putString("currency", "USD")
//            putDouble("value", round6(value))
//        }
//    }
//
//    private fun logDebug(message: String,tag:String = TAG) {
//        message.loge(tag)
//    }
//
//   private fun sendFBAndAF(event:String){
//        try {
//            sendFBEvent(event)
//            sendAFEvent(event)
//        }catch (_:Throwable){
//
//        }
//    }
//    private val installDays = listOf(2, 3, 7, 14)
//    fun sendActive(){
//        installDays.forEach { day ->
//            if ((day - 1).isDaySinceInstall(mContext)) {
//                val key = "user_day${day}_active"
//                if (SpUtils.getBoolean(key, false)) {
//                    return@forEach
//                }
//                sendFBAndAF("user_day${day}_active")
//                SpUtils.putBoolean(key, true)
//            }
//        }
//
//    }
//}
//fun round6(value: Double) = round(value * 1000000) / 1000000.0
//fun getPingBack(value:Double) = round6(value / 1000.0)
//
