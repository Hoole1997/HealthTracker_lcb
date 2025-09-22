package com.healthtracker.blood.suger.ui.weight

/**
 * 等级分类通用接口
 * 定义等级在进度条上的位置和对应颜色
 */
interface LevelCategory {
    /**
     * 在进度条上的位置 (0.0 - 1.0)
     */
    val position: Float

    /**
     * 对应的颜色资源ID
     */
    val colorRes: Int
}