package com.smartsales.prism.data.persistence

import com.smartsales.prism.domain.scheduler.ScheduledTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DaoMappersTest {

    @Test
    fun `scheduled task mapper preserves vague and conflict state`() {
        val task = ScheduledTask(
            id = "uni-b-001",
            timeDisplay = "待定",
            title = "提醒我开会",
            startTime = Instant.parse("2026-03-21T00:00:00Z"),
            notes = "时间待定（线索：下午）",
            isVague = true,
            hasConflict = false
        )

        val entity = task.toEntity()
        assertTrue(entity.isVague)
        assertFalse(entity.hasConflict)

        val domain = entity.toDomain()
        assertTrue(domain.isVague)
        assertFalse(domain.hasConflict)
        assertEquals("待定", domain.timeDisplay)
        assertEquals("2026-03-21 · 时间待定", domain.dateRange)
    }
}
