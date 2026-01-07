package com.android.common.weather.network

import android.util.Log
import com.android.common.weather.MD5Utils
import com.healthtracker.framework.BuildState
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

/**
 * 天气 API 签名拦截器
 * 自动为每个请求添加签名 Header，防止接口滥用
 * 
 * 签名算法：MD5(appKey + "-" + timestamp + "-" + appSecret + "-" + appName)
 */
class SignatureInterceptor : Interceptor {

    companion object {
        private const val TAG = "SignatureInterceptor"
        private const val APP_KEY = "weatherQuery1"
        private const val APP_SECRET = "secret1"

        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_APP_NAME = "AppName"
        private const val HEADER_VERSION = "Version"
        private const val HEADER_TIMESTAMP = "Timestamp"
        private const val HEADER_SIGN = "Sign"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 动态获取应用信息
        val appName = RetrofitClient.getAppName()
        val version = RetrofitClient.getVersion()

        // 生成当前时间戳（秒）
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        // 生成签名：MD5(appKey + "-" + timestamp + "-" + appSecret + "-" + appName)
        val sign = generateSign(APP_KEY, timestamp, APP_SECRET, appName)
        if(BuildState.debug){
            Log.d(TAG, "=== 天气 API 签名拦截器 ===")
            Log.d(TAG, "URL: ${originalRequest.url}")
            Log.d(TAG, "添加 Headers:")
            Log.d(TAG, "  $HEADER_AUTHORIZATION: $APP_KEY")
            Log.d(TAG, "  $HEADER_APP_NAME: $appName")
            Log.d(TAG, "  $HEADER_VERSION: $version")
            Log.d(TAG, "  $HEADER_TIMESTAMP: $timestamp")
            Log.d(TAG, "  $HEADER_SIGN: $sign")
        }

        // 添加签名 Header
        val newRequest = originalRequest.newBuilder()
            .addHeader(HEADER_AUTHORIZATION, APP_KEY)
            .addHeader(HEADER_APP_NAME, appName)
            .addHeader(HEADER_VERSION, version)
            .addHeader(HEADER_TIMESTAMP, timestamp)
            .addHeader(HEADER_SIGN, sign)
            .build()

        // 验证 Headers 是否添加成功
        if(BuildState.debug){
            Log.d(TAG, "请求中的所有 Headers:")
            newRequest.headers.forEach { (name, value) ->
                Log.d(TAG, "  $name: $value")
            }
        }

        return chain.proceed(newRequest)
    }

    /**
     * 生成签名
     * @param appKey 应用密钥
     * @param timestamp 时间戳
     * @param appSecret 应用密文
     * @param appName 应用包名
     * @return MD5 签名字符串
     */
    private fun generateSign(appKey: String, timestamp: String, appSecret: String, appName: String): String {
        val source = "$appKey-$timestamp-$appSecret-$appName"
        if(BuildState.debug){
            Log.d(TAG, "待签名字符串: $source")
        }
        return MD5Utils().getMD5(source)
    }

}
