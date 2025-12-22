package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.MedicineReminder
import com.healthtracker.blood.suger.data.repository.MedicineReminderRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.ui.model.MedsReminderItem
import com.healthtracker.blood.suger.ui.model.ReminderStatus
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * 药物管理页面ViewModel
 * 负责处理日期选择逻辑和相关数据管理
 */
class MedsViewModel(
    // 这里可以注入需要的Repository，比如药物相关的Repository
     private val medsRepository: MedicineReminderRepository
) : BaseViewModel() {

    // 当前选中的日期
    private val _selectedDate = MutableStateFlow(DateTimeUtils.now())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    // 当前显示的周是否为系统当前周
    private val _isCurrentWeek = MutableStateFlow(true)
    val isCurrentWeek: StateFlow<Boolean> = _isCurrentWeek.asStateFlow()

    // 选中日期是否可以添加提醒（不早于当前日期）
    val canAddReminder: StateFlow<Boolean> = selectedDate.map { selectedDate ->
        !DateTimeUtils.isSameDay(selectedDate, DateTimeUtils.now()) && 
        selectedDate.before(DateTimeUtils.now())
    }.map { isBeforeToday ->
        !isBeforeToday // 如果不是早于今天，则可以添加提醒
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true // 默认可以添加（当前日期）
    )

    // 格式化月份的Flow，基于selectedDate动态计算
    val formattedMonth: StateFlow<Date> = selectedDate.map { date ->
        date
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _selectedDate.value
    )

    // 药物提醒列表数据
    val reminderItems: StateFlow<List<MedsReminderItem>> = combine(
        selectedDate,
        medsRepository.getActiveReminders()
    ) { selectedDate, allReminders ->
        convertToReminderItems(selectedDate, allReminders)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * 日期选择处理
     * @param date 选中的日期
     */
    fun onDateSelected(date: Date) {
        viewModelScope.launch {
            "Date selected: ${DateTimeUtils.formatDate(date)}".logd(TAG)
            _selectedDate.value = date
            
            // 加载选中日期的数据
            loadDataForSelectedDate(date)
        }
    }

    /**
     * 处理周切换事件
     * @param isCurrentWeek 当前显示的周是否为系统当前周
     */
    fun onWeekChanged(isCurrentWeek: Boolean) {
        viewModelScope.launch {
            "Week view switched, isCurrentWeek: $isCurrentWeek".logd(TAG)
            
            _isCurrentWeek.value = isCurrentWeek
            
            // 这里可以添加根据周切换加载相关数据的逻辑
            loadDataForCurrentWeek(isCurrentWeek)
        }
    }

    /**
     * 根据选中的日期加载相关数据
     * @param date 选中的日期
     */
    private fun loadDataForSelectedDate(date: Date) {
        // 数据已通过combine自动更新，此方法用于日志记录
        "Load medication data for date ${DateTimeUtils.formatDate(date)}".logd(TAG)
    }

    /**
     * 根据周切换加载相关数据
     * @param isCurrentWeek 是否为当前周
     */
    private fun loadDataForCurrentWeek(isCurrentWeek: Boolean) {
        // 数据已通过combine自动更新，此方法用于日志记录
        "Load medication data for ${if (isCurrentWeek) "current week" else "other week"}".logd(TAG)
    }

    /**
     * 将药物提醒数据转换为显示项列表
     * @param selectedDate 选中的日期
     * @param allReminders 所有活跃的药物提醒
     * @return 排序后的显示项列表
     */
    private fun convertToReminderItems(
        selectedDate: Date,
        allReminders: List<MedicineReminder>
    ): List<MedsReminderItem> {
        return allReminders
            .filter { reminder ->
                // 过滤180天范围内的提醒
                isWithin180Days(selectedDate, reminder.createdAt)
            }
            .flatMap { reminder ->
                // 将每个提醒展开为多个时间点
                expandReminderToItems(selectedDate, reminder)
            }
            .sortedBy { it.reminderDateTime } // 按时间排序
            .also { items ->
                "Conversion completed, ${items.size} reminder items".logd(TAG)
            }
    }

    /**
     * 判断选中日期是否在创建时间的180天范围内
     * @param selectedDate 选中日期
     * @param createdDate 创建时间
     * @return 是否在范围内
     */
    private fun isWithin180Days(selectedDate: Date, createdDate: Date): Boolean {
        val selectedCalendar = Calendar.getInstance().apply { time = selectedDate }
        val createdCalendar = Calendar.getInstance().apply { time = createdDate }

        // 重置到当天开始时间，按自然天计算
        selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
        selectedCalendar.set(Calendar.MINUTE, 0)
        selectedCalendar.set(Calendar.SECOND, 0)
        selectedCalendar.set(Calendar.MILLISECOND, 0)

        createdCalendar.set(Calendar.HOUR_OF_DAY, 0)
        createdCalendar.set(Calendar.MINUTE, 0)
        createdCalendar.set(Calendar.SECOND, 0)
        createdCalendar.set(Calendar.MILLISECOND, 0)

        val daysDifference = (selectedCalendar.timeInMillis - createdCalendar.timeInMillis) / (24 * 60 * 60 * 1000)
        return daysDifference in 0..180
    }

    /**
     * 将单个药物提醒展开为多个时间点的显示项
     * @param selectedDate 选中日期
     * @param reminder 药物提醒
     * @return 该提醒在选中日期的所有时间点显示项
     */
    private fun expandReminderToItems(
        selectedDate: Date,
        reminder: MedicineReminder
    ): List<MedsReminderItem> {
        val timeStrings = reminder.getStartRemindTimeStrings()
        val takenTimesList = reminder.getTakedTimeList()

        return timeStrings.map { timeString ->
            val reminderDateTime = combineDateTime(selectedDate, timeString)
            val status = determineReminderStatus(reminderDateTime, takenTimesList)

            MedsReminderItem(
                reminderId = reminder.id,
                time = timeString,
                medicineName = reminder.medicineName,
                notes = reminder.note,
                status = status,
                reminderDateTime = reminderDateTime,
                medicineCover = reminder.medicineCover
            )
        }
    }

    /**
     * 组合日期和时间字符串为完整的DateTime对象
     * @param date 日期
     * @param timeString 时间字符串，格式为"HH:mm"
     * @return 完整的日期时间
     */
    private fun combineDateTime(date: Date, timeString: String): Date {
        val calendar = Calendar.getInstance().apply { time = date }
        val timeParts = timeString.split(":")

        if (timeParts.size == 2) {
            val hour = timeParts[0].toIntOrNull() ?: 0
            val minute = timeParts[1].toIntOrNull() ?: 0

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        return calendar.time
    }

    /**
     * 判断服药状态
     * @param reminderDateTime 提醒时间
     * @param takenTimesList 已服药时间列表
     * @return 服药状态
     */
    private fun determineReminderStatus(
        reminderDateTime: Date,
        takenTimesList: List<Date>
    ): ReminderStatus {
        // 检查是否存在匹配的服药时间
        val isTaken = takenTimesList.any { takenTime ->
            isSameDateTime(reminderDateTime, takenTime)
        }

        return if (isTaken) ReminderStatus.TAKEN else ReminderStatus.PENDING
    }

    /**
     * 比较两个时间是否为同一时刻（精确到分钟）
     * @param time1 时间1
     * @param time2 时间2
     * @return 是否为同一时刻
     */
    private fun isSameDateTime(time1: Date, time2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = time1 }
        val cal2 = Calendar.getInstance().apply { time = time2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) &&
               cal1.get(Calendar.HOUR_OF_DAY) == cal2.get(Calendar.HOUR_OF_DAY) &&
               cal1.get(Calendar.MINUTE) == cal2.get(Calendar.MINUTE)
    }

    /**
     * 标记服药
     * @param reminderId 提醒ID
     * @param reminderDateTime 提醒时间
     */
    fun markMedicationTaken(reminderId: Long, reminderDateTime: Date) {
        viewModelScope.launch {
            try {
                "Mark medication taken: ID=$reminderId, Time=${DateTimeUtils.formatDateTime(reminderDateTime)}".logd(TAG)
                medsRepository.recordMedication(reminderId, reminderDateTime)
                "Medication marked successfully".logd(TAG)
            } catch (e: Exception) {
                "Failed to mark medication: ${e.message}".logd(TAG)
            }
        }
    }

    /**
     * 删除药物提醒
     * @param reminderId 提醒ID
     */
    fun deleteMedicineReminder(reminderId: Long) {
        viewModelScope.launch {
            try {
                "Delete medication reminder: ID=$reminderId".logd(TAG)
                medsRepository.deleteMedicine(reminderId)
                "Medication reminder deleted successfully".logd(TAG)
            } catch (e: Exception) {
                "Failed to delete medication reminder: ${e.message}".logd(TAG)
            }
        }
    }

    /**
     * 获取格式化的月份字符串
     * @return 格式化的月份字符串，如"Sep.2025"
     */
    fun getFormattedMonth(): String {
        return DateTimeUtils.formatMonthYear(_selectedDate.value)
    }

}