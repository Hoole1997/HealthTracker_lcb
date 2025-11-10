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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.roundToInt
import javax.inject.Inject

data class BloodSugarData(
    val timestamp: Long,
    val value: Float,
    val type: Int
)

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

    // Mock 数据开关（响应式）
    private val _useMockData = MutableStateFlow(true)

    // 根据开关在仓库与模拟数据之间切换，统一输出 mg/dL 的原始值
    private val chartSourceFlow = _useMockData.flatMapLatest { useMock ->
        if (useMock) {
            // generateMock 返回的是 mmol/L，这里转换到 mg/dL 统一到图表层再按 unit 转换显示
            kotlinx.coroutines.flow.flow {
                val mmolList = generateMock()
                val mgdlList = mmolList.map { item ->
                    val mgdl = ((BsUnit.mmolToMgdl(item.value) * 10).roundToInt() / 10f)
                    item.copy(value = mgdl)
                }
                emit(mgdlList)
            }
        } else {
            bloodSugarRepository.getChartBloodSugarRecords()
                .map { records ->
                    records.map { record ->
                        BloodSugarData(
                            timestamp = record.recordTime.time,
                            value = record.glucoseValue.toFloat(), // mg/dL 原始存储
                            type = record.satus
                        )
                    }
                }
        }
    }

    val chartUiState: StateFlow<ChartUiState> =
        combine(
            chartSourceFlow,
            chartUnit
        ) { points, unit ->
            val sortedPoints = points.sortedBy { it.timestamp }
            if (sortedPoints.isEmpty()) {
                ChartUiState()
            } else {
                val labels = sortedPoints.map { ts -> chartLabelFormatter.format(Date(ts.timestamp)) }
                val xValues = sortedPoints.indices.map(Int::toFloat)
                val yValues = sortedPoints.map { p ->
                    unit.convertFromMgdl(p.value.toDouble()).toFloat()
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

    // ---- Mock 数据与切换 ----
    fun toggleMockData(enable: Boolean) {
        _useMockData.value = enable
        notifyDataChanged()
    }

    fun getData(): List<BloodSugarData> {
        return if (_useMockData.value) {
            generateMock()
        } else {
            queryRepoData()
        }
    }

    private fun notifyDataChanged() {

    }

    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun generateMock(days: Int = 365): List<BloodSugarData> {
        if (days <= 0) throw IllegalArgumentException("days must be > 0")

        val total = days * 4
        val result = ArrayList<BloodSugarData>(total)
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val rnd = ThreadLocalRandom.current()

        fun round1(v: Float): Float = (v * 10f).roundToInt() / 10f

        fun valueForType(type: Int): Float {
            val base = if (type == 0) 5.5f else 7.0f
            val offset = rnd.nextFloat() * 3.0f - 1.5f // [-1.5, 1.5]
            return round1(base + offset)
        }

        fun randomInWindow(dayStartMillis: Long, startMin: Int, endMin: Int): Long {
            val windowStart = dayStartMillis + startMin * 60_000L
            val windowLenMs = ((endMin - startMin).coerceAtLeast(1)) * 60_000L
            val delta = rnd.nextLong(windowLenMs)
            return windowStart + delta
        }

        val daysSeq = sequence {
            var d = 0
            while (d < days) {
                yield(d)
                d++
            }
        }

        for (d in daysSeq) {
            cal.timeInMillis = now - d * 24L * 60 * 60 * 1000
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis

            val tsFasting = randomInWindow(dayStart, 6 * 60, 8 * 60)
            val tsBreakfast = randomInWindow(dayStart, 8 * 60 + 30, 10 * 60 + 30)
            val tsLunch = randomInWindow(dayStart, 12 * 60 + 30, 14 * 60 + 30)
            val tsDinner = randomInWindow(dayStart, 18 * 60 + 30, 20 * 60 + 30)

            result.add(BloodSugarData(tsFasting, valueForType(0), 0))
            result.add(BloodSugarData(tsBreakfast, valueForType(1), 1))
            result.add(BloodSugarData(tsLunch, valueForType(2), 2))
            result.add(BloodSugarData(tsDinner, valueForType(3), 3))
        }

        result.sortBy { it.timestamp }
        return result
    }

    private fun queryRepoData(): List<BloodSugarData> = runBlocking {
        bloodSugarRepository.getChartBloodSugarRecords().first().map { record ->
            // 统一按 mg/dL 返回，在图表层根据 chartUnit 再转换
            val valueMgdl = record.glucoseValue.toFloat()
            val rounded = (valueMgdl * 10f).roundToInt() / 10f
            BloodSugarData(
                timestamp = record.recordTime.time,
                value = rounded,
                type = record.satus
            )
        }
    }
}
