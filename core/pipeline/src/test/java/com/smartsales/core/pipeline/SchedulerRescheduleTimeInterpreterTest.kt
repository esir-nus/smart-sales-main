package com.smartsales.core.pipeline

import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.time.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SchedulerRescheduleTimeInterpreterTest {

    private val timeProvider = object : TimeProvider {
        override val now = Instant.parse("2026-03-22T08:00:00Z")
        override val zoneId: ZoneId = ZoneId.of("UTC")
        override val today = ZonedDateTime.ofInstant(now, zoneId).toLocalDate()
        override val currentTime: LocalTime = ZonedDateTime.ofInstant(now, zoneId).toLocalTime()

        override fun formatForLlm(): String = "2026-03-22 08:00 UTC"
    }

    @Test
    fun `resolveNaturalInstruction applies delta to exact task and preserves duration`() =
        kotlinx.coroutines.test.runTest {
            val result = SchedulerRescheduleTimeInterpreter.resolveNaturalInstruction(
                originalTask = scheduledTask(
                    id = "task-exact",
                    title = "赶高铁",
                    startTime = Instant.parse("2026-03-22T09:00:00Z"),
                    durationMinutes = 30,
                    isVague = false
                ),
                transcript = "推迟1个小时",
                displayedDateIso = "2026-03-22",
                timeProvider = timeProvider,
                uniAExtractionService = mock()
            )

            assertTrue(result is SchedulerRescheduleTimeInterpreter.Result.Success)
            result as SchedulerRescheduleTimeInterpreter.Result.Success
            assertEquals(Instant.parse("2026-03-22T10:00:00Z"), result.startTime)
            assertEquals(30, result.durationMinutes)
        }

    @Test
    fun `resolveNaturalInstruction rejects delta for vague task`() = kotlinx.coroutines.test.runTest {
        val result = SchedulerRescheduleTimeInterpreter.resolveNaturalInstruction(
            originalTask = scheduledTask(
                id = "task-vague",
                title = "提醒我开会",
                startTime = Instant.parse("2026-03-22T09:00:00Z"),
                durationMinutes = 0,
                isVague = true
            ),
            transcript = "推迟1个小时",
            displayedDateIso = "2026-03-22",
            timeProvider = timeProvider,
            uniAExtractionService = mock()
        )

        assertTrue(result is SchedulerRescheduleTimeInterpreter.Result.Unsupported)
    }

    private fun scheduledTask(
        id: String,
        title: String,
        startTime: Instant,
        durationMinutes: Int,
        isVague: Boolean
    ): ScheduledTask {
        return ScheduledTask(
            id = id,
            timeDisplay = if (isVague) "待定" else "09:00",
            title = title,
            urgencyLevel = UrgencyLevel.L2_IMPORTANT,
            startTime = startTime,
            durationMinutes = durationMinutes,
            isVague = isVague
        )
    }
}
