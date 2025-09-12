package com.healthtracker.framework.util

import android.os.Handler
import android.os.Looper

fun postRunnable(runnable: Runnable) = Handler(Looper.getMainLooper()).post(runnable)

fun postDelayed(delay: Long, runnable: Runnable) = Handler(Looper.getMainLooper()).postDelayed(runnable, delay)