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
