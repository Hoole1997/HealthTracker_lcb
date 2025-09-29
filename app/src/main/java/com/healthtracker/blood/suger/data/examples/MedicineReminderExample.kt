package com.healthtracker.blood.suger.data.examples

import com.healthtracker.blood.suger.data.entity.MedicineReminder
import com.healthtracker.blood.suger.data.entity.PresetTimes
import com.healthtracker.blood.suger.data.repository.MedicineReminderRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import kotlinx.coroutines.flow.first
import java.util.*

/**
 * 药物提醒使用示例
 * 展示如何使用一个表解决所有药物提醒需求
 */
class MedicineReminderExample(
    private val repository: MedicineReminderRepository
) {

    /**
     * 示例1: 添加常见药物提醒
     */
    suspend fun example1_AddCommonMedicines() {
        println("=== Example 1: Add Common Medication Reminders ===")

        // 一日一次的维生素，带封面和备注
        repository.addMedicine(
            medicineName = "Vitamin C",
            reminderTimes = PresetTimes.ONCE_DAILY,
            medicineCover = "/storage/medicine_covers/vitamin_c.jpg",
            note = "Enhance immunity, take after meals",
            syncCalendar = true
        )

        // 一日两次的降压药
        repository.addMedicine(
            medicineName = "Blood Pressure Medicine",
            reminderTimes = PresetTimes.TWICE_DAILY,
            note = "Take on time, monitor blood pressure changes"
        )

        // 一日三次的抗生素
        repository.addMedicine(
            medicineName = "Amoxicillin",
            reminderTimes = PresetTimes.THREE_TIMES_DAILY,
            note = "7-day course, take 30 minutes after meals",
            syncCalendar = false
        )

        println("✅ Added 3 medication reminders")
    }

    /**
     * 示例2: 自定义提醒时间
     */
    suspend fun example2_CustomReminderTimes() {
        println("\n=== Example 2: Custom Reminder Times ===")

        // 特殊时间的胰岛素注射，带封面和详细备注
        repository.addMedicine(
            medicineName = "Insulin",
            reminderTimes = listOf("07:30", "11:30", "17:30", "21:30"),
            medicineCover = "/storage/medicine_covers/insulin.jpg",
            note = "Inject before meals, monitor blood sugar, eat immediately if hypoglycemic symptoms occur",
            syncCalendar = true
        )

        // 睡前服用的药物
        repository.addMedicine(
            medicineName = "Melatonin",
            reminderTimes = listOf("22:30"),
            note = "Take 30 minutes before bedtime, helps with sleep"
        )

        println("✅ Added medication reminders with custom times")
    }

    /**
     * 示例3: 记录服药操作
     */
    suspend fun example3_RecordMedication() {
        println("\n=== Example 3: Record Medication ===")

        val reminders = repository.getActiveReminders().first()
        if (reminders.isNotEmpty()) {
            val firstReminder = reminders[0]

            // 记录服药
            repository.recordMedication(firstReminder.id)
            println("✅ Recorded medication for ${firstReminder.medicineName}")

            // 记录系统提醒
            repository.recordRealRemind(firstReminder.id)
            println("🔔 Recorded system reminder for ${firstReminder.medicineName}")

            println("✨ 服药记录已成功保存")
        }
    }

    /**
     * 示例4: 查看所有药物信息
     */
    suspend fun example4_ViewAllMedicines() {
        println("\n=== 示例4: 查看所有药物信息 ===")

        val reminders = repository.getActiveReminders().first()

        println("📋 当前药物提醒列表:")
        reminders.forEach { medicine ->
            val times = medicine.getStartRemindTimeStrings().joinToString(", ")
            val noteInfo = if (medicine.note.isNotEmpty()) " - 备注: ${medicine.note}" else ""
            val syncInfo = if (medicine.isSyncToCalendar()) " [已同步日历]" else ""
            val coverInfo = if (medicine.medicineCover.isNotEmpty()) " [有封面]" else ""

            println("  • ${medicine.medicineName} - 提醒时间: $times$noteInfo$syncInfo$coverInfo")
        }

        println("\n📈 统计信息:")
        println("  总药物数: ${reminders.size}")
        println("  有备注的: ${reminders.count { it.note.isNotEmpty() }}")
        println("  已同步日历的: ${reminders.count { it.isSyncToCalendar() }}")
    }

    /**
     * 示例5: 搜索和管理药物
     */
    suspend fun example5_SearchAndManageMedicines() {
        println("\n=== 示例5: 搜索和管理药物 ===")

        // 搜索药物
        val searchResults = repository.searchMedicines("维生素")
        println("🔍 搜索'维生素'结果: ${searchResults.size}个")

        searchResults.forEach { medicine ->
            println("  找到: ${medicine.medicineName}")
        }

        // 禁用药物提醒
        if (searchResults.isNotEmpty()) {
            val firstMedicine = searchResults[0]
            repository.setMedicineActive(firstMedicine.id, false)
            println("⏸️ 已暂停 ${firstMedicine.medicineName} 的提醒")
        }

        // 重新启用
        if (searchResults.isNotEmpty()) {
            val firstMedicine = searchResults[0]
            repository.setMedicineActive(firstMedicine.id, true)
            println("▶️ 已重新启用 ${firstMedicine.medicineName} 的提醒")
        }
    }

    /**
     * 示例6: 数据分析和新功能展示
     */
    suspend fun example6_AdvancedFeaturesDemo() {
        println("\n=== 示例6: 新功能展示 ===\n")

        // 测试系统提醒记录
        val reminders = repository.getActiveReminders().first()
        if (reminders.isNotEmpty()) {
            val firstReminder = reminders[0]

            // 记录系统提醒
            repository.recordRealRemind(firstReminder.id)
            println("🔔 Recorded system reminder for ${firstReminder.medicineName}")

            // 更新备注
            repository.updateNote(firstReminder.id, "已更新备注信息")
            println("📝 已更新 ${firstReminder.medicineName} 的备注")

            // 更新日历同步设置
            repository.updateSyncCalendar(firstReminder.id, true)
            println("📅 已开启 ${firstReminder.medicineName} 的日历同步")
        }

        // 查看有备注的药物
        val withNotes = repository.getRemindersWithNotes()
        println("\n📝 有备注的药物提醒: ${withNotes.size}个")
        withNotes.forEach { medicine ->
            println("  • ${medicine.medicineName}: ${medicine.note}")
        }

        // 查看同步到日历的药物
        val synced = repository.getSyncedReminders()
        println("\n📅 已同步到日历的药物: ${synced.size}个")
        synced.forEach { medicine ->
            println("  • ${medicine.medicineName}")
        }

        example6_DetailedAnalytics()
    }

    /**
     * 详细数据分析
     */
    suspend fun example6_DetailedAnalytics() {
        println("\n=== 详细数据分析 ===")

        val reminders = repository.getActiveReminders().first()

        if (reminders.isNotEmpty()) {
            // 服药历史统计
            reminders.forEach { medicine ->
                val records = medicine.getTakedTimeList()
                val realReminds = medicine.getRealRemindTimeList()

                if (records.isNotEmpty() || realReminds.isNotEmpty()) {
                    println("💊 ${medicine.medicineName}:")

                    // 封面和备注信息
                    if (medicine.medicineCover.isNotEmpty()) {
                        println("  封面图片: ${medicine.medicineCover}")
                    }
                    if (medicine.note.isNotEmpty()) {
                        println("  备注: ${medicine.note}")
                    }
                    if (medicine.isSyncToCalendar()) {
                        println("  日历同步: 已启用")
                    }

                    println("  总服药次数: ${records.size}")
                    println("  真实提醒次数: ${realReminds.size}")

                    // 最近5次服药记录
                    if (records.isNotEmpty()) {
                        val recentRecords = records.takeLast(5)
                        println("  最近服药:")
                        recentRecords.forEach { record ->
                            println("    - ${DateTimeUtils.formatDateTimeWithSeconds(record)}")
                        }
                    }

                    // 最近5次真实提醒记录
                    if (realReminds.isNotEmpty()) {
                        val recentReminds = realReminds.takeLast(5)
                        println("  最近提醒:")
                        recentReminds.forEach { remind ->
                            println("    - ${DateTimeUtils.formatDateTimeWithSeconds(remind)}")
                        }
                    }
                    println()
                }
            }
        }
    }

    /**
     * 运行所有示例
     */
    suspend fun runAllExamples() {
        println("🚀 开始运行药物提醒系统示例")
        println("=" * 50)

        try {
            example1_AddCommonMedicines()
            example2_CustomReminderTimes()
            example3_RecordMedication()
            example4_ViewAllMedicines()
            example5_SearchAndManageMedicines()
            example6_AdvancedFeaturesDemo()

            println("\n🎉 所有示例运行完成！")
            println("💡 这就是简洁设计的力量：一个表，几个方法，解决服药管理需求！")

        } catch (e: Exception) {
            println("❌ 运行示例时出错: ${e.message}")
            e.printStackTrace()
        }
    }

    companion object {
        /**
         * 快速使用指南
         */
        fun printUsageGuide() {
            println("""
            📚 药物提醒系统使用指南
            ================================

            🎯 核心设计理念：
            • 一个表解决所有需求
            • 简单直观的API
            • 零学习成本

            📋 基本用法：

            1. 添加药物提醒：
               repository.addMedicine(
                   "阿司匹林",
                   listOf("08:00", "20:00"),
                   medicineCover = "/path/to/cover.jpg",
                   note = "饭后服用",
                   syncCalendar = true
               )

            2. 记录服药：
               repository.recordMedication(medicineId)

            3. 查看数据：
               medicine.getTakedTimeList()      // 获取所有服药记录

            4. 查看历史：
               medicine.getTakedTimeList()      // 已服药时间
               medicine.getRealRemindTimeList() // 真实提醒时间

            5. 搜索药物：
               repository.searchMedicines("维生素")

            6. 管理备注和设置：
               repository.updateNote(id, "新备注")
               repository.updateSyncCalendar(id, true)
               repository.getRemindersWithNotes()
               repository.getSyncedReminders()

            ✨ 预设时间模板：
            • ONCE_DAILY = ["08:00"]
            • TWICE_DAILY = ["08:00", "20:00"]
            • THREE_TIMES_DAILY = ["08:00", "12:00", "18:00"]
            • FOUR_TIMES_DAILY = ["08:00", "12:00", "18:00", "22:00"]

            🚀 就是这么简单！
            """.trimIndent())
        }
    }

    // Kotlin字符串重复扩展函数
    private operator fun String.times(n: Int): String = this.repeat(n)
}