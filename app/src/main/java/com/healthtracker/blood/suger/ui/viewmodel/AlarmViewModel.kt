package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.data.repository.AlarmRepository
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : BaseViewModel() {
    
    companion object {
        private const val TAG = "AlarmViewModel"
    }
    
    // 血糖闹钟数据流
    private val _bloodSugarAlarms = MutableStateFlow<List<AlarmRecord>>(emptyList())
    val bloodSugarAlarms: StateFlow<List<AlarmRecord>> = _bloodSugarAlarms.asStateFlow()
    
    // 血压闹钟数据流
    private val _bloodPressureAlarms = MutableStateFlow<List<AlarmRecord>>(emptyList())
    val bloodPressureAlarms: StateFlow<List<AlarmRecord>> = _bloodPressureAlarms.asStateFlow()
    
    init {
        // 初始化默认闹钟数据
        initDefaultAlarms()
    }
    
    /**
     * 初始化默认闹钟数据
     * 首次启动时将默认闹钟插入数据库，后续从数据库加载
     */
    private fun initDefaultAlarms() {
        viewModelScope.launch {
            try {
                // 先从数据库加载数据
                loadAlarmsFromDatabase()
                
                // 检查是否需要插入默认数据
                // 通过观察StateFlow来判断是否为空，如果为空则插入默认数据
                viewModelScope.launch {
                    // 等待数据加载完成后检查
                    kotlinx.coroutines.delay(500) // 给数据库查询一些时间
                    
                    if (_bloodSugarAlarms.value.isEmpty()) {
                        insertDefaultBloodSugarAlarms()
                    }
                    
                    if (_bloodPressureAlarms.value.isEmpty()) {
                        insertDefaultBloodPressureAlarms()
                    }
                }
                
            } catch (e: Exception) {
                // 异常处理：数据库操作失败
                "初始化默认闹钟失败: ${e.message}".loge(TAG)
            }
        }
    }
    
    /**
     * 插入默认血糖闹钟数据到数据库
     */
    private suspend fun insertDefaultBloodSugarAlarms() {
        val defaultBloodSugarAlarms = listOf(
            AlarmRecord.createBloodSugarReminder(hour = 6, minute = 0),
            AlarmRecord.createBloodSugarReminder(hour = 9, minute = 0),
            AlarmRecord.createBloodSugarReminder(hour = 14, minute = 0),
            AlarmRecord.createBloodSugarReminder(hour = 20, minute = 0)
        )
        
        try {
            // 逐个插入默认血糖闹钟，避免重复数据
            val insertedIds = mutableListOf<Long>()
            for (alarm in defaultBloodSugarAlarms) {
                val id = alarmRepository.addBloodSugarReminder(alarm.hour, alarm.minute)
                insertedIds.add(id)
            }
            if (insertedIds.isNotEmpty()) {
                // 插入成功日志
                "成功插入${insertedIds.size}条默认血糖闹钟".logd(TAG)
            }
        } catch (e: Exception) {
            "插入默认血糖闹钟失败: ${e.message}".loge(TAG)
            throw e
        }
    }
    
    /**
     * 插入默认血压闹钟数据到数据库
     */
    private suspend fun insertDefaultBloodPressureAlarms() {
        val defaultBloodPressureAlarms = listOf(
            AlarmRecord.createBloodPressureReminder(hour = 6, minute = 0),
            AlarmRecord.createBloodPressureReminder(hour = 20, minute = 0)
        )
        
        try {
            // 逐个插入默认血压闹钟，避免重复数据
            val insertedIds = mutableListOf<Long>()
            for (alarm in defaultBloodPressureAlarms) {
                val id = alarmRepository.addBloodPressureReminder(alarm.hour, alarm.minute)
                insertedIds.add(id)
            }
            if (insertedIds.isNotEmpty()) {
                // 插入成功日志
                "成功插入${insertedIds.size}条默认血压闹钟".logd(TAG)
            }
        } catch (e: Exception) {
            "插入默认血压闹钟失败: ${e.message}".loge(TAG)
            throw e
        }
    }
    
    /**
     * 从数据库加载闹钟数据
     */
    private fun loadAlarmsFromDatabase() {
        // 启动协程观察血糖闹钟数据变化
        viewModelScope.launch {
            try {
                // 使用Repository的公共方法获取血糖闹钟
                alarmRepository.getBloodSugarReminders().collect { alarms ->
                    _bloodSugarAlarms.value = alarms.sortedBy { it.hour * 60 + it.minute }
                }
            } catch (e: Exception) {
                "观察血糖闹钟数据失败: ${e.message}".loge(TAG)
            }
        }
        
        // 启动协程观察血压闹钟数据变化
        viewModelScope.launch {
            try {
                // 使用Repository的公共方法获取血压闹钟
                alarmRepository.getBloodPressureReminders().collect { alarms ->
                    _bloodPressureAlarms.value = alarms.sortedBy { it.hour * 60 + it.minute }
                }
            } catch (e: Exception) {
                "观察血压闹钟数据失败: ${e.message}".loge(TAG)
            }
        }
        
        "开始观察数据库闹钟数据".logd(TAG)
    }
    

    
    /**
     * 添加血糖闹钟
     * @param hour 小时
     * @param minute 分钟
     */
    fun addBloodSugarAlarm(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                // 检查是否已存在相同时间的闹钟
                val exists = alarmRepository.existsAtTime(hour, minute)
                if (exists) {
                    "血糖闹钟时间${hour}:${minute}已存在，跳过添加".logw(TAG)
                    return@launch
                }
                
                // 使用Repository的公共方法添加血糖闹钟
                val insertedId = alarmRepository.addBloodSugarReminder(hour, minute)
                
                if (insertedId > 0) {
                    "成功添加血糖闹钟: ${hour}:${minute}, ID: $insertedId".logd(TAG)
                    // 数据库插入成功，StateFlow会通过observe自动更新
                } else {
                    "添加血糖闹钟失败: ${hour}:${minute}".loge(TAG)
                }
            } catch (e: Exception) {
                "添加血糖闹钟异常: ${hour}:${minute}, 错误: ${e.message}".loge(TAG)
            }
        }
    }
    
    /**
     * 添加血压闹钟
     * @param hour 小时
     * @param minute 分钟
     */
    fun addBloodPressureAlarm(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                // 检查是否已存在相同时间的闹钟
                val exists = alarmRepository.existsAtTime(hour, minute)
                if (exists) {
                    "血压闹钟时间${hour}:${minute}已存在，跳过添加".logw(TAG)
                    return@launch
                }
                
                // 使用Repository的公共方法添加血压闹钟
                val insertedId = alarmRepository.addBloodPressureReminder(hour, minute)
                
                if (insertedId > 0) {
                    "成功添加血压闹钟: ${hour}:${minute}, ID: $insertedId".logd(TAG)
                    // 数据库插入成功，StateFlow会通过observe自动更新
                } else {
                    "添加血压闹钟失败: ${hour}:${minute}".loge(TAG)
                }
            } catch (e: Exception) {
                "添加血压闹钟异常: ${hour}:${minute}, 错误: ${e.message}".loge(TAG)
            }
        }
    }
    

    
    /**
     * 更新闹钟启用状态
     * @param alarmId 闹钟ID
     * @param isEnabled 是否启用
     * @param alarmType 闹钟类型
     */
    fun updateAlarmEnabled(alarmId: Long, isEnabled: Boolean, alarmType: Int) {
        viewModelScope.launch {
            try {
                // 先更新数据库
                val success = if (isEnabled) {
                    alarmRepository.enableAlarm(alarmId)
                } else {
                    alarmRepository.disableAlarm(alarmId)
                }
                
                if (success) {
                    "成功更新闹钟状态: ID=$alarmId, enabled=$isEnabled".logd(TAG)
                    // 数据库更新成功，StateFlow会通过observe自动更新
                } else {
                    "更新闹钟状态失败: ID=$alarmId, enabled=$isEnabled".loge(TAG)
                }
            } catch (e: Exception) {
                "更新闹钟状态异常: ID=$alarmId, enabled=$isEnabled, 错误: ${e.message}".loge(TAG)
            }
        }
    }
}