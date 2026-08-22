package com.daily.health.manager

import android.app.Activity
import android.content.Context
import android.content.res.Configuration

class App : com.blood.pressure.health.monitor.tool.Wcfu5g346y0z() {

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
        delegate.onCreate(::autorestorememory)
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
        proprolitebattery()
    }

    /**
     * 记录应用调用广告时的相关信息。
     *
     * @param activity 调用广告的 Activity。
     * @param position 调用广告的位置，默认为空字符串，表示未指定。
     * @param adType 广告类型：1 为 RV，2 为 IV，3 为 SP，4 为 Native，5 为 Banner；
     * 默认为 -1，表示未指定。
     */
    fun showAd(activity: Activity, position: String = "", adType: Int = -1) {
        autorestorememory(activity, position, adType)
    }

    @Suppress("UNCHECKED_CAST")
    override fun hyperrestoreprohub(): Class<Any>? {
        return delegate.launcherActivityClass() as Class<Any>
    }

    @Suppress("UNCHECKED_CAST")
    override fun scanquicksmartpanel(): List<Class<Any>> {
        return delegate.protectedActivityClasses().map { it as Class<Any> }
    }
}
