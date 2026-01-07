package net.corekit.monetize.ump

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.corekit.monetize.ads.log.AdLogger
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * IP 地理位置检测服务
 * 
 * 使用 api.country.is 查询用户所在国家
 * 用于判断是否需要展示 UMP 同意弹窗
 */
object GeoLocationService {
    
    private const val TAG = "GeoLocation"
    private const val API_URL = "https://api.country.is/"
    
    /** IP 查询超时时间（毫秒） */
    private const val TIMEOUT_MS = 3000L
    
    /** 连接超时时间（毫秒） */
    private const val CONNECT_TIMEOUT_MS = 2000
    
    /** 读取超时时间（毫秒） */
    private const val READ_TIMEOUT_MS = 2000
    
    /**
     * 查询当前用户所在国家代码
     * 
     * @return 国家代码（如 "US", "DE", "CN"），失败返回 null
     */
    suspend fun getCountryCode(): String? = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(TIMEOUT_MS) {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/json")
                    // 禁用缓存，确保获取最新 IP 位置
                    useCaches = false
                }
                
                try {
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        val countryCode = json.optString("country", null)
                        if (!countryCode.isNullOrBlank()) {
                            AdLogger.d("[$TAG] IP 查询成功，国家代码: $countryCode")
                            countryCode.uppercase()
                        } else {
                            AdLogger.w("[$TAG] IP 查询成功但国家代码为空")
                            null
                        }
                    } else {
                        AdLogger.w("[$TAG] IP 查询失败，HTTP 状态码: $responseCode")
                        null
                    }
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] IP 查询异常: ${e.message}")
            null
        }
    }
}
