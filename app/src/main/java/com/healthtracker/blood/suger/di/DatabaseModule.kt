package com.healthtracker.blood.suger.di

import android.content.Context
import com.healthtracker.blood.suger.data.dao.BloodPressureDao
import com.healthtracker.blood.suger.data.dao.BloodSugarDao
import com.healthtracker.blood.suger.data.dao.HealthTagDao
import com.healthtracker.blood.suger.data.database.HealthDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 * 提供Room数据库和DAO实例
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供Health数据库实例
     * @param context 应用上下文
     * @return HealthDatabase实例
     */
    @Provides
    @Singleton
    fun provideHealthDatabase(@ApplicationContext context: Context): HealthDatabase {
        return HealthDatabase.getDatabase(context)
    }

    /**
     * 提供血糖记录DAO
     * @param database HealthDatabase实例
     * @return BloodSugarDao实例
     */
    @Provides
    fun provideBloodSugarDao(database: HealthDatabase): BloodSugarDao {
        return database.bloodSugarDao()
    }

    /**
     * 提供血压记录DAO
     * @param database HealthDatabase实例
     * @return BloodPressureDao实例
     */
    @Provides
    fun provideBloodPressureDao(database: HealthDatabase): BloodPressureDao {
        return database.bloodPressureDao()
    }

    /**
     * 提供健康标签DAO
     * @param database HealthDatabase实例
     * @return HealthTagDao实例
     */
    @Provides
    fun provideHealthTagDao(database: HealthDatabase): HealthTagDao {
        return database.healthTagDao()
    }
}