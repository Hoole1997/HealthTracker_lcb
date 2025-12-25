package com.daily.health.manager.face.viewmodel

import com.daily.health.manager.alarm.PermissionManager
import com.daily.health.manager.manager.HealthServiceManager
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(
    private val healthServiceManager: HealthServiceManager,
    private val permissionManager: PermissionManager
) : BaseViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

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