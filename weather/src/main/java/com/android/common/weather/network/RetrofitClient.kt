package com.android.common.weather.network

import com.android.common.weather.model.WeatherResponse
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit 天气 API Service
 * 定义所有天气相关的接口
 */
interface WeatherApiService {

    /**
     * 通过 IP 查询天气
     * op=1: 自动根据用户 IP 定位查询天气
     */
    @GET("/query?op=1")
    suspend fun getWeatherByIP(): WeatherResponse

    /**
     * 通过 LocationKey 查询天气
     * op=2: 使用地点唯一标识码查询具体城市的天气信息
     * 
     * @param locationKey 地点唯一标识码（从地名查询或IP查询结果中获取）
     */
    @GET("/query")
    suspend fun getWeatherByKey(
        @Query("op") op: String = "2",
        @Query("key") locationKey: String
    ): WeatherResponse

    /**
     * 通过地名查询天气
     * op=3: 通过地名搜索城市，返回多个匹配结果（如果有）
     * 
     * @param address 地名（不需要 URL 编码，Retrofit 会自动处理）
     */
    @GET("/query")
    suspend fun getWeatherByAddress(
        @Query("op") op: String = "3",
        @Query("addr") address: String
    ): WeatherResponse
}

/**
 * Retrofit 客户端单例
 * 管理 Retrofit 实例和 API Service
 * 
 * 注意：CurrentConditions 和 DailyForecasts 字段使用 @JsonAdapter 注解
 * 自动处理字符串到对象的转换，无需在此手动注册 TypeAdapter
 */
object RetrofitClient {

    private const val BASE_URL = "https://api.blazelabstudio.com"

    // 在首次访问前设置，之后不可更改
    private var logEnabled: Boolean = true

    /**
     * OkHttpClient 实例（单例）
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(SignatureInterceptor())
            .apply {
                if (logEnabled) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(loggingInterceptor)
                }
            }
            .build()
    }

    /**
     * Retrofit 实例（单例）
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
    }

    /**
     * WeatherApiService 实例（单例）
     */
    val weatherApi: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }

    /**
     * 设置是否启用日志
     * 注意：必须在首次使用 weatherApi 之前调用才有效
     * @param enabled 是否启用日志
     */
    fun setLogEnabled(enabled: Boolean) {
        logEnabled = enabled
    }
}
