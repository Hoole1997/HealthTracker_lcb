package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.Calendar
import java.util.Date

/**
 * 历史记录页面ViewModel
 */
class HistoryViewModel(
    private val bpRepository: BloodPressureRepository,
    private val bsRepository: BloodSugarRepository,
    private val cholesterolRepository: CholesterolRepository,
    private val heartRateRepository: HeartRateRepository,
    private val bmiRepository: BmiRepository,
    private var savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    companion object {
        private const val TAG = "HistoryViewModel"
        private const val KEY_RECORD_TYPE = "RECORD_TYPE"
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

    // 记录类型（默认血糖）
    private val _recordType = MutableStateFlow(
        savedStateHandle.get<Int>(KEY_RECORD_TYPE)?.let {
            HistoryRecordItem.RecordType.values()[it]
        } ?: HistoryRecordItem.RecordType.BLOOD_SUGAR
    )
    val recordType: StateFlow<HistoryRecordItem.RecordType> = _recordType.asStateFlow()

    // 血糖状态类型筛选（null表示全部，非null表示具体状态）
    private val _selectedBloodSugarStatus = MutableStateFlow<BloodSugarStatus?>(
        savedStateHandle.get<Int>(KEY_SELECTED_STATUS)?.let {
            BloodSugarStatus.fromStatusType(it)
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

    // 胆固醇历史记录数据
    private val _cholesterolRecords = MutableStateFlow<List<CholesterolRecord>>(emptyList())
    val cholesterolRecords: StateFlow<List<CholesterolRecord>> = _cholesterolRecords.asStateFlow()

    // 心率历史记录数据
    private val _heartRateRecords = MutableStateFlow<List<HeartRateRecord>>(emptyList())
    val heartRateRecords: StateFlow<List<HeartRateRecord>> = _heartRateRecords.asStateFlow()

    // BMI历史记录数据
    private val _bmiRecords = MutableStateFlow<List<BmiRecord>>(emptyList())
    val bmiRecords: StateFlow<List<BmiRecord>> = _bmiRecords.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误状态
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var recordsJob: Job? = null

    init {
        // 只有在没有保存状态时才初始化默认日期范围
        if (_startDate.value == 0L || _endDate.value == 0L) {
            initDefaultDateRange()
        }

        // 监听筛选条件变化，自动重新加载数据
        setupDataLoading()
    }

    /**
     * 初始化默认日期范围（去年今天到今天）
     * 使用DateTimeUtils统一处理日期，确保：
     * 1. 数据库查询的时间戳与本地时区一致
     * 2. UI显示的日期范围与用户本地时区一致
     * 3. 避免时区转换导致的日期显示错误
     */
    private fun initDefaultDateRange() {
        // 获取今天的日期范围
        val (todayStart, todayEnd) = DateTimeUtils.getTodayRange()
        val endDate = todayEnd.time
        
        // 获取去年今天的开始时间，使用addYears确保正确处理闰年
        val oneYearAgo = DateTimeUtils.addYears(todayStart, -1)
        val startDate = oneYearAgo.time

        // 应用日期范围设置
        setDateRange(startDate, endDate)
    }

    /**
     * 设置日期范围
     */
    fun setDateRange(startDate: Long, endDate: Long) {
        if (startDate <= 0L || endDate <= 0L) return

        val (normalizedStart, normalizedEnd) = normalizeDateRange(startDate, endDate)

        _startDate.value = normalizedStart
        _endDate.value = normalizedEnd

        savedStateHandle[KEY_START_DATE] = normalizedStart
        savedStateHandle[KEY_END_DATE] = normalizedEnd

        updateDateRangeText()
    }

    /**
     * 将用户选择的毫秒时间戳规范化到本地时区的起止时刻
     */
    private fun normalizeDateRange(
        rawStart: Long,
        rawEnd: Long
    ): Pair<Long, Long> {
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = rawStart
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCalendar = Calendar.getInstance().apply {
            timeInMillis = rawEnd
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startMillis = startCalendar.timeInMillis
        val endMillis = maxOf(endCalendar.timeInMillis, startMillis)
        return startMillis to endMillis
    }

    /**
     * 设置历史记录类型
     */
    fun setHistoryType(recordType: HistoryRecordItem.RecordType) {
        _recordType.value = recordType
        savedStateHandle[KEY_RECORD_TYPE] = recordType.ordinal
        // 数据会通过setupDataLoading()中的combine自动重新加载
    }

    /**
     * 设置血糖状态筛选
     * @param status null表示全部状态，非null表示具体状态筛选
     */
    fun setBloodSugarStatusFilter(status: BloodSugarStatus?) {
        val previousStatus = _selectedBloodSugarStatus.value
        "Setting blood sugar status filter: $previousStatus -> $status".logd(TAG)

        _selectedBloodSugarStatus.value = status
        // 保存到savedStateHandle
        if (status == null) {
            savedStateHandle.remove<Int>(KEY_SELECTED_STATUS) // null时移除key
            "Removed status filter from savedStateHandle".logd(TAG)
        } else {
            savedStateHandle[KEY_SELECTED_STATUS] = status.statusType
            "Saved status filter to savedStateHandle: ${status.name} (statusType=${status.statusType})".logd(TAG)
        }
        // 数据会通过setupDataLoading()中的combine自动重新加载
    }

    /**
     * 更新日期范围显示文本
     * 使用统一的日期格式
     */
    fun updateDateRangeText() {
        val startDateStr = DateTimeUtils.formatDate(Date(_startDate.value))
        val endDateStr = DateTimeUtils.formatDate(Date(_endDate.value))
        _dateRangeText.value = "$startDateStr - $endDateStr"
    }

    /**
     * 设置数据加载监听
     * 使用combine监听所有筛选条件，避免重复数据库查询
     */
    private fun setupDataLoading() {
        // 监听筛选条件变化，自动重新加载数据
        viewModelScope.launch {
            combine(
                _startDate,
                _endDate,
                _recordType,
                _selectedBloodSugarStatus
            ) { startDate, endDate, recordType, selectedStatus ->
                FilterParams(startDate, endDate, recordType, selectedStatus)
            }.collect { params ->
                // 只有当日期范围有效时才加载数据
                if (params.startDate > 0L && params.endDate > 0L) {
                    loadHistoryRecordsWithParams(params)
                }
            }
        }
    }

    /**
     * 筛选参数数据类
     */
    private data class FilterParams(
        val startDate: Long,
        val endDate: Long,
        val recordType: HistoryRecordItem.RecordType,
        val selectedStatus: BloodSugarStatus?
    )

    /**
     * 加载历史记录数据
     */
    fun loadHistoryRecords() {
        val params = FilterParams(
            _startDate.value,
            _endDate.value,
            _recordType.value,
            _selectedBloodSugarStatus.value
        )
        loadHistoryRecordsWithParams(params)
    }

    /**
     * 根据筛选参数加载历史记录数据
     */
    private fun loadHistoryRecordsWithParams(params: FilterParams) {
        if (params.startDate <= 0L || params.endDate <= 0L) {
            return // 日期范围无效，不加载数据
        }

        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                when (params.recordType) {
                    HistoryRecordItem.RecordType.BLOOD_SUGAR -> {
                        loadBloodSugarRecordsWithFilter(params)
                    }
                    HistoryRecordItem.RecordType.BLOOD_PRESSURE -> {
                        loadBloodPressureRecordsWithFilter(params)
                    }
                    HistoryRecordItem.RecordType.CHOLESTEROL -> {
                        loadCholesterolRecordsWithFilter(params)
                    }
                    HistoryRecordItem.RecordType.HEART_RATE -> {
                        loadHeartRateRecordsWithFilter(params)
                    }
                    HistoryRecordItem.RecordType.BMI_RECORD -> {
                        loadBmiRecordsWithFilter(params)
                    }
                }
            } catch (e: CancellationException) {
                "History record loading cancelled".logd(TAG)
                throw e
            } catch (e: Exception) {
                "Failed to load history records: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
                _errorMessage.value = "加载历史记录失败: ${e.message ?: "未知错误"}"
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载血糖记录（优化版，避免重复筛选）
     */
    private suspend fun loadBloodSugarRecordsWithFilter(params: FilterParams) {
        val startDate = Date(params.startDate)
        val endDate = Date(params.endDate)

        var isFirstEmission = true
        bsRepository.getBloodSugarRecordsByTimeRange(startDate, endDate)
            .collect { allRecords ->
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }

                // 统一在内存中筛选，避免重复数据库查询
                val filteredRecords = if (params.selectedStatus == null) {
                    // null 表示显示所有状态的记录
                    "Filtering: selectedStatus=null, showing all ${allRecords.size} records".logd(TAG)
                    allRecords
                } else {
                    // 非null 表示只显示指定状态的记录（包括 DEFAULT 状态）
                    val targetStatusType = params.selectedStatus.statusType
                    "Filtering: selectedStatus=${params.selectedStatus.name} (statusType=$targetStatusType)".logd(TAG)

                    // 记录所有记录的状态类型用于调试
                    val statusCounts = allRecords.groupBy { it.satus }.mapValues { it.value.size }
                    "Available record status types: $statusCounts".logd(TAG)
                    "Looking for records with statusType=$targetStatusType (${params.selectedStatus.name})".logd(TAG)

                    // 记录前5个记录的详细信息用于调试
                    allRecords.take(5).forEachIndexed { index, record ->
                        "Record $index: id=${record.id}, satus=${record.satus}, glucoseValue=${record.glucoseValue}, recordTime=${record.recordTime}".logd(TAG)
                    }

                    val filtered = allRecords.filter { record ->
                        record.satus == targetStatusType
                    }
                    "Filtered result: ${filtered.size} records match statusType=$targetStatusType".logd(TAG)
                    filtered
                }

                _bloodSugarRecords.value = filteredRecords.sortedByDescending { it.recordTime }
            }
    }

    /**
     * 向后兼容的血糖记录加载方法
     */
    private suspend fun loadBloodSugarRecords() {
        val params = FilterParams(
            _startDate.value,
            _endDate.value,
            _recordType.value,
            _selectedBloodSugarStatus.value
        )
        loadBloodSugarRecordsWithFilter(params)
    }

    /**
     * 加载血压记录（优化版）
     */
    private suspend fun loadBloodPressureRecordsWithFilter(params: FilterParams) {
        val startDate = Date(params.startDate)
        val endDate = Date(params.endDate)

        var isFirstEmission = true
        bpRepository.getBloodPressureRecordsByTimeRange(startDate, endDate)
            .collect { allRecords ->
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }

                _bloodPressureRecords.value = allRecords.sortedByDescending { it.recordTime }
            }
    }

    /**
     * 向后兼容的血压记录加载方法
     */
    private suspend fun loadBloodPressureRecords() {
        val params = FilterParams(
            _startDate.value,
            _endDate.value,
            _recordType.value,
            _selectedBloodSugarStatus.value
        )
        loadBloodPressureRecordsWithFilter(params)
    }

    /**
     * 加载胆固醇记录
     */
    private suspend fun loadCholesterolRecordsWithFilter(params: FilterParams) {
        val startDate = Date(params.startDate)
        val endDate = Date(params.endDate)

        var isFirstEmission = true
        cholesterolRepository.getCholesterolRecordsByTimeRange(startDate, endDate)
            .collect { allRecords ->
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }

                _cholesterolRecords.value = allRecords.sortedByDescending { it.recordTime }
            }
    }

    /**
     * 加载心率记录
     */
    private suspend fun loadHeartRateRecordsWithFilter(params: FilterParams) {
        val startDate = Date(params.startDate)
        val endDate = Date(params.endDate)

        var isFirstEmission = true
        heartRateRepository.getHeartRateRecordsByTimeRange(startDate, endDate)
            .collect { allRecords ->
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }

                _heartRateRecords.value = allRecords.sortedByDescending { it.recordTime }
            }
    }

    /**
     * 加载BMI记录
     */
    private suspend fun loadBmiRecordsWithFilter(params: FilterParams) {
        val startDate = Date(params.startDate)
        val endDate = Date(params.endDate)

        var isFirstEmission = true
        bmiRepository.getBmiRecordsByTimeRange(startDate, endDate)
            .collect { allRecords ->
                if (isFirstEmission) {
                    _isLoading.value = false
                    isFirstEmission = false
                }

                _bmiRecords.value = allRecords.sortedByDescending { it.recordTime }
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
                _errorMessage.value = "删除记录失败: ${e.message}"
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
                _errorMessage.value = "删除记录失败: ${e.message}"
            }
        }
    }

    fun deleteCholesterolRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                cholesterolRepository.deleteCholesterolRecord(recordId)
                "Successfully deleted cholesterol record: ID=$recordId".logd(TAG)
            } catch (e: CancellationException) {
                "Cholesterol record deletion cancelled: ID=$recordId".logd(TAG)
                throw e
            } catch (e: Exception) {
                "Cholesterol record deletion error: ID=$recordId, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
                _errorMessage.value = "删除记录失败: ${e.message}"
            }
        }
    }

    fun deleteHeartRateRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                heartRateRepository.deleteHeartRateRecord(recordId)
                "Successfully deleted heart rate record: ID=$recordId".logd(TAG)
            } catch (e: CancellationException) {
                "Heart rate record deletion cancelled: ID=$recordId".logd(TAG)
                throw e
            } catch (e: Exception) {
                "Heart rate record deletion error: ID=$recordId, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
                _errorMessage.value = "删除记录失败: ${e.message}"
            }
        }
    }

    fun deleteBmiRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                bmiRepository.deleteBmiRecord(recordId)
                "Successfully deleted BMI record: ID=$recordId".logd(TAG)
            } catch (e: CancellationException) {
                "BMI record deletion cancelled: ID=$recordId".logd(TAG)
                throw e
            } catch (e: Exception) {
                "BMI record deletion error: ID=$recordId, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
                _errorMessage.value = "删除记录失败: ${e.message}"
            }
        }
    }
}
