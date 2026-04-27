package com.healthtracker.earthquake

import android.content.Context
import android.view.ViewGroup

object EarthquakeAdBridge {
    var nativeAdLoader: (suspend (Context, ViewGroup) -> Boolean)? = null

    suspend fun showNativeAd(context: Context, container: ViewGroup): Boolean {
        return nativeAdLoader?.invoke(context, container) ?: false
    }
}
