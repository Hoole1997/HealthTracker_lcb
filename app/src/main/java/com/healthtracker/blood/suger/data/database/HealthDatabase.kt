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
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord

/**
 * 健康数据Room数据库
 *
 * 数据库版本: 1
 * 包含的表:
 * - blood_sugar_records: 血糖记录表
 * - blood_pressure_records: 血压记录表
 */
@Database(
    entities = [
        BloodSugarRecord::class,
        BloodPressureRecord::class
    ],
    version = 1,
    exportSchema = true
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
                    .addCallback(DatabaseCallback)
                    // 如果需要支持数据库升级，可以添加migration
                    // .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 数据库创建和打开回调
         */
        private object DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 数据库创建时的初始化操作
                // 可以在这里插入初始数据或创建索引

                // 创建查询优化索引
                db.execSQL("CREATE INDEX IF NOT EXISTS index_blood_sugar_records_record_time ON blood_sugar_records(record_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_blood_sugar_records_show_in_chart ON blood_sugar_records(show_in_chart)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_blood_sugar_records_measurement_tag ON blood_sugar_records(measurement_tag)")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_blood_pressure_records_record_time ON blood_pressure_records(record_time)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_blood_pressure_records_show_in_chart ON blood_pressure_records(show_in_chart)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_blood_pressure_records_measurement_tag ON blood_pressure_records(measurement_tag)")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // 数据库每次打开时的操作
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