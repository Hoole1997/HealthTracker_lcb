package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.HydrateRecord
import com.healthtracker.blood.suger.data.repository.HydrateRepository
import com.healthtracker.blood.suger.ui.adapter.HydrateRecordItem
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HydrateViewModel @Inject constructor(
    private val hydrateRepository: HydrateRepository
) : BaseViewModel() {

    // 今日饮水记录列表
    val todayRecords = hydrateRepository.getTodayRecords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 今日总饮水量（ML）
    val todayTotalIntakeMl = todayRecords
        .map { records -> records.sumOf { it.intakeMl } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 今日饮水次数
    val todayDrinkCount = todayRecords
        .map { records -> records.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 供 RecordAdapter 使用的 UI 数据
    val todayRecordItems = todayRecords
        .map { records -> records.map { HydrateRecordItem(id = it.id, intakeMl = it.intakeMl, date = it.recordTime) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 快捷添加：新增一条饮水记录
    fun addIntake(intakeMl: Int) {
        viewModelScope.launch {
            hydrateRepository.addHydrateRecord(intakeMl)
        }
    }

    // 预留：按ID删除（当前 UI 未暴露ID，可后续扩展）
    fun deleteRecordById(id: Long) {
        viewModelScope.launch {
            hydrateRepository.deleteHydrateRecordById(id)
        }
    }
}