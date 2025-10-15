package com.healthtracker.blood.suger.data.enums

/**
 * 标签类型枚举
 * 用于区分不同类型的健康标签
 */
enum class TagType(val value: Int) {
    /**
     * 血糖标签
     */
    BLOOD_SUGAR(0),
    
    /**
     * 血压标签
     */
    BLOOD_PRESSURE(1),

    /**
     * BMI 标签
     */
    BMI(2),

    /**
     * 心率标签
     */
    HEART_RATE(3);
    
    companion object {
        /**
         * 根据整数值获取对应的TagType
         * @param value 整数值
         * @return 对应的TagType，如果值无效则返回null
         */
        fun fromValue(value: Int): TagType? {
            return entries.find { it.value == value }
        }
    }
}
