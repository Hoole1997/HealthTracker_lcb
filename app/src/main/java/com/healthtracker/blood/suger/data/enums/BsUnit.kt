package com.healthtracker.blood.suger.data.enums

import com.healthtracker.framework.util.SpUtils
import kotlin.math.roundToInt

/**
 * 统一的血糖单位枚举类
 * 整合了数据存储、UI显示和用户偏好管理功能
 *
 * @param value 数据库存储值
 * @param displayName 显示名称
 * @param minValue 最小值
 * @param maxValue 最大值
 * @param scrollableMinValue 可滚动最小值
 * @param scrollableMaxValue 可滚动最大值
 * @param step 步长
 * @param decimalPlaces 小数位数
 * @param scaleCount 刻度数量
 */
enum class BsUnit(
    val value: Int,
    val displayName: String,
    val minValue: Float,
    val maxValue: Float,
    val scrollableMinValue: Float,
    val scrollableMaxValue: Float,
    val step: Float,
    val decimalPlaces: Int,
    val scaleCount: Int
) {
    /**
     * mg/dL 单位
     */
    MG_DL(
        value = 0,
        displayName = "mg/dL",
        minValue = 0f,
        maxValue = 700f,
        scrollableMinValue = 18f,
        scrollableMaxValue = 630f,
        step = 0.1f,
        decimalPlaces = 1,
        scaleCount = 10
    ),

    /**
     * mmol/L 单位
     */
    MMOL_L(
        value = 1,
        displayName = "mmol/L",
        minValue = 0f,
        maxValue = 37f,
        scrollableMinValue = 1f,
        scrollableMaxValue = 35f,
        step = 0.1f,
        decimalPlaces = 1,
        scaleCount = 5
    );

    companion object {
        /**
         * mg/dL 转 mmol/L 的转换系数
         * 使用更精确的转换系数
         */
        const val CONVERSION_FACTOR = 18.0182

        /**
         * 用户偏好单位存储键
         */
        private const val KEY_PREFERRED_UNIT = "blood_sugar_preferred_unit"

        /**
         * 根据数据库值获取枚举
         * @param value 数据库存储的整数值
         * @return 对应的枚举，默认为 MG_DL
         */
        fun fromValue(value: Int): BsUnit {
            return entries.find { it.value == value } ?: MG_DL
        }

        /**
         * 从 mmol/L 转换为 mg/dL
         */
        fun mmolToMgdl(mmolValue: Float): Float {
            return mmolValue * CONVERSION_FACTOR.toFloat()
        }

        /**
         * 从 mg/dL 转换为 mmol/L
         */
        fun mgdlToMmol(mgdlValue: Float): Float {
            return mgdlValue / CONVERSION_FACTOR.toFloat()
        }

        /**
         * 在两个单位之间转换数值
         */
        fun convertValue(value: Float, fromUnit: BsUnit, toUnit: BsUnit): Float {
            return when {
                fromUnit == toUnit -> value
                fromUnit == MMOL_L && toUnit == MG_DL -> {
                    // mmol/L -> mg/dL，保持一位小数
                    (mmolToMgdl(value) * 10).roundToInt() / 10f
                }
                fromUnit == MG_DL && toUnit == MMOL_L -> {
                    // mg/dL -> mmol/L，保持一位小数
                    (mgdlToMmol(value) * 10).roundToInt() / 10f
                }
                else -> value
            }
        }

        /**
         * 格式化显示值
         */
        fun formatValue(value: Float, unit: BsUnit): String {
            return when (unit.decimalPlaces) {
                0 -> value.roundToInt().toString()
                else -> String.format("%.${unit.decimalPlaces}f", value)
            }
        }

        /**
         * 获取刻度配置
         */
        fun getScaleConfig(unit: BsUnit): ScaleConfig {
            return ScaleConfig(
                minScale = unit.minValue,
                maxScale = unit.maxValue,
                scrollableMinScale = unit.scrollableMinValue,
                scrollableMaxScale = unit.scrollableMaxValue,
                scaleStep = unit.step,
                decimalPlaces = unit.decimalPlaces,
                scaleCount = unit.scaleCount
            )
        }

        /**
         * 保存用户偏好的血糖单位
         */
        fun savePreferredUnit(unit: BsUnit) {
            SpUtils.putString(KEY_PREFERRED_UNIT, unit.name)
        }

        /**
         * 获取用户偏好的血糖单位
         * 如果未设置偏好，默认返回 MMOL_L
         */
        fun getPreferredUnit(): BsUnit {
            val savedUnitName = SpUtils.getString(KEY_PREFERRED_UNIT)
            return if (savedUnitName.isNotEmpty()) {
                try {
                    valueOf(savedUnitName)
                } catch (e: IllegalArgumentException) {
                    // 如果保存的单位名称无效，返回默认值
                    MMOL_L
                }
            } else {
                MMOL_L  // 默认使用 mmol/L
            }
        }

        /**
         * 检查是否已设置用户偏好
         */
        fun hasPreferredUnit(): Boolean {
            return SpUtils.contain(KEY_PREFERRED_UNIT)
        }

        /**
         * 清除用户偏好设置
         */
        fun clearPreferredUnit() {
            SpUtils.remove(KEY_PREFERRED_UNIT)
        }
    }

    /**
     * 将 mg/dL 值转换为当前单位的值
     * @param mgdlValue mg/dL 单位的血糖值
     * @return 转换后的血糖值
     */
    fun convertFromMgdl(mgdlValue: Double): Double {
        return when (this) {
            MG_DL -> mgdlValue
            MMOL_L -> mgdlValue / CONVERSION_FACTOR
        }
    }

    /**
     * 将当前单位的值转换为 mg/dL 值
     * @param value 当前单位的血糖值
     * @return mg/dL 单位的血糖值
     */
    fun convertToMgdl(value: Double): Double {
        return when (this) {
            MG_DL -> value
            MMOL_L -> value * CONVERSION_FACTOR
        }
    }

    /**
     * 刻度配置数据类
     */
    data class ScaleConfig(
        val minScale: Float,
        val maxScale: Float,
        val scrollableMinScale: Float,
        val scrollableMaxScale: Float,
        val scaleStep: Float,
        val decimalPlaces: Int,
        val scaleCount: Int
    )
}