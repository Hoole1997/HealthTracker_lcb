package com.daily.health.manager.steps

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.daily.health.manager.feature.NotificationFeatureSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StepTrackingManagerTest {
    @Test
    fun collectsInForegroundWhileAllNotificationFeaturesAreDisabled() {
        assertFalse(NotificationFeatureSwitch.notificationsEnabled)
        assertFalse(NotificationFeatureSwitch.foregroundServiceEnabled)
        val owner = TestOwner()
        val source = FakeSource()
        val saved = mutableListOf<Int>()
        val manager = StepTrackingManager(source, { saved.add(it.rawSteps) }, Dispatchers.Unconfined)
        try {
            manager.initialize(owner.lifecycle)
            assertEquals(0, source.registrations)
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            source.emit(100)
            source.emit(102)
            assertEquals(listOf(100, 102), saved)
            assertEquals(StepTrackingStatus.ACTIVE, manager.status.value)
        } finally {
            manager.close()
        }
    }

    @Test
    fun repeatedRefreshDoesNotDuplicateRegistrationAndBackgroundStopsCollection() {
        val owner = TestOwner()
        val source = FakeSource()
        val manager = StepTrackingManager(source, {}, Dispatchers.Unconfined)
        try {
            manager.initialize(owner.lifecycle)
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            manager.refresh()
            manager.refresh()
            assertEquals(1, source.registrations)
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            assertFalse(source.active)
            assertEquals(StepTrackingStatus.STOPPED, manager.status.value)
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            assertEquals(2, source.registrations)
        } finally {
            manager.close()
        }
    }

    @Test
    fun grantingPermissionWhileAlreadyForegroundStartsCollection() {
        val owner = TestOwner()
        val source = FakeSource(StepTrackingStatus.PERMISSION_REQUIRED)
        val manager = StepTrackingManager(source, {}, Dispatchers.Unconfined)
        try {
            manager.initialize(owner.lifecycle)
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            assertEquals(StepTrackingStatus.PERMISSION_REQUIRED, manager.status.value)
            source.availability = StepTrackingStatus.ACTIVE
            manager.refresh()
            assertEquals(StepTrackingStatus.ACTIVE, manager.status.value)
            assertEquals(1, source.registrations)
        } finally {
            manager.close()
        }
    }

    @Test
    fun unsupportedSensorIsReportedInsteadOfSilentlyShowingZero() {
        val owner = TestOwner()
        val source = FakeSource(StepTrackingStatus.SENSOR_UNAVAILABLE)
        val manager = StepTrackingManager(source, {}, Dispatchers.Unconfined)
        try {
            manager.initialize(owner.lifecycle)
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            assertEquals(StepTrackingStatus.SENSOR_UNAVAILABLE, manager.status.value)
            assertEquals(0, source.registrations)
        } finally {
            manager.close()
        }
    }

    @Test
    fun queuedReadingsRemainOrderedAndDrainAfterEnteringBackground() {
        val owner = TestOwner()
        val source = FakeSource()
        val gate = Channel<Unit>(1)
        val saved = mutableListOf<Int>()
        val manager = StepTrackingManager(source, {
            if (it.rawSteps == 100) gate.receive()
            saved.add(it.rawSteps)
        }, Dispatchers.Unconfined)
        try {
            manager.initialize(owner.lifecycle)
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            source.emit(100)
            source.emit(101)
            source.emit(103)
            assertEquals(emptyList<Int>(), saved)
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            gate.trySend(Unit)
            assertEquals(listOf(100, 101, 103), saved)
        } finally {
            manager.close()
            gate.close()
        }
    }

    private class TestOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.CREATED
        }
    }

    private class FakeSource(var availability: StepTrackingStatus = StepTrackingStatus.ACTIVE) : StepCounterSource {
        private var callback: ((StepCounterReading) -> Unit)? = null
        val active get() = callback != null
        var registrations = 0
            private set

        override fun start(onReading: (StepCounterReading) -> Unit): StepTrackingStatus {
            if (availability != StepTrackingStatus.ACTIVE) {
                callback = null
                return availability
            }
            if (callback == null) {
                registrations++
                callback = onReading
            }
            return StepTrackingStatus.ACTIVE
        }

        override fun stop() {
            callback = null
        }

        fun emit(raw: Int) {
            callback?.invoke(StepCounterReading(raw, 4, 1000L))
        }
    }
}
