package com.healthtracker.blood.suger.config.di

import com.google.gson.Gson
import com.healthtracker.blood.suger.config.parsers.PushConfigParser
import com.healthtracker.blood.suger.config.parsers.PushMessageParser
import com.healthtracker.blood.suger.config.registry.AppConfigRegistry
import com.healthtracker.framework.config.core.ConfigRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用配置依赖注入模块
 *
 * 提供应用特定的配置解析器和注册表
 */
@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    /**
     * 提供推送消息解析器
     */
    @Provides
    @Singleton
    fun providePushMessageParser(gson: Gson): PushMessageParser {
        return PushMessageParser(gson)
    }

    /**
     * 提供推送配置解析器
     */
    @Provides
    @Singleton
    fun providePushConfigParser(
        gson: Gson,
        pushMessageParser: PushMessageParser
    ): PushConfigParser {
        return PushConfigParser(gson, pushMessageParser)
    }

    /**
     * 提供应用配置注册表
     */
    @Provides
    @Singleton
    fun provideAppConfigRegistry(
        registry: ConfigRegistry,
        pushConfigParser: PushConfigParser,
        pushMessageParser: PushMessageParser
    ): AppConfigRegistry {
        return AppConfigRegistry(registry, pushConfigParser, pushMessageParser)
    }
}
