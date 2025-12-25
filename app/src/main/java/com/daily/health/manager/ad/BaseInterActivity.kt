package com.daily.health.manager.ad

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.daily.health.manager.face.act.HealthDetailScreen
import com.daily.health.manager.face.act.HealthRecordScreen
import com.daily.health.manager.face.act.HealthStatisticsScreen
import com.daily.health.manager.face.act.HydrateScreen
import com.daily.health.manager.face.act.StepCountScreen
import com.daily.health.manager.utils.loadRewardBidding
import com.daily.health.manager.utils.showInter
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.daily.health.manager.face.tracker.HealthType
import com.daily.health.manager.face.tracker.trackEnterTrackPageClick
import com.daily.health.manager.face.tracker.trackNewRecordPageBack
import com.daily.health.manager.face.tracker.trackResultPageBack
import com.daily.health.manager.face.tracker.trackTrackPageBack
import kotlinx.coroutines.launch

abstract class BaseInterActivity<VM : BaseViewModel, VB : ViewBinding>: BaseMVVMActivity<VM,VB>() {

    override fun handleBackPress(): Boolean {

        
        showInter {
            // 根据 Activity 类型自动上报返回事件
            trackBackEvent()
            // 显示插屏广告后关闭 Activity
            finish()
        }
        // 返回 true 表示已处理返回键事件
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(this is HealthStatisticsScreen){
            trackEnterTrackPageClick(getCurrentHealthType())
        }
    }
    
    /**
     * 追踪返回事件
     * 根据当前 Activity 类型自动确定是 NewRecordPage_Back、ResultPage_Back 还是 Trackpage_Back
     */
    private fun trackBackEvent() {
        val (eventType, healthType) = getEventTypeAndHealthType() ?: return
        
        when (eventType) {
            EventType.NEW_RECORD_PAGE -> trackNewRecordPageBack(healthType)
            EventType.RESULT_PAGE -> trackResultPageBack(healthType)
            EventType.TRACK_PAGE -> trackTrackPageBack(healthType)
        }
    }
    
    /**
     * 根据 Activity 类名确定事件类型和健康类型
     */
    private fun getEventTypeAndHealthType(): Pair<EventType, HealthType>? {
        return when(this) {
            // Record Activities -> NewRecordPage_Back
            is HealthRecordScreen -> EventType.NEW_RECORD_PAGE to getCurrentHealthType()
            is StepCountScreen -> EventType.NEW_RECORD_PAGE to HealthType.WALKING_STEPS
            is HydrateScreen -> EventType.NEW_RECORD_PAGE to HealthType.HYDRATE

            // Detail Activities -> ResultPage_Back
            is HealthDetailScreen -> {
                val healthType = getCurrentHealthType()
                EventType.RESULT_PAGE to healthType
            }

            // Statistics Activity -> Trackpage_Back
            is HealthStatisticsScreen -> {
                // 从子类获取当前的健康类型
                val healthType = getCurrentHealthType()
                EventType.TRACK_PAGE to healthType
            }

            // 其他不需要追踪的页面
            else -> null
        }
    }
    
    /**
     * 事件类型枚举
     */
    private enum class EventType {
        NEW_RECORD_PAGE,
        RESULT_PAGE,
        TRACK_PAGE
    }
    
    /**
     * 获取当前健康类型（由子类覆盖以提供具体的健康类型）
     * 用于 HealthStatisticsActivity 等需要动态确定健康类型的页面
     */
    protected open fun getCurrentHealthType(): HealthType = HealthType.OTHER


    protected fun showReword(){
       lifecycleScope.launch {
           loadRewardBidding {
               if(it){
                   hideMask()
               }
           }
       }
    }


    protected open fun hideMask(){

    }


}

