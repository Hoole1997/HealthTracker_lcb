package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.enums.BMIEnum
import com.healthtracker.blood.suger.data.enums.BmiUnit
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.blood.suger.ui.chart.ChartDataSet
import com.healthtracker.blood.suger.ui.chart.ChartSeriesIds
import com.healthtracker.blood.suger.ui.chart.ChartUiState
import com.healthtracker.blood.suger.util.LineStyle
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToInt

@HiltViewModel
class BmiDetailViewModel @Inject constructor(
    private val bmiRepository: BmiRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    companion object {
        private const val TAG = "BmiDetailViewModel"
        private const val RECORD_ID = "record_id"
    }

    // 编辑模式的记录ID
    private var recordId: Long? = null

    // BMI 记录数据
    private val _bmiRecord = MutableStateFlow<BmiRecord?>(null)
    val bmiRecord: StateFlow<BmiRecord?> = _bmiRecord.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val chartLabelFormatter = SimpleDateFormat("M.dd", Locale.getDefault())

    val chartUiState: StateFlow<ChartUiState> =
        bmiRepository.getChartBmiRecords()
            .map { records ->
                val sortedRecords = records.sortedBy { it.recordTime }
                    val points = sortedRecords.mapNotNull { record ->
                        val heightMeters = record.heightCm / 100.0
                        if (heightMeters <= 0) {
                            null
                        } else {
                            val bmiRaw = (record.weightKg / heightMeters.pow(2.0)).toFloat()
                            val bmiValue = ((bmiRaw * 10).roundToInt() / 10f)
                            record to bmiValue
                        }
                    }

                if (points.isEmpty()) {
                    ChartUiState()
                } else {
                    val labels = points.map { (record, _) -> chartLabelFormatter.format(record.recordTime) }
                    val xValues = points.indices.map(Int::toFloat)
                    val yValues = points.map { it.second }
                    ChartUiState(
                        labels = labels,
                        dataSets = listOf(
                            ChartDataSet(
                                id = ChartSeriesIds.BMI,
                                label = "BMI",
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

    /**
     * 初始化并加载记录
     */
    fun initializeWithRecord(recordId: Long) {
        this.recordId = recordId
        loadRecord(recordId)
    }

    /**
     * 加载 BMI 记录
     */
    private fun loadRecord(recordId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                "Loading BMI record with ID: $recordId".logd(TAG)
                bmiRepository.observerRecord(recordId).collect { record ->
                    "BMI record loaded: $record".logd(TAG)
                    _bmiRecord.value = record
                }
            } catch (e: Exception) {
                "Failed to load BMI record: ${e.message}".logd(TAG)
                _error.value = "Failed to load record: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 计算 BMI 值
     * BMI = weight (kg) / [height (m)]²
     */
    fun calculateBmi(): Float? {
        val record = _bmiRecord.value ?: return null
        val heightInMeters = record.heightCm / 100.0
        if (heightInMeters <= 0) return null

        val bmi = record.weightKg / heightInMeters.pow(2.0)
        return bmi.toFloat()
    }

    /**
     * 获取 BMI 分类
     */
    fun getBmiCategory(): BMIEnum? {
        val bmi = calculateBmi() ?: return null
        return BMIEnum.fromBmi(bmi)
    }

    /**
     * 获取体重值（kg）
     */
    fun getWeightValue(): Double? {
        return _bmiRecord.value?.weightKg
    }

    /**
     * 获取身高值（cm）
     */
    fun getHeightValue(): Double? {
        return _bmiRecord.value?.heightCm
    }

    /**
     * 获取记录时间
     */
    fun getRecordTime() = _bmiRecord.value?.recordTime

    /**
     * 获取格式化的显示体重（根据用户偏好单位）
     */
    fun getDisplayWeight(): String {
        val weightKg = _bmiRecord.value?.weightKg ?: return "--"
        val preferredUnit = BmiUnit.getPreferredWeightUnit()
        return BmiUnit.formatDisplayWeight(weightKg.toFloat(), preferredUnit)
    }

    /**
     * 获取格式化的显示身高（根据用户偏好单位）
     */
    fun getDisplayHeight(): String {
        val heightCm = _bmiRecord.value?.heightCm ?: return "--"
        val preferredUnit = BmiUnit.getPreferredHeightUnit()
        return BmiUnit.formatDisplayHeight(heightCm.toFloat(), preferredUnit)
    }

    /**
     * 删除记录
     */
    suspend fun deleteRecord(): Boolean {
        val id = recordId ?: return false
        return try {
            _isLoading.value = true
            val result = bmiRepository.deleteBmiRecord(id)
            result > 0
        } catch (e: Exception) {
            "Failed to delete record: ${e.message}".logd(TAG)
            _error.value = "Failed to delete record: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = null
    }

    private fun generateMockBmiPoints(entries: Int = 7): List<BmiChartPoint> {
        val cal = Calendar.getInstance()
        val random = ThreadLocalRandom.current()
        return (0 until entries).map { offset ->
            cal.timeInMillis = System.currentTimeMillis() - offset * 24L * 60 * 60 * 1000
            cal.set(Calendar.HOUR_OF_DAY, 7 + random.nextInt(12))
            cal.set(Calendar.MINUTE, random.nextInt(60))
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val value = 20f + random.nextFloat() * 12f
            val rounded = (value * 10f).roundToInt() / 10f
            BmiChartPoint(cal.timeInMillis, rounded)
        }.sortedBy { it.timestamp }
    }

    private data class BmiChartPoint(val timestamp: Long, val value: Float)
}
