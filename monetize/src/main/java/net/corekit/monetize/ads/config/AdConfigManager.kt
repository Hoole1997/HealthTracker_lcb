package net.corekit.monetize.ads.config

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import net.corekit.core.controller.ChannelUserController
import net.corekit.core.ext.DataStoreStringDelegate
import net.corekit.core.utils.ConfigRemoteManager
import net.corekit.monetize.ads.log.AdLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.text.isNotEmpty

/**
 * 广告配置管理器
 */
@SuppressLint("StaticFieldLeak")
object AdConfigManager {
    private const val CONFIG_FILE_NAME = "ad_config.json"

    private const val CONFIG_KEY_INTERSTITIAL = "interstitial"
    private const val CONFIG_KEY_NATIVE = "native"
    private const val CONFIG_KEY_BANNER = "banner"
    private const val CONFIG_KEY_APP_OPEN = "app_open"
    private const val CONFIG_KEY_FULLSCREEN_NATIVE = "fullscreen_native"
    private const val CONFIG_KEY_REWARDED = "rewarded"
    private const val CONFIG_KEY_REWARDED_INTERSTITIAL = "rewarded_interstitial"

    private var adConfigJsonFromRemote by DataStoreStringDelegate("pdf_b8j2n6w9", "")

    private var configData: AdConfigData? = null
    private var context: Context? = null

    /**
     * 初始化所有配置
     */
    fun initialize(context: Context) {
        try {
            // 保存Context引用
            this.context = context

            // 1. 先使用本地配置进行初始化
            initializeWithLocalConfig(context)

            // 2. 监听用户渠道变化
            setupChannelListener()

            fetchRemoteConfig()

            AdLogger.d("广告配置初始化成功，当前渠道: ${ChannelUserController.getCurrentChannel()}")
        } catch (e: Exception) {
            AdLogger.e("广告配置初始化失败", e)
        }
    }

    /**
     * 异步获取远程配置
     */
    private fun fetchRemoteConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AdLogger.d("开始获取远程广告配置")
                val remoteJsonString = ConfigRemoteManager.getString("adConfigJson", "")

