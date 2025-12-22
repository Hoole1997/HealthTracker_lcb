package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val bpRepository: BloodPressureRepository,
    private val bsRepository: BloodSugarRepository,
    private val cholesterolRepository: CholesterolRepository,
    private val heartRateRepository: HeartRateRepository,
    private val bmiRepository: BmiRepository): BaseViewModel(){
    private val _recentRecord = kotlinx.coroutines.flow.MutableStateFlow<com.healthtracker.blood.suger.ui.history.HistoryRecordItem?>(null)
    val recentRecord = _recentRecord.asStateFlow()

    init {
        loadRecentRecord()
    }

    private fun loadRecentRecord() {
        val lastTypeOrdinal = com.healthtracker.framework.util.SpUtils.getInt(com.healthtracker.blood.suger.constants.KEY_LAST_RECORD_TYPE, -1)
        if (lastTypeOrdinal == -1) return

        val type = HistoryRecordItem.RecordType.entries.getOrNull(lastTypeOrdinal) ?: return

        viewModelScope.launch {
            try {
                when (type) {
                    com.healthtracker.blood.suger.ui.history.HistoryRecordItem.RecordType.BLOOD_PRESSURE -> {
                        bpRepository.getLatestBloodPressureRecords(1).collect { list ->
                             _recentRecord.value = list.firstOrNull()?.let { com.healthtracker.blood.suger.ui.history.BloodPressureHistoryItem(it) }
                        }
                    }
                    com.healthtracker.blood.suger.ui.history.HistoryRecordItem.RecordType.BLOOD_SUGAR -> {
                        bsRepository.getLatestBloodSugarRecords(1).collect { list ->
                            _recentRecord.value = list.firstOrNull()?.let { com.healthtracker.blood.suger.ui.history.BloodSugarHistoryItem(it) }
                        }
                    }
                    com.healthtracker.blood.suger.ui.history.HistoryRecordItem.RecordType.HEART_RATE -> {
                        heartRateRepository.getLatestHeartRateRecords(1).collect { list ->
                            _recentRecord.value = list.firstOrNull()?.let { com.healthtracker.blood.suger.ui.history.HeartRateHistoryItem(it) }
                        }
                    }
                    com.healthtracker.blood.suger.ui.history.HistoryRecordItem.RecordType.BMI_RECORD -> {
                        bmiRepository.getLatestBmiRecords(1).collect { list ->
                            _recentRecord.value = list.firstOrNull()?.let { com.healthtracker.blood.suger.ui.history.BmiHistoryItem(it) }
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