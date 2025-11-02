package net.corekit.adsdk.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 支持业务超时语义的挂起等待工具。
 * 超时仅影响调用方的等待时长，底层 Deferred 会继续执行并通过 onTimeout 回调告知调用者。
 */
internal suspend fun <T> Deferred<T>.awaitWithBusinessTimeout(
    timeoutMillis: Long?,
    onTimeout: (Deferred<T>) -> Unit
): T? {
    val effectiveTimeout = timeoutMillis?.takeIf { it > 0 } ?: return await()
    val result = withTimeoutOrNull(effectiveTimeout) { await() }
    if (result == null) {
        onTimeout(this)
    }
    return result
}
