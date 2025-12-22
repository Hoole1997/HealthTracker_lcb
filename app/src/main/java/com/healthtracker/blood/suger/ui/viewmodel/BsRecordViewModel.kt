package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.enums.TagType
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.HealthTagRepository
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.util.BloodSugarScaleHelper
import com.healthtracker.blood.suger.constants.KEY_LAST_RECORD_TYPE
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.util.SpUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class BsRecordViewModel(
    private val bloodSugarRepository: BloodSugarRepository,
    private val healthTagRepository: HealthTagRepository
) : BaseViewModel() {

    // 编辑模式的记录ID
    private var editingRecordId: Long? = null

    // 使用StateFlow管理状态
    private val _currentUnit = MutableStateFlow(BsUnit.MMOL_L)
    val currentUnit: StateFlow<BsUnit> = _currentUnit.asStateFlow()

    private val _currentValue = MutableStateFlow(
        BloodSugarScaleHelper.getDefaultValueForUnit(BsUnit.getPreferredUnit())
    )
    val currentValue: StateFlow<Float> = _currentValue.asStateFlow()

    private val _currentStatus = MutableStateFlow(BloodSugarStatus.DEFAULT)
    val currentStatus: StateFlow<BloodSugarStatus> = _currentStatus.asStateFlow()

    private val _recordTime = MutableStateFlow(DateTimeUtils.now())
    val recordTime: StateFlow<Date> = _recordTime.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _healthTags = MutableStateFlow<List<Long>>(emptyList())

    val healthTags = _healthTags.asStateFlow()




    // 初始化方法，支持编辑模式
    fun initializeWithRecord(recordId: Long?) {
        editingRecordId = recordId

        if (recordId != null) {
            // 编辑模式：加载现有记录
            loadExistingRecord(recordId)
        } else {
            // 新增模式：使用默认值
            initializeWithDefaults()
        }
        // 初始化预定义标签
        viewModelScope.launch {
            healthTagRepository.initializePredefinedTags()
        }
    }

    private fun loadExistingRecord(recordId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val record = bloodSugarRepository.getBloodSugarRecordById(recordId)
                record?.let {
                    // 根据记录的选择单位显示数据
                    val selectedUnit = it.getSelectedUnitEnum()
                    val displayValue = it.getDisplayGlucoseValue().toFloat()

                    _currentUnit.value = selectedUnit
                    _currentValue.value = displayValue
                    _currentStatus.value = convertMeasurementTagToBloodSugarStatus(it.satus)
                    _recordTime.value = it.recordTime
                    _healthTags.value = it.tagIds?.let { tags ->
                        val split = tags.split(",")
                        split.map { id -> id.toLong() }
                    } ?: kotlin.run {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                // 加载失败，使用默认值
                initializeWithDefaults()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun initializeWithDefaults() {
        // 使用用户偏好的血糖单位，如果没有设置偏好则使用默认值
        _currentUnit.value = BsUnit.getPreferredUnit()
        val defaultValue = BloodSugarScaleHelper.getDefaultValueForUnit(_currentUnit.value)
        _currentValue.value = defaultValue
        _currentStatus.value = BloodSugarStatus.DEFAULT
        _recordTime.value = DateTimeUtils.now()
    }

    // 状态更新方法
    fun updateValue(value: Float) {
        _currentValue.value = value
    }

    fun updateTags(tags:List<HealthTag>){
        _healthTags.value = tags.map { it.id }
    }

    fun switchUnit(newUnit: BsUnit) {
        val convertedValue = BsUnit.convertValue(
            _currentValue.value,
            _currentUnit.value,
            newUnit
        )
        _currentUnit.value = newUnit
        _currentValue.value = convertedValue

        // 保存用户偏好的单位选择
        BsUnit.savePreferredUnit(newUnit)
    }

    fun updateStatus(status: BloodSugarStatus) {
        _currentStatus.value = status
    }

    fun updateRecordTime(time: Date) {
        _recordTime.value = time
    }

    // 保存记录，返回记录ID（新建时）或操作结果
    suspend fun saveRecord(): SaveRecordResult {
        return try {
            _isLoading.value = true
            if (editingRecordId != null) {
                // 更新现有记录
                updateExistingRecord()
                SpUtils.putInt(KEY_LAST_RECORD_TYPE, HistoryRecordItem.RecordType.BLOOD_SUGAR.ordinal)
                SaveRecordResult.Updated(editingRecordId!!)
            } else {
                // 创建新记录
                val newRecordId = createNewRecord()
                SpUtils.putInt(KEY_LAST_RECORD_TYPE, HistoryRecordItem.RecordType.BLOOD_SUGAR.ordinal)
                SaveRecordResult.Created(newRecordId)
            }
        } catch (e: Exception) {
            SaveRecordResult.Failed(e.message ?: "Save failed")
        } finally {
            _isLoading.value = false
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

    private suspend fun updateExistingRecord() {
        val recordId = editingRecordId ?: return
        val existingRecord = bloodSugarRepository.getBloodSugarRecordById(recordId) ?: return

        // 将当前单位的值转换为mg/dL存储
        val valueInMgdl = _currentUnit.value.convertToMgdl(_currentValue.value.toDouble())

        val updatedRecord = existingRecord.copy(
            glucoseValue = valueInMgdl,
            satus = _currentStatus.value.statusType,
            recordTime = _recordTime.value,
            selectedUnit = _currentUnit.value.value,
            tagIds = _healthTags.value.joinToString(",")
        )
        bloodSugarRepository.updateBloodSugarRecord(updatedRecord)
    }

    private suspend fun createNewRecord(): Long {
        // 将当前单位的值转换为mg/dL存储
        val valueInMgdl = _currentUnit.value.convertToMgdl(_currentValue.value.toDouble())

        return bloodSugarRepository.addBloodSugarRecord(
            glucoseValue = valueInMgdl,
            status = _currentStatus.value.statusType,
            selectedTime = _recordTime.value,
            selectedUnit = _currentUnit.value,
            tagIds = _healthTags.value
        )
    }

    // 判断是否为编辑模式
    fun isEditMode(): Boolean = editingRecordId != null

    // 辅助转换方法

    private fun convertMeasurementTagToBloodSugarStatus(status: Int) = BloodSugarStatus.entries.first { it.statusType == status }


    fun getAvailableHealthTags(): Flow<List<HealthTag>> {
        return healthTagRepository.getTagsByType(TagType.BLOOD_SUGAR)
    }

    suspend fun createCustomTag(tagName: String): Long {
        val name = tagName
        // 重名检查（保留）
        if (healthTagRepository.isTagNameExists(com.healthtracker.blood.suger.data.enums.TagType.BLOOD_SUGAR, name)) {
            return -1L
        }
        return try {
            val id = healthTagRepository.createBloodSugarCustomTag(name)
            if (id > 0L) {
                val current = _healthTags.value.toMutableList()
                if (!current.contains(id)) {
                    current.add(id)
                    _healthTags.value = current
                }
            }
            id
        } catch (e: Exception) {
            -1L
        }
    }

    fun deleteTag(tag: HealthTag) {
        viewModelScope.launch {
            healthTagRepository.deleteTag(tag)
            // 删除后，如果当前选中包含该标签，移除以保持一致性
            val current = _healthTags.value.toMutableList()
            if (current.remove(tag.id)) {
                _healthTags.value = current
            }
        }
    }



}