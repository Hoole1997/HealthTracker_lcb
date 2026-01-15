package net.corekit.monetize.ui.debug

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.InterstitialAds
import net.corekit.monetize.ads.LaunchAds
import net.corekit.monetize.ads.RewardedAds
import net.corekit.monetize.ads.bidding.BiddingPlatformController
import net.corekit.monetize.ads.bidding.BiddingWinner
import net.corekit.monetize.ads.bidding.SplashTwoLayerBiddingManager
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.log.AdLogger

/**
 * 广告调试面板
 * 
 * 仅在 Debug 版本可用，提供：
 * - 查看缓存状态
 * - 切换测试场景（模拟不同平台胜出）
 * - 触发竞价测试
 * - 查看频控状态
 * 
 * 使用示例：
 * ```kotlin
 * // 在 Settings 页面添加入口
 * if (BuildConfig.DEBUG) {
 *     settingItem("广告调试面板") {
 *         AdDebugPanel.showDebugDialog(requireActivity())
 *     }
 * }
 * ```
 */
object AdDebugPanel {

    private const val TAG = "AdDebugPanel"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * 测试场景枚举
     */
    enum class TestScenario(val description: String) {
        NORMAL("正常模式（真实 eCPM）"),
        ADMOB_WINS("AdMob 胜出（Mock eCPM: $10.00）"),
        TOPON_WINS("TopOn 胜出（Mock eCPM: $10.00）"),
        PANGLE_WINS("Pangle 胜出（Mock eCPM: $10.00）"),
        ALL_FAIL("全部失败（Mock eCPM: $0.00）")
    }

    private var currentScenario = TestScenario.NORMAL

