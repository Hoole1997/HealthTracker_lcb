package net.corekit.monetize.ui

/**
 * 原生广告样式模型类
 * 定义不同的原生广告布局样式，支持动态自定义
 */
data class NativeAdStyle(
    val layoutResId: Int,
    val description: String,
) {
    
    companion object {
        /**
         * 标准样式：水平布局，图标+标题+描述+按钮
         */
        val STANDARD = NativeAdStyle(
            layoutResId = net.corekit.monetize.R.layout.layout_native_ads,
            description = "normal",
        )
        
        /**
         * 卡片样式：垂直布局，更适合大尺寸展示
         */
        val CARD = NativeAdStyle(
            layoutResId = net.corekit.monetize.R.layout.layout_native_ad_card,
            description = "card",
        )

        /**
         * 卡片样式3：另一种卡片布局
         */
        val CARD_3 = NativeAdStyle(
            layoutResId = net.corekit.monetize.R.layout.layout_native_ad_card3,
            description = "card3",
        )


        /**
         * 卡片样式3：另一种卡片布局
         */
        val CARD_4 = NativeAdStyle(
            layoutResId = net.corekit.monetize.R.layout.layout_native_ad_card4,
            description = "card4",
        )

        /**
         * 卡片样式3：另一种卡片布局
         */
        val CARD_5 = NativeAdStyle(
            layoutResId = net.corekit.monetize.R.layout.layout_native_ad_card5,
            description = "card5",
        )
        
        /**
         * 创建自定义样式
         * @param layoutResId 布局资源ID
         * @param description 样式描述
         * @return 自定义的NativeAdStyle实例
         */
        fun createCustom(
            layoutResId: Int,
            description: String,
        ): NativeAdStyle {
            return NativeAdStyle(
                layoutResId = layoutResId,
                description = description,
            )
        }
    }

} 