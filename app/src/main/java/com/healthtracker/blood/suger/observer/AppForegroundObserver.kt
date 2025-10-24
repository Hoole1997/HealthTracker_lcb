package com.healthtracker.blood.suger.observer

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.healthtracker.blood.suger.strategy.LoopPushManager
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用前后台状态观察器
 *
 * 职责：
 * 1. 监听应用的前台/后台状态变化
 * 2. 当应用切回前台时，停止所有 Loop 推送
 *
 * 使用场景：
 * - 用户收到通知后，没有点击或划掉，而是直接打开应用
 * - 此时应停止所有 Loop 推送，避免后台继续发送通知
 *
 * 集成方式：
 * - 在 App.onCreate() 中调用 initialize()
 * - 使用 ProcessLifecycleOwner 监听整个应用的生命周期
 */
@Singleton
class AppForegroundObserver @Inject constructor(
    private val loopPushManager: LoopPushManager
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AppForegroundObserver"
    }

    /**
     * 初始化观察器
     * 应在 App.onCreate() 中调用
     */
    fun initialize() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (BuildState.debug) {
            "AppForegroundObserver initialized".logd(TAG)
        }
    }

    /**
     * 应用切换到前台时调用
     *
     * ProcessLifecycleOwner.onStart() 表示应用切换到前台
     * 此时停止所有 Loop 推送
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        if (BuildState.debug) {
            "App moved to foreground, stopping all Loop pushes".logd(TAG)
        }

        // 停止所有 Loop 推送
        loopPushManager.stopAllLoopPushes("app_foreground")
    }

    /**
     * 应用切换到后台时调用
     *
     * ProcessLifecycleOwner.onStop() 表示应用切换到后台
     * 不需要特殊处理，Loop 推送会在下次推送时自然启动
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)

        if (BuildState.debug) {
            "App moved to background".logd(TAG)
        }
    }
}
