package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 首页ViewModel
 * 显示最近一次的血糖和血压记录
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bloodSugarRepository: BloodSugarRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val bmiRepository: BmiRepository
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
}