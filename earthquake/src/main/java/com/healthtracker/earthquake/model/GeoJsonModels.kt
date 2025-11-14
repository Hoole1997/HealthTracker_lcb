package com.healthtracker.earthquake.model

/**
 * 简化的 GeoJSON 数据模型，仅包含 features 与 properties。
 */
data class FeatureCollection(
    val features: List<Feature> = emptyList()
)

data class Feature(
    val properties: EarthquakeProperties?
)