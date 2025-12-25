package com.daily.health.manager.face.tracker

/**
 * 健康相关埋点事件密封类
 * 
 * 使用密封类统一管理所有健康相关的埋点事件，提供类型安全和编译期检查。
 * 
 * @property eventName 事件名称
 * @property params 事件参数
 */
sealed class HealthTrackEvent(
    val eventName: String,
    val params: Map<String, String>
) {
    // New events
    data class EnterPageClick(val healthType: HealthType) : HealthTrackEvent(
        eventName = "enter_page_click",
        params = mapOf("page_name" to healthType.pageName)
    )
    data class AddNewRecord(val healthType: HealthType) : HealthTrackEvent(
        eventName = "add_new_record",
        params = mapOf("page_name" to healthType.pageName)
    )
    data class InsightsCategoryClick(val healthType: HealthType) : HealthTrackEvent(
        eventName = "Insights_category_click",
        params = mapOf("page_name" to healthType.pageName)
    )
    data class EnterTrackPageClick(val healthType: HealthType) : HealthTrackEvent(
        eventName = "enter_Trackerpage_click",
        params = mapOf("page_name" to healthType.pageName)
    )
    
    // Back events
    data class NewRecordPageBack(val healthType: HealthType) : HealthTrackEvent(
        eventName = "NewRecordPage_Back",
        params = mapOf("page_name" to healthType.pageName)
    )
    data class ResultPageBack(val healthType: HealthType) : HealthTrackEvent(
        eventName = "ResultPage_Back",
        params = mapOf("page_name" to healthType.pageName)
    )
    data class TrackPageBack(val healthType: HealthType) : HealthTrackEvent(
        eventName = "Trackpage_Back",
        params = mapOf("page_name" to healthType.pageName)
    )
    
    
    // Ad and Recommendation events
    data class RecommFreeUnlock(val healthType: HealthType) : HealthTrackEvent(
        eventName = "recomm_FreeUnlock",
        params = mapOf("page_name" to healthType.pageName)
    )
    data class RecommCancel(val healthType: HealthType) : HealthTrackEvent(
        eventName = "recomm_Cancel",
        params = mapOf("page_name" to healthType.pageName)
    )
    data class AdAutoPlay(val healthType: HealthType) : HealthTrackEvent(
        eventName = "Ad_auto_play",
        params = mapOf("page_name" to healthType.pageName)
    )
    data class AdAutoCancel(val healthType: HealthType) : HealthTrackEvent(
        eventName = "Ad_auto_Cancel",
        params = mapOf("page_name" to healthType.pageName)
    )
}
