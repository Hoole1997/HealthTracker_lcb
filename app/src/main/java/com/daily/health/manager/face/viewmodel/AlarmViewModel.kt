package com.daily.health.manager.face.viewmodel

import androidx.lifecycle.viewModelScope
import com.daily.health.manager.alarm.AlarmScheduler
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.data.repository.AlarmRepository
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
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

    // 心率闹钟数据流
    private val _heartRateAlarms = MutableStateFlow<List<AlarmRecord>>(emptyList())
    val heartRateAlarms: StateFlow<List<AlarmRecord>> = _heartRateAlarms.asStateFlow()

    // BMI闹钟数据流
    private val _bmiAlarms = MutableStateFlow<List<AlarmRecord>>(emptyList())
    val bmiAlarms: StateFlow<List<AlarmRecord>> = _bmiAlarms.asStateFlow()

    // 胆固醇闹钟数据流
    private val _cholesterolAlarms = MutableStateFlow<List<AlarmRecord>>(emptyList())
    val cholesterolAlarms: StateFlow<List<AlarmRecord>> = _cholesterolAlarms.asStateFlow()
    
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
                // [Fix] 移除自动初始化逻辑：不再主动插入默认闹钟，确保新用户列表为空
                // initializeDefaultAlarmsIfNeeded()
                
                // 加载数据到StateFlow（仅加载用户已手动创建的数据）
                loadAlarmsFromDatabase()
                
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "Default alarm initialization cancelled".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "Failed to initialize default alarms: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
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
            "Preparing to insert ${defaultBloodSugarAlarms.size} default blood sugar alarms".logd(TAG)
        }
        
        // 检查并准备血压闹钟默认数据
        if (existingBloodPressureAlarms.isEmpty()) {
            val defaultBloodPressureAlarms = createDefaultBloodPressureAlarms()
            defaultAlarmsToInsert.addAll(defaultBloodPressureAlarms)
            "Preparing to insert ${defaultBloodPressureAlarms.size} default blood pressure alarms".logd(TAG)
        }
        
        // 批量插入默认闹钟（如果有需要插入的）
        if (defaultAlarmsToInsert.isNotEmpty()) {
            val insertedIds = alarmRepository.batchInsertRecords(defaultAlarmsToInsert)
            "Batch insert completed, inserted ${insertedIds.size} default alarm records".logd(TAG)
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
                "Alarm data observation cancelled (page closed or ViewModel destroyed)".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况（如数据库错误、网络问题等）
                "Failed to observe alarm data: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
        
        "Started observing database alarm data (optimized version)".logd(TAG)
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
            val heartRateAlarms = allAlarms.filter { it.type == AlarmRecord.TYPE_HEART_RATE }
            val bmiAlarms = allAlarms.filter { it.type == AlarmRecord.TYPE_BMI }
            val cholesterolAlarms = allAlarms.filter { it.type == AlarmRecord.TYPE_CHOLESTEROL }
            
            // 按时间排序（小时*60+分钟）
            val sortedBloodSugarAlarms = bloodSugarAlarms.sortedBy { it.hour * 60 + it.minute }
            val sortedBloodPressureAlarms = bloodPressureAlarms.sortedBy { it.hour * 60 + it.minute }
            val sortedHeartRateAlarms = heartRateAlarms.sortedBy { it.hour * 60 + it.minute }
            val sortedBmiAlarms = bmiAlarms.sortedBy { it.hour * 60 + it.minute }
            val sortedCholesterolAlarms = cholesterolAlarms.sortedBy { it.hour * 60 + it.minute }
            
            // 更新StateFlow
            _bloodSugarAlarms.value = sortedBloodSugarAlarms
            _bloodPressureAlarms.value = sortedBloodPressureAlarms
            _heartRateAlarms.value = sortedHeartRateAlarms
            _bmiAlarms.value = sortedBmiAlarms
            _cholesterolAlarms.value = sortedCholesterolAlarms
            
            "Data processing completed - BS: ${sortedBloodSugarAlarms.size}, BP: ${sortedBloodPressureAlarms.size}, HR: ${sortedHeartRateAlarms.size}, BMI: ${sortedBmiAlarms.size}, CH: ${sortedCholesterolAlarms.size}".logd(TAG)
        } catch (e: Exception) {
            "Failed to process alarm data: ${e.message}".loge(TAG)
        }
    }
    

    
    /**
     * 添加血糖闹钟
     * @param hour 小时
     * @param minute 分钟
     * @param repeatFlag 重复标志
     */
    fun addBloodSugarAlarm(hour: Int, minute: Int, repeatFlag: Int = AlarmRecord.REPEAT_DAILY) {
        viewModelScope.launch {
            try {
                // 检查是否已存在相同时间的闹钟
                val exists = alarmRepository.existsAtTime(hour, minute)
                if (exists) {
                    "Blood sugar alarm time ${hour}:${minute} already exists, skipping addition".logw(TAG)
                    return@launch
                }
                
                // 使用Repository的公共方法添加血糖闹钟
                val insertedId = alarmRepository.addBloodSugarReminder(hour, minute, repeatFlag)
                
                if (insertedId > 0) {
                    "Successfully added blood sugar alarm: ${hour}:${minute}, ID: $insertedId".logd(TAG)
                    
                    // 获取新创建的闹钟记录
                    val newAlarm = alarmRepository.getRecordById(insertedId)
                    if (newAlarm != null) {
                        // 调度系统级闹钟
                        val scheduled = alarmScheduler.scheduleAlarm(newAlarm)
                        if (scheduled) {
                            "System alarm scheduled successfully for blood sugar reminder: ID=$insertedId".logd(TAG)
                        } else {
                            "Failed to schedule system alarm for blood sugar reminder: ID=$insertedId".logw(TAG)
                        }
                    }
                    
                    // 数据库插入成功，StateFlow会通过observe自动更新
                } else {
                    "Failed to add blood sugar alarm: ${hour}:${minute}".loge(TAG)
                }
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "Blood sugar alarm addition cancelled: ${hour}:${minute}".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                "Blood sugar alarm addition error: ${hour}:${minute}, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
    }
    
    /**
     * 添加血压闹钟
     * @param hour 小时
     * @param minute 分钟
     * @param repeatFlag 重复标志
     */
    fun addBloodPressureAlarm(hour: Int, minute: Int, repeatFlag: Int = AlarmRecord.REPEAT_DAILY) {
        viewModelScope.launch {
            try {
                // 检查是否已存在相同时间的闹钟
                val exists = alarmRepository.existsAtTime(hour, minute)
                if (exists) {
                    "Blood pressure alarm time ${hour}:${minute} already exists, skipping addition".logw(TAG)
                    return@launch
                }
                
                // 使用Repository的公共方法添加血压闹钟
                val insertedId = alarmRepository.addBloodPressureReminder(hour, minute, repeatFlag)
                
                if (insertedId > 0) {
                    "Successfully added blood pressure alarm: ${hour}:${minute}, ID: $insertedId".logd(TAG)
                    
                    // 获取新创建的闹钟记录
                    val newAlarm = alarmRepository.getRecordById(insertedId)
                    if (newAlarm != null) {
                        // 调度系统级闹钟
                        val scheduled = alarmScheduler.scheduleAlarm(newAlarm)
                        if (scheduled) {
                            "System alarm scheduled successfully for blood pressure reminder: ID=$insertedId".logd(TAG)
                        } else {
                            "Failed to schedule system alarm for blood pressure reminder: ID=$insertedId".logw(TAG)
                        }
                    }
                    
                    // 数据库插入成功，StateFlow会通过observe自动更新
                } else {
                    "Failed to add blood pressure alarm: ${hour}:${minute}".loge(TAG)
                }
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "Blood pressure alarm addition cancelled: ${hour}:${minute}".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                "Blood pressure alarm addition error: ${hour}:${minute}, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
    }

    /**
     * 添加心率闹钟
     */
    fun addHeartRateAlarm(hour: Int, minute: Int, repeatFlag: Int = AlarmRecord.REPEAT_DAILY) {
        addAlarm(hour, minute) { h, m -> alarmRepository.addHeartRateReminder(h, m, repeatFlag) }
    }

    /**
     * 添加BMI闹钟
     */
    fun addBmiAlarm(hour: Int, minute: Int, repeatFlag: Int = AlarmRecord.REPEAT_DAILY) {
        addAlarm(hour, minute) { h, m -> alarmRepository.addBmiReminder(h, m, repeatFlag) }
    }

    /**
     * 添加胆固醇闹钟
     */
    fun addCholesterolAlarm(hour: Int, minute: Int, repeatFlag: Int = AlarmRecord.REPEAT_DAILY) {
        addAlarm(hour, minute) { h, m -> alarmRepository.addCholesterolReminder(h, m, repeatFlag) }
    }

    /**
     * 根据类型统一添加闹钟 (Public API)
     */
    fun addAlarmByType(type: Int, hour: Int, minute: Int, repeatFlag: Int = AlarmRecord.REPEAT_DAILY) {
        when (type) {
            AlarmRecord.TYPE_BLOOD_SUGAR -> addBloodSugarAlarm(hour, minute, repeatFlag)
            AlarmRecord.TYPE_BLOOD_PRESSURE -> addBloodPressureAlarm(hour, minute, repeatFlag)
            AlarmRecord.TYPE_HEART_RATE -> addHeartRateAlarm(hour, minute, repeatFlag)
            AlarmRecord.TYPE_BMI -> addBmiAlarm(hour, minute, repeatFlag)
            AlarmRecord.TYPE_CHOLESTEROL -> addCholesterolAlarm(hour, minute, repeatFlag)
        }
    }

    /**
     * 检查是否存在指定类型的任何闹钟
     */
    suspend fun hasAnyAlarm(type: Int): Boolean {
        return alarmRepository.hasAnyAlarmSync(type)
    }

    /**
     * 更新闹钟
     */
    fun updateAlarm(recordId: Long, hour: Int, minute: Int, repeatFlag: Int) {
        viewModelScope.launch {
            try {
                // 1. 获取原记录
                val record = alarmRepository.getRecordById(recordId) ?: return@launch
                
                // 2. 更新字段
                val updatedRecord = record.copy(
                    hour = hour,
                    minute = minute,
                    repeatFlag = repeatFlag,
                    updatedAt = System.currentTimeMillis()
                )
                
                // 3. 写入数据库
                val status = alarmRepository.updateExistingRecord(updatedRecord)
                
                if (status > 0) {
                    // 4. 更新系统闹钟
                    alarmScheduler.updateAlarm(updatedRecord)
                    "Updated alarm $recordId successfully".logd(TAG)
                }
            } catch (e: Exception) {
                "Failed to update alarm: ${e.message}".loge(TAG)
            }
        }
    }

    private fun addAlarm(hour: Int, minute: Int, insertAction: suspend (Int, Int) -> Long) {
        viewModelScope.launch {
            try {
                val exists = alarmRepository.existsAtTime(hour, minute)
                if (exists) {
                    "Alarm time ${hour}:${minute} already exists".logw(TAG)
                    return@launch
                }
                val insertedId = insertAction(hour, minute)
                if (insertedId > 0) {
                    val newAlarm = alarmRepository.getRecordById(insertedId)
                    if (newAlarm != null) {
                        alarmScheduler.scheduleAlarm(newAlarm)
                    }
                }
            } catch (e: Exception) {
                "Alarm addition error: ${e.message}".loge(TAG)
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
                // 先获取闹钟记录
                val alarmRecord = alarmRepository.getRecordById(alarmId)
                if (alarmRecord == null) {
                    "Alarm record not found: ID=$alarmId".logw(TAG)
                    return@launch
                }
                
                // 更新数据库
                val success = if (isEnabled) {
                    alarmRepository.enableAlarm(alarmId)
                } else {
                    alarmRepository.disableAlarm(alarmId)
                }
                
                if (success) {
                    "Successfully updated alarm status: ID=$alarmId, enabled=$isEnabled".logd(TAG)
                    
                    // 同步更新系统级闹钟
                    val updatedAlarm = alarmRecord.copy(isEnabled = isEnabled)
                    val systemSuccess = alarmScheduler.updateAlarm(updatedAlarm)
                    if (systemSuccess) {
                        "System alarm updated successfully: ID=$alarmId, enabled=$isEnabled".logd(TAG)
                    } else {
                        "Failed to update system alarm: ID=$alarmId, enabled=$isEnabled".logw(TAG)
                    }
                    
                    // 数据库更新成功，StateFlow会通过observe自动更新
                } else {
                    "Failed to update alarm status: ID=$alarmId, enabled=$isEnabled".loge(TAG)
                }
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "Alarm status update cancelled: ID=$alarmId".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "Alarm status update error: ID=$alarmId, enabled=$isEnabled, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
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
                // 先获取闹钟记录用于取消系统级闹钟
                val alarmRecord = alarmRepository.getRecordById(alarmId)
                
                // 软删除数据库记录
                val success = alarmRepository.softDeleteRecord(alarmId)
                if (success) {
                    "Successfully deleted alarm: ID=$alarmId".logd(TAG)
                    
                    // 取消系统级闹钟
                    if (alarmRecord != null) {
                        val systemSuccess = alarmScheduler.cancelAlarm(alarmRecord)
                        if (systemSuccess) {
                            "System alarm cancelled successfully: ID=$alarmId".logd(TAG)
                        } else {
                            "Failed to cancel system alarm: ID=$alarmId".logw(TAG)
                        }
                    }
                    
                    // StateFlow会通过observe自动更新，无需手动刷新
                } else {
                    "Failed to delete alarm: ID=$alarmId".loge(TAG)
                }
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "Alarm deletion cancelled: ID=$alarmId".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "Alarm deletion error: ID=$alarmId, Error: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
            }
        }
    }
}