package com.smartsales.prism.domain.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class RelativeTimeResolverRegressionTest {

    private val nowMillis = 1770814800000L
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `explicit later variants resolve as exact relative times`() {
        assertEquals("2026-02-11 21:05", RelativeTimeResolver.resolve("五分钟之后开会", nowMillis, zoneId))
        assertEquals("2026-02-12 05:00", RelativeTimeResolver.resolve("八个小时以后赶高铁", nowMillis, zoneId))
        assertEquals("2026-02-12 00:00", RelativeTimeResolver.resolve("三小时之后赶飞机", nowMillis, zoneId))
    }

    @Test
    fun `normalize transcript canonicalizes explicit later suffixes`() {
        val normalized = RelativeTimeResolver.normalizeExplicitRelativeTimeTranscript("八个小时以后赶高铁，三小时之后赶飞机")
        assertEquals("八个小时后赶高铁，三小时后赶飞机", normalized)
    }

    @Test
    fun `resolveExact returns ISO-8601 start time`() {
        val resolution = RelativeTimeResolver.resolveExact("八个小时以后赶高铁", nowMillis, zoneId)
        assertNotNull(resolution)
        assertEquals("2026-02-12T05:00+08:00", resolution!!.startTimeIso)
        assertEquals(480L, resolution.offsetMinutes)
    }

    @Test
    fun `explicit reschedule delta phrases resolve as signed offsets`() {
        assertEquals(60L, RelativeTimeResolver.resolveSignedDeltaMinutes("赶高铁时间推迟1个小时")?.offsetMinutes)
        assertEquals(-15L, RelativeTimeResolver.resolveSignedDeltaMinutes("往前提一刻钟")?.offsetMinutes)
    }

    @Test
    fun `fuzzy language remains unsupported`() {
        assertNull(RelativeTimeResolver.resolve("待会儿开会", nowMillis, zoneId))
        assertNull(RelativeTimeResolver.resolve("以后想学吉他", nowMillis, zoneId))
    }
}
