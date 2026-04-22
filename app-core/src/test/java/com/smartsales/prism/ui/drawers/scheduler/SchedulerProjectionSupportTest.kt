package com.smartsales.prism.ui.drawers.scheduler

import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.ui.scheduler.mapper.toUiState
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulerProjectionSupportTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `crossed off mapper emits done task ui state without synthetic scheduled task`() {
        val crossedOff = SchedulerTimelineItem.CrossedOff(
            id = "done-1",
            timeDisplay = "已完成",
            title = "Finished follow-up",
            startTime = Instant.parse("2026-04-21T02:00:00Z"),
            urgencyLevel = UrgencyLevel.L2_IMPORTANT,
            reminderCascade = listOf("-30m", "0m")
        )

        val uiState = crossedOff.toUiState()

        assertTrue(uiState is TimelineItem.Task)
        uiState as TimelineItem.Task
        assertTrue(uiState.isDone)
        assertEquals("Finished follow-up", uiState.title)
        assertEquals(listOf("-30m", "0m"), uiState.alarmCascade)
    }
}
