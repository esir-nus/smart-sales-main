package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SimReminderBannerSupportTest {

    @Test
    fun `mergeSimReminderBannerEntries promotes 5 minute reminder ahead of 15 minute reminder`() {
        val existing = listOf(
            SimReminderBannerEntry(
                taskId = "task-15",
                title = "十五分钟提醒",
                supportingText = "",
                offsetMinutes = 15,
                target = null,
                emittedAtMillis = 10L
            )
        )

        val merged = mergeSimReminderBannerEntries(
            existing = existing,
            incoming = SimReminderBannerEntry(
                taskId = "task-5",
                title = "五分钟提醒",
                supportingText = "",
                offsetMinutes = 5,
                target = null,
                emittedAtMillis = 20L
            )
        )

        assertEquals(listOf("task-5", "task-15"), merged.map { it.taskId })
    }

    @Test
    fun `mergeSimReminderBannerEntries de duplicates task id`() {
        val existing = listOf(
            SimReminderBannerEntry(
                taskId = "task-1",
                title = "旧标题",
                supportingText = "",
                offsetMinutes = 15,
                target = null,
                emittedAtMillis = 10L
            )
        )

        val merged = mergeSimReminderBannerEntries(
            existing = existing,
            incoming = SimReminderBannerEntry(
                taskId = "task-1",
                title = "新标题",
                supportingText = "",
                offsetMinutes = 5,
                target = null,
                emittedAtMillis = 20L
            )
        )

        assertEquals(1, merged.size)
        assertEquals("新标题", merged.first().title)
        assertEquals(5, merged.first().offsetMinutes)
    }

    @Test
    fun `buildSimReminderBannerEntry uses scheduler target when task exists`() {
        val task = ScheduledTask(
            id = "task-1",
            timeDisplay = "21:00",
            title = "重要的采购大会",
            urgencyLevel = UrgencyLevel.L2_IMPORTANT,
            hasAlarm = true,
            startTime = Instant.parse("2026-04-02T13:00:00Z"),
            keyPerson = "华为"
        )

        val entry = buildSimReminderBannerEntry(
            task = task,
            taskId = task.id,
            title = task.title,
            offsetMinutes = 15,
            emittedAtMillis = 123L
        )

        assertEquals("重要的采购大会", entry.title)
        assertEquals("华为", entry.supportingText)
        assertNotNull(entry.target)
        assertEquals("task-1", entry.target?.taskId)
    }

    @Test
    fun `SimReminderBannerState builds aggregate warning copy`() {
        val state = SimReminderBannerState(
            listOf(
                SimReminderBannerEntry("task-1", "任务A", "华为", 5, null, 20L),
                SimReminderBannerEntry("task-2", "任务B", "", 15, null, 10L)
            )
        )

        assertEquals(SimReminderBannerAccent.WARNING, state.accentKind)
        assertEquals("最近有 2 个提醒即将开始", state.headline)
        assertEquals("任务A · 华为  +1", state.description)
    }
}
