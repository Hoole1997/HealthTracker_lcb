package com.healthtracker.blood.suger

import android.app.Application
import android.content.Intent
import android.os.Looper
import com.blankj.utilcode.util.ActivityUtils
import com.healthtracker.blood.suger.config.registry.AppConfigRegistry
import com.healthtracker.blood.suger.constants.KEY_APP_FIRST_START_TIME
import com.healthtracker.blood.suger.di.IoDispatcher
import com.healthtracker.blood.suger.ui.act.SplashActivity
import com.healthtracker.blood.suger.work.HealthWorkTask
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.lifecycle.AppForegroundObserver
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.LogUtils
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.hasP
import com.healthtracker.framework.util.logException
import com.healthtracker.framework.util.postRunnable
import com.knightboot.spwaitkiller.SpWaitKiller
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.LaunchAds
import org.lsposed.hiddenapibypass.HiddenApiBypass
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java

/**
 * 应用初始化器
 * 统一管理应用启动时的初始化逻辑
 * 迁移自App.kt，保持所有原有功能，使用SpUtils管理偏好设置
 */
@Singleton
class AppInitializer @Inject constructor(
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val remoteConfigManager: RemoteConfigManager,
    private val appConfigRegistry: AppConfigRegistry,
    private val healthServiceForegroundObserver: com.healthtracker.blood.suger.observer.HealthServiceForegroundObserver
) {
    
    private val initScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var isFirstLaunch = true
    /**
     * 配置刷新观察者
     *
     * 监听应用生命周期，在进入前台时自动刷新配置
     */
    private val configRefreshObserver = object : AppForegroundObserver {
        override fun onAppForeground() {
            "App entered foreground, refreshing config...".logd("AppInitializer")
            initScope.launch {
                remoteConfigManager.refreshConfig()
                //检查是否满足展示开屏广告条件
                val result = LaunchAds.getInstance().checkInterceptor(application)
                if(!isFirstLaunch && result is AdResult.Success){
                    startSplashActivity()
                }else{
                    isFirstLaunch = false
                }
            }



        }
    }

    fun startSplashActivity() {
        try {
            val intent = Intent(application, SplashActivity::class.java)
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
        BuildState.debug == BuildConfig.DEBUG
        // 1. 核心同步初始化 (原onCreate中的同步部分)
        initializeCoreServices()

        // 2. 延迟异步初始化 (原UIUtils.postRunnable中的部分)
        initializeDelayedServices()

        // 3. 架构验证初始化
        initializeArchitectureValidation()

        // 4. 远程配置初始化
        initializeRemoteConfig()
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
            SpUtils.init(application)
            
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
            //反射处理sp造成主线程阻塞问
            try {
                if (hasP()) {
                    HiddenApiBypass.addHiddenApiExemptions("")
                }
                SpWaitKiller.builder(application).build().work()

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
                "Config parsers registered: ${appConfigRegistry.getRegisteredCount()}".logd("AppInitializer")

                // 2. 初始化 RemoteConfigManager
                val result = remoteConfigManager.initialize()
                result.onSuccess {
                    "RemoteConfigManager initialized successfully".logd("AppInitializer")
                }.onFailure { e ->
                    "Failed to initialize RemoteConfigManager: ${e.message}".loge("AppInitializer")
                }
            } catch (e: Exception) {
                "Failed to initialize remote config: ${e.message}".loge("AppInitializer")
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
            "Config refresh observer registered".logd("AppInitializer")

            // ✅ 注册健康服务前台观察器
            // 监听应用前后台切换，自动启动/管理健康服务
            AppLifecycleManager.addObserver(healthServiceForegroundObserver)
            "HealthServiceForegroundObserver registered".logd("AppInitializer")

        } catch (e: Exception) {
            "Failed to register lifecycle observers: ${e.message}".loge("AppInitializer")
        }
    }
} 