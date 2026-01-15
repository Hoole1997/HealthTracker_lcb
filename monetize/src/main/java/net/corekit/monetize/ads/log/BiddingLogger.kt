package net.corekit.monetize.ads.log

import android.util.Log

/**
 * 竞价专用日志工具
 * 
 * 提供表格格式的竞价结果输出，便于 Logcat 调试
 * 
 * 使用示例：
 * ```kotlin
 * val entries = listOf(
 *     BiddingLogEntry("AdMob", "Splash", "✓ 成功", "$4.80"),
 *     BiddingLogEntry("TopOn", "Splash", "✓ 成功", "$3.50"),
 *     BiddingLogEntry("Pangle", "Splash", "✗ 超时", "--")
 * )
 * BiddingLogger.logBiddingTable("SplashBidding", entries)
 * BiddingLogger.logWinner("SplashBidding", "AdMob", "Splash", 4.80)
 * ```
 */
object BiddingLogger {

    private const val TAG = "AdModule"
    
    /**
     * 竞价日志条目
     */
    data class BiddingLogEntry(
        val platform: String,
        val adType: String,
        val status: String,    // "✓ 成功" / "✗ 失败" / "✗ 超时" / "- 未参与"
        val ecpm: String       // "$4.80" / "--"
    )

    /**
     * 输出竞价结果表格
     * 
     * 输出格式：
     * ```
     * ┌──────────┬──────────────┬──────────┬───────────┐
     * │ 平台      │ 广告类型      │ 状态      │ eCPM      │
     * ├──────────┼──────────────┼──────────┼───────────┤
     * │ AdMob    │ Splash       │ ✓ 成功   │ $4.80     │
     * │ TopOn    │ Splash       │ ✓ 成功   │ $3.50     │
     * │ Pangle   │ Splash       │ ✗ 超时   │ --        │
     * └──────────┴──────────────┴──────────┴───────────┘
     * ```
     * 
     * @param tag 日志标签（如 "SplashBidding"）
     * @param entries 竞价条目列表
     */
    fun logBiddingTable(tag: String, entries: List<BiddingLogEntry>) {
        if (!AdLogger.isLogEnabled()) return
        
        val header = "┌──────────┬──────────────┬──────────┬───────────┐"
        val titleRow = "│ 平台     │ 广告类型     │ 状态     │ eCPM      │"
        val separator = "├──────────┼──────────────┼──────────┼───────────┤"
        val footer = "└──────────┴──────────────┴──────────┴───────────┘"
        
        Log.d(TAG, "[$tag] $header")
        Log.d(TAG, "[$tag] $titleRow")
        Log.d(TAG, "[$tag] $separator")
        
        entries.forEach { entry ->
            val row = String.format(
                "│ %-8s │ %-12s │ %-8s │ %-9s │",
                entry.platform.take(8),
                entry.adType.take(12),
                entry.status.take(8),
                entry.ecpm.take(9)
            )
            Log.d(TAG, "[$tag] $row")
        }
        
        Log.d(TAG, "[$tag] $footer")
    }

    /**
     * 输出竞价胜出者
     * 
     * @param tag 日志标签
     * @param platform 胜出平台
     * @param adType 胜出广告类型
     * @param ecpm eCPM 值
     */
    fun logWinner(tag: String, platform: String, adType: String, ecpm: Double) {
        if (!AdLogger.isLogEnabled()) return
        Log.d(TAG, "[$tag] ★ 胜出者: $platform $adType (eCPM: \$${String.format("%.2f", ecpm)})")
    }

    /**
     * 输出竞价失败
     * 
     * @param tag 日志标签
     * @param reason 失败原因
     */
    fun logBiddingFailed(tag: String, reason: String) {
        if (!AdLogger.isLogEnabled()) return
        Log.w(TAG, "[$tag] ✗ 竞价失败: $reason")
    }

