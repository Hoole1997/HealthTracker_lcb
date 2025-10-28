package net.corekit.core.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import net.corekit.core.log.CoreLogger
import net.corekit.core.utils.ConfigRemoteManager

/**
 * Base模块内容提供者
 * 用于在模块初始化时获取 Context 并提供给其他模块使用
 * 优先级最高，确保在其他模块初始化之前就准备好
 */
class CoreModuleProvider : ContentProvider() {
    
    companion object {
        private var applicationContext: android.content.Context? = null
        
        /**
         * 获取应用上下文
         * 替代 Utils.getApp() 的依赖
         */
        fun getApplicationContext(): android.content.Context? = applicationContext
    }
    
    override fun onCreate(): Boolean {
        applicationContext = context?.applicationContext
        applicationContext?.let { ctx ->
            try {
                CoreLogger.d("CoreModuleProvider 初始化完成，Context 已准备就绪")
                ConfigRemoteManager.initialize()
            } catch (e: Exception) {
                CoreLogger.e("CoreModuleProvider 初始化失败", e)
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

