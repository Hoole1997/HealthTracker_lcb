package com.daily.health.manager.face.viewmodel

import androidx.lifecycle.viewModelScope
import com.daily.health.manager.data.repository.BloodPressureRepository
import com.daily.health.manager.data.repository.BloodSugarRepository
import com.daily.health.manager.data.repository.BmiRepository
import com.daily.health.manager.data.repository.CholesterolRepository
import com.daily.health.manager.data.repository.HeartRateRepository
import com.daily.health.manager.presentation.history.BloodPressureHistoryRow
import com.daily.health.manager.presentation.history.BloodSugarHistoryRow
import com.daily.health.manager.presentation.history.BmiHistoryRow
import com.daily.health.manager.presentation.history.HeartRateHistoryRow
import com.daily.health.manager.presentation.history.HealthHistoryRow
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val bpRepository: BloodPressureRepository,
    private val bsRepository: BloodSugarRepository,
    private val cholesterolRepository: CholesterolRepository,
    private val heartRateRepository: HeartRateRepository,
    private val bmiRepository: BmiRepository): BaseViewModel(){
    private val _recentRecord = kotlinx.coroutines.flow.MutableStateFlow<HealthHistoryRow?>(null)
    val recentRecord = _recentRecord.asStateFlow()

    init {
        loadRecentRecord()
    }

    private fun loadRecentRecord() {
        val lastTypeOrdinal = com.healthtracker.framework.util.SpUtils.getInt(com.daily.health.manager.constants.KEY_LAST_RECORD_TYPE, -1)
        if (lastTypeOrdinal == -1) return

        val type = HealthHistoryRow.RecordType.entries.getOrNull(lastTypeOrdinal) ?: return

        viewModelScope.launch {
            try {
                when (type) {
                    HealthHistoryRow.RecordType.BLOOD_PRESSURE -> {
                        bpRepository.getLatestBloodPressureRecords(1).collect { list ->
                             _recentRecord.value = list.firstOrNull()?.let {
                                 BloodPressureHistoryRow(
                                     it
                                 )
                             }
                        }
                    }
                    HealthHistoryRow.RecordType.BLOOD_SUGAR -> {
                        bsRepository.getLatestBloodSugarRecords(1).collect { list ->
                            _recentRecord.value = list.firstOrNull()?.let { BloodSugarHistoryRow(it) }
                        }
                    }
                    HealthHistoryRow.RecordType.HEART_RATE -> {
                        heartRateRepository.getLatestHeartRateRecords(1).collect { list ->
                            _recentRecord.value = list.firstOrNull()?.let { HeartRateHistoryRow(it) }
                        }
                    }
                    HealthHistoryRow.RecordType.BMI_RECORD -> {
                        bmiRepository.getLatestBmiRecords(1).collect { list ->
                            _recentRecord.value = list.firstOrNull()?.let { BmiHistoryRow(it) }
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}