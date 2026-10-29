package com.daily.health.manager.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/** 仅持有 Application Context；计步不启动 Service，也不访问通知模块。 */
class AndroidStepCounterSource(context: Context) : StepCounterSource {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var listener: SensorEventListener? = null

    override fun start(onReading: (StepCounterReading) -> Unit): StepTrackingStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACTIVITY_RECOGNITION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            stop()
            return StepTrackingStatus.PERMISSION_REQUIRED
        }
        if (listener != null) return StepTrackingStatus.ACTIVE

        return try {
            val manager = sensorManager ?: return StepTrackingStatus.SENSOR_UNAVAILABLE
            val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                ?: return StepTrackingStatus.SENSOR_UNAVAILABLE
            val bootCount = runCatching {
                Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT)
            }.getOrDefault(StepCounterReading.UNKNOWN_BOOT_COUNT)
            val newListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val raw = event.values.firstOrNull() ?: return
                    if (!raw.isFinite() || raw < 0 || raw.toDouble() > Int.MAX_VALUE) return
                    // 批量上报可能跨过午夜，按事件发生时间归属日期，而不是数据库写入时间。
                    val delayMillis = ((SystemClock.elapsedRealtimeNanos() - event.timestamp) / 1_000_000L)
                        .coerceAtLeast(0)
                    onReading(StepCounterReading(raw.toInt(), bootCount, System.currentTimeMillis() - delayMillis))
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            if (manager.registerListener(newListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, Handler(Looper.getMainLooper()))) {
                listener = newListener
                StepTrackingStatus.ACTIVE
            } else {
                StepTrackingStatus.ERROR
            }
        } catch (error: RuntimeException) {
            Log.e(TAG, "Unable to register step counter", error)
            stop()
            StepTrackingStatus.ERROR
        }
    }

    override fun stop() {
        val current = listener ?: return
        listener = null
        try {
            sensorManager?.unregisterListener(current)
        } catch (error: RuntimeException) {
            Log.w(TAG, "Unable to unregister step counter", error)
        }
    }

    private companion object {
        const val TAG = "StepCounterSource"
    }
}
