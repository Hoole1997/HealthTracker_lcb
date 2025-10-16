package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.enums.HeartRateStatus
import com.healthtracker.blood.suger.data.repository.HealthTagRepository
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HeartRateDetailViewModel @Inject constructor(
    private val heartRateRepository: HeartRateRepository,
    private val healthTagRepository: HealthTagRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    companion object {
        const val RECORD_ID = "record_id"
    }

    private val _record = MutableStateFlow<HeartRateRecord?>(null)
    val record: StateFlow<HeartRateRecord?> = _record.asStateFlow()

    private val _status = MutableStateFlow<HeartRateStatus?>(null)
    val status: StateFlow<HeartRateStatus?> = _status.asStateFlow()

    private val _tags = MutableStateFlow<List<HealthTag>>(emptyList())
    val tags: StateFlow<List<HealthTag>> = _tags.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val recordId: Long = savedStateHandle.get<Long>(RECORD_ID) ?: -1L
    private var hasNotifiedMissing = false

    private var isDelete = false

    init {
        if (recordId != -1L) {
            observeRecord(recordId)
        } else {
            _error.value = "Invalid record id"
        }
    }

    private fun observeRecord(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            heartRateRepository.observeHeartRateRecordById(id)
                .collect { result ->
                    _record.value = result
                    if (result != null) {
                        hasNotifiedMissing = false
                        _status.value = HeartRateStatus.fromHeartRate(result.heartRateBpm)
                        loadTags(result.getTagIdList())
                        _isLoading.value = false
                    } else {
                        _status.value = null
                        _tags.value = emptyList()
                        _isLoading.value = false
                        if (!hasNotifiedMissing && !isDelete) {
                            _error.value = "Heart rate record not found"
                            hasNotifiedMissing = true
                        }
                    }
                }
        }
    }

    private suspend fun loadTags(ids: List<Long>) {
        try {
            _tags.value = if (ids.isEmpty()) {
                emptyList()
            } else {
                healthTagRepository.getTagsByIds(ids)
            }
        } catch (e: Exception) {
            _tags.value = emptyList()
        }
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun deleteRecord(): Boolean {
        return try {
            val id = _record.value?.id ?: return false
            _isLoading.value = true
            isDelete = true
            heartRateRepository.deleteHeartRateRecord(id) > 0
        } catch (e: Exception) {
            _error.value = e.message ?: "Delete failed"
            false
        } finally {
            _isLoading.value = false
        }
    }

    fun currentRecordId(): Long? = _record.value?.id
}
