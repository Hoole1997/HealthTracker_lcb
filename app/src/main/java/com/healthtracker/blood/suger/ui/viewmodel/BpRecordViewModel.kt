package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodPressureTag
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.BloodPressureTagRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val bloodPressureTagRepository: BloodPressureTagRepository
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

    private val _availableTags = MutableStateFlow<List<BloodPressureTag>>(emptyList())
    val availableTags: StateFlow<List<BloodPressureTag>> = _availableTags.asStateFlow()

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
    suspend fun saveBloodPressureRecord(): Boolean {
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
                }
            } else {
                // 添加新记录
                bloodPressureRepository.addBloodPressureRecord(
                    systolic = _systolicPressure.value,
                    diastolic = _diastolicPressure.value,
                    pulse = _pulseRate.value,
                    selectedTime = _recordTime.value,
                    tagIds = _selectedTagIds.value
                )
            }
            
            _isSaved.value = true
            true
        } catch (e: Exception) {
            // 处理错误
            e.printStackTrace()
            false
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
                    
                    // 加载记录关联的标签
                    val recordTags = bloodPressureRepository.getRecordTags(it)
                    _selectedTagIds.value = recordTags.map { tag -> tag.id }
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
                bloodPressureTagRepository.initializePredefinedTags()
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
            bloodPressureTagRepository.getAllBloodPressureTags().collect { tags ->
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
            val tagId = bloodPressureTagRepository.createCustomTagBusiness(tagName)
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
            bloodPressureTagRepository.isTagNameExists(tagName)
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
        return bloodPressureTagRepository.getTagDisplayText(selectedTags)
    }

    /**
     * 清空选中的标签
     */
    fun clearSelectedTags() {
        _selectedTagIds.value = emptyList()
    }
}