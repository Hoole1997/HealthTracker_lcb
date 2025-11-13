package com.healthtracker.blood.suger.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.healthtracker.blood.suger.data.converter.DateTimeConverter
import com.healthtracker.blood.suger.data.dao.AlarmDao
import com.healthtracker.blood.suger.data.dao.BloodPressureDao
import com.healthtracker.blood.suger.data.dao.BloodSugarDao
import com.healthtracker.blood.suger.data.dao.HealthTagDao
import com.healthtracker.blood.suger.data.dao.MedicineReminderDao
import com.healthtracker.blood.suger.data.dao.BmiDao
import com.healthtracker.blood.suger.data.dao.HeartRateDao
import com.healthtracker.blood.suger.data.dao.CholesterolDao
import com.healthtracker.blood.suger.data.dao.HydrateDao
import com.healthtracker.blood.suger.data.dao.HydrateReminderDao
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.entity.MedicineReminder
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.entity.HydrateRecord
import com.healthtracker.blood.suger.data.entity.HydrateReminder

/**
 * 健康数据Room数据库 - 极简设计
 *
 * 数据库版本: 3
 * 包含的表:
 * - blood_sugar_records: 血糖记录表
 * - blood_pressure_records: 血压记录表
 * - health_tags: 统一健康标签表
 * - alarm_records: 闹钟记录表
 * - medicine_reminders: 药物提醒表（一表解决所有需求）
 */
@Database(
    entities = [
        BloodSugarRecord::class,
        BloodPressureRecord::class,
        HealthTag::class,
        AlarmRecord::class,
        MedicineReminder::class,
        BmiRecord::class,
        HeartRateRecord::class,
        CholesterolRecord::class,
        HydrateRecord::class,
        HydrateReminder::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class HealthDatabase : RoomDatabase() {

    /**
     * 获取血糖记录DAO
     */
    abstract fun bloodSugarDao(): BloodSugarDao

    /**
     * 获取血压记录DAO
     */
    abstract fun bloodPressureDao(): BloodPressureDao

    /**
     * 获取闹钟记录DAO
     */
    abstract fun alarmDao(): AlarmDao

    /**
     * 获取统一健康标签DAO
     */
    abstract fun healthTagDao(): HealthTagDao

    /**
     * 获取药物提醒DAO - 一个DAO解决所有药物提醒需求
     */
    abstract fun medicineReminderDao(): MedicineReminderDao

    /**
     * 获取BMI记录DAO
     */
    abstract fun bmiDao(): BmiDao

    /**
     * 获取心率记录DAO
     */
    abstract fun heartRateDao(): HeartRateDao

    /**
     * 获取胆固醇记录DAO
     */
    abstract fun cholesterolDao(): CholesterolDao

    /**
     * 获取饮水记录DAO
     */
    abstract fun hydrateDao(): HydrateDao

    /**
     * 获取饮水提醒DAO
     */
    abstract fun hydrateReminderDao(): HydrateReminderDao

    companion object {
        /**
         * 数据库名称
         */
        private const val DATABASE_NAME = "health_tracker.db"

        /**
         * 单例数据库实例
         */
        @Volatile
        private var INSTANCE: HealthDatabase? = null

        /**
         * 获取数据库实例
         * 使用单例模式确保全局只有一个数据库实例
         *
         * @param context 应用上下文
         * @return 数据库实例
         */
        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedHydrateReminders(db)
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

//        /**
//         * 数据库迁移：从版本1升级到版本2
//         * 添加alarm_records表
//         */
//        private val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                // 创建alarm_records表
//                database.execSQL("""
//                    CREATE TABLE alarm_records (
//                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
//                        type INTEGER NOT NULL,
//                        hour INTEGER NOT NULL,
//                        minute INTEGER NOT NULL,
//                        repeat_flag INTEGER NOT NULL,
//                        sound_id INTEGER NOT NULL,
//                        enable INTEGER NOT NULL DEFAULT 1,
//                        vibrate_time INTEGER NOT NULL DEFAULT 0,
//                        is_delete INTEGER NOT NULL DEFAULT 0,
//                        other TEXT,
//                        int1 INTEGER,
//                        int2 INTEGER,
//                        int3 INTEGER,
//                        float1 REAL,
//                        float2 REAL,
//                        long1 INTEGER,
//                        long2 INTEGER,
//                        text1 TEXT,
//                        text2 TEXT,
//                        text3 TEXT
//                    )
//                """)
//                
//                // 为alarm_records表创建索引
//                database.execSQL("CREATE INDEX index_alarm_records_type ON alarm_records(type)")
//                database.execSQL("CREATE INDEX index_alarm_records_time ON alarm_records(hour, minute)")
//                database.execSQL("CREATE INDEX index_alarm_records_enable ON alarm_records(enable)")
//            }
//        }

//        /**
//         * 数据库迁移：从版本2升级到版本3
//         * 移除measurement_tag字段
//         */
//        private val MIGRATION_2_3 = object : Migration(2, 3) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                // 创建新的血压记录表（不包含measurement_tag字段）
//                database.execSQL("""
//                    CREATE TABLE blood_pressure_records_new (
//                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
//                        record_time INTEGER NOT NULL,
//                        systolic_pressure INTEGER NOT NULL,
//                        diastolic_pressure INTEGER NOT NULL,
//                        pulse_rate INTEGER NOT NULL,
//                        bp_category TEXT NOT NULL,
//                        pulse_category TEXT NOT NULL,
//                        show_in_chart INTEGER NOT NULL DEFAULT 1,
//                        tag_ids TEXT,
//                        ext1 TEXT,
//                        ext2 TEXT,
//                        ext3 TEXT
//                    )
//                """)
//
//                // 复制数据（排除measurement_tag字段）
//                database.execSQL("""
//                    INSERT INTO blood_pressure_records_new
//                    (id, record_time, systolic_pressure, diastolic_pressure, pulse_rate,
//                     bp_category, pulse_category, show_in_chart, tag_ids, ext1, ext2, ext3)
//                    SELECT id, record_time, systolic_pressure, diastolic_pressure, pulse_rate,
//                           bp_category, pulse_category, show_in_chart, tag_ids, ext1, ext2, ext3
//                    FROM blood_pressure_records
//                """)
//
//                // 删除旧表
//                database.execSQL("DROP TABLE blood_pressure_records")
//
//                // 重命名新表
//                database.execSQL("ALTER TABLE blood_pressure_records_new RENAME TO blood_pressure_records")
//            }
//        }

        /**
         * 清除数据库实例（用于测试）
         */
        @JvmStatic
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        /**
         * 首次创建数据库时，预置8条默认饮水提醒（均启用）
         */
        private fun seedHydrateReminders(db: SupportSQLiteDatabase) {
            val hours = intArrayOf(8, 10, 12, 14, 16, 18, 20, 22)
            for (h in hours) {
                db.execSQL(
                    "INSERT INTO hydrate_reminders (hour, minute, enabled) VALUES (?, ?, 1)",
                    arrayOf<Any>(h, 0)
                )
            }
        }
    }

    /**
     * 获取数据库大小（字节）
     */
    fun getDatabaseSize(context: Context): Long {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        return if (dbFile.exists()) dbFile.length() else 0L
    }

    /**
     * 检查数据库是否存在
     */
    fun databaseExists(context: Context): Boolean {
        return context.getDatabasePath(DATABASE_NAME).exists()
    }
}
