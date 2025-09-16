package com.healthtracker.blood.suger.ui.viewmodel

import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 首页ViewModel
 * 显示最近一次的血糖和血压记录
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bloodSugarRepository: BloodSugarRepository,
    private val bloodPressureRepository: BloodPressureRepository
) : BaseViewModel() {

    // 获取最近一次血糖记录
    val latestBloodSugarRecord = bloodSugarRepository.getRecentBloodSugarRecordsWithLimit(1).map { it.firstOrNull() }

    // 获取最近一次血压记录
    val latestBloodPressureRecord = bloodPressureRepository.getRecentBloodPressureRecordsWithLimit(1).map { it.firstOrNull() }
}