package com.smartsales.prism.domain.scheduler

import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.memory.ScheduleItem
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class FastTrackMutationEngineTest {

    private lateinit var taskRepository: FakeScheduledTaskRepository
    private lateinit var scheduleBoard: FakeScheduleBoard
    private lateinit var inspirationRepository: FakeInspirationRepository
    private lateinit var engine: FastTrackMutationEngine

    @Before
    fun setup() {
        taskRepository = FakeScheduledTaskRepository()
        // Ensure starting clean per Clean Slate protocol
        scheduleBoard = FakeScheduleBoard()
        inspirationRepository = FakeInspirationRepository()
        engine = FastTrackMutationEngine(taskRepository, scheduleBoard, inspirationRepository, FakeTimeProvider())
    }

    @Test
    fun `Reschedule matches 1 task and succeeds with GUID inheritance`() = runTest {
        val originalStart = Instant.now()
        val originalTask = ScheduledTask(
            id = "", timeDisplay = "", title = "Discuss Budget",
            startTime = originalStart, durationMinutes = 30
        )
        val generatedId = taskRepository.insertTask(originalTask)

        // Board mock matches the query
        scheduleBoard.nextLexicalMatch = ScheduleItem(
            entryId = generatedId,
            title = "Discuss Budget",
            scheduledAt = originalStart.toEpochMilli(),
            durationMinutes = 30,
            durationSource = DurationSource.DEFAULT,
            conflictPolicy = ConflictPolicy.EXCLUSIVE
        )

        val intent = FastTrackResult.RescheduleTask(
            RescheduleTaskParams(
                targetQuery = "Budget",
                newStartTimeIso = "2026-03-20T14:00:00Z",
                newDurationMinutes = 60
            )
        )

        val result = engine.execute(intent)

        assertTrue("Expected Success, got $result", result is MutationResult.Success)
        
        val newTargetTask = taskRepository.getTask(generatedId)
        assertNotNull(newTargetTask)
        assertEquals("Discuss Budget", newTargetTask?.title)
        assertEquals(Instant.parse("2026-03-20T14:00:00Z"), newTargetTask?.startTime)
        assertEquals(60, newTargetTask?.durationMinutes)
        
        // Assert conflict was evaluated (it defaults to clear in FakeBoard)
        assertFalse(newTargetTask!!.hasConflict)
    }

    @Test
    fun `Reschedule with multiple matches returns AmbiguousMatch`() = runTest {
        scheduleBoard.nextLexicalMatch = null // Null means 0 or 2+ in findLexicalMatch contract
        
        val intent = FastTrackResult.RescheduleTask(
            RescheduleTaskParams(
                targetQuery = "Meeting",
                newStartTimeIso = "2026-03-20T14:00:00Z",
                newDurationMinutes = 60
            )
        )

        val result = engine.execute(intent)

        assertTrue(result is MutationResult.AmbiguousMatch)
        assertEquals("Meeting", (result as MutationResult.AmbiguousMatch).query)
    }

    @Test
    fun `CreateInspiration delegates to InspirationRepository`() = runTest {
        val intent = FastTrackResult.CreateInspiration(
            CreateInspirationParams(content = "Buy a guitar")
        )

        val result = engine.execute(intent)

        assertTrue(result is MutationResult.InspirationCreated)
        val inspirations = inspirationRepository.inspirations
        assertEquals(1, inspirations.size)
        assertEquals("Buy a guitar", inspirations.first().text)
    }

    @Test
    fun `CreateTasks with unifiedId preserves exact task id`() = runTest {
        val intent = FastTrackResult.CreateTasks(
            CreateTasksParams(
                unifiedId = "uni-a-123",
                tasks = listOf(
                    TaskDefinition(
                        title = "开会",
                        startTimeIso = "2026-03-20T14:00:00Z",
                        durationMinutes = 30,
                        urgency = UrgencyEnum.L2_IMPORTANT
                    )
                )
            )
        )

        val result = engine.execute(intent)

        assertTrue(result is MutationResult.Success)
        assertEquals(listOf("uni-a-123"), (result as MutationResult.Success).taskIds)
        val persisted = taskRepository.getTask("uni-a-123")
        assertNotNull(persisted)
        assertEquals("开会", persisted?.title)
        assertEquals(Instant.parse("2026-03-20T14:00:00Z"), persisted?.startTime)
        assertFalse(persisted!!.isVague)
        assertFalse(persisted.hasConflict)
    }

    @Test
    fun `CreateTasks with unifiedId and conflict persists Uni-D caution state`() = runTest {
        scheduleBoard.nextConflictResult = ConflictResult.Conflict(
            overlaps = listOf(
                ScheduleItem(
                    entryId = "existing-1",
                    title = "牙医预约",
                    scheduledAt = Instant.parse("2026-03-20T14:00:00Z").toEpochMilli(),
                    durationMinutes = 30,
                    durationSource = DurationSource.DEFAULT,
                    conflictPolicy = ConflictPolicy.EXCLUSIVE
                )
            )
        )

        val intent = FastTrackResult.CreateTasks(
            CreateTasksParams(
                unifiedId = "uni-a-conflict",
                tasks = listOf(
                    TaskDefinition(
                        title = "冲突会议",
                        startTimeIso = "2026-03-20T14:00:00Z",
                        durationMinutes = 30,
                        urgency = UrgencyEnum.L2_IMPORTANT
                    )
                )
            )
        )

        val result = engine.execute(intent)

        assertTrue(result is MutationResult.Success)
        assertEquals(listOf("uni-a-conflict"), (result as MutationResult.Success).taskIds)
        val persisted = taskRepository.getTask("uni-a-conflict")
        assertNotNull(persisted)
        assertTrue(persisted!!.hasConflict)
        assertFalse(persisted.isVague)
        assertEquals("existing-1", persisted.conflictWithTaskId)
        assertEquals("与「牙医预约」时间冲突", persisted.conflictSummary)
        assertEquals(Instant.parse("2026-03-20T14:00:00Z"), persisted.startTime)
    }

    @Test
    fun `CreateVagueTask preserves unifiedId and bypasses conflict evaluation`() = runTest {
        scheduleBoard.nextConflictResult = ConflictResult.Conflict(
            overlaps = emptyList()
        )

        val intent = FastTrackResult.CreateVagueTask(
            CreateVagueTaskParams(
                unifiedId = "uni-b-123",
                title = "提醒我开会",
                anchorDateIso = "2026-03-21",
                timeHint = "下午",
                urgency = UrgencyEnum.L3_NORMAL
            )
        )

        val result = engine.execute(intent)

        assertTrue(result is MutationResult.Success)
        assertEquals(listOf("uni-b-123"), (result as MutationResult.Success).taskIds)
        val persisted = taskRepository.getTask("uni-b-123")
        assertNotNull(persisted)
        assertTrue(persisted!!.isVague)
        assertFalse(persisted.hasConflict)
        assertEquals("待定", persisted.timeDisplay)
        assertEquals("时间待定（线索：下午）", persisted.notes)
        assertEquals(Instant.parse("2026-03-20T16:00:00Z"), persisted.startTime)
    }
}

/** Stub Fake */
class FakeInspirationRepository : InspirationRepository {
    data class Insp(val id: String, val text: String)
    val inspirations = mutableListOf<Insp>()
    
    override suspend fun insert(text: String): String {
        val id = "insp_${inspirations.size}"
        inspirations.add(Insp(id, text))
        return id
    }
    
    override fun getAll(): Flow<List<SchedulerTimelineItem.Inspiration>> = flowOf(emptyList())
    override suspend fun delete(id: String) { inspirations.removeAll { it.id == id } }
}
