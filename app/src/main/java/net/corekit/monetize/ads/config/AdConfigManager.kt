package net.corekit.monetize.ads.config

import com.daily.health.manager.utils.isAdSlotEnabled
import net.corekit.monetize.ads.AdPosition

object AdConfigManager {
    fun shouldShowBottomNativeOnLanguageSelection(): Boolean = true
    fun showNewGuide(): Boolean = false
    fun shouldShowGuideFullNative(): Boolean = true
    fun autoPlayReward(): Boolean = true
    fun isRewardBiddingEnabled(): Boolean = true
    fun shouldShowUninstall1Native(): Boolean = true
    // 历史上硬编码关闭；只有线上明确开启对应广告位才可触发，缺省仍为关闭。
    fun shouldShowUninstall1Interstitial(): Boolean = isAdSlotEnabled(AdPosition.IV_UNINSTALL_1)
    fun shouldShowUninstall2Native(): Boolean = true
    fun shouldShowUninstall2Interstitial(): Boolean = isAdSlotEnabled(AdPosition.IV_UNINSTALL_2)
}
