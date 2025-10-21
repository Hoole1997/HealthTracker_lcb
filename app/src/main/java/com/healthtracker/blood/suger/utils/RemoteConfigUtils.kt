package com.healthtracker.blood.suger.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.healthtracker.blood.suger.constants.KEY_USER_COUNTRY
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.SpUtils
import java.util.concurrent.atomic.AtomicBoolean

object RemoteConfigUtils {
    private val fetchRemoteConfig = AtomicBoolean(false)
    fun fetchRemoteConfig() {
        if (BuildState.debug) "fetchRemoteConfig ======".loge("firebase")
        if (!fetchRemoteConfig.compareAndSet(false, true)) return
        if (BuildState.debug) "fetchRemoteConfig start ======".loge("firebase")

//        /// 设置默认内网环境VIP显示
//        if (BuildState.debug) {
//            SpUtils.putBoolean(Constants.KEY_SHOW_VIP_ICON, true)
//        }

        val configSettings = if (BuildState.debug) {
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(5L)
                .setFetchTimeoutInSeconds(60L).build()
        } else {
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600L)
                .setFetchTimeoutInSeconds(60L).build()
        }
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener {
                try {
                    if (it.isSuccessful) {
                        if (BuildState.debug) "fetchRemoteConfig success".logd("firebase")
                        try {
                            if(BuildState.debug){
                                for ((k, v) in remoteConfig.all) {
                                    "$k:${v?.asString()}".logd("firebase")
                                }
                            }
                        } catch (_: Throwable) {
                        }


                    } else {
                        if (BuildState.debug) "fetchRemoteConfig fail".logd("firebase")
                    }
                } finally {
                    fetchRemoteConfig.set(false)
                }
            }
    }
}