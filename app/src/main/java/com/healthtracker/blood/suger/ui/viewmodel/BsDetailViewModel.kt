package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BsDetailViewModel @Inject constructor(
    private val bloodSugarRepository: BloodSugarRepository
) : BaseViewModel() {

    // 血糖记录状态
    private val _bloodSugarRecord = MutableStateFlow<BloodSugarRecord?>(null)
    val bloodSugarRecord: StateFlow<BloodSugarRecord?> = _bloodSugarRecord.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误状态
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 根据记录ID初始化并加载记录
     * @param recordId 血糖记录ID
     */
    fun initializeWithRecord(recordId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val record = bloodSugarRepository.getBloodSugarRecordById(recordId)
                _bloodSugarRecord.value = record

                if (record == null) {
                    _error.value = "血糖记录不存在"
                }
            } catch (e: CancellationException) {
                // 协程正常取消，不记录为错误
                "加载血糖记录操作已取消: ID=$recordId".logd(TAG)
                throw e // 重新抛出以保持协程取消语义
            } catch (e: Exception) {
                // 真正的异常情况：数据库操作失败等
                "加载血糖记录异常: ID=$recordId, 错误: ${e.javaClass.simpleName} - ${e.message}".loge(TAG)
                _error.value = "加载血糖记录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取血糖状态
     */
    fun getBloodSugarStatus(): BloodSugarStatus? {
        return _bloodSugarRecord.value?.let { record ->
            convertMeasurementTagToBloodSugarStatus(record.satus)
        }
    }

    /**
     * 获取显示单位
     */
    fun getDisplayUnit(): BsUnit? {
        return _bloodSugarRecord.value?.getSelectedUnitEnum()
    }

    /**
     * 获取显示血糖值
     */
    fun getDisplayValue(): Float? {
        return _bloodSugarRecord.value?.getDisplayGlucoseValue()?.toFloat()
    }

    /**
     * 转换测量标签为血糖状态
     * 参考BsRecordViewModel的实现
     */
    private fun convertMeasurementTagToBloodSugarStatus(statusCode: Int): BloodSugarStatus {
        return BloodSugarStatus.fromStatusType(statusCode)
    }
}