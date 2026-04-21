package com.smartsales.prism.ui.sim

import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.TaskCreationBadgeSignal
import com.smartsales.core.pipeline.PromptCompiler
import com.smartsales.core.test.fakes.FakeAlarmScheduler
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.prism.data.notification.ExactAlarmPermissionGate
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.memory.ScheduleItem
import com.smartsales.prism.domain.memory.TargetResolution
import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.TaskDefinition
import com.smartsales.prism.domain.scheduler.UrgencyEnum
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimSchedulerMutationCoordinatorTest {

    @Test
    fun `handleMutation triggers badge signal once for successful exact create`() = runTest {
        val fixture = buildFixture()

        fixture.coordinator.handleMutation(
            FastTrackResult.CreateTasks(
                params = com.smartsales.prism.domain.scheduler.CreateTasksParams(
                    tasks = listOf(
                        TaskDefinition(
                            title = "开会",
                            startTimeIso = "2026-04-22T02:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        )
                    )
                )
            )
        )

        assertEquals(1, fixture.badgeSignal.calls)
        assertEquals("已创建日程", fixture.bridge.pipelineStatus)
        assertEquals(2, fixture.alarmScheduler.getAlarmsForTask(fixture.repository.tasks.single().id).size)
    }

    @Test
    fun `handleMutation does not trigger badge signal for reschedule success`() = runTest {
        val fixture = buildFixture()
        fixture.repository.upsertTask(
            ScheduledTask(
                id = "task-1",
                timeDisplay = "10:00",
                title = "原会议",
                urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-04-22T02:00:00Z"),
                durationMinutes = 60
            )
        )

        fixture.coordinator.handleMutation(
            FastTrackResult.RescheduleTask(
                params = com.smartsales.prism.domain.scheduler.RescheduleTaskParams(
                    resolvedTaskId = "task-1",
                    targetQuery = "原会议",
                    newStartTimeIso = "2026-04-22T03:00:00Z",
                    newDurationMinutes = 60
                )
            )
        )

        assertEquals(0, fixture.badgeSignal.calls)
        assertEquals(
            Instant.parse("2026-04-22T03:00:00Z"),
            fixture.repository.tasks.single { it.id == "task-1" }.startTime
        )
    }

    private fun buildFixture(): Fixture {
        val scope = TestScope(UnconfinedTestDispatcher())
        val timeProvider = object : TimeProvider {
            override val now: Instant = Instant.parse("2026-04-21T08:00:00Z")
            override val today: LocalDate = LocalDate.parse("2026-04-21")
            override val currentTime: LocalTime = LocalTime.of(16, 0)
            override val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")
            override fun formatForLlm(): String = "2026年4月21日 16:00"
        }
        val repository = FakeScheduledTaskRepository()
        val scheduleBoard = FakeScheduleBoard()
        val alarmScheduler = FakeAlarmScheduler()
        val badgeSignal = FakeTaskCreationBadgeSignal()
        val bridge = FakeBridge()
        val projectionSupport = SimSchedulerProjectionSupport(
            scope = scope,
            timeProvider = timeProvider,
            bridge = bridge.asUiBridge()
        )
        val reminderSupport = SimSchedulerReminderSupport(
            alarmScheduler = alarmScheduler,
            exactAlarmPermissionGate = object : ExactAlarmPermissionGate {
                override fun shouldPromptForExactAlarm(): Boolean = false
            },
            bridge = bridge.asUiBridge()
        )
        val coordinator = SimSchedulerMutationCoordinator(
            taskRepository = repository,
            scheduleBoard = scheduleBoard,
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = repository,
                scheduleBoard = scheduleBoard,
                inspirationRepository = FakeInspirationRepository(),
                timeProvider = timeProvider
            ),
            uniAExtractionService = RealUniAExtractionService(
                executor = FakeExecutor(),
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            timeProvider = timeProvider,
            projectionSupport = projectionSupport,
            reminderSupport = reminderSupport,
            taskCreationBadgeSignal = badgeSignal
        )
        return Fixture(
            coordinator = coordinator,
            repository = repository,
            alarmScheduler = alarmScheduler,
            badgeSignal = badgeSignal,
            bridge = bridge
        )
    }

    private data class Fixture(
        val coordinator: SimSchedulerMutationCoordinator,
        val repository: FakeScheduledTaskRepository,
        val alarmScheduler: FakeAlarmScheduler,
        val badgeSignal: FakeTaskCreationBadgeSignal,
        val bridge: FakeBridge
    )

    private class FakeTaskCreationBadgeSignal : TaskCreationBadgeSignal {
        var calls = 0

        override suspend fun onTasksCreated() {
            calls++
        }
    }

    private class FakeBridge {
        var unacknowledgedDates: Set<Int> = emptySet()
        var rescheduledDates: Set<Int> = emptySet()
        var conflictWarning: String? = null
        var conflictedTaskIds: Set<String> = emptySet()
        var causingTaskId: String? = null
        var exitingTasks: List<com.smartsales.prism.ui.drawers.scheduler.RescheduleExitMotion> = emptyList()
        var pipelineStatus: String? = null
        var pipelineStatusResetJob: Job? = null
        var exactAlarmPromptCount = 0

        fun asUiBridge(): SimSchedulerUiBridge {
            return SimSchedulerUiBridge(
                getActiveDayOffset = { 0 },
                getUnacknowledgedDates = { unacknowledgedDates },
                setUnacknowledgedDates = { unacknowledgedDates = it },
                getRescheduledDates = { rescheduledDates },
                setRescheduledDates = { rescheduledDates = it },
                setConflictWarning = { conflictWarning = it },
                setConflictedTaskIds = { conflictedTaskIds = it },
                setCausingTaskId = { causingTaskId = it },
                getExitingTasks = { exitingTasks },
                setExitingTasks = { exitingTasks = it },
                getPipelineStatus = { pipelineStatus },
                setPipelineStatus = { pipelineStatus = it },
                getPipelineStatusResetJob = { pipelineStatusResetJob },
                setPipelineStatusResetJob = { pipelineStatusResetJob = it },
                emitExactAlarmPermissionNeeded = { exactAlarmPromptCount++ }
            )
        }
    }

    private class FakeScheduledTaskRepository : ScheduledTaskRepository {
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
            tasks += newTask.copy(id = oldTaskId)
        }

        override suspend fun deleteItem(id: String) {
            tasks.removeAll { it.id == id }
        }

        override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()

        override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null

        override fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>> = flowOf(emptyList())
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

        override suspend fun findLexicalMatch(targetQuery: String): ScheduleItem? = null

        override suspend fun resolveTarget(request: TargetResolutionRequest): TargetResolution {
            return TargetResolution.NoMatch(request.describeForFailure())
        }
    }

    private class FakeInspirationRepository : InspirationRepository {
        override suspend fun insert(text: String): String = "inspiration"
        override fun getAll(): Flow<List<SchedulerTimelineItem.Inspiration>> = flowOf(emptyList())
        override suspend fun delete(id: String) = Unit
    }
}
