package com.healthtracker.blood.suger.ad

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.healthtracker.blood.suger.ui.act.BmiDetailActivity
import com.healthtracker.blood.suger.ui.act.BmiRecordActivity
import com.healthtracker.blood.suger.ui.act.BpDetailActivity
import com.healthtracker.blood.suger.ui.act.BpRecordActivity
import com.healthtracker.blood.suger.ui.act.BsDetailActivity
import com.healthtracker.blood.suger.ui.act.BsRecordActivity
import com.healthtracker.blood.suger.ui.act.CholesterolDetailActivity
import com.healthtracker.blood.suger.ui.act.CholesterolRecordActivity
import com.healthtracker.blood.suger.ui.act.HealthStatisticsActivity
import com.healthtracker.blood.suger.ui.act.HeartRateDetailActivity
import com.healthtracker.blood.suger.ui.act.HeartRateRecordActivity
import com.healthtracker.blood.suger.ui.act.HydrateActivity
import com.healthtracker.blood.suger.ui.act.StepCountActivity
import com.healthtracker.blood.suger.utils.loadRewardBidding
import com.healthtracker.blood.suger.utils.showInter
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.blood.suger.ui.tracker.HealthType
import com.healthtracker.blood.suger.ui.tracker.trackEnterTrackPageClick
import com.healthtracker.blood.suger.ui.tracker.trackNewRecordPageBack
import com.healthtracker.blood.suger.ui.tracker.trackResultPageBack
import com.healthtracker.blood.suger.ui.tracker.trackTrackPageBack
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
        if(this is HealthStatisticsActivity){
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
            is BsRecordActivity -> EventType.NEW_RECORD_PAGE to HealthType.BLOOD_SUGAR
            is BpRecordActivity -> EventType.NEW_RECORD_PAGE to HealthType.BLOOD_PRESSURE
            is CholesterolRecordActivity -> EventType.NEW_RECORD_PAGE to HealthType.CHOLESTEROL
            is HeartRateRecordActivity-> EventType.NEW_RECORD_PAGE to HealthType.HEART_RATE
            is BmiRecordActivity -> EventType.NEW_RECORD_PAGE to HealthType.BMI
            is StepCountActivity -> EventType.NEW_RECORD_PAGE to HealthType.WALKING_STEPS
            is HydrateActivity -> EventType.NEW_RECORD_PAGE to HealthType.HYDRATE

            // Detail Activities -> ResultPage_Back
            is BsDetailActivity -> EventType.RESULT_PAGE to HealthType.BLOOD_SUGAR
            is BpDetailActivity -> EventType.RESULT_PAGE to HealthType.BLOOD_PRESSURE
            is CholesterolDetailActivity -> EventType.RESULT_PAGE to HealthType.CHOLESTEROL
            is HeartRateDetailActivity -> EventType.RESULT_PAGE to HealthType.HEART_RATE
            is BmiDetailActivity -> EventType.RESULT_PAGE to HealthType.BMI

            // Statistics Activity -> Trackpage_Back
            is HealthStatisticsActivity -> {
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

