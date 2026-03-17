package com.smartsales.prism.data.real

import android.content.Context

import com.smartsales.core.pipeline.UnifiedPipeline
import com.smartsales.core.test.fakes.FakeToolRegistry
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.domain.scheduler.TipGenerator
import com.smartsales.core.test.fakes.FakeMemoryRepository
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.ui.drawers.scheduler.SchedulerViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class L2CrossOffLifecycleTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var taskRepository: FakeScheduledTaskRepository
    private lateinit var memoryRepository: FakeMemoryRepository
    private lateinit var viewModel: SchedulerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        taskRepository = FakeScheduledTaskRepository()
        memoryRepository = FakeMemoryRepository()

        // Mock out dependencies not relevant to the Cross-Off lifecycle
        val coordinator = mock(com.smartsales.prism.domain.scheduler.SchedulerCoordinator::class.java)
        val appContext = mock(Context::class.java)
        val inspirationRepository = mock(InspirationRepository::class.java)
        val tipGenerator = mock(TipGenerator::class.java)
        val asrService = mock(com.smartsales.prism.domain.asr.AsrService::class.java)
        val intentOrchestrator = mock(com.smartsales.core.pipeline.IntentOrchestrator::class.java)

        viewModel = SchedulerViewModel(
            appContext = appContext,
            taskRepository = taskRepository,
            memoryRepository = memoryRepository,
            inspirationRepository = inspirationRepository,
            coordinator = coordinator,
            tipGenerator = tipGenerator,
            asrService = asrService,
            intentOrchestrator = intentOrchestrator,
            toolRegistry = FakeToolRegistry()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleDone true migrates task to factual memory and deletes from actionable feed`() = runTest {
        // 1. Setup Phase: Create an active task
        val activeTask = ScheduledTask(
            id = "temp-id",
            timeDisplay = "10:00",
            title = "Follow up with client",
            startTime = Instant.now(),
            isDone = false, // Not yet done
            keyPersonEntityId = "client-entity-123", // Has CRM linkage
            keyPerson = "John Doe"
        )
        val insertedId = taskRepository.insertTask(activeTask)

        // Ensure task is in actionable feed
        assertNotNull(taskRepository.getTask(insertedId))

        // 2. Action: Complete the task
        viewModel.toggleDone(insertedId)
        advanceUntilIdle() // Wait for coroutines to execute

        // 3. Verification - Factual Knowledge (MemoryEntry)
        // Search should find the entry
        val memoryEntries = memoryRepository.search("Follow up with client")
        assertEquals(1, memoryEntries.size)
        
        val migratedEntry = memoryEntries.first()
        assertEquals(MemoryEntryType.SCHEDULE_ITEM, migratedEntry.entryType)
        assertEquals(insertedId, migratedEntry.entryId)
        assertEquals("Follow up with client", migratedEntry.content) // Title maps to content
        assertEquals(true, migratedEntry.structuredJson?.contains("client-entity-123") == true)
        
        // 4. Verification - Actionable Feed (ScheduledTask)
        // Task MUST be deleted from the actionable feed to prevent pipeline ghosting
        assertNull("Task should be deleted from actionable feed after migration", taskRepository.getTask(insertedId))
    }
}
