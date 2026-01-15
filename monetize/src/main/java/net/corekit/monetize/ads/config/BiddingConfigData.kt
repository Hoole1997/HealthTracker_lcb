package net.corekit.monetize.ads.config

import com.google.gson.annotations.SerializedName

/**
 * 竞价配置数据类
 * 
 * 从 Firebase Remote Config 的 biddingConfigJson 参数解析
 */
data class BiddingConfigData(
    @SerializedName("free_user")
    val natural: ChannelBiddingConfig? = null,
    @SerializedName("premium_user")
    val paid: ChannelBiddingConfig? = null
) {
    /**
     * 渠道竞价配置
     */
    data class ChannelBiddingConfig(
        /** 是否启用多平台竞价（0=禁用，1=启用） */
        @SerializedName("bidding_enabled")
        val biddingEnabled: Int = 1,
        /** 是否启用两层竞价（0=仅跨平台，1=平台内+跨平台） */
        @SerializedName("two_layer_bidding_enabled")
        val twoLayerBiddingEnabled: Int = 1,
        /** 竞价超时时间（秒） */
        @SerializedName("bidding_timeout_seconds")
        val biddingTimeoutSeconds: Int = 10,
        /** 平台配置 */
        @SerializedName("platforms")
        val platforms: PlatformsConfig? = null,
        /** 场景配置 */
        @SerializedName("scene_config")
        val sceneConfig: Map<String, SceneConfig>? = null,
        /** 平台级频控是否启用（默认禁用，追求收入最大化） */
        @SerializedName("platform_frequency_enabled")
        val platformFrequencyEnabled: Boolean = false,
        /** 平台频控配置 */
        @SerializedName("platform_frequency")
        val platformFrequency: PlatformFrequencyConfigs? = null
    )

    /**
     * 平台配置集合
     */
    data class PlatformsConfig(
        @SerializedName("admob")
        val admob: PlatformConfig? = null,
        @SerializedName("pangle")
        val pangle: PlatformConfig? = null,
        @SerializedName("topon")
        val topon: PlatformConfig? = null
    )

    /**
     * 单个平台配置
     */
    data class PlatformConfig(
        /** 该平台是否启用 */
        @SerializedName("enabled")
        val enabled: Int = 1,
        /** 平台优先级（eCPM 相同时使用，数字越小优先级越高） */
        @SerializedName("priority")
        val priority: Int = 99,
        /** 广告类型配置 */
        @SerializedName("ad_types")
        val adTypes: Map<String, AdTypeConfig>? = null
    )

    /**
     * 广告类型配置
     */
    data class AdTypeConfig(
        /** 该广告类型是否启用 */
        @SerializedName("enabled")
        val enabled: Int = 1,
        /** 该广告类型是否参与竞价 */
        @SerializedName("participate_bidding")
        val participateBidding: Int = 1
    )

    /**
     * 场景配置
     */
    data class SceneConfig(
        /** 竞价模式：two_layer / single_layer */
        @SerializedName("bidding_mode")
        val biddingMode: String = "two_layer",
        /** 平台内竞价的广告类型列表 */
        @SerializedName("internal_bidding_types")
        val internalBiddingTypes: List<String>? = null,
        /** 回退平台 */
        @SerializedName("fallback_platform")
        val fallbackPlatform: String = "admob",
        /** 回退广告类型 */
        @SerializedName("fallback_ad_type")
        val fallbackAdType: String = "splash"
    )

    /**
     * 平台频控配置集合
     * 
     * 结构：平台 → 广告类型 → 配置
     * 
     * JSON 示例：
     * ```json
     * {
     *   "admob": {
     *     "splash": { "max_daily_show": 50, "max_daily_click": 20, "min_show_interval_seconds": 60 },
     *     "interstitial": { "max_daily_show": 30, "max_daily_click": 15, "min_show_interval_seconds": 120 }
     *   }
     * }
     * ```
     */
    data class PlatformFrequencyConfigs(
        @SerializedName("admob")
        val admob: Map<String, PlatformFrequencyConfig>? = null,
        @SerializedName("pangle")
        val pangle: Map<String, PlatformFrequencyConfig>? = null,
        @SerializedName("topon")
        val topon: Map<String, PlatformFrequencyConfig>? = null
    )

    /**
     * 单个平台+广告类型的频控配置
     */
    data class PlatformFrequencyConfig(
        /** 每日展示上限 */
        @SerializedName("max_daily_show")
        val maxDailyShow: Int = 100,
        /** 每日点击上限 */
        @SerializedName("max_daily_click")
        val maxDailyClick: Int = 50,
        /** 最小展示间隔（秒），0 表示不限制 */
        @SerializedName("min_show_interval_seconds")
        val minShowIntervalSeconds: Int = 0
    )
}
