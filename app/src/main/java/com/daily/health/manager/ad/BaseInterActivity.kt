package com.daily.health.manager.ad

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.daily.health.manager.face.act.HealthDetailAct
import com.daily.health.manager.face.act.HealthRecordAct
import com.daily.health.manager.face.act.HealthStatisticsAct
import com.daily.health.manager.face.act.HydrateAct
import com.daily.health.manager.face.act.StepCountAct
import com.daily.health.manager.utils.loadRewardBidding
import com.daily.health.manager.utils.showInter
import net.corekit.monetize.ads.AdPosition
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
        // 根据 Activity 类型自动上报返回事件
        trackBackEvent()
        finish()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 进入页面时立即展示插页广告
        showInter(getBackAdPosition()) {}
        if(this is HealthStatisticsAct){
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
            is HealthRecordAct -> EventType.NEW_RECORD_PAGE to getCurrentHealthType()
            is StepCountAct -> EventType.NEW_RECORD_PAGE to HealthType.WALKING_STEPS
            is HydrateAct -> EventType.NEW_RECORD_PAGE to HealthType.HYDRATE

            // Detail Activities -> ResultPage_Back
            is HealthDetailAct -> {
                val healthType = getCurrentHealthType()
                EventType.RESULT_PAGE to healthType
            }

            // Statistics Activity -> Trackpage_Back
            is HealthStatisticsAct -> {
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

    /**
     * 根据当前Activity和健康类型获取返回时的插屏广告position
     */
    protected open fun getBackAdPosition(): String {
        val healthType = getCurrentHealthType()
        return when(this) {
            // 录入页面返回
            is HealthRecordAct -> when(healthType) {
                HealthType.BLOOD_SUGAR -> AdPosition.IV_BLOOD_SUGAR_BACK
                HealthType.BLOOD_PRESSURE -> AdPosition.IV_BLOOD_PRESSURE_BACK
                HealthType.CHOLESTEROL -> AdPosition.IV_CHOLESTEROL_BACK
                HealthType.HEART_RATE -> AdPosition.IV_HEART_RATE_BACK
                HealthType.BMI -> AdPosition.IV_BMI_BACK
                else -> AdPosition.IV_BLOOD_SUGAR_BACK
            }
            is HydrateAct -> AdPosition.IV_WATER_BACK
            is StepCountAct -> AdPosition.IV_WALK_BACK
            // 报表页面返回
            is HealthStatisticsAct -> when(healthType) {
                HealthType.BLOOD_SUGAR -> AdPosition.IV_BLOOD_SUGAR_TRACK_BACK
                HealthType.BLOOD_PRESSURE -> AdPosition.IV_BLOOD_PRESSURE_TRACK_BACK
                HealthType.CHOLESTEROL -> AdPosition.IV_CHOLESTEROL_TRACK_BACK
                HealthType.HEART_RATE -> AdPosition.IV_HEART_RATE_TRACK_BACK
                HealthType.BMI -> AdPosition.IV_BMI_TRACK_BACK
                HealthType.HYDRATE -> AdPosition.IV_WATER_TRACK_BACK
                HealthType.WALKING_STEPS -> AdPosition.IV_WALK_TRACK_BACK
                else -> AdPosition.IV_BLOOD_SUGAR_TRACK_BACK
            }
            // 详情页面返回 - 使用报表返回position
            is HealthDetailAct -> when(healthType) {
                HealthType.BLOOD_SUGAR -> AdPosition.IV_BLOOD_SUGAR_TRACK_BACK
                HealthType.BLOOD_PRESSURE -> AdPosition.IV_BLOOD_PRESSURE_TRACK_BACK
                HealthType.CHOLESTEROL -> AdPosition.IV_CHOLESTEROL_TRACK_BACK
                HealthType.HEART_RATE -> AdPosition.IV_HEART_RATE_TRACK_BACK
                HealthType.BMI -> AdPosition.IV_BMI_TRACK_BACK
                else -> AdPosition.IV_BLOOD_SUGAR_TRACK_BACK
            }
            else -> AdPosition.IV_BLOOD_SUGAR_BACK
        }
    }

    /**
     * 根据当前健康类型获取激励广告position
     */
    protected open fun getRewardAdPosition(): String {
        return when(getCurrentHealthType()) {
            HealthType.BLOOD_SUGAR -> AdPosition.RV_BLOOD_SUGAR_NOTE
            HealthType.BLOOD_PRESSURE -> AdPosition.RV_BLOOD_PRESSURE_NOTE
            HealthType.CHOLESTEROL -> AdPosition.RV_CHOLESTEROL_NOTE
            HealthType.HEART_RATE -> AdPosition.RV_HEART_RATE_NOTE
            HealthType.BMI -> AdPosition.RV_BMI_NOTE
            else -> AdPosition.RV_BLOOD_SUGAR_NOTE
        }
    }

    protected fun showReword(){
       lifecycleScope.launch {
           loadRewardBidding(getRewardAdPosition()) {
               hideMask()
           }
       }
    }


    protected open fun hideMask(){

    }


}

