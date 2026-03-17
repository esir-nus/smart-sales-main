package com.smartsales.core.pipeline

import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeLightningRouter
import com.smartsales.core.test.fakes.FakeEntityWriter
import com.smartsales.core.test.fakes.FakeAliasCache
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.FastTrackParser
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import com.smartsales.core.test.fakes.FakeToolRegistry

class IntentOrchestratorBreakItTest {

    private lateinit var orchestrator: IntentOrchestrator
    private lateinit var fakeContextBuilder: FakeContextBuilder
    private lateinit var fakeLightningRouter: FakeLightningRouter
    private lateinit var fakeMascotService: FakeMascotService
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    private lateinit var fakeEntityWriter: FakeEntityWriter
    private lateinit var fakeAliasCache: FakeAliasCache
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        fakeContextBuilder = FakeContextBuilder()
        fakeLightningRouter = FakeLightningRouter()
        fakeMascotService = FakeMascotService()
        fakeUnifiedPipeline = FakeUnifiedPipeline()
        fakeEntityWriter = FakeEntityWriter()
        fakeAliasCache = FakeAliasCache()

        orchestrator = IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = fakeLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline,
            entityWriter = fakeEntityWriter,
            aliasCache = fakeAliasCache,
            fastTrackParser = FastTrackParser(object : TimeProvider { 
                override val now: Instant = Instant.now() 
                override val currentTime: java.time.LocalTime = java.time.LocalTime.now()
                override val today: java.time.LocalDate = java.time.LocalDate.now()
                override val zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()
                override fun formatForLlm(): String = ""
            }),
            taskRepository = object : ScheduledTaskRepository {
                override suspend fun batchInsertTasks(rules: List<ScheduledTask>): List<String> = emptyList()
                override suspend fun upsertTask(task: ScheduledTask): String = ""
                override suspend fun insertTask(task: ScheduledTask): String = ""
                override suspend fun updateTask(task: ScheduledTask) {}
                override suspend fun getTask(id: String): ScheduledTask? = null
                override fun queryByDateRange(start: java.time.LocalDate, end: java.time.LocalDate): Flow<List<ScheduledTask>> = emptyFlow()
                override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = emptyFlow()
                override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()
                override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null
                override fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>> = emptyFlow()
                override suspend fun deleteItem(id: String) {}
                override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) {}
            },
            toolRegistry = FakeToolRegistry(),
            appScope = testScope
        )
    }

    @Test
    fun testEmptyInput() = runTest {
        // Break-it: Send an empty string
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.NOISE, false, "输入为空"))
        val result = orchestrator.processInput("").firstOrNull()
        assertEquals(PipelineResult.MascotIntercepted, result)
    }

    @Test
    fun testBlankInput() = runTest {
        // Break-it: Send a blank string
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.NOISE, false, "未检测到有效输入"))
        val result = orchestrator.processInput("   ").firstOrNull()
        assertEquals(PipelineResult.MascotIntercepted, result)
    }

    @Test
    fun testMaxLenInput() = runTest {
        // Break-it: Extremely long string
        val longString = "开会".repeat(5000)
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        
        // This should not crash the orchestrator
        val result = orchestrator.processInput(longString).firstOrNull()
        assertNull(result) // fake emits nothing
    }

    @Test
    fun testEmojiSpecialCharacters() = runTest {
        // Break-it: Emojis and special chars
        val weirdString = "👍 帮我是！！！🚨@#%@#%开会"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.NOISE, true, ""))
        val result = orchestrator.processInput(weirdString).firstOrNull()
        assertEquals(PipelineResult.MascotIntercepted, result)
    }
}
