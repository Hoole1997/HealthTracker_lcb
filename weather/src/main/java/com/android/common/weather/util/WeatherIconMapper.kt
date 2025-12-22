package com.android.common.weather.util

import com.android.common.weather.R

/**
 * 天气图标映射工具
 * 
 * 根据 AccuWeather API 返回的图标 ID（1-44）映射到本地图标资源
 * 
 * AccuWeather 图标文档：
 * https://developer.accuweather.com/documentation/weather-icons
 */
object WeatherIconMapper {
    
    /**
     * 根据图标 ID 获取对应的 Drawable 资源 ID
     * 
     * @param iconId AccuWeather 图标 ID (1-44)
     * @return Drawable 资源 ID
     */
    fun getIconResource(iconId: Int?): Int {
        return when (iconId) {
            // 白天晴朗天气 (1-5)
            1 -> R.drawable.ic_sunny           // Sunny - 晴天
            2 -> R.drawable.ic_mostly_sunny    // Mostly Sunny - 大部分晴
            3 -> R.drawable.ic_partly_sunny    // Partly Sunny - 部分晴
            4 -> R.drawable.ic_intermittent_clouds  // Intermittent Clouds - 间歇性多云
            5 -> R.drawable.ic_hazy_sunshine   // Hazy Sunshine - 朦胧阳光
            
            // 多云天气 (6-8)
            6 -> R.drawable.ic_mostly_cloudy   // Mostly Cloudy - 大部分多云
            7 -> R.drawable.ic_cloudy          // Cloudy - 多云
            8 -> R.drawable.ic_dreary          // Dreary (Overcast) - 阴天
            
            // 雾 (11)
            11 -> R.drawable.ic_fog            // Fog - 雾
            
            // 降雨天气 (12-14, 18)
            12 -> R.drawable.ic_showers        // Showers - 阵雨
            13 -> R.drawable.ic_mostly_cloudy_with_showers  // Mostly Cloudy w/ Showers
            14 -> R.drawable.ic_partly_sunny_with_showers   // Partly Sunny w/ Showers

            
            // 雷暴天气 (15-17)
            15 -> R.drawable.ic_t_storms       // T-Storms - 雷暴
            16 -> R.drawable.ic_mostly_cloudy_with_t_storms  // Mostly Cloudy w/ T-Storms
            17 -> R.drawable.ic_partly_sunny_with_t_storms   // Partly Sunny w/ T-Storms
            18 -> R.drawable.ic_rain           // Rain - 雨
            
            // 降雪天气 (19-23)
            19 -> R.drawable.ic_flurries       // Flurries - 小雪
            20 -> R.drawable.ic_mostly_cloudy_with_flurries  // Mostly Cloudy w/ Flurries
            21 -> R.drawable.ic_partly_sunny_with_flurries   // Partly Sunny w/ Flurries
            22 -> R.drawable.ic_snow           // Snow - 雪
            23 -> R.drawable.ic_mostly_cloudy_with_snow     // Mostly Cloudy w/ Snow
            
            // 冰冻天气 (24-26, 29)
            24 -> R.drawable.ic_ice            // Ice - 冰
            25 -> R.drawable.ic_sleet          // Sleet - 雨夹雪
            26 -> R.drawable.ic_freezing_rain  // Freezing Rain - 冻雨
            29 -> R.drawable.ic_rain_and_snow  // Rain and Snow - 雨雪混合
            
            // 极端天气 (30-32)
            30 -> R.drawable.ic_hot            // Hot - 炎热
            31 -> R.drawable.ic_cold           // Cold - 寒冷
            32 -> R.drawable.ic_windy          // Windy - 大风
            
            // 夜间天气 (33-44)
            33 -> R.drawable.ic_clear_night    // Clear - 晴朗夜晚
            34 -> R.drawable.ic_mostly_clear_night  // Mostly Clear - 大部分晴朗
            35 -> R.drawable.ic_partly_cloudy_night // Partly Cloudy - 部分多云
            36 -> R.drawable.ic_intermittent_clouds_night  // Intermittent Clouds
            37 -> R.drawable.ic_hazy_moonlight // Hazy Moonlight - 朦胧月光
            38 -> R.drawable.ic_mostly_cloudy_night  // Mostly Cloudy
            39 -> R.drawable.ic_partly_cloudy_with_showers_night  // Partly Cloudy w/ Showers
            40 -> R.drawable.ic_mostly_cloudy_with_showers_night  // Mostly Cloudy w/ Showers
            41 -> R.drawable.ic_partly_cloudy_with_t_storms_night  // Partly Cloudy w/ T-Storms
            42 -> R.drawable.ic_mostly_cloudy_with_t_storms_night  // Mostly Cloudy w/ T-Storms
            43 -> R.drawable.ic_mostly_cloudy_with_flurries_night  // Mostly Cloudy w/ Flurries
            44 -> R.drawable.ic_mostly_cloudy_with_snow_night      // Mostly Cloudy w/ Snow
            
            // 默认图标（未知或无效 ID）
            else -> R.drawable.ic_sunny
        }
    }
    
