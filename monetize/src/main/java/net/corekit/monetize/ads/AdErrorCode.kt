package net.corekit.monetize.ads

/**
 * 广告错误码枚举
 * 
 * 统一管理所有广告相关错误，便于国际化和日志分析
 */
enum class AdErrorCode(val code: Int, val defaultMessage: String) {
    // SDK 初始化错误 (1xxx)
    PANGLE_APP_ID_NOT_CONFIGURED(1001, "Pangle App ID not configured"),
    TOPON_APP_ID_NOT_CONFIGURED(1002, "TopOn App ID/Key not configured"),
    SDK_INIT_FAILED(1010, "SDK initialization failed"),
    SDK_INIT_TIMEOUT(1011, "SDK initialization timeout"),
    SDK_INIT_EXCEPTION(1012, "SDK initialization exception"),
    
    // 广告位 ID 错误 (2xxx)
    SPLASH_AD_ID_NOT_CONFIGURED(2001, "Splash ad ID not configured"),
    INTERSTITIAL_AD_ID_NOT_CONFIGURED(2002, "Interstitial ad ID not configured"),
    NATIVE_AD_ID_NOT_CONFIGURED(2003, "Native ad ID not configured"),
    FULL_NATIVE_AD_ID_NOT_CONFIGURED(2004, "Full-screen native ad ID not configured"),
    BANNER_AD_ID_NOT_CONFIGURED(2005, "Banner ad ID not configured"),
    REWARDED_AD_ID_NOT_CONFIGURED(2006, "Rewarded ad ID not configured"),
    
    // 加载错误 (3xxx)
    AD_LOAD_FAILED(3001, "Ad load failed"),
    AD_LOAD_TIMEOUT(3002, "Ad load timeout"),
    AD_LOAD_INTERRUPTED(3003, "Ad load interrupted"),
    AD_LOAD_EXCEPTION(3004, "Ad load exception"),
    NO_FILL(3010, "No ad fill"),
    
    // 竞价错误 (4xxx)
    BIDDING_ALL_FAILED(4001, "All platforms failed in bidding"),
    
    // 展示错误 (5xxx)
    AD_NOT_READY(5001, "Ad not ready to show"),
    AD_ALREADY_SHOWING(5002, "Ad already showing"),
    AD_CACHE_NOT_AVAILABLE(5003, "No cached ad available"),
    RENDER_FAILED(5010, "Ad render failed"),
    AD_BIND_FAILED(5023, "Ad bind failed"),
    AD_SHOW_EXCEPTION(5021, "Ad show exception"),
    AD_SHOW_FAILED(5022, "Ad show failed"),

    // Global Config Errors
    GLOBAL_AD_DISABLED(-100, "Global ad disabled"),

    // Lifecycle Errors
    LIFECYCLE_CHECK_FAILED(-400, "Lifecycle check failed"),
    ACTIVITY_FINISHING(-401, "Activity is finishing or destroyed"),
    ACTIVITY_NOT_RESUMED(-402, "Activity not in RESUMED state");

    /**
     * 转换为 AdException
     */
    fun toAdException(cause: Throwable? = null) = AdException(
        code = code,
        message = defaultMessage,
        cause = cause
    )

    /**
     * 转换为 AdException (使用自定义消息)
     */
    fun toAdException(message: String, cause: Throwable? = null) = AdException(
        code = code,
        message = message,
        cause = cause
    )
}
