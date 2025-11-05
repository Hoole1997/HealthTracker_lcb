package net.corekit.monetize.ads.config

import android.content.Context
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.util.SpUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 广告配置管理器
 */
class AdConfig private constructor(
    private val context: Context,
    private val configKey: String,
    private val maxDailyShow: Int,
    private val maxDailyClick: Int,
    private val minInterval: Long
) {
    companion object {
        // SP存储键前缀
        private const val SP_NAME_PREFIX = "ad_config_"
        private const val KEY_DAILY_SHOW_COUNT = "daily_show_count"
        private const val KEY_DAILY_CLICK_COUNT = "daily_click_count"
        private const val KEY_LAST_SHOW_TIME = "last_show_time"
        private const val KEY_LAST_DATE = "last_date"
        
        // 默认配置
        private const val DEFAULT_MAX_DAILY_SHOW = 50  // 每日最大展示次数
        private const val DEFAULT_MAX_DAILY_CLICK = 10 // 每日最大点击次数
        private const val DEFAULT_MIN_INTERVAL = 30L   // 最小展示间隔（秒）
    }

    
    /**
     * 获取当日展示次数
     */
    fun getDailyShowCount(): Int {
        checkAndResetDaily()
        return SpUtils.getInt("${configKey}_${KEY_DAILY_SHOW_COUNT}", 0)
    }
    
    /**
     * 获取当日点击次数
     */
    fun getDailyClickCount(): Int {
        checkAndResetDaily()
        return SpUtils.getInt("${configKey}_${KEY_DAILY_CLICK_COUNT}", 0)
    }
    
    /**
     * 获取距离上次展示的间隔（秒）
     */
    fun getLastShowInterval(): Long {
        val lastShowTime = SpUtils.getLong("${configKey}_${KEY_LAST_SHOW_TIME}", 0L)
        if (lastShowTime == 0L) return Long.MAX_VALUE
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastShowTime)
    }
    
    /**
     * 获取最大每日展示次数
     */
    fun getMaxDailyShow(): Int = maxDailyShow
    
    /**
     * 获取最大每日点击次数
     */
    fun getMaxDailyClick(): Int = maxDailyClick
    
    /**
     * 获取最小展示间隔（秒）
     */
    fun getMinInterval(): Long = minInterval
    
    /**
     * 获取配置键
     */
    fun getConfigKey(): String = configKey
    
    /**
     * 记录展示
     */
    fun recordShow() {
        checkAndResetDaily()
        SpUtils.putInt("${configKey}_${KEY_DAILY_SHOW_COUNT}", getDailyShowCount() + 1)
        SpUtils.putLong("${configKey}_${KEY_LAST_SHOW_TIME}", System.currentTimeMillis())
    }
    
    /**
     * 重置上次展示时间（用于处理系统时间异常）
     */
    fun resetLastShowTime() {
        SpUtils.putLong("${configKey}_${KEY_LAST_SHOW_TIME}", 0L)
    }
    
    /**
     * 记录点击
     */
    fun recordClick() {
        AppLifecycleManager
        checkAndResetDaily()
        AppLifecycleManager.clickAdTime = System.currentTimeMillis()
        SpUtils.putInt("${configKey}_${KEY_DAILY_CLICK_COUNT}", getDailyClickCount() + 1)
    }
    
    /**
     * 检查并重置每日统计
     */
    private fun checkAndResetDaily() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val lastDate = SpUtils.getString("${configKey}_${KEY_LAST_DATE}", "")
        
        if (today != lastDate) {
            // 新的一天，重置统计
            SpUtils.apply {
                putString("${configKey}_${KEY_LAST_DATE}", today)
                putInt("${configKey}_${KEY_DAILY_SHOW_COUNT}", 0)
                putInt("${configKey}_${KEY_DAILY_CLICK_COUNT}", 0)
            }
        }
    }
    
    /**
     * 建造者
     */
    class Builder(private val context: Context, private val configKey: String) {
        private var maxDailyShow: Int = DEFAULT_MAX_DAILY_SHOW
        private var maxDailyClick: Int = DEFAULT_MAX_DAILY_CLICK
        private var minInterval: Long = DEFAULT_MIN_INTERVAL
        
        /**
         * 设置每日最大展示次数
         */
        fun setMaxDailyShow(count: Int): Builder {
            maxDailyShow = count
            return this
        }
        
        /**
         * 设置每日最大点击次数
         */
        fun setMaxDailyClick(count: Int): Builder {
            maxDailyClick = count
            return this
        }
        
        /**
         * 设置最小展示间隔（秒）
         */
        fun setMinInterval(seconds: Long): Builder {
            minInterval = seconds
            return this
        }
        
        /**
         * 构建配置实例
         */
        fun build(): AdConfig {
            return AdConfig(
                context = context.applicationContext,
                configKey = configKey,
                maxDailyShow = maxDailyShow,
                maxDailyClick = maxDailyClick,
                minInterval = minInterval
            )
        }
    }
} 