package com.daily.health.manager

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import com.daily.health.manager.observer.AppForegroundObserver
import com.leafmotivation.quizguessoncolor.Iej9ieio6r89e7ya
import org.koin.android.ext.android.inject
import java.lang.ref.WeakReference
import java.util.Locale

class App : Iej9ieio6r89e7ya() {

    private val appInitializer: AppInitializer by inject()
    private val appForegroundObserver: AppForegroundObserver by inject()

    private val delegate: AppDelegate by lazy(LazyThreadSafetyMode.NONE) {
        AppDelegate(
            application = this,
            getDefaultLocale = { defaultLocale?.get() },
            updateDefaultLocale = { locale -> defaultLocale = WeakReference(locale) },
        )
    }

    companion object {
        lateinit var INSTANCE: App
            private set

        @JvmStatic
        var defaultLocale: WeakReference<Locale>? = null

        fun fixAdBug(activity: Activity) {
            INSTANCE.maxquicklitememory(activity, "", -1)
        }
    }

    init {
        INSTANCE = this
        defaultLocale = WeakReference(Locale.getDefault())
    }

    var isGoSetting = false
    var isFeatureLeave = false
    var isClickAdLeave = false

    override fun ultrasafecorehub(): Class<Any>? {
        // 新版 Local SDK：启动入口返回单个 Activity Class。
        return AppDelegate.splashClass()
    }

    override fun prodailysmartmemory(): List<Class<Any>> {
        // 新版 Local SDK：应用页面注册返回 Activity Class 列表。
        return AppDelegate.launcherClasses()
    }

    fun backToLauncher() {
        localproshield()
    }

    override fun attachBaseContext(base: Context?) {
        delegate.installMultiDex()
        try {
            super.attachBaseContext(delegate.prepareBaseContext(base))
            delegate.onBaseContextAttached()
        } catch (_: Throwable) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        delegate.onCreate(
            appInitializerProvider = { appInitializer },
            appForegroundObserverProvider = { appForegroundObserver },
            registerAttributionCallback = {
                maxquicklitememory { _, network, campaign, adgroup, creative, jsonResponse ->
                    delegate.handleAttributionChanged(
                        network = network,
                        campaign = campaign,
                        adgroup = adgroup,
                        creative = creative,
                        jsonResponse = jsonResponse,
                    )
                }
            },
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        delegate.onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
    }

}
