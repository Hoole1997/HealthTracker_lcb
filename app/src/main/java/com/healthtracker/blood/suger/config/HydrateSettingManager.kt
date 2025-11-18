package com.healthtracker.blood.suger.config

import com.healthtracker.framework.util.SpUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * 饮水配置管理器
 * 支持用户自定义目标杯数、杯子容积、容积单位。使用MMKV缓存
 */
object HydrateSettingManager {

    private const val KEY_PREFIX = "hydrate_setting_"
    private const val KEY_DAILY_CUPS = KEY_PREFIX + "daily_cups"
    private const val KEY_CUP_VOLUME = KEY_PREFIX + "cup_volume"
    private const val KEY_CUP_UNIT = KEY_PREFIX + "cup_unit"

    private const val DEFAULT_DAILY_CUPS = 8
    private const val DEFAULT_CUP_VOLUME = 100 // 存储基准为 ml，此值表示 20ml
    private val DEFAULT_CUP_UNIT = CupUnit.ML
    private const val ML_PER_FLOZ = 29.5735f

    @Volatile
    private var cacheDailyCups: Int? = null
    @Volatile
    private var cacheCupVolume: Int? = null
    @Volatile
    private var cacheCupUnit: CupUnit? = null

    // 供界面观察的单位流，单位变更后立即发出新值以触发 UI 更新
    private val cupUnitStateFlow: MutableStateFlow<CupUnit> = MutableStateFlow(DEFAULT_CUP_UNIT)
    // 供界面观察的杯子容积流，容积变更后立即发出新值以触发 UI 更新（存储基准为 ml）
    private val cupVolumeStateFlow: MutableStateFlow<Int> = MutableStateFlow(DEFAULT_CUP_VOLUME)

    init {
        // 初始化时同步持久化的单位到流
        cupUnitStateFlow.value = getCupUnit()
        // 初始化时同步持久化的容积到流
        cupVolumeStateFlow.value = getCupVolume()
    }

    /**
     * 杯子单位
     */
    enum class CupUnit { ML, FL_OZ }

    /**
     * 获取每日目标杯数
     */
    fun getDailyCups(): Int {
        cacheDailyCups?.let { return it }
        val value = SpUtils.getInt(KEY_DAILY_CUPS, DEFAULT_DAILY_CUPS)
        cacheDailyCups = value
        return value
    }

    /**
     * 设置每日目标杯数
     */
    fun setDailyCups(cups: Int) {
        val safe = cups.coerceAtLeast(1)
        cacheDailyCups = safe
        SpUtils.putInt(KEY_DAILY_CUPS, safe)
    }

    /**
     * 获取杯子容积（与当前单位对应的数值）
     */
    fun getCupVolume(): Int {
        cacheCupVolume?.let { return it }
        val value = SpUtils.getInt(KEY_CUP_VOLUME, DEFAULT_CUP_VOLUME)
        cacheCupVolume = value
        return value
    }

    /**
     * 设置杯子容积（与当前单位对应的数值）
     */
    fun setCupVolume(volume: Int) {
        val safe = volume.coerceAtLeast(1)
        cacheCupVolume = safe
        SpUtils.putInt(KEY_CUP_VOLUME, safe)
        cupVolumeStateFlow.value = safe
    }

    /**
     * 获取用于界面展示的杯子容积（按当前单位换算）
     */
    fun getCupDisplayVolume(): Int {
        val ml = getCupVolume()
        return fromMl(ml, getCupUnit())
    }

    /**
     * 保存界面展示的杯子容积（同时换算为 ml 存储）
     */
    fun setCupDisplayVolume(value: Int, unit: CupUnit) {
        val ml = toMl(value, unit)
        setCupVolume(ml)
        setCupUnit(unit)
    }

    /**
     * 获取杯子单位
     */
    fun getCupUnit(): CupUnit {
        cacheCupUnit?.let { return it }
        val name = SpUtils.getString(KEY_CUP_UNIT)
        val unit = runCatching { CupUnit.valueOf(name) }.getOrDefault(DEFAULT_CUP_UNIT)
        cacheCupUnit = unit
        return unit
    }

    /**
     * 设置杯子单位
     */
    fun setCupUnit(unit: CupUnit) {
        cacheCupUnit = unit
        SpUtils.putString(KEY_CUP_UNIT, unit.name)
        cupUnitStateFlow.value = unit
    }

    /**
     * 单位变更的观察流（UI 层订阅此流以在单位改变时刷新展示）
     */
    fun cupUnitFlow(): StateFlow<CupUnit> = cupUnitStateFlow

    /**
     * 杯子容积变更的观察流（单位为 ml，用于驱动 UI 的饮水按钮与步进）
     */
    fun cupVolumeFlow(): StateFlow<Int> = cupVolumeStateFlow

    /**
     * 是否已设置过任意饮水配置
     */
    fun hasAnySetting(): Boolean {
        return SpUtils.contain(KEY_DAILY_CUPS) || SpUtils.contain(KEY_CUP_VOLUME) || SpUtils.contain(KEY_CUP_UNIT)
    }

    /**
     * 清除所有饮水配置（用于重置或测试）
     */
    fun clearAll() {
        cacheDailyCups = null
        cacheCupVolume = null
        cacheCupUnit = null
        SpUtils.remove(KEY_DAILY_CUPS)
        SpUtils.remove(KEY_CUP_VOLUME)
        SpUtils.remove(KEY_CUP_UNIT)
    }

    /**
     * 工具：不同单位与 ml 的互转（四舍五入）
     */
    fun toMl(value: Int, unit: CupUnit): Int {
        return when (unit) {
            CupUnit.ML -> value
            CupUnit.FL_OZ -> (value * ML_PER_FLOZ).roundToInt()
        }.coerceAtLeast(1)
    }

    fun fromMl(ml: Int, unit: CupUnit): Int {
        return when (unit) {
            CupUnit.ML -> ml
            CupUnit.FL_OZ -> (ml / ML_PER_FLOZ).roundToInt()
        }.coerceAtLeast(1)
    }

}