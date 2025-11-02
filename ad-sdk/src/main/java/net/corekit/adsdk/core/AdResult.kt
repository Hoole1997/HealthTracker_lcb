package net.corekit.adsdk.core

/**
 * 广告操作结果封装
 * 使用密封类实现类型安全的结果处理
 *
 * @param T 成功时返回的数据类型
 */
sealed class AdResult<out T> {
    /**
     * 操作成功
     * @param data 成功返回的数据
     */
    data class Success<T>(val data: T) : AdResult<T>()

    /**
     * 操作失败
     * @param error 失败的异常信息
     */
    data class Failure(val error: AdException) : AdResult<Nothing>()

    /**
     * 加载中状态
     */
    data object Loading : AdResult<Nothing>()

    /**
     * 判断是否成功
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * 判断是否失败
     */
    val isFailure: Boolean
        get() = this is Failure

    /**
     * 判断是否加载中
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * 获取成功结果数据（失败时返回 null）
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * 获取失败异常（成功时返回 null）
     */
    fun exceptionOrNull(): AdException? = when (this) {
        is Failure -> error
        else -> null
    }

    companion object {
        /**
         * 创建成功结果
         */
        fun <T> success(data: T): AdResult<T> = Success(data)

        /**
         * 创建失败结果
         */
        fun failure(error: AdException): AdResult<Nothing> = Failure(error)

        /**
         * 创建失败结果（通过异常创建）
         */
        fun failure(throwable: Throwable): AdResult<Nothing> =
            Failure(AdException.from(throwable))

        /**
         * 创建加载中结果
         */
        fun loading(): AdResult<Nothing> = Loading
    }
}

/**
 * 对结果进行转换
 * @param transform 转换函数
 */
inline fun <T, R> AdResult<T>.map(transform: (T) -> R): AdResult<R> {
    return when (this) {
        is AdResult.Success -> AdResult.Success(transform(data))
        is AdResult.Failure -> AdResult.Failure(error)
        is AdResult.Loading -> AdResult.Loading
    }
}

/**
 * 对失败结果进行转换
 * @param transform 转换函数
 */
inline fun <T> AdResult<T>.mapError(transform: (AdException) -> AdException): AdResult<T> {
    return when (this) {
        is AdResult.Success -> this
        is AdResult.Failure -> AdResult.Failure(transform(error))
        is AdResult.Loading -> this
    }
}

/**
 * 当成功时执行操作
 * @param action 要执行的操作
 */
inline fun <T> AdResult<T>.onSuccess(action: (T) -> Unit): AdResult<T> {
    if (this is AdResult.Success) {
        action(data)
    }
    return this
}

/**
 * 当失败时执行操作
 * @param action 要执行的操作
 */
inline fun <T> AdResult<T>.onFailure(action: (AdException) -> Unit): AdResult<T> {
    if (this is AdResult.Failure) {
        action(error)
    }
    return this
}
