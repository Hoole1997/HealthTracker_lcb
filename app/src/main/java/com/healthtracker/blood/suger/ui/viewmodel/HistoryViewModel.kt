package com.healthtracker.blood.suger.ui.viewmodel

import android.icu.text.DateFormat
import androidx.lifecycle.SavedStateHandle
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject

/**
 * 历史记录页面ViewModel
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val bpRepository: BloodPressureRepository,
    private val bsRepository: BloodSugarRepository,
    private var savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    // 日期范围状态
    private val _startDate = MutableStateFlow(0L)
    val startDate: StateFlow<Long> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(0L)
    val endDate: StateFlow<Long> = _endDate.asStateFlow()

    // 是否为血糖历史记录（true: 血糖, false: 血压）
    private val _isBloodSugarHistory = MutableStateFlow(true)
    val isBloodSugarHistory: StateFlow<Boolean> = _isBloodSugarHistory.asStateFlow()

    // 血糖状态类型筛选（null表示全部）
    private val _selectedBloodSugarStatus = MutableStateFlow<BloodSugarStatus?>(null)
    val selectedBloodSugarStatus: StateFlow<BloodSugarStatus?> = _selectedBloodSugarStatus.asStateFlow()

    // 格式化的日期范围显示字符串
    private val _dateRangeText = MutableStateFlow("")
    val dateRangeText: StateFlow<String> = _dateRangeText.asStateFlow()

    init {
        initDefaultDateRange()
    }

    /**
     * 初始化默认日期范围（去年今天 ~ 今天）
     */
    private fun initDefaultDateRange() {
        // 先获取本地今天的日期，然后转换为UTC的midnight
        val localCalendar = Calendar.getInstance()
        val year = localCalendar.get(Calendar.YEAR)
        val month = localCalendar.get(Calendar.MONTH)
        val dayOfMonth = localCalendar.get(Calendar.DAY_OF_MONTH)

        // 创建UTC时区的Calendar，设置为今天的midnight (UTC)
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.set(year, month, dayOfMonth, 0, 0, 0)
        utcCalendar.set(Calendar.MILLISECOND, 0)
        val endDate = utcCalendar.timeInMillis

        // 计算去年今天的日期
        val startUtcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        startUtcCalendar.set(year - 1, month, dayOfMonth, 0, 0, 0)
        startUtcCalendar.set(Calendar.MILLISECOND, 0)
        val startDate = startUtcCalendar.timeInMillis

        setDateRange(startDate, endDate)
    }

    /**
     * 设置日期范围
     */
    fun setDateRange(startDate: Long, endDate: Long) {
        _startDate.value = startDate
        _endDate.value = endDate
        updateDateRangeText()
    }

    /**
     * 设置历史记录类型
     */
    fun setHistoryType(isBloodSugar: Boolean) {
        _isBloodSugarHistory.value = isBloodSugar
    }

    /**
     * 设置血糖状态筛选
     */
    fun setBloodSugarStatusFilter(status: BloodSugarStatus?) {
        _selectedBloodSugarStatus.value = status
    }

    /**
     * 更新日期范围显示文本
     * 使用系统本地化日期格式
     */
    fun updateDateRangeText() {

        val dateFormat = DateFormat.getDateInstance()
        val startDateStr = dateFormat.format(Date(_startDate.value))
        val endDateStr = dateFormat.format(Date(_endDate.value))
        _dateRangeText.value = "$startDateStr - $endDateStr"
    }
}