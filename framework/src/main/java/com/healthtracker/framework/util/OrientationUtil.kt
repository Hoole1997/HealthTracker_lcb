package com.healthtracker.framework.util

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build

@SuppressLint("SourceLockedOrientationActivity")
fun Activity.requestOrientation() {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
        if (ActivityCore.isTranslucentOrFloating(this)) {
            ActivityCore.fixOrientation(this)
        } else {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    } else {
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}