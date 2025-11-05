package com.healthtracker.blood.suger.ui.viewmodel

import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.manager.HealthServiceManager
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val healthServiceManager: HealthServiceManager,
    private val permissionManager: PermissionManager
) : BaseViewModel() {

    // 服务运行状态
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    init {
        checkServiceStatus()
    }

    private fun checkServiceStatus() {
        _serviceRunning.value = healthServiceManager.isServiceRunning()
    }

    fun startHealthService() {
        if (!_serviceRunning.value && permissionManager.isNotificationPermissionGranted()) {
            healthServiceManager.startHealthService()
            _serviceRunning.value = true
        }
    }
}