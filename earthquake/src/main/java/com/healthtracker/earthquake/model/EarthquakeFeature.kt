package com.healthtracker.earthquake.model
import java.io.Serializable

data class EarthquakeFeature(
    val type: String?,
    val properties: EarthquakeProperties?,
    val geometry: EarthquakeGeometry?,
    val id: String?
) : Serializable