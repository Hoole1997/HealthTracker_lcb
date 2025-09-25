package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * 药物管理页面ViewModel
 * 负责处理日期选择逻辑和相关数据管理
 */
@HiltViewModel
class MedsViewModel @Inject constructor(
    // 这里可以注入需要的Repository，比如药物相关的Repository
    // private val medsRepository: MedsRepository
) : BaseViewModel() {

    // 当前选中的日期
    private val _selectedDate = MutableStateFlow(DateTimeUtils.now())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    // 当前显示的周是否为系统当前周
    private val _isCurrentWeek = MutableStateFlow(true)
    val isCurrentWeek: StateFlow<Boolean> = _isCurrentWeek.asStateFlow()

    // 格式化月份的Flow，基于selectedDate动态计算
    val formattedMonth: StateFlow<String> = selectedDate.map { date ->
        DateTimeUtils.formatMonthYear(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DateTimeUtils.formatMonthYear(_selectedDate.value)
    )

    /**
     * 日期选择处理
     * @param date 选中的日期
     */
    fun onDateSelected(date: Date) {
        viewModelScope.launch {
            "日期选择: ${DateTimeUtils.formatDate(date)}".logd(TAG)
            _selectedDate.value = date
            
            // 加载选中日期的数据
            loadDataForSelectedDate(date)
        }
    }

    /**
     * 处理周切换事件
     * @param isCurrentWeek 当前显示的周是否为系统当前周
     */
    fun onWeekChanged(isCurrentWeek: Boolean) {
        viewModelScope.launch {
            "周视图切换，是否当前周: $isCurrentWeek".logd(TAG)
            
            _isCurrentWeek.value = isCurrentWeek
            
            // 这里可以添加根据周切换加载相关数据的逻辑
            loadDataForCurrentWeek(isCurrentWeek)
        }
    }

    /**
     * 根据选中的日期加载相关数据
     * @param date 选中的日期
     */
    private fun loadDataForSelectedDate(date: Date) {
        // TODO: 实现根据日期加载药物相关数据的逻辑
        // 例如：加载该日期的用药记录、提醒等
        "加载日期 ${DateTimeUtils.formatDate(date)} 的药物数据".logd(TAG)
    }

    /**
     * 根据周切换加载相关数据
     * @param isCurrentWeek 是否为当前周
     */
    private fun loadDataForCurrentWeek(isCurrentWeek: Boolean) {
        // TODO: 实现根据周切换加载相关数据的逻辑
        // 例如：加载本周或其他周的用药统计数据
        "加载${if (isCurrentWeek) "当前周" else "其他周"}的药物数据".logd(TAG)
    }

    /**
     * 获取格式化的月份字符串
     * @return 格式化的月份字符串，如"Sep.2025"
     */
    fun getFormattedMonth(): String {
        return DateTimeUtils.formatMonthYear(_selectedDate.value)
    }

    /**
     * 重置到今天
     */
    fun resetToToday() {
        onDateSelected(DateTimeUtils.now())
    }
}