    /**
     * 输出竞价开始
     * 
     * @param tag 日志标签
     * @param platforms 参与竞价的平台列表
     * @param timeoutMs 超时时间（毫秒）
     */
    fun logBiddingStart(tag: String, platforms: List<String>, timeoutMs: Long) {
        if (!AdLogger.isLogEnabled()) return
        Log.d(TAG, "[$tag] ▶ 竞价开始 | 平台: ${platforms.joinToString(", ")} | 超时: ${timeoutMs}ms")
    }

    /**
     * 输出竞价结束
     * 
     * @param tag 日志标签
     * @param durationMs 耗时（毫秒）
     */
    fun logBiddingEnd(tag: String, durationMs: Long) {
        if (!AdLogger.isLogEnabled()) return
        Log.d(TAG, "[$tag] ■ 竞价结束 | 耗时: ${durationMs}ms")
    }

    /**
     * 输出平台加载状态
     * 
     * @param tag 日志标签
     * @param platform 平台名称
     * @param success 是否成功
     * @param ecpm eCPM（成功时提供）
     * @param errorMsg 错误信息（失败时提供）
     */
    fun logPlatformResult(
        tag: String,
        platform: String,
        adType: String,
        success: Boolean,
        ecpm: Double? = null,
        errorMsg: String? = null
    ) {
        if (!AdLogger.isLogEnabled()) return
        
        if (success && ecpm != null) {
            Log.d(TAG, "[$tag] ✓ $platform $adType 加载成功 | eCPM: \$${String.format("%.2f", ecpm)}")
        } else {
            Log.w(TAG, "[$tag] ✗ $platform $adType 加载失败 | 原因: ${errorMsg ?: "未知"}")
        }
    }

    /**
     * 从竞价结果生成日志条目
     */
    fun createEntry(
        platform: String,
        adType: String,
        success: Boolean,
        ecpm: Double?
    ): BiddingLogEntry {
        return BiddingLogEntry(
            platform = platform,
            adType = adType,
            status = if (success) "✓ 成功" else "✗ 失败",
            ecpm = if (ecpm != null && ecpm > 0) "\$${String.format("%.2f", ecpm)}" else "--"
        )
    }

    // ==================== 两层竞价日志 ====================

    /**
     * 竞价条目（含频控信息）
     */
    data class BiddingEntry(
        val platform: String,
        val adType: String,
        val status: EntryStatus,
        val ecpm: Double,
        val frequencyInfo: FrequencyInfo? = null
    )

    enum class EntryStatus(val display: String) {
        READY("✓ 就绪"),
        NO_CACHE("✗ 无缓存"),
        TIMEOUT("✗ 超时"),
        DISABLED("- 禁用"),
        FREQ_LIMITED("✗ 频控")
    }

    /**
     * 频控信息
     */
    data class FrequencyInfo(
        val dailyShow: Int,
        val maxDailyShow: Int,
        val isIntervalOk: Boolean = true,
        val currentIntervalSec: Int? = null,
        val minIntervalSec: Int? = null
    ) {
        fun toDisplayString(): String {
            return when {
                !isIntervalOk && currentIntervalSec != null && minIntervalSec != null ->
                    "⏳ 间隔 (${currentIntervalSec}s < ${minIntervalSec}s)"
                dailyShow >= maxDailyShow -> "✗ 超限 ($dailyShow/$maxDailyShow)"
                maxDailyShow > 0 -> "✓ 可用 ($dailyShow/$maxDailyShow)"
                else -> "- 未配置"
            }
        }
    }

