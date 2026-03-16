package com.smartsales.prism.domain.scheduler

import com.smartsales.core.pipeline.UnifiedPipeline
import com.smartsales.core.test.fakes.FakeMemoryRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.ScheduleBoard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class SchedulerCoordinatorTest {

    private lateinit var coordinator: SchedulerCoordinator
    private lateinit var taskRepository: FakeScheduledTaskRepository
    private lateinit var memoryRepository: FakeMemoryRepository
    private lateinit var scheduleBoard: ScheduleBoard
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var unifiedPipeline: UnifiedPipeline

    @Before
    fun setup() {
        taskRepository = FakeScheduledTaskRepository()
        memoryRepository = FakeMemoryRepository()
        
        // Setup minimal dummy stubs for dependencies not being tested directly
        scheduleBoard = object : ScheduleBoard {
            override val upcomingItems = kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.smartsales.prism.domain.memory.ScheduleItem>())
            override suspend fun checkConflict(proposedStart: Long, durationMinutes: Int, excludeId: String?) = com.smartsales.prism.domain.memory.ConflictResult.Clear
            override suspend fun refresh() {}
            override suspend fun findLexicalMatch(targetQuery: String): com.smartsales.prism.domain.memory.ScheduleItem? = null
        }
        alarmScheduler = object : AlarmScheduler {
            override suspend fun cancelReminder(taskId: String) {}
            override suspend fun scheduleCascade(taskId: String, taskTitle: String, eventTime: Instant, cascade: List<String>) {}
        }
        unifiedPipeline = object : UnifiedPipeline {
            override suspend fun processInput(input: com.smartsales.core.pipeline.PipelineInput) = kotlinx.coroutines.flow.emptyFlow<com.smartsales.core.pipeline.PipelineResult>()
        }

        coordinator = SchedulerCoordinator(
            taskRepository,
            memoryRepository,
            scheduleBoard,
            alarmScheduler,
            unifiedPipeline
        )
    }

    @Test
    fun `autoCompleteExpiredTasks sweeps actionable task into factual memory`() = runTest {
        // Arrange: Inject a task from yesterday that is not done
        val yesterday = Instant.now().minus(1, ChronoUnit.DAYS)
        val expiredTask = ScheduledTask(
            id = "expired_task_1",
            timeDisplay = "10:00",
            title = "Follow up overdue",
            startTime = yesterday,
            endTime = yesterday.plus(1, ChronoUnit.HOURS),
            durationMinutes = 60,
            hasAlarm = false,
            alarmCascade = emptyList(),
            isDone = false,
            keyPersonEntityId = null
        )
        val insertedId = taskRepository.insertTask(expiredTask)

        // Act: Trigger sweeping
        coordinator.autoCompleteExpiredTasks()

        // Assert 1: Removed from actionable feed
        val today = LocalDate.now()
        val activeTasks = taskRepository.queryByDateRange(today, today).first()
        val stillExists = activeTasks.any { it.id == insertedId }
        assertEquals(false, stillExists)

        // Assert 2: Migrated to factual memory correctly
        val memories = memoryRepository.getAll(10)
        val migratedMemory = memories.find { it.entryId == insertedId }
        
        assertNotNull("Migrated memory entry should exist in MemoryRepository", migratedMemory)
        assertEquals("Follow up overdue", migratedMemory?.content)
        assertEquals(MemoryEntryType.SCHEDULE_ITEM, migratedMemory?.entryType)
        assertTrue("Migrated memory should be marked as archived by default", migratedMemory?.isArchived == true)
    }
}
