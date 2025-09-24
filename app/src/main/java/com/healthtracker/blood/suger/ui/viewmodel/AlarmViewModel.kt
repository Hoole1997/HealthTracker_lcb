package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.data.repository.AlarmRepository
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : BaseViewModel() {
    

    
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
     * 优化版本：合并加载和插入操作到一个协程中，使用同步查询和批量插入
     */
    private fun initDefaultAlarms() {
        viewModelScope.launch {
            try {
                // 执行默认闹钟初始化逻辑
                initializeDefaultAlarmsIfNeeded()
                
                // 加载数据到StateFlow（无论是否插入了新数据都需要加载）
                loadAlarmsFromDatabase()
                
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "默认闹钟初始化已取消".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "初始化默认闹钟失败: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
    }
    
    /**
     * 检查并初始化默认闹钟数据
     * 如果数据库中不存在对应类型的闹钟，则插入默认闹钟
     * 
     * @throws Exception 数据库操作异常
     */
    private suspend fun initializeDefaultAlarmsIfNeeded() {
        // 同步获取当前数据库中的所有闹钟记录
        val existingRecords = alarmRepository.getAllRecordsSync()
        
        // 分离血糖和血压闹钟
        val existingBloodSugarAlarms = existingRecords.filter { it.type == AlarmRecord.TYPE_BLOOD_SUGAR }
        val existingBloodPressureAlarms = existingRecords.filter { it.type == AlarmRecord.TYPE_BLOOD_PRESSURE }
        
        // 准备需要插入的默认闹钟列表
        val defaultAlarmsToInsert = mutableListOf<AlarmRecord>()
        
        // 检查并准备血糖闹钟默认数据
        if (existingBloodSugarAlarms.isEmpty()) {
            val defaultBloodSugarAlarms = createDefaultBloodSugarAlarms()
            defaultAlarmsToInsert.addAll(defaultBloodSugarAlarms)
            "准备插入${defaultBloodSugarAlarms.size}条默认血糖闹钟".logd(TAG)
        }
        
        // 检查并准备血压闹钟默认数据
        if (existingBloodPressureAlarms.isEmpty()) {
            val defaultBloodPressureAlarms = createDefaultBloodPressureAlarms()
            defaultAlarmsToInsert.addAll(defaultBloodPressureAlarms)
            "准备插入${defaultBloodPressureAlarms.size}条默认血压闹钟".logd(TAG)
        }
        
        // 批量插入默认闹钟（如果有需要插入的）
        if (defaultAlarmsToInsert.isNotEmpty()) {
            val insertedIds = alarmRepository.batchInsertRecords(defaultAlarmsToInsert)
            "批量插入完成，共插入${insertedIds.size}条默认闹钟记录".logd(TAG)
        }
    }
    
    /**
     * 创建默认血糖闹钟列表
     * 
     * @return 默认血糖闹钟记录列表
     */
    private fun createDefaultBloodSugarAlarms(): List<AlarmRecord> {
        return listOf(
            AlarmRecord.createBloodSugarReminder(hour = 6, minute = 0),
            AlarmRecord.createBloodSugarReminder(hour = 9, minute = 0),
            AlarmRecord.createBloodSugarReminder(hour = 14, minute = 0),
            AlarmRecord.createBloodSugarReminder(hour = 20, minute = 0)
        )
    }
    
    /**
     * 创建默认血压闹钟列表
     * 
     * @return 默认血压闹钟记录列表
     */
    private fun createDefaultBloodPressureAlarms(): List<AlarmRecord> {
        return listOf(
            AlarmRecord.createBloodPressureReminder(hour = 6, minute = 0),
            AlarmRecord.createBloodPressureReminder(hour = 20, minute = 0)
        )
    }
    

    
    /**
     * 从数据库加载闹钟数据
     * 优化版本：使用单一查询获取所有闹钟记录，然后在内存中进行过滤和排序
     */
    private fun loadAlarmsFromDatabase() {
        // 启动单一协程观察所有闹钟数据变化
        viewModelScope.launch {
            try {
                // 使用Repository的getAllRecordsFlow方法获取所有闹钟记录
                alarmRepository.getAllRecordsFlow().collect { allAlarms ->
                    // 检查协程是否仍然活跃，避免在取消过程中执行不必要的操作
                    if (isActive) {
                        // 在内存中进行数据过滤和排序处理
                        processAndUpdateAlarmData(allAlarms)
                    }
                }
            } catch (e: CancellationException) {
                // 协程正常取消（如页面关闭），不记录为错误
                "闹钟数据观察已取消（页面关闭或ViewModel销毁）".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况（如数据库错误、网络问题等）
                "观察闹钟数据失败: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
        
        "开始观察数据库闹钟数据（优化版本）".logd(TAG)
    }
    
    /**
     * 处理并更新闹钟数据
     * 将完整的闹钟记录列表按类型过滤并排序后更新到对应的StateFlow
     * 
     * @param allAlarms 所有闹钟记录列表
     */
    private fun processAndUpdateAlarmData(allAlarms: List<AlarmRecord>) {
        try {
            // 按类型过滤闹钟记录
            val bloodSugarAlarms = allAlarms.filter { it.type == AlarmRecord.TYPE_BLOOD_SUGAR }
            val bloodPressureAlarms = allAlarms.filter { it.type == AlarmRecord.TYPE_BLOOD_PRESSURE }
            
            // 按时间排序（小时*60+分钟）
            val sortedBloodSugarAlarms = bloodSugarAlarms.sortedBy { it.hour * 60 + it.minute }
            val sortedBloodPressureAlarms = bloodPressureAlarms.sortedBy { it.hour * 60 + it.minute }
            
            // 更新StateFlow
            _bloodSugarAlarms.value = sortedBloodSugarAlarms
            _bloodPressureAlarms.value = sortedBloodPressureAlarms
            
            "数据处理完成 - 血糖闹钟: ${sortedBloodSugarAlarms.size}条, 血压闹钟: ${sortedBloodPressureAlarms.size}条".logd(TAG)
        } catch (e: Exception) {
            "处理闹钟数据失败: ${e.message}".loge(TAG)
        }
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
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "添加血糖闹钟操作已取消: ${hour}:${minute}".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                "添加血糖闹钟异常: ${hour}:${minute}, 错误: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
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
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "添加血压闹钟操作已取消: ${hour}:${minute}".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                "添加血压闹钟异常: ${hour}:${minute}, 错误: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
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
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "更新闹钟状态操作已取消: ID=$alarmId".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "更新闹钟状态异常: ID=$alarmId, enabled=$isEnabled, 错误: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
    }
    
    /**
     * 删除闹钟
     * @param alarmId 闹钟ID
     */
    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            try {
                val success = alarmRepository.softDeleteRecord(alarmId)
                if (success) {
                    "成功删除闹钟: ID=$alarmId".logd(TAG)
                    // StateFlow会通过observe自动更新，无需手动刷新
                } else {
                    "删除闹钟失败: ID=$alarmId".loge(TAG)
                }
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "删除闹钟操作已取消: ID=$alarmId".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "删除闹钟异常: ID=$alarmId, 错误: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
    }
}