package com.smartsales.core.pipeline

import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.test.fakes.FakeAliasCache
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeEntityWriter
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakeInspirationRepository
import com.smartsales.core.test.fakes.FakeLightningRouter
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import com.smartsales.core.test.fakes.artifactGeneratePlugin
import com.smartsales.core.test.fakes.audioAnalyzePlugin
import com.smartsales.core.test.fakes.crmSheetGeneratePlugin
import com.smartsales.core.test.fakes.simulationTalkPlugin
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class IntentOrchestratorPluginLaneTest {

    private lateinit var fakeContextBuilder: FakeContextBuilder
    private lateinit var fakeLightningRouter: FakeLightningRouter
    private lateinit var fakeMascotService: FakeMascotService
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    private lateinit var fakeEntityWriter: FakeEntityWriter
    private lateinit var fakeAliasCache: FakeAliasCache
    private lateinit var fakeScheduleBoard: FakeScheduleBoard
    private lateinit var fakeTaskRepository: ScheduledTaskRepository
    private lateinit var fakeExecutor: FakeExecutor
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        fakeContextBuilder = FakeContextBuilder()
        fakeLightningRouter = FakeLightningRouter()
        fakeMascotService = FakeMascotService()
        fakeUnifiedPipeline = FakeUnifiedPipeline()
        fakeEntityWriter = FakeEntityWriter()
        fakeAliasCache = FakeAliasCache()
        fakeScheduleBoard = FakeScheduleBoard()
        fakeExecutor = FakeExecutor()

        fakeTaskRepository = object : ScheduledTaskRepository {
            override suspend fun batchInsertTasks(rules: List<ScheduledTask>): List<String> = emptyList()
            override suspend fun upsertTask(task: ScheduledTask): String = task.id
            override suspend fun insertTask(task: ScheduledTask): String = task.id
            override suspend fun updateTask(task: ScheduledTask) {}
            override suspend fun getTask(id: String): ScheduledTask? = null
            override fun queryByDateRange(start: java.time.LocalDate, end: java.time.LocalDate): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null
            override fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>> = emptyFlow()
            override suspend fun deleteItem(id: String) {}
            override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) {}
        }
    }

    private fun timeProvider(): TimeProvider = object : TimeProvider {
        override val now: Instant = Instant.parse("2026-03-18T00:00:00Z")
        override val currentTime = java.time.LocalTime.NOON
        override val today = java.time.LocalDate.parse("2026-03-18")
        override val zoneId: java.time.ZoneId = java.time.ZoneId.of("UTC")
        override fun formatForLlm(): String = "2026-03-18"
    }

    private fun orchestratorWithRegistry(toolRegistry: ToolRegistry): IntentOrchestrator {
        return IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = fakeLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline,
            entityWriter = fakeEntityWriter,
            aliasCache = fakeAliasCache,
            uniAExtractionService = RealUniAExtractionService(
                executor = fakeExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            uniBExtractionService = RealUniBExtractionService(
                executor = fakeExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            uniCExtractionService = RealUniCExtractionService(
                executor = fakeExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter()
            ),
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = fakeTaskRepository,
                scheduleBoard = fakeScheduleBoard,
                inspirationRepository = FakeInspirationRepository(),
                timeProvider = timeProvider()
            ),
            taskRepository = fakeTaskRepository,
            scheduleBoard = fakeScheduleBoard,
            toolRegistry = toolRegistry,
            timeProvider = timeProvider(),
            appScope = testScope
        )
    }

    @Test
    fun `non voice plugin dispatch becomes proposal and confirm executes through plugin lane`() = runTest {
        val plugin = artifactGeneratePlugin()
        val orchestrator = orchestratorWithRegistry(RealToolRegistry(setOf(plugin)))
        fakeContextBuilder.loadSession(
            "session-1",
            listOf(
                com.smartsales.core.context.ChatTurn("user", "先看华为进展"),
                com.smartsales.core.context.ChatTurn("assistant", "好的")
            )
        )

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.ToolDispatch(
                toolId = "artifact.generate",
                params = mapOf("ruleId" to "executive_report")
            )
        )

        val firstTurn = orchestrator.processInput("生成汇报").toList()
        assertEquals(1, firstTurn.size)
        assertTrue(firstTurn.single() is PipelineResult.ToolDispatchProposal)
        assertTrue(plugin.executedRequests.isEmpty())

        val confirmTurn = orchestrator.processInput("确认执行").toList()
        assertTrue(confirmTurn.any { it == PipelineResult.PluginExecutionStarted("artifact.generate") })
        assertTrue(
            confirmTurn.any { result ->
                result is PipelineResult.PluginExecutionEmittedState &&
                    result.uiState is com.smartsales.prism.domain.model.UiState.Response &&
                    (result.uiState as com.smartsales.prism.domain.model.UiState.Response).content.contains("artifact.generate completed")
            }
        )
        assertEquals(1, plugin.executedRequests.size)
        assertEquals("生成汇报", plugin.executedRequests.single().rawInput)
    }

    @Test
    fun `voice plugin dispatch yields execution states back to OS`() = runTest {
        val plugin = audioAnalyzePlugin()
        val orchestrator = orchestratorWithRegistry(RealToolRegistry(setOf(plugin)))
        fakeContextBuilder.loadSession(
            "session-1",
            listOf(
                com.smartsales.core.context.ChatTurn("user", "上次会议讲了预算"),
                com.smartsales.core.context.ChatTurn("assistant", "已记录")
            )
        )

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success("""{"decision":"NO_MATCH","reason":"not schedule"}""")
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.ToolDispatch(
                toolId = "audio.analyze",
                params = mapOf("ruleId" to "meeting_analysis")
            )
        )

        val results = orchestrator.processInput("分析这段录音", isVoice = true).toList()

        assertTrue(results.any { it == PipelineResult.PluginExecutionStarted("audio.analyze") })
        assertTrue(
            results.any { result ->
                result is PipelineResult.PluginExecutionEmittedState &&
                    result.uiState is com.smartsales.prism.domain.model.UiState.Response &&
                    (result.uiState as com.smartsales.prism.domain.model.UiState.Response).content.contains("audio.analyze completed")
            }
        )
        assertEquals(1, plugin.executedRequests.size)
        assertEquals("meeting_analysis", plugin.executedRequests.single().parameters["ruleId"])
    }

    @Test
    fun `voice plugin dispatch fails safely when permission is denied`() = runTest {
        val deniedPlugin = crmSheetGeneratePlugin(
            requiredPermissions = setOf(CoreModulePermission.READ_CRM_MEMORY)
        )
        val orchestrator = orchestratorWithRegistry(RealToolRegistry(setOf(deniedPlugin)))

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success("""{"decision":"NO_MATCH","reason":"not schedule"}""")
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.ToolDispatch(
                toolId = "crm.sheet.generate",
                params = mapOf("ruleId" to "account_sheet")
            )
        )

        val results = orchestrator.processInput("生成 CRM 表", isVoice = true).toList()

        assertTrue(results.any { it == PipelineResult.PluginExecutionStarted("crm.sheet.generate") })
        assertTrue(
            results.any { result ->
                result is PipelineResult.PluginExecutionEmittedState &&
                    result.uiState is com.smartsales.prism.domain.model.UiState.Error &&
                    (result.uiState as com.smartsales.prism.domain.model.UiState.Error).message.contains("READ_CRM_MEMORY")
            }
        )
        assertTrue(deniedPlugin.executedRequests.isEmpty())
    }

    @Test
    fun `voice plugin dispatch yields safe unknown tool error`() = runTest {
        val orchestrator = orchestratorWithRegistry(RealToolRegistry(setOf(simulationTalkPlugin())))

        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success("""{"decision":"NO_MATCH","reason":"not schedule"}""")
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(
            PipelineResult.ToolDispatch(
                toolId = "unknown.plugin",
                params = mapOf("ruleId" to "missing")
            )
        )

        val results = orchestrator.processInput("执行未知插件", isVoice = true).toList()

        assertTrue(results.any { it == PipelineResult.PluginExecutionStarted("unknown.plugin") })
        assertTrue(
            results.any { result ->
                result is PipelineResult.PluginExecutionEmittedState &&
                    result.uiState is com.smartsales.prism.domain.model.UiState.Error &&
                    (result.uiState as com.smartsales.prism.domain.model.UiState.Error).message.contains("Unknown tool ID")
            }
        )
    }
}
