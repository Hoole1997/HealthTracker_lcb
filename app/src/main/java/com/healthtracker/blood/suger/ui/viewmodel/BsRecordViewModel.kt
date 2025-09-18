package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.enums.MeasurementTag
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import com.healthtracker.blood.suger.util.BloodSugarScaleHelper

@HiltViewModel
class BsRecordViewModel @Inject constructor(
    private val bloodSugarRepository: BloodSugarRepository
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

    private val _recordTime = MutableStateFlow(Date())
    val recordTime: StateFlow<Date> = _recordTime.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
        _recordTime.value = Date()
    }

    // 状态更新方法
    fun updateValue(value: Float) {
        _currentValue.value = value
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

    // 保存记录
    suspend fun saveRecord(): Boolean {
        return try {
            _isLoading.value = true
            if (editingRecordId != null) {
                // 更新现有记录
                updateExistingRecord()
            } else {
                // 创建新记录
                createNewRecord()
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            _isLoading.value = false
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
            selectedUnit = _currentUnit.value.value
        )
        bloodSugarRepository.updateBloodSugarRecord(updatedRecord)
    }

    private suspend fun createNewRecord() {
        // 将当前单位的值转换为mg/dL存储
        val valueInMgdl = _currentUnit.value.convertToMgdl(_currentValue.value.toDouble())

        bloodSugarRepository.addBloodSugarRecord(
            glucoseValue = valueInMgdl,
            status = _currentStatus.value.statusType,
            selectedTime = _recordTime.value,
            selectedUnit = _currentUnit.value
        )
    }

    // 判断是否为编辑模式
    fun isEditMode(): Boolean = editingRecordId != null

    // 辅助转换方法

    private fun convertMeasurementTagToBloodSugarStatus(status: Int) = BloodSugarStatus.entries.first { it.statusType == status }




}