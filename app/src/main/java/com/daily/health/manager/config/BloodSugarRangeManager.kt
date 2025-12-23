package com.daily.health.manager.config

import com.google.gson.Gson
import com.daily.health.manager.data.enums.BloodSugarRanges
import com.daily.health.manager.data.enums.BloodSugarStatus
import com.daily.health.manager.data.enums.BsUnit
import com.healthtracker.framework.util.SpUtils

/**
 * 血糖范围配置管理器
 * 支持用户自定义边界值，使用MMKV缓存
 */
object BloodSugarRangeManager {

    private const val KEY_PREFIX = "blood_sugar_ranges_"
    private val gson = Gson()
    private val rangeCache = mutableMapOf<String, BloodSugarRanges>()

    /**
     * 获取指定状态和单位的血糖范围
     * 优先返回用户自定义值，否则返回默认值
     */
    fun getCustomRangesForStatus(status: BloodSugarStatus, unit: BsUnit): BloodSugarRanges? {
        val cacheKey = getCacheKey(status.statusType, unit)

        // 先检查内存缓存
        rangeCache[cacheKey]?.let { return it }

        // 尝试从MMKV读取用户自定义值
        val customRanges = loadCustomRanges(status.statusType, unit)
        if (customRanges != null) {
            rangeCache[cacheKey] = customRanges
            return customRanges
        }

        return null
    }

    /**
     * 兼容性方法：获取指定状态的血糖范围（mg/dL单位）
     * @deprecated 使用 getCustomRangesForStatus(status, unit) 代替
     */
    @Deprecated("Use getCustomRangesForStatus(status, unit) instead")
    fun getRangesForStatus(status: BloodSugarStatus): BloodSugarRanges {
        return getCustomRangesForStatus(status, BsUnit.MG_DL) ?: status.defaultMgdlRanges
    }

    /**
     * 更新指定状态和单位的自定义血糖范围
     */
    fun updateCustomRanges(statusType: Int, unit: BsUnit, ranges: BloodSugarRanges) {
        val cacheKey = getCacheKey(statusType, unit)

        // 更新内存缓存
        rangeCache[cacheKey] = ranges

        // 保存到MMKV
        saveCustomRanges(statusType, unit, ranges)
    }

    /**
     * 检查指定状态和单位是否有自定义范围
     */
    fun hasCustomRanges(statusType: Int, unit: BsUnit): Boolean {
        return SpUtils.contain(getKey(statusType, unit))
    }

    /**
     * 重置指定状态和单位为默认值
     */
    fun resetToDefault(statusType: Int, unit: BsUnit) {
        val cacheKey = getCacheKey(statusType, unit)

        // 清除内存缓存
        rangeCache.remove(cacheKey)

        // 删除MMKV中的自定义值
        SpUtils.remove(getKey(statusType, unit))
    }

    /**
     * 清除所有缓存（用于测试或重置）
     */
    fun clearAllCache() {
        rangeCache.clear()
        BloodSugarStatus.entries.forEach { status ->
            BsUnit.entries.forEach { unit ->
                SpUtils.remove(getKey(status.statusType, unit))
            }
        }
    }

    /**
     * 从MMKV加载自定义范围
     */
    private fun loadCustomRanges(statusType: Int, unit: BsUnit): BloodSugarRanges? {
        return try {
            val json = SpUtils.getString(getKey(statusType, unit))
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
    private fun saveCustomRanges(statusType: Int, unit: BsUnit, ranges: BloodSugarRanges) {
        try {
            val json = gson.toJson(ranges)
            SpUtils.putString(getKey(statusType, unit), json)
        } catch (e: Exception) {
            // 序列化失败，忽略
        }
    }

    /**
     * 生成存储key
     */
    private fun getKey(statusType: Int, unit: BsUnit): String {
        return "${KEY_PREFIX}${statusType}_${unit.name}"
    }

    /**
     * 生成缓存key
     */
    private fun getCacheKey(statusType: Int, unit: BsUnit): String {
        return "${statusType}_${unit.name}"
    }
}