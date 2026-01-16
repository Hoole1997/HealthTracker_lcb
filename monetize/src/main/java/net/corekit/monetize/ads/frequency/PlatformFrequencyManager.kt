package net.corekit.monetize.ads.frequency

import android.content.Context
import android.content.SharedPreferences
import net.corekit.monetize.ads.bidding.BiddingAdType
import net.corekit.monetize.ads.bidding.BiddingPlatform
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.log.AdLogger
import java.text.SimpleDateFormat
import java.util.*

/**
 * 平台级频控管理器
 * 
 * 按平台维度控制广告展示频率：
 * - 每日展示上限
 * - 每日点击上限
 * 
 * 配置从 Firebase Remote Config 获取，默认禁用（追求收入最大化）
 * 
 * 使用示例：
 * ```kotlin
 * // 检查平台是否可参与竞价
 * if (PlatformFrequencyManager.canParticipate(BiddingPlatform.ADMOB, BiddingAdType.SPLASH)) {
 *     // 参与竞价
 * }
 * 
 * // 记录展示
 * PlatformFrequencyManager.recordShow(BiddingPlatform.ADMOB, BiddingAdType.SPLASH)
 * ```
 */
object PlatformFrequencyManager {

    private const val TAG = "PlatformFrequency"
    private const val PREFS_NAME = "platform_frequency_prefs"
    
    private var sharedPreferences: SharedPreferences? = null
    
