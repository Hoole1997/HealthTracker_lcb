package com.android.common.weather.util

import com.android.common.weather.model.CityInfo
import com.android.common.weather.model.CurrentConditionResponse
import com.android.common.weather.model.DayForecasts
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * 自定义 TypeAdapter 用于处理字符串格式的 JSON 字段
 * 
 * 后端 API 返回的某些字段（如 CurrentConditions、DailyForecasts）
 * 是将 JSON 对象序列化为字符串后返回的，需要二次反序列化
 */

/**
 * CurrentConditions 字段的自定义反序列化器
 * 将字符串 "[{...}]" 解析为 List<CurrentConditionResponse>
 */
class CurrentConditionsTypeAdapter : TypeAdapter<List<CurrentConditionResponse>?>() {
    
    private val gson = Gson()
    private val listType = object : TypeToken<List<CurrentConditionResponse>>() {}.type
    
    override fun write(out: JsonWriter, value: List<CurrentConditionResponse>?) {
        if (value == null) {
            out.nullValue()
        } else {
            // 将对象序列化为 JSON 字符串
            out.value(gson.toJson(value, listType))
        }
    }
    
    override fun read(reader: JsonReader): List<CurrentConditionResponse>? {
        // 检查是否为 null
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        
        // 读取字符串值
        val jsonString = reader.nextString()
        
        // 如果是空字符串，返回 null
        if (jsonString.isBlank()) {
            return null
        }
        
        // 将字符串解析为对象列表
        return gson.fromJson(jsonString, listType)
    }
}

/**
 * DailyForecasts 字段的自定义反序列化器
 * 将字符串 "{...}" 解析为 DayForecasts 对象
 */
class DayForecastsTypeAdapter : TypeAdapter<DayForecasts?>() {
    
    private val gson = Gson()
    
    override fun write(out: JsonWriter, value: DayForecasts?) {
        if (value == null) {
            out.nullValue()
        } else {
            // 将对象序列化为 JSON 字符串
            out.value(gson.toJson(value, DayForecasts::class.java))
        }
    }
    
    override fun read(reader: JsonReader): DayForecasts? {
        // 检查是否为 null
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        
        // 读取字符串值
        val jsonString = reader.nextString()
        
        // 如果是空字符串，返回 null
        if (jsonString.isBlank()) {
            return null
        }
        
        // 将字符串解析为对象
        return gson.fromJson(jsonString, DayForecasts::class.java)
    }
}

/**
 * City 字段的自定义反序列化器
 * 将字符串 "{...}" 解析为 CityInfo 对象
 */
class CityInfoTypeAdapter : TypeAdapter<CityInfo?>() {
    
    private val gson = Gson()
    
    override fun write(out: JsonWriter, value: CityInfo?) {
        if (value == null) {
            out.nullValue()
        } else {
            // 将对象序列化为 JSON 字符串
            out.value(gson.toJson(value, CityInfo::class.java))
        }
    }
    
    override fun read(reader: JsonReader): CityInfo? {
        // 检查是否为 null
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        
        // 读取字符串值
        val jsonString = reader.nextString()
        
        // 如果是空字符串，返回 null
        if (jsonString.isBlank()) {
            return null
        }
        
        // 将字符串解析为对象
        return gson.fromJson(jsonString, CityInfo::class.java)
    }
}

/**
 * Locations 字段的自定义反序列化器
 * 将字符串 "[{...}]" 解析为 List<CityInfo>
 */
class LocationsTypeAdapter : TypeAdapter<List<CityInfo>?>() {
    
    private val gson = Gson()
    private val listType = object : TypeToken<List<CityInfo>>() {}.type
    
    override fun write(out: JsonWriter, value: List<CityInfo>?) {
        if (value == null) {
            out.nullValue()
        } else {
            // 将对象序列化为 JSON 字符串
            out.value(gson.toJson(value, listType))
        }
    }
    
    override fun read(reader: JsonReader): List<CityInfo>? {
        // 检查是否为 null
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        
        // 读取字符串值
        val jsonString = reader.nextString()
        
        // 如果是空字符串，返回 null
        if (jsonString.isBlank()) {
            return null
        }
        
        // 将字符串解析为对象列表
        return gson.fromJson(jsonString, listType)
    }
}
