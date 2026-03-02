package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * FakeOrchestrator 单元测试
 * 验证 Pipeline 核心契约
 */
class FakeOrchestratorTest {

    private lateinit var controller: AgentActivityController
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var taskRepository: ScheduledTaskRepository
    private lateinit var orchestrator: FakeOrchestrator
    
    @Before
    fun setup() {
        controller = AgentActivityController()
        timeProvider = FakeTimeProvider()
        taskRepository = object : ScheduledTaskRepository {
            override fun getTimelineItems(dayOffset: Int): Flow<List<TimelineItemModel>> = flowOf(emptyList())
            override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<TimelineItemModel>> = flowOf(emptyList())
            override suspend fun insertTask(task: TimelineItemModel.Task): String = "test-id"
            override suspend fun updateTask(task: TimelineItemModel.Task) {}
            override suspend fun deleteItem(id: String) {}
            override suspend fun getTask(id: String): TimelineItemModel.Task? = null
            override suspend fun getRecentCompleted(limit: Int): List<TimelineItemModel.Task> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): TimelineItemModel.Task? = null
        }
        orchestrator = FakeOrchestrator(controller, taskRepository, timeProvider)
    }

    @Test
    fun `processInput returns Response for Coach mode`() = runTest {
        val result = orchestrator.processInput("test input")
        
        assertTrue("Expected UiState.Response", result is UiState.Response)
    }

    @Test
    fun `switchMode updates currentMode`() = runTest {
        orchestrator.switchMode(Mode.ANALYST)
        
        assertEquals(Mode.ANALYST, orchestrator.currentMode.value)
    }

    @Test
    fun `processInput returns Response for Analyst mode with plan command`() = runTest {
        orchestrator.switchMode(Mode.ANALYST)
        
        // /plan command triggers Response (PlanCard is deprecated)
        val result = orchestrator.processInput("/plan analyze this")
        
        assertTrue("Expected UiState.Response", result is UiState.Response)
    }
    
    @Test
    fun `createScheduledTask returns SchedulerTaskCreated`() = runTest {
        val result = orchestrator.createScheduledTask("明天凌晨3点赶飞机")
        
        assertTrue("Expected UiState.SchedulerTaskCreated", result is UiState.SchedulerTaskCreated)
        val created = result as UiState.SchedulerTaskCreated
        assertEquals("赶飞机", created.title)
        assertEquals(1, created.dayOffset)
        assertTrue("durationMinutes should be positive", created.durationMinutes > 0)
        assertTrue("scheduledAtMillis should be positive", created.scheduledAtMillis > 0)
    }
}
