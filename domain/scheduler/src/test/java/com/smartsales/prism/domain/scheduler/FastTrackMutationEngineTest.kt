package com.smartsales.prism.domain.scheduler

import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.memory.ScheduleItem
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
        engine = FastTrackMutationEngine(taskRepository, scheduleBoard, inspirationRepository)
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
