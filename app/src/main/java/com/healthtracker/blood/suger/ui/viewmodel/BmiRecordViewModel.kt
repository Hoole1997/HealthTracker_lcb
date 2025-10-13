package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.TagType
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.blood.suger.data.repository.HealthTagRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.data.utils.TagUtils
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BmiRecordViewModel @Inject constructor(
    private val bmiRepository: BmiRepository,
    private val healthTagRepository: HealthTagRepository
): BaseViewModel() {

    // 编辑模式的记录ID
    private var editingRecordId: Long? = null

    // 身高(cm)与体重(kg)
    private val _heightCm = MutableStateFlow(170.0)
    val heightCm: StateFlow<Double> = _heightCm.asStateFlow()

    private val _weightKg = MutableStateFlow(65.0)
    val weightKg: StateFlow<Double> = _weightKg.asStateFlow()

    // 记录时间
    private val _recordTime = MutableStateFlow(DateTimeUtils.now())
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

    // 初始化（支持编辑模式）
    fun initializeWithRecord(recordId: Long?) {
        editingRecordId = recordId
        if (recordId != null) {
            loadEditRecord(recordId)
        } else {
            initializeDefaults()
        }
        initializeTags()
    }

    private fun initializeDefaults() {
        _heightCm.value = 170.0
        _weightKg.value = 65.0
        _recordTime.value = DateTimeUtils.now()
        _selectedTagIds.value = emptyList()
    }

    fun updateHeight(height: Double) { _heightCm.value = height }
    fun updateWeight(weight: Double) { _weightKg.value = weight }
    fun updateRecordTime(time: Date) { _recordTime.value = time }

    // 加载编辑记录
    fun loadEditRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val record = bmiRepository.getBmiRecordById(recordId)
                if (record != null) {
                    _heightCm.value = record.heightCm
                    _weightKg.value = record.weightKg
                    _recordTime.value = record.recordTime
                    _selectedTagIds.value = record.getTagIdList()
                    "Loaded BMI record for edit: $recordId".logd(TAG)
                } else {
                    "BMI record not found for id: $recordId".loge(TAG)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                "Failed to load BMI record: ${e.message}".loge(TAG)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 初始化标签
    fun initializeTags() {
        viewModelScope.launch {
            try {
                _isTagsLoading.value = true
                // 初始化预定义标签（包含BMI类型）
                healthTagRepository.initializePredefinedTags()
                loadAvailableTags()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isTagsLoading.value = false
            }
        }
    }

    private suspend fun loadAvailableTags() {
        healthTagRepository.getTagsByType(TagType.BMI).collect { tags ->
            _availableTags.value = tags
        }
    }

    fun addTag(tagId: Long) {
        val current = _selectedTagIds.value
        if (!current.contains(tagId)) {
            _selectedTagIds.value = current + tagId
        }
    }

    fun removeTag(tagId: Long) {
        _selectedTagIds.value = _selectedTagIds.value.filter { it != tagId }
    }

    fun clearSelectedTags() { _selectedTagIds.value = emptyList() }

    fun getSelectedTagsDisplayText(): String {
        val selectedTags = _availableTags.value.filter { tag ->
            _selectedTagIds.value.contains(tag.id)
        }
        return selectedTags.joinToString(", ") { healthTagRepository.getTagDisplayText(it) }
    }

    // 保存记录（新增/编辑）
    fun saveBmiRecord(onResult: (SaveRecordResult) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val height = _heightCm.value
                val weight = _weightKg.value
                val time = _recordTime.value
                val tags = _selectedTagIds.value

                // 基本校验
                if (height <= 0 || weight <= 0) {
                    onResult(SaveRecordResult.Failed("身高或体重值不合法"))
                    return@launch
                }

                val recordId = editingRecordId
                if (recordId != null) {
                    // 编辑模式：更新记录
                    val existing = bmiRepository.getBmiRecordById(recordId)
                    if (existing == null) {
                        onResult(SaveRecordResult.Failed("记录不存在，无法更新"))
                        return@launch
                    }
                    val updatedTags = TagUtils.mergeTagIds(existing.getTagIdList(), tags)
                    val tagStr = TagUtils.tagIdsToString(updatedTags)
                    val updated = existing.copy(
                        heightCm = height,
                        weightKg = weight,
                        recordTime = time,
                        tagIds = tagStr
                    ).withUpdatedTimestamp()
                    val rows = bmiRepository.updateBmiRecord(updated)
                    if (rows > 0) {
                        _isSaved.value = true
                        onResult(SaveRecordResult.Updated(recordId))
                    } else {
                        onResult(SaveRecordResult.Failed("更新失败"))
                    }
                } else {
                    // 新增模式：创建记录
                    val tagStr = TagUtils.tagIdsToString(tags)
                    val newRecord = BmiRecord(
                        recordTime = time,
                        heightCm = height,
                        weightKg = weight,
                        tagIds = tagStr
                    )
                    val newId = bmiRepository.insertBmiRecord(newRecord)
                    if (newId > 0) {
                        _isSaved.value = true
                        editingRecordId = newId
                        onResult(SaveRecordResult.Created(newId))
                    } else {
                        onResult(SaveRecordResult.Failed("创建失败"))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(SaveRecordResult.Failed("保存失败: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAvailableHealthTagsFlow(): Flow<List<HealthTag>> {
        return healthTagRepository.getTagsByType(TagType.BMI)
    }

    fun deleteTag(tag: HealthTag) {
        viewModelScope.launch {
            try {
                healthTagRepository.deleteTag(tag)
                // 同步移除选中的标签ID
                val current = _selectedTagIds.value.toMutableList()
                if (current.remove(tag.id)) {
                    _selectedTagIds.value = current
                }
                // 重新加载标签（可选，Flow会自动刷新）
                // loadAvailableTags()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun createCustomTag(tagName: String): Long {
        val name = tagName
        return try {
            if (healthTagRepository.isTagNameExists(TagType.BMI, name)) {
                -1L
            } else {
                val tag = healthTagRepository.createCustomTag(name, TagType.BMI)
                val tagId = tag.id
                if (tagId > 0) {
                    // 自动选择新创建的标签并刷新可用列表
                    val current = _selectedTagIds.value.toMutableList()
                    if (!current.contains(tagId)) {
                        current.add(tagId)
                        _selectedTagIds.value = current
                    }
                    // 重新加载标签列表
                    // loadAvailableTags()
                }
                tagId
            }
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
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

    // 判断是否为编辑模式
    fun isEditMode(): Boolean = editingRecordId != null
}