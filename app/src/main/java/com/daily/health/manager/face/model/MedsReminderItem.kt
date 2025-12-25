package com.daily.health.manager.face.model

import java.util.Date

/**
 * 药物提醒显示项数据模型
 * 用于在MedsFragment列表中展示单个提醒时间点
 */
data class MedsReminderItem(
    /**
     * 原始提醒记录ID，用于后续操作（如标记已服用、编辑等）
     */
    val reminderId: Long,

    /**
     * 显示时间，格式为"HH:mm"，如"08:00"
     */
    val time: String,

    /**
     * 药物名称
     */
    val medicineName: String,

    /**
     * 备注信息，可能为空
     */
    val notes: String,

    /**
     * 服药状态
     */
    val status: ReminderStatus,

    /**
     * 完整的提醒时间（选中日期+提醒时间），用于排序和状态判断
     */
    val reminderDateTime: Date,

    val medicineCover:String
) {
    /**
     * 是否已服药
     */
    fun isTaken(): Boolean = status == ReminderStatus.TAKEN
}

/**
 * 服药状态枚举
 */
enum class ReminderStatus {
    /**
     * 未服用
     */
    PENDING,

    /**
     * 已服用
     */
    TAKEN
}