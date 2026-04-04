package com.smartsales.prism.ui.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmActivityStateTest {

    @Test
    fun `upsertAlarmStack inserts newest item at top`() {
        val existing = listOf(
            AlarmItem("task-1", "notif-1", "旧提醒", "15:00")
        )

        val updated = upsertAlarmStack(
            existing = existing,
            incoming = AlarmItem("task-2", "notif-2", "新提醒", "16:00")
        )

        assertEquals(listOf("task-2", "task-1"), updated.map { it.taskId })
    }

    @Test
    fun `upsertAlarmStack refreshes duplicate task without keeping stale copy`() {
        val existing = listOf(
            AlarmItem("task-1", "notif-old", "旧标题", "15:00"),
            AlarmItem("task-2", "notif-2", "第二条", "16:00")
        )

        val updated = upsertAlarmStack(
            existing = existing,
            incoming = AlarmItem("task-1", "notif-new", "新标题", "15:05")
        )

        assertEquals(listOf("task-1", "task-2"), updated.map { it.taskId })
        assertEquals("notif-new", updated.first().notificationId)
        assertEquals("新标题", updated.first().title)
    }
}
