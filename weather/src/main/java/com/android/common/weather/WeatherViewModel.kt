package com.android.common.weather

import androidx.lifecycle.viewModelScope
import com.android.common.weather.cache.WeatherCacheManager
import com.android.common.weather.model.CityInfo
import com.android.common.weather.model.WeatherResponse
import com.android.common.weather.network.RetrofitClient
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 天气页面 ViewModel
 * 负责天气数据的请求和状态管理
 * 
 * 加载策略：
 * 1. 缓存有效时直接显示缓存，不发起网络请求
 * 2. 缓存过期时：有 locationKey 按 key 请求，无缓存按 IP 请求
 * 3. 搜索弹窗选中城市时使旧缓存过期，然后按 locationKey 请求
 */
class WeatherViewModel : BaseViewModel() {
    
    companion object {
        private const val TAG = "WeatherViewModel"
    }

    // UI 状态 Flow
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
    
    // 搜索结果 Flow
    private val _searchResult = MutableStateFlow<WeatherResponse?>(null)
    val searchResult: StateFlow<WeatherResponse?> = _searchResult.asStateFlow()
    
    // 当前显示的 locationKey
    private var currentLocationKey: String? = null

    init {
        requestWeather()
    }

    /**
     * 请求天气数据
     * 使用缓存管理器获取天气信息（自动判断缓存有效性）
     * @param forceRefresh 是否强制刷新（忽略缓存）
     */
    fun requestWeather(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WeatherUiState.Loading
            
            try {
                "Requesting weather data (forceRefresh=$forceRefresh)...".logd(TAG)
                val response = WeatherCacheManager.getWeatherData(forceRefresh)
                
                if (response != null && response.isSuccess()) {
                    currentLocationKey = response.locationKey
                    updateUiState(response)
                } else {
                    val errorMsg = "Failed to get weather data"
                    errorMsg.loge(TAG)
                    _uiState.value = WeatherUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Weather request failed: ${e.message}"
                errorMsg.loge(TAG)
                e.printStackTrace()
                _uiState.value = WeatherUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 通过地址搜索天气
     * @param address 地址关键词
     */
    fun searchWeatherByAddress(address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                "Searching weather by address: $address".logd(TAG)
                val response = RetrofitClient.weatherApi.getWeatherByAddress(address = address)
                
                if (response.isSuccess()) {
                    "Search result: locationKey=${response.locationKey}, city=${response.city?.localizedName}".logd(TAG)
                    _searchResult.value = response
                } else {
                    "Search failed: code=${response.code}".loge(TAG)
                    _searchResult.value = null
                }
            } catch (e: Exception) {
                "Search failed: ${e.message}".loge(TAG)
                e.printStackTrace()
                _searchResult.value = null
            }
        }
    }
    
    /**
     * 通过 IP 刷新天气
     * 使旧缓存过期，然后按 IP 请求新数据
     */
    fun refreshWeatherByIP() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WeatherUiState.Loading
            try {
                "Refreshing weather by IP...".logd(TAG)
                
                // 使旧缓存过期
                WeatherCacheManager.invalidateCache()
                
                val response = RetrofitClient.weatherApi.getWeatherByIP()
                
                if (response.isSuccess()) {
                    val newLocationKey = response.locationKey
                    "IP refresh result: locationKey=$newLocationKey".logd(TAG)
                    
                    currentLocationKey = newLocationKey
                    // 保存到缓存
                    WeatherCacheManager.saveCache(response)
                    // 更新 UI
                    updateUiState(response)
                } else {
                    "IP refresh failed: code=${response.code}".loge(TAG)
                    _uiState.value = WeatherUiState.Error("IP refresh failed: ${response.code}")
                }
            } catch (e: Exception) {
                "IP refresh failed: ${e.message}".loge(TAG)
                e.printStackTrace()
                _uiState.value = WeatherUiState.Error(e.message ?: "Unknown IP refresh error")
            }
        }
    }
    
    /**
     * 更新 UI 状态
     * @param response API 响应数据
     * @param fallbackCityInfo 备用城市信息（当 response.city 为空时使用）
     */
    private fun updateUiState(response: WeatherResponse, fallbackCityInfo: CityInfo? = null) {
        val currentWeather = response.currentConditions?.firstOrNull()
        val forecasts = response.dailyForecasts?.dailyForecasts
        val headline = response.dailyForecasts?.headline
        // 优先使用 API 返回的 city，若为空则使用备用 cityInfo
        val cityInfo = response.city ?: fallbackCityInfo
        
        "Weather data loaded successfully".logd(TAG)
        "Current weather: ${currentWeather?.weatherText}".logd(TAG)
        "Forecasts count: ${forecasts?.size}".logd(TAG)
        "City: ${cityInfo?.localizedName}".logd(TAG)
        
        _uiState.value = WeatherUiState.Success(
            currentWeather = currentWeather,
            dailyForecasts = forecasts,
            headline = headline,
            cityInfo = cityInfo
        )
    }
    
    /**
     * 通过 LocationKey 获取天气
     * 使旧缓存过期，然后按 locationKey 请求新数据
     * 
     * @param locationKey 城市的唯一标识
     * @param cityInfo 选中城市的信息（用于备用，因为 API 可能不返回 city 信息）
     */
    fun getWeatherByLocationKey(locationKey: String, cityInfo: CityInfo? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WeatherUiState.Loading
            
            try {
                "Getting weather by locationKey: $locationKey".logd(TAG)
                
                // 使旧缓存过期
                WeatherCacheManager.invalidateCache()
                
                val response = RetrofitClient.weatherApi.getWeatherByKey(locationKey = locationKey)
                
                if (response.isSuccess()) {
                    currentLocationKey = response.locationKey
                    
                    // 如果 API 返回的 response 中没有 city 信息，但我们有传入的 cityInfo，则合并它
                    // 这样保存到缓存的数据就会包含城市名，下次从缓存加载时就能显示
                    val responseToCache = if (response.city == null && cityInfo != null) {
                        response.copy(city = cityInfo)
                    } else {
                        response
                    }
                    
                    // 保存到缓存
                    WeatherCacheManager.saveCache(responseToCache)
                    // 更新 UI（传入备用 cityInfo）
                    updateUiState(responseToCache, cityInfo)
                } else {
                    val errorMsg = "Failed to get weather by key: code=${response.code}"
                    errorMsg.loge(TAG)
                    _uiState.value = WeatherUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Weather request failed: ${e.message}"
                errorMsg.loge(TAG)
                e.printStackTrace()
                _uiState.value = WeatherUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}