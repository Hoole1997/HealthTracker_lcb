package com.android.common.weather.model

import com.android.common.weather.util.CityInfoTypeAdapter
import com.android.common.weather.util.CurrentConditionsTypeAdapter
import com.android.common.weather.util.DayForecastsTypeAdapter
import com.android.common.weather.util.LocationsTypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * 天气查询接口返回的顶层数据结构
 */
data class WeatherResponse(

    /** 接口状态码，0 表示成功，非 0 表示失败 */
    val code: Int,

    /** 位置唯一标识（locationKey），后续可以用它做其他类型查询 */
    @SerializedName("LocationKey")
    val locationKey: String?,

    /**
     * 位置列表
     * API 返回格式：字符串包含数组 `"[{...}]"`
     * 使用自定义 TypeAdapter 自动将字符串解析为对象列表
     */
    @SerializedName("Locations")
    @JsonAdapter(LocationsTypeAdapter::class)
    val locations: List<CityInfo>?,

    /**
     * 当前天气状况列表
     * API 返回格式：字符串包含数组 `"[{...}]"`
     * 使用自定义 TypeAdapter 自动将字符串解析为对象列表
     */
    @SerializedName("CurrentConditions")
    @JsonAdapter(CurrentConditionsTypeAdapter::class)
    val currentConditions: List<CurrentConditionResponse>?,

    /**
     * 未来多天预报
     * API 返回格式：字符串包含对象 `"{...}"`
     * 使用自定义 TypeAdapter 自动将字符串解析为对象
     */
    @SerializedName("DailyForecasts")
    @JsonAdapter(DayForecastsTypeAdapter::class)
    val dailyForecasts: DayForecasts?,
    /**
     * 未来多天预报
     * API 返回格式：字符串包含对象 `"{...}"`
     * 使用自定义 TypeAdapter 自动将字符串解析为对象
     */
    @SerializedName("City")
    @JsonAdapter(CityInfoTypeAdapter::class)
    val city: CityInfo?


) {
    /**
     * 判断请求是否成功
     * @return true 表示成功（code=0 或 code=3），false 表示失败
     */
    fun isSuccess(): Boolean = code == 0 || code == 3
}


