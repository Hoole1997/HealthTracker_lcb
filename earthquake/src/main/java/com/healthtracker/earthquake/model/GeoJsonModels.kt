package com.healthtracker.earthquake.model
import java.io.Serializable

/**
 * 简化的 GeoJSON 数据模型，仅包含 features 与 properties。
 */
data class FeatureCollection(
    val features: List<Feature> = emptyList()
) : Serializable

data class Feature(
    val properties: EarthquakeProperties?
) : Serializable