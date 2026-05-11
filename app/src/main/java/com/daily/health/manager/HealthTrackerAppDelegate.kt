package com.daily.health.manager

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import com.android.common.bill.ads.log.AdLogger
import com.android.common.bill.ui.admob.AdmobFullScreenNativeAdActivity
import com.android.common.bill.ui.gam.GamFullScreenNativeAdActivity
import com.android.common.bill.ui.pangle.PangleFullScreenNativeAdActivity
import com.android.common.bill.ui.topon.ToponFullScreenNativeAdActivity
import com.android.common.weather.WeatherActivity
import com.blankj.utilcode.util.LogUtils
import com.daily.health.manager.BuildConfig
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
import com.healthtracker.earthquake.EarthquakeActivity
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.LanguageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.corekit.core.log.CoreLogger
import net.corekit.metrics.adjust.AdjustTracker
import net.corekit.metrics.log.MetricsLogger
import net.corekit.monetize.ads.config.AdConfigManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.lang.ref.WeakReference
import java.util.Locale

typealias AttributionListener = (
    isOrganic: Boolean,
    network: String,
    campaign: String,
    adgroup: String,
    creative: String,
    jsonResponse: String
) -> Unit

class HealthTrackerAppDelegate(
    private val application: Application,
) {
    var isGoSetting = false
    var isFeatureLeave = false
    var isClickAdLeave = false

    private var isMainProcess: Boolean? = null
    private var leaveAppTime = 0L
    private var defaultLocale: WeakReference<Locale>? = WeakReference(Locale.getDefault())
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun prepareBaseContext(base: Context?): Context? {
        val localizedContext = base?.let { LanguageUtils.attachBaseContext(it) }
        return localizedContext ?: base
    }

    fun onBaseContextAttached() {
        initializeAttachBaseContextConfig()
        try {
            WebViewZygote.webViewCompact(application, applicationScope)
        } catch (_: Throwable) {
        }
    }

    private fun initializeAttachBaseContextConfig() {
        val appLogEnabled = BuildConfig.showLog
        BuildState.debug = appLogEnabled
        AdLogger.setLogEnabled(appLogEnabled)
        MetricsLogger.enableLog(appLogEnabled)
        CoreLogger.setLogEnabled(appLogEnabled)
    }

    fun onCreate(registerAttributionListener: (AttributionListener) -> Unit) {
        if (!isMainProcess(application)) {
            return
        }

        registerAttributionListener { isOrganic, network, campaign, adgroup, creative, jsonResponse ->
            AdjustTracker.init(
                context = application.applicationContext,
                network = network,
                campaign = campaign,
                adgroup = adgroup,
                creative = creative,
                jsonResponse = jsonResponse,
                isOrganic = isOrganic
            )
            LogUtils.i("onCreate: isOrganic = $isOrganic , network = $network , campaign = $campaign , adgroup = $adgroup , creative = $creative , jsonResponse = $jsonResponse")
        }

        val koin = runCatching { GlobalContext.get() }.getOrNull() ?: startKoin {
            androidContext(application)
            modules(
                appModule,
                databaseModule,
                frameworkConfigModule,
                appConfigModule,
            )
        }.koin

        val appInitializer = koin.get<AppInitializer>()
        val appForegroundObserver = koin.get<AppForegroundObserver>()

        appInitializer.initialize()

//        if (isLeast8()) {
//            BusinessShortcutManager.setAppShortcuts(application)
//        }
        AppLifecycleManager.initialize(application)

        AppLifecycleManager.configure {
            debounceMillis = 300
            trackScreenLock = true
        }

        appInitializer.registerLifecycleObservers()
        appForegroundObserver.initialize()
    }

    private fun isMainProcess(context: Context): Boolean {
        if (isMainProcess == null) {
            val packageName = context.packageName
            if (!TextUtils.isEmpty(packageName)) {
                isMainProcess = packageName == getCurProcessName(application)
            }
        }
        return isMainProcess ?: false
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.locale != defaultLocale?.get()) {
            defaultLocale = WeakReference(newConfig.locale)
            LanguageUtils.attachBaseContext(application)
        }
    }

    fun setLeaveTime() {
        leaveAppTime = System.currentTimeMillis()
    }

    suspend fun isLongLeaveApp(): Boolean {
        val leaveTime = System.currentTimeMillis() - leaveAppTime
        val configLongLeaveTime = AdConfigManager.getLongLeaveTime() * 1000L
        if (BuildState.debug) {
            "leaveTime = $leaveTime ms configLongLeaveTime = $configLongLeaveTime ms".logd(TAG)
        }

        return leaveTime > configLongLeaveTime
    }

    fun launcherActivityClass(): Class<*> = SplashScreen::class.java

    fun protectedActivityClasses(): List<Class<*>> = listOf(
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
        AiAssistantActivity::class.java,
        EarthquakeActivity::class.java,
        WeatherActivity::class.java,
        AdmobFullScreenNativeAdActivity::class.java,
        GamFullScreenNativeAdActivity::class.java,
        PangleFullScreenNativeAdActivity::class.java,
        ToponFullScreenNativeAdActivity::class.java,
    )

    private companion object {
        private const val TAG = "App"
    }
}
