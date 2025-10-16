package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.enums.CholesterolLevel
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CholesterolDetailViewModel @Inject constructor(
    private val cholesterolRepository: CholesterolRepository
) : BaseViewModel() {

    private val _cholesterolRecord = MutableStateFlow<CholesterolRecord?>(null)
    val cholesterolRecord: StateFlow<CholesterolRecord?> = _cholesterolRecord.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 初始化并加载记录
     */
    fun initializeWithRecord(recordId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cholesterolRepository.observerRecord(recordId).collect { record ->
                    _cholesterolRecord.value = record
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取HDL值（整数）
     */
    fun getHdlValue(): String {
        val hdl = _cholesterolRecord.value?.hdl
        return hdl?.toString() ?: "--"
    }

    /**
     * 获取TC/HDL比率（2位小数）
     */
    fun getTcHdlRatio(): String {
        val ratio = _cholesterolRecord.value?.tcHdlRatio
        return ratio?.let { String.format(Locale.getDefault(),"%.2f", it) } ?: "--"
    }

    /**
     * 获取LDL/HDL比率（1位小数）
     */
    fun getLdlHdlRatio(): String {
        val ratio = _cholesterolRecord.value?.ldlHdlRatio
        return ratio?.let { String.format(Locale.getDefault(),"%.2f", it) } ?: "--"
    }

    /**
     * 获取记录时间
     */
    fun getRecordTime(): Date? {
        return _cholesterolRecord.value?.recordTime
    }

    /**
     * 获取胆固醇水平
     */
    fun getCholesterolLevel(): CholesterolLevel {
        val record = _cholesterolRecord.value ?: return CholesterolLevel.UNKNOWN
        return CholesterolLevel.fromMetrics(
            totalCholesterol = record.tc,
            nonHdl = record.nonHdl,
            ldl = record.ldl?.toFloat(),
            hdl = record.hdl?.toFloat()
        )
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 删除记录
     */
    suspend fun deleteRecord(): Boolean {
        val recordId = _cholesterolRecord.value?.id ?: return false
        return try {
            val result = cholesterolRepository.deleteCholesterolRecord(recordId)
            result > 0
        } catch (e: Exception) {
            _errorMessage.value = e.message
            false
        }
    }
}
