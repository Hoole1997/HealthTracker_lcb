package com.healthtracker.blood.suger.ui.dialog

/**
 * 通用等级说明项
 * 用于 LevelExplainDialog 展示名称、范围描述与颜色
 */
data class LevelExplainItem(
    val name: String,
    val desc: String,
    val colorInt: Int
)