    /**
     * 输出两层竞价完整日志
     */
    fun logTwoLayerBidding(
        scene: String,
        layer1Name: String,
        layer1Entries: List<BiddingEntry>,
        layer1Winner: BiddingEntry?,
        layer2Name: String,
        layer2Entries: List<BiddingEntry>,
        layer2Winner: BiddingEntry?,
        finalWinner: BiddingEntry?,
        durationMs: Long
    ) {
        if (!AdLogger.isLogEnabled()) return

        val tag = "${scene}TwoLayer"
        
        // 标题
        Log.d(TAG, "[$tag] ╔══════════════════════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "[$tag] ║                         🎯 ${scene}两层竞价                                    ║")
        Log.d(TAG, "[$tag] ╠══════════════════════════════════════════════════════════════════════════════╣")
        
        // 第一层
        Log.d(TAG, "[$tag] ║ 📊 第一层: $layer1Name")
        logLayerTable(tag, layer1Entries)
        if (layer1Winner != null) {
            Log.d(TAG, "[$tag] ║ 🏆 胜出: ${layer1Winner.platform} - ${layer1Winner.adType} (eCPM: \$${String.format("%.4f", layer1Winner.ecpm)})")
        } else {
            Log.d(TAG, "[$tag] ║ ❌ 无胜出")
        }
        
        Log.d(TAG, "[$tag] ╟──────────────────────────────────────────────────────────────────────────────╢")
        
        // 第二层
        Log.d(TAG, "[$tag] ║ 📊 第二层: $layer2Name")
        logLayerTable(tag, layer2Entries)
        if (layer2Winner != null) {
            Log.d(TAG, "[$tag] ║ 🏆 胜出: ${layer2Winner.platform} - ${layer2Winner.adType} (eCPM: \$${String.format("%.4f", layer2Winner.ecpm)})")
        } else {
            Log.d(TAG, "[$tag] ║ ❌ 无胜出")
        }
        
        Log.d(TAG, "[$tag] ╠══════════════════════════════════════════════════════════════════════════════╣")
        
        // 最终胜出
        if (finalWinner != null) {
            Log.d(TAG, "[$tag] ║ ⭐ 最终胜出: ${finalWinner.platform} - ${finalWinner.adType} (eCPM: \$${String.format("%.4f", finalWinner.ecpm)}) | 耗时: ${durationMs}ms")
        } else {
            Log.w(TAG, "[$tag] ║ ❌ 竞价失败，无可用广告 | 耗时: ${durationMs}ms")
        }
        Log.d(TAG, "[$tag] ╚══════════════════════════════════════════════════════════════════════════════╝")
    }

    private fun logLayerTable(tag: String, entries: List<BiddingEntry>) {
        Log.d(TAG, "[$tag] ║ 平台     │ 广告类型       │ 状态     │ eCPM        │ 频控状态")
        Log.d(TAG, "[$tag] ║──────────┼────────────────┼──────────┼─────────────┼─────────────────────")
        
        entries.forEach { entry ->
            val freqDisplay = entry.frequencyInfo?.toDisplayString() ?: "- 未配置"
            Log.d(TAG, "[$tag] ║ ${entry.platform.padEnd(8)} │ ${entry.adType.padEnd(14)} │ ${entry.status.display.padEnd(8)} │ \$${String.format("%.4f", entry.ecpm).padEnd(10)} │ $freqDisplay")
        }
    }

    /**
     * 输出单层竞价日志（带频控）
     */
    fun logSingleLayerBidding(
        scene: String,
        entries: List<BiddingEntry>,
        winner: BiddingEntry?,
        durationMs: Long
    ) {
        if (!AdLogger.isLogEnabled()) return

        val tag = "${scene}Bidding"
        
        Log.d(TAG, "[$tag] ╔══════════════════════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "[$tag] ║                         🎯 ${scene}广告竞价                                   ║")
        Log.d(TAG, "[$tag] ╠══════════════════════════════════════════════════════════════════════════════╣")
        
        logLayerTable(tag, entries)
        
        Log.d(TAG, "[$tag] ╠══════════════════════════════════════════════════════════════════════════════╣")
        if (winner != null) {
            Log.d(TAG, "[$tag] ║ ⭐ 胜出: ${winner.platform} - ${winner.adType} (eCPM: \$${String.format("%.4f", winner.ecpm)}) | 耗时: ${durationMs}ms")
        } else {
            Log.w(TAG, "[$tag] ║ ❌ 竞价失败，无可用广告 | 耗时: ${durationMs}ms")
        }
        Log.d(TAG, "[$tag] ╚══════════════════════════════════════════════════════════════════════════════╝")
    }
}
