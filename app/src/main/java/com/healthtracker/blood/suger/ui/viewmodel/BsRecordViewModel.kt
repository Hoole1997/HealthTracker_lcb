package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.enums.GlucoseUnit
import com.healthtracker.blood.suger.data.enums.MeasurementTag
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.enum.BloodSugarUnit
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BsRecordViewModel @Inject constructor(
    private val bloodSugarRepository: BloodSugarRepository
) : BaseViewModel() {

    // 编辑模式的记录ID
    private var editingRecordId: Long? = null

    // 使用StateFlow管理状态
    private val _currentUnit = MutableStateFlow(BloodSugarUnit.MMOL_L)
    val currentUnit: StateFlow<BloodSugarUnit> = _currentUnit.asStateFlow()

    private val _currentValue = MutableStateFlow(4.2f)
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

                    _currentUnit.value = convertGlucoseUnitToBloodSugarUnit(selectedUnit)
                    _currentValue.value = displayValue
                    _currentStatus.value = convertMeasurementTagToBloodSugarStatus(it.measurementTag)
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
        _currentUnit.value = BloodSugarUnit.getPreferredUnit()
        _currentValue.value = 4.2f
        _currentStatus.value = BloodSugarStatus.DEFAULT
        _recordTime.value = Date()
    }

    // 状态更新方法
    fun updateValue(value: Float) {
        _currentValue.value = value
    }

    fun switchUnit(newUnit: BloodSugarUnit) {
        val convertedValue = BloodSugarUnit.convertValue(
            _currentValue.value,
            _currentUnit.value,
            newUnit
        )
        _currentUnit.value = newUnit
        _currentValue.value = convertedValue

        // 保存用户偏好的单位选择
        BloodSugarUnit.savePreferredUnit(newUnit)
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
        val valueInMgdl = convertToMgdl(_currentValue.value, _currentUnit.value)

        val updatedRecord = existingRecord.copy(
            glucoseValue = valueInMgdl.toDouble(),
            measurementTag = convertBloodSugarStatusToMeasurementTag(_currentStatus.value),
            recordTime = _recordTime.value,
            selectedUnit = convertBloodSugarUnitToGlucoseUnit(_currentUnit.value).value
        )
        bloodSugarRepository.updateBloodSugarRecord(updatedRecord)
    }

    private suspend fun createNewRecord() {
        // 将当前单位的值转换为mg/dL存储
        val valueInMgdl = convertToMgdl(_currentValue.value, _currentUnit.value)

        bloodSugarRepository.addBloodSugarRecord(
            glucoseValue = valueInMgdl.toDouble(),
            measurementTag = convertBloodSugarStatusToMeasurementTag(_currentStatus.value),
            selectedTime = _recordTime.value
        )
    }

    // 判断是否为编辑模式
    fun isEditMode(): Boolean = editingRecordId != null

    // 辅助转换方法
    private fun convertGlucoseUnitToBloodSugarUnit(glucoseUnit: GlucoseUnit): BloodSugarUnit {
        return when (glucoseUnit) {
            GlucoseUnit.MG_DL -> BloodSugarUnit.MG_DL
            GlucoseUnit.MMOL_L -> BloodSugarUnit.MMOL_L
        }
    }

    private fun convertBloodSugarUnitToGlucoseUnit(bloodSugarUnit: BloodSugarUnit): GlucoseUnit {
        return when (bloodSugarUnit) {
            BloodSugarUnit.MG_DL -> GlucoseUnit.MG_DL
            BloodSugarUnit.MMOL_L -> GlucoseUnit.MMOL_L
        }
    }

    private fun convertMeasurementTagToBloodSugarStatus(measurementTag: String): BloodSugarStatus {
        val tag = MeasurementTag.fromString(measurementTag)
        return when (tag) {
            MeasurementTag.FASTING -> BloodSugarStatus.FASTING
            MeasurementTag.BEFORE_BREAKFAST, MeasurementTag.BEFORE_LUNCH, MeasurementTag.BEFORE_DINNER -> BloodSugarStatus.BEFORE_MEAL
            MeasurementTag.AFTER_BREAKFAST, MeasurementTag.AFTER_LUNCH, MeasurementTag.AFTER_DINNER -> BloodSugarStatus.TWO_HOURS_AFTER_MEAL
            MeasurementTag.BEDTIME -> BloodSugarStatus.BEDTIME
            MeasurementTag.AFTER_EXERCISE -> BloodSugarStatus.AFTER_EXERCISE
            else -> BloodSugarStatus.DEFAULT
        }
    }

    private fun convertBloodSugarStatusToMeasurementTag(status: BloodSugarStatus): String {
        val tag = when (status) {
            BloodSugarStatus.FASTING -> MeasurementTag.FASTING
            BloodSugarStatus.BEFORE_MEAL -> MeasurementTag.BEFORE_BREAKFAST
            BloodSugarStatus.ONE_HOUR_AFTER_MEAL -> MeasurementTag.AFTER_BREAKFAST
            BloodSugarStatus.TWO_HOURS_AFTER_MEAL -> MeasurementTag.AFTER_BREAKFAST
            BloodSugarStatus.BEDTIME -> MeasurementTag.BEDTIME
            BloodSugarStatus.BEFORE_EXERCISE -> MeasurementTag.BEFORE_MEDICATION
            BloodSugarStatus.AFTER_EXERCISE -> MeasurementTag.AFTER_EXERCISE
            else -> MeasurementTag.OTHER
        }
        return tag.code
    }

    private fun convertToMgdl(value: Float, unit: BloodSugarUnit): Float {
        return when (unit) {
            BloodSugarUnit.MG_DL -> value
            BloodSugarUnit.MMOL_L -> value * GlucoseUnit.CONVERSION_FACTOR.toFloat()
        }
    }
}