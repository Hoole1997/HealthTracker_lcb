package com.daily.health.manager

import android.app.Application
import android.content.Intent
import android.os.Looper
import android.view.Gravity
import com.blankj.utilcode.util.ActivityUtils
import com.daily.health.manager.config.registry.AppConfigRegistry
import com.daily.health.manager.constants.KEY_APP_FIRST_START_TIME
import com.daily.health.manager.helper.NotificationHelper
import com.daily.health.manager.strategy.PushScenario
import com.daily.health.manager.toast.CustomToastStyle
import com.daily.health.manager.face.act.SplashScreen
import com.daily.health.manager.utils.InsightAssetPreparer
import com.daily.health.manager.utils.isAdPage
import com.daily.health.manager.work.HealthWorkTask
import com.healthtracker.earthquake.push.EarthquakePushInitializer
import com.android.common.weather.WeatherInitializer
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.lifecycle.AppForegroundObserver
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.LogUtils
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.logException
import com.healthtracker.framework.util.postRunnable
import com.hjq.toast.Toaster
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.LaunchAds

/**
 * 应用初始化器
 * 统一管理应用启动时的初始化逻辑
 * 迁移自App.kt，保持所有原有功能，使用SpUtils管理偏好设置
 */
class AppInitializer(
    private val application: Application,
    private val ioDispatcher: CoroutineDispatcher,
    private val remoteConfigManager: RemoteConfigManager,
    private val appConfigRegistry: AppConfigRegistry,
    private val healthServiceForegroundObserver: com.daily.health.manager.observer.HealthServiceForegroundObserver
) {
    
    private val initScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var isFirstLaunch = true
    companion object{
        private const val TAG = "App"
    }

    /**
     * 配置刷新观察者
     *
     * 监听应用生命周期，在进入前台时自动刷新配置
     */
    private val configRefreshObserver = object : AppForegroundObserver {
        override fun onAppForeground() {
            if (BuildState.debug) "App entered foreground, refreshing config...".logd(TAG)
            initScope.launch {
                remoteConfigManager.refreshConfig()
            }

            initScope.launch(Dispatchers.Main) {
                try {

                    if(App.INSTANCE.isLongLeaveApp()){
                        if(BuildState.debug) "长时间离开应用，不检查离开原因，都尝试走开屏".logd(TAG)
                    }else{
                        if(BuildState.debug) "短时间离开应用，检查离开原因".logd(TAG)
                        if(App.INSTANCE.isGoSetting){
                            if(BuildState.debug) "去授权离开的应用，返回不走开屏".logd(TAG)
                            App.INSTANCE.isGoSetting = false
                            return@launch
                        }
                        if(App.INSTANCE.isFeatureLeave){
                            if(BuildState.debug) "功能需要离开应用，返回不走开屏".logd(TAG)
                            App.INSTANCE.isFeatureLeave = false
                            return@launch
                        }

                        if(App.INSTANCE.isClickAdLeave){
                            if(BuildState.debug) "点击广告离开应用，返回不走开屏".logd(TAG)
                            App.INSTANCE.isClickAdLeave = false
                            return@launch
                        }
                    }



                    //检查是否满足展示开屏广告条件
                    val topActivity = ActivityUtils.getTopActivity()
                    "回到前台,尝试重走启动页 topActivity:${topActivity::class.java.simpleName}".logd(TAG)
                    if (!ActivityUtils.isActivityExistsInStack(SplashScreen::class.java) && !isAdPage(topActivity
                        )) {
                        val result = LaunchAds.getInstance().checkInterceptor(application)
                        if(result is AdResult.Success){
                            startSplashActivity()
                        }else{
                            "当前不满足启动页开屏广告条件，不走启动页".logd(TAG)
                        }

                    }else{
                        "当前前台页面是启动页或广告页面，或引导页面，不重新走启动页面".logd(TAG)
                    }
                }catch (e: Throwable){
                    e.printStackTrace()
                }
            }

        }

        override fun onScreenLocked() {
            super.onScreenLocked()
            if(BuildState.debug){
                //TODO 测试用的
                initScope.launch {
                    delay(5000)
                    NotificationHelper.show(PushScenario.BACKGROUND)
                }
            }
        }

        override fun onAppBackground() {
            super.onAppBackground()
            App.INSTANCE.setLeaveTime()
        }
    }

    fun startSplashActivity() {
        try {
            val intent = Intent(application, SplashScreen::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            application.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 执行应用初始化
     * 按照App.kt中的原始顺序执行初始化
     */
    fun initialize() {
        // 1. 核心同步初始化 (原onCreate中的同步部分)
        initializeCoreServices()

        // 2. 延迟异步初始化 (原UIUtils.postRunnable中的部分)
        initializeDelayedServices()

        // 3. 架构验证初始化
        initializeArchitectureValidation()

        // 4. 远程配置初始化
        initializeRemoteConfig()

        // 5. 地震模块初始化
        initEarthquakeModule()

        // 6. 天气模块初始化
        initializeWeatherModule()
    }

    private fun initializeWeatherModule() {
        try {
            WeatherInitializer.init(application, BuildState.debug)
            if (BuildState.debug) "Weather module initialized".logd(TAG)
        } catch (e: Exception) {
            if (BuildState.debug) "Failed to initialize weather module: ${e.message}".loge(TAG)
        }
    }

    private fun initEarthquakeModule() {
        // 第二个参数可以传入RemoteConfig获取的intervalHours,不传默认32H
        EarthquakePushInitializer.init(application)
    }

    /**
     * 核心同步初始化服务
     * 对应App.kt中onCreate的同步部分
     */
    private fun initializeCoreServices() {
        try {
            if (!BuildState.debug) {
                //捕获非主线程和后台发生的异常
                setBackgroundExceptionHandler()
            }

            initScope.launch {
                AdsManager.init(application)
            }

            initScope.launch {
                InsightAssetPreparer.prepare(application)
            }

//            initScope.launch {
//                AdKit.initialize(application, BuildConfig.ADMOB_APPLICATION_ID, configs = listOf(
//                    AdUnitConfig(
//                        adType = AdType.APP_OPEN,
//                        adUnitId = BuildConfig.ADMOB_SPLASH_ID,
//                        expiryDurationMs = 4 * 3600_000L,
//                        loadTimeoutMillis = 10_000L
//                    )
//                ))
//            }
            
        } catch (e: Throwable) {
            e.printStackTrace()
            // 即使某个服务初始化失败，也要继续其他服务的初始化
        }
    }
    
    /**
     * 延迟异步初始化服务
     * 对应App.kt中UIUtils.postRunnable的部分
     */
    private fun initializeDelayedServices() {
        postRunnable {
            try {
                Toaster.init(application, CustomToastStyle(R.layout.ht_toast_success, Gravity.BOTTOM))
                // 1. ScanWorkTask.registerReceiver(this) - 屏幕解锁广播
                HealthWorkTask.registerReceiver(application)

                // 2. ScanWorkTask.start(this, BuildConfig.APPLICATION_ID) - 后台任务
                HealthWorkTask.start(application, BuildConfig.APPLICATION_ID)

            }catch (e: Throwable){
                e.printStackTrace()
            }
        }


    }
    
    /**
     * 架构验证初始化
     * 验证新架构的组件是否正常工作
     */
    private fun initializeArchitectureValidation() {
        initScope.launch {
            try {
                // 验证Hilt注入是否正常工作
                validateArchitectureComponents()
                
                // 处理首次启动逻辑 (使用SpUtils)
                handleFirstLaunch()
                
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 验证架构组件
     */
    private fun validateArchitectureComponents() {
        try {
            // 验证Hilt注入是否正常工作
            // 验证协程调度器是否正常
            // 这里可以添加一些轻量级的验证逻辑
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    
    /**
     * 处理首次启动逻辑
     * 使用SpUtils进行首次启动检查
     */
    private fun handleFirstLaunch() {
        try {
            val isFirstLaunch = SpUtils.getLong(KEY_APP_FIRST_START_TIME, 0L) == 0L
            if (isFirstLaunch) {
                onFirstLaunch()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    
    /**
     * 首次启动处理
     */
    private fun onFirstLaunch() {
        try {
            // 首次启动的特殊处理
            SpUtils.putLong(KEY_APP_FIRST_START_TIME, System.currentTimeMillis())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun setBackgroundExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            LogUtils.logException(exception, false)
            exception.printStackTrace()
            if (thread.id != Looper.getMainLooper().thread.id) {
                logException(exception)
                return@setDefaultUncaughtExceptionHandler
            }
            if (AppLifecycleManager.isBackground()) {
                logException(exception)
            } else {
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }

    /**
     * 初始化远程配置框架
     *
     * 包括:
     * 1. 注册所有配置解析器
     * 2. 初始化 RemoteConfigManager
     */
    private fun initializeRemoteConfig() {
        initScope.launch {
            try {
                // 1. 注册所有配置解析器
                appConfigRegistry.registerAllParsers()
                if(BuildState.debug) "Config parsers registered: ${appConfigRegistry.getRegisteredCount()}".logd(TAG)

                // 2. 初始化 RemoteConfigManager
                val result = remoteConfigManager.initialize()
                result.onSuccess {
                    if(BuildState.debug)  "RemoteConfigManager initialized successfully".logd(TAG)
                }.onFailure { e ->
                    if(BuildState.debug) "Failed to initialize RemoteConfigManager: ${e.message}".loge(TAG)
                }
            } catch (e: Exception) {
                if(BuildState.debug)  "Failed to initialize remote config: ${e.message}".loge(TAG)
            }
        }
    }

    /**
     * 注册生命周期观察者
     *
     * 必须在 AppLifecycleManager 初始化后调用
     */
    fun registerLifecycleObservers() {
        try {
            // 注册配置刷新观察器
            AppLifecycleManager.addObserver(configRefreshObserver)
            if(BuildState.debug)  "Config refresh observer registered".logd(TAG)

            // ✅ 注册健康服务前台观察器
            // 监听应用前后台切换，自动启动/管理健康服务
            AppLifecycleManager.addObserver(healthServiceForegroundObserver)
            if(BuildState.debug) "HealthServiceForegroundObserver registered".logd(TAG)

        } catch (e: Exception) {
            if(BuildState.debug) "Failed to register lifecycle observers: ${e.message}".loge(TAG)
        }
    }

}
