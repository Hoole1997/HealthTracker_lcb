package com.healthtracker.earthquake.model
import java.io.Serializable

data class EarthquakeGeometry(
    val type: String?,
    val coordinates: List<Double>?
) : Serializable