    /**
     * 获取图标描述文字资源 ID
     * 
     * @param iconId AccuWeather 图标 ID (1-44)
     * @return 字符串资源 ID
     */
    fun getIconDescriptionRes(iconId: Int?): Int {
        return when (iconId) {
            1 -> R.string.ht_weather_sunny
            2 -> R.string.ht_weather_mostly_sunny
            3 -> R.string.ht_weather_partly_sunny
            4 -> R.string.ht_weather_intermittent_clouds
            5 -> R.string.ht_weather_hazy_sunshine
            6 -> R.string.ht_weather_mostly_cloudy
            7 -> R.string.ht_weather_cloudy
            8 -> R.string.ht_weather_overcast
            11 -> R.string.ht_weather_fog
            12 -> R.string.ht_weather_showers
            13 -> R.string.ht_weather_mostly_cloudy_w_showers
            14 -> R.string.ht_weather_partly_sunny_w_showers
            15 -> R.string.ht_weather_t_storms
            16 -> R.string.ht_weather_mostly_cloudy_w_t_storms
            17 -> R.string.ht_weather_partly_sunny_w_t_storms
            18 -> R.string.ht_weather_rain
            19 -> R.string.ht_weather_flurries
            20 -> R.string.ht_weather_mostly_cloudy_w_flurries
            21 -> R.string.ht_weather_partly_sunny_w_flurries
            22 -> R.string.ht_weather_snow
            23 -> R.string.ht_weather_mostly_cloudy_w_snow
            24 -> R.string.ht_weather_ice
            25 -> R.string.ht_weather_sleet
            26 -> R.string.ht_weather_freezing_rain
            29 -> R.string.ht_weather_rain_and_snow
            30 -> R.string.ht_weather_hot
            31 -> R.string.ht_weather_cold
            32 -> R.string.ht_weather_windy
            33 -> R.string.ht_weather_clear
            34 -> R.string.ht_weather_mostly_clear
            35 -> R.string.ht_weather_partly_cloudy
            36 -> R.string.ht_weather_intermittent_clouds
            37 -> R.string.ht_weather_hazy_moonlight
            38 -> R.string.ht_weather_mostly_cloudy
            39 -> R.string.ht_weather_partly_cloudy_w_showers
            40 -> R.string.ht_weather_mostly_cloudy_w_showers
            41 -> R.string.ht_weather_partly_cloudy_w_t_storms
            42 -> R.string.ht_weather_mostly_cloudy_w_t_storms
            43 -> R.string.ht_weather_mostly_cloudy_w_flurries
            44 -> R.string.ht_weather_mostly_cloudy_w_snow
            else -> R.string.ht_weather_unknown
        }
    }
}
