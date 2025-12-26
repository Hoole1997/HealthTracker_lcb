package com.daily.health.manager.face.viewmodel

import androidx.lifecycle.viewModelScope
import com.daily.health.manager.data.repository.BloodPressureRepository
import com.daily.health.manager.data.repository.BloodSugarRepository
import com.daily.health.manager.data.repository.BmiRepository
import com.daily.health.manager.data.repository.HeartRateRepository
import com.daily.health.manager.data.repository.CholesterolRepository
import com.daily.health.manager.data.repository.HydrateRepository
import com.daily.health.manager.data.utils.DateTimeUtils
import com.healthtracker.framework.base.BaseViewModel
import com.daily.health.manager.App
import com.daily.health.manager.data.repo.StepRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 首页ViewModel
 * 显示最近一次的血糖和血压记录
 */
class HomeViewModel(
    private val bloodSugarRepository: BloodSugarRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val bmiRepository: BmiRepository,
    private val heartRateRepository: HeartRateRepository,
    private val cholesterolRepository: CholesterolRepository,
    private val hydrateRepository: HydrateRepository,
) : BaseViewModel() {

    // 获取最近一次血糖记录
    val latestBloodSugarRecord = bloodSugarRepository.getRecentBloodSugarRecordsWithLimit(1).map { it.firstOrNull() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // 获取最近一次血压记录
    val latestBloodPressureRecord = bloodPressureRepository.getRecentBloodPressureRecordsWithLimit(1).map { it.firstOrNull() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // 获取最近一次 BMI 记录
    val latestBmiRecord = bmiRepository.getAllBmiRecords()
        .map { records -> records.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 获取最近一次心率记录
    val latestHeartRateRecord = heartRateRepository.getAllHeartRateRecords()
        .map { records -> records.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // 获取最近一次胆固醇记录
    val latestCholesterolRecord = cholesterolRepository.getAllRecords()
        .map { records -> records.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )


    // 今日总饮水量（毫升）
    val todayTotalIntakeMl = hydrateRepository.getRecordsByTimeRange(DateTimeUtils.getTodayRange().first,
        DateTimeUtils.getTodayRange().second)
        .map { records -> records.sumOf { it.intakeMl } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val stepRepo = StepRepository.get(App.INSTANCE)
    val todayStepStat = stepRepo.observeTodayDynamic()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
