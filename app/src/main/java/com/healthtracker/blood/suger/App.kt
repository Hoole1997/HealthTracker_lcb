package com.healthtracker.blood.suger

import android.content.Context
import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.healthtracker.blood.suger.utils.WebViewZygote
import com.healthtracker.blood.suger.utils.getCurProcessName
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class App : MultiDexApplication() {

    @Inject
    lateinit var appInitializer: AppInitializer


    companion object {
        lateinit var INSTANCE: App
            private set
        var isInBackground = true
    }

    /**
     * 主进程检查缓存
     * 对应原App.kt中的isMainProcess逻辑
     */
    private var isMainProcess: Boolean? = null

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        // 1. 设置静态实例 (对应原App.kt中的INSTANCE = this)
        INSTANCE = this

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
            // 应用初始化将通过AppInitializer统一管理
            appInitializer.initialize()
            initProcessLifeCycle()
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

    private fun initProcessLifeCycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        if (isInBackground) {
                            isInBackground = false

                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        isInBackground = true
                    }

                    else -> {

                    }
                }
            }
        })
    }
}