package com.daily.health.manager.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.corekit.monetize.ads.AdPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessAdRequestTest {
    @Test
    fun everyDisabledSlotSkipsSdkAndReturnsImmediately() = runBlocking {
        val positions = AdPosition::class.java.declaredFields
            .filter { it.type == String::class.java }
            .map { it.get(null) as String }
        assertTrue(positions.isNotEmpty())
        positions.forEach { position ->
            val shown = requestBusinessAd(enabled = { false }) { error("Disabled SDK request: $position") }
            assertFalse(shown)
        }
    }

    @Test
    fun rewardRequestReportsDisabledSuccessAndFailureToCaller() = runBlocking {
        assertTrue(requestBusinessAd(enabled = { false }, whenDisabled = true) { error("Must not load") })
        assertTrue(requestBusinessAd(enabled = { true }, whenDisabled = true) { true })
        assertFalse(requestBusinessAd(enabled = { true }, whenDisabled = true) { false })
        assertFalse(requestBusinessAd(enabled = { true }, whenDisabled = true) { throw IllegalStateException() })
    }

    @Test
    fun configErrorsAreDisabledAndDoNotAffectOtherRequests() = runBlocking {
        assertFalse(requestBusinessAd(enabled = { error("Unavailable config") }) { error("Must not load") })
        assertTrue(requestBusinessAd(enabled = { error("Unavailable config") }, whenDisabled = true) { error("Must not load") })
        assertTrue(requestBusinessAd(enabled = { true }) { true })
    }

    @Test
    fun nextRequestUsesLatestSwitchValue() = runBlocking {
        var enabled = true
        var requests = 0
        suspend fun request() = requestBusinessAd(enabled = { enabled }) { requests++; true }
        assertTrue(request())
        enabled = false
        assertFalse(request())
        enabled = true
        assertTrue(request())
        assertEquals(2, requests)
    }

    @Test
    fun closingSlotWhileLoadingRejectsLateNativeResult() = runBlocking {
        var enabled = true
        val started = CompletableDeferred<Unit>()
        val loaded = CompletableDeferred<Boolean>()
        val request = async {
            requestBusinessAd(enabled = { enabled }) {
                started.complete(Unit)
                loaded.await()
            }
        }
        started.await()
        enabled = false
        loaded.complete(true)
        assertFalse(request.await())
    }

    @Test
    fun closingRewardWhileLoadingStillAllowsNextStep() = runBlocking {
        var enabled = true
        val canUnlock = requestBusinessAd(enabled = { enabled }, whenDisabled = true) {
            enabled = false
            throw IllegalStateException("SDK failure after disabled")
        }
        assertTrue(canUnlock)
    }

    @Test
    fun sdkTimeoutReturnsFailureAndContinuesOnceInsteadOfLosingCallback() = runBlocking {
        var continuations = 0
        val shown = requestBusinessAd(enabled = { true }) {
            withTimeout(1) { awaitCancellation() }
        }
        continuations++
        assertFalse(shown)
        assertEquals(1, continuations)
    }

    @Test
    fun sdkCancellationDoesNotCancelAnActivePage() = runBlocking {
        assertFalse(requestBusinessAd(enabled = { true }) { throw CancellationException("SDK cancelled load") })
        assertTrue(requestBusinessAd(enabled = { true }) { true })
    }

    @Test
    fun destroyedPageCleansUpButNeverNavigatesAfterCancellation() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var cleanedUp = false
        var continued = false
        val job = launch {
            requestBusinessAd(enabled = { true }) {
                try {
                    started.complete(Unit)
                    awaitCancellation()
                } finally {
                    cleanedUp = true
                }
            }
            continued = true
        }
        started.await()
        job.cancelAndJoin()
        assertTrue(cleanedUp)
        assertFalse(continued)
    }
}
