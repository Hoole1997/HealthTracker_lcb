package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.config.BloodSugarRangeManager
import com.healthtracker.blood.suger.data.enums.BloodSugarRanges
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 血糖目标范围设置页面的 ViewModel
 */
@HiltViewModel
class TargetRangeViewModel @Inject constructor() : BaseViewModel() {

    // 当前选择的单位
    private val _currentUnit = MutableStateFlow(BsUnit.getPreferredUnit())
    val currentUnit: StateFlow<BsUnit> = _currentUnit.asStateFlow()

    // 8 种状态的范围项列表
    private val _rangeItems = MutableStateFlow<List<RangeItem>>(emptyList())
    val rangeItems: StateFlow<List<RangeItem>> = _rangeItems.asStateFlow()

    // 是否有任何自定义范围（任何状态、任何单位）
    // 用于控制 Reset 按钮的显示
    private val _hasAnyCustomRanges = MutableStateFlow(false)
    val hasAnyCustomRanges: StateFlow<Boolean> = _hasAnyCustomRanges.asStateFlow()

    init {
        loadRangeItems()
    }

    /**
     * 加载所有状态的范围数据
     */
    fun loadRangeItems() {
        viewModelScope.launch {
            val unit = _currentUnit.value
            val items = BloodSugarStatus.entries.map { status ->
                val ranges = status.getRangesForUnit(unit)
                val isCustomized = BloodSugarRangeManager.hasCustomRanges(status.statusType, unit)
                RangeItem(
                    status = status,
                    ranges = ranges,
                    isCustomized = isCustomized
                )
            }
            _rangeItems.value = items

            // 更新是否有任何自定义范围的状态
            updateHasAnyCustomRanges()
        }
    }

    /**
     * 保存指定状态的范围值
     * @param status 血糖状态
     * @param inputUnit 用户输入时使用的单位
     * @param ranges 用户输入的范围值
     */
    fun saveRanges(status: BloodSugarStatus, inputUnit: BsUnit, ranges: BloodSugarRanges) {
        viewModelScope.launch {
            // 1. 保存用户输入的单位的范围
            BloodSugarRangeManager.updateCustomRanges(status.statusType, inputUnit, ranges)

            // 2. 同步转换并保存另一个单位的范围
            val otherUnit = if (inputUnit == BsUnit.MG_DL) BsUnit.MMOL_L else BsUnit.MG_DL
            val convertedRanges = BsUnit.convertRanges(ranges, inputUnit, otherUnit)
            BloodSugarRangeManager.updateCustomRanges(status.statusType, otherUnit, convertedRanges)

            // 3. 重新加载数据
            loadRangeItems()
        }
    }

    /**
     * 重置所有状态的范围到默认值
     */
    fun resetAllRanges() {
        viewModelScope.launch {
            // 清除所有自定义范围
            BloodSugarStatus.entries.forEach { status ->
                BsUnit.entries.forEach { unit ->
                    BloodSugarRangeManager.resetToDefault(status.statusType, unit)
                }
            }
            // 重新加载数据
            loadRangeItems()
        }
    }

    /**
     * 切换显示单位
     * 当用户在对话框中切换单位后，需要同步更新页面显示单位
     */
    fun switchUnit(newUnit: BsUnit) {
        if (_currentUnit.value != newUnit) {
            _currentUnit.value = newUnit
            BsUnit.savePreferredUnit(newUnit)  // 保存首选单位
            loadRangeItems()  // 重新加载数据
        }
    }

    /**
     * 检查是否有任何自定义范围（任何状态、任何单位）
     * 用于控制 Reset 按钮的显示
     */
    private fun updateHasAnyCustomRanges() {
        val hasAny = BloodSugarStatus.entries.any { status ->
            BsUnit.entries.any { unit ->
                BloodSugarRangeManager.hasCustomRanges(status.statusType, unit)
            }
        }
        _hasAnyCustomRanges.value = hasAny
    }
}

/**
 * 范围列表项数据类
 * @param status 血糖状态
 * @param ranges 当前单位下的范围值
 * @param isCustomized 是否已自定义（字段保留但不显示）
 */
data class RangeItem(
    val status: BloodSugarStatus,
    val ranges: BloodSugarRanges,
    val isCustomized: Boolean = false
)