package net.corekit.adsdk.core

/**
 * 广告相关异常类
 * 封装所有广告操作可能产生的异常
 *
 * @param code 错误码
 * @param message 错误消息
 * @param cause 原始异常（可选）
 */
class AdException(
    val code: Int,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    companion object {
        // ========== 错误码定义 ==========

        /** SDK 未初始化 */
        const val ERROR_NOT_INITIALIZED = 1000

        /** 广告加载失败 */
        const val ERROR_LOAD_FAILED = 1001

        /** 广告展示失败 */
        const val ERROR_SHOW_FAILED = 1002

        /** 没有可用的广告 */
        const val ERROR_NO_AD = 1003

        /** 内部错误 */
        const val ERROR_INTERNAL = 1004

        /** 无效的广告位 ID */
        const val ERROR_INVALID_AD_UNIT_ID = 1005

        /** 广告类型不支持 */
        const val ERROR_UNSUPPORTED_AD_TYPE = 1006

        /** 配置错误 */
        const val ERROR_CONFIG = 1007

        /** 缓存已满 */
        const val ERROR_CACHE_FULL = 1008

        /** 广告已过期 */
        const val ERROR_AD_EXPIRED = 1009

        /** 超时 */
        const val ERROR_TIMEOUT = 1010

        // ========== 快捷创建方法 ==========

        /**
         * 创建 SDK 未初始化异常
         */
        fun notInitialized(): AdException {
            return AdException(
                code = ERROR_NOT_INITIALIZED,
                message = "AdSdk has not been initialized. Call AdSdkManager.initialize() first."
            )
        }

        /**
         * 创建广告加载失败异常
         * @param message 错误消息
         * @param cause 原始异常
         */
        fun loadFailed(message: String, cause: Throwable? = null): AdException {
            return AdException(
                code = ERROR_LOAD_FAILED,
                message = "Ad load failed: $message",
                cause = cause
            )
        }

        /**
         * 创建广告展示失败异常
         * @param message 错误消息
         * @param cause 原始异常
         */
        fun showFailed(message: String, cause: Throwable? = null): AdException {
            return AdException(
                code = ERROR_SHOW_FAILED,
                message = "Ad show failed: $message",
                cause = cause
            )
        }

        /**
         * 创建没有可用广告异常
         * @param adUnitId 广告位 ID
         */
        fun noAd(adUnitId: String): AdException {
            return AdException(
                code = ERROR_NO_AD,
                message = "No ad available for ad unit: $adUnitId"
            )
        }

        /**
         * 创建无效广告位 ID 异常
         * @param adUnitId 广告位 ID
         */
        fun invalidAdUnitId(adUnitId: String): AdException {
            return AdException(
                code = ERROR_INVALID_AD_UNIT_ID,
                message = "Invalid ad unit ID: $adUnitId"
            )
        }

        /**
         * 创建不支持的广告类型异常
         * @param adType 广告类型
         */
        fun unsupportedAdType(adType: AdType): AdException {
            return AdException(
                code = ERROR_UNSUPPORTED_AD_TYPE,
                message = "Unsupported ad type: ${adType.typeName}"
            )
        }

        /**
         * 创建配置错误异常
         * @param message 错误消息
         */
        fun configError(message: String): AdException {
            return AdException(
                code = ERROR_CONFIG,
                message = "Configuration error: $message"
            )
        }

        /**
         * 创建缓存已满异常
         * @param adUnitId 广告位 ID
         */
        fun cacheFull(adUnitId: String): AdException {
            return AdException(
                code = ERROR_CACHE_FULL,
                message = "Cache is full for ad unit: $adUnitId"
            )
        }

        /**
         * 创建广告已过期异常
         */
        fun adExpired(): AdException {
            return AdException(
                code = ERROR_AD_EXPIRED,
                message = "Ad has expired"
            )
        }

        /**
         * 创建超时异常
         * @param operation 操作名称
         */
        fun timeout(operation: String): AdException {
            return AdException(
                code = ERROR_TIMEOUT,
                message = "Operation timeout: $operation"
            )
        }

        /**
         * 从通用异常创建
         * @param throwable 原始异常
         */
        fun from(throwable: Throwable): AdException {
            return when (throwable) {
                is AdException -> throwable
                else -> AdException(
                    code = ERROR_INTERNAL,
                    message = throwable.message ?: "Unknown error",
                    cause = throwable
                )
            }
        }
    }

    /**
     * 获取详细的错误信息
     */
    fun getDetailedMessage(): String {
        return buildString {
            append("AdException(code=$code, message='$message'")
            if (cause != null) {
                append(", cause=${cause.javaClass.simpleName}: ${cause.message}")
            }
            append(")")
        }
    }

    override fun toString(): String = getDetailedMessage()
}
