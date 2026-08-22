package com.daily.health.manager

import android.content.Context
import android.content.res.Configuration

class App : com.leafmotivation.quizguessoncolor.Iej9ieio6r89e7ya() {

    private val delegate = HealthTrackerAppDelegate(this)

    companion object {
        lateinit var INSTANCE: App
            private set
    }

    init {
        INSTANCE = this
    }

    var isGoSetting: Boolean
        get() = delegate.isGoSetting
        set(value) {
            delegate.isGoSetting = value
        }

    var isFeatureLeave: Boolean
        get() = delegate.isFeatureLeave
        set(value) {
            delegate.isFeatureLeave = value
        }

    var isClickAdLeave: Boolean
        get() = delegate.isClickAdLeave
        set(value) {
            delegate.isClickAdLeave = value
        }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(delegate.prepareBaseContext(base))
        delegate.onBaseContextAttached()
    }

    override fun onCreate() {
        super.onCreate()
        delegate.onCreate(::maxquicklitememory)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        delegate.onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    fun setLeaveTime() {
        delegate.setLeaveTime()
    }

    suspend fun isLongLeaveApp(): Boolean = delegate.isLongLeaveApp()

    fun goHome() {
        localproshield()
    }

    @Suppress("UNCHECKED_CAST")
    override fun ultrasafecorehub(): Class<Any>? {
        return delegate.launcherActivityClass() as Class<Any>
    }

    @Suppress("UNCHECKED_CAST")
    override fun prodailysmartmemory(): List<Class<Any>> {
        return delegate.protectedActivityClasses().map { it as Class<Any> }
    }
}