    /**
     * 显示调试面板对话框
     */
    fun showDebugDialog(activity: Activity) {
        if (!BuildConfig.DEBUG) {
            AdLogger.w("[$TAG] 调试面板仅在 Debug 版本可用")
            return
        }

        val items = arrayOf(
            "📊 查看广告缓存状态",
            "🔧 查看竞价配置",
            "📈 查看频控状态",
            "──────────────",
            "🎯 切换测试场景 (当前: ${currentScenario.description})",
            "🔄 重置测试配置",
            "──────────────",
            "🚀 触发开屏竞价",
            "🎬 触发激励竞价 (插页)",
            "📢 触发插屏展示"
        )

        AlertDialog.Builder(activity)
            .setTitle("🔧 广告调试面板")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showCacheStatus(activity)
                    1 -> showBiddingConfig(activity)
                    2 -> showFrequencyStatus(activity)
                    // 3 是分隔线，忽略
                    4 -> showTestScenarioDialog(activity)
                    5 -> resetTestConfig(activity)
                    // 6 是分隔线，忽略
                    7 -> triggerSplashBidding(activity)
                    8 -> triggerRewardBidding(activity)
                    9 -> triggerInterstitial(activity)
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    /**
     * 显示缓存状态
     */
    private fun showCacheStatus(context: Context) {
        val sb = StringBuilder()
        sb.appendLine("📦 广告缓存状态\n")
        
        sb.appendLine("AdMob:")
        sb.appendLine("  • 开屏: ${if (LaunchAds.getInstance().hasCachedAd()) "✅ 有缓存" else "❌ 无缓存"}")
        sb.appendLine("  • 插屏: ${if (InterstitialAds.getInstance().hasCachedAd()) "✅ 有缓存" else "❌ 无缓存"}")
        sb.appendLine("  • 激励: ${if (RewardedAds.getInstance().hasCachedAd()) "✅ 有缓存" else "❌ 无缓存"}")
        
        // TODO: 添加 Pangle/TopOn 缓存状态

        showInfoDialog(context, "广告缓存状态", sb.toString())
    }

    /**
     * 显示竞价配置
     */
    private fun showBiddingConfig(context: Context) {
        val sb = StringBuilder()
        sb.appendLine("⚙️ 竞价配置\n")
        
        sb.appendLine("全局设置:")
        sb.appendLine("  • 竞价开关: ${if (BiddingConfigManager.isBiddingEnabled()) "✅ 开启" else "❌ 关闭"}")
        sb.appendLine("  • 两层竞价: ${if (BiddingConfigManager.isTwoLayerBiddingEnabled()) "✅ 开启" else "❌ 关闭"}")
        sb.appendLine("  • 超时时间: ${BiddingConfigManager.getBiddingTimeoutMs()}ms")
        sb.appendLine("  • 平台频控: ${if (BiddingConfigManager.isPlatformFrequencyEnabled()) "✅ 开启" else "❌ 关闭"}")
        
        sb.appendLine("\n平台状态:")
        BiddingWinner.values().forEach { platform ->
            val enabled = BiddingPlatformController.isPlatformEnabled(platform)
            val priority = BiddingPlatformController.getPlatformPriority(platform)
            sb.appendLine("  • ${platform.name}: ${if (enabled) "✅" else "❌"} (优先级: $priority)")
        }

        sb.appendLine("\n测试模式:")
        sb.appendLine("  • 状态: ${if (BiddingPlatformController.isTestMode()) "✅ 开启" else "❌ 关闭"}")
        sb.appendLine("  • 当前场景: ${currentScenario.description}")

        showInfoDialog(context, "竞价配置", sb.toString())
    }

    /**
     * 显示频控状态
     */
    private fun showFrequencyStatus(context: Context) {
        val sb = StringBuilder()
        sb.appendLine("📈 频控状态\n")
        
        sb.appendLine("平台级频控: ${if (PlatformFrequencyManager.isEnabled()) "✅ 开启" else "❌ 关闭"}")
        
        if (PlatformFrequencyManager.isEnabled()) {
            val allStatus = PlatformFrequencyManager.getAllPlatformStatus()
            
            sb.appendLine("\n平台统计:")
            allStatus.forEach { (key, status) ->
                sb.appendLine("  • $key:")
                sb.appendLine("    展示: ${status.dailyShow}/${status.maxDailyShow} ${if (status.isShowLimitReached) "⚠ 超限" else ""}")
                sb.appendLine("    点击: ${status.dailyClick}/${status.maxDailyClick} ${if (status.isClickLimitReached) "⚠ 超限" else ""}")
            }
        }

        showInfoDialog(context, "频控状态", sb.toString())
    }

    /**
     * 显示测试场景选择对话框
     */
    private fun showTestScenarioDialog(activity: Activity) {
        val scenarios = TestScenario.values()
        val items = scenarios.map { 
            "${if (it == currentScenario) "● " else "○ "}${it.description}" 
        }.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle("选择测试场景")
            .setItems(items) { _, which ->
                currentScenario = scenarios[which]
                applyTestScenario(currentScenario)
                Toast.makeText(activity, "已切换到: ${currentScenario.description}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 应用测试场景
     */
    private fun applyTestScenario(scenario: TestScenario) {
        when (scenario) {
            TestScenario.NORMAL -> {
                BiddingPlatformController.setTestMode(false)
            }
            TestScenario.ADMOB_WINS -> {
                BiddingPlatformController.setTestMode(true)
                BiddingPlatformController.setMockEcpm(BiddingWinner.ADMOB, 10.0)
                BiddingPlatformController.setMockEcpm(BiddingWinner.TOPON, 1.0)
                BiddingPlatformController.setMockEcpm(BiddingWinner.PANGLE, 1.0)
            }
            TestScenario.TOPON_WINS -> {
                BiddingPlatformController.setTestMode(true)
                BiddingPlatformController.setMockEcpm(BiddingWinner.ADMOB, 1.0)
                BiddingPlatformController.setMockEcpm(BiddingWinner.TOPON, 10.0)
                BiddingPlatformController.setMockEcpm(BiddingWinner.PANGLE, 1.0)
            }
            TestScenario.PANGLE_WINS -> {
                BiddingPlatformController.setTestMode(true)
                BiddingPlatformController.setMockEcpm(BiddingWinner.ADMOB, 1.0)
                BiddingPlatformController.setMockEcpm(BiddingWinner.TOPON, 1.0)
                BiddingPlatformController.setMockEcpm(BiddingWinner.PANGLE, 10.0)
            }
            TestScenario.ALL_FAIL -> {
                BiddingPlatformController.setTestMode(true)
                BiddingPlatformController.setMockEcpm(BiddingWinner.ADMOB, 0.0)
                BiddingPlatformController.setMockEcpm(BiddingWinner.TOPON, 0.0)
                BiddingPlatformController.setMockEcpm(BiddingWinner.PANGLE, 0.0)
            }
        }
        AdLogger.d("[$TAG] 测试场景已切换为: ${scenario.description}")
    }

    /**
     * 重置测试配置
     */
    private fun resetTestConfig(context: Context) {
        currentScenario = TestScenario.NORMAL
        BiddingPlatformController.setTestMode(false)
        BiddingPlatformController.clearMockEcpm()
        PlatformFrequencyManager.resetAllCounts()
        Toast.makeText(context, "已重置所有测试配置", Toast.LENGTH_SHORT).show()
    }

    /**
     * 触发开屏竞价
     */
    private fun triggerSplashBidding(activity: Activity) {
        Toast.makeText(activity, "正在触发开屏竞价...", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val result = SplashTwoLayerBiddingManager.performTwoLayerBidding(activity)
                withContext(Dispatchers.Main) {
                    val winner = result.winner
                    val message = if (winner != null) {
                        "竞价完成!\n胜出: ${winner.platform} ${winner.winnerType}\neCPM: \$${String.format("%.2f", winner.ecpm)}"
                    } else {
                        "竞价失败: 无可用广告"
                    }
                    showInfoDialog(activity, "开屏竞价结果", message)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showInfoDialog(activity, "开屏竞价结果", "竞价异常: ${e.message}")
                }
            }
        }
    }

    /**
     * 触发激励竞价（实际展示插屏）
     */
    private fun triggerRewardBidding(activity: Activity) {
        Toast.makeText(activity, "正在触发激励竞价...", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val result = RewardedAds.getInstance().show(activity, "debug_panel")
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "激励广告结果: $result", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "激励广告异常: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * 触发插屏展示
     */
    private fun triggerInterstitial(activity: Activity) {
        Toast.makeText(activity, "正在触发插屏广告...", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val result = InterstitialAds.getInstance().displayAd(activity, "debug_panel")
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "插屏广告结果: $result", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "插屏广告异常: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * 显示信息对话框
     */
    private fun showInfoDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    /**
     * 检查是否可用
     */
    fun isAvailable(): Boolean = BuildConfig.DEBUG
}
