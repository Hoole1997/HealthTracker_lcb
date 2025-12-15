package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.enums.HeartRateStatus
import com.healthtracker.blood.suger.data.enums.TagType
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.blood.suger.data.repository.HealthTagRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.data.utils.TagUtils
import com.healthtracker.blood.suger.constants.KEY_LAST_RECORD_TYPE
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * 心率记录 ViewModel
 * 负责管理心率记录页面的状态与业务逻辑
 */
@HiltViewModel
class HeartRateRecordViewModel @Inject constructor(
    private val heartRateRepository: HeartRateRepository,
    private val healthTagRepository: HealthTagRepository
) : BaseViewModel() {

    private var editingRecordId: Long? = null

    private val _heartRate = MutableStateFlow(70)
    val heartRate: StateFlow<Int> = _heartRate.asStateFlow()

    private val _status = MutableStateFlow(HeartRateStatus.fromHeartRate(_heartRate.value))
    val status: StateFlow<HeartRateStatus> = _status.asStateFlow()

    private val _recordTime = MutableStateFlow(DateTimeUtils.now())
    val recordTime: StateFlow<Date> = _recordTime.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<List<Long>>(emptyList())
    val selectedTagIds: StateFlow<List<Long>> = _selectedTagIds.asStateFlow()

    private val _availableTags = MutableStateFlow<List<HealthTag>>(emptyList())
    val availableTags: StateFlow<List<HealthTag>> = _availableTags.asStateFlow()

    private val _isTagsLoading = MutableStateFlow(false)
    val isTagsLoading: StateFlow<Boolean> = _isTagsLoading.asStateFlow()

    fun getHeartRateTagsFlow() = healthTagRepository.getTagsByType(TagType.HEART_RATE)

    fun initialize(recordId: Long?) {
        editingRecordId = recordId
        if (recordId != null) {
            loadRecord(recordId)
        }
        initializeTags()
    }



    private fun loadRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val record = heartRateRepository.getHeartRateRecordById(recordId)
                if (record != null) {
                    _heartRate.value = record.heartRateBpm
                    _status.value = HeartRateStatus.fromHeartRate(record.heartRateBpm)
                    _recordTime.value = record.recordTime
                    _selectedTagIds.value = record.getTagIdList()
                    "Loaded heart rate record: $recordId".logd(TAG)
                } else {
                    "Heart rate record not found: $recordId".loge(TAG)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                "Failed to load heart rate record: ${e.message}".loge(TAG)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateHeartRate(value: Int) {
        _heartRate.value = value
        _status.value = HeartRateStatus.fromHeartRate(value)
    }

    fun updateRecordTime(date: Date) {
        _recordTime.value = date
    }

    fun addTag(tagId: Long) {
        if (!_selectedTagIds.value.contains(tagId)) {
            _selectedTagIds.value = _selectedTagIds.value + tagId
        }
    }

//    fun removeTag(tagId: Long) {
//        _selectedTagIds.value = _selectedTagIds.value.filter { it != tagId }
//    }

    fun clearSelectedTags() {
        _selectedTagIds.value = emptyList()
    }

    fun initializeTags() {
        viewModelScope.launch {
            try {
                _isTagsLoading.value = true
                healthTagRepository.initializePredefinedTags()
                healthTagRepository.getTagsByType(TagType.HEART_RATE).collect { tags ->
                    _availableTags.value = tags
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTagsLoading.value = false
            }
        }
    }

    fun saveHeartRateRecord(onResult: (SaveRecordResult) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val bpm = _heartRate.value
                if (bpm < MIN_HEART_RATE || bpm > MAX_HEART_RATE) {
                    onResult(SaveRecordResult.Failed("心率值不在有效范围内"))
                    return@launch
                }

                val recordId = editingRecordId
                val tags = _selectedTagIds.value
                if (recordId == null) {
                    val newId = heartRateRepository.addHeartRateRecord(
                        heartRateBpm = bpm,
                        recordTime = _recordTime.value,
                        tagIds = tags
                    )
                    if (newId > 0) {
                        editingRecordId = newId
                        SpUtils.putInt(KEY_LAST_RECORD_TYPE, HistoryRecordItem.RecordType.HEART_RATE.ordinal)
                        onResult(SaveRecordResult.Created(newId))
                    } else {
                        onResult(SaveRecordResult.Failed("创建失败"))
                    }
                } else {
                    val existing = heartRateRepository.getHeartRateRecordById(recordId)
                    if (existing == null) {
                        onResult(SaveRecordResult.Failed("记录不存在"))
                    } else {
                        val updated = existing.copy(
                            recordTime = _recordTime.value,
                            heartRateBpm = bpm,
                            tagIds = TagUtils.tagIdsToString(tags)
                        )
                        val rows = heartRateRepository.updateHeartRateRecord(updated)
                        if (rows > 0) {
                            com.healthtracker.framework.util.SpUtils.putInt(
                                com.healthtracker.blood.suger.constants.KEY_LAST_RECORD_TYPE,
                                com.healthtracker.blood.suger.ui.history.HistoryRecordItem.RecordType.HEART_RATE.ordinal
                            )
                            onResult(SaveRecordResult.Updated(updated.id))
                        } else {
                            onResult(SaveRecordResult.Failed("更新失败"))
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(SaveRecordResult.Failed(e.message ?: "保存心率记录失败"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTag(tag: HealthTag) {
        viewModelScope.launch {
            try {
                if (healthTagRepository.deleteTag(tag)) {
                    _selectedTagIds.value = _selectedTagIds.value.filter { it != tag.id }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun createCustomTag(tagName: String): Long {
        return try {
            if (healthTagRepository.isTagNameExists(TagType.HEART_RATE, tagName)) {
                -1L
            } else {
                val tag = healthTagRepository.createCustomTag(tagName, TagType.HEART_RATE)
                val id = tag.id
                if (id > 0) {
                    val current = _selectedTagIds.value.toMutableList()
                    if (!current.contains(id)) {
                        current.add(id)
                        _selectedTagIds.value = current
                    }
                }
                id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    suspend fun removeTag(tagId: Long) {
        try {
            if (healthTagRepository.deleteTagById(tagId)) {
                _selectedTagIds.value = _selectedTagIds.value.filter { it != tagId }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSelectedTagsDisplayText(): String {
        val selected = _availableTags.value.filter { _selectedTagIds.value.contains(it.id) }
        return selected.joinToString(", ") { it.name }
    }

    sealed class SaveRecordResult {
        data class Created(val recordId: Long) : SaveRecordResult()
        data class Updated(val recordId: Long) : SaveRecordResult()
        data class Failed(val error: String) : SaveRecordResult()

        fun getRecordId(): Long? = when (this) {
            is Created -> recordId
            is Updated -> recordId
            is Failed -> null
        }
    }

    companion object {
        private const val TAG = "HeartRateRecordVM"
        private const val MIN_HEART_RATE = 40
        private const val MAX_HEART_RATE = 220
    }
}
