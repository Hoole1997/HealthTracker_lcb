package com.android.common.weather

import android.content.Context
import com.android.common.weather.network.RetrofitClient

/**
 * 天气模块初始化器
 * 在 Application 中调用 init() 方法完成初始化
 */
object WeatherInitializer {

    private var isInitialized = false

    /**
     * 初始化天气模块
     *
     * @param context 应用上下文
     * @param enableLog 是否启用日志打印，默认为 false
     */
    fun init(context: Context, enableLog: Boolean = false) {
        if (isInitialized) {
            return
        }

        // 设置 Retrofit 日志开关
        RetrofitClient.setLogEnabled(enableLog)

        isInitialized = true
    }

    /**
     * 判断模块是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized
}
