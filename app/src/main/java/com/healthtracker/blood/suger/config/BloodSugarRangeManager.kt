package com.healthtracker.blood.suger.config

import com.google.gson.Gson
import com.healthtracker.blood.suger.enum.BloodSugarRanges
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.framework.util.SpUtils

/**
 * 血糖范围配置管理器
 * 支持用户自定义边界值，使用MMKV缓存
 */
object BloodSugarRangeManager {

    private const val KEY_PREFIX = "blood_sugar_ranges_"
    private val gson = Gson()
    private val rangeCache = mutableMapOf<Int, BloodSugarRanges>()

    /**
     * 获取指定状态的血糖范围
     * 优先返回用户自定义值，否则返回默认值
     */
    fun getRangesForStatus(status: BloodSugarStatus): BloodSugarRanges {
        val statusType = status.statusType

        // 先检查内存缓存
        rangeCache[statusType]?.let { return it }

        // 尝试从MMKV读取用户自定义值
        val customRanges = loadCustomRanges(statusType)
        if (customRanges != null) {
            rangeCache[statusType] = customRanges
            return customRanges
        }

        // 使用默认值
        val defaultRanges = status.defaultMgdlRanges
        rangeCache[statusType] = defaultRanges
        return defaultRanges
    }

    /**
     * 更新指定状态的自定义血糖范围
     */
    fun updateCustomRanges(statusType: Int, ranges: BloodSugarRanges) {
        // 更新内存缓存
        rangeCache[statusType] = ranges

        // 保存到MMKV
        saveCustomRanges(statusType, ranges)
    }

    /**
     * 检查指定状态是否有自定义范围
     */
    fun hasCustomRanges(statusType: Int): Boolean {
        return SpUtils.contain(getKey(statusType))
    }

    /**
     * 重置指定状态为默认值
     */
    fun resetToDefault(statusType: Int) {
        // 清除内存缓存
        rangeCache.remove(statusType)

        // 删除MMKV中的自定义值
        SpUtils.remove(getKey(statusType))
    }

    /**
     * 清除所有缓存（用于测试或重置）
     */
    fun clearAllCache() {
        rangeCache.clear()
        BloodSugarStatus.entries.forEach { status ->
            SpUtils.remove(getKey(status.statusType))
        }
    }

    /**
     * 从MMKV加载自定义范围
     */
    private fun loadCustomRanges(statusType: Int): BloodSugarRanges? {
        return try {
            val json = SpUtils.getString(getKey(statusType))
            if (json.isNotEmpty()) {
                gson.fromJson(json, BloodSugarRanges::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            // JSON解析失败，返回null使用默认值
            null
        }
    }

    /**
     * 保存自定义范围到MMKV
     */
    private fun saveCustomRanges(statusType: Int, ranges: BloodSugarRanges) {
        try {
            val json = gson.toJson(ranges)
            SpUtils.putString(getKey(statusType), json)
        } catch (e: Exception) {
            // 序列化失败，忽略
        }
    }

    /**
     * 生成存储key
     */
    private fun getKey(statusType: Int): String {
        return "$KEY_PREFIX$statusType"
    }
}