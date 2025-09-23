package com.healthtracker.blood.suger.ui.viewmodel

import android.icu.text.DateFormat
import android.nfc.Tag
import androidx.lifecycle.SavedStateHandle
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
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

    companion object {
        private const val TAG = "HistoryViewModel"
        private const val KEY_IS_BLOOD_SUGAR = "is_blood_sugar"
        private const val KEY_SELECTED_STATUS = "selected_status"
        private const val KEY_START_DATE = "start_date"
        private const val KEY_END_DATE = "end_date"
    }

    // 日期范围状态
    private val _startDate = MutableStateFlow(
        savedStateHandle.get<Long>(KEY_START_DATE) ?: 0L
    )
    val startDate: StateFlow<Long> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(
        savedStateHandle.get<Long>(KEY_END_DATE) ?: 0L
    )
    val endDate: StateFlow<Long> = _endDate.asStateFlow()

    // 是否为血糖历史记录（true: 血糖, false: 血压）
    private val _isBloodSugarHistory = MutableStateFlow(
        savedStateHandle.get<Boolean>(KEY_IS_BLOOD_SUGAR) ?: true
    )
    val isBloodSugarHistory: StateFlow<Boolean> = _isBloodSugarHistory.asStateFlow()

    // 血糖状态类型筛选（null表示全部）
    private val _selectedBloodSugarStatus = MutableStateFlow<BloodSugarStatus?>(
        savedStateHandle.get<Int>(KEY_SELECTED_STATUS)?.let {
            BloodSugarStatus.fromStatusType(it).takeIf { status -> status != BloodSugarStatus.DEFAULT }
        }
    )
    val selectedBloodSugarStatus: StateFlow<BloodSugarStatus?> = _selectedBloodSugarStatus.asStateFlow()

    // 格式化的日期范围显示字符串
    private val _dateRangeText = MutableStateFlow("")
    val dateRangeText: StateFlow<String> = _dateRangeText.asStateFlow()

    init {
        // 只有在没有保存状态时才初始化默认日期范围
        if (_startDate.value == 0L || _endDate.value == 0L) {
            initDefaultDateRange()
        }
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
        // 保存到savedStateHandle
        savedStateHandle[KEY_START_DATE] = startDate
        savedStateHandle[KEY_END_DATE] = endDate


    }

    /**
     * 设置历史记录类型
     */
    fun setHistoryType(isBloodSugar: Boolean) {
        _isBloodSugarHistory.value = isBloodSugar
        // 保存到savedStateHandle
        savedStateHandle[KEY_IS_BLOOD_SUGAR] = isBloodSugar
    }

    /**
     * 设置血糖状态筛选
     */
    fun setBloodSugarStatusFilter(status: BloodSugarStatus?) {
        _selectedBloodSugarStatus.value = status
        // 保存到savedStateHandle (null表示全部，不保存statusType)
        savedStateHandle[KEY_SELECTED_STATUS] = status?.statusType
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


    fun loadData() {
        if (_isBloodSugarHistory.value) {
            loadBsRecords()
        } else {
            loadBpRecords()
        }
    }

    private fun loadBsRecords(){
        "load bs records".logd(TAG)

    }

    private fun loadBpRecords(){
        "load bp records".logd(TAG)

    }
}