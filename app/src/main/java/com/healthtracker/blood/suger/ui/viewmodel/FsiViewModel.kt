package com.healthtracker.blood.suger.ui.viewmodel

import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.data.entity.DailyStepStat
import com.healthtracker.blood.suger.data.repo.StepRepository
import com.healthtracker.blood.suger.data.repository.HydrateRepository
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class FsiViewModel @Inject constructor(
    private val hydrateRepository: HydrateRepository,
): BaseViewModel() {
    private val repo = StepRepository.get(App.INSTANCE)
    val todayStatFlow: Flow<DailyStepStat?> = repo.observeTodayDynamic()
}