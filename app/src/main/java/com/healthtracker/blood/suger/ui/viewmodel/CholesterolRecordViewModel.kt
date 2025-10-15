package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.enums.CholesterolLevel
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.util.CholesterolCalculator
import com.healthtracker.blood.suger.util.CholesterolMetrics
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.blood.suger.getUserAge
import com.healthtracker.blood.suger.isMale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * 胆固醇记录新增/编辑页对应的 ViewModel
 * 负责管理输入值、派生指标与保存逻辑
 */
@HiltViewModel
class CholesterolRecordViewModel @Inject constructor(
    private val cholesterolRepository: CholesterolRepository
) : BaseViewModel() {

    companion object {
        private const val DEFAULT_HDL = 50f
        private const val DEFAULT_LDL = 100f
        private const val DEFAULT_TRIGLYCERIDE = 150f
    }

    // 当前初始化状态
    private var hasInitialized = false
    // 当前是否为编辑模式
    private var editingRecordId: Long? = null

    // 输入指标
    private val _hdl = MutableStateFlow<Float?>(DEFAULT_HDL)
    val hdl: StateFlow<Float?> = _hdl.asStateFlow()

    private val _ldl = MutableStateFlow<Float?>(DEFAULT_LDL)
    val ldl: StateFlow<Float?> = _ldl.asStateFlow()

    private val _triglyceride = MutableStateFlow<Float?>(DEFAULT_TRIGLYCERIDE)
    val triglyceride: StateFlow<Float?> = _triglyceride.asStateFlow()

    // 记录时间
    private val _recordTime = MutableStateFlow(DateTimeUtils.now())
    val recordTime: StateFlow<Date> = _recordTime.asStateFlow()

    // 用户画像（用于展示性别/年龄）
    private val _userAge = MutableStateFlow(getUserAge())
    val userAge: StateFlow<Int> = _userAge.asStateFlow()

    private val _isMale = MutableStateFlow(isMale())
    val isMaleUser: StateFlow<Boolean> = _isMale.asStateFlow()

    // 加载/保存状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // 计算得到的胆固醇指标（StateFlow 方便 UI 订阅）
    val metrics: StateFlow<CholesterolMetrics> = combine(hdl, ldl, triglyceride) { hdlValue, ldlValue, tgValue ->
        CholesterolCalculator.buildMetrics(hdlValue, ldlValue, tgValue)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CholesterolCalculator.buildMetrics(DEFAULT_HDL, DEFAULT_LDL, DEFAULT_TRIGLYCERIDE)
    )

    val riskLevel: StateFlow<CholesterolLevel> = metrics
        .map { it.riskLevel }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = metrics.value.riskLevel
        )

    /**
     * 刷新用户画像（从偏好中重新读取）
     */
    fun refreshUserProfile() {
        _userAge.value = getUserAge()
        _isMale.value = isMale()
    }

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
        } else {
            loadDefaultValues()
        }
    }

    /**
     * 设置 HDL 输入值
     */
    fun updateHdl(value: Float?) {
        _hdl.value = value
    }

    /**
     * 设置 LDL 输入值
     */
    fun updateLdl(value: Float?) {
        _ldl.value = value
    }

    /**
     * 设置甘油三酯输入值
     */
    fun updateTriglyceride(value: Float?) {
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
                } else {
                    // 找不到记录则回退到默认状态
                    loadDefaultValues()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 使用默认值初始化（尝试读取最新记录作为参考）
     */
    private fun loadDefaultValues() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val latest = cholesterolRepository.getLatestRecord()
                if (latest != null) {
                    bindRecord(latest, fallbackToDefault = true)
                } else {
                    _hdl.value = DEFAULT_HDL
                    _ldl.value = DEFAULT_LDL
                    _triglyceride.value = DEFAULT_TRIGLYCERIDE
                    _recordTime.value = DateTimeUtils.now()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 将记录内容绑定到 StateFlow
     */
    private fun bindRecord(record: CholesterolRecord, fallbackToDefault: Boolean = false) {
        _hdl.value = record.hdl ?: if (fallbackToDefault) DEFAULT_HDL else null
        _ldl.value = record.ldl ?: if (fallbackToDefault) DEFAULT_LDL else null
        _triglyceride.value = record.triglyceride ?: if (fallbackToDefault) DEFAULT_TRIGLYCERIDE else null
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
