package net.corekit.monetize.ads.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.log.AdLogger

/**
 * 广告模块内容提供者
 * 用于在模块初始化时获取 Context 并初始化 AdConfigManager
 */
class AdModuleProvider : ContentProvider() {
    
    companion object {
        private var applicationContext: android.content.Context? = null
        
        /**
         * 获取应用上下文
         */
        fun getApplicationContext(): android.content.Context? = applicationContext
    }
    
    override fun onCreate(): Boolean {
        applicationContext = context?.applicationContext
        applicationContext?.let { ctx ->
            try {
                // 初始化广告配置控制器
                AdConfigManager.initialize(ctx)

                // 初始化平台级频控管理器
                PlatformFrequencyManager.initialize(ctx)

                // 初始化IPU上报控制器
                IpuController.initialize(ctx)

                // 初始化FPU上报控制器
                FpuController.initialize(ctx)

                // 初始化RPU上报控制器
                RpuController.initialize(ctx)

                AdLogger.d("AdModuleProvider 初始化完成")
            } catch (e: Exception) {
                AdLogger.e("AdModuleProvider 初始化失败", e)
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
