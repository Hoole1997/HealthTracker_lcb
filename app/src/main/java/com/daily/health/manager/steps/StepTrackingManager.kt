package com.daily.health.manager.steps

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 应用前台期间独立采集；退到后台释放监听，不依赖通知权限或通知服务开关。 */
class StepTrackingManager(
    private val source: StepCounterSource,
    private val persistReading: suspend (StepCounterReading) -> Unit,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    // STEP_COUNTER 事件频率低；顺序消费，避免并发读改写丢步或把乱序值误认成重启。
    private val readings = Channel<StepCounterReading>(Channel.UNLIMITED)
    private val mutableStatus = MutableStateFlow(StepTrackingStatus.STOPPED)
    val status = mutableStatus.asStateFlow()
    private var lifecycle: Lifecycle? = null
    private var foreground = false

    init {
        scope.launch {
            for (reading in readings) {
                try {
                    persistReading(reading)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("StepTrackingManager", "Unable to save step counter reading", error)
                }
            }
        }
    }

    fun initialize(lifecycle: Lifecycle = ProcessLifecycleOwner.get().lifecycle) {
        if (this.lifecycle != null) return
        this.lifecycle = lifecycle
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        foreground = true
        refresh()
    }

    override fun onStop(owner: LifecycleOwner) {
        foreground = false
        source.stop()
        mutableStatus.value = StepTrackingStatus.STOPPED
        // 不取消写入作用域，确保最后几个事件在进入后台后仍能保存。
    }

    /** 授权回调和设置页返回时重查权限；采集端负责避免重复注册。 */
    fun refresh() {
        if (!foreground) return
        mutableStatus.value = source.start { readings.trySend(it) }
    }

    internal fun close() {
        lifecycle?.removeObserver(this)
        lifecycle = null
        source.stop()
        readings.close()
        scope.cancel()
    }
}
