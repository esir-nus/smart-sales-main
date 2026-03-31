package com.smartsales.prism.data.persistence

import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
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

    @Test
    fun `scheduled task mapper preserves Uni-D conflict evidence`() {
        val task = ScheduledTask(
            id = "uni-d-001",
            timeDisplay = "14:00",
            title = "开会",
            startTime = Instant.parse("2026-03-21T14:00:00Z"),
            hasConflict = true,
            conflictWithTaskId = "existing-1",
            conflictSummary = "与「牙医预约」时间冲突"
        )

        val entity = task.toEntity()
        assertTrue(entity.hasConflict)
        assertEquals("existing-1", entity.conflictWithTaskId)
        assertEquals("与「牙医预约」时间冲突", entity.conflictSummary)

        val domain = entity.toDomain()
        assertTrue(domain.hasConflict)
        assertEquals("existing-1", domain.conflictWithTaskId)
        assertEquals("与「牙医预约」时间冲突", domain.conflictSummary)
    }

    @Test
    fun `scheduled task mapper normalizes stale exact reminder metadata from urgency`() {
        val entity = ScheduledTaskEntity(
            taskId = "exact-reminder-001",
            title = "客户会议",
            startTimeMillis = Instant.parse("2026-03-21T07:00:00Z").toEpochMilli(),
            endTimeMillis = null,
            durationMinutes = 60,
            durationSource = "DEFAULT",
            conflictPolicy = "EXCLUSIVE",
            location = null,
            notes = null,
            keyPerson = null,
            keyPersonEntityId = null,
            highlights = null,
            isDone = false,
            hasAlarm = false,
            isSmartAlarm = false,
            alarmCascadeJson = """["-1h","-15m","-5m","0m"]""",
            urgencyLevel = "L2_IMPORTANT",
            hasConflict = false,
            conflictWithTaskId = null,
            conflictSummary = null,
            isVague = false
        )

        val domain = entity.toDomain()

        assertTrue(domain.hasAlarm)
        assertEquals(UrgencyLevel.L2_IMPORTANT, domain.urgencyLevel)
        assertEquals(listOf("-30m", "0m"), domain.alarmCascade)
    }

    @Test
    fun `scheduled task mapper keeps vague tasks reminder free`() {
        val task = ScheduledTask(
            id = "uni-b-002",
            timeDisplay = "待定",
            title = "提醒我回电话",
            startTime = Instant.parse("2026-03-21T00:00:00Z"),
            urgencyLevel = UrgencyLevel.L1_CRITICAL,
            isVague = true,
            hasAlarm = true,
            alarmCascade = listOf("-1h", "0m")
        )

        val domain = task.toEntity().toDomain()

        assertFalse(domain.hasAlarm)
        assertTrue(domain.alarmCascade.isEmpty())
    }
}
