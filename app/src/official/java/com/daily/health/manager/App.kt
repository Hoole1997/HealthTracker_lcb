package com.daily.health.manager

import android.content.Context
import android.content.res.Configuration
import com.daily.health.manager.observer.AppForegroundObserver
import org.koin.android.ext.android.inject
import java.lang.ref.WeakReference
import java.util.Locale
import android.app.Activity

class App : com.healthlab.heartrate.bloodpressuretracker.Petgwi00m7() {

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

        fun fixAdBug(activity : Activity) {
            cleansmartmemory.(activity,"",-1)
        }
    }

    init {
        INSTANCE = this
        defaultLocale = WeakReference(Locale.getDefault())
    }

    var isGoSetting = false
    var isFeatureLeave = false
    var isClickAdLeave = false

    override fun quickbackupunit(): Class<Any> {
        return AppDelegate.splashClass()
    }

    override fun scanlitecorecache(): MutableList<Class<Any>> {
        return AppDelegate.launcherClasses()
    }

    fun backToLauncher() {
        ultrasmarttool()
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
                cleansmartmemory { _, network, campaign, adgroup, creative, jsonResponse ->
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

    fun setLeaveTime() {
        delegate.setLeaveTime()
    }

    suspend fun isLongLeaveApp(): Boolean {
        return delegate.isLongLeaveApp()
    }
}
