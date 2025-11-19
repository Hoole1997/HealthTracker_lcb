package com.healthtracker.earthquake.model
import java.io.Serializable

/**
 * 对应 USGS GeoJSON features[0].properties。
 * 字段均可为空，避免解析失败。
 */
data class EarthquakeProperties(
    val mag: Double?,
    val place: String?,
    val time: Long?,
    val updated: Long?,
    val tz: Int?,
    val url: String?,
    val detail: String?,
    val felt: Int?,
    val cdi: Double?,
    val mmi: Double?,
    val alert: String?,
    val status: String?,
    val tsunami: Int?,
    val sig: Int?,
    val net: String?,
    val code: String?,
    val ids: String?,
    val sources: String?,
    val types: String?,
    val nst: Int?,
    val dmin: Double?,
    val rms: Double?,
    val gap: Int?,
    val magType: String?,
    val type: String?,
    val title: String?
) : Serializable