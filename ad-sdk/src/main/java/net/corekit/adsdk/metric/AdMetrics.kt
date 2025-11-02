package net.corekit.adsdk.metric

import com.tencent.mmkv.MMKV
import net.corekit.adsdk.core.AdType

/**
 * 广告指标统计接口
 * 提供广告各项指标的统计功能
 */
internal interface AdMetrics {
    /**
     * 增加加载次数
     */
    fun incrementLoad(adType: AdType)

    /**
     * 增加展示次数
     */
    fun incrementShow(adType: AdType)

    /**
     * 增加点击次数
     */
    fun incrementClick(adType: AdType)

    /**
     * 增加曝光次数
     */
    fun incrementImpression(adType: AdType)

    /**
     * 获取加载次数
     */
    fun getLoadCount(adType: AdType): Int

    /**
     * 获取展示次数
     */
    fun getShowCount(adType: AdType): Int

    /**
     * 获取点击次数
     */
    fun getClickCount(adType: AdType): Int

    /**
     * 获取曝光次数
     */
    fun getImpressionCount(adType: AdType): Int

    /**
     * 获取点击率（CTR）
     */
    fun getClickRate(adType: AdType): Double

    /**
     * 获取填充率
     */
    fun getFillRate(adType: AdType): Double

    /**
     * 重置统计数据
     */
    fun reset(adType: AdType)

    /**
     * 重置所有统计数据
     */
    fun resetAll()
}

/**
 * 默认指标统计实现
 * 使用 MMKV 持久化存储，数据在应用重启后依然保留
 *
 * 存储格式：
 * - Key: ad_metrics_{AdType}_{metric}
 * - Value: Int (计数)
 *
 * 特点：
 * - 线程安全（MMKV 内置）
 * - 支持多进程（MULTI_PROCESS_MODE）
 * - 高性能（比 SharedPreferences 快）
 *
 * @param mmkv MMKV 实例，用于持久化存储
 */
internal class DefaultAdMetrics(
    private val mmkv: MMKV
) : AdMetrics {

    companion object {
        private const val KEY_PREFIX = "ad_metrics_"

        // Key 格式生成器
        private fun loadKey(adType: AdType) = "${KEY_PREFIX}${adType.name}_load"
        private fun showKey(adType: AdType) = "${KEY_PREFIX}${adType.name}_show"
        private fun clickKey(adType: AdType) = "${KEY_PREFIX}${adType.name}_click"
        private fun impressionKey(adType: AdType) = "${KEY_PREFIX}${adType.name}_impression"
    }

    override fun incrementLoad(adType: AdType) {
        val key = loadKey(adType)
        val current = mmkv.decodeInt(key, 0)
        mmkv.encode(key, current + 1)
    }

    override fun incrementShow(adType: AdType) {
        val key = showKey(adType)
        val current = mmkv.decodeInt(key, 0)
        mmkv.encode(key, current + 1)
    }

    override fun incrementClick(adType: AdType) {
        val key = clickKey(adType)
        val current = mmkv.decodeInt(key, 0)
        mmkv.encode(key, current + 1)
    }

    override fun incrementImpression(adType: AdType) {
        val key = impressionKey(adType)
        val current = mmkv.decodeInt(key, 0)
        mmkv.encode(key, current + 1)
    }

    override fun getLoadCount(adType: AdType): Int {
        return mmkv.decodeInt(loadKey(adType), 0)
    }

    override fun getShowCount(adType: AdType): Int {
        return mmkv.decodeInt(showKey(adType), 0)
    }

    override fun getClickCount(adType: AdType): Int {
        return mmkv.decodeInt(clickKey(adType), 0)
    }

    override fun getImpressionCount(adType: AdType): Int {
        return mmkv.decodeInt(impressionKey(adType), 0)
    }

    override fun getClickRate(adType: AdType): Double {
        val shows = getShowCount(adType)
        if (shows == 0) return 0.0

        val clicks = getClickCount(adType)
        return clicks.toDouble() / shows.toDouble()
    }

    override fun getFillRate(adType: AdType): Double {
        val loads = getLoadCount(adType)
        if (loads == 0) return 0.0

        val shows = getShowCount(adType)
        return shows.toDouble() / loads.toDouble()
    }

    override fun reset(adType: AdType) {
        mmkv.encode(loadKey(adType), 0)
        mmkv.encode(showKey(adType), 0)
        mmkv.encode(clickKey(adType), 0)
        mmkv.encode(impressionKey(adType), 0)
    }

    override fun resetAll() {
        AdType.entries.forEach { adType ->
            reset(adType)
        }
    }

    /**
     * 获取所有统计数据（调试用）
     */
    fun getAllStats(): Map<AdType, Map<String, Int>> {
        return AdType.entries.associateWith { adType ->
            mapOf(
                "load" to getLoadCount(adType),
                "show" to getShowCount(adType),
                "click" to getClickCount(adType),
                "impression" to getImpressionCount(adType)
            )
        }
    }
}
