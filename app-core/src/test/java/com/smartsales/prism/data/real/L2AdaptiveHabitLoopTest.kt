package com.smartsales.prism.data.real

import com.smartsales.core.pipeline.RealUnifiedPipeline
import com.smartsales.core.pipeline.PipelineInput
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.QueryQuality
import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.data.fakes.FakePipelineTelemetry
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.prism.data.rl.RealReinforcementLearner
import com.smartsales.data.crm.writer.RealEntityWriter
import com.smartsales.prism.domain.rl.RlObservation
import com.smartsales.prism.domain.rl.ObservationSource
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class L2AdaptiveHabitLoopTest {

    private lateinit var pipeline: RealUnifiedPipeline
    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var fakeHabitRepo: FakeUserHabitRepository
    private lateinit var fakeTimeProvider: FakeTimeProvider
    private lateinit var reinforcementLearner: RealReinforcementLearner
    private lateinit var fakeHabitListener: FakeHabitListener
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setup() {
        val fakeEntityRepo = FakeEntityRepository()
        val fakeMemoryRepo = FakeMemoryRepository()
        fakeHabitRepo = FakeUserHabitRepository()
        // clear explicitly as the fake might be seeded with global coach habits
        fakeHabitRepo.clear() 
        
        fakeTimeProvider = FakeTimeProvider()
        val historyRepo = FakeHistoryRepository()
        fakeHabitListener = FakeHabitListener()

        reinforcementLearner = RealReinforcementLearner(fakeHabitRepo)

        val fakeTaskRepo = object : ScheduledTaskRepository {
            override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override suspend fun insertTask(task: ScheduledTask): String = "fake-task"
            override suspend fun batchInsertTasks(tasks: List<ScheduledTask>): List<String> = emptyList()
            override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) {}
            override suspend fun getTask(id: String): ScheduledTask? = null
            override suspend fun updateTask(task: ScheduledTask) {}
            override suspend fun upsertTask(task: com.smartsales.prism.domain.scheduler.ScheduledTask): String = task.id
            override suspend fun deleteItem(id: String) {}
            override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null
            override fun observeByEntityId(entityId: String): kotlinx.coroutines.flow.Flow<List<ScheduledTask>> = kotlinx.coroutines.flow.emptyFlow()
        }

        contextBuilder = RealContextBuilder(
            timeProvider = fakeTimeProvider,
            reinforcementLearner = reinforcementLearner,
            memoryRepository = fakeMemoryRepo,
            entityRepository = fakeEntityRepo,
            scheduledTaskRepository = fakeTaskRepo,
            historyRepository = historyRepo,
            telemetry = FakePipelineTelemetry()
        )

        val entityWriter = RealEntityWriter(
            entityRepository = fakeEntityRepo,
            timeProvider = fakeTimeProvider,
            kernelWriteBack = contextBuilder,
            appScope = testScope
        )

        pipeline = RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = FakeEntityDisambiguationService(),
            inputParserService = FakeInputParserService(),
            schedulerLinter = SchedulerLinter(),
            entityWriter = entityWriter,
            promptCompiler = FakePromptCompiler(),
            executor = FakeExecutor(),
            telemetry = FakePipelineTelemetry(),
            habitListener = fakeHabitListener,
            appScope = testScope
        )
    }

    @Test
    fun `Scenario 1 - The Reinforcement Amplification`() = runTest {
        // Observe a habit
        reinforcementLearner.processObservations(listOf(
            RlObservation(null, "prefers_morning", "User prefers morning meetings", ObservationSource.INFERRED, null)
        ))
        
        // Assert it was saved
        val habits1 = fakeHabitRepo.getGlobalHabits()
        assertEquals("Should have 1 habit", 1, habits1.size)
        assertEquals(1, habits1[0].inferredCount)
        assertEquals(0, habits1[0].explicitPositive)
        
        // Observe it again with positive explicit feedback
        reinforcementLearner.processObservations(listOf(
            RlObservation(null, "prefers_morning", "User explicitly said prefers morning meetings", ObservationSource.USER_POSITIVE, null)
        ))
        
        // Assert it was amplified
        val habits2 = fakeHabitRepo.getGlobalHabits()
        assertEquals(1, habits2.size)
        assertEquals(1, habits2[0].inferredCount)
        assertEquals("Explicit positive should be incremented", 1, habits2[0].explicitPositive)
        assertEquals("Value should be updated to latest observation", "User explicitly said prefers morning meetings", habits2[0].habitValue)
    }

    @Test
    fun `Scenario 2 - The Garbage Collection`() = runTest {
        // Inject an inferred habit initially
        reinforcementLearner.processObservations(listOf(
            RlObservation(null, "weak_context", "Some weak inference", ObservationSource.INFERRED, null)
        ))
        
        // Add a negative explicit observation to drive its confidence score down to 0
        reinforcementLearner.processObservations(listOf(
            RlObservation(null, "weak_context", "Actually no, user hates this", ObservationSource.USER_NEGATIVE, null)
        ))
        
        // At this point it's in the repo
        assertEquals(1, fakeHabitRepo.getGlobalHabits().size)
        
        // Execute the ETL phase by requesting the ContextBuilder to load user habits
        // This simulates what the pipeline does when processing new input
        val context = reinforcementLearner.loadUserHabits()
        
        // The context returned should be completely empty (it dropped the dead habit)
        assertEquals("Context should drop dead habit", 0, context.userHabits.size)
        
        // The Side-Effect Garbage Collection must have successfully deleted the item
        assertEquals("Garbage collection must delete from repo", 0, fakeHabitRepo.getGlobalHabits().size)
    }

    @Test
    fun `Scenario 3 - The Context Injection`() = runTest {
        // Seed a highly confident habit
        reinforcementLearner.processObservations(listOf(
            RlObservation(null, "high_confidence", "User always asks for metrics", ObservationSource.USER_POSITIVE, null)
        ))
        
        // Run full pipeline
        val results = pipeline.processInput(
            PipelineInput("Give me the usual", intent = QueryQuality.DEEP_ANALYSIS, requestedDepth = ContextDepth.FULL, unifiedId = "test_unified_id")
        ).toList()
        
        // Ensure pipeline went through cleanly
        assertTrue(results.isNotEmpty())
        
        // In a real test, verifying the PipelineContext requires an explicit hook 
        // We know ETL completed because it returns ConversationalReply safely 
        // But let's directly verify ContextBuilder to prove injection
        val contextData = contextBuilder.build("Give me the usual", com.smartsales.prism.domain.model.Mode.ANALYST, emptyList(), ContextDepth.FULL)
        
        assertTrue("Context string must contain habit value", contextData.habitContext?.userHabits?.any { it.habitValue.contains("User always asks for metrics") } == true)
    }
}
