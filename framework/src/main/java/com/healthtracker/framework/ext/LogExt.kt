package com.healthtracker.framework.ext

import android.util.Log
import com.healthtracker.framework.BuildConfig
import com.healthtracker.framework.BuildState

const val TAG = "KLog"

private enum class LEVEL {
    V, D, I, W, E
}

fun String.logv(tag: String = TAG) =
    if (BuildState.debug && BuildConfig.DEBUG) log(LEVEL.V, tag, this) else Unit

fun String.logd(tag: String = TAG) =
    if (BuildState.debug && BuildConfig.DEBUG) log(LEVEL.D, tag, this) else Unit

fun String.logi(tag: String = TAG) =
    if (BuildState.debug && BuildConfig.DEBUG) log(LEVEL.I, tag, this) else Unit

fun String.logResponse(tag: String = TAG) =
    if (BuildState.debug && BuildConfig.DEBUG) log(LEVEL.I, tag, this, true) else Unit

fun String.logw(tag: String = TAG) =
    if (BuildState.debug && BuildConfig.DEBUG) log(LEVEL.W, tag, this) else Unit

fun String.loge(tag: String = TAG) =
    if (BuildState.debug && BuildConfig.DEBUG) log(LEVEL.E, tag, this) else Unit

private fun log(level: LEVEL, tag: String, message: String, sub: Boolean = false) {
    when (level) {
        LEVEL.V -> Log.v(tag, message)
        LEVEL.D -> Log.d(tag, message)
        LEVEL.I -> {
            if (sub) {
                val maxLogSize = 3000
                for (i in 0..message.length / maxLogSize) {
                    val start = i * maxLogSize
                    val end =
                        if ((i + 1) * maxLogSize > message.length) message.length else (i + 1) * maxLogSize
                    if (i == 0) {
                        Log.i(tag, "[Buss] response:${message.substring(start, end)}")
                    } else {
                        Log.i(tag, message.substring(start, end))
                    }
                }
            } else {
                Log.i(tag, message)
            }
        }

        LEVEL.W -> Log.w(tag, message)
        LEVEL.E -> Log.e(tag, message)
    }
}