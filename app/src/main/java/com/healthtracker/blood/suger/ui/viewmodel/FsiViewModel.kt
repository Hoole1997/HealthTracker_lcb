package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.config.HydrateSettingManager
import com.healthtracker.blood.suger.data.entity.DailyStepStat
import com.healthtracker.blood.suger.data.repo.StepRepository
import com.healthtracker.blood.suger.data.repository.HydrateRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FsiViewModel @Inject constructor(
    private val hydrateRepository: HydrateRepository,
): BaseViewModel() {
    private val repo = StepRepository.get(App.INSTANCE)
    val todayStatFlow: Flow<DailyStepStat?> = repo.observeTodayDynamic()

    /**
     * 饮水显示状态
     */
    data class HydrateDisplayState(
        val currentIntakeMl: Int,           // 当前饮水量（ml）
        val targetIntakeMl: Int,            // 目标饮水量（ml）
        val isGoalReached: Boolean,         // 是否达标
        val cupUnit: HydrateSettingManager.CupUnit  // 单位
    )

    /**
     * 饮水数据状态 - 在页面打开时获取一次
     */
    val hydrateDisplayState: Flow<HydrateDisplayState> = 
        hydrateRepository.getRecordsByTimeRange(
            DateTimeUtils.getTodayRange().first,
            DateTimeUtils.getTodayRange().second
        ).map { records ->
            // 计算当天总饮水量
            val currentIntakeMl = records.sumOf { it.intakeMl }
            
            // 获取目标设置
            val dailyCups = HydrateSettingManager.getDailyCups()
            val cupVolumeMl = HydrateSettingManager.getCupVolume()
            val targetIntakeMl = dailyCups * cupVolumeMl
            
            // 获取单位
            val cupUnit = HydrateSettingManager.getCupUnit()
            
            // 判断是否达标
            val isGoalReached = currentIntakeMl >= targetIntakeMl
            
            HydrateDisplayState(
                currentIntakeMl = currentIntakeMl,
                targetIntakeMl = targetIntakeMl,
                isGoalReached = isGoalReached,
                cupUnit = cupUnit
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HydrateDisplayState(
                currentIntakeMl = 0,
                targetIntakeMl = HydrateSettingManager.getDailyCups() * HydrateSettingManager.getCupVolume(),
                isGoalReached = false,
                cupUnit = HydrateSettingManager.getCupUnit()
            )
        )
}