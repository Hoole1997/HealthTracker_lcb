package com.daily.health.manager.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.daily.health.manager.data.entity.PresetTimes
import com.daily.health.manager.data.repository.MedicineReminderRepository
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class AddReminderViewModel(
    private val medicineReminderRepository: MedicineReminderRepository
) : BaseViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(AddReminderUiState())
    val uiState: StateFlow<AddReminderUiState> = _uiState.asStateFlow()

    // 保存状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle(false))
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // 当前编辑的提醒ID，null表示新建模式
    private var currentReminderId: Long? = null

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

    fun setCoverUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(coverUri = uri)
    }

    /**
     * 设置是否同步到日历
     */
    fun setSyncCalendar(sync: Boolean) {
        _uiState.value = _uiState.value.copy(syncCalendar = sync)
    }

    /**
     * 初始化页面 - 支持编辑模式和新建模式
     * @param remindId 提醒ID，null表示新建模式
     * @param startDate 新建模式时的起始日期（可选）
     */
    fun initPage(remindId: Long? = null, startDate: String? = null) {
        currentReminderId = remindId

        if (remindId != null) {
            // 编辑模式：加载现有提醒数据
            loadExistingReminder(remindId)
        } else {
            // 新建模式：设置默认值，准备创建180天提醒
            _uiState.value = _uiState.value.copy(
                isEditMode = false,
                startDate = startDate ?: getCurrentDateString()
            )
        }
    }

    /**
     * 加载现有的提醒数据
     */
    private fun loadExistingReminder(remindId: Long) {
        viewModelScope.launch {
            try {
                val reminder = medicineReminderRepository.getMedicineById(remindId)
                if (reminder != null) {
                    _uiState.value = _uiState.value.copy(
                        isEditMode = true,
                        medicineName = reminder.medicineName,
                        reminderTimes = reminder.getStartRemindTimeStrings().toMutableList(),
                        dailyDoses = reminder.getStartRemindTimeStrings().size,
                        notes = reminder.note,
                        syncCalendar = reminder.isSyncToCalendar(),
                        coverUri = if(reminder.medicineCover.isEmpty()) null else reminder.medicineCover.toUri()
                    )
                    validateForm()
                }
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Load failed：${e.message}")
            }
        }
    }

    /**
     * 保存药物提醒
     */
    fun saveReminder() {
        val currentState = _uiState.value

        _saveState.value = SaveState.Loading

        viewModelScope.launch {
            try {
                if (currentState.isEditMode && currentReminderId != null) {
                    // 更新现有提醒
                    medicineReminderRepository.updateMedicine(
                        id = currentReminderId!!,
                        medicineName = currentState.medicineName.trim(),
                        reminderTimes = currentState.reminderTimes,
                        medicineCover = currentState.coverUri?.toString() ?: "",
                        note = currentState.notes.trim(),
                        syncCalendar = currentState.syncCalendar
                    )
                } else {
                    // 创建新的180天提醒
                    createMultiDayReminder(currentState)
                }
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Save failed：${e.message}")
            }
        }
    }

    /**
     * 创建180天的药物提醒
     */
    private suspend fun createMultiDayReminder(state: AddReminderUiState) {
        // 这里可以根据需求创建多天的提醒
        // 当前先创建一个基础提醒，后续可以扩展为多天逻辑
        medicineReminderRepository.addMedicine(
            medicineName = state.medicineName.trim(),
            reminderTimes = state.reminderTimes,
            medicineCover = state.coverUri?.toString() ?: "",
            note = state.notes.trim(),
            syncCalendar = state.syncCalendar
        )
    }

    /**
     * 获取当前日期字符串
     */
    private fun getCurrentDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        return "${calendar.get(java.util.Calendar.YEAR)}-" +
               "${(calendar.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')}-" +
                calendar.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
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
    val isEditMode: Boolean = false, // 是否为编辑模式
    val startDate: String = "", // 新建模式的起始日期
    val medicineName: String = "",
    val dailyDoses: Int = 3,
    val reminderTimes: List<String> = PresetTimes.THREE_TIMES_DAILY,
    val notes: String = "",
    val syncCalendar: Boolean = false,
    val isFormValid: Boolean = false,
    val coverUri: Uri? = null
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