                if (remoteJsonString != null && remoteJsonString.isNotEmpty()) {
                    AdLogger.d("成功获取远程广告配置")
                    val remoteConfig = parseConfigFromAssets(remoteJsonString)

                    // 更新本地配置
                    configData = remoteConfig
                    adConfigJsonFromRemote = remoteJsonString
                    AdLogger.d("远程广告配置更新成功")
                } else {
                    AdLogger.w("远程广告配置为空或获取超时，使用本地配置")
                }

            } catch (e: Exception) {
                AdLogger.e("获取远程广告配置异常", e)
            }
        }
    }

    /**
     * 设置渠道监听器
     */
    private fun setupChannelListener() {
        ChannelUserController.addChannelChangeListener(object : ChannelUserController.ChannelChangeListener {
            override fun onChannelChanged(oldChannel: ChannelUserController.UserChannelType, newChannel: ChannelUserController.UserChannelType) {
                AdLogger.d("广告渠道变化: ${oldChannel.value} -> ${newChannel.value}")
                // 渠道变化时，可以在这里做一些额外的处理
                // 比如重新加载配置、清理缓存等
            }
        })
    }

    /**
     * 使用本地配置初始化
     */
    private fun initializeWithLocalConfig(context: Context) {
        // 解析 JSON 配置
        val jsonString = adConfigJsonFromRemote.orEmpty().takeIf { it.isNotEmpty() }?: context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { it.readText() }
        configData = parseConfigFromAssets(jsonString)

        AdLogger.d("本地广告配置初始化完成")
    }

    /**
     * 从 assets 解析配置
     */
    private fun parseConfigFromAssets(jsonString: String): AdConfigData {
        return try {
            Gson().fromJson(jsonString, AdConfigData::class.java).takeIf { it.natural!=null && it.paid!=null }?:run {
                throw Exception("IllegalArgumentException")
            }
        } catch (e: IOException) {
            AdLogger.e("读取配置文件失败", e)
            throw e
        } catch (e: Exception) {
            AdLogger.e("解析配置文件失败", e)
            throw e
        }
    }

    /**
     * 获取插页广告配置
     */
    fun getInterstitialConfig(): AdConfig {
        return createInterstitialConfig()
    }

    /**
     * 获取原生广告配置
     */
    fun getNativeConfig(): AdConfig {
        return createNativeConfig()
    }

    /**
     * 获取Banner广告配置
     */
    fun getBannerConfig(): AdConfig {
        return createBannerConfig()
    }

    /**
     * 获取开屏广告配置
     */
    fun getAppOpenConfig(): AdConfig {
        return createAppOpenConfig()
    }

    /**
     * 获取全屏原生广告配置
     */
    fun getFullscreenNativeConfig(): AdConfig {
        return createFullscreenNativeConfig()
    }

    /**
     * 获取激励广告配置
     */
    fun getRewardedConfig(): AdConfig {
        return createRewardedConfig()
    }

    /**
     * 获取插页激励广告配置
     */
    fun getRewardedInterstitialConfig(): AdConfig {
        return createRewardedInterstitialConfig()
    }

    /**
     * 获取是否在语言选择页显示底部原生广告
     */
    fun shouldShowBottomNativeOnLanguageSelection(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.showBottomNativeOnLanguageSelection == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.showBottomNativeOnLanguageSelection == 1
            }
        } ?: false
    }

    /**
     * 获取插屏结束后的全屏Native广告数量
     */
    fun getFullscreenNativeAfterInterstitialCount(): Int {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.fullscreenNativeAfterInterstitial
                ChannelUserController.UserChannelType.PAID -> config.paid.fullscreenNativeAfterInterstitial
            }
        } ?: 0
    }

    /**
     * 获取开屏失败后是否展示插屏
     */
    fun shouldShowInterstitialAfterAppOpenFailure(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.showInterstitialAfterAppOpenFailure == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.showInterstitialAfterAppOpenFailure == 1
            }
        } ?: false
    }

    /**
     * 获取开屏失败后是否展示插屏
     */
    fun getSplashTimeout(): Int {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.splashTimeout
                ChannelUserController.UserChannelType.PAID -> config.paid.splashTimeout
            }
        } ?: 10
    }

    fun getRewardBiddingTimeoutMs(): Long {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.rewardBiddingTimeout * 1000L
                ChannelUserController.UserChannelType.PAID -> config.paid.rewardBiddingTimeout * 1000L
            }
        } ?: 5000L
    }

    /**
     * 获取是否启用激励竞价
     * 激励竞价：同时请求激励广告、插页激励广告和插屏广告，展示eCPM更高的广告
     */
    fun isRewardBiddingEnabled(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.rewardBiddingEnabled == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.rewardBiddingEnabled == 1
            }
        } ?: true
    }

    /**
     * 获取返回主页时是否展示插屏
     */
    fun shouldShowGuideFullNative(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.showGuideFullNative == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.showGuideFullNative == 1
            }
        } ?: true
    }

    /**
     * 获取是否开启新手引导流程
     */
    fun showNewGuide(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.showNewGuide == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.showNewGuide == 1
            }
        } ?: false
    }

    fun autoPlayReward() = configData?.let { config ->
        when(ChannelUserController.getCurrentChannel()){
            ChannelUserController.UserChannelType.NATURAL -> config.natural.autoPlayReward == 1
            ChannelUserController.UserChannelType.PAID -> config.paid.autoPlayReward == 1
        }

    } ?: true

    /**
     * 卸载拦截页1是否展示原生广告
     */
    fun shouldShowUninstall1Native(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.showUninstall1Native == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.showUninstall1Native == 1
            }
        } ?: true
    }

    /**
     * 卸载拦截页1是否展示插屏广告
     */
    fun shouldShowUninstall1Interstitial(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.showUninstall1Interstitial == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.showUninstall1Interstitial == 1
            }
        } ?: false
    }

    /**
     * 卸载拦截页2是否展示原生广告
     */
    fun shouldShowUninstall2Native(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.showUninstall2Native == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.showUninstall2Native == 1
            }
        } ?: true
    }

    /**
     * 卸载拦截页2是否展示插屏广告
     */
    fun shouldShowUninstall2Interstitial(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.showUninstall2Interstitial == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.showUninstall2Interstitial == 1
            }
        } ?: false
    }

    /**
     * 获取原生广告自动刷新间隔（毫秒）
     * @return 刷新间隔毫秒数，默认30秒
     */
    fun getNativeAdRefreshIntervalMs(): Long {
        return configData?.let { config ->
            val intervalSeconds = when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.nativeAdRefreshInterval
                ChannelUserController.UserChannelType.PAID -> config.paid.nativeAdRefreshInterval
            }
            // 确保最小间隔为10秒，避免配置错误导致频繁刷新
            (intervalSeconds.coerceAtLeast(10) * 1000L)
        } ?: 30_000L
    }

    /**
     * 获取是否启用开屏竞价
     * 开屏竞价：同时请求开屏和插屏广告，展示eCPM更高的广告
     */
    fun isSplashBiddingEnabled(): Boolean {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.splashBiddingEnabled == 1
                ChannelUserController.UserChannelType.PAID -> config.paid.splashBiddingEnabled == 1
            }
        } ?: false
    }

    /**
     * 获取随机插屏页间隔（秒）
     */
    fun getLongLeaveTime(): Int {
        return configData?.let { config ->
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural.longLeaveTime
                ChannelUserController.UserChannelType.PAID -> config.paid.longLeaveTime
            }
        } ?: 20
    }

    /**
     * 创建插页广告配置（根据当前渠道）
     */
    private fun createInterstitialConfig(): AdConfig {
        val config = checkNotNull(configData) { "请先调用 initialize 初始化配置" }
        val ctx = checkNotNull(context) { "Context 未初始化" }
        val channelConfig = getCurrentChannelConfig(config)

        return AdConfig.Builder(ctx, CONFIG_KEY_INTERSTITIAL)
            .setMaxDailyShow(channelConfig.interstitial.maxDailyShow)
            .setMaxDailyClick(channelConfig.interstitial.maxDailyClick)
            .setMinInterval(channelConfig.interstitial.minInterval.toLong())
            .build()
    }

    /**
     * 创建原生广告配置（根据当前渠道）
     */
    private fun createNativeConfig(): AdConfig {
        val config = checkNotNull(configData) { "请先调用 initialize 初始化配置" }
        val ctx = checkNotNull(context) { "Context 未初始化" }
        val channelConfig = getCurrentChannelConfig(config)

        return AdConfig.Builder(ctx, CONFIG_KEY_NATIVE)
            .setMaxDailyShow(channelConfig.native.maxDailyShow)
            .setMaxDailyClick(channelConfig.native.maxDailyClick)
            .setMinInterval(channelConfig.native.minInterval.toLong())
            .build()
    }

    /**
     * 创建Banner广告配置（根据当前渠道）
     */
    private fun createBannerConfig(): AdConfig {
        val config = checkNotNull(configData) { "请先调用 initialize 初始化配置" }
        val ctx = checkNotNull(context) { "Context 未初始化" }
        val channelConfig = getCurrentChannelConfig(config)

        return AdConfig.Builder(ctx, CONFIG_KEY_BANNER)
            .setMaxDailyShow(channelConfig.native.maxDailyShow)
            .setMaxDailyClick(channelConfig.native.maxDailyClick)
            .setMinInterval(channelConfig.native.minInterval.toLong())
            .build()
    }

    /**
     * 创建开屏广告配置（根据当前渠道）
     */
    private fun createAppOpenConfig(): AdConfig {
        val config = configData ?: run {
            AdLogger.e("配置数据为空，请先调用 initialize 初始化配置")
            throw IllegalStateException("配置数据为空，请先调用 initialize 初始化配置")
        }

        val ctx = context ?: run {
            AdLogger.e("Context 未初始化")
            throw IllegalStateException("Context 未初始化")
        }

        val channelConfig = try {
            getCurrentChannelConfig(config)
        } catch (e: Exception) {
            AdLogger.e("获取渠道配置失败，使用默认配置", e)
            // 使用默认的natural配置
            config.natural
        }

        return AdConfig.Builder(ctx, CONFIG_KEY_APP_OPEN)
            .setMaxDailyShow(channelConfig.appOpen.maxDailyShow)
            .setMaxDailyClick(channelConfig.appOpen.maxDailyClick)
            .setMinInterval(channelConfig.appOpen.minInterval.toLong())
            .build()
    }

    /**
     * 创建全屏原生广告配置（根据当前渠道）
     */
    private fun createFullscreenNativeConfig(): AdConfig {
        val config = checkNotNull(configData) { "请先调用 initialize 初始化配置" }
        val ctx = checkNotNull(context) { "Context 未初始化" }
        val channelConfig = getCurrentChannelConfig(config)

        return AdConfig.Builder(ctx, CONFIG_KEY_FULLSCREEN_NATIVE)
            .setMaxDailyShow(channelConfig.native.maxDailyShow)
            .setMaxDailyClick(channelConfig.native.maxDailyClick)
            .setMinInterval(channelConfig.native.minInterval.toLong())
            .build()
    }

    /**
     * 创建激励广告配置（根据当前渠道）
     * 使用与插页广告相同的频控策略
     */
    private fun createRewardedConfig(): AdConfig {
        val config = checkNotNull(configData) { "请先调用 initialize 初始化配置" }
        val ctx = checkNotNull(context) { "Context 未初始化" }
        val channelConfig = getCurrentChannelConfig(config)

        return AdConfig.Builder(ctx, CONFIG_KEY_REWARDED)
            .setMaxDailyShow(channelConfig.interstitial.maxDailyShow)
            .setMaxDailyClick(channelConfig.interstitial.maxDailyClick)
            .setMinInterval(channelConfig.interstitial.minInterval.toLong())
            .build()
    }

    /**
     * 创建插页激励广告配置（根据当前渠道）
     * 使用与插页广告相同的频控策略
     */
    private fun createRewardedInterstitialConfig(): AdConfig {
        val config = checkNotNull(configData) { "请先调用 initialize 初始化配置" }
        val ctx = checkNotNull(context) { "Context 未初始化" }
        val channelConfig = getCurrentChannelConfig(config)

        return AdConfig.Builder(ctx, CONFIG_KEY_REWARDED_INTERSTITIAL)
            .setMaxDailyShow(channelConfig.interstitial.maxDailyShow)
            .setMaxDailyClick(channelConfig.interstitial.maxDailyClick)
            .setMinInterval(channelConfig.interstitial.minInterval.toLong())
            .build()
    }

    /**
     * 获取当前渠道的配置
     */
    private fun getCurrentChannelConfig(config: AdConfigData): AdConfigData.ChannelConfig {
        return try {
            when (ChannelUserController.getCurrentChannel()) {
                ChannelUserController.UserChannelType.NATURAL -> config.natural
                ChannelUserController.UserChannelType.PAID -> config.paid
            }
        } catch (e: Exception) {
            AdLogger.e("获取用户渠道失败，使用默认natural配置", e)
            config.natural
        }
    }
} 