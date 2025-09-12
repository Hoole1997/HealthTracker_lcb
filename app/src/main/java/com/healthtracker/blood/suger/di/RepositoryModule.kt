package com.healthtracker.blood.suger.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Repository依赖注入模块
 * 提供Repository接口与实现的绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    

} 