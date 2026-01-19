package net.corekit.monetize.ads.pangle

import android.app.Application
import android.content.Context
import com.bytedance.sdk.openadsdk.api.init.PAGConfig
import com.bytedance.sdk.openadsdk.api.init.PAGSdk
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdErrorCode
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import kotlin.coroutines.resume

/**
 * Pangle SDK 管理器
 * 
 * 负责 Pangle SDK 的初始化和状态管理
 */
object PangleManager {

    private const val TAG = "PangleManager"
    
    @Volatile
    private var isInitialized = false
    
    @Volatile
    private var isInitializing = false

    /**
     * 初始化 Pangle SDK
     * 
     * @param context Application Context
     * @return 初始化结果
     */
    suspend fun initialize(context: Context): AdResult<Unit> {
        // 检查是否有有效的 App ID
        if (!AdIdHelper.hasPangleAppId()) {
            AdLogger.w("[$TAG] Pangle App ID 未配置，跳过初始化")
            return AdResult.Failure(AdErrorCode.PANGLE_APP_ID_NOT_CONFIGURED.toAdException())
        }
        
        // 已初始化，直接返回成功
        if (isInitialized) {
            AdLogger.d("[$TAG] Pangle SDK 已初始化")
            return AdResult.Success(Unit)
        }
        
        // 正在初始化中，等待完成
        if (isInitializing) {
            AdLogger.d("[$TAG] Pangle SDK 正在初始化中...")
            return waitForInitialization()
        }
        
        isInitializing = true
        
        return try {
            val result = initializeSdk(context.applicationContext as Application)
            isInitializing = false
            result
        } catch (e: Exception) {
            isInitializing = false
            AdLogger.e("[$TAG] Pangle SDK 初始化异常", e)
            AdResult.Failure(AdErrorCode.SDK_INIT_EXCEPTION.toAdException(e))
        }
    }

    /**
     * 执行 SDK 初始化
     */
    private suspend fun initializeSdk(application: Application): AdResult<Unit> = 
        suspendCancellableCoroutine { continuation ->
            val appId = BuildConfig.PANGLE_APPLICATION_ID
            
            AdLogger.d("[$TAG] 开始初始化 Pangle SDK, App ID: %s", appId)
            
            val config = PAGConfig.Builder()
                .appId(appId)
                .debugLog(BuildConfig.DEBUG)
                .build()
            
            PAGSdk.init(application, config, object : PAGSdk.PAGInitCallback {
                override fun success() {
                    isInitialized = true
                    AdLogger.d("[$TAG] ✅ Pangle SDK 初始化成功")
                    if (continuation.isActive) {
                        continuation.resume(AdResult.Success(Unit))
                    }
                }

                override fun fail(code: Int, message: String?) {
                    AdLogger.e("[$TAG] ❌ Pangle SDK 初始化失败: code=%d, message=%s", code, message)
                    if (continuation.isActive) {
                        continuation.resume(AdResult.Failure(AdErrorCode.SDK_INIT_FAILED.toAdException()))
                    }
                }
            })
        }

    /**
     * 等待初始化完成
     */
    private suspend fun waitForInitialization(): AdResult<Unit> = 
        suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()
            val maxWaitTime = 10000L // 最多等待 10 秒
            
            Thread {
                while (!isInitialized && (System.currentTimeMillis() - startTime) < maxWaitTime) {
                    Thread.sleep(100)
                }
                
                if (continuation.isActive) {
                    if (isInitialized) {
                        continuation.resume(AdResult.Success(Unit))
                    } else {
                        continuation.resume(AdResult.Failure(AdErrorCode.SDK_INIT_TIMEOUT.toAdException()))
                    }
                }
            }.start()
        }

    /**
     * 检查 SDK 是否已初始化
     */
    fun isReady(): Boolean = isInitialized

    /**
     * 获取 SDK 版本号
     */
    fun getSdkVersion(): String {
        return try {
            PAGSdk.getSDKVersion()
        } catch (e: Exception) {
            "unknown"
        }
    }
}

