package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.dao.HydrateReminderDao
import com.healthtracker.blood.suger.data.entity.HydrateReminder
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HydrateSettingViewModel @Inject constructor(
    private val hydrateReminderDao: HydrateReminderDao
) : BaseViewModel() {

    /**
     * 当前所有提醒时间（"HH:MM" 字符串列表），按小时与分钟升序
     */
    val reminderTimes = hydrateReminderDao.getAll()
        .map { list ->
            list.map { DateTimeUtils.formatTimeComponents(it.hour, it.minute) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** 添加提醒时间 */
    fun addReminder(hour: Int, minute: Int) {
        viewModelScope.launch {
            hydrateReminderDao.insert(HydrateReminder(hour = hour, minute = minute))
        }
    }

    /** 删除提醒时间 */
    fun deleteReminder(hour: Int, minute: Int) {
        viewModelScope.launch {
            hydrateReminderDao.deleteByTime(hour, minute)
        }
    }
}