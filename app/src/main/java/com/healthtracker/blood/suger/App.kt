package com.healthtracker.blood.suger

import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import androidx.multidex.MultiDexApplication
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.observer.AppForegroundObserver
import com.healthtracker.blood.suger.utils.WebViewZygote
import com.healthtracker.blood.suger.utils.getCurProcessName
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.LanguageUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.lang.ref.WeakReference
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App : MultiDexApplication() {

    @Inject
    lateinit var appInitializer: AppInitializer

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var appForegroundObserver: AppForegroundObserver

    companion object {
        private const val TAG = "App"
        lateinit var INSTANCE: App
            private set

        @JvmStatic
        var defaultLocale: WeakReference<Locale>? = null
    }

    init {
        // 1. 设置静态实例
        INSTANCE = this
        defaultLocale = WeakReference(Locale.getDefault())
    }

    /**
     * 主进程检查缓存
     * 对应原App.kt中的isMainProcess逻辑
     */
    private var isMainProcess: Boolean? = null

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    override fun attachBaseContext(base: Context?) {


        base?.run {
            var context = LanguageUtils.attachBaseContext(this)
            if (context == null) {
                context = this
            }
            super.attachBaseContext(context)
        } ?: kotlin.run {
            super.attachBaseContext(null)
        }

        // 3. WebView兼容性处理
        try {
            WebViewZygote.webViewCompact(this,applicationScope )
        } catch (_: Throwable) {

        }
    }

    override fun onCreate() {
        super.onCreate()
        // 只在主进程中进行初始化 (对应原App.kt中的isMainProcess检查)
        if (isMainProcess(this)) {
            // 应用初始化（包含远程配置初始化）
            appInitializer.initialize()

            // ✅ 初始化应用生命周期管理器(替代旧的initProcessLifeCycle)
            AppLifecycleManager.initialize(this)

            // 可选: 配置生命周期管理器(如需自定义参数)
            AppLifecycleManager.configure {
                debounceMillis = 300
                trackScreenLock = true
            }

            // 注册生命周期观察者（包含配置刷新观察者）
            appInitializer.registerLifecycleObservers()

            // 初始化权限管理器session
            permissionManager.initializeSession()

            // 初始化前后台状态观察器（用于 Loop 推送）
            appForegroundObserver.initialize()
        }
    }


    /**
     * 主进程检查
     * 完全复制自原App.kt中的isMainProcess逻辑
     */
    private fun isMainProcess(context: Context): Boolean {
        if (isMainProcess == null) {
            val packageName = context.packageName
            if (!TextUtils.isEmpty(packageName)) {
                isMainProcess = packageName == getCurProcessName(this)
            }
        }
        return isMainProcess ?: false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.locale != defaultLocale?.get()) {
            defaultLocale = WeakReference(newConfig.locale)
            LanguageUtils.attachBaseContext(this)
        }
        super.onConfigurationChanged(newConfig)
    }
}