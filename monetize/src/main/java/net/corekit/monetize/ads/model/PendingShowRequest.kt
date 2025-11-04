package net.corekit.monetize.ads.model

import kotlinx.coroutines.CancellableContinuation
import net.corekit.monetize.ads.AdResult

data class PendingShowRequest<T>(
    val ad: T,
    val adUnitId: String,
    val continuation: CancellableContinuation<AdResult<Unit>>
)
