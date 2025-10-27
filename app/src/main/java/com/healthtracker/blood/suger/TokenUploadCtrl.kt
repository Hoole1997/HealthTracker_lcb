package com.healthtracker.blood.suger

import com.blankj.utilcode.util.SPUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.util.SpUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Token 上报控制器
 * 负责 FCM Token 的上传和状态管理
 */
object TokenUploadCtrl {

    private const val TAG = "TokenUploadCtrl"
    private const val PREFS_NAME = "token_upload_status"
    private const val KEY_UPLOAD_STATUS_MAP = "upload_status_map"
    private const val SECRET_KEY = "J22W7bkOCr9nz0xdJlhXMs6OuxF2l0So"
    
    // 请求参数名
    private const val PARAM_TOKEN = "vie"      // token 参数
    private const val PARAM_UID = "idv"      // userid 参数
    private const val PARAM_PACK = "pack"    // package 参数
    private const val HEADER_SIG = "cks"     // 签名 header

    // UUID 持久化存储
    private var uuid  =  SpUtils.getString("uuuuuuuuii1212ld","")

    // 全局参数
    private val baseUrl: String = BuildConfig.FCM_URL + "/docv/tokenup"
    private val gson = Gson()
    
    init {
        // 确保 UUID 已生成
        if (uuid.isEmpty()) {
            val javaUuid = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            uuid = "${javaUuid}-${timestamp}"
            if(BuildState.debug) "生成新的 UUID: $uuid".logd(TAG)
        }
    }

    /**
     * 上传 Token 到服务器
     * @param token FCM Token
     */
    fun uploadToken(token: String) {
        // 检查是否已经上传成功
        if (isTokenUploaded(token)) {
            return
        }

        val userid = uuid ?: ""
        val pkg = BuildConfig.APPLICATION_ID

        if(BuildState.debug) "开始上传 Token".logd(TAG)
        if(BuildState.debug) "baseUrl: $baseUrl".logd(TAG)
        if(BuildState.debug) "token: $token".logd(TAG)
        if(BuildState.debug) "userid: $userid".logd(TAG)
        if(BuildState.debug) "pkg: $pkg".logd(TAG)

        // 使用协程在后台线程执行网络请求
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 生成签名
                val rsig = generateSignature(token, userid, pkg)

                // 构建完整的请求URL
                val requestUrl = "$baseUrl?$PARAM_TOKEN=$token&$PARAM_UID=$userid&$PARAM_PACK=$pkg"

                if(BuildState.debug) "请求URL: $requestUrl".logd(TAG)
                if(BuildState.debug) "签名: $rsig".logd(TAG)

                // 创建 OkHttpClient
                val clientBuilder = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)

                // 如果日志开启，添加日志拦截器
                if (BuildState.debug) {
                    val loggingInterceptor = HttpLoggingInterceptor { message ->
                        if (BuildState.debug) "OkHttp: $message".logd(TAG)
                    }.apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    clientBuilder.addInterceptor(loggingInterceptor)
                }

                val client = clientBuilder.build()

                // 构建请求
                val request = Request.Builder()
                    .url(requestUrl)
                    .get()
                    .addHeader(HEADER_SIG, rsig)
                    .build()

                // 执行请求
                val response = client.newCall(request).execute()

