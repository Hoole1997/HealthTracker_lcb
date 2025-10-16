package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.enums.HeartRateStatus
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HeartRateDetailViewModel @Inject constructor(
    private val heartRateRepository: HeartRateRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    companion object {
        const val RECORD_ID = "record_id"
    }

    private val _record = MutableStateFlow<HeartRateRecord?>(null)
    val record: StateFlow<HeartRateRecord?> = _record.asStateFlow()

    private val _status = MutableStateFlow<HeartRateStatus?>(null)
    val status: StateFlow<HeartRateStatus?> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val recordId: Long = savedStateHandle.get<Long>(RECORD_ID) ?: -1L

    init {
        if (recordId != -1L) {
            loadRecord(recordId)
        } else {
            _error.value = "Invalid record id"
        }
    }

    private fun loadRecord(id: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = heartRateRepository.getHeartRateRecordById(id)
                _record.value = result
                _status.value = result?.let { HeartRateStatus.fromHeartRate(it.heartRateBpm) }
                if (result == null) {
                    _error.value = "Heart rate record not found"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load heart rate record"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        if (recordId != -1L) {
            loadRecord(recordId)
        }
    }

    fun clearError() {
        _error.value = null
    }
}
