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
            // 初始化 Pangle
            try {
                PangleManager.initialize(context)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] Pangle 初始化失败", e)
            }

            // 初始化 TopOn
            try {
                TopOnManager.initialize(context)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] TopOn 初始化失败", e)
            }
            
            AdLogger.d("[$TAG] 多平台 SDK 初始化请求已发出")
        }
    }
}
