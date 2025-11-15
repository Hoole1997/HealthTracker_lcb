package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.enums.BmiUnit
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StepSettingViewModel @Inject constructor(
    private val bmiRepository: BmiRepository
) : BaseViewModel() {
    companion object{
        private const val TAG = "StepSettingViewModel"
    }


    /**
     * 加载 BMI 记录
     */
    fun loadRecord() {
        viewModelScope.launch {

            try {
                "Loading BMI ".logd(TAG)
                bmiRepository.getAllBmiRecords().collect { records ->
                    "BMI record loaded: ${records.size}".logd(TAG)
                    _bmiRecord.value = records.firstOrNull()
                }
            } catch (e: Exception) {
                "Failed to load BMI record: ${e.message}".logd(TAG)
            } finally {

            }
        }
    }
    // BMI 记录数据
    private val _bmiRecord = MutableStateFlow<BmiRecord?>(null)
    val bmiRecord: StateFlow<BmiRecord?> = _bmiRecord.asStateFlow()

    /**
     * 获取格式化的显示体重（根据用户偏好单位）
     */
    fun getDisplayWeight(): String {
        val weightKg = _bmiRecord.value?.weightKg ?: return "--"
        val preferredUnit = BmiUnit.getPreferredWeightUnit()
        return BmiUnit.formatDisplayWeight(weightKg.toFloat(), preferredUnit)
    }

    /**
     * 获取格式化的显示身高（根据用户偏好单位）
     */
    fun getDisplayHeight(): String {
        val heightCm = _bmiRecord.value?.heightCm ?: return "--"
        val preferredUnit = BmiUnit.getPreferredHeightUnit()
        return BmiUnit.formatDisplayHeight(heightCm.toFloat(), preferredUnit)
    }
}