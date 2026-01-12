package net.corekit.monetize.ads.bidding

import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.log.AdLogger

/**
 * 广告 ID 配置助手
 * 
 * 用于检查各平台的广告 ID 是否有效配置
 * 当广告 ID 为空时，该广告类型不参与竞价
 */
object AdIdHelper {

    private const val TAG = "AdIdHelper"

    // ==================== Pangle 广告 ID ====================
    
    /**
     * 检查 Pangle 平台是否有有效的 App ID
     */
    fun hasPangleAppId(): Boolean {
        return BuildConfig.PANGLE_APPLICATION_ID.isNotBlank()
    }
    
    /**
     * 检查 Pangle 开屏广告 ID 是否有效
     */
    fun hasPangleSplashId(): Boolean {
        return BuildConfig.PANGLE_SPLASH_ID.isNotBlank()
    }
    
    /**
     * 检查 Pangle 插页广告 ID 是否有效
     */
    fun hasPangleInterstitialId(): Boolean {
        return BuildConfig.PANGLE_INTERSTITIAL_ID.isNotBlank()
    }
    
    /**
     * 检查 Pangle 原生广告 ID 是否有效
     */
    fun hasPangleNativeId(): Boolean {
        return BuildConfig.PANGLE_NATIVE_ID.isNotBlank()
    }
    
    /**
     * 检查 Pangle 全屏原生广告 ID 是否有效
     */
    fun hasPangleFullNativeId(): Boolean {
        return BuildConfig.PANGLE_FULL_NATIVE_ID.isNotBlank()
    }
    
    /**
     * 检查 Pangle Banner 广告 ID 是否有效
     */
    fun hasPangleBannerId(): Boolean {
        return BuildConfig.PANGLE_BANNER_ID.isNotBlank()
    }
    
    /**
     * 检查 Pangle 激励广告 ID 是否有效
     */
    fun hasPangleRewardedId(): Boolean {
        return BuildConfig.PANGLE_REWARDED_ID.isNotBlank()
    }

    // ==================== TopOn 广告 ID ====================
    
    /**
     * 检查 TopOn 平台是否有有效的 App ID 和 Key
     */
    fun hasTopOnAppId(): Boolean {
        return BuildConfig.TOPON_APPLICATION_ID.isNotBlank() && 
               BuildConfig.TOPON_APP_KEY.isNotBlank()
    }
    
    /**
     * 检查 TopOn 开屏广告 ID 是否有效
     */
    fun hasTopOnSplashId(): Boolean {
        return BuildConfig.TOPON_SPLASH_ID.isNotBlank()
    }
    
    /**
     * 检查 TopOn 插页广告 ID 是否有效
     */
    fun hasTopOnInterstitialId(): Boolean {
        return BuildConfig.TOPON_INTERSTITIAL_ID.isNotBlank()
    }
    
    /**
     * 检查 TopOn 原生广告 ID 是否有效
     */
    fun hasTopOnNativeId(): Boolean {
        return BuildConfig.TOPON_NATIVE_ID.isNotBlank()
    }
    
    /**
     * 检查 TopOn 全屏原生广告 ID 是否有效
     */
    fun hasTopOnFullNativeId(): Boolean {
        return BuildConfig.TOPON_FULL_NATIVE_ID.isNotBlank()
    }
    
    /**
     * 检查 TopOn Banner 广告 ID 是否有效
     */
    fun hasTopOnBannerId(): Boolean {
        return BuildConfig.TOPON_BANNER_ID.isNotBlank()
    }
    
    /**
     * 检查 TopOn 激励广告 ID 是否有效
     */
    fun hasTopOnRewardedId(): Boolean {
        return BuildConfig.TOPON_REWARDED_ID.isNotBlank()
    }

    // ==================== 通用判断方法 ====================
    
    /**
     * 判断指定平台的指定广告类型是否有有效的广告 ID
     * 
     * @param platform 平台
     * @param adType 广告类型（splash, interstitial, native, full_native, banner, rewarded）
     * @return 是否有有效的广告 ID
     */
    fun hasValidAdId(platform: BiddingWinner, adType: String): Boolean {
        return when (platform) {
            BiddingWinner.ADMOB -> true // AdMob 始终有广告 ID 配置
            BiddingWinner.PANGLE -> hasPangleAppId() && hasPangleAdTypeId(adType)
            BiddingWinner.TOPON -> hasTopOnAppId() && hasTopOnAdTypeId(adType)
        }
    }
    
    private fun hasPangleAdTypeId(adType: String): Boolean {
        return when (adType) {
            "splash" -> hasPangleSplashId()
            "interstitial" -> hasPangleInterstitialId()
            "native" -> hasPangleNativeId()
            "full_native" -> hasPangleFullNativeId()
            "banner" -> hasPangleBannerId()
            "rewarded", "rewarded_interstitial" -> hasPangleRewardedId()
            else -> false
        }
    }
    
    private fun hasTopOnAdTypeId(adType: String): Boolean {
        return when (adType) {
            "splash" -> hasTopOnSplashId()
            "interstitial" -> hasTopOnInterstitialId()
            "native" -> hasTopOnNativeId()
            "full_native" -> hasTopOnFullNativeId()
            "banner" -> hasTopOnBannerId()
            "rewarded", "rewarded_interstitial" -> hasTopOnRewardedId()
            else -> false
        }
    }

    /**
     * 打印当前广告 ID 配置状态（仅 Debug 模式）
     */
    fun logAdIdConfig() {
        AdLogger.d("[$TAG] ╔══════════════════════════════════════════════════════════════")
        AdLogger.d("[$TAG] ║ 广告 ID 配置状态")
        AdLogger.d("[$TAG] ╠══════════════════════════════════════════════════════════════")
        
        // Pangle
        AdLogger.d("[$TAG] ║ Pangle:")
        AdLogger.d("[$TAG] ║   • App ID:      %s", if (hasPangleAppId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 开屏:        %s", if (hasPangleSplashId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 插页:        %s", if (hasPangleInterstitialId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 原生:        %s", if (hasPangleNativeId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 全屏原生:    %s", if (hasPangleFullNativeId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • Banner:      %s", if (hasPangleBannerId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 激励:        %s", if (hasPangleRewardedId()) "✅" else "❌ 未配置")
        
        AdLogger.d("[$TAG] ╟──────────────────────────────────────────────────────────────")
        
        // TopOn
        AdLogger.d("[$TAG] ║ TopOn:")
        AdLogger.d("[$TAG] ║   • App ID/Key:  %s", if (hasTopOnAppId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 开屏:        %s", if (hasTopOnSplashId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 插页:        %s", if (hasTopOnInterstitialId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 原生:        %s", if (hasTopOnNativeId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 全屏原生:    %s", if (hasTopOnFullNativeId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • Banner:      %s", if (hasTopOnBannerId()) "✅" else "❌ 未配置")
        AdLogger.d("[$TAG] ║   • 激励:        %s", if (hasTopOnRewardedId()) "✅" else "❌ 未配置")
        
        AdLogger.d("[$TAG] ╚══════════════════════════════════════════════════════════════")
    }
}
