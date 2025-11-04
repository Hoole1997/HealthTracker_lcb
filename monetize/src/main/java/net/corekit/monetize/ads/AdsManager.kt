package net.corekit.monetize.ads

import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CompletableDeferred
import net.corekit.monetize.ads.log.AdLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.monetize.BuildConfig
import kotlin.coroutines.resume

/**
 * AdMob SDK 管理器
 * 负责SDK初始化和全局配置
 */
object AdsManager {
    
    private const val TAG = "AdsManager"
    
    private val _initializationState = MutableStateFlow<AdResult<Unit>>(AdResult.Loading)
    val initializationState: StateFlow<AdResult<Unit>> = _initializationState.asStateFlow()
    private val initDeferred = CompletableDeferred<Boolean>()
    private var isInitialized = false
    
    /**
     * 初始化 AdMob SDK
     */
    suspend fun init(context: Context): AdResult<Unit> {
        if (isInitialized) {
            return AdResult.Success(Unit)
        }
        
        return suspendCancellableCoroutine { continuation ->
            _initializationState.value = AdResult.Loading
            val initConfig = InitializationConfig.Builder(BuildConfig.ADMOB_APPLICATION_ID).build()
            MobileAds.initialize(context,initConfig) { initializationStatus ->
                try {
                    val statusMap = initializationStatus.adapterStatusMap
                    AdLogger.d("AdMob SDK初始化完成")
                    
                    // 输出各个适配器的状态
                    for ((className, status) in statusMap) {
                        AdLogger.d("AdMob 适配器: $className, 状态: ${status.initializationState}, 描述: ${status.description}")
                    }
                    
                    isInitialized = true
                    initDeferred.complete(isInitialized)
                    val result = AdResult.Success(Unit)
                    _initializationState.value = result
                    continuation.resume(result)
                    
                } catch (e: Exception) {
                    AdLogger.e("AdMob SDK初始化过程中发生异常", e)
                    val result = AdResult.Failure(
                        AdException(
                            code = AdException.ERROR_INTERNAL,
                            message = "SDK初始化异常: ${e.message}",
                            cause = e
                        )
                    )
                    _initializationState.value = result
                    continuation.resume(result)
                }
            }
        }
    }

    internal suspend fun awaitInitialized() {
        initDeferred.await()  // 阻塞调用方直到初始化完成
    }
    /**
     * 检查SDK是否已初始化
     */
    fun checkInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * 获取当前初始化状态
     */
    fun getInitState(): AdResult<Unit> {
        return _initializationState.value
    }
    
    /**
     * 获取所有广告控制器的快捷访问器
     */
    object Controllers {
        val interstitial: InterstitialAds
            get() = InterstitialAds.getInstance()
            
        val appOpen: LaunchAds
            get() = LaunchAds.getInstance()
            
        val native: NativeAds
            get() = NativeAds.getInstance()
            
        val fullScreenNative: FullNativeAds
            get() = FullNativeAds.getInstance()
            
        val banner: BannerAds
            get() = BannerAds.getInstance()
    }
    
    /**
     * 清理所有控制器资源
     */
    fun cleanupAll() {
//        Controllers.interstitial.cleanup()
        Controllers.appOpen.cleanup()
        Controllers.native.cleanup()
        Controllers.fullScreenNative.cleanup()
        Controllers.banner.cleanup()
        AdLogger.d("所有广告控制器已清理")
    }
} 