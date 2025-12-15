package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.android.common.weather.cache.WeatherCacheManager
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.manager.HealthServiceManager
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.lifecycle.HiltViewModel
import com.android.common.weather.util.TemperaturePreferences
import com.android.common.weather.util.TemperatureUtils
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 天气数据状态
 */
data class WeatherData(
    val temperatureCelsius: Int,  // 原始温度（摄氏度）
    val weatherIconId: Int        // 天气图标 ID
) {
    /**
     * 获取根据当前单位设置转换后的温度
     */
    fun getDisplayTemperature(): Int {
        return if (TemperaturePreferences.isCelsius()) {
            temperatureCelsius
        } else {
            TemperatureUtils.celsiusToFahrenheit(temperatureCelsius.toFloat())
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val healthServiceManager: HealthServiceManager,
    private val permissionManager: PermissionManager
) : BaseViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    // 服务运行状态
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    // 天气数据状态
    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    init {
        checkServiceStatus()
        fetchWeatherData()
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

    /**
     * 获取天气数据（使用缓存管理器）
     * 
     * 加载策略：
     * 1. 缓存有效时直接显示缓存，不发起网络请求
     * 2. 缓存过期时：有 locationKey 按 key 请求，无缓存按 IP 请求
     */
    private fun fetchWeatherData() {
        viewModelScope.launch {
            try {
                "Fetching weather data via cache manager".logd(TAG)
                val response = WeatherCacheManager.getWeatherData()
                
                if (response != null && response.isSuccess()) {
                    // 获取当前天气数据
                    val currentWeather = response.currentConditions?.firstOrNull()
                    
                    if (currentWeather != null) {
                        val temp = currentWeather.temperature?.metric?.value?.roundToInt() ?: 0
                        val iconId = currentWeather.weatherIcon ?: 1
                        
                        _weatherData.value = WeatherData(
                            temperatureCelsius = temp,
                            weatherIconId = iconId
                        )
                        "Weather data updated: temp=$temp, icon=$iconId".logd(TAG)
                    }
                }
            } catch (e: Exception) {
                "Failed to fetch weather data: ${e.message}".loge(TAG)
            }
        }
    }

    /**
     * 刷新天气显示
     * 从缓存中重新读取最新数据并更新 UI
     * 用于温度单位切换后刷新显示，或从天气页面返回时同步数据
     * 
     * 加载策略：
     * 1. 缓存有效时直接显示缓存，不发起网络请求
     * 2. 缓存过期时：有 locationKey 按 key 请求，无缓存按 IP 请求
     */
    fun refreshWeatherDisplay() {
        viewModelScope.launch {
            try {
                "Refreshing weather display".logd(TAG)
                // 使用缓存管理器获取数据（自动判断缓存有效性）
                val response = WeatherCacheManager.getWeatherData()
                
                if (response != null && response.isSuccess()) {
                    val currentWeather = response.currentConditions?.firstOrNull()
                    
                    if (currentWeather != null) {
                        val temp = currentWeather.temperature?.metric?.value?.roundToInt() ?: 0
                        val iconId = currentWeather.weatherIcon ?: 1
                        
                        _weatherData.value = WeatherData(
                            temperatureCelsius = temp,
                            weatherIconId = iconId
                        )
                        "Weather display refreshed: temp=$temp, icon=$iconId".logd(TAG)
                    }
                } else {
                    // 如果获取失败，尝试重新发射当前数据以触发 UI 更新（温度单位切换场景）
                    _weatherData.value?.let { currentData ->
                        _weatherData.value = currentData.copy()
                        "Weather display refreshed (fallback to existing data)".logd(TAG)
                    }
                }
            } catch (e: Exception) {
                "Failed to refresh weather display: ${e.message}".loge(TAG)
                // 出错时回退到重新发射当前数据
                _weatherData.value?.let { currentData ->
                    _weatherData.value = currentData.copy()
                }
            }
        }
    }
}