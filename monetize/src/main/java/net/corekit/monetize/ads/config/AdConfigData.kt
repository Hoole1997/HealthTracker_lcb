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
        @SerializedName("Guide_Full_Native")
        val showGuideFullNative: Int,
        @SerializedName("Guide_Page")
        val showNewGuide: Int,
        @SerializedName("enable_bottom_ad_on_locale")
        val showBottomNativeOnLanguageSelection: Int,
        @SerializedName("random_fullpage_cooldown")
        val randomInterstitialInterval: Int,
        @SerializedName("onboarding_bottom_ad_enabled")
        val showGuideBottomNative: Int,
        @SerializedName("onboarding_dialog_ad_enabled")
        val showGuideDialogNative: Int,
        @SerializedName(value = "splash_time_out")
        val splashTimeout:Int,
        @SerializedName(value = "long_leave_app")
        val longLeaveTime: Int,
        @SerializedName(value = "auto_play_reward")
        val autoPlayReward: Int,
        @SerializedName(value = "splash_bidding_enabled")
        val splashBiddingEnabled: Int = 0

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