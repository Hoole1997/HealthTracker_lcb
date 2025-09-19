package com.healthtracker.blood.suger.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.healthtracker.blood.suger.data.converter.DateTimeConverter
import com.healthtracker.blood.suger.data.dao.BloodPressureDao
import com.healthtracker.blood.suger.data.dao.BloodSugarDao
import com.healthtracker.blood.suger.data.dao.BloodSugarTagDao
import com.healthtracker.blood.suger.data.dao.BloodPressureTagDao
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarTag
import com.healthtracker.blood.suger.data.entity.BloodPressureTag

/**
 * 健康数据Room数据库
 *
 * 数据库版本: 1
 * 包含的表:
 * - blood_sugar_records: 血糖记录表
 * - blood_pressure_records: 血压记录表
 * - health_tags: 健康标签表
 */
@Database(
    entities = [
        BloodSugarRecord::class,
        BloodPressureRecord::class,
        BloodSugarTag::class,
        BloodPressureTag::class
    ],
    version = 2,
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
     * 获取血糖标签DAO
     */
    abstract fun bloodSugarTagDao(): BloodSugarTagDao

    /**
     * 获取血压标签DAO
     */
    abstract fun bloodPressureTagDao(): BloodPressureTagDao

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
                    // 如果需要支持数据库升级，可以添加migration
                    // .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }


        /**
         * 示例数据库迁移
         * 从版本1升级到版本2的迁移策略
         */
        /*
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加新字段的示例
                database.execSQL("ALTER TABLE blood_sugar_records ADD COLUMN new_field TEXT")
                database.execSQL("ALTER TABLE blood_pressure_records ADD COLUMN new_field TEXT")
            }
        }
        */

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