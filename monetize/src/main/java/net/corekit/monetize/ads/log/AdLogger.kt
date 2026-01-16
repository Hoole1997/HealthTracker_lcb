package net.corekit.monetize.ads.log

import android.util.Log
import com.healthtracker.framework.BuildState
import net.corekit.monetize.BuildConfig

/**
 * 广告日志工具类
 * 提供统一的日志输出控制和管理
 * 
 * 日志格式规范：[子标签] 事件描述 | 参数1: 值1 | 参数2: 值2
 * 
 * 子标签定义：
 * - Native: 原生广告
 * - Interstitial: 插屏广告
 * - Rewarded: 激励广告
 * - RewardedInterstitial: 插页激励广告
 * - Banner: 横幅广告
 * - AppOpen: 开屏广告
 * - FullNative: 全屏原生广告
 * - Bidding: 竞价管理
 * - Preload: 预加载
 * - FreqControl: 频控
 * - Config: 配置
 * - Pangle: Pangle 平台
 * - TopOn: TopOn 平台
 */
object AdLogger {
    private const val TAG = "AdModule"
    
    /**
     * 日志开关，默认为 debug 模式开启
     */
    private var isLogEnabled = BuildState.debug
    
    /**
     * 详细日志模式开关
     * 开启后会输出更详细的日志（如竞价表格）
     */
    var verboseMode: Boolean = false
    
    /**
     * 设置日志开关
     * @param enabled 是否启用日志
     */
    fun setLogEnabled(enabled: Boolean) {
        isLogEnabled = enabled
    }
    
    /**
     * 获取日志开关状态
     * @return 是否启用日志
     */
    fun isLogEnabled(): Boolean = isLogEnabled
    
    // ==================== 带子标签的日志方法（推荐使用） ====================
    
    /**
     * Debug日志（带子标签）
     * @param subTag 子标签，如 "Native", "Bidding"
     * @param message 日志消息
     */
    fun logD(subTag: String, message: String) {
        if (isLogEnabled) {
            Log.d(TAG, "[$subTag] $message")
        }
    }
    
    /**
     * Debug日志（带子标签和参数）
     * @param subTag 子标签
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun logD(subTag: String, message: String, vararg args: Any?) {
        if (isLogEnabled) {
            Log.d(TAG, "[$subTag] ${message.format(*args)}")
        }
    }
    
    /**
     * Warning日志（带子标签）
     * @param subTag 子标签
     * @param message 日志消息
     */
    fun logW(subTag: String, message: String) {
        if (isLogEnabled) {
            Log.w(TAG, "[$subTag] $message")
        }
    }
    
    /**
     * Warning日志（带子标签和参数）
     * @param subTag 子标签
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun logW(subTag: String, message: String, vararg args: Any?) {
        if (isLogEnabled) {
            Log.w(TAG, "[$subTag] ${message.format(*args)}")
        }
    }
    
    /**
     * Error日志（带子标签）
     * @param subTag 子标签
     * @param message 日志消息
     */
    fun logE(subTag: String, message: String) {
        if (isLogEnabled) {
            Log.e(TAG, "[$subTag] $message")
        }
    }
    
    /**
     * Error日志（带子标签和参数）
     * @param subTag 子标签
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun logE(subTag: String, message: String, vararg args: Any?) {
        if (isLogEnabled) {
            Log.e(TAG, "[$subTag] ${message.format(*args)}")
        }
    }
    
    /**
     * Error日志（带子标签和异常）
     * @param subTag 子标签
     * @param message 日志消息
     * @param throwable 异常对象
     */
    fun logE(subTag: String, message: String, throwable: Throwable?) {
        if (isLogEnabled) {
            Log.e(TAG, "[$subTag] $message", throwable)
        }
    }
    
    /**
     * Info日志（带子标签）
     * @param subTag 子标签
     * @param message 日志消息
     */
    fun logI(subTag: String, message: String) {
        if (isLogEnabled) {
            Log.i(TAG, "[$subTag] $message")
        }
    }
    
    /**
     * Info日志（带子标签和参数）
     * @param subTag 子标签
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun logI(subTag: String, message: String, vararg args: Any?) {
        if (isLogEnabled) {
            Log.i(TAG, "[$subTag] ${message.format(*args)}")
        }
    }
    
    /**
     * 仅在详细模式下输出的日志
     * @param subTag 子标签
     * @param message 日志消息
     */
    fun verbose(subTag: String, message: String) {
        if (isLogEnabled && verboseMode) {
            Log.d(TAG, "[$subTag] $message")
        }
    }
    
    /**
     * 仅在详细模式下输出的日志（带参数）
     * @param subTag 子标签
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun verbose(subTag: String, message: String, vararg args: Any?) {
        if (isLogEnabled && verboseMode) {
            Log.d(TAG, "[$subTag] ${message.format(*args)}")
        }
    }
    
    // ==================== 原有日志方法（兼容旧代码） ====================
    
    /**
     * Debug日志
     * @param message 日志消息
     */
    fun d(message: String) {
        if (isLogEnabled) {
            Log.d(TAG, message)
        }
    }
    
    /**
     * Debug日志（带参数）
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun d(message: String, vararg args: Any?) {
        if (isLogEnabled) {
            Log.d(TAG, message.format(*args))
        }
    }
    
    /**
     * Warning日志
     * @param message 日志消息
     */
    fun w(message: String) {
        if (isLogEnabled) {
            Log.w(TAG, message)
        }
    }
    
    /**
     * Warning日志（带参数）
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun w(message: String, vararg args: Any?) {
        if (isLogEnabled) {
            Log.w(TAG, message.format(*args))
        }
    }
    
    /**
     * Error日志
     * @param message 日志消息
     */
    fun e(message: String) {
        if (isLogEnabled) {
            Log.e(TAG, message)
        }
    }
    
    /**
     * Error日志（带异常）
     * @param message 日志消息
     * @param throwable 异常对象
     */
    fun e(message: String, throwable: Throwable?) {
        if (isLogEnabled) {
            Log.e(TAG, message, throwable)
        }
    }
    
    /**
     * Error日志（带参数）
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun e(message: String, vararg args: Any?) {
        if (isLogEnabled) {
            Log.e(TAG, message.format(*args))
        }
    }
    
    /**
     * Error日志（带参数和异常）
     * @param message 日志消息模板
     * @param throwable 异常对象
     * @param args 参数列表
     */
    fun e(message: String, throwable: Throwable?, vararg args: Any?) {
        if (isLogEnabled) {
            Log.e(TAG, message.format(*args), throwable)
        }
    }
    
    /**
     * Info日志
     * @param message 日志消息
     */
    fun i(message: String) {
        if (isLogEnabled) {
            Log.i(TAG, message)
        }
    }
    
    /**
     * Info日志（带参数）
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun i(message: String, vararg args: Any?) {
        if (isLogEnabled) {
            Log.i(TAG, message.format(*args))
        }
    }
    
    /**
     * Verbose日志
     * @param message 日志消息
     */
    fun v(message: String) {
        if (isLogEnabled) {
            Log.v(TAG, message)
        }
    }
    
    /**
     * Verbose日志（带参数）
     * @param message 日志消息模板
     * @param args 参数列表
     */
    fun v(message: String, vararg args: Any?) {
        if (isLogEnabled) {
            Log.v(TAG, message.format(*args))
        }
    }
} 