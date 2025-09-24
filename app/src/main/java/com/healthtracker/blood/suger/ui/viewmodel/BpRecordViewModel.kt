package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.TagType
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.HealthTagRepository
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BpRecordViewModel @Inject constructor(
    private val bloodPressureRepository: BloodPressureRepository,
    private val healthTagRepository: HealthTagRepository
): BaseViewModel() {

    // 编辑模式的记录ID
    private var editingRecordId: Long? = null

    private val _systolicPressure = MutableStateFlow(100)
    val systolicPressure: StateFlow<Int> = _systolicPressure.asStateFlow()


    private val _diastolicPressure = MutableStateFlow(75)
    val diastolicPressure: StateFlow<Int> = _diastolicPressure.asStateFlow()


    private val _pulseRate = MutableStateFlow(70)
    val pulseRate: StateFlow<Int> = _pulseRate.asStateFlow()

    // 记录时间
    private val _recordTime = MutableStateFlow(Date())
    val recordTime: StateFlow<Date> = _recordTime.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 保存状态
    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    // 标签相关状态
    private val _selectedTagIds = MutableStateFlow<List<Long>>(emptyList())
    val selectedTagIds: StateFlow<List<Long>> = _selectedTagIds.asStateFlow()

    private val _availableTags = MutableStateFlow<List<HealthTag>>(emptyList())
    val availableTags: StateFlow<List<HealthTag>> = _availableTags.asStateFlow()

    private val _isTagsLoading = MutableStateFlow(false)
    val isTagsLoading: StateFlow<Boolean> = _isTagsLoading.asStateFlow()

    /**
     * 更新收缩压值
     * @param value 收缩压值 (90-300 mmHg)
     */
    fun updateSystolicPressure(value: Int) {
        _systolicPressure.value = value
    }

    /**
     * 更新舒张压值
     * @param value 舒张压值 (40-150 mmHg)
     */
    fun updateDiastolicPressure(value: Int) {
        _diastolicPressure.value = value
    }

    /**
     * 更新脉搏值
     * @param value 脉搏值 (40-220 次/分钟)
     */
    fun updatePulseRate(value: Int) {
        _pulseRate.value = value
    }

    /**
     * 更新记录时间
     * @param time 记录时间
     */
    fun updateRecordTime(time: Date) {
        _recordTime.value = time
    }

    /**
     * 保存血压记录
     */
    suspend fun saveBloodPressureRecord(): SaveRecordResult {
        return try {
            _isLoading.value = true

            if (editingRecordId != null) {
                // 更新现有记录
                val existingRecord = bloodPressureRepository.getBloodPressureRecordById(editingRecordId!!)
                existingRecord?.let { record ->
                    val updatedRecord = record.copy(
                        systolicPressure = _systolicPressure.value,
                        diastolicPressure = _diastolicPressure.value,
                        pulseRate = _pulseRate.value,
                        recordTime = _recordTime.value
                    )
                    bloodPressureRepository.updateBloodPressureRecord(updatedRecord)

                    // 更新标签关联
                    bloodPressureRepository.addTagsToBloodPressureRecord(editingRecordId!!, _selectedTagIds.value)

                    _isSaved.value = true
                    SaveRecordResult.Updated(editingRecordId!!)
                } ?: SaveRecordResult.Failed("Record not found")
            } else {
                // 添加新记录
                val newRecordId = bloodPressureRepository.addBloodPressureRecord(
                    systolic = _systolicPressure.value,
                    diastolic = _diastolicPressure.value,
                    pulse = _pulseRate.value,
                    selectedTime = _recordTime.value,
                    tagIds = _selectedTagIds.value
                )

                _isSaved.value = true
                SaveRecordResult.Created(newRecordId)
            }
        } catch (e: CancellationException) {
            // 协程正常取消，不记录为错误
            "Blood pressure record save cancelled".logd(TAG)
            throw e // 重新抛出以保持协程取消语义
        } catch (e: Exception) {
            // 真正的异常情况：数据库操作失败等
            "Failed to save blood pressure record: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            SaveRecordResult.Failed(e.message ?: "Save failed")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * 加载编辑记录
     * @param recordId 记录ID
     */
    fun loadEditRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val record = bloodPressureRepository.getBloodPressureRecordById(recordId)
                record?.let {
                    editingRecordId = recordId
                    _systolicPressure.value = it.systolicPressure
                    _diastolicPressure.value = it.diastolicPressure
                    _pulseRate.value = it.pulseRate
                    _recordTime.value = it.recordTime
                    
                    // 注意：getRecordTags方法已删除，标签功能现在由HealthTagRepository统一处理
                    // TODO: 需要根据新的HealthTag系统重新实现标签加载逻辑
                    _selectedTagIds.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 重置保存状态
     */
    fun resetSavedState() {
        _isSaved.value = false
    }

    /**
     * 获取所有血压记录的Flow
     */
    fun getAllBloodPressureRecords() = bloodPressureRepository.getAllBloodPressureRecords()

    /**
     * 获取最近的血压记录
     */
    fun getRecentBloodPressureRecords(limit: Int = 10) = 
        bloodPressureRepository.getRecentBloodPressureRecordsWithLimit(limit)

    // ==================== 标签管理方法 ====================

    /**
     * 初始化标签数据
     */
    fun initializeTags() {
        viewModelScope.launch {
            try {
                _isTagsLoading.value = true
                // 初始化预定义标签
                healthTagRepository.initializePredefinedTags()
                // 加载所有可用标签
                loadAvailableTags()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTagsLoading.value = false
            }
        }
    }

    /**
     * 加载所有可用标签
     */
    private fun loadAvailableTags() {
        viewModelScope.launch {
            healthTagRepository.getTagsByType(TagType.BLOOD_PRESSURE).collect { tags ->
                _availableTags.value = tags
            }
        }
    }

    /**
     * 选择或取消选择标签
     * @param tagId 标签ID
     */
    fun toggleTagSelection(tagId: Long) {
        val currentSelection = _selectedTagIds.value.toMutableList()
        if (currentSelection.contains(tagId)) {
            currentSelection.remove(tagId)
        } else {
            currentSelection.add(tagId)
        }
        _selectedTagIds.value = currentSelection
    }

    /**
     * 创建自定义标签
     * @param tagName 标签名称
     * @return 创建的标签ID，失败返回-1
     */
    suspend fun createCustomTag(tagName: String): Long {
        return try {
            val tag = healthTagRepository.createCustomTag(tagName, TagType.BLOOD_PRESSURE)
            val tagId = tag.id
            if (tagId > 0) {
                // 重新加载标签列表
                loadAvailableTags()
                // 自动选择新创建的标签
                toggleTagSelection(tagId)
            }
            tagId
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * 检查标签名称是否已存在
     * @param tagName 标签名称
     * @return 是否存在
     */
    suspend fun isTagNameExists(tagName: String): Boolean {
        return try {
            healthTagRepository.isTagNameExists(TagType.BLOOD_PRESSURE, tagName)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取选中标签的显示文本
     * @return 标签显示文本
     */
    fun getSelectedTagsDisplayText(): String {
        val selectedTags = _availableTags.value.filter { tag ->
            _selectedTagIds.value.contains(tag.id)
        }
        return selectedTags.joinToString(", ") { healthTagRepository.getTagDisplayText(it) }
    }

    /**
     * 清空选中的标签
     */
    fun clearSelectedTags() {
        _selectedTagIds.value = emptyList()
    }

    // 保存结果密封类
    sealed class SaveRecordResult {
        data class Created(val recordId: Long) : SaveRecordResult()
        data class Updated(val recordId: Long) : SaveRecordResult()
        data class Failed(val error: String) : SaveRecordResult()

        fun isSuccess(): Boolean = this is Created || this is Updated
        fun getRecordId(): Long? = when (this) {
            is Created -> recordId
            is Updated -> recordId
            is Failed -> null
        }
    }
}