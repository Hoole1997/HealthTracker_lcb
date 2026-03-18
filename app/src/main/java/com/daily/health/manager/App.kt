package com.daily.health.manager

import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import androidx.multidex.MultiDex
import com.android.common.weather.WeatherActivity
import com.daily.health.manager.ad.FullScreenNativeAdActivity
import com.daily.health.manager.di.appConfigModule
import com.daily.health.manager.di.appModule
import com.daily.health.manager.di.databaseModule
import com.daily.health.manager.di.frameworkConfigModule
import com.daily.health.manager.face.act.AddReminderAct
import com.daily.health.manager.face.act.AiAssistantActivity
import com.daily.health.manager.face.act.AlarmManageScreen
import com.daily.health.manager.face.act.FeedbackAct
import com.daily.health.manager.face.act.GuideAct
import com.daily.health.manager.face.act.HealthDetailAct
import com.daily.health.manager.face.act.HealthRecordAct
import com.daily.health.manager.face.act.HealthStatisticsAct
import com.daily.health.manager.face.act.HeartRateMeasureScreen
import com.daily.health.manager.face.act.HistoryRecordAct
import com.daily.health.manager.face.act.HydrateAct
import com.daily.health.manager.face.act.HydrateCompleteScreen
import com.daily.health.manager.face.act.HydrateSettingAct
import com.daily.health.manager.face.act.InnerWebAct
import com.daily.health.manager.face.act.InsightsDetailAct
import com.daily.health.manager.face.act.LanguageAct
import com.daily.health.manager.face.act.MainAct
import com.daily.health.manager.face.act.ProfileActivity
import com.daily.health.manager.face.act.SplashScreen
import com.daily.health.manager.face.act.StepCountAct
import com.daily.health.manager.face.act.StepSettingAct
import com.daily.health.manager.face.act.TargetRangeAct
import com.daily.health.manager.face.act.UninstallConfirmActivity
import com.daily.health.manager.face.act.UninstallResenActivity
import com.daily.health.manager.observer.AppForegroundObserver
import com.daily.health.manager.utils.WebViewZygote
import com.daily.health.manager.utils.getCurProcessName
import com.healthtracker.framework.BuildState
import com.healthtracker.earthquake.EarthquakeActivity
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.isLeast8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.corekit.metrics.adjust.AdjustTracker
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.pangle.PangleFullScreenNativeAdActivity
import net.corekit.monetize.ads.topon.TopOnFullScreenNativeAdActivity
import net.corekit.monetize.ui.AdmobFullScreenNativeAdActivity
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.lang.ref.WeakReference
import java.util.Locale

class App : com.rocket.candy.line.Hdm6xfn0f7mv6dem7e() {

    private val appInitializer: AppInitializer by inject()

    private val appForegroundObserver: AppForegroundObserver by inject()

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

    var isGoSetting = false

    var isFeatureLeave = false

    var isClickAdLeave = false


    /**
     * 主进程检查缓存
     * 对应原App.kt中的isMainProcess逻辑
     */
    private var isMainProcess: Boolean? = null

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    override fun lpu(): Class<in Any>? {
        return SplashScreen::class.java as Class<in Any>?
    }

    override fun tep(): List<Class<in Any>?>? {
        return listOf(
            SplashScreen::class.java,
            GuideAct::class.java,
            MainAct::class.java,
            StepCountAct::class.java,
            StepSettingAct::class.java,
            TargetRangeAct::class.java,
            HealthDetailAct::class.java,
            HealthRecordAct::class.java,
            InsightsDetailAct::class.java,
            HistoryRecordAct::class.java,
            AlarmManageScreen::class.java,
            AddReminderAct::class.java,
            ProfileActivity::class.java,
            HydrateAct::class.java,
            HydrateSettingAct::class.java,
            HydrateCompleteScreen::class.java,
            HealthStatisticsAct::class.java,
            LanguageAct::class.java,
            InnerWebAct::class.java,
            FeedbackAct::class.java,
            HeartRateMeasureScreen::class.java,
            UninstallResenActivity::class.java,
            UninstallConfirmActivity::class.java,
            FullScreenNativeAdActivity::class.java,
            AiAssistantActivity::class.java,
            EarthquakeActivity::class.java,
            WeatherActivity::class.java,
            AdmobFullScreenNativeAdActivity::class.java,
            PangleFullScreenNativeAdActivity::class.java,
            TopOnFullScreenNativeAdActivity::class.java,
        ) as List<Class<in Any>?>?
    }

    override fun attachBaseContext(base: Context?) {
        MultiDex.install(this)
        try {
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
            WebViewZygote.webViewCompact(this,applicationScope )
        } catch (_: Throwable) {

        }
    }

    override fun onCreate() {
        super.onCreate()

        // 只在主进程中进行初始化 (对应原App.kt中的isMainProcess检查)
        if (isMainProcess(this)) {

            // adjust
            AdjustTracker.init(this)
            iiy { attribution ->
                AdjustTracker.handleAttributionChanged(
                    network = attribution.dzo(),
                    campaign = attribution.gwe(),
                    adgroup = attribution.bbs(),
                    creative = attribution.qsu(),
                    jsonResponse = attribution.kpm(),
                    isOrganic = attribution.cgo(),
                )
            }

            startKoin {
                androidContext(this@App)
                modules(
                    appModule,
                    databaseModule,
                    frameworkConfigModule,
                    appConfigModule,
                )
            }
            // 应用初始化（包含远程配置初始化）
            appInitializer.initialize()
            if(isLeast8()){
                BusinessShortcutManager.setAppShortcuts(this)
            }
            // ✅ 初始化应用生命周期管理器(替代旧的initProcessLifeCycle)
            AppLifecycleManager.initialize(this)

            // 可选: 配置生命周期管理器(如需自定义参数)
            AppLifecycleManager.configure {
                debounceMillis = 300
                trackScreenLock = true
            }

            // 注册生命周期观察者（包含配置刷新观察者）
            appInitializer.registerLifecycleObservers()

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

    private var leaveAppTime = 0L
    fun setLeaveTime(){
        leaveAppTime = System.currentTimeMillis()
    }

    suspend fun isLongLeaveApp(): Boolean{
        val leaveTime = System.currentTimeMillis() - leaveAppTime
        val configLongLeaveTime = AdConfigManager.getLongLeaveTime() * 1000L
        if(BuildState.debug) "leaveTime = $leaveTime ms configLongLeaveTime = $configLongLeaveTime ms".logd(TAG)

        return leaveTime > configLongLeaveTime
    }
}
