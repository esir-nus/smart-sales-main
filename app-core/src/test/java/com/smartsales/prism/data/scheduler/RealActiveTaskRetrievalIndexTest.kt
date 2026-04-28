package com.smartsales.prism.data.scheduler

import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealActiveTaskRetrievalIndexTest {

    private val taskRepository = FakeScheduledTaskRepository()
    private val index = RealActiveTaskRetrievalIndex(taskRepository)

    @Test
    fun `buildShortlist includes overdue active task for global follow up`() = runTest {
        taskRepository.insertTask(
            task(
                id = "overdue-task",
                title = "和张总吃饭",
                startTime = Instant.parse("2026-03-01T10:00:00Z")
            )
        )

        val shortlist = index.buildShortlist("张总吃饭改到明天早上8点")

        assertEquals(listOf("overdue-task"), shortlist.map { it.taskId })
    }

    @Test
    fun `resolveTarget notes stay weak and exact title wins`() = runTest {
        taskRepository.insertTask(
            task(
                id = "title-match",
                title = "预算复盘",
                startTime = Instant.parse("2026-03-20T10:00:00Z")
            )
        )
        taskRepository.insertTask(
            task(
                id = "notes-only",
                title = "客户回访",
                notes = "预算复盘材料要带上",
                startTime = Instant.parse("2026-03-20T11:00:00Z")
            )
        )

        val result = index.resolveTarget(
            target = TargetResolutionRequest(targetQuery = "预算复盘")
        )

        assertEquals(ActiveTaskResolveResult.Resolved("title-match"), result)
    }

    @Test
    fun `resolveTarget safe fails when suggested task is not corroborated`() = runTest {
        taskRepository.insertTask(
            task(
                id = "right-task",
                title = "赶高铁",
                startTime = Instant.parse("2026-03-20T09:00:00Z")
            )
        )
        taskRepository.insertTask(
            task(
                id = "wrong-task",
                title = "客户回访",
                startTime = Instant.parse("2026-03-20T11:00:00Z")
            )
        )

        val result = index.resolveTarget(
            target = TargetResolutionRequest(targetQuery = "赶高铁"),
            suggestedTaskId = "wrong-task"
        )

        assertEquals(ActiveTaskResolveResult.NoMatch("赶高铁"), result)
    }

    @Test
    fun `resolveTarget returns ambiguous for near tie candidates`() = runTest {
        taskRepository.insertTask(
            task(
                id = "task-a",
                title = "客户复盘 A",
                startTime = Instant.parse("2026-03-20T09:00:00Z")
            )
        )
        taskRepository.insertTask(
            task(
                id = "task-b",
                title = "客户复盘 B",
                startTime = Instant.parse("2026-03-20T11:00:00Z")
            )
        )

        val result = index.resolveTarget(
            target = TargetResolutionRequest(targetQuery = "客户复盘")
        )

        assertTrue(result is ActiveTaskResolveResult.Ambiguous)
        result as ActiveTaskResolveResult.Ambiguous
        assertEquals(listOf("task-a", "task-b"), result.candidateIds.take(2))
    }

    @Test
    fun `resolveTargetByClockAnchor resolves one exact minute match on displayed date`() = runTest {
        taskRepository.insertTask(task("task-9", "起床", Instant.parse("2026-03-20T01:00:00Z")))

        val result = index.resolveTargetByClockAnchor(
            clockCue = "9点",
            nowIso = "2026-03-19T12:00:00Z",
            timezone = "Asia/Shanghai",
            displayedDateIso = "2026-03-20"
        )

        assertEquals(ActiveTaskResolveResult.Resolved("task-9"), result)
    }

    @Test
    fun `resolveTargetByClockAnchor returns no match for missing clock slot`() = runTest {
        taskRepository.insertTask(task("task-10", "起床", Instant.parse("2026-03-20T02:00:00Z")))

        val result = index.resolveTargetByClockAnchor(
            clockCue = "9点",
            nowIso = "2026-03-20T00:00:00Z",
            timezone = "Asia/Shanghai"
        )

        assertEquals(ActiveTaskResolveResult.NoMatch("9点"), result)
    }

    @Test
    fun `resolveTargetByClockAnchor returns ambiguous for duplicate clock slot`() = runTest {
        taskRepository.insertTask(task("task-a", "起床", Instant.parse("2026-03-20T01:00:00Z")))
        taskRepository.insertTask(task("task-b", "赶车", Instant.parse("2026-03-20T01:00:00Z")))

        val result = index.resolveTargetByClockAnchor(
            clockCue = "9点",
            nowIso = "2026-03-20T00:00:00Z",
            timezone = "Asia/Shanghai"
        )

        assertTrue(result is ActiveTaskResolveResult.Ambiguous)
        result as ActiveTaskResolveResult.Ambiguous
        assertEquals(listOf("task-a", "task-b"), result.candidateIds)
    }

    @Test
    fun `resolveTargetByClockAnchor plain clock uses today in timezone`() = runTest {
        taskRepository.insertTask(task("today-9", "今天任务", Instant.parse("2026-03-20T01:00:00Z")))
        taskRepository.insertTask(task("tomorrow-9", "明天任务", Instant.parse("2026-03-21T01:00:00Z")))

        val result = index.resolveTargetByClockAnchor(
            clockCue = "9点",
            nowIso = "2026-03-20T00:30:00Z",
            timezone = "Asia/Shanghai"
        )

        assertEquals(ActiveTaskResolveResult.Resolved("today-9"), result)
    }

    @Test
    fun `resolveTargetByClockAnchor date-qualified cue uses qualified date`() = runTest {
        taskRepository.insertTask(task("today-9", "今天任务", Instant.parse("2026-03-20T01:00:00Z")))
        taskRepository.insertTask(task("tomorrow-9", "明天任务", Instant.parse("2026-03-21T01:00:00Z")))

        val result = index.resolveTargetByClockAnchor(
            clockCue = "明天9点",
            nowIso = "2026-03-20T00:30:00Z",
            timezone = "Asia/Shanghai"
        )

        assertEquals(ActiveTaskResolveResult.Resolved("tomorrow-9"), result)
    }

    @Test
    fun `resolveTargetByClockAnchor compares exact minute`() = runTest {
        taskRepository.insertTask(task("nine-thirty", "九点半任务", Instant.parse("2026-03-20T01:30:00Z")))

        val result = index.resolveTargetByClockAnchor(
            clockCue = "9点",
            nowIso = "2026-03-20T00:30:00Z",
            timezone = "Asia/Shanghai"
        )

        assertEquals(ActiveTaskResolveResult.NoMatch("9点"), result)
    }

    private fun task(
        id: String,
        title: String,
        startTime: Instant,
        notes: String? = null
    ): ScheduledTask {
        return ScheduledTask(
            id = id,
            timeDisplay = startTime.toString(),
            title = title,
            urgencyLevel = UrgencyLevel.L2_IMPORTANT,
            startTime = startTime,
            notes = notes
        )
    }
}
