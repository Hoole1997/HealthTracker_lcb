package com.healthtracker.blood.suger.ui.act

import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.data.entity.DailyStepStat
import com.healthtracker.blood.suger.data.repo.StepRepository
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class StepCountViewModel : BaseViewModel() {
    private val repo = StepRepository.get(App.INSTANCE)
    private val zoneId = ZoneId.systemDefault()

    val todayStatFlow: Flow<DailyStepStat?> = repo.observeToday()

    fun recent7DaysFlow(): Flow<List<DailyStepStat>> {
        val end = LocalDate.now(zoneId).toEpochDay()
        val start = end - 6
        return repo.range(start, end)
    }
}