package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.ui.chart.ChartDataSet
import com.healthtracker.blood.suger.ui.chart.ChartSeriesIds
import com.healthtracker.blood.suger.ui.chart.ChartUiState
import com.healthtracker.blood.suger.util.ChartPalette
import com.healthtracker.blood.suger.util.LineStyle
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TrackerViewModel @Inject constructor(private val bsRepository: BloodSugarRepository) :
    BaseViewModel() {

    // 图表数据状态
    private val _chartUiState = MutableStateFlow(ChartUiState())
    val chartUiState: StateFlow<ChartUiState> = _chartUiState

    // 加载状态

    // 日期格式化器
    private val chartLabelFormatter = SimpleDateFormat("M/d", Locale.getDefault())

    init {
        loadBloodSugarChartData()
    }

    /**
     * 加载最近7条血糖图表数据
     */
    fun loadBloodSugarChartData() {
        viewModelScope.launch {
            try {
                // 获取用户选择的单位
                val unit = BsUnit.getPreferredUnit()
                
                // 查询最近7条血糖记录
                bsRepository.getRecentRecords(7)
                    .catch { e ->
                        _chartUiState.value = ChartUiState()
                    }
                    .collect { records ->
                        _chartUiState.value = buildBloodSugarChart(records, unit)
                    }
            } catch (e: Exception) {
                _chartUiState.value = ChartUiState()
            }
        }
    }

    /**
     * 构建血糖图表数据
     */
    private fun buildBloodSugarChart(records: List<BloodSugarRecord>, unit: BsUnit): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }
        
        // 按时间排序（升序）
        val sorted = records.sortedWith(
            compareBy<BloodSugarRecord> { it.recordTime.time }.thenBy { it.updatedAt }
        )
        
        // 构建标签和数据
        val labels = sorted.map { chartLabelFormatter.format(it.recordTime) }
        val xValues = sorted.indices.map { it.toFloat() }
        val yValues = sorted.map { record ->
            // 转换为用户选择的单位
            unit.convertFromMgdl(record.glucoseValue).toFloat()
        }
        
        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BS_GLUCOSE,
                    xValues = xValues,
                    yValues = yValues,
                    label = unit.displayName,
                    style = LineStyle(color = ChartPalette.lineBloodSugar)
                )
            )
        )
    }

}