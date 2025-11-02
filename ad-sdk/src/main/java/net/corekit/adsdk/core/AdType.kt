package net.corekit.adsdk.core

/**
 * 广告类型枚举
 * 定义所有支持的广告格式
 */
enum class AdType(val typeName: String) {
    /**
     * 开屏广告 (App Open Ad)
     * 应用启动时展示的全屏广告
     */
    APP_OPEN("app_open"),

    /**
     * 插页广告 (Interstitial Ad)
     * 在应用流程的自然过渡点展示的全屏广告
     */
    INTERSTITIAL("interstitial"),

    /**
     * 横幅广告 (Banner Ad)
     * 在应用界面顶部或底部展示的矩形广告
     */
    BANNER("banner"),

    /**
     * 原生广告 (Native Ad)
     * 可自定义样式，与应用内容融合的广告
     */
    NATIVE("native"),

    /**
     * 激励视频广告 (Rewarded Ad)
     * 用户观看后可获得奖励的视频广告
     */
    REWARDED("rewarded");

    companion object {
        /**
         * 通过类型名称查找广告类型
         * @param typeName 类型名称
         * @return 对应的广告类型，未找到则返回 null
         */
        fun fromTypeName(typeName: String): AdType? {
            return entries.find { it.typeName == typeName }
        }
    }
}
