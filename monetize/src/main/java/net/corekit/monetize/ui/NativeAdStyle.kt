package net.corekit.monetize.ui

import net.corekit.monetize.R

/**
 * 原生广告样式模型类
 * 定义不同的原生广告布局样式，支持动态自定义
 */
data class NativeAdStyle(
    val layoutResId: Int,
    val description: String,
) {
    
    /**
     * 获取 Pangle 平台对应的布局资源 ID
     */
    fun getPangleLayout(): Int {
        return when (this) {
            CARD -> R.layout.layout_pangle_native_ad_card
            CARD_3 -> R.layout.layout_pangle_native_ad_card3
            CARD_4 -> R.layout.layout_pangle_native_ad_card4
            CARD_7 -> R.layout.layout_pangle_native_ad_card7
            CARD_5,CARD_8 -> R.layout.layout_pangle_native_ad_card8
            else -> R.layout.layout_pangle_native_ads
        }
    }
    
    /**
     * 获取 TopOn 平台对应的布局资源 ID
     */
    fun getTopOnLayout(): Int {
        return when (this) {
            CARD -> R.layout.layout_topon_native_ad_card
            CARD_3 -> R.layout.layout_topon_native_ad_card3
            CARD_4 -> R.layout.layout_topon_native_ad_card4
            CARD_7 -> R.layout.layout_topon_native_ad_card7
            CARD_5,CARD_8 -> R.layout.layout_topon_native_ad_card8
            else -> R.layout.layout_topon_native_ads
        }
    }
    
    companion object {
        /**
         * 标准样式：水平布局，图标+标题+描述+按钮
         */
        val STANDARD = NativeAdStyle(
            layoutResId = R.layout.layout_native_ads,
            description = "normal",
        )
        
        /**
         * 卡片样式：垂直布局，更适合大尺寸展示
         */
        val CARD = NativeAdStyle(
            layoutResId = R.layout.layout_native_ad_card,
            description = "card",
        )

        /**
         * 卡片样式3：包含媒体区域
         */
        val CARD_3 = NativeAdStyle(
            layoutResId = R.layout.layout_native_ad_card3,
            description = "card3",
        )

        /**
         * 卡片样式4：大媒体区域 + 居中图标
         */
        val CARD_4 = NativeAdStyle(
            layoutResId = R.layout.layout_native_ad_card4,
            description = "card4",
        )

        /**
         * 卡片样式5：包含星级评分
         */
        val CARD_5 = NativeAdStyle(
            layoutResId = R.layout.layout_native_ad_card5,
            description = "card5",
        )

        /**
         * 卡片样式6：浅蓝背景 + 星级评分
         */
        val CARD_6 = NativeAdStyle(
            layoutResId = R.layout.layout_native_ad_card6,
            description = "card6",
        )

        /**
         * 卡片样式7：带角标AD标签
         */
        val CARD_7 = NativeAdStyle(
            layoutResId = R.layout.layout_native_ad_card7,
            description = "card7",
        )

        /**
         * 卡片样式8：水平紧凑型
         */
        val CARD_8 = NativeAdStyle(
            layoutResId = R.layout.layout_native_ad_card8,
            description = "card8",
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