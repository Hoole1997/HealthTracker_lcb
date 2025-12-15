package com.android.common.weather.util

import com.healthtracker.framework.util.SpUtils

/**
 * Temperature unit preferences manager
 */
object TemperaturePreferences {

    private const val KEY_TEMPERATURE_UNIT = "key_temperature_unit"

    const val UNIT_CELSIUS = 0
    const val UNIT_FAHRENHEIT = 1

    /**
     * Save temperature unit
     * @param unit 0: Celsius, 1: Fahrenheit
     */
    fun saveUnit(unit: Int) {
        SpUtils.putInt(KEY_TEMPERATURE_UNIT, unit)
    }

    /**
     * Get temperature unit
     * @return 0: Celsius (default), 1: Fahrenheit
     */
    fun getUnit(): Int {
        return SpUtils.getInt(KEY_TEMPERATURE_UNIT, UNIT_CELSIUS)
    }
    
    /**
     * Check if current unit is Celsius
     */
    fun isCelsius(): Boolean {
        return getUnit() == UNIT_CELSIUS
    }
}