                response.use {
                    val responseCode = it.code
                    val responseBody = it.body?.string() ?: ""

                    if(BuildState.debug) "响应码: $responseCode".logd(TAG)
                    if(BuildState.debug) "响应内容: $responseBody".logd(TAG)

                    val isSuccess = if (responseBody.isNotEmpty()) {
                        try {
                            // 尝试解析 JSON 响应
                            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                            
                            if (jsonObject.has("code")) {
                                // 存在 code 字段，需要同时满足 HTTP 200 和 code 0
                                val codeValue = jsonObject.get("code").asInt
                                val httpOk = it.isSuccessful
                                val codeOk = codeValue == 0
                                
                                if(BuildState.debug) "HTTP状态: $httpOk, Code字段: $codeValue, Code状态: $codeOk".logd(TAG)
                                httpOk && codeOk
                            } else {
                                // 不存在 code 字段，只判断 HTTP 响应码
                                if(BuildState.debug) "响应中无code字段，仅判断HTTP状态: ${it.isSuccessful}".logd(TAG)
                                it.isSuccessful
                            }
                        } catch (e: Exception) {
                            // JSON 解析失败，回退到只判断 HTTP 状态码
                            if(BuildState.debug) "JSON解析失败，回退到HTTP状态判断: ${it.isSuccessful}".logd(TAG)
                            it.isSuccessful
                        }
                    } else {
                        // 响应体为空，只判断 HTTP 状态码
                        if(BuildState.debug) "响应体为空，仅判断HTTP状态: ${it.isSuccessful}".logd(TAG)
                        it.isSuccessful
                    }

                    if(BuildState.debug) "上传成功: $isSuccess".logd(TAG)
                    
                    if (isSuccess) {
                        if(BuildState.debug) "Token 上传成功".logd(TAG)
                        // 标记为上传成功
                        saveUploadStatus(userid, token, true)
//                        ReportDataManager.reportData("fcm_token_report_suc", mapOf("userid" to userid,"token" to token,"pkg" to pkg,"sig" to rsig))
                    } else {
                        if(BuildState.debug) "Token 上传失败".logd(TAG)
                        // 标记为上传失败
                        saveUploadStatus(userid, token, false)
//                        ReportDataManager.reportData("fcm_token_report_fail", mapOf("userid" to userid,"token" to token,"pkg" to pkg,"sig" to rsig))
                    }
                }

            } catch (e: Exception) {
                if(BuildState.debug) "Token 上传异常: ${e.message}".logd(TAG)
                e.printStackTrace()
                // 标记为上传失败
                saveUploadStatus(uuid ?: "", token, false)
            }
        }
    }

    /**
     * 检查 Token 是否已成功上传
     * @param token FCM Token
     * @return true 表示已成功上传，false 表示未上传或上传失败
     */
    fun isTokenUploaded(token: String): Boolean {
        val key = "${uuid ?: ""}_$token"
        val statusMap = getUploadStatusMap()
        return statusMap[key] == true
    }

    /**
     * 保存 Token 上传状态
     * @param userid 用户ID
     * @param token FCM Token
     * @param success 是否上传成功
     */
    private fun saveUploadStatus(userid: String, token: String, success: Boolean) {
        val key = "${userid}_$token"
        val statusMap = getUploadStatusMap().toMutableMap()
        statusMap[key] = success

        // 保存到 SharedPreferences
        val json = gson.toJson(statusMap)
        SPUtils.getInstance(PREFS_NAME).put(KEY_UPLOAD_STATUS_MAP, json)

        if(BuildState.debug) "保存上传状态: key=$key, success=$success".logd(TAG)
    }

    /**
     * 获取 Token 上传状态映射表
     * @return Map<String, Boolean> key为 "uuid_token"，value为上传状态
     */
    private fun getUploadStatusMap(): Map<String, Boolean> {
        val json = SPUtils.getInstance(PREFS_NAME).getString(KEY_UPLOAD_STATUS_MAP, null) ?: return emptyMap()

        return try {
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            if(BuildState.debug) "解析上传状态失败: ${e.message}".logd(TAG)
            emptyMap()
        }
    }

    /**
     * 清除所有上传状态记录
     */
    fun clearUploadStatus() {
        SPUtils.getInstance(PREFS_NAME).remove(KEY_UPLOAD_STATUS_MAP)
        if(BuildState.debug) "清除所有上传状态".logd(TAG)
    }

    /**
     * 生成请求签名
     * 规则：固定密钥 + 参数按字母升序排序拼接 + MD5
     */
    private fun generateSignature(token: String, userid: String, pkg: String): String {
        // 创建参数Map并按字母升序排序
        val params = mapOf(
            PARAM_PACK to pkg,
            PARAM_TOKEN to token,
            PARAM_UID to userid
        )

        // 按key的字母顺序排序并拼接参数
        val sortedParams = params.toSortedMap()
        val paramString = sortedParams.map { "${it.key}=${it.value}" }.joinToString("&")

        // 拼接固定密钥和参数
        val signString = SECRET_KEY + paramString

        if(BuildState.debug) "签名原始字符串: $signString".logd(TAG)

        // 计算MD5
        return md5(signString)
    }

    /**
     * 计算字符串的MD5值
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 获取所有上传状态（用于调试）
     * @return Map<String, Boolean>
     */
    fun getAllUploadStatus(): Map<String, Boolean> {
        return getUploadStatusMap()
    }
}

