package com.daily.health.manager.steps

import org.junit.Assert.assertEquals
import org.junit.Test

class StepCounterAccumulatorTest {
    @Test
    fun firstReadingSetsBaselineWithoutImportingEarlierSteps() {
        assertEquals(StepCounterState(0, 1000, 1000, 4), update(null, 1000, 4))
    }

    @Test
    fun newReadingsAddOnlyTheirDeltaAndDuplicatesDoNotCountTwice() {
        val state = update(update(null, 1000, 4), 1003, 4)
        assertEquals(3, state.steps)
        assertEquals(state, update(state, 1003, 4))
    }

    @Test
    fun rebootPreservesEarlierStepsAndAddsNewBootSteps() {
        val before = update(update(null, 1000, 4), 1125, 4)
        val after = update(before, 12, 5)
        assertEquals(137, after.steps)
        assertEquals(140, update(after, 15, 5).steps)
    }

    @Test
    fun bootCountDetectsRebootEvenWhenNewRawValueIsLarger() {
        val before = update(update(null, 100, 4), 120, 4)
        assertEquals(170, update(before, 150, 5).steps)
    }

    @Test
    fun counterRollbackAlsoPreservesStepsWhenBootCountIsUnavailable() {
        val before = StepCounterState(125, 1000, 1125, -1)
        assertEquals(137, update(before, 12, -1).steps)
    }

    @Test
    fun legacyRecordsContinueAccumulatingAfterSchemaUpgrade() {
        val before = StepCounterState(125, 1000, 1125, -1)
        assertEquals(130, update(before, 1130, 4).steps)
        assertEquals(137, update(before, 12, 4).steps)
    }

    @Test
    fun multipleRebootsNeverEraseTheDailyTotal() {
        var state = update(update(null, 1000, 4), 1100, 4)
        state = update(state, 10, 5)
        state = update(state, 13, 5)
        state = update(state, 2, 6)
        assertEquals(115, state.steps)
    }

    @Test
    fun sumDoesNotOverflowIntoNegativeNumbers() {
        val before = StepCounterState(Int.MAX_VALUE - 1, 0, 1, 4)
        assertEquals(Int.MAX_VALUE, update(before, 5, 4).steps)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidNegativeCounterIsRejected() {
        update(null, -1, 4)
    }

    private fun update(previous: StepCounterState?, raw: Int, boot: Int) =
        StepCounterAccumulator.update(previous, raw, boot)
}
