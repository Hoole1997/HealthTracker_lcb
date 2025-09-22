package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 血压详情页ViewModel
 * 负责加载和管理血压记录详情数据
 */
@HiltViewModel
class BpDetailViewModel @Inject constructor(
    private val bloodPressureRepository: BloodPressureRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    companion object {
        const val RECORD_ID = "record_id"
    }

    // 血压记录数据
    private val _bloodPressureRecord = MutableStateFlow<BloodPressureRecord?>(null)
    val bloodPressureRecord: StateFlow<BloodPressureRecord?> = _bloodPressureRecord.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误状态
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 获取传递的记录ID
    private val recordId: Long = savedStateHandle.get<Long>(RECORD_ID) ?: -1L

    init {
        if (recordId != -1L) {
            loadBloodPressureRecord(recordId)
        }
    }

    /**
     * 加载血压记录详情
     * @param recordId 记录ID
     */
    private fun loadBloodPressureRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val record = bloodPressureRepository.getBloodPressureRecordById(recordId)
                _bloodPressureRecord.value = record

                if (record == null) {
                    _error.value = "未找到对应的血压记录"
                }
            } catch (e: Exception) {
                _error.value = "加载血压记录失败：${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        if (recordId != -1L) {
            loadBloodPressureRecord(recordId)
        }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 删除血压记录
     */
    suspend fun deleteRecord(): Boolean {
        return try {
            _isLoading.value = true
            _bloodPressureRecord.value?.let { record ->
                bloodPressureRepository.deleteBloodPressureRecord(record.id)
                true
            } ?: false
        } catch (e: Exception) {
            _error.value = "删除记录失败：${e.message}"
            e.printStackTrace()
            false
        } finally {
            _isLoading.value = false
        }
    }
}