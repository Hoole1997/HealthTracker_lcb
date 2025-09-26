package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.PresetTimes
import com.healthtracker.blood.suger.data.repository.MedicineReminderRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val medicineReminderRepository: MedicineReminderRepository
) : BaseViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(AddReminderUiState())
    val uiState: StateFlow<AddReminderUiState> = _uiState.asStateFlow()

    // 保存状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle(false))
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    /**
     * 设置药物名称
     */
    fun setMedicineName(name: String) {
        _uiState.value = _uiState.value.copy(medicineName = name)
        validateForm()
    }

    /**
     * 设置每日服药次数
     */
    fun setDailyDoses(count: Int) {
        if (count < 1 || count > 6) return

        val newTimes = when (count) {
            1 -> PresetTimes.ONCE_DAILY
            2 -> PresetTimes.TWICE_DAILY
            3 -> PresetTimes.THREE_TIMES_DAILY
            4 -> PresetTimes.FOUR_TIMES_DAILY
            5 -> PresetTimes.FIVE_TIMES_DAILY
            6 -> PresetTimes.SIX_TIMES_DAILY
            else -> PresetTimes.THREE_TIMES_DAILY
        }

        _uiState.value = _uiState.value.copy(
            dailyDoses = count,
            reminderTimes = newTimes.toMutableList()
        )
        validateForm()
    }

    /**
     * 更新指定位置的提醒时间
     */
    fun updateReminderTime(index: Int, time: String) {
        val currentTimes = _uiState.value.reminderTimes.toMutableList()
        if (index in 0 until currentTimes.size) {
            currentTimes[index] = time
            _uiState.value = _uiState.value.copy(reminderTimes = currentTimes)
            validateForm()
        }
    }

    /**
     * 设置备注
     */
    fun setNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    /**
     * 设置是否同步到日历
     */
    fun setSyncCalendar(sync: Boolean) {
        _uiState.value = _uiState.value.copy(syncCalendar = sync)
    }

    /**
     * 保存药物提醒
     */
    fun saveReminder() {
        val currentState = _uiState.value

        if (!currentState.isFormValid) {
            _saveState.value = SaveState.Error("请填写完整信息")
            return
        }

        _saveState.value = SaveState.Loading

        viewModelScope.launch {
            try {
                medicineReminderRepository.addMedicine(
                    medicineName = currentState.medicineName.trim(),
                    reminderTimes = currentState.reminderTimes,
                    note = currentState.notes.trim(),
                    syncCalendar = currentState.syncCalendar
                )
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Save File：${e.message}")
            }
        }
    }


    /**
     * 表单验证
     */
    private fun validateForm() {
        val currentState = _uiState.value
        val isValid = currentState.medicineName.isNotBlank() &&
                     currentState.reminderTimes.isNotEmpty() &&
                     currentState.reminderTimes.all { it.isNotBlank() }

        _uiState.value = currentState.copy(isFormValid = isValid)
        _saveState.value = SaveState.Idle(isValid)
    }

    /**
     * 初始化默认状态
     */
    init {
        setDailyDoses(3) // 默认一日3次
    }
}

/**
 * UI状态数据类
 */
data class AddReminderUiState(
    val medicineName: String = "",
    val dailyDoses: Int = 3,
    val reminderTimes: List<String> = PresetTimes.THREE_TIMES_DAILY,
    val notes: String = "",
    val syncCalendar: Boolean = false,
    val isFormValid: Boolean = false
)

/**
 * 保存状态封装
 */
sealed class SaveState {
    data class Idle(val isAlbe: Boolean = false) : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}