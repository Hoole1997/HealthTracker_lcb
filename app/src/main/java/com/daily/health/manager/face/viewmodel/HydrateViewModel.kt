package com.daily.health.manager.face.viewmodel

import androidx.lifecycle.viewModelScope
import com.daily.health.manager.data.repository.HydrateRepository
import com.daily.health.manager.face.adapter.HydrateRecordItem
import com.daily.health.manager.data.utils.DateTimeUtils
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Calendar

class HydrateViewModel(
    private val hydrateRepository: HydrateRepository
) : BaseViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // 选中日期（默认今天）
    private val _selectedDate = MutableStateFlow(DateTimeUtils.now())
    val selectedDate = _selectedDate.asStateFlow()

    // 按选中日期的饮水记录列表
    val todayRecords = _selectedDate.flatMapLatest { date ->
        val (startOfDay, endOfDay) = getDayRange(date)
        hydrateRepository.getRecordsByTimeRange(startOfDay, endOfDay)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 今日总饮水量（ML）
    val todayTotalIntakeMl = todayRecords
        .map { records -> records.sumOf { it.intakeMl } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 今日饮水次数
    val todayDrinkCount = todayRecords
        .map { records -> records.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 供 RecordAdapter 使用的 UI 数据
    val todayRecordItems = todayRecords
        .map { records -> records.map { HydrateRecordItem(id = it.id, intakeMl = it.intakeMl, date = it.recordTime) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * WeeklyDateSelector 日期选择回调入口
     */
    fun onDateSelected(date: Date) {
        _selectedDate.value = date
    }

    private fun getDayRange(date: Date): Pair<Date, Date> {
        val startCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return startCal.time to endCal.time
    }

    // 预留：按ID删除（当前 UI 未暴露ID，可后续扩展）
    fun deleteRecordById(id: Long) {
        viewModelScope.launch {
            hydrateRepository.deleteHydrateRecordById(id)
        }
    }

    fun addIntake(intakeMl: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val recordTime = composeRecordTimeForSelectedDate()
                hydrateRepository.addHydrateRecord(intakeMl, recordTime = recordTime)
                delay(500)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun composeRecordTimeForSelectedDate(): Date {
        val selected = _selectedDate.value
        val now = DateTimeUtils.now()
        val calSelected = Calendar.getInstance().apply { time = selected }
        val calNow = Calendar.getInstance().apply { time = now }
        calSelected.set(Calendar.HOUR_OF_DAY, calNow.get(Calendar.HOUR_OF_DAY))
        calSelected.set(Calendar.MINUTE, calNow.get(Calendar.MINUTE))
        calSelected.set(Calendar.SECOND, calNow.get(Calendar.SECOND))
        calSelected.set(Calendar.MILLISECOND, calNow.get(Calendar.MILLISECOND))
        return calSelected.time
    }
}