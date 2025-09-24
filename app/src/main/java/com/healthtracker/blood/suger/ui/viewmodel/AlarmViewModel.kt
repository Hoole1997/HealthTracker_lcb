package com.healthtracker.blood.suger.ui.viewmodel

import com.healthtracker.blood.suger.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(private val repository: AlarmRepository) {
}