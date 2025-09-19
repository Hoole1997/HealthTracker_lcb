package com.healthtracker.blood.suger.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.healthtracker.blood.suger.data.converter.DateTimeConverter
import com.healthtracker.blood.suger.data.dao.BloodPressureDao
import com.healthtracker.blood.suger.data.dao.BloodSugarDao
import com.healthtracker.blood.suger.data.dao.HealthTagDao
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.HealthTag

/**
 * 健康数据Room数据库
 *
 * 数据库版本: 1
 * 包含的表:
 * - blood_sugar_records: 血糖记录表
 * - blood_pressure_records: 血压记录表
 * - health_tags: 统一健康标签表
 */
@Database(
    entities = [
        BloodSugarRecord::class,
        BloodPressureRecord::class,
        HealthTag::class
    ],
    version = 1,
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
     * 获取统一健康标签DAO
     */
    abstract fun healthTagDao(): HealthTagDao

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
//                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }


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