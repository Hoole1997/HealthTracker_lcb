package com.healthtracker.blood.suger.ui.act

import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.data.entity.DailyStepStat
import com.healthtracker.blood.suger.data.repo.StepRepository
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.flow.Flow
import com.healthtracker.blood.suger.data.utils.DateTimeUtils

class StepCountViewModel : BaseViewModel() {
    private val repo = StepRepository.get(App.INSTANCE)

    val todayStatFlow: Flow<DailyStepStat?> = repo.observeTodayDynamic()

    fun recent7DaysFlow(): Flow<List<DailyStepStat>> {
        val end = DateTimeUtils.getTodayRange().first.time / 86_400_000L
        val start = end - 6
        return repo.range(start, end)
    }
}