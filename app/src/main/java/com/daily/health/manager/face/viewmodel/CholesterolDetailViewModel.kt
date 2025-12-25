package com.daily.health.manager.face.viewmodel

import androidx.lifecycle.viewModelScope
import com.daily.health.manager.data.entity.CholesterolRecord
import com.daily.health.manager.data.enums.CholesterolLevel
import com.daily.health.manager.data.repository.CholesterolRepository
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
import java.util.Date
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom

class CholesterolDetailViewModel(
    private val cholesterolRepository: CholesterolRepository
) : BaseViewModel() {

    private val chartLabelFormatter = SimpleDateFormat("M/d", Locale.getDefault())

    private val _cholesterolRecord = MutableStateFlow<CholesterolRecord?>(null)
    val cholesterolRecord: StateFlow<CholesterolRecord?> = _cholesterolRecord.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val chartUiState: StateFlow<ChartUiState> =
        cholesterolRepository.getAllRecords()
            .map { records ->
                val sorted = records
                    .sortedWith(compareBy<CholesterolRecord> { it.recordTime.time }.thenBy { it.updatedAt })

                if (sorted.isEmpty()) {
                    ChartUiState()
                } else {
                    val labels = sorted.map { chartLabelFormatter.format(it.recordTime) }
                    val xValues = sorted.indices.map(Int::toFloat)

                    val tgValues = sorted.map { it.triglyceride.toFloat() }
                    val ldlValues = sorted.map { it.ldl.toFloat() }
                    val hdlValues = sorted.map { it.hdl.toFloat() }

                ChartUiState(
                    labels = labels,
                    dataSets = listOf(
                        ChartDataSet(
                            id = ChartSeriesIds.CHO_TG,
                            label = "TG",
                            xValues = xValues,
                            yValues = tgValues,
                            style = LineStyle(color = ChartPalette.lineCholesterolTg)
                        ),
                        ChartDataSet(
                            id = ChartSeriesIds.CHO_LDL,
                            label = "LDL",
                            xValues = xValues,
                            yValues = ldlValues,
                            style = LineStyle(color = ChartPalette.lineCholesterolLdl)
                        ),
                        ChartDataSet(
                            id = ChartSeriesIds.CHO_HDL,
                            label = "HDL",
                            xValues = xValues,
                            yValues = hdlValues,
                            style = LineStyle(color = ChartPalette.lineCholesterolHdl)
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
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cholesterolRepository.observerRecord(recordId).collect { record ->
                    _cholesterolRecord.value = record
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取HDL值（整数）
     */
    fun getHdlValue(): String {
        val hdl = _cholesterolRecord.value?.hdl
        return hdl?.toString() ?: "--"
    }

    /**
     * 获取TC/HDL比率（2位小数）
     */
    fun getTcHdlRatio(): String {
        val ratio = _cholesterolRecord.value?.tcHdlRatio
        return ratio?.let { String.format(Locale.getDefault(),"%.2f", it) } ?: "--"
    }

    /**
     * 获取LDL/HDL比率（1位小数）
     */
    fun getLdlHdlRatio(): String {
        val ratio = _cholesterolRecord.value?.ldlHdlRatio
        return ratio?.let { String.format(Locale.getDefault(),"%.2f", it) } ?: "--"
    }

    /**
     * 获取记录时间
     */
    fun getRecordTime(): Date? {
        return _cholesterolRecord.value?.recordTime
    }

    /**
     * 获取胆固醇水平
     */
    fun getCholesterolLevel(): CholesterolLevel {
        val record = _cholesterolRecord.value ?: return CholesterolLevel.UNKNOWN
        return CholesterolLevel.fromMetrics(
            totalCholesterol = record.tc,
            nonHdl = record.nonHdl,
            ldl = record.ldl?.toFloat(),
            hdl = record.hdl?.toFloat()
        )
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 删除记录
     */
    suspend fun deleteRecord(): Boolean {
        val recordId = _cholesterolRecord.value?.id ?: return false
        return try {
            val result = cholesterolRepository.deleteCholesterolRecord(recordId)
            result > 0
        } catch (e: Exception) {
            _errorMessage.value = e.message
            false
        }
    }

    private fun generateMockCholesterolRecords(days: Int = 7): List<CholesterolRecord> {
        val cal = Calendar.getInstance()
        val random = ThreadLocalRandom.current()
        return (0 until days).map { offset ->
            cal.timeInMillis = System.currentTimeMillis() - offset * 24L * 60 * 60 * 1000
            cal.set(Calendar.HOUR_OF_DAY, 7 + random.nextInt(10))
            cal.set(Calendar.MINUTE, random.nextInt(60))
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val tg = 90 + random.nextInt(110)
            val ldl = 80 + random.nextInt(70)
            val hdl = 40 + random.nextInt(20)
            val tc = tg / 5f + ldl + hdl
            val nonHdl = tc - hdl
            val tcHdlRatio = if (hdl == 0) 0f else tc / hdl
            val ldlHdlRatio = if (hdl == 0) 0f else ldl.toFloat() / hdl

            CholesterolRecord(
                recordTime = Date(cal.timeInMillis),
                hdl = hdl,
                ldl = ldl,
                triglyceride = tg,
                tc = tc,
                nonHdl = nonHdl,
                tcHdlRatio = tcHdlRatio,
                ldlHdlRatio = ldlHdlRatio
            )
        }.sortedBy { it.recordTime }
    }

}
