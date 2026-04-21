package com.smartsales.prism.ui.onboarding

import com.smartsales.core.pipeline.TaskCreationBadgeSignal
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.memory.ScheduleItem
import com.smartsales.prism.domain.memory.TargetResolution
import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingSchedulerQuickStartCommitterTest {

    @Test
    fun `commit writes exact and vague staged quick start items through path a engine`() = runTest {
        val repository = FakeScheduledTaskRepository()
        val scheduleBoard = FakeScheduleBoard()
        val inspirationRepository = FakeInspirationRepository()
        val timeProvider = FakeTimeProvider()
        val badgeSignal = FakeTaskCreationBadgeSignal()
        val mutationEngine = FastTrackMutationEngine(
            taskRepository = repository,
            scheduleBoard = scheduleBoard,
            inspirationRepository = inspirationRepository,
            timeProvider = timeProvider
        )
        val committer = RealOnboardingSchedulerQuickStartCommitter(
            mutationEngine = mutationEngine,
            taskRepository = repository,
            timeProvider = timeProvider,
            taskCreationBadgeSignal = badgeSignal
        )

        committer.stage(
            listOf(
                OnboardingQuickStartItem(
                    stableId = "item-1",
                    title = "起床闹钟",
                    timeLabel = "07:00",
                    dateLabel = "明天",
                    dateIso = "2026-04-04",
                    urgencyLevel = UrgencyLevel.L1_CRITICAL,
                    startHour = 7,
                    startMinute = 0
                ),
                OnboardingQuickStartItem(
                    stableId = "item-2",
                    title = "推迟后的飞机",
                    timeLabel = "待定",
                    dateLabel = "大后天",
                    dateIso = "2026-04-06",
                    urgencyLevel = UrgencyLevel.L2_IMPORTANT
                )
            )
        )

        val result = committer.commitIfNeeded()

        assertEquals(
            OnboardingSchedulerQuickStartCommitResult.Success(listOf("item-1", "item-2")),
            result
        )
        assertEquals(2, repository.tasks.size)
        assertTrue(repository.tasks.any { it.id == "item-1" && !it.isVague })
        assertTrue(repository.tasks.any { it.id == "item-2" && it.isVague })
        assertEquals(1, badgeSignal.calls)
    }

    @Test
    fun `commit rolls back already written tasks when later mutation fails`() = runTest {
        val repository = FailingScheduledTaskRepository(failingTaskId = "item-2")
        val scheduleBoard = FakeScheduleBoard()
        val inspirationRepository = FakeInspirationRepository()
        val timeProvider = FakeTimeProvider()
        val badgeSignal = FakeTaskCreationBadgeSignal()
        val mutationEngine = FastTrackMutationEngine(
            taskRepository = repository,
            scheduleBoard = scheduleBoard,
            inspirationRepository = inspirationRepository,
            timeProvider = timeProvider
        )
        val committer = RealOnboardingSchedulerQuickStartCommitter(
            mutationEngine = mutationEngine,
            taskRepository = repository,
            timeProvider = timeProvider,
            taskCreationBadgeSignal = badgeSignal
        )

        committer.stage(
            listOf(
                OnboardingQuickStartItem(
                    stableId = "item-1",
                    title = "起床闹钟",
                    timeLabel = "07:00",
                    dateLabel = "明天",
                    dateIso = "2026-04-04",
                    urgencyLevel = UrgencyLevel.L1_CRITICAL,
                    startHour = 7,
                    startMinute = 0
                ),
                OnboardingQuickStartItem(
                    stableId = "item-2",
                    title = "推迟后的飞机",
                    timeLabel = "待定",
                    dateLabel = "大后天",
                    dateIso = "2026-04-06",
                    urgencyLevel = UrgencyLevel.L2_IMPORTANT
                )
            )
        )

        val result = committer.commitIfNeeded()

        assertEquals(
            OnboardingSchedulerQuickStartCommitResult.Failure("体验日程同步失败，请稍后重试。"),
            result
        )
        assertTrue(repository.tasks.isEmpty())
        assertEquals(0, badgeSignal.calls)
    }

    private open class FakeScheduledTaskRepository : ScheduledTaskRepository {
        val tasks = mutableListOf<ScheduledTask>()

        override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = flowOf(emptyList())

        override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<SchedulerTimelineItem>> =
            flowOf(emptyList())

        override suspend fun insertTask(task: ScheduledTask): String {
            tasks.removeAll { it.id == task.id }
            tasks += task
            return task.id
        }

        override suspend fun getTask(id: String): ScheduledTask? = tasks.firstOrNull { it.id == id }

        override suspend fun updateTask(task: ScheduledTask) {
            tasks.removeAll { it.id == task.id }
            tasks += task
        }

        override suspend fun upsertTask(task: ScheduledTask): String {
            tasks.removeAll { it.id == task.id }
            tasks += task
            return task.id
        }

        override suspend fun batchInsertTasks(tasks: List<ScheduledTask>): List<String> {
            tasks.forEach { upsertTask(it) }
            return tasks.map { it.id }
        }

        override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) {
            tasks.removeAll { it.id == oldTaskId }
            tasks += newTask
        }

        override suspend fun deleteItem(id: String) {
            tasks.removeAll { it.id == id }
        }

        override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()

        override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null

        override fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>> = flowOf(emptyList())
    }

    private class FailingScheduledTaskRepository(
        private val failingTaskId: String
    ) : FakeScheduledTaskRepository() {
        override suspend fun upsertTask(task: ScheduledTask): String {
            if (task.id == failingTaskId) {
                throw IllegalStateException("boom")
            }
            return super.upsertTask(task)
        }
    }

    private class FakeScheduleBoard : ScheduleBoard {
        private val state = MutableStateFlow<List<ScheduleItem>>(emptyList())
        override val upcomingItems: StateFlow<List<ScheduleItem>> = state.asStateFlow()

        override suspend fun checkConflict(
            proposedStart: Long,
            durationMinutes: Int,
            excludeId: String?
        ): ConflictResult = ConflictResult.Clear

        override suspend fun refresh() = Unit

        override suspend fun resolveTarget(request: TargetResolutionRequest): TargetResolution {
            return TargetResolution.NoMatch(request.describeForFailure())
        }
    }

    private class FakeInspirationRepository : InspirationRepository {
        override suspend fun insert(text: String): String = "inspiration"
        override fun getAll(): Flow<List<SchedulerTimelineItem.Inspiration>> = flowOf(emptyList())
        override suspend fun delete(id: String) = Unit
    }

    private class FakeTimeProvider : TimeProvider {
        override val now: Instant = Instant.parse("2026-04-03T08:00:00Z")
        override val today: LocalDate = LocalDate.parse("2026-04-03")
        override val currentTime: LocalTime = LocalTime.of(16, 0)
        override val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

        override fun formatForLlm(): String = "2026年4月3日（周五）16:00"
    }

    private class FakeTaskCreationBadgeSignal : TaskCreationBadgeSignal {
        var calls = 0

        override suspend fun onTasksCreated() {
            calls++
        }
    }
}
