package com.daily.health.manager.ui.tracker

/**
 * 健康类型枚举
 * 用于事件上报的 page_name 参数值
 *
 * @property pageName 在埋点事件中上报的页面名称
 */
enum class HealthType(val pageName: String) {
    /** 血糖 */
    BLOOD_SUGAR("Blood Sugar"),
    
    /** 血压 */
    BLOOD_PRESSURE("Blood Pressure"),
    
    /** 胆固醇 */
    CHOLESTEROL("Cholesterol"),
    
    /** 心率 */
    HEART_RATE("Heart Rate"),
    
    /** BMI 体重指数 */
    BMI("BMI"),
    
    /** 饮水 */
    HYDRATE("Hydrate"),
    WALKING_STEPS("Walking Steps"),
    OTHER("Other"),

}
