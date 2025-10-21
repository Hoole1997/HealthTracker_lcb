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

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .build()
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener {
                try {
                    if (it.isSuccessful) {
                        if (BuildState.debug) "fetchRemoteConfig success".logd("firebase")
                        try {
                            val sFileListNum = remoteConfig.getString("filelist_num").trim()
                            val count = sFileListNum.toInt()
                            if (BuildState.debug) "获取到文件间隔数量-->${count}".loge("firebase")
                        } catch (_: Throwable) {
                        }



                        try {
                            val userCountry = remoteConfig.getString("user_country").trim()
                            if (userCountry.isNotEmpty()) {
                                SpUtils.putString(KEY_USER_COUNTRY, userCountry)
                            }
                        } catch (_: Throwable) {
                            if (BuildState.debug) "user_country fail".logd("firebase")
                        }


                    } else {
                        if (BuildState.debug) "fetchRemoteConfig fail".logd("firebase")

//                        SpUtils.putBoolean(Constants.GET_FIREBASE_CONFIG, false)
                    }
                } finally {
                    fetchRemoteConfig.set(false)
                }
            }

        // PlacementManager.onRemoteAdGroupTick() - 广告系统已删除
    }
}