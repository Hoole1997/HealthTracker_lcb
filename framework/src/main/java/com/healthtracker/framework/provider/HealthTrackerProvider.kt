package com.healthtracker.framework.provider

import android.content.Context
import android.content.pm.ProviderInfo
import androidx.core.content.FileProvider

class HealthTrackerProvider : FileProvider(){
    override fun attachInfo(context: Context, info: ProviderInfo) {
        super.attachInfo(getDPContext(context), info)
    }

    companion object{
        fun getDPContext(context: Context): Context {
            var storageContext = context
            if(!context.isDeviceProtectedStorage){
                val deviceContext = context.createDeviceProtectedStorageContext()
                storageContext = deviceContext
            }
            return storageContext
        }
    }
}