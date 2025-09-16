package com.healthtracker.blood.suger.data.enums

/**
 * 血糖单位枚举
 *
 * @param value 数据库存储值
 * @param symbol 单位符号
 * @param name 单位名称
 */
enum class GlucoseUnit(
    val value: Int
) {
    /**
     * mg/dL 单位
     */
    MG_DL(0),

    /**
     * mmol/L 单位
     */
    MMOL_L(1);

    companion object {
        /**
         * 根据数据库值获取枚举
         * @param value 数据库存储的整数值
         * @return 对应的枚举，默认为 MG_DL
         */
        fun fromValue(value: Int): GlucoseUnit {
            return entries.find { it.value == value } ?: MG_DL
        }

        /**
         * mg/dL 转 mmol/L 的转换系数
         */
        const val CONVERSION_FACTOR = 18.0
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
}