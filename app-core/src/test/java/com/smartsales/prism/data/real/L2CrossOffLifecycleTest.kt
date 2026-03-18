package com.smartsales.prism.data.real

import android.content.Context
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniCExtractionService
import com.smartsales.core.test.fakes.FakeToolRegistry
import com.smartsales.core.test.fakes.FakeAlarmScheduler
import com.smartsales.core.test.fakes.FakeAliasCache
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakeLightningRouter
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.prism.domain.scheduler.TipGenerator
import com.smartsales.core.test.fakes.FakeMemoryRepository
import com.smartsales.core.test.fakes.FakePromptCompiler
import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.ui.drawers.scheduler.SchedulerViewModel
import com.smartsales.prism.domain.scheduler.SchedulerCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.time.Instant
import java.io.File

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
        val scheduleBoard = FakeScheduleBoard()
        val unifiedPipeline = FakeUnifiedPipeline()
        val coordinator = SchedulerCoordinator(
            taskRepository = taskRepository,
            memoryRepository = memoryRepository,
            scheduleBoard = scheduleBoard,
            alarmScheduler = FakeAlarmScheduler(),
            unifiedPipeline = unifiedPipeline
        )
        // Local JVM test leaf seam: Android Context has no shared fake and is not on the behavior path here.
        val appContext = mock(Context::class.java)
        val inspirationRepository = StubInspirationRepository()
        val tipGenerator = StubTipGenerator()
        val asrService = StubAsrService()
        val intentOrchestrator = buildIntentOrchestrator(
            taskRepository = taskRepository,
            inspirationRepository = inspirationRepository
        )

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

    private fun buildIntentOrchestrator(
        taskRepository: FakeScheduledTaskRepository,
        inspirationRepository: InspirationRepository
    ): IntentOrchestrator {
        val timeProvider = FakeTimeProvider()
        val schedulerLinter = SchedulerLinter(timeProvider)
        val uniAExtractionService = RealUniAExtractionService(
            executor = FakeExecutor(),
            promptCompiler = FakePromptCompiler(),
            schedulerLinter = schedulerLinter
        )
        val uniBExtractionService = RealUniBExtractionService(
            executor = FakeExecutor(),
            promptCompiler = FakePromptCompiler(),
            schedulerLinter = schedulerLinter
        )
        val uniCExtractionService = RealUniCExtractionService(
            executor = FakeExecutor(),
            promptCompiler = FakePromptCompiler(),
            schedulerLinter = schedulerLinter
        )

        return IntentOrchestrator(
            contextBuilder = FakeContextBuilder(),
            lightningRouter = FakeLightningRouter(),
            mascotService = FakeMascotService(),
            unifiedPipeline = FakeUnifiedPipeline(),
            entityWriter = com.smartsales.core.test.fakes.FakeEntityWriter(),
            aliasCache = FakeAliasCache(),
            uniAExtractionService = uniAExtractionService,
            uniBExtractionService = uniBExtractionService,
            uniCExtractionService = uniCExtractionService,
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = taskRepository,
                scheduleBoard = FakeScheduleBoard(),
                inspirationRepository = inspirationRepository,
                timeProvider = timeProvider
            ),
            taskRepository = taskRepository,
            scheduleBoard = FakeScheduleBoard(),
            toolRegistry = FakeToolRegistry(),
            timeProvider = timeProvider,
            appScope = TestScope(testDispatcher)
        )
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

    private class StubInspirationRepository : InspirationRepository {
        override suspend fun insert(text: String): String = "stub-inspiration"

        override fun getAll(): Flow<List<SchedulerTimelineItem.Inspiration>> = flowOf(emptyList())

        override suspend fun delete(id: String) = Unit
    }

    private class StubTipGenerator : TipGenerator {
        override suspend fun generate(task: ScheduledTask): List<String> = emptyList()
    }

    private class StubAsrService : AsrService {
        override suspend fun transcribe(file: File): AsrResult = AsrResult.Success("")

        override suspend fun isAvailable(): Boolean = true
    }
}
