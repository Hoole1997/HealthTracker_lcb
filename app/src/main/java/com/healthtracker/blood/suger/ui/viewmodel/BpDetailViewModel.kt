package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.ui.chart.ChartDataSet
import com.healthtracker.blood.suger.ui.chart.ChartSeriesIds
import com.healthtracker.blood.suger.ui.chart.ChartUiState
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
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

    // 血压记录数据，使用StateFlow进行状态管理
    private val _bloodPressureRecord = MutableStateFlow<BloodPressureRecord?>(null)
    val bloodPressureRecord: StateFlow<BloodPressureRecord?> = _bloodPressureRecord.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误状态
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val chartLabelFormatter = SimpleDateFormat("M.dd", Locale.getDefault())

    val chartUiState: StateFlow<ChartUiState> =
        bloodPressureRepository.getChartBloodPressureRecords()
            .map { records ->
                val sortedRecords = records.sortedBy { it.recordTime }
                if (sortedRecords.isEmpty()) {
                    ChartUiState()
                } else {
                    val labels = sortedRecords.map { chartLabelFormatter.format(it.recordTime) }
                    val xValues = sortedRecords.indices.map { it.toFloat() }
                    ChartUiState(
                        labels = labels,
                        dataSets = listOf(
                            ChartDataSet(
                                id = ChartSeriesIds.BP_SYS,
                                label = "Systolic",
                                xValues = xValues,
                                yValues = sortedRecords.map { it.systolicPressure.toFloat() }
                            ),
                            ChartDataSet(
                                id = ChartSeriesIds.BP_DIA,
                                label = "Diastolic",
                                xValues = xValues,
                                yValues = sortedRecords.map { it.diastolicPressure.toFloat() }
                            )
                        )
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ChartUiState()
            )

    // 获取传递的记录ID
    private val recordId: Long = savedStateHandle.get<Long>(RECORD_ID) ?: -1L

    init {
        // 获取传入的记录ID
        "BpDetailViewModel init with recordId: $recordId".logd(TAG)
        
        if (recordId != -1L) {
            // 使用Flow响应式查询，自动监听数据变化
            observeBloodPressureRecord(recordId)
        } else {
            "Invalid recordId: $recordId".loge(TAG)
        }
    }

    /**
     * 使用Flow响应式查询血压记录，支持数据变化监听
     * @param recordId 记录ID
     */
    private fun observeBloodPressureRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                "开始监听血压记录变化，recordId: $recordId".logd(TAG)
                
                // 使用Repository的Flow方法进行响应式查询
                bloodPressureRepository.getBloodPressureRecordByIdFlow(recordId)
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = null
                    )
                    .collect { record ->
                        "血压记录数据更新: $record".logd(TAG)
                        _bloodPressureRecord.value = record
                    }
            } catch (e: CancellationException) {
                "血压记录查询被取消".logd(TAG)
                throw e
            } catch (e: Exception) {
                "查询血压记录失败: ${e.message}".loge(TAG)
                _bloodPressureRecord.value = null
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        if (recordId != -1L) {
            observeBloodPressureRecord(recordId)
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
            _error.value = "Failed to delete record: ${e.message}"
            e.printStackTrace()
            false
        } finally {
            _isLoading.value = false
        }
    }
}
