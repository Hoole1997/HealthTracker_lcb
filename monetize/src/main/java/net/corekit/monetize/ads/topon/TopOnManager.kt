package net.corekit.monetize.ads.topon

import android.content.Context
import com.thinkup.core.api.TUSDK
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import kotlin.coroutines.resume

/**
 * TopOn SDK 管理器
 * 
 * 负责 TopOn SDK 的初始化和状态管理
 * 使用 thinkup SDK 包名（com.thinkup.sdk）
 */
object TopOnManager {

    private const val TAG = "TopOnManager"
    
    @Volatile
    private var isInitialized = false
    
    @Volatile
    private var isInitializing = false

    /**
     * 初始化 TopOn SDK
     */
    suspend fun initialize(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnAppId()) {
            AdLogger.w("[$TAG] TopOn App ID/Key 未配置，跳过初始化")
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "TopOn App ID/Key 未配置"))
        }
        
        if (isInitialized) {
            AdLogger.d("[$TAG] TopOn SDK 已初始化")
            return AdResult.Success(Unit)
        }
        
        if (isInitializing) {
            AdLogger.d("[$TAG] TopOn SDK 正在初始化中...")
            return waitForInitialization()
        }
        
        isInitializing = true
        
        return try {
            val result = initializeSdk(context)
            isInitializing = false
            result
        } catch (e: Exception) {
            isInitializing = false
            AdLogger.e("[$TAG] TopOn SDK 初始化异常", e)
            AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "TopOn SDK 初始化异常: ${e.message}", e))
        }
    }

    private suspend fun initializeSdk(context: Context): AdResult<Unit> = 
        suspendCancellableCoroutine { continuation ->
            val appId = BuildConfig.TOPON_APPLICATION_ID
            val appKey = BuildConfig.TOPON_APP_KEY
            
            AdLogger.d("[$TAG] 开始初始化 TopOn SDK, App ID: %s", appId)
            
            try {
                // 设置调试模式
                TUSDK.setNetworkLogDebug(BuildConfig.DEBUG)
                
                // 初始化 SDK（TopOn 使用同步初始化）
                TUSDK.init(context, appId, appKey)
                
                isInitialized = true
                AdLogger.d("[$TAG] ✅ TopOn SDK 初始化成功")
                
                if (continuation.isActive) {
                    continuation.resume(AdResult.Success(Unit))
                }
            } catch (e: Exception) {
                AdLogger.e("[$TAG] ❌ TopOn SDK 初始化失败", e)
                if (continuation.isActive) {
                    continuation.resume(AdResult.Failure(AdException(AdException.ERROR_INTERNAL, e.message ?: "初始化失败")))
                }
            }
        }

    private suspend fun waitForInitialization(): AdResult<Unit> = 
        suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()
            val maxWaitTime = 10000L
            
            Thread {
                while (!isInitialized && (System.currentTimeMillis() - startTime) < maxWaitTime) {
                    Thread.sleep(100)
                }
                
                if (continuation.isActive) {
                    if (isInitialized) {
                        continuation.resume(AdResult.Success(Unit))
                    } else {
                        continuation.resume(AdResult.Failure(AdException(AdException.ERROR_TIMEOUT, "TopOn SDK 初始化超时")))
                    }
                }
            }.start()
        }

    fun isReady(): Boolean = isInitialized

    fun getSdkVersion(): String {
        return try {
            TUSDK.getSDKVersionName()
        } catch (e: Exception) {
            "unknown"
        }
    }
}
