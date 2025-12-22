package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.util.CholesterolCalculator
import com.healthtracker.blood.suger.util.CholesterolMetrics
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 胆固醇记录新增/编辑页对应的 ViewModel
 * 负责管理输入值、派生指标与保存逻辑
 */
class CholesterolRecordViewModel(
    private val cholesterolRepository: CholesterolRepository
) : BaseViewModel() {

    companion object {
        private const val DEFAULT_HDL = 60
        private const val DEFAULT_LDL = 98
        private const val DEFAULT_TRIGLYCERIDE = 100
    }

    // 当前初始化状态
    private var hasInitialized = false
    // 当前是否为编辑模式
    private var editingRecordId: Long? = null

    // 输入指标
    private val _hdl = MutableStateFlow(DEFAULT_HDL)
    val hdl: StateFlow<Int> = _hdl.asStateFlow()

    private val _ldl = MutableStateFlow(DEFAULT_LDL)
    val ldl: StateFlow<Int> = _ldl.asStateFlow()

    private val _triglyceride = MutableStateFlow(DEFAULT_TRIGLYCERIDE)
    val triglyceride: StateFlow<Int> = _triglyceride.asStateFlow()

    // 记录时间
    private val _recordTime = MutableStateFlow(DateTimeUtils.now())
    val recordTime: StateFlow<Date> = _recordTime.asStateFlow()


    // 加载/保存状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // 计算得到的胆固醇指标（StateFlow 方便 UI 订阅）
    val metrics: StateFlow<CholesterolMetrics> = combine(hdl, ldl, triglyceride) { hdlValue, ldlValue, tgValue ->
        CholesterolCalculator.buildMetrics(hdlValue.toFloat(), ldlValue.toFloat(), tgValue.toFloat())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CholesterolCalculator.buildMetrics(DEFAULT_HDL.toFloat(), DEFAULT_LDL.toFloat(), DEFAULT_TRIGLYCERIDE.toFloat())
    )


    /**
     * 初始化页面数据（可选编辑模式）
     */
    fun initialize(recordId: Long? = null) {
        if (hasInitialized && editingRecordId == recordId) {
            return
        }
        hasInitialized = true
        editingRecordId = recordId
        if (recordId != null) {
            loadRecord(recordId)
        }
    }

    /**
     * 设置 HDL 输入值
     */
    fun updateHdl(value: Int) {
        _hdl.value = value
    }

    /**
     * 设置 LDL 输入值
     */
    fun updateLdl(value: Int) {
        _ldl.value = value
    }

    /**
     * 设置甘油三酯输入值
     */
    fun updateTriglyceride(value: Int) {
        _triglyceride.value = value
    }

    /**
     * 更新记录时间
     */
    fun updateRecordTime(time: Date) {
        _recordTime.value = time
    }

    /**
     * 保存记录（新增或更新）
     */
    suspend fun saveRecord(): SaveResult {
        return try {
            _isSaving.value = true
            val currentMetrics = metrics.value
            if (editingRecordId == null) {
                val newId = cholesterolRepository.addCholesterolRecord(
                    hdl = _hdl.value,
                    ldl = _ldl.value,
                    triglyceride = _triglyceride.value,
                    tc = currentMetrics.totalCholesterol,
                    nonHdl = currentMetrics.nonHdl,
                    tcHdlRatio = currentMetrics.tcHdlRatio,
                    ldlHdlRatio = currentMetrics.ldlHdlRatio,
                    recordTime = _recordTime.value
                )
                SaveResult.Created(newId)
            } else {
                val recordId = editingRecordId!!
                val original = cholesterolRepository.getRecordById(recordId)
                    ?: return SaveResult.Failed("记录不存在")
                val updated = original.copy(
                    recordTime = _recordTime.value,
                    hdl = _hdl.value,
                    ldl = _ldl.value,
                    triglyceride = _triglyceride.value,
                    tc = currentMetrics.totalCholesterol,
                    nonHdl = currentMetrics.nonHdl,
                    tcHdlRatio = currentMetrics.tcHdlRatio,
                    ldlHdlRatio = currentMetrics.ldlHdlRatio
                )
                cholesterolRepository.updateCholesterolRecord(updated)
                SaveResult.Updated(recordId)
            }
        } catch (e: Exception) {
            SaveResult.Failed(e.message ?: "保存失败")
        } finally {
            _isSaving.value = false
        }
    }

    /**
     * 当前是否处于编辑模式
     */
    fun isEditMode(): Boolean = editingRecordId != null

    /**
     * 加载已有记录用于编辑
     */
    private fun loadRecord(recordId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val record = cholesterolRepository.getRecordById(recordId)
                if (record != null) {
                    bindRecord(record)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     * 将记录内容绑定到 StateFlow
     */
    private fun bindRecord(record: CholesterolRecord) {
        _hdl.value = record.hdl ?: DEFAULT_HDL
        _ldl.value = record.ldl ?: DEFAULT_LDL
        _triglyceride.value = record.triglyceride ?: DEFAULT_TRIGLYCERIDE
        _recordTime.value = record.recordTime
    }

    /**
     * 保存结果封装
     */
    sealed class SaveResult {
        data class Created(val recordId: Long) : SaveResult()
        data class Updated(val recordId: Long) : SaveResult()
        data class Failed(val error: String) : SaveResult()

        fun isSuccess(): Boolean = this is Created || this is Updated

        fun recordIdOrNull(): Long? = when (this) {
            is Created -> recordId
            is Updated -> recordId
            is Failed -> null
        }
    }
}
