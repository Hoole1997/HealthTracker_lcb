package com.healthtracker.blood.suger

import android.app.Application
import android.os.Looper
import com.healthtracker.blood.suger.App.Companion.isInBackground
import com.healthtracker.blood.suger.constants.KEY_APP_FIRST_START_TIME
import com.healthtracker.blood.suger.di.IoDispatcher
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.util.LogUtils
import com.healthtracker.framework.util.LogUtils.logException
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.hasP
import com.healthtracker.framework.util.postRunnable
import com.knightboot.spwaitkiller.SpWaitKiller
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用初始化器
 * 统一管理应用启动时的初始化逻辑
 * 迁移自App.kt，保持所有原有功能，使用SpUtils管理偏好设置
 */
@Singleton
class AppInitializer @Inject constructor(
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    
    private val initScope = CoroutineScope(SupervisorJob() + ioDispatcher)
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
//                logException(exception)
                return@setDefaultUncaughtExceptionHandler
            }
            if (isInBackground) {
//                logException(exception)
            } else {
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }
} 