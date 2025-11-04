package net.corekit.monetize.ads.interceptor

import android.content.Context
import net.corekit.core.ext.DataStoreBoolDelegate
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.config.AdConfig
import net.corekit.monetize.ads.log.AdLogger

/**
 * 广告拦截器接口
 */
interface AdInterceptor {
    /**
     * 拦截广告操作
     * @param context 上下文
     * @param adConfig 广告配置
     * @return AdResult 拦截结果
     */
    suspend fun intercept(context: Context, adConfig: AdConfig): AdResult<Unit>
}

/**
 * 展示次数限制拦截器
 */
class ShowCountLimitInterceptor : AdInterceptor {
    companion object {
        private const val TAG = "AdModule"
    }
    
    override suspend fun intercept(context: Context, adConfig: AdConfig): AdResult<Unit> {
        // 检查日展示次数
        val dailyShow = adConfig.getDailyShowCount()
        if (dailyShow >= adConfig.getMaxDailyShow()) {
            AdLogger.w("[${adConfig.getConfigKey()}] 超出每日展示限制: $dailyShow/${adConfig.getMaxDailyShow()}")
            return AdResult.Failure(
                AdException(
                    code = -1,
                    message = "超出每日展示限制"
                )
            )
        }
        
        return AdResult.Success(Unit)
    }
}

/**
 * 展示间隔限制拦截器
 */
class ShowIntervalLimitInterceptor : AdInterceptor {
    companion object {
        private const val TAG = "AdModule"
    }
    
    override suspend fun intercept(context: Context, adConfig: AdConfig): AdResult<Unit> {
        // 检查展示间隔
        val interval = adConfig.getLastShowInterval()
        
        // 如果间隔为负数或异常值，说明系统时间被修改过，重置时间记录
        if (interval < 0) {
            AdLogger.w("[${adConfig.getConfigKey()}] 检测到系统时间异常，重置展示时间记录")
            adConfig.resetLastShowTime()
            return AdResult.Success(Unit)
        }

        val minInterval = adConfig.getMinInterval()

        AdLogger.d("lastInterval: $interval s,config interval:$minInterval s")

        if (interval < minInterval) {
            AdLogger.w("[${adConfig.getConfigKey()}] 展示间隔过短: ${interval}s < ${minInterval}s")
            return AdResult.Failure(
                AdException(
                    code = -2,
                    message = "展示间隔过短，请稍后再试"
                )
            )
        }
        
        return AdResult.Success(Unit)
    }
}

/**
 * 点击限制拦截器
 */
class ClickLimitInterceptor : AdInterceptor {
    companion object {
        private const val TAG = "AdModule"
    }
    
    override suspend fun intercept(context: Context, adConfig: AdConfig): AdResult<Unit> {
        // 检查日点击次数
        val dailyClick = adConfig.getDailyClickCount()
        if (dailyClick >= adConfig.getMaxDailyClick()) {
            AdLogger.w("[${adConfig.getConfigKey()}] 超出每日点击限制: $dailyClick/${adConfig.getMaxDailyClick()}")
            return AdResult.Failure(
                AdException(
                    code = -3,
                    message = "超出每日点击限制"
                )
            )
        }
        
        return AdResult.Success(Unit)
    }
}

/**
 * 全局广告开关拦截器
 * 使用临时变量控制全局广告的开启和关闭
 */
class GlobalAdSwitchInterceptor : AdInterceptor {
    companion object {
        private const val TAG = "GlobalAdSwitch"

        private var _isGlobalAdEnabled by DataStoreBoolDelegate("pdf_a7k9m3x5", true)

        /**
         * 开启全局广告
         */
        fun enableGlobalAd() {
            _isGlobalAdEnabled = true
            AdLogger.d("[$TAG] 全局广告已开启")
        }
        
        /**
         * 关闭全局广告
         */
        fun disableGlobalAd() {
            _isGlobalAdEnabled = false
            AdLogger.d("[$TAG] 全局广告已关闭")
        }
        
        /**
         * 获取当前全局广告状态
         */
        fun isGlobalAdEnabled(): Boolean = _isGlobalAdEnabled
        
        /**
         * 切换全局广告状态
         */
        fun toggleGlobalAd() {
            _isGlobalAdEnabled = !_isGlobalAdEnabled
            AdLogger.d("[$TAG] 全局广告状态已切换为: ${if (_isGlobalAdEnabled) "开启" else "关闭"}")
        }
    }
    
    override suspend fun intercept(context: Context, adConfig: AdConfig): AdResult<Unit> {
        if (!_isGlobalAdEnabled) {
            AdLogger.w("[${adConfig.getConfigKey()}] 全局广告已关闭，跳过广告展示")
            return AdResult.Failure(
                AdException(
                    code = -100,
                    message = "全局广告已关闭"
                )
            )
        }
        
        return AdResult.Success(Unit)
    }
}

/**
 * 拦截器链
 */
class InterceptorChain(
    private val interceptors: List<AdInterceptor>
) : AdInterceptor {
    override suspend fun intercept(context: Context, adConfig: AdConfig): AdResult<Unit> {
        interceptors.forEach { interceptor ->
            when (val result = interceptor.intercept(context, adConfig)) {
                is AdResult.Failure -> {
                    // 将拦截器信息拼接到message中
                    val interceptorName = interceptor::class.simpleName ?: "Unknown"
                    val interceptorDetails = getInterceptorDetails(interceptor, adConfig)
                    val enhancedMessage = "[Interceptor: $interceptorName, Details: $interceptorDetails]"
                    
                    val enhancedException = AdException(
                        code = result.error.code,
                        message = enhancedMessage,
                        cause = result.error.cause
                    )
                    return AdResult.Failure(enhancedException)
                }
                else -> { /* continue */ }
            }
        }
        return AdResult.Success(Unit)
    }
    
    /**
     * 获取拦截器的详细信息
     */
    private fun getInterceptorDetails(interceptor: AdInterceptor, adConfig: AdConfig): String {
        return when (interceptor) {
            is GlobalAdSwitchInterceptor -> "Global ad switch is disabled"
            is ShowCountLimitInterceptor -> "Daily show limit exceeded: ${adConfig.getDailyShowCount()}/${adConfig.getMaxDailyShow()}"
            is ShowIntervalLimitInterceptor -> "Show interval too short: ${adConfig.getLastShowInterval()}s < ${adConfig.getMinInterval()}s"
            is ClickLimitInterceptor -> "Daily click limit exceeded: ${adConfig.getDailyClickCount()}/${adConfig.getMaxDailyClick()}"
            else -> "Unknown interceptor"
        }
    }
} 