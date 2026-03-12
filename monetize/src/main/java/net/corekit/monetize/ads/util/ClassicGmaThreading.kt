package net.corekit.monetize.ads.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Classic GMA 24.x load/build APIs require main-thread execution.
 * Centralize that guarantee so preload callers can stay coroutine-friendly.
 */
suspend fun <T> runClassicGmaOnMain(block: suspend () -> T): T {
    return withContext(Dispatchers.Main.immediate) {
        block()
    }
}
