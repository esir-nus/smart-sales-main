package com.smartsales.prism.domain.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactTimeCueResolverTest {

    private val nowIso = "2026-03-20T09:00:00Z"
    private val timezone = "Asia/Shanghai"

    @Test
    fun `resolveExactDayClockStartTime resolves tomorrow morning phrase`() {
        val result = ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = "明天早上8点",
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = null
        )

        assertNotNull(result)
        assertEquals("2026-03-21T08:00+08:00", result)
    }

    @Test
    fun `resolveExactDayClockStartTime resolves tomorrow afternoon half hour phrase`() {
        val result = ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = "明天下午三点半",
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = null
        )

        assertNotNull(result)
        assertEquals("2026-03-21T15:30+08:00", result)
    }

    @Test
    fun `resolveExactDayClockStartTime resolves qualified next week weekday phrase`() {
        val result = ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = "下周三早上8点",
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = null
        )

        assertNotNull(result)
        assertEquals("2026-03-25T08:00+08:00", result)
    }

    @Test
    fun `normalizeRelativeDayStartTime rewrites earlier llm date to lawful anchor`() {
        val result = ExactTimeCueResolver.normalizeRelativeDayStartTime(
            transcript = "明天早上8点开会",
            startTimeIso = "2026-03-20T08:00:00+08:00",
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = null
        )

        assertEquals("2026-03-21T08:00+08:00", result)
    }

    @Test
    fun `normalizeRelativeDayStartTime keeps later llm date when it stays within bound`() {
        val result = ExactTimeCueResolver.normalizeRelativeDayStartTime(
            transcript = "明天早上8点开会",
            startTimeIso = "2026-03-25T08:00:00+08:00",
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = null
        )

        assertEquals("2026-03-25T08:00:00+08:00", result)
    }

    @Test
    fun `rejectRelativeDayStartTime rejects llm date beyond one year from lawful anchor`() {
        val result = ExactTimeCueResolver.rejectRelativeDayStartTime(
            transcript = "明天早上8点开会",
            startTimeIso = "2027-04-26T08:00:00+08:00",
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = null
        )

        assertTrue(result == ExactTimeCueResolver.CueRejection.DateOutOfBounds)
    }
}