    // 日期格式化器（用于生成每日唯一 key）
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    /**
     * 初始化（需在 Application 中调用）
     */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cleanupExpiredKeys()
        AdLogger.d("[$TAG] 平台频控管理器已初始化")
    }

    /**
     * 清理 7 天前的过期 Key
     * 避免 SharedPreferences 文件无限膨胀
     */
    private fun cleanupExpiredKeys() {
        val sp = sharedPreferences ?: return
        val allKeys = sp.all.keys
        val cutoffDate = getDateNDaysAgo(7)
        
        val keysToRemove = allKeys.filter { key ->
            // 匹配格式 pf_xxx_yyyyMMdd
            val dateMatch = Regex("_(\\d{8})$").find(key)
            dateMatch?.let {
                val keyDate = it.groupValues[1]
                keyDate < cutoffDate
            } ?: false
        }
        
        if (keysToRemove.isNotEmpty()) {
            sp.edit().apply {
                keysToRemove.forEach { remove(it) }
                apply()
            }
            AdLogger.d("[$TAG] 已清理 ${keysToRemove.size} 个过期频控 Key")
        }
    }

    private fun getDateNDaysAgo(n: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -n)
        return dateFormat.format(calendar.time)
    }

    /**
     * 平台级频控是否启用
     * 
     * 从 Firebase Remote Config 读取，默认禁用
     */
    fun isEnabled(): Boolean {
        return BiddingConfigManager.isPlatformFrequencyEnabled()
    }

    /**
     * 检查平台是否可参与竞价
     * 
     * 检查项：
     * 1. 每日展示上限
     * 2. 每日点击上限
     * 3. 展示间隔
     * 
     * @param platform 平台类型
     * @param adType 广告类型
     * @return true 可参与，false 已超限或间隔不足
     */
    fun canParticipate(platform: BiddingPlatform, adType: BiddingAdType): Boolean {
        // 频控未启用时，所有平台都可参与
        if (!isEnabled()) {
            return true
        }
        
        val adTypeStr = adType.name.lowercase()
        val config = BiddingConfigManager.getPlatformFrequencyConfig(platform, adTypeStr)
        if (config == null) {
            // 无配置时默认可参与
            return true
        }
        
        val dailyShow = getDailyShowCount(platform, adType)
        val dailyClick = getDailyClickCount(platform, adType)
        
        // 1. 检查每日展示上限
        if (dailyShow >= config.maxDailyShow) {
            AdLogger.logW(TAG, "频控拦截 | 平台: %s | 类型: %s | 原因: 展示次数超限 (%d/%d)", 
                platform.name, adType.name, dailyShow, config.maxDailyShow)
            return false
        }
        
        // 2. 检查每日点击上限
        if (dailyClick >= config.maxDailyClick) {
            AdLogger.logW(TAG, "频控拦截 | 平台: %s | 类型: %s | 原因: 点击次数超限 (%d/%d)", 
                platform.name, adType.name, dailyClick, config.maxDailyClick)
            return false
        }
        
        // 3. 检查展示间隔
        if (config.minShowIntervalSeconds > 0) {
            val lastShowTime = getLastShowTime(platform, adType)
            if (lastShowTime > 0) {
                val intervalSeconds = (System.currentTimeMillis() - lastShowTime) / 1000
                if (intervalSeconds < config.minShowIntervalSeconds) {
                    AdLogger.logW(TAG, "频控拦截 | 平台: %s | 类型: %s | 原因: 展示间隔不足 (%ds < %ds)", 
                        platform.name, adType.name, intervalSeconds, config.minShowIntervalSeconds)
                    return false
                }
            }
        }
        
        // 频控检查通过，输出详细状态（仅在 verbose 模式）
        AdLogger.verbose(TAG, "频控检查通过 | 平台: %s | 类型: %s | 展示: %d/%d | 点击: %d/%d",
            platform.name, adType.name, dailyShow, config.maxDailyShow, dailyClick, config.maxDailyClick)
        
        return true
    }

    /**
     * 记录平台展示
     * 同时记录展示次数和上次展示时间
     */
    fun recordShow(platform: BiddingPlatform, adType: BiddingAdType) {
        if (!isEnabled()) return
        
        // 记录展示次数
        val countKey = getDailyKey(platform, adType, "show")
        val currentCount = sharedPreferences?.getInt(countKey, 0) ?: 0
        
        // 记录上次展示时间
        val timeKey = getLastShowTimeKey(platform, adType)
        
        sharedPreferences?.edit()
            ?.putInt(countKey, currentCount + 1)
            ?.putLong(timeKey, System.currentTimeMillis())
            ?.apply()
        
        // 获取配置以显示上限
        val adTypeStr = adType.name.lowercase()
        val config = BiddingConfigManager.getPlatformFrequencyConfig(platform, adTypeStr)
        val maxShow = config?.maxDailyShow ?: 0
        
        AdLogger.logD(TAG, "记录展示 | 平台: %s | 类型: %s | 今日展示: %d/%d", 
            platform.name, adType.name, currentCount + 1, maxShow)
    }

    /**
     * 记录平台点击
     */
    fun recordClick(platform: BiddingPlatform, adType: BiddingAdType) {
        if (!isEnabled()) return
        
        val key = getDailyKey(platform, adType, "click")
        val currentCount = sharedPreferences?.getInt(key, 0) ?: 0
        sharedPreferences?.edit()?.putInt(key, currentCount + 1)?.apply()
        
        // 获取配置以显示上限
        val adTypeStr = adType.name.lowercase()
        val config = BiddingConfigManager.getPlatformFrequencyConfig(platform, adTypeStr)
        val maxClick = config?.maxDailyClick ?: 0
        
        AdLogger.logD(TAG, "记录点击 | 平台: %s | 类型: %s | 今日点击: %d/%d", 
            platform.name, adType.name, currentCount + 1, maxClick)
    }

    /**
     * 获取平台上次展示时间
     */
    fun getLastShowTime(platform: BiddingPlatform, adType: BiddingAdType): Long {
        val key = getLastShowTimeKey(platform, adType)
        return sharedPreferences?.getLong(key, 0L) ?: 0L
    }

    /**
     * 生成上次展示时间 key（不按日期，持久保存）
     */
    private fun getLastShowTimeKey(platform: BiddingPlatform, adType: BiddingAdType): String {
        return "pf_${platform.name.lowercase()}_${adType.name.lowercase()}_last_show_time"
    }

    /**
     * 获取平台每日展示次数
     */
    fun getDailyShowCount(platform: BiddingPlatform, adType: BiddingAdType): Int {
        val key = getDailyKey(platform, adType, "show")
        return sharedPreferences?.getInt(key, 0) ?: 0
    }

    /**
     * 获取平台每日点击次数
     */
    fun getDailyClickCount(platform: BiddingPlatform, adType: BiddingAdType): Int {
        val key = getDailyKey(platform, adType, "click")
        return sharedPreferences?.getInt(key, 0) ?: 0
    }

    /**
     * 获取所有平台的频控状态（用于调试面板）
     */
    fun getAllPlatformStatus(): Map<String, PlatformFrequencyStatus> {
        val result = mutableMapOf<String, PlatformFrequencyStatus>()
        
        BiddingPlatform.values().forEach { platform ->
            BiddingAdType.values().forEach { adType ->
                val key = "${platform.name}_${adType.name}"
                val adTypeStr = adType.name.lowercase()
                val config = BiddingConfigManager.getPlatformFrequencyConfig(platform, adTypeStr)
                result[key] = PlatformFrequencyStatus(
                    platform = platform.name,
                    adType = adType.name,
                    dailyShow = getDailyShowCount(platform, adType),
                    maxDailyShow = config?.maxDailyShow ?: 100,
                    dailyClick = getDailyClickCount(platform, adType),
                    maxDailyClick = config?.maxDailyClick ?: 50,
                    minShowIntervalSeconds = config?.minShowIntervalSeconds ?: 0,
                    lastShowTime = getLastShowTime(platform, adType)
                )
            }
        }
        
        return result
    }

    /**
     * 重置所有计数（用于测试）
     */
    fun resetAllCounts() {
        sharedPreferences?.edit()?.clear()?.apply()
        AdLogger.d("[$TAG] 已重置所有平台频控计数")
    }

    /**
     * 生成每日唯一 key
     */
    private fun getDailyKey(platform: BiddingPlatform, adType: BiddingAdType, type: String): String {
        val today = dateFormat.format(Date())
        return "pf_${platform.name.lowercase()}_${adType.name.lowercase()}_${type}_$today"
    }

    /**
     * 平台频控状态
     */
    data class PlatformFrequencyStatus(
        val platform: String,
        val adType: String,
        val dailyShow: Int,
        val maxDailyShow: Int,
        val dailyClick: Int,
        val maxDailyClick: Int,
        val minShowIntervalSeconds: Int = 0,
        val lastShowTime: Long = 0L
    ) {
        val isShowLimitReached: Boolean get() = dailyShow >= maxDailyShow
        val isClickLimitReached: Boolean get() = dailyClick >= maxDailyClick
        
        /** 距离上次展示的间隔（秒） */
        val intervalSinceLastShow: Long 
            get() = if (lastShowTime > 0) (System.currentTimeMillis() - lastShowTime) / 1000 else -1
        
        /** 展示间隔是否充足 */
        val isIntervalSufficient: Boolean 
            get() = minShowIntervalSeconds == 0 || intervalSinceLastShow < 0 || intervalSinceLastShow >= minShowIntervalSeconds
    }
}
