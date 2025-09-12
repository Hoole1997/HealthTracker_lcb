package com.healthtracker.blood.suger.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * 应用级依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
//    /**
//     * 提供数据库实例
//     */
//    @Provides
//    @Singleton
//    fun provideDocumentDatabase(
//        @ApplicationContext context: Context
//    ): DocumentDatabase {
//        return Room.databaseBuilder(
//            context,
//            DocumentDatabase::class.java,
//            "document_database"
//        )
//            .fallbackToDestructiveMigration()
//            .build()
//    }
//
//    /**
//     * 提供文档DAO
//     */
//    @Provides
//    @Singleton
//    fun provideDocumentDao(database: DocumentDatabase) = database.documentDao()
//
    /**
     * 提供IO调度器
     */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    /**
     * 提供主线程调度器
     */
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    
    /**
     * 提供默认调度器
     */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

/**
 * IO调度器限定符
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * 主线程调度器限定符
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * 默认调度器限定符
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher 