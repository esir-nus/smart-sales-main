package com.smartsales.prism.ui.components

import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DynamicIslandStateMapperTest {

    @Test
    fun `fromScheduler uses pending task summary when one task exists`() {
        val state = DynamicIslandStateMapper.fromScheduler(
            sessionTitle = "客户A",
            upcoming = listOf(task(id = "t1", title = "跟进报价", timeDisplay = "15:00"))
        )

        val visible = state as DynamicIslandUiState.Visible
        assertEquals("客户A", visible.item.sessionTitle)
        assertEquals("最近：跟进报价 · 15:00", visible.item.schedulerSummary)
        assertEquals("最近：跟进报价 · 15:00", visible.item.displayText)
    }

    @Test
    fun `fromScheduler falls back to no pending summary when list is empty`() {
        val state = DynamicIslandStateMapper.fromScheduler(
            sessionTitle = "",
            upcoming = emptyList()
        )

        val visible = state as DynamicIslandUiState.Visible
        assertEquals("", visible.item.sessionTitle)
        assertEquals("暂无待办", visible.item.schedulerSummary)
    }

    @Test
    fun `fromScheduler uses earliest pending task with upcoming prefix`() {
        val state = DynamicIslandStateMapper.fromScheduler(
            sessionTitle = "客户B",
            upcoming = listOf(
                task(id = "later", title = "发纪要", timeDisplay = "18:00", startTime = "2026-03-22T10:00:00Z"),
                task(id = "earlier", title = "回电", timeDisplay = "09:30", startTime = "2026-03-22T01:30:00Z"),
                task(id = "done", title = "已完成", timeDisplay = "08:00", startTime = "2026-03-22T00:00:00Z", isDone = true)
            )
        )

        val visible = state as DynamicIslandUiState.Visible
        assertEquals("最近：回电 · 09:30", visible.item.schedulerSummary)
        val tapAction = visible.item.tapAction as DynamicIslandTapAction.OpenSchedulerDrawer
        assertEquals("earlier", tapAction.target?.taskId)
        assertTrue(tapAction.target?.date != null)
    }

    @Test
    fun `fromScheduler prioritizes conflict item over earlier normal reminder`() {
        val state = DynamicIslandStateMapper.fromScheduler(
            sessionTitle = "客户C",
            upcoming = listOf(
                task(
                    id = "normal",
                    title = "普通回访",
                    timeDisplay = "09:00",
                    startTime = "2026-03-22T01:00:00Z"
                ),
                task(
                    id = "conflict",
                    title = "冲突会议",
                    timeDisplay = "15:00",
                    startTime = "2026-03-22T07:00:00Z",
                    hasConflict = true
                )
            )
        )

        val visible = state as DynamicIslandUiState.Visible
        assertEquals("冲突：冲突会议 · 15:00", visible.item.schedulerSummary)
        assertTrue(visible.item.isConflict)
        val tapAction = visible.item.tapAction as DynamicIslandTapAction.OpenSchedulerDrawer
        assertEquals("conflict", tapAction.target?.taskId)
        assertTrue(tapAction.target?.isConflict == true)
    }

    private fun task(
        id: String,
        title: String,
        timeDisplay: String,
        startTime: String = "2026-03-22T00:00:00Z",
        isDone: Boolean = false,
        hasConflict: Boolean = false
    ) = ScheduledTask(
        id = id,
        timeDisplay = timeDisplay,
        title = title,
        urgencyLevel = UrgencyLevel.L3_NORMAL,
        isDone = isDone,
        hasConflict = hasConflict,
        hasAlarm = false,
        isSmartAlarm = false,
        startTime = Instant.parse(startTime),
        endTime = null,
        durationMinutes = 0,
        durationSource = DurationSource.DEFAULT,
        conflictPolicy = ConflictPolicy.EXCLUSIVE
    )
}
