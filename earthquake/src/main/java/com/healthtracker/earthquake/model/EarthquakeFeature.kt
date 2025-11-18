package com.healthtracker.earthquake.model

data class EarthquakeFeature(
    val type: String?,
    val properties: EarthquakeProperties?,
    val geometry: EarthquakeGeometry?,
    val id: String?
)