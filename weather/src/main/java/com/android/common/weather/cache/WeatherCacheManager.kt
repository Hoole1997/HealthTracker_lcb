package com.android.common.weather.cache

import com.android.common.weather.model.WeatherResponse
import com.android.common.weather.network.RetrofitClient
import com.google.gson.Gson
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.SpUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 天气数据缓存管理器
 * 使用 MMKV 持久化缓存，有效期 5 分钟
 * 
 * 缓存策略：
 * 1. 缓存有效时直接返回缓存，不发起网络请求
 * 2. 缓存过期时：有 locationKey 按 key 请求，无缓存按 IP 请求
 * 3. 搜索弹窗选中城市时使旧缓存过期
 * 4. 使用 Mutex + Deferred 实现请求并发控制，避免重复请求
 */
object WeatherCacheManager {

    private const val TAG = "WeatherCacheManager"
    private const val KEY_WEATHER_CACHE = "weather_cache_json"
    private const val KEY_CACHE_TIME = "weather_cache_time"
    private const val CACHE_DURATION = 5 * 60 * 1000L  // 5 分钟

    private val gson = Gson()
    private val mutex = Mutex()

    // 内存缓存（避免重复反序列化）
    private var memoryCache: WeatherResponse? = null
    private var memoryCacheTime: Long = 0L
    
    // 请求并发控制：正在进行的请求
    private var pendingRequest: Deferred<WeatherResponse?>? = null

    /**
     * 检查缓存是否有效
     */
    fun isCacheValid(): Boolean {
        val cacheTime = SpUtils.getLong(KEY_CACHE_TIME, 0L)
        val isValid = System.currentTimeMillis() - cacheTime < CACHE_DURATION
        "Cache valid: $isValid (age: ${(System.currentTimeMillis() - cacheTime) / 1000}s)".logd(TAG)
        return isValid
    }

    /**
     * 获取缓存的天气数据
     */
    fun getCachedResponse(): WeatherResponse? {
        // 先检查内存缓存
        if (memoryCache != null && System.currentTimeMillis() - memoryCacheTime < CACHE_DURATION) {
            "Using memory cache".logd(TAG)
            return memoryCache
        }

        // 从 MMKV 读取
        val json = SpUtils.getString(KEY_WEATHER_CACHE)
        if (json.isBlank()) {
            "No cache found".logd(TAG)
            return null
        }

        return try {
            val response = gson.fromJson(json, WeatherResponse::class.java)
            // 更新内存缓存
            memoryCache = response
            memoryCacheTime = SpUtils.getLong(KEY_CACHE_TIME, 0L)
            "Loaded cache from MMKV".logd(TAG)
            response
        } catch (e: Exception) {
            "Failed to parse cache: ${e.message}".loge(TAG)
            null
        }
    }
    
    /**
     * 获取缓存的 locationKey
     * @return 缓存的 locationKey，如果缓存不存在则返回 null
     */
    fun getCachedLocationKey(): String? {
        val cached = getCachedResponse()
        return cached?.locationKey.also {
            "Cached locationKey: $it".logd(TAG)
        }
    }

    /**
     * 保存缓存数据
     */
    fun saveCache(response: WeatherResponse) {
        try {
            val json = gson.toJson(response)
            val currentTime = System.currentTimeMillis()
            SpUtils.putString(KEY_WEATHER_CACHE, json)
            SpUtils.putLong(KEY_CACHE_TIME, currentTime)
            // 更新内存缓存
            memoryCache = response
            memoryCacheTime = currentTime
            "Cache saved successfully".logd(TAG)
        } catch (e: Exception) {
            "Failed to save cache: ${e.message}".loge(TAG)
        }
    }
    
    /**
     * 使缓存过期（将缓存时间置 0）
     * 保留缓存数据以便在请求失败时回退显示
     */
    fun invalidateCache() {
        SpUtils.putLong(KEY_CACHE_TIME, 0L)
        memoryCacheTime = 0L
        "Cache invalidated".logd(TAG)
    }

    /**
     * 获取天气数据（自动判断缓存）
     * 
     * 策略：
     * 1. 缓存有效时直接返回缓存，不发起网络请求
     * 2. 缓存过期 + 有 locationKey：按 locationKey 请求
     * 3. 无缓存：按 IP 请求
     * 4. 请求失败时：无缓存回退到 IP 请求；有缓存返回过期缓存
     * 
     * @param forceRefresh 是否强制刷新（忽略缓存有效期）
     * @return 天气数据，失败时返回 null
     */
    suspend fun getWeatherData(forceRefresh: Boolean = false): WeatherResponse? {
        return mutex.withLock {
            // 如果有正在进行的请求，等待其完成并复用结果
            pendingRequest?.let { request ->
                "Waiting for pending request...".logd(TAG)
                return@withLock try {
                    request.await()
                } catch (e: Exception) {
                    "Pending request failed: ${e.message}".loge(TAG)
                    null
                }
            }
            
            withContext(Dispatchers.IO) {
                // 检查缓存有效性（非强制刷新时）
                if (!forceRefresh && isCacheValid()) {
                    val cached = getCachedResponse()
                    if (cached != null) {
                        "Returning valid cached weather data".logd(TAG)
                        return@withContext cached
                    }
                }

                // 缓存过期或无缓存，需要请求新数据
                val cachedLocationKey = getCachedLocationKey()
                val cachedResponse = getCachedResponse()
                
                // 创建 Deferred 用于并发控制
                val deferred = CompletableDeferred<WeatherResponse?>()
                pendingRequest = deferred
                
                try {
                    val response = if (cachedLocationKey != null) {
                        // 有 locationKey，按 key 请求
                        "Fetching weather data by locationKey: $cachedLocationKey".logd(TAG)
                        try {
                            val keyResponse = RetrofitClient.weatherApi.getWeatherByKey(locationKey = cachedLocationKey)
                            if (keyResponse.isSuccess()) {
                                // 如果新响应没有 City 信息，但旧缓存有，尝试合并
                                if (keyResponse.city == null && cachedResponse?.city != null) {
                                    "Merging cached city info into new response".logd(TAG)
                                    keyResponse.copy(city = cachedResponse.city)
                                } else {
                                    keyResponse
                                }
                            } else {
                                "Request by locationKey failed (code=${keyResponse.code})".loge(TAG)
                                // 有缓存时不回退到 IP 请求，返回过期缓存
                                if (cachedResponse != null) {
                                    "Returning expired cache".logd(TAG)
                                    cachedResponse
                                } else {
                                    // 无缓存时回退到 IP 请求
                                    "Falling back to IP request".logd(TAG)
                                    fetchByIP()
                                }
                            }
                        } catch (e: Exception) {
                            "Request by locationKey exception: ${e.message}".loge(TAG)
                            if (cachedResponse != null) {
                                "Returning expired cache".logd(TAG)
                                cachedResponse
                            } else {
                                "Falling back to IP request".logd(TAG)
                                fetchByIP()
                            }
                        }
                    } else {
                        // 无 locationKey，按 IP 请求
                        "Fetching weather data by IP".logd(TAG)
                        fetchByIP()
                    }
                    
                    // 保存缓存（如果请求成功）
                    if (response != null && response.isSuccess()) {
                        saveCache(response)
                    }
                    
                    deferred.complete(response)
                    response
                } catch (e: Exception) {
                    "Weather request failed: ${e.message}".loge(TAG)
                    deferred.complete(null)
                    // 返回过期缓存（如果有）
                    cachedResponse
                } finally {
                    pendingRequest = null
                }
            }
        }
    }
    
    /**
     * 通过 IP 请求天气数据
     */
    private suspend fun fetchByIP(): WeatherResponse? {
        return try {
            val response = RetrofitClient.weatherApi.getWeatherByIP()
            if (response.isSuccess()) {
                "IP request succeeded".logd(TAG)
                response
            } else {
                "IP request failed (code=${response.code})".loge(TAG)
                null
            }
        } catch (e: Exception) {
            "IP request exception: ${e.message}".loge(TAG)
            null
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        SpUtils.remove(KEY_WEATHER_CACHE)
        SpUtils.remove(KEY_CACHE_TIME)
        memoryCache = null
        memoryCacheTime = 0L
        "Cache cleared".logd(TAG)
    }
}
