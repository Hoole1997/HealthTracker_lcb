package com.healthtracker.framework.util

import android.annotation.SuppressLint
import android.os.Build
@SuppressLint("AnnotateVersionCheck")
fun isLeast8() =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.O


@SuppressLint("AnnotateVersionCheck")
fun isLeast9(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
}

@SuppressLint("AnnotateVersionCheck")
fun isLeast10(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}

@SuppressLint("AnnotateVersionCheck")
fun isLeast11(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
}

@SuppressLint("AnnotateVersionCheck")
fun isLeast12(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

@SuppressLint("AnnotateVersionCheck")
fun isLeast13(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

@SuppressLint("AnnotateVersionCheck")
fun isLeast14() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

@SuppressLint("AnnotateVersionCheck")
fun isLeast15() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM