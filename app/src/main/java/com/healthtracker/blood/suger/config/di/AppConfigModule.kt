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

    // PushMessageParser 通过 @Inject 构造函数自动注入（包括 Context）
    // 无需手动提供

    /**
     * 提供推送配置解析器
     */
    @Provides
    @Singleton
    fun providePushConfigParser(
        gson: Gson,
        pushMessageParser: PushMessageParser,
        remoteConfig: com.google.firebase.remoteconfig.FirebaseRemoteConfig
    ): PushConfigParser {
        return PushConfigParser(gson, pushMessageParser, remoteConfig)
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
