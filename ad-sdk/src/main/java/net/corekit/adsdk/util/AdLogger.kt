package net.corekit.adsdk.util

import android.util.Log

/**
 * 广告 SDK 日志工具类
 * 统一管理日志输出，方便调试和问题排查
 */
internal object AdLogger {
    private const val TAG = "AdSdk"

    /**
     * 是否启用日志输出
     * 可通过 setEnabled() 动态控制
     */
    @Volatile
    private var isEnabled = true

    /**
     * 最低日志级别
     * 低于此级别的日志将不会输出
     */
    @Volatile
    private var minLevel = Log.DEBUG

    /**
     * 自定义日志处理器（可选）
     * 用于将日志输出到自定义目标（如文件、远程服务器）
     */
    @Volatile
    private var customHandler: ((level: Int, tag: String, message: String, throwable: Throwable?) -> Unit)? = null

    /**
     * 启用/禁用日志
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * 设置最低日志级别
     * @param level 日志级别（Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR）
     */
    fun setMinLevel(level: Int) {
        minLevel = level
    }

    /**
     * 设置自定义日志处理器
     */
    fun setCustomHandler(handler: ((level: Int, tag: String, message: String, throwable: Throwable?) -> Unit)?) {
        customHandler = handler
    }

    /**
     * Debug 日志
     */
    fun d(message: String) {
        log(Log.DEBUG, message, null)
    }

    /**
     * Debug 日志（带格式化）
     */
    fun d(format: String, vararg args: Any) {
        log(Log.DEBUG, String.format(format, *args), null)
    }

    /**
     * Info 日志
     */
    fun i(message: String) {
        log(Log.INFO, message, null)
    }

    /**
     * Info 日志（带格式化）
     */
    fun i(format: String, vararg args: Any) {
        log(Log.INFO, String.format(format, *args), null)
    }

    /**
     * Warning 日志
     */
    fun w(message: String) {
        log(Log.WARN, message, null)
    }

    /**
     * Warning 日志（带格式化）
     */
    fun w(format: String, vararg args: Any) {
        log(Log.WARN, String.format(format, *args), null)
    }

    /**
     * Warning 日志（带异常）
     */
    fun w(message: String, throwable: Throwable) {
        log(Log.WARN, message, throwable)
    }

    /**
     * Error 日志
     */
    fun e(message: String) {
        log(Log.ERROR, message, null)
    }

    /**
     * Error 日志（带格式化）
     */
    fun e(format: String, vararg args: Any) {
        log(Log.ERROR, String.format(format, *args), null)
    }

    /**
     * Error 日志（带异常）
     */
    fun e(message: String, throwable: Throwable) {
        log(Log.ERROR, message, throwable)
    }

    /**
     * Verbose 日志
     */
    fun v(message: String) {
        log(Log.VERBOSE, message, null)
    }

    /**
     * 核心日志输出方法
     */
    private fun log(level: Int, message: String, throwable: Throwable?) {
        if (!isEnabled || level < minLevel) {
            return
        }

        // 调用自定义处理器
        customHandler?.invoke(level, TAG, message, throwable)

        // 系统日志输出
        when (level) {
            Log.VERBOSE -> Log.v(TAG, message, throwable)
            Log.DEBUG -> Log.d(TAG, message, throwable)
            Log.INFO -> Log.i(TAG, message, throwable)
            Log.WARN -> Log.w(TAG, message, throwable)
            Log.ERROR -> Log.e(TAG, message, throwable)
        }
    }

    /**
     * 获取调用栈信息（用于调试）
     */
    fun getStackTrace(): String {
        val stackTrace = Thread.currentThread().stackTrace
        return stackTrace.joinToString("\n") { it.toString() }
    }
}
