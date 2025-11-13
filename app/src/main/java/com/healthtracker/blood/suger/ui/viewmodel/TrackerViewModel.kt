package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.blood.suger.ui.chart.ChartDataSet
import com.healthtracker.blood.suger.ui.chart.ChartSeriesIds
import com.healthtracker.blood.suger.ui.chart.ChartUiState
import com.healthtracker.blood.suger.util.ChartPalette
import com.healthtracker.blood.suger.util.LineStyle
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val bsRepository: BloodSugarRepository,
    private val bpRepository: BloodPressureRepository,
    private val hrRepository: HeartRateRepository,
    private val choRepository: CholesterolRepository,
    private val bmiRepository: BmiRepository
) : BaseViewModel() {

    companion object {
        /** 图表显示的记录数量 */
        private const val CHART_RECORDS_LIMIT = 7
    }

    // ========== 血糖 ==========
    private val _bsRecords = MutableStateFlow<List<BloodSugarRecord>>(emptyList())
    val bloodSugarChartState: StateFlow<ChartUiState> = _bsRecords
        .map { records -> buildBloodSugarChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    // ========== 血压 ==========
    private val _bpRecords = MutableStateFlow<List<BloodPressureRecord>>(emptyList())
    val bloodPressureChartState: StateFlow<ChartUiState> = _bpRecords
        .map { records -> buildBloodPressureChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    // ========== 心率 ==========
    private val _hrRecords = MutableStateFlow<List<HeartRateRecord>>(emptyList())
    val heartRateChartState: StateFlow<ChartUiState> = _hrRecords
        .map { records -> buildHeartRateChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    // ========== 胆固醇 ==========
    private val _choRecords = MutableStateFlow<List<CholesterolRecord>>(emptyList())
    val cholesterolChartState: StateFlow<ChartUiState> = _choRecords
        .map { records -> buildCholesterolChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    // ========== BMI ==========
    private val _bmiRecords = MutableStateFlow<List<BmiRecord>>(emptyList())
    val bmiChartState: StateFlow<ChartUiState> = _bmiRecords
        .map { records -> buildBmiChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    // 观察标记：防止重复订阅
    private var isObserving = false

    /**
     * 开始观察数据（由 Fragment 可见时调用）
     */
    fun startObservingData() {
        if (isObserving) return
        isObserving = true

        viewModelScope.launch {
            // 并行观察所有5个数据源
            launch {
                bsRepository.getLatestBloodSugarRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _bsRecords.value = records }
            }

            launch {
                bpRepository.getLatestBloodPressureRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _bpRecords.value = records }
            }

            launch {
                hrRepository.getLatestHeartRateRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _hrRecords.value = records }
            }

            launch {
                choRepository.getLatestCholesterolRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _choRecords.value = records }
            }

            launch {
                bmiRepository.getLatestBmiRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _bmiRecords.value = records }
            }
        }
    }

    /**
     * 构建血糖图表数据（已存在，保持不变）
     */
    private fun buildBloodSugarChart(records: List<BloodSugarRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        // 反转顺序：数据库已按 DESC 排序，图表需要 ASC 显示
        val sorted = records.reversed()

        // 简单数字标签
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }
        val yValues = sorted.map { it.glucoseValue.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BS_GLUCOSE,
                    xValues = xValues,
                    yValues = yValues,
                    label = "Blood Sugar",
                    style = LineStyle(color = ChartPalette.lineBloodSugar)
                )
            )
        )
    }

    /**
     * ✨ 新增：构建血压图表数据（双线：收缩压 + 舒张压）
     */
    private fun buildBloodPressureChart(records: List<BloodPressureRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val sorted = records.reversed()
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BP_SYS,
                    xValues = xValues,
                    yValues = sorted.map { it.systolicPressure.toFloat() },
                    label = "Systolic",
                    style = LineStyle(color = ChartPalette.lineBpSystolic)
                ),
                ChartDataSet(
                    id = ChartSeriesIds.BP_DIA,
                    xValues = xValues,
                    yValues = sorted.map { it.diastolicPressure.toFloat() },
                    label = "Diastolic",
                    style = LineStyle(color = ChartPalette.lineBpDiastolic)
                )
            )
        )
    }

    /**
     * ✨ 新增：构建心率图表数据（单线）
     */
    private fun buildHeartRateChart(records: List<HeartRateRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val sorted = records.reversed()
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }
        val yValues = sorted.map { it.heartRateBpm.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.HR_MAIN,
                    xValues = xValues,
                    yValues = yValues,
                    label = "Heart Rate",
                    style = LineStyle(color = ChartPalette.lineHeartRate)
                )
            )
        )
    }

    /**
     * ✨ 新增：构建胆固醇图表数据（三线：HDL + LDL + TG）
     */
    private fun buildCholesterolChart(records: List<CholesterolRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val sorted = records.reversed()
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.CHO_HDL,
                    xValues = xValues,
                    yValues = sorted.map { it.hdl.toFloat() },
                    label = "HDL",
                    style = LineStyle(color = ChartPalette.lineCholesterolHdl)
                ),
                ChartDataSet(
                    id = ChartSeriesIds.CHO_LDL,
                    xValues = xValues,
                    yValues = sorted.map { it.ldl.toFloat() },
                    label = "LDL",
                    style = LineStyle(color = ChartPalette.lineCholesterolLdl)
                ),
                ChartDataSet(
                    id = ChartSeriesIds.CHO_TG,
                    xValues = xValues,
                    yValues = sorted.map { it.triglyceride.toFloat() },
                    label = "TG",
                    style = LineStyle(color = ChartPalette.lineCholesterolTg)
                )
            )
        )
    }

    /**
     * ✨ 新增：构建BMI图表数据（单线，计算BMI值）
     */
    private fun buildBmiChart(records: List<BmiRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val sorted = records.reversed()
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }

        // 计算 BMI = weightKg / (heightCm/100)²
        val bmiValues = sorted.map { record ->
            val heightM = record.heightCm / 100.0
            (record.weightKg / heightM.pow(2)).toFloat()
        }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BMI,
                    xValues = xValues,
                    yValues = bmiValues,
                    label = "BMI",
                    style = LineStyle(color = ChartPalette.lineBmi)
                )
            )
        )
    }
}
