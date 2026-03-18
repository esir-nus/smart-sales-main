package com.smartsales.prism.ui.drawers.scheduler

import android.content.Context
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.PromptCompiler
import com.smartsales.core.pipeline.QueryQuality
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniCExtractionService
import com.smartsales.core.pipeline.RouterResult
import com.smartsales.core.test.fakes.FakeAlarmScheduler
import com.smartsales.core.test.fakes.FakeAliasCache
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeEntityWriter
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakeInspirationRepository
import com.smartsales.core.test.fakes.FakeLightningRouter
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.core.test.fakes.FakeMemoryRepository
import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.core.test.fakes.FakeToolRegistry
import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.SchedulerCoordinator
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.TipGenerator
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulerViewModelAudioStatusTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var taskRepository: FakeScheduledTaskRepository
    private lateinit var memoryRepository: FakeMemoryRepository
    private lateinit var fakeLightningRouter: FakeLightningRouter
    private lateinit var fakeUniAExecutor: FakeExecutor
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    private lateinit var fakeInspirationRepository: FakeInspirationRepository
    private lateinit var asrService: StubAsrService
    private lateinit var viewModel: SchedulerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        taskRepository = FakeScheduledTaskRepository()
        memoryRepository = FakeMemoryRepository()
        fakeLightningRouter = FakeLightningRouter()
        fakeUniAExecutor = FakeExecutor()
        fakeUnifiedPipeline = FakeUnifiedPipeline()
        fakeInspirationRepository = FakeInspirationRepository()
        asrService = StubAsrService()

        val coordinator = SchedulerCoordinator(
            taskRepository = taskRepository,
            memoryRepository = memoryRepository,
            scheduleBoard = FakeScheduleBoard(),
            alarmScheduler = FakeAlarmScheduler(),
            unifiedPipeline = fakeUnifiedPipeline
        )

        val intentOrchestrator = IntentOrchestrator(
            contextBuilder = FakeContextBuilder(),
            lightningRouter = fakeLightningRouter,
            mascotService = FakeMascotService(),
            unifiedPipeline = fakeUnifiedPipeline,
            entityWriter = FakeEntityWriter(),
            aliasCache = FakeAliasCache(),
            uniAExtractionService = RealUniAExtractionService(
                executor = fakeUniAExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter(FakeTimeProvider())
            ),
            uniBExtractionService = RealUniBExtractionService(
                executor = fakeUniAExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter(FakeTimeProvider())
            ),
            uniCExtractionService = RealUniCExtractionService(
                executor = fakeUniAExecutor,
                promptCompiler = PromptCompiler(),
                schedulerLinter = SchedulerLinter(FakeTimeProvider())
            ),
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = taskRepository,
                scheduleBoard = FakeScheduleBoard(),
                inspirationRepository = fakeInspirationRepository,
                timeProvider = FakeTimeProvider()
            ),
            taskRepository = taskRepository,
            scheduleBoard = FakeScheduleBoard(),
            toolRegistry = FakeToolRegistry(),
            timeProvider = FakeTimeProvider(),
            appScope = TestScope(testDispatcher)
        )

        viewModel = SchedulerViewModel(
            appContext = mock(Context::class.java),
            taskRepository = taskRepository,
            memoryRepository = memoryRepository,
            inspirationRepository = fakeInspirationRepository,
            coordinator = coordinator,
            tipGenerator = StubTipGenerator(),
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
    fun `processAudio shows success only after PathACommitted`() = kotlinx.coroutines.test.runTest {
        asrService.nextResult = AsrResult.Success("明天下午三点开会")
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "开会",
                    "startTimeIso": "2026-03-18T07:00:00Z",
                    "durationMinutes": 60,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(PipelineResult.ConversationalReply("Path B reply"))
        val observedStatuses = mutableListOf<String?>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.pipelineStatus.take(5).toList(observedStatuses)
        }

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals(true, observedStatuses.contains("✅ 搞定"))
        assertNull(viewModel.pipelineStatus.value)
        collectJob.cancel()
    }

    @Test
    fun `processAudio does not show success for conversational reply without scheduler write proof`() = kotlinx.coroutines.test.runTest {
        asrService.nextResult = AsrResult.Success("明天开会")
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "缺少明确时间"
                }
                """.trimIndent()
            )
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(PipelineResult.ConversationalReply("分析结果"))
        val observedStatuses = mutableListOf<String?>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.pipelineStatus.take(5).toList(observedStatuses)
        }

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals(true, observedStatuses.contains("未创建日程"))
        assertEquals(false, observedStatuses.contains("✅ 搞定"))
        collectJob.cancel()
    }

    @Test
    fun `processAudio forwards displayed scheduler day into Uni-A prompt`() = kotlinx.coroutines.test.runTest {
        val expectedDisplayedDate = LocalDate.now().plusDays(1).toString()
        asrService.nextResult = AsrResult.Success("后一天一点提醒我开会")
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "测试页锚点"
                }
                """.trimIndent()
            )
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(PipelineResult.ConversationalReply("分析结果"))

        viewModel.onDateSelected(1)
        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertTrue(
            fakeUniAExecutor.executedPrompts.any { it.contains("displayed_date_iso: $expectedDisplayedDate") }
        )
    }

    @Test
    fun `processAudio shows success for Uni-B vague create after exact lane declines`() = kotlinx.coroutines.test.runTest {
        asrService.nextResult = AsrResult.Success("三天以后提醒我开会")
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "缺少明确时间"
                }
                """.trimIndent()
            )
        )
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "VAGUE_CREATE",
                  "task": {
                    "title": "提醒我开会",
                    "anchorDateIso": "2026-03-21",
                    "timeHint": "时间待定",
                    "urgency": "L3"
                  }
                }
                """.trimIndent()
            )
        )
        fakeUnifiedPipeline.nextResultFlow = flowOf(PipelineResult.ConversationalReply("Path B reply"))
        val observedStatuses = mutableListOf<String?>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.pipelineStatus.take(5).toList(observedStatuses)
        }

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals(true, observedStatuses.contains("✅ 搞定"))
        collectJob.cancel()
    }

    @Test
    fun `processAudio shows inspiration success for Uni-C and never claims scheduler success`() = kotlinx.coroutines.test.runTest {
        asrService.nextResult = AsrResult.Success("以后想练口语")
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "没有时间承诺"
                }
                """.trimIndent()
            )
        )
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_VAGUE",
                  "reason": "不是待定日程"
                }
                """.trimIndent()
            )
        )
        fakeUniAExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "INSPIRATION_CREATE",
                  "idea": {
                    "content": "以后想练口语",
                    "title": "练口语"
                  }
                }
                """.trimIndent()
            )
        )

        val observedStatuses = mutableListOf<String?>()
        val collectJob = backgroundScope.launch(testDispatcher) {
            viewModel.pipelineStatus.take(5).toList(observedStatuses)
        }

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals(true, observedStatuses.contains("💡 已保存灵感"))
        assertEquals(false, observedStatuses.contains("✅ 搞定"))
        assertEquals(0, taskRepository.getTimelineItems(0).first().filterIsInstance<ScheduledTask>().size)
        collectJob.cancel()
    }

    private class StubTipGenerator : TipGenerator {
        override suspend fun generate(task: ScheduledTask): List<String> = emptyList()
    }

    private class StubAsrService : AsrService {
        var nextResult: AsrResult = AsrResult.Success("")

        override suspend fun transcribe(file: File): AsrResult = nextResult

        override suspend fun isAvailable(): Boolean = true
    }
}
