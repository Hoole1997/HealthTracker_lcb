package com.healthtracker.blood.suger.ui.viewmodel

import android.icu.text.DateFormat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
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

    // 血糖历史记录数据
    private val _bloodSugarRecords = MutableStateFlow<List<BloodSugarRecord>>(emptyList())
    val bloodSugarRecords: StateFlow<List<BloodSugarRecord>> = _bloodSugarRecords.asStateFlow()

    // 血压历史记录数据
    private val _bloodPressureRecords = MutableStateFlow<List<BloodPressureRecord>>(emptyList())
    val bloodPressureRecords: StateFlow<List<BloodPressureRecord>> = _bloodPressureRecords.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误状态
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // 只有在没有保存状态时才初始化默认日期范围
        if (_startDate.value == 0L || _endDate.value == 0L) {
            initDefaultDateRange()
        }

        // 监听筛选条件变化，自动重新加载数据
        setupDataLoading()
    }

    /**
     * 初始化默认日期范围
     * 设置为去年今天到今天的时间范围（本地时区）
     * 
     * 时间范围说明：
     * - 开始时间：去年今天的 00:00:00.000 (本地时区)
     * - 结束时间：今天的 23:59:59.999 (本地时区)
     * 
     * 这样设置可以确保：
     * 1. 包含今天的所有血糖记录
     * 2. UI显示的日期范围与用户本地时区一致
     * 3. 避免时区转换导致的日期显示错误
     */
    private fun initDefaultDateRange() {
        // 使用本地时区的Calendar实例，避免时区转换问题
        val localCalendar = Calendar.getInstance()
        val currentYear = localCalendar.get(Calendar.YEAR)
        val currentMonth = localCalendar.get(Calendar.MONTH)
        val currentDay = localCalendar.get(Calendar.DAY_OF_MONTH)
        
        // 设置结束时间：今天的最后一秒 (23:59:59.999 本地时区)
        localCalendar.set(currentYear, currentMonth, currentDay, 23, 59, 59)
        localCalendar.set(Calendar.MILLISECOND, 999)
        val endDate = localCalendar.timeInMillis

        // 设置开始时间：去年今天的第一秒 (00:00:00.000 本地时区)
        localCalendar.set(currentYear - 1, currentMonth, currentDay, 0, 0, 0)
        localCalendar.set(Calendar.MILLISECOND, 0)
        val startDate = localCalendar.timeInMillis

        // 应用日期范围设置
        setDateRange(startDate, endDate)
    }

    /**
     * 设置日期范围
     */
    fun setDateRange(startDate: Long, endDate: Long) {
        _startDate.value = startDate
        _endDate.value = endDate
        // 保存到savedStateHandle
        savedStateHandle[KEY_START_DATE] = startDate
        savedStateHandle[KEY_END_DATE] = endDate
        viewModelScope.launch {
            combine(_startDate,_endDate){
                updateDateRangeText()
            }.collect()
        }
    }

    /**
     * 设置历史记录类型
     */
    fun setHistoryType(isBloodSugar: Boolean) {
        _isBloodSugarHistory.value = isBloodSugar
        // 保存到savedStateHandle
        savedStateHandle[KEY_IS_BLOOD_SUGAR] = isBloodSugar
        // 数据会通过setupDataLoading()中的combine自动重新加载
    }

    /**
     * 设置血糖状态筛选
     */
    fun setBloodSugarStatusFilter(status: BloodSugarStatus?) {
        _selectedBloodSugarStatus.value = status
        // 保存到savedStateHandle (null表示全部，不保存statusType)
        savedStateHandle[KEY_SELECTED_STATUS] = status?.statusType
        // 数据会通过setupDataLoading()中的combine自动重新加载
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

    /**
     * 设置数据加载监听
     */
    private fun setupDataLoading() {
        // 监听筛选条件变化，自动重新加载数据
        viewModelScope.launch {
            combine(
                _startDate,
                _endDate,
                _isBloodSugarHistory,
                _selectedBloodSugarStatus
            ) { startDate, endDate, isBloodSugar, selectedStatus ->
                // 只有当日期范围有效时才加载数据
                if (startDate > 0L && endDate > 0L) {
                    loadHistoryRecords()
                }
            }.collect()
        }
    }

    /**
     * 加载历史记录数据
     */
    fun loadHistoryRecords() {
        if (_startDate.value <= 0L || _endDate.value <= 0L) {
            return // 日期范围无效，不加载数据
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                if (_isBloodSugarHistory.value) {
                    loadBloodSugarRecords()
                } else {
                    loadBloodPressureRecords()
                }
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "History record loading cancelled".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "Failed to load history records: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
                _errorMessage.value = e.message ?: "Failed to load history records"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载血糖记录
     */
    private suspend fun loadBloodSugarRecords() {
        val startDate = Date(_startDate.value)
        val endDate = Date(_endDate.value)

        bsRepository.getBloodSugarRecordsByTimeRange(startDate, endDate)
            .collect { allRecords ->
                _isLoading.value = false
                // 如果有状态筛选，则进行筛选
                val filteredRecords = _selectedBloodSugarStatus.value?.let { selectedStatus ->
                    allRecords.filter { record ->
                        record.satus == selectedStatus.statusType
                    }
                } ?: allRecords

                _bloodSugarRecords.value = filteredRecords.sortedByDescending { it.recordTime }
            }
    }

    /**
     * 加载血压记录
     */
    private suspend fun loadBloodPressureRecords() {
        val startDate = Date(_startDate.value)
        val endDate = Date(_endDate.value)

        bpRepository.getBloodPressureRecordsByTimeRange(startDate, endDate)
            .collect { allRecords ->
                _isLoading.value = false
                _bloodPressureRecords.value = allRecords.sortedByDescending { it.recordTime }
            }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }


    fun deleteBsRecord(recordId:Long){
        viewModelScope.launch {
            try {
                bsRepository.deleteBloodSugarRecord(recordId)
                "Successfully deleted blood sugar record: ID=$recordId".logd(TAG)
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "Blood sugar record deletion cancelled: ID=$recordId".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "Blood sugar record deletion error: ID=$recordId, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
    }

    fun deleteBpRecord(recordId:Long){
        viewModelScope.launch {
            try {
                bpRepository.deleteBloodPressureRecord(recordId)
                "Successfully deleted blood pressure record: ID=$recordId".logd(TAG)
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "Blood pressure record deletion cancelled: ID=$recordId".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "Blood pressure record deletion error: ID=$recordId, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
    }
}