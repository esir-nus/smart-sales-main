package com.smartsales.prism.ui.drawers.scheduler

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulerTaskDisplayTextTest {

    @Test
    fun simTimelineRailLabel_usesStartTimeOnlyForActiveTask() {
        val task = timelineTask(
            timeDisplay = "17:00 - ...",
            hour = 17,
            minute = 0
        )

        assertEquals("17:00", task.simTimelineRailLabel())
        assertEquals("17:00", task.simCollapsedTimeLabel())
    }

    @Test
    fun simDetailDateLabel_hidesEndTimePlaceholder() {
        val task = timelineTask(
            timeDisplay = "17:00 - ...",
            dateRange = "2026-03-26 17:00 - ...",
            hour = 17,
            minute = 0
        )

        assertEquals("2026-03-26 · 17:00", task.simDetailDateLabel())
    }

    @Test
    fun simLabels_keepVagueFallback() {
        val task = timelineTask(
            timeDisplay = "--:--",
            dateRange = "2026-03-26 · 时间待定",
            hour = 17,
            minute = 0,
            isVague = true
        )

        assertEquals("待定", task.simTimelineRailLabel())
        assertEquals("2026-03-26 · 时间待定", task.simDetailDateLabel())
    }

    @Test
    fun simTimelineRailLabel_preservesCompletedLabel() {
        val task = timelineTask(
            timeDisplay = "已完成",
            hour = 17,
            minute = 0,
            isDone = true
        )

        assertEquals("已完成", task.simTimelineRailLabel())
    }

    private fun timelineTask(
        timeDisplay: String,
        hour: Int,
        minute: Int,
        dateRange: String = "",
        isDone: Boolean = false,
        isVague: Boolean = false
    ): TimelineItem.Task {
        val start = LocalDateTime.of(2026, 3, 26, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        return TimelineItem.Task(
            id = "task",
            timeDisplay = timeDisplay,
            title = "Test task",
            sortInstant = start,
            dateRange = dateRange,
            isDone = isDone,
            isVague = isVague
        )
    }
}
