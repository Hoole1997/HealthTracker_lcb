package com.healthtracker.blood.suger

import android.util.Log
import androidx.multidex.MultiDexApplication

class App:MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        Log.e("APP","test message")
    }
}