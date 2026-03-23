package com.smartsales.prism.domain.memory

import com.smartsales.prism.domain.scheduler.UrgencyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleItemConflictTest {

    @Test
    fun `transport exact task without explicit duration uses two hour occupancy`() {
        assertEquals(
            120,
            effectiveConflictOccupancyMinutes(
                title = "提醒我去赶高铁",
                urgencyLevel = UrgencyLevel.L1_CRITICAL,
                explicitDurationMinutes = 0
            )
        )
    }

    @Test
    fun `fire off task always bypasses occupancy`() {
        assertEquals(
            0,
            effectiveConflictOccupancyMinutes(
                title = "赶飞机",
                urgencyLevel = UrgencyLevel.FIRE_OFF,
                explicitDurationMinutes = 45
            )
        )
    }

    @Test
    fun `explicit duration overrides semantic fallback`() {
        assertEquals(
            15,
            effectiveConflictOccupancyMinutes(
                title = "赶飞机",
                urgencyLevel = UrgencyLevel.L1_CRITICAL,
                explicitDurationMinutes = 15
            )
        )
    }

    @Test
    fun `existing zero duration transport task blocks overlapping exact task`() {
        val existing = ScheduleItem(
            entryId = "train",
            title = "提醒我去赶高铁",
            scheduledAt = 12 * 60_000L,
            durationMinutes = 0,
            durationSource = DurationSource.DEFAULT,
            urgencyLevel = UrgencyLevel.L1_CRITICAL,
            conflictPolicy = ConflictPolicy.EXCLUSIVE
        )

        assertTrue(
            overlapsInScheduleBoard(
                proposedStart = 15 * 60_000L,
                proposedDurationMinutes = 30,
                existingStart = existing.scheduledAt,
                existingDurationMinutes = existing.effectiveConflictDurationMinutes
            )
        )
    }

    @Test
    fun `existing fire off task does not block overlapping exact task`() {
        val existing = ScheduleItem(
            entryId = "water",
            title = "提醒我喝水",
            scheduledAt = 12 * 60_000L,
            durationMinutes = 0,
            durationSource = DurationSource.DEFAULT,
            urgencyLevel = UrgencyLevel.FIRE_OFF,
            conflictPolicy = ConflictPolicy.EXCLUSIVE
        )

        assertFalse(
            overlapsInScheduleBoard(
                proposedStart = 15 * 60_000L,
                proposedDurationMinutes = 30,
                existingStart = existing.scheduledAt,
                existingDurationMinutes = existing.effectiveConflictDurationMinutes
            )
        )
    }
}
