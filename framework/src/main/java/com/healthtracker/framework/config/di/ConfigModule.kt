package com.healthtracker.framework.config.di

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.healthtracker.framework.config.core.ConfigCache
import com.healthtracker.framework.config.core.ConfigRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 配置模块 - Hilt 依赖注入配置
 *
 * 提供配置框架所需的核心依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    /**
     * 提供 Firebase Remote Config 实例
     */
    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance()
    }

    /**
     * 提供 Gson 实例
     *
     * 用于 JSON 解析
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    /**
     * 提供配置注册表
     */
    @Provides
    @Singleton
    fun provideConfigRegistry(): ConfigRegistry {
        return ConfigRegistry()
    }

    /**
     * 提供配置缓存
     */
    @Provides
    @Singleton
    fun provideConfigCache(): ConfigCache {
        return ConfigCache()
    }
}
