package com.android.common.weather.util

import kotlin.math.roundToInt

/**
 * 温度单位转换工具类
 */
object TemperatureUtils {
    
    /**
     * 华氏度转摄氏度
     * @param fahrenheit 华氏度
     * @return 摄氏度（整数）
     */
    fun fahrenheitToCelsius(fahrenheit: Float): Int {
        return ((fahrenheit - 32) * 5 / 9).roundToInt()
    }
    
    /**
     * 摄氏度转华氏度
     * @param celsius 摄氏度
     * @return 华氏度（整数）
     */
    fun celsiusToFahrenheit(celsius: Float): Int {
        return ((celsius * 9 / 5) + 32).roundToInt()
    }
}

/**
 * Float 扩展函数：华氏度转摄氏度
 */
fun Float.fahrenheitToCelsius(): Int = TemperatureUtils.fahrenheitToCelsius(this)

/**
 * Float 扩展函数：摄氏度转华氏度
 */
fun Float.celsiusToFahrenheit(): Int = TemperatureUtils.celsiusToFahrenheit(this)
