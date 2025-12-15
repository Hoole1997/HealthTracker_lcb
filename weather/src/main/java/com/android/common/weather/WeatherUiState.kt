package com.android.common.weather

/**
 * 天气 UI 状态
 * 用于管理天气数据的加载、成功和失败状态
 */
sealed class WeatherUiState {
    
    /** 加载中状态 */
    object Loading : WeatherUiState()
    
    /**
     * 成功状态
     * @param currentWeather 当前天气数据
     * @param dailyForecasts 未来 5 天预报列表
     * @param headline 天气总览信息
     */
    data class Success(
        val currentWeather: com.android.common.weather.model.CurrentConditionResponse?,
        val dailyForecasts: List<com.android.common.weather.model.AccuWeatherDailyForecast>?,
        val headline: com.android.common.weather.model.Headline?,
        val cityInfo: com.android.common.weather.model.CityInfo?
    ) : WeatherUiState()
    
    /**
     * 错误状态
     * @param message 错误信息
     */
    data class Error(val message: String) : WeatherUiState()
}
