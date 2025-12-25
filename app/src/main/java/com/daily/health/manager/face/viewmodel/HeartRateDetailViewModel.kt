package com.daily.health.manager.face.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.daily.health.manager.data.entity.HealthTag
import com.daily.health.manager.data.entity.HeartRateRecord
import com.daily.health.manager.data.enums.HeartRateStatus
import com.daily.health.manager.data.repository.HealthTagRepository
import com.daily.health.manager.data.repository.HeartRateRepository
import com.daily.health.manager.face.chart.ChartDataSet
import com.daily.health.manager.face.chart.ChartSeriesIds
import com.daily.health.manager.face.chart.ChartUiState
import com.daily.health.manager.util.ChartPalette
import com.daily.health.manager.util.LineStyle
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom

class HeartRateDetailViewModel(
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

    private val chartLabelFormatter = SimpleDateFormat("M/d", Locale.getDefault())

    val chartUiState: StateFlow<ChartUiState> =
        heartRateRepository.getChartHeartRateRecords()
            .map { records ->
                val sortedRecords = records.sortedWith(compareBy<HeartRateRecord> { it.recordTime.time }.thenBy { it.updatedAt })
                if (sortedRecords.isEmpty()) {
                    ChartUiState()
                } else {
                    val labels = sortedRecords.map { chartLabelFormatter.format(it.recordTime) }
                    val xValues = sortedRecords.indices.map(Int::toFloat)
                    val yValues = sortedRecords.map { it.heartRateBpm.toFloat() }
                    ChartUiState(
                        labels = labels,
                        dataSets = listOf(
                            ChartDataSet(
                                id = ChartSeriesIds.HR_MAIN,
                                label = "BPM",
                                xValues = xValues,
                                yValues = yValues,
                                style = LineStyle(color = ChartPalette.lineHeartRate)
                            )
                        ),
                        forceIntegerYAxis = true
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ChartUiState()
            )

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

    private fun generateMockHeartRatePoints(days: Int = 7): List<HeartRatePoint> {
        val cal = Calendar.getInstance()
        val random = ThreadLocalRandom.current()
        return (0 until days).map { offset ->
            cal.timeInMillis = System.currentTimeMillis() - offset * 24L * 60 * 60 * 1000
            cal.set(Calendar.MINUTE, random.nextInt(60))
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.set(Calendar.HOUR_OF_DAY, 6 + random.nextInt(12))
            val bpm = 63 + random.nextInt(24)
            HeartRatePoint(cal.timeInMillis, bpm)
        }.sortedBy { it.timestamp }
    }

    private data class HeartRatePoint(val timestamp: Long, val bpm: Int)
}
