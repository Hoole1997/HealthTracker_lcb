package net.corekit.monetize.ads.config

import com.google.gson.annotations.SerializedName

/**
 * 广告配置数据类
 */
data class AdConfigData(
    @SerializedName("free_user")
    val natural: ChannelConfig,
    @SerializedName("premium_user")
    val paid: ChannelConfig
) {
    data class ChannelConfig(
        @SerializedName("launch_ad")
        val appOpen: AdTypeConfig,
        @SerializedName("fullpage_ad")
        val interstitial: AdTypeConfig,
        @SerializedName("embed_ad")
        val native: AdTypeConfig,
        @SerializedName("immersive_ad_after_fullpage")
        val fullscreenNativeAfterInterstitial: Int,
        @SerializedName("fallback_fullpage_on_launch_fail")
        val showInterstitialAfterAppOpenFailure: Int,
        @SerializedName("trigger_fullpage_on_home")
        val showInterstitialOnHomeReturn: Int,
        @SerializedName("enable_launch_ad_on_locale")
        val showAppOpenOnLanguageSelection: Int,
        @SerializedName("enable_bottom_ad_on_locale")
        val showBottomNativeOnLanguageSelection: Int,
        @SerializedName("random_fullpage_cooldown")
        val randomInterstitialInterval: Int,
        @SerializedName("onboarding_bottom_ad_enabled")
        val showGuideBottomNative: Int,
        @SerializedName("onboarding_dialog_ad_enabled")
        val showGuideDialogNative: Int,
        @SerializedName(value = "native_time_interval")
        val homeNativeTimeInterval: Int
    )
    
    data class AdTypeConfig(
        @SerializedName("daily_display_cap")
        val maxDailyShow: Int,
        @SerializedName("daily_interaction_cap")
        val maxDailyClick: Int,
        @SerializedName("cooldown_seconds")
        val minInterval: Int
    )
} 