package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
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

@HiltViewModel
class TrackerViewModel @Inject constructor(private val bsRepository: BloodSugarRepository) :
    BaseViewModel() {

    companion object {
        /** 图表显示的记录数量 */
        private const val CHART_RECORDS_LIMIT = 7
    }

    // 私有：接收数据库变化
    private val _records = MutableStateFlow<List<BloodSugarRecord>>(emptyList())

    // 公开：转换后的图表状态
    val chartUiState: StateFlow<ChartUiState> = _records
        .map { records -> buildBloodSugarChart(records) }
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
            bsRepository.getLatestBloodSugarRecords(CHART_RECORDS_LIMIT)
                .collect { records ->
                    _records.value = records
                }
        }
    }

    /**
     * 构建血糖图表数据
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

}
