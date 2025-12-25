package com.daily.health.manager.face.tracker

import android.content.Context
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logw
import net.corekit.core.report.ReportDataManager

/**
 * 健康埋点扩展函数
 * 
 * 提供简洁的 DSL 风格调用接口，简化埋点调用代码。
 */

/**
 * 上报健康相关埋点事件
 * 
 * @param event 健康埋点事件
 */
fun Context.trackHealthEvent(event: HealthTrackEvent) {
    val bundle = mutableMapOf<String,String>().apply {
        event.params.forEach { (key, value) ->
            put(key,value)
        }
    }
    if(BuildState.debug) "上报事件:${event.eventName},data:${bundle.toString()}".logw("HealthTrackExtensions")
    ReportDataManager.reportData(eventName = event.eventName, data = bundle)
}


/**
 * Track "enter_page_click" event
 */
fun Context.trackEnterPageClick(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.EnterPageClick(healthType))
}

/**
 * Track "add_new_record" event
 */
fun Context.trackAddNewRecord(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.AddNewRecord(healthType))
}

/**
 * Track "Insights_category_click" event
 */
fun Context.trackInsightsCategoryClick(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.InsightsCategoryClick(healthType))
}

/**
 * Track "enter_Trackerpage_click" event
 */
fun Context.trackEnterTrackPageClick(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.EnterTrackPageClick(healthType))
}

/**
 * Track "NewRecordPage_Back" event
 */
fun Context.trackNewRecordPageBack(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.NewRecordPageBack(healthType))
}

/**
 * Track "ResultPage_Back" event
 */
fun Context.trackResultPageBack(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.ResultPageBack(healthType))
}

/**
 * Track "Trackpage_Back" event
 */
fun Context.trackTrackPageBack(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.TrackPageBack(healthType))
}

/**
 * Track "recomm_FreeUnlock" event
 */
fun Context.trackRecommFreeUnlock(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.RecommFreeUnlock(healthType))
}

/**
 * Track "recomm_Cancel" event
 */
fun Context.trackRecommCancel(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.RecommCancel(healthType))
}

/**
 * Track "Ad_auto_play" event
 */
fun Context.trackAdAutoPlay(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.AdAutoPlay(healthType))
}

/**
 * Track "Ad_auto_Cancel" event
 */
fun Context.trackAdAutoCancel(healthType: HealthType) {
    trackHealthEvent(HealthTrackEvent.AdAutoCancel(healthType))
}
