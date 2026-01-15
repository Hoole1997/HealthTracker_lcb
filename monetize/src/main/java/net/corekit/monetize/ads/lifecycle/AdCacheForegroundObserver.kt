package net.corekit.monetize.ads.lifecycle

import android.content.Context
import com.healthtracker.framework.lifecycle.AppForegroundObserver
import com.healthtracker.framework.lifecycle.AppLifecycleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.bidding.BiddingPlatformController
import net.corekit.monetize.ads.bidding.RewardTwoLayerBiddingManager
import net.corekit.monetize.ads.log.AdLogger

/**
 * 广告预加载前台观察者
 * 
 * 当应用从后台返回前台时，异步触发广告预加载。
 * 竞价在展示时实时执行，此观察者仅负责预加载。
 */
class AdCacheForegroundObserver(
    private val context: Context
) : AppForegroundObserver {
    
    companion object {
        private const val TAG = "AdPreloadForeground"
        
        // 防止频繁触发的最小间隔（毫秒）
        private const val MIN_INTERVAL_MS = 30_000L
        
        @Volatile
        private var lastPreloadTime = 0L
    }
    
    override fun onAppForeground() {
        // 检查是否启用了多平台竞价
        if (!BiddingPlatformController.isMultiPlatformBiddingEnabled()) {
            return
        }
        
        // 防抖：避免频繁触发
        val now = System.currentTimeMillis()
        if (now - lastPreloadTime < MIN_INTERVAL_MS) {
            AdLogger.d("[$TAG] 距离上次预加载不足 ${MIN_INTERVAL_MS}ms，跳过")
            return
        }
        
        AdLogger.d("[$TAG] 前台返回，触发后台广告预加载")
        lastPreloadTime = now
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RewardTwoLayerBiddingManager.preloadAll(context)
                AdLogger.d("[$TAG] 前台预加载完成")
            } catch (e: Exception) {
                AdLogger.e("[$TAG] 前台预加载失败: ${e.message}")
            }
        }
    }
    
    override fun onAppBackground() {
        // 不处理后台事件
    }
    
    override fun onScreenLocked() {
        // 不处理锁屏事件
    }
    
    override fun onScreenUnlocked() {
        // 不处理解锁事件
    }
    
    override fun onStateChanged(newState: AppLifecycleState, oldState: AppLifecycleState) {
        // 不处理通用状态变化
    }
}
