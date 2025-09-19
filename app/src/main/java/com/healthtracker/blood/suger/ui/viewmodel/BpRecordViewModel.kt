package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BpRecordViewModel @Inject constructor(
    private val bloodPressureRepository: BloodPressureRepository
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
     * @param tagIds 关联的标签ID列表
     */
    fun saveBloodPressureRecord(
        tagIds: List<Long>? = null
    ) {
        viewModelScope.launch {
            try {
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
                    }
                } else {
                    // 添加新记录
                    bloodPressureRepository.addBloodPressureRecord(
                        systolic = _systolicPressure.value,
                        diastolic = _diastolicPressure.value,
                        pulse = _pulseRate.value,
                        selectedTime = _recordTime.value,
                        tagIds = tagIds
                    )
                }
                
                _isSaved.value = true
            } catch (e: Exception) {
                // 处理错误
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
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
}