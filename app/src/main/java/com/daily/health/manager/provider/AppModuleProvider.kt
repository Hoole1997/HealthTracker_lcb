package com.daily.health.manager.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.daily.health.manager.BuildConfig
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.util.SpUtils
import net.corekit.core.log.CoreLogger

/**
 * Base模块内容提供者
 * 用于在模块初始化时获取 Context 并提供给其他模块使用
 * 优先级最高，确保在其他模块初始化之前就准备好
 */
class AppModuleProvider : ContentProvider() {
    
    companion object {
        private var applicationContext: Context? = null
        
        /**
         * 获取应用上下文
         * 替代 Utils.getApp() 的依赖
         */
        fun getApplicationContext(): Context? = applicationContext
    }
    
    override fun onCreate(): Boolean {
        applicationContext = context?.applicationContext
        applicationContext?.let { ctx ->
            try {
                BuildState.debug = BuildConfig.showLog
                SpUtils.init(ctx)
                CoreLogger.d("AppModuleProvider 初始化完成，Context 已准备就绪")

            } catch (e: Exception) {
                CoreLogger.e("AppModuleProvider 初始化失败", e)
            }
        }

        return true
    }
    
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null
    
    override fun getType(uri: Uri): String? = null
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0
}

