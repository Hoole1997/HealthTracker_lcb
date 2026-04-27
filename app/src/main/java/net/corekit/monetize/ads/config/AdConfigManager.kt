package net.corekit.monetize.ads.config

object AdConfigManager {
    fun getSplashTimeout(): Int = 10

    fun getLongLeaveTime(): Int = 20

    fun shouldShowBottomNativeOnLanguageSelection(): Boolean = true
    fun showNewGuide(): Boolean = false
    fun shouldShowGuideFullNative(): Boolean = true
    fun autoPlayReward(): Boolean = true
    fun isRewardBiddingEnabled(): Boolean = true
    fun shouldShowUninstall1Native(): Boolean = true
    fun shouldShowUninstall1Interstitial(): Boolean = false
    fun shouldShowUninstall2Native(): Boolean = true
    fun shouldShowUninstall2Interstitial(): Boolean = false
}
