package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.HealthTagRepository
import com.healthtracker.blood.suger.ui.chart.ChartDataSet
import com.healthtracker.blood.suger.ui.chart.ChartSeriesIds
import com.healthtracker.blood.suger.ui.chart.ChartUiState
import com.healthtracker.blood.suger.util.LineStyle
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BsDetailViewModel @Inject constructor(
    private val bloodSugarRepository: BloodSugarRepository,
    private val healthTagRepository: HealthTagRepository
) : BaseViewModel() {

    // 血糖记录状态
    private val _bloodSugarRecord = MutableStateFlow<BloodSugarRecord?>(null)
    val bloodSugarRecord: StateFlow<BloodSugarRecord?> = _bloodSugarRecord.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误状态
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _tags = MutableStateFlow<List<HealthTag>>(emptyList())
    val tags: StateFlow<List<HealthTag>> = _tags.asStateFlow()

    private val _chartUnit = MutableStateFlow(BsUnit.getPreferredUnit())
    private val chartUnit = _chartUnit.asStateFlow()

    private val chartLabelFormatter = SimpleDateFormat("M.dd", Locale.getDefault())

    val chartUiState: StateFlow<ChartUiState> =
        combine(
            bloodSugarRepository.getChartBloodSugarRecords(),
            chartUnit
        ) { records, unit ->
            val sortedRecords = records.sortedBy { it.recordTime }
            if (sortedRecords.isEmpty()) {
                ChartUiState()
            } else {
                val labels = sortedRecords.map { chartLabelFormatter.format(it.recordTime) }
                val xValues = sortedRecords.indices.map(Int::toFloat)
                val yValues = sortedRecords.map { record ->
                    unit.convertFromMgdl(record.glucoseValue).toFloat()
                }
                ChartUiState(
                    labels = labels,
                    dataSets = listOf(
                        ChartDataSet(
                            id = ChartSeriesIds.BS_GLUCOSE,
                            label = unit.displayName,
                            xValues = xValues,
                            yValues = yValues,
                            style = LineStyle(color = "#FF6B4D")
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

    private var hasNotifiedMissing = false

    private var isDelete = false

    private suspend fun loadTags(ids: List<Long>) {
        try {
            _tags.value = if (ids.isEmpty()) emptyList() else healthTagRepository.getTagsByIds(ids)
        } catch (e: Exception) {
            _tags.value = emptyList()
        }
    }

    /**
     * 根据记录ID初始化并加载记录
     * @param recordId 血糖记录ID
     */
    fun initializeWithRecord(recordId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                bloodSugarRepository.observeBloodSugarRecordById(recordId).collect { record ->
                    _bloodSugarRecord.value = record
                    record?.let {
                        _chartUnit.value = it.getSelectedUnitEnum()
                    }
                    _isLoading.value = false
                    if (record == null) {
                        if (!hasNotifiedMissing && !isDelete) {
                            _error.value = "Blood sugar record not found"
                            hasNotifiedMissing = true
                        }
                    } else {
                        hasNotifiedMissing = false
                        loadTags(record.getTagIdList())
                    }
                }
            } catch (e: CancellationException) {
                "Blood sugar record loading cancelled: ID=$recordId".logd(TAG)
                throw e
            } catch (e: Exception) {
                "Failed to observe blood sugar record: ID=$recordId, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
                _error.value = "Failed to load blood sugar record: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取血糖状态
     */
    fun getBloodSugarStatus(): BloodSugarStatus? {
        return _bloodSugarRecord.value?.let { record ->
            convertMeasurementTagToBloodSugarStatus(record.satus)
        }
    }

    /**
     * 获取显示单位
     */
    fun getDisplayUnit(): BsUnit? {
        return _bloodSugarRecord.value?.getSelectedUnitEnum()
    }

    /**
     * 获取显示血糖值
     */
    fun getDisplayValue(): Float? {
        return _bloodSugarRecord.value?.getDisplayGlucoseValue()?.toFloat()
    }

    fun getRecordTime() = _bloodSugarRecord.value?.recordTime

    /**
     * 转换测量标签为血糖状态
     * 参考BsRecordViewModel的实现
     */
    private fun convertMeasurementTagToBloodSugarStatus(statusCode: Int): BloodSugarStatus {
        return BloodSugarStatus.fromStatusType(statusCode)
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun deleteRecord(): Boolean {
        return try {
            val id = _bloodSugarRecord.value?.id ?: return false
            _isLoading.value = true
            isDelete = true
            bloodSugarRepository.deleteBloodSugarRecord(id) > 0
        } catch (e: Exception) {
            _error.value = e.message ?: "Delete failed"
            false
        } finally {
            _isLoading.value = false
        }
    }
}
