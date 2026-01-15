package net.corekit.monetize.ads.bidding

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleManager
import net.corekit.monetize.ads.topon.TopOnManager

/**
 * 竞价系统统一初始化器
 */
object BiddingInitializer {
    private const val TAG = "BiddingInitializer"

    /**
     * 初始化所有竞价相关的组件
     * @param context Context
     * @param appIconId Pangle App Open 广告展示需要的图标资源 ID
     */
    fun initialize(context: Context, appIconId: Int? = null) {
        AdLogger.d("[$TAG] 开始初始化多平台竞价系统")
        
        // 1. 初始化竞价配置管理器（加载本地和远程配置）
        BiddingConfigManager.initialize(context)
        
        // 打印广告 ID 配置状态
        AdIdHelper.logAdIdConfig()
        
        // 2. 异步初始化各平台 SDK
        CoroutineScope(Dispatchers.IO).launch {
            val initEntries = mutableListOf<net.corekit.monetize.ads.log.BiddingLogger.SdkInitEntry>()

            // 初始化 AdMob (通常在 Application 中已初始化，这里记录一下状态或假设成功，或者跳过)
            // 这里主要关注我们手动初始化的 Pangle 和 TopOn

            // 初始化 Pangle
            val pangleStartTime = System.currentTimeMillis()
            var pangleSuccess = false
            var pangleError: String? = null
            try {
                PangleManager.initialize(context)
                pangleSuccess = true
            } catch (e: Exception) {
                AdLogger.e("[$TAG] Pangle 初始化失败", e)
                pangleError = e.message
            }
            initEntries.add(net.corekit.monetize.ads.log.BiddingLogger.SdkInitEntry(
                platform = "Pangle",
                isSuccess = pangleSuccess,
                durationMs = System.currentTimeMillis() - pangleStartTime,
                errorMessage = pangleError
            ))

            // 初始化 TopOn
            val topOnStartTime = System.currentTimeMillis()
            var topOnSuccess = false
            var topOnError: String? = null
            try {
                TopOnManager.initialize(context)
                topOnSuccess = true
            } catch (e: Exception) {
                AdLogger.e("[$TAG] TopOn 初始化失败", e)
                topOnError = e.message
            }
             initEntries.add(net.corekit.monetize.ads.log.BiddingLogger.SdkInitEntry(
                platform = "TopOn",
                isSuccess = topOnSuccess,
                durationMs = System.currentTimeMillis() - topOnStartTime,
                errorMessage = topOnError
            ))
            
            // 输出表格
            net.corekit.monetize.ads.log.BiddingLogger.logSdkInitStatus(initEntries)

            AdLogger.d("[$TAG] 多平台 SDK 初始化请求已发出")
        }
    }
}
