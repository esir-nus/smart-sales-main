package com.smartsales.prism.ui.sim

import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.pipeline.PromptCompiler
import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
import com.smartsales.core.pipeline.SchedulerIntelligenceRouter
import com.smartsales.core.pipeline.SchedulerPathACreateInterpreter
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniCExtractionService
import com.smartsales.core.pipeline.RealUniMExtractionService
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.test.fakes.FakeActiveTaskRetrievalIndex
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakeInspirationRepository
import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.prism.data.notification.ExactAlarmPermissionGate
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleItem
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.memory.TargetResolution
import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.memory.bypassesConflictEvaluation
import com.smartsales.prism.domain.memory.overlapsInScheduleBoard
import com.smartsales.core.test.fakes.FakeAlarmScheduler
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.CreateVagueTaskParams
import com.smartsales.prism.domain.scheduler.CreateTasksParams
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.TaskDefinition
import com.smartsales.prism.domain.scheduler.UrgencyEnum
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SimSchedulerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var taskRepository: FakeScheduledTaskRepository
    private lateinit var inspirationRepository: FakeInspirationRepository
    private lateinit var scheduleBoard: FakeScheduleBoard
    private lateinit var activeTaskRetrievalIndex: FakeActiveTaskRetrievalIndex
    private lateinit var fakeExecutor: FakeExecutor
    private lateinit var asrService: StubAsrService
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var alarmScheduler: FakeAlarmScheduler
    private lateinit var exactAlarmPermissionGate: FakeExactAlarmPermissionGate
    private lateinit var viewModel: SimSchedulerViewModel
    private val valveEvents = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        valveEvents.clear()
        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            valveEvents += checkpoint to summary
        }
        taskRepository = FakeScheduledTaskRepository()
        inspirationRepository = FakeInspirationRepository()
        scheduleBoard = FakeScheduleBoard()
        activeTaskRetrievalIndex = FakeActiveTaskRetrievalIndex()
        fakeExecutor = FakeExecutor()
        asrService = StubAsrService()
        timeProvider = FakeTimeProvider()
        alarmScheduler = FakeAlarmScheduler()
        exactAlarmPermissionGate = FakeExactAlarmPermissionGate()
        timeProvider.fixedInstant = Instant.parse("2026-03-20T09:00:00Z")

        val schedulerLinter = SchedulerLinter(timeProvider)
        val promptCompiler = PromptCompiler()

        viewModel = buildViewModel(scheduleBoard, schedulerLinter, promptCompiler)
    }

    private fun buildViewModel(
        scheduleBoard: ScheduleBoard,
        schedulerLinter: SchedulerLinter = SchedulerLinter(timeProvider),
        promptCompiler: PromptCompiler = PromptCompiler()
    ): SimSchedulerViewModel {
        return SimSchedulerViewModel(
            taskRepository = taskRepository,
            inspirationRepository = inspirationRepository,
            scheduleBoard = scheduleBoard,
            activeTaskRetrievalIndex = activeTaskRetrievalIndex,
            alarmScheduler = alarmScheduler,
            exactAlarmPermissionGate = exactAlarmPermissionGate,
            fastTrackMutationEngine = FastTrackMutationEngine(
                taskRepository = taskRepository,
                scheduleBoard = scheduleBoard,
                inspirationRepository = inspirationRepository,
                timeProvider = timeProvider
            ),
            asrService = asrService,
            globalRescheduleExtractionService = RealGlobalRescheduleExtractionService(fakeExecutor, promptCompiler, schedulerLinter),
            uniMExtractionService = RealUniMExtractionService(fakeExecutor, promptCompiler, schedulerLinter),
            uniAExtractionService = RealUniAExtractionService(fakeExecutor, promptCompiler, schedulerLinter),
            uniBExtractionService = RealUniBExtractionService(fakeExecutor, promptCompiler, schedulerLinter),
            uniCExtractionService = RealUniCExtractionService(fakeExecutor, promptCompiler, schedulerLinter),
            timeProvider = timeProvider
        )
    }

    @After
    fun tearDown() {
        PipelineValve.testInterceptor = null
        Dispatchers.resetMain()
    }

    @Test
    fun `processAudio single task now day offset exact create emits telemetry`() = runTest {
        asrService.nextResult = AsrResult.Success("明天下午三点开会")
        enqueueExactCreateResponse(startTimeIso = "2026-03-21T07:00:00Z")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertTrue(taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot().any { it is ScheduledTask && it.title == "开会" })
        assertTrue(
            valveEvents.any {
                it.first == PipelineValve.Checkpoint.TASK_EXTRACTED &&
                    it.second == "SIM scheduler single-task NOW_DAY_OFFSET extracted"
            }
        )
    }

    @Test
    fun `processAudio single task now offset exact create emits telemetry`() = runTest {
        asrService.nextResult = AsrResult.Success("三小时后开会")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertTrue(taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(1)).snapshot().any { it is ScheduledTask && it.title == "开会" })
        assertTrue(
            valveEvents.any {
                it.first == PipelineValve.Checkpoint.TASK_EXTRACTED &&
                it.second == "SIM scheduler single-task NOW_OFFSET extracted"
            }
        )
        assertTrue(fakeExecutor.executedPrompts.isEmpty())
    }

    @Test
    fun `processAudio single task explicit later variant stays exact and emits now offset telemetry`() = runTest {
        asrService.nextResult = AsrResult.Success("八个小时以后赶高铁")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(1)).snapshot()
            .filterIsInstance<ScheduledTask>()
        assertTrue(tasks.any { it.title == "赶高铁" && !it.isVague && it.startTime == Instant.parse("2026-03-20T17:00:00Z") })
        assertTrue(
            valveEvents.any {
                it.first == PipelineValve.Checkpoint.TASK_EXTRACTED &&
                it.second == "SIM scheduler single-task NOW_OFFSET extracted"
            }
        )
        assertTrue(fakeExecutor.executedPrompts.isEmpty())
    }

    @Test
    fun `processAudio single task explicit after variant stays exact and emits now offset telemetry`() = runTest {
        asrService.nextResult = AsrResult.Success("三小时之后开会")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(1)).snapshot()
            .filterIsInstance<ScheduledTask>()
        assertTrue(tasks.any { it.title == "开会" && !it.isVague && it.startTime == Instant.parse("2026-03-20T12:00:00Z") })
        assertTrue(
            valveEvents.any {
                it.first == PipelineValve.Checkpoint.TASK_EXTRACTED &&
                it.second == "SIM scheduler single-task NOW_OFFSET extracted"
            }
        )
        assertTrue(fakeExecutor.executedPrompts.isEmpty())
    }


    @Test
    fun `processAudio keeps displayed page date in shared create prompts`() = runTest {
        viewModel.onDateSelected(2)
        asrService.nextResult = AsrResult.Success("后一天提醒我回电话")
        enqueueNotMultiResponse()
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "date only"
                }
                """.trimIndent()
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "VAGUE_CREATE",
                  "task": {
                    "title": "回电话",
                    "anchorDateIso": "2026-03-23",
                    "timeHint": "白天",
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertTrue(fakeExecutor.executedPrompts.any { it.contains("displayed_date_iso: 2026-03-22") })
        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(4)).snapshot()
            .filterIsInstance<ScheduledTask>()
        assertTrue(tasks.any { it.title == "回电话" && it.isVague })
    }

    @Test
    fun `processAudio actually prefix no longer forces scheduler drawer into reschedule lane`() = runTest {
        asrService.nextResult = AsrResult.Success("actually 明天早上九点带合同见老板")
        enqueueNotMultiResponse()
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "带合同见老板",
                    "startTimeIso": "2026-03-21T01:00:00Z",
                    "durationMinutes": 0,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot()
            .filterIsInstance<ScheduledTask>()
        assertTrue(tasks.any { it.title == "带合同见老板" })
        assertNull(activeTaskRetrievalIndex.lastShortlistTranscript)
    }

    @Test
    fun `processAudio malformed explicit relative input fails with scheduler owned copy`() = runTest {
        asrService.nextResult = AsrResult.Success("八个小时以后")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals("已识别为相对时间日程，但任务内容不完整", viewModel.pipelineStatus.value)
        assertEquals("已识别为相对时间日程，但任务内容不完整", viewModel.conflictWarning.value)
        assertTrue(fakeExecutor.executedPrompts.isEmpty())
        assertTrue(taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(1)).snapshot().isEmpty())
    }

    @Test
    fun `processAudio wake reminder day clock create uses deterministic exact branch`() = runTest {
        asrService.nextResult = AsrResult.Success("明天早上九点喊我起来")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val expectedStart = timeProvider.today.plusDays(1)
            .atTime(9, 0)
            .atZone(timeProvider.zoneId)
            .toInstant()
        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot()
            .filterIsInstance<ScheduledTask>()
        val createdTask = tasks.single()

        assertEquals("喊我起来", createdTask.title)
        assertEquals(expectedStart, createdTask.startTime)
        assertEquals(UrgencyLevel.FIRE_OFF, createdTask.urgencyLevel)
        assertEquals(listOf("0m"), createdTask.alarmCascade)
        assertTrue(createdTask.hasAlarm)
        assertEquals(null, viewModel.conflictWarning.value)
        assertTrue(fakeExecutor.executedPrompts.isEmpty())
    }

    @Test
    fun `processAudio qualified weekday wake reminder uses deterministic exact branch`() = runTest {
        asrService.nextResult = AsrResult.Success("下周三早上八点钟提醒我起床")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val expectedStart = timeProvider.today.plusDays(5)
            .atTime(8, 0)
            .atZone(timeProvider.zoneId)
            .toInstant()
        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(7)).snapshot()
            .filterIsInstance<ScheduledTask>()
        val createdTask = tasks.single()

        assertEquals("起床", createdTask.title)
        assertEquals(expectedStart, createdTask.startTime)
        assertEquals(UrgencyLevel.FIRE_OFF, createdTask.urgencyLevel)
        assertEquals(listOf("0m"), createdTask.alarmCascade)
        assertTrue(createdTask.hasAlarm)
        assertNull(viewModel.conflictWarning.value)
        assertTrue(fakeExecutor.executedPrompts.isEmpty())
    }

    @Test
    fun `processAudio date only wake reminder does not fabricate exact time`() = runTest {
        asrService.nextResult = AsrResult.Success("明天叫我起来")
        enqueueNotMultiResponse()
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "缺少明确时间"
                }
                """.trimIndent()
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "VAGUE_CREATE",
                  "task": {
                    "title": "叫我起来",
                    "anchorDateIso": "2026-03-21",
                    "urgency": "FIRE_OFF"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot()
            .filterIsInstance<ScheduledTask>()
        val createdTask = tasks.single()
        assertTrue(createdTask.isVague)
        assertTrue(alarmScheduler.getAlarmsForTask(createdTask.id).isEmpty())
    }

    @Test
    fun `processAudio final classifier reason is replaced with scheduler owned copy`() = runTest {
        asrService.nextResult = AsrResult.Success("明天下午三点开会")
        enqueueNotMultiResponse()
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "缺少明确时间"
                }
                """.trimIndent()
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_VAGUE",
                  "reason": "该输入明确是安排日程，包含时间信息"
                }
                """.trimIndent()
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_INSPIRATION",
                  "reason": "该输入明确是安排日程，包含时间信息"
                }
                """.trimIndent()
            )
        )
        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals("未能解析为可创建日程，请换一种更明确的说法", viewModel.pipelineStatus.value)
        assertEquals("未能解析为可创建日程，请换一种更明确的说法", viewModel.conflictWarning.value)
    }

    @Test
    fun `normalizeSimSchedulerDrawerFailureMessage replaces raw classifier wording`() {
        val rawMessage = "输入包含可执行的日程安排，如“下周三提醒我”和“早上8点起床”，不符合灵感提取标准"

        val displayed = normalizeSimSchedulerDrawerFailureMessage(
            intentKind = SchedulerIntelligenceRouter.SchedulerIntentKind.CREATE,
            rawMessage = rawMessage
        )

        assertEquals("未能解析为可创建日程，请换一种更明确的说法", displayed)
    }

    @Test
    fun `normalizeSimSchedulerDrawerFailureMessage replaces schedulable classifier wording family`() {
        val rawMessage = "该输入明确是一个可安排的日程提醒，属于可执行的排程承诺"

        val displayed = normalizeSimSchedulerDrawerFailureMessage(
            intentKind = SchedulerIntelligenceRouter.SchedulerIntentKind.CREATE,
            rawMessage = rawMessage
        )

        assertEquals("未能解析为可创建日程，请换一种更明确的说法", displayed)
    }

    @Test
    fun `buildSimSchedulerTranscriptLog includes transcript source and text`() {
        val message = buildSimSchedulerTranscriptLog(
            transcript = "明天早上九点带合同见老板",
            source = "scheduler_rec_asr"
        )

        assertEquals(
            "transcript_ingress source=scheduler_rec_asr length=12 text=明天早上九点带合同见老板",
            message
        )
    }

    @Test
    fun `buildSimSchedulerRouterDecisionLog includes owner and reason`() {
        val metadata = SchedulerIntelligenceRouter.RouteMetadata(
            surface = SchedulerIntelligenceRouter.SchedulerSurface.SCHEDULER_DRAWER,
            intentKind = SchedulerIntelligenceRouter.SchedulerIntentKind.NONE,
            taskShape = SchedulerIntelligenceRouter.SchedulerTaskShape.UNSUPPORTED,
            owner = SchedulerIntelligenceRouter.SchedulerRouteOwner.REJECT,
            schedulerTerminalOnCommit = false,
            reason = "Uni-B decided input is not vague-create"
        )

        val message = buildSimSchedulerRouterDecisionLog(metadata)

        assertEquals(
            "route_decision intent=NONE shape=UNSUPPORTED owner=REJECT terminal=false reason=Uni-B decided input is not vague-create",
            message
        )
    }

    @Test
    fun `buildSimSchedulerUiFailureLog includes create and uni c reasons`() {
        val metadata = SchedulerIntelligenceRouter.RouteMetadata(
            surface = SchedulerIntelligenceRouter.SchedulerSurface.SCHEDULER_DRAWER,
            intentKind = SchedulerIntelligenceRouter.SchedulerIntentKind.NONE,
            taskShape = SchedulerIntelligenceRouter.SchedulerTaskShape.UNSUPPORTED,
            owner = SchedulerIntelligenceRouter.SchedulerRouteOwner.REJECT,
            schedulerTerminalOnCommit = false,
            reason = "not used"
        )

        val message = buildSimSchedulerUiFailureLog(
            branch = "not_matched_uni_c",
            metadata = metadata,
            displayedMessage = "未能解析为可创建日程，请换一种更明确的说法",
            createReason = "Uni-B decided input is not vague-create",
            uniCReason = "该输入明确包含时间信息和可执行的提醒动作，属于日程安排"
        )

        assertEquals(
            "ui_failure branch=not_matched_uni_c intent=NONE owner=REJECT createReason=Uni-B decided input is not vague-create uniCReason=该输入明确包含时间信息和可执行的提醒动作，属于日程安排 displayed=未能解析为可创建日程，请换一种更明确的说法",
            message
        )
    }

    @Test
    fun `buildSimSchedulerCreateResultLog includes route stage and uni m outcome`() {
        val message = buildSimSchedulerCreateResultLog(
            resultKind = "CreateTasks",
            telemetry = SchedulerPathACreateInterpreter.Telemetry(
                routeStage = SchedulerPathACreateInterpreter.RouteStage.UNI_A,
                uniMAttemptOutcome = SchedulerPathACreateInterpreter.UniMAttemptOutcome.NOT_MULTI
            ),
            itemCount = 1,
            parseUnresolvedCount = 0,
            downgradedCount = 0
        )

        assertEquals(
            "create_result kind=CreateTasks routeStage=UNI_A uniM=NOT_MULTI itemCount=1 parseUnresolved=0 downgraded=0",
            message
        )
    }

    @Test
    fun `processAudio scheduler drawer reschedule unsupported keeps scheduler owned copy`() = runTest {
        asrService.nextResult = AsrResult.Success("把客户复盘改期")
        enqueueGlobalRescheduleUnsupported("输入不明确，无法确定要改期的具体任务或目标线索")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals("SIM 当前仅支持明确目标 + 明确时间改期", viewModel.pipelineStatus.value)
        assertEquals("SIM 当前仅支持明确目标 + 明确时间改期", viewModel.conflictWarning.value)
    }

    @Test
    fun `processAudio vague create falls back to Uni-B`() = runTest {
        asrService.nextResult = AsrResult.Success("明天提醒我开会")
        enqueueNotMultiResponse()
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "缺少明确时间"
                }
                """.trimIndent()
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "VAGUE_CREATE",
                  "task": {
                    "title": "提醒我开会",
                    "anchorDateIso": "2026-03-21",
                    "timeHint": "下午",
                    "urgency": "L3"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertTrue(taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot().any {
            it is ScheduledTask && it.title == "提醒我开会" && it.isVague
        })
    }

    @Test
    fun `processAudio inspiration create keeps shelf alive`() = runTest {
        asrService.nextResult = AsrResult.Success("记一下下周可以聊新的会员方案")
        enqueueNotMultiResponse()
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "不是精确任务"
                }
                """.trimIndent()
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_VAGUE",
                  "reason": "更像灵感"
                }
                """.trimIndent()
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "INSPIRATION_CREATE",
                  "idea": {
                    "content": "下周可以聊新的会员方案"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertTrue(inspirationRepository.getAll().snapshot().isNotEmpty())
    }

    @Test
    fun `topUrgentTasks returns top three active tasks with conflict first then urgency then time`() = runTest {
        taskRepository.insertTask(
            scheduledTask(
                id = "l3_early",
                title = "普通早任务",
                startTime = timeProvider.now.plus(20, ChronoUnit.MINUTES),
                urgencyLevel = UrgencyLevel.L3_NORMAL
            )
        )
        taskRepository.insertTask(
            scheduledTask(
                id = "l1_late",
                title = "关键晚任务",
                startTime = timeProvider.now.plus(3, ChronoUnit.HOURS),
                urgencyLevel = UrgencyLevel.L1_CRITICAL
            )
        )
        taskRepository.insertTask(
            scheduledTask(
                id = "l2_early",
                title = "重要早任务",
                startTime = timeProvider.now.plus(30, ChronoUnit.MINUTES),
                urgencyLevel = UrgencyLevel.L2_IMPORTANT
            )
        )
        taskRepository.insertTask(
            scheduledTask(
                id = "l1_earlier",
                title = "关键早任务",
                startTime = timeProvider.now.plus(10, ChronoUnit.MINUTES),
                urgencyLevel = UrgencyLevel.L1_CRITICAL
            )
        )
        taskRepository.insertTask(
            scheduledTask(
                id = "conflict",
                title = "冲突提醒",
                startTime = timeProvider.now.plus(4, ChronoUnit.HOURS),
                urgencyLevel = UrgencyLevel.L3_NORMAL,
                hasConflict = true
            )
        )
        taskRepository.insertTask(
            scheduledTask(
                id = "done_hidden",
                title = "已完成任务",
                startTime = timeProvider.now.plus(5, ChronoUnit.MINUTES),
                urgencyLevel = UrgencyLevel.L1_CRITICAL,
                isDone = true
            )
        )

        advanceUntilIdle()

        assertEquals(
            listOf("冲突提醒", "关键早任务", "关键晚任务"),
            viewModel.topUrgentTasks.value.map { it.title }
        )
    }

    @Test
    fun `onReschedule with explicit time updates existing task`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task-1",
                timeDisplay = "10:00",
                title = "客户会议",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-20T02:00:00Z"),
                durationMinutes = 60
            )
        )

        viewModel.onReschedule(taskId, "改到今天下午四点半")
        advanceUntilIdle()

        val updated = taskRepository.getTask(taskId)
        assertNotNull(updated)
        assertEquals(Instant.parse("2026-03-20T08:30:00Z"), updated!!.startTime)
        assertEquals(60, updated.durationMinutes)
        assertTrue(fakeExecutor.executedPrompts.isEmpty())
    }

    @Test
    fun `onReschedule off page move arms right exit motion and destination attention`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task-1",
                timeDisplay = "10:00",
                title = "客户会议",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-20T02:00:00Z"),
                durationMinutes = 60
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "客户会议",
                    "startTimeIso": "2026-03-21T08:30:00Z",
                    "durationMinutes": 45,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.onReschedule(taskId, "改到明天下午四点半")
        advanceTimeBy(300)
        runCurrent()

        assertEquals(1, viewModel.exitingTasks.value.size)
        assertEquals(com.smartsales.prism.ui.drawers.scheduler.ExitDirection.RIGHT, viewModel.exitingTasks.value.single().exitDirection)
        assertEquals(setOf(1), viewModel.unacknowledgedDates.value)
        assertEquals(setOf(1), viewModel.rescheduledDates.value)
        assertTrue(
            valveEvents.any {
                it.first == PipelineValve.Checkpoint.UI_STATE_EMITTED &&
                    it.second == "SIM scheduler reschedule exit motion armed"
            }
        )

        advanceTimeBy(500)
        advanceUntilIdle()
        assertTrue(viewModel.exitingTasks.value.isEmpty())
    }

    @Test
    fun `onReschedule same day later time arms right exit motion without destination attention`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task-1",
                timeDisplay = "10:00",
                title = "客户会议",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-20T02:00:00Z"),
                durationMinutes = 60
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "客户会议",
                    "startTimeIso": "2026-03-20T08:30:00Z",
                    "durationMinutes": 45,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.onReschedule(taskId, "改到今天下午四点半")
        advanceTimeBy(300)
        runCurrent()

        assertEquals(1, viewModel.exitingTasks.value.size)
        assertEquals(com.smartsales.prism.ui.drawers.scheduler.ExitDirection.RIGHT, viewModel.exitingTasks.value.single().exitDirection)
        assertTrue(viewModel.unacknowledgedDates.value.isEmpty())
        assertTrue(viewModel.rescheduledDates.value.isEmpty())
    }

    @Test
    fun `onReschedule same day earlier time arms left exit motion without destination attention`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task-1",
                timeDisplay = "16:00",
                title = "客户会议",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-20T08:00:00Z"),
                durationMinutes = 60
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "客户会议",
                    "startTimeIso": "2026-03-20T05:00:00Z",
                    "durationMinutes": 45,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.onReschedule(taskId, "改到今天下午一点")
        advanceTimeBy(300)
        runCurrent()

        assertEquals(1, viewModel.exitingTasks.value.size)
        assertEquals(com.smartsales.prism.ui.drawers.scheduler.ExitDirection.LEFT, viewModel.exitingTasks.value.single().exitDirection)
        assertTrue(viewModel.unacknowledgedDates.value.isEmpty())
        assertTrue(viewModel.rescheduledDates.value.isEmpty())
    }

    @Test
    fun `onReschedule duration only change does not arm exit motion`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task-1",
                timeDisplay = "10:00",
                title = "客户会议",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-20T02:00:00Z"),
                durationMinutes = 60
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "客户会议",
                    "startTimeIso": "2026-03-20T02:00:00Z",
                    "durationMinutes": 90,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.onReschedule(taskId, "延长到九十分钟")
        advanceUntilIdle()

        assertTrue(viewModel.exitingTasks.value.isEmpty())
        assertTrue(viewModel.unacknowledgedDates.value.isEmpty())
        assertTrue(viewModel.rescheduledDates.value.isEmpty())
        assertFalse(
            valveEvents.any {
                it.first == PipelineValve.Checkpoint.UI_STATE_EMITTED &&
                    it.second == "SIM scheduler reschedule exit motion armed"
            }
        )
    }

    @Test
    fun `processAudio scheduler drawer voice reschedule updates matched task`() = runTest {
        val original = scheduledTask(
            id = "task-1",
            title = "跟张总吃饭",
            startTime = Instant.parse("2026-03-20T09:00:00Z"),
            urgencyLevel = UrgencyLevel.L2_IMPORTANT
        )
        taskRepository.insertTask(original)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.Resolved("task-1")
        asrService.nextResult = AsrResult.Success("把跟张总吃饭改到明天下午四点半")
        enqueueGlobalRescheduleExtraction(
            targetQuery = "跟张总吃饭",
            timeInstruction = "明天下午四点半",
            targetPerson = "张总"
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "改期占位",
                    "startTimeIso": "2026-03-21T08:30:00Z",
                    "durationMinutes": 60,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val updated = taskRepository.getTask("task-1")
        assertNotNull(updated)
        assertEquals(Instant.parse("2026-03-21T08:30:00Z"), updated!!.startTime)
        assertEquals(setOf(1), viewModel.rescheduledDates.value)
        assertEquals(setOf(1), viewModel.unacknowledgedDates.value)
    }

    @Test
    fun `processAudio scheduler drawer voice reschedule exact day clock phrase updates matched task without prompt`() = runTest {
        val original = scheduledTask(
            id = "task-1",
            title = "赶高铁",
            startTime = Instant.parse("2026-03-20T09:56:00Z"),
            urgencyLevel = UrgencyLevel.L2_IMPORTANT
        )
        taskRepository.insertTask(original)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.Resolved("task-1")
        asrService.nextResult = AsrResult.Success("把赶高铁的时间改到明天早上8点")
        enqueueGlobalRescheduleExtraction(
            targetQuery = "赶高铁",
            timeInstruction = "明天早上8点"
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val updated = taskRepository.getTask("task-1")
        assertNotNull(updated)
        assertEquals(Instant.parse("2026-03-21T00:00:00Z"), updated!!.startTime)
        assertTrue(fakeExecutor.executedPrompts.isNotEmpty())
    }

    @Test
    fun `processAudio scheduler drawer voice reschedule delta phrase safely fails without mutation`() = runTest {
        val original = scheduledTask(
            id = "task-1",
            title = "赶高铁",
            startTime = Instant.parse("2026-03-20T09:00:00Z"),
            urgencyLevel = UrgencyLevel.L2_IMPORTANT
        )
        taskRepository.insertTask(original)
        asrService.nextResult = AsrResult.Success("赶高铁时间推迟1个小时")
        enqueueGlobalRescheduleUnsupported("delta-only reschedule is unsupported")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val updated = taskRepository.getTask("task-1")
        assertNotNull(updated)
        assertEquals(Instant.parse("2026-03-20T09:00:00Z"), updated!!.startTime)
        assertFalse(updated.hasConflict)
        assertEquals("SIM 当前仅支持明确目标 + 明确时间改期", viewModel.conflictWarning.value)
        assertNull(scheduleBoard.lastDurationMinutes)
    }

    @Test
    fun `processAudio scheduler drawer voice reschedule delta phrase keeps conflicted target unchanged`() = runTest {
        val original = scheduledTask(
            id = "task-1",
            title = "赶高铁",
            startTime = Instant.parse("2026-03-20T09:00:00Z"),
            urgencyLevel = UrgencyLevel.L2_IMPORTANT,
            hasConflict = true
        )
        taskRepository.insertTask(original)
        scheduleBoard.nextConflictResult = ConflictResult.Conflict(
            overlaps = listOf(
                ScheduleItem(
                    entryId = "task-2",
                    title = "叫我吃饭",
                    scheduledAt = Instant.parse("2026-03-20T10:00:00Z").toEpochMilli(),
                    durationMinutes = 30,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        asrService.nextResult = AsrResult.Success("赶高铁时间推迟1个小时")
        enqueueGlobalRescheduleUnsupported("delta-only reschedule is unsupported")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val updated = taskRepository.getTask("task-1")
        assertNotNull(updated)
        assertEquals(Instant.parse("2026-03-20T09:00:00Z"), updated!!.startTime)
        assertTrue(updated.hasConflict)
        assertEquals("SIM 当前仅支持明确目标 + 明确时间改期", viewModel.conflictWarning.value)
    }

    @Test
    fun `processAudio scheduler drawer voice reschedule delta phrase keeps fire off task unchanged`() = runTest {
        val original = scheduledTask(
            id = "task-fireoff",
            title = "提醒我喝水",
            startTime = Instant.parse("2026-03-20T09:00:00Z"),
            urgencyLevel = UrgencyLevel.FIRE_OFF
        )
        taskRepository.insertTask(original)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.Resolved("task-fireoff")
        scheduleBoard.nextConflictResult = ConflictResult.Conflict(
            overlaps = listOf(
                ScheduleItem(
                    entryId = "task-2",
                    title = "客户会议",
                    scheduledAt = Instant.parse("2026-03-20T10:00:00Z").toEpochMilli(),
                    durationMinutes = 60,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        asrService.nextResult = AsrResult.Success("提醒我喝水推迟1个小时")
        enqueueGlobalRescheduleUnsupported("delta-only reschedule is unsupported")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val updated = taskRepository.getTask("task-fireoff")
        assertNotNull(updated)
        assertEquals(Instant.parse("2026-03-20T09:00:00Z"), updated!!.startTime)
        assertFalse(updated.hasConflict)
        assertEquals("SIM 当前仅支持明确目标 + 明确时间改期", viewModel.conflictWarning.value)
        assertNull(scheduleBoard.lastDurationMinutes)
    }

    @Test
    fun `processAudio scheduler drawer voice reschedule safe fails on no match`() = runTest {
        val original = scheduledTask(
            id = "task-1",
            title = "客户复盘",
            startTime = Instant.parse("2026-03-20T09:00:00Z"),
            urgencyLevel = UrgencyLevel.L2_IMPORTANT
        )
        taskRepository.insertTask(original)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.NoMatch("那个会议")
        asrService.nextResult = AsrResult.Success("把那个会议改到晚上九点")
        enqueueGlobalRescheduleExtraction(
            targetQuery = "那个会议",
            timeInstruction = "晚上九点"
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals("未找到匹配的日程，请更具体一些。", viewModel.conflictWarning.value)
        assertEquals(original.startTime, taskRepository.getTask("task-1")?.startTime)
    }

    @Test
    fun `processAudio scheduler drawer voice reschedule safe fails on ambiguity`() = runTest {
        val original = scheduledTask(
            id = "task-1",
            title = "客户复盘 A",
            startTime = Instant.parse("2026-03-20T09:00:00Z"),
            urgencyLevel = UrgencyLevel.L2_IMPORTANT
        )
        taskRepository.insertTask(original)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.Ambiguous(
            query = "客户复盘",
            candidateIds = listOf("task-1", "task-2")
        )
        asrService.nextResult = AsrResult.Success("把客户复盘改到晚上九点")
        enqueueGlobalRescheduleExtraction(
            targetQuery = "客户复盘",
            timeInstruction = "晚上九点"
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals("目标不明确，未执行改动", viewModel.conflictWarning.value)
        assertEquals(original.startTime, taskRepository.getTask("task-1")?.startTime)
    }

    @Test
    fun `processAudio exact create surfaces conflict state`() = runTest {
        asrService.nextResult = AsrResult.Success("明天下午三点开会")
        scheduleBoard.nextConflictResult = ConflictResult.Conflict(
            overlaps = listOf(
                ScheduleItem(
                    entryId = "other-task",
                    title = "已有会议",
                    scheduledAt = Instant.parse("2026-03-21T07:00:00Z").toEpochMilli(),
                    durationMinutes = 60,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        enqueueExactCreateResponse(startTimeIso = "2026-03-21T07:00:00Z")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertTrue(viewModel.conflictedTaskIds.value.contains("other-task"))
    }

    @Test
    fun `processAudio transport tasks a few minutes apart still surface conflict`() = runTest {
        val board = DerivedConflictScheduleBoard(
            existingItems = listOf(
                ScheduleItem(
                    entryId = "train-task",
                    title = "提醒我去赶高铁",
                    scheduledAt = Instant.parse("2026-03-21T00:12:00Z").toEpochMilli(),
                    durationMinutes = 0,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    urgencyLevel = UrgencyLevel.L1_CRITICAL,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        viewModel = buildViewModel(board)
        asrService.nextResult = AsrResult.Success("凌晨十二点十五提醒我去坐飞机")
        enqueueExactCreateResponse(
            startTimeIso = "2026-03-21T00:15:00Z",
            title = "提醒我去坐飞机",
            durationMinutes = 0,
            urgency = "L1"
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val persisted = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2))
            .snapshot()
            .filterIsInstance<ScheduledTask>()
            .first { it.title == "提醒我去坐飞机" }
        assertTrue(persisted.hasConflict)
        assertEquals("train-task", persisted.conflictWithTaskId)
        assertTrue(viewModel.conflictedTaskIds.value.contains("train-task"))
    }

    @Test
    fun `processAudio fire off reminder remains non conflicting`() = runTest {
        val board = DerivedConflictScheduleBoard(
            existingItems = listOf(
                ScheduleItem(
                    entryId = "meeting-task",
                    title = "客户会议",
                    scheduledAt = Instant.parse("2026-03-21T00:12:00Z").toEpochMilli(),
                    durationMinutes = 60,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        viewModel = buildViewModel(board)
        asrService.nextResult = AsrResult.Success("凌晨十二点十五提醒我喝水")
        enqueueExactCreateResponse(
            startTimeIso = "2026-03-21T00:15:00Z",
            title = "提醒我喝水",
            durationMinutes = 0,
            urgency = "FIRE_OFF"
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val persisted = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2))
            .snapshot()
            .filterIsInstance<ScheduledTask>()
            .first { it.title == "提醒我喝水" }
        assertFalse(persisted.hasConflict)
        assertTrue(viewModel.conflictedTaskIds.value.isEmpty())
    }

    @Test
    fun `processAudio off page exact create marks normal date attention`() = runTest {
        asrService.nextResult = AsrResult.Success("明天下午三点开会")
        enqueueExactCreateResponse(startTimeIso = "2026-03-21T07:00:00Z")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals(setOf(1), viewModel.unacknowledgedDates.value)
        assertTrue(viewModel.rescheduledDates.value.isEmpty())
    }

    @Test
    fun `processAudio off page vague create marks normal date attention`() = runTest {
        asrService.nextResult = AsrResult.Success("明天提醒我开会")
        enqueueNotMultiResponse()
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_EXACT",
                  "reason": "缺少明确时间"
                }
                """.trimIndent()
            )
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "VAGUE_CREATE",
                  "task": {
                    "title": "提醒我开会",
                    "anchorDateIso": "2026-03-21",
                    "timeHint": "下午",
                    "urgency": "L3"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals(setOf(1), viewModel.unacknowledgedDates.value)
        assertTrue(viewModel.rescheduledDates.value.isEmpty())
    }

    @Test
    fun `processAudio off page conflict create marks warning date attention`() = runTest {
        asrService.nextResult = AsrResult.Success("明天下午三点开会")
        scheduleBoard.nextConflictResult = ConflictResult.Conflict(
            overlaps = listOf(
                ScheduleItem(
                    entryId = "other-task",
                    title = "已有会议",
                    scheduledAt = Instant.parse("2026-03-21T07:00:00Z").toEpochMilli(),
                    durationMinutes = 60,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        enqueueExactCreateResponse(startTimeIso = "2026-03-21T07:00:00Z")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertEquals(setOf(1), viewModel.unacknowledgedDates.value)
        assertEquals(setOf(1), viewModel.rescheduledDates.value)
    }

    @Test
    fun `processAudio same page create does not mark date attention`() = runTest {
        viewModel.onDateSelected(1)
        asrService.nextResult = AsrResult.Success("明天下午三点开会")
        enqueueExactCreateResponse(startTimeIso = "2026-03-21T07:00:00Z")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        assertTrue(viewModel.unacknowledgedDates.value.isEmpty())
        assertTrue(viewModel.rescheduledDates.value.isEmpty())
    }

    @Test
    fun `processAudio multi task create marks dates independently`() = runTest {
        val sequenceBoard = SequenceScheduleBoard(
            listOf(
                ConflictResult.Clear,
                ConflictResult.Conflict(
                    overlaps = listOf(
                        ScheduleItem(
                            entryId = "other-task",
                            title = "已有会议",
                            scheduledAt = Instant.parse("2026-03-22T07:00:00Z").toEpochMilli(),
                            durationMinutes = 60,
                            durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                            conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                        )
                    )
                )
            )
        )
        viewModel = buildViewModel(sequenceBoard)
        viewModel.applyFastTrackResultForTesting(
            FastTrackResult.CreateTasks(
                CreateTasksParams(
                    tasks = listOf(
                        TaskDefinition(
                            title = "客户会A",
                            startTimeIso = "2026-03-21T07:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        ),
                        TaskDefinition(
                            title = "客户会B",
                            startTimeIso = "2026-03-22T07:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        assertEquals(setOf(1, 2), viewModel.unacknowledgedDates.value)
        assertEquals(setOf(2), viewModel.rescheduledDates.value)
    }

    @Test
    fun `processAudio multi task same date becomes warning when any task conflicts`() = runTest {
        val sequenceBoard = SequenceScheduleBoard(
            listOf(
                ConflictResult.Clear,
                ConflictResult.Conflict(
                    overlaps = listOf(
                        ScheduleItem(
                            entryId = "other-task",
                            title = "已有会议",
                            scheduledAt = Instant.parse("2026-03-21T08:00:00Z").toEpochMilli(),
                            durationMinutes = 60,
                            durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                            conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                        )
                    )
                )
            )
        )
        viewModel = buildViewModel(sequenceBoard)
        viewModel.applyFastTrackResultForTesting(
            FastTrackResult.CreateTasks(
                CreateTasksParams(
                    tasks = listOf(
                        TaskDefinition(
                            title = "客户会A",
                            startTimeIso = "2026-03-21T07:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        ),
                        TaskDefinition(
                            title = "客户会B",
                            startTimeIso = "2026-03-21T08:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        assertEquals(setOf(1), viewModel.unacknowledgedDates.value)
        assertEquals(setOf(1), viewModel.rescheduledDates.value)
    }

    @Test
    fun `onDateSelected acknowledges create attention`() = runTest {
        asrService.nextResult = AsrResult.Success("明天下午三点开会")
        enqueueExactCreateResponse(startTimeIso = "2026-03-21T07:00:00Z")

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()
        viewModel.onDateSelected(1)

        assertTrue(viewModel.unacknowledgedDates.value.isEmpty())
        assertTrue(viewModel.rescheduledDates.value.isEmpty())
    }

    @Test
    fun `processAudio multi task create via Uni-M creates independent tasks and attention`() = runTest {
        val sequenceBoard = SequenceScheduleBoard(
            listOf(
                ConflictResult.Clear,
                ConflictResult.Conflict(
                    overlaps = listOf(
                        ScheduleItem(
                            entryId = "other-task",
                            title = "已有会议",
                            scheduledAt = Instant.parse("2026-03-22T00:00:00Z").toEpochMilli(),
                            durationMinutes = 60,
                            durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                            conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                        )
                    )
                )
            )
        )
        viewModel = buildViewModel(sequenceBoard)
        asrService.nextResult = AsrResult.Success("今晚八点吃饭，明天凌晨三点起床，四小时后赶飞机")
        enqueueMultiCreateResponse(
            """
            {
              "decision": "MULTI_CREATE",
              "fragments": [
                {
                  "title": "吃饭",
                  "mode": "EXACT",
                  "anchorKind": "ABSOLUTE",
                  "startTimeIso": "2026-03-20T12:00:00Z",
                  "durationMinutes": 60,
                  "urgency": "L2"
                },
                {
                  "title": "起床",
                  "mode": "EXACT",
                  "anchorKind": "PREVIOUS_DAY_OFFSET",
                  "relativeDayOffset": 1,
                  "clockTime": "03:00",
                  "durationMinutes": 0,
                  "urgency": "L1"
                },
                {
                  "title": "赶飞机",
                  "mode": "EXACT",
                  "anchorKind": "PREVIOUS_EXACT_OFFSET",
                  "relativeOffsetMinutes": 240,
                  "durationMinutes": 0,
                  "urgency": "L1"
                }
              ]
            }
            """.trimIndent()
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(3)).snapshot()
            .filterIsInstance<ScheduledTask>()
        assertEquals(3, tasks.size)
        assertEquals(setOf(1), viewModel.unacknowledgedDates.value)
        assertEquals(setOf(1), viewModel.rescheduledDates.value)
        assertEquals("已创建 3 个日程", viewModel.pipelineStatus.value)
    }

    @Test
    fun `processAudio multi task clock relative after vague predecessor downgrades to vague`() = runTest {
        asrService.nextResult = AsrResult.Success("明天开会，三小时后赶飞机")
        enqueueMultiCreateResponse(
            """
            {
              "decision": "MULTI_CREATE",
              "fragments": [
                {
                  "title": "开会",
                  "mode": "VAGUE",
                  "anchorKind": "ABSOLUTE",
                  "anchorDateIso": "2026-03-21",
                  "timeHint": "明天",
                  "urgency": "L2"
                },
                {
                  "title": "赶飞机",
                  "mode": "EXACT",
                  "anchorKind": "PREVIOUS_EXACT_OFFSET",
                  "relativeOffsetMinutes": 180,
                  "timeHint": "三小时后",
                  "urgency": "L1"
                }
              ]
            }
            """.trimIndent()
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot()
            .filterIsInstance<ScheduledTask>()
        assertEquals(2, tasks.size)
        assertTrue(tasks.any { it.title == "赶飞机" && it.isVague })
        assertEquals("已创建 2 个日程，1 个片段已按待定处理", viewModel.pipelineStatus.value)
    }

    @Test
    fun `processAudio multi task explicit after variant after vague predecessor still downgrades to vague`() = runTest {
        asrService.nextResult = AsrResult.Success("明天开会，三小时之后赶飞机")
        enqueueMultiCreateResponse(
            """
            {
              "decision": "MULTI_CREATE",
              "fragments": [
                {
                  "title": "开会",
                  "mode": "VAGUE",
                  "anchorKind": "ABSOLUTE",
                  "anchorDateIso": "2026-03-21",
                  "timeHint": "明天",
                  "urgency": "L2"
                },
                {
                  "title": "赶飞机",
                  "mode": "EXACT",
                  "anchorKind": "PREVIOUS_EXACT_OFFSET",
                  "relativeOffsetMinutes": 180,
                  "timeHint": "三小时之后",
                  "urgency": "L1"
                }
              ]
            }
            """.trimIndent()
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot()
            .filterIsInstance<ScheduledTask>()
        assertEquals(2, tasks.size)
        assertTrue(tasks.any { it.title == "赶飞机" && it.isVague })
        assertTrue(fakeExecutor.executedPrompts.any { it.contains("规范化输入") && it.contains("三小时后赶飞机") })
        assertEquals("已创建 2 个日程，1 个片段已按待定处理", viewModel.pipelineStatus.value)
    }

    @Test
    fun `processAudio multi task now anchored fragments resolve from current moment and next day`() = runTest {
        asrService.nextResult = AsrResult.Success("三小时后开会，明天下午三点汇报")
        enqueueMultiCreateResponse(
            """
            {
              "decision": "MULTI_CREATE",
              "fragments": [
                {
                  "title": "开会",
                  "mode": "EXACT",
                  "anchorKind": "NOW_OFFSET",
                  "relativeOffsetMinutes": 180,
                  "durationMinutes": 60,
                  "urgency": "L2"
                },
                {
                  "title": "汇报",
                  "mode": "EXACT",
                  "anchorKind": "NOW_DAY_OFFSET",
                  "relativeDayOffset": 1,
                  "clockTime": "15:00",
                  "durationMinutes": 60,
                  "urgency": "L2"
                }
              ]
            }
            """.trimIndent()
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot()
            .filterIsInstance<ScheduledTask>()
        assertEquals(2, tasks.size)
        assertTrue(tasks.any { it.title == "开会" && it.startTime == Instant.parse("2026-03-20T12:00:00Z") })
        assertTrue(tasks.any { it.title == "汇报" && it.startTime == Instant.parse("2026-03-21T07:00:00Z") })
        assertEquals("已创建 2 个日程", viewModel.pipelineStatus.value)
    }

    @Test
    fun `processAudio multi task partial success keeps later lawful tasks`() = runTest {
        asrService.nextResult = AsrResult.Success("然后赶飞机，明天下午三点开会")
        enqueueMultiCreateResponse(
            """
            {
              "decision": "MULTI_CREATE",
              "fragments": [
                {
                  "title": "赶飞机",
                  "mode": "EXACT",
                  "anchorKind": "PREVIOUS_EXACT_OFFSET",
                  "relativeOffsetMinutes": 240,
                  "urgency": "L1"
                },
                {
                  "title": "开会",
                  "mode": "EXACT",
                  "anchorKind": "NOW_DAY_OFFSET",
                  "relativeDayOffset": 1,
                  "clockTime": "15:00",
                  "durationMinutes": 60,
                  "urgency": "L2"
                }
              ]
            }
            """.trimIndent()
        )

        viewModel.processAudio(File("dummy.wav"))
        advanceUntilIdle()

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot()
            .filterIsInstance<ScheduledTask>()
        assertEquals(1, tasks.size)
        assertEquals("开会", tasks.single().title)
        assertEquals(Instant.parse("2026-03-21T07:00:00Z"), tasks.single().startTime)
        assertEquals("已创建 1 个日程，1 个片段未创建", viewModel.pipelineStatus.value)
    }

    @Test
    fun `applyFastTrack exact create schedules native reminder cascade`() = runTest {
        viewModel.applyFastTrackResultForTesting(
            FastTrackResult.CreateTasks(
                CreateTasksParams(
                    unifiedId = "task-exact",
                    tasks = listOf(
                        TaskDefinition(
                            title = "客户会议",
                            startTimeIso = "2026-03-21T07:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        val alarms = alarmScheduler.getAlarmsForTask("task-exact")
        val persisted = taskRepository.getTask("task-exact")
        assertNotNull(persisted)
        assertTrue(persisted!!.hasAlarm)
        assertEquals(listOf("-30m", "0m"), persisted.alarmCascade)
        assertEquals(UrgencyLevel.buildCascade(UrgencyLevel.L2_IMPORTANT).size, alarms.size)
        assertEquals(listOf(30, 0), alarms.map { it.offsetMinutes })
    }

    @Test
    fun `applyFastTrack conflict exact create still schedules native reminder cascade`() = runTest {
        val sequenceBoard = SequenceScheduleBoard(
            listOf(
                ConflictResult.Conflict(
                    overlaps = listOf(
                        ScheduleItem(
                            entryId = "other-task",
                            title = "已有会议",
                            scheduledAt = Instant.parse("2026-03-21T07:00:00Z").toEpochMilli(),
                            durationMinutes = 60,
                            durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                            conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                        )
                    )
                )
            )
        )
        viewModel = buildViewModel(sequenceBoard)

        viewModel.applyFastTrackResultForTesting(
            FastTrackResult.CreateTasks(
                CreateTasksParams(
                    unifiedId = "task-conflict",
                    tasks = listOf(
                        TaskDefinition(
                            title = "客户会议",
                            startTimeIso = "2026-03-21T07:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        val task = taskRepository.getTask("task-conflict")
        assertNotNull(task)
        assertTrue(task!!.hasConflict)
        assertEquals(listOf("-30m", "0m"), task.alarmCascade)
        assertEquals(UrgencyLevel.buildCascade(UrgencyLevel.L2_IMPORTANT).size, alarmScheduler.getAlarmsForTask("task-conflict").size)
    }

    @Test
    fun `applyFastTrack vague create emits prompt but does not schedule native reminders`() = runTest {
        exactAlarmPermissionGate.needsPrompt = true

        val promptCount = collectReminderPromptCount {
            viewModel.applyFastTrackResultForTesting(
                FastTrackResult.CreateVagueTask(
                    CreateVagueTaskParams(
                        unifiedId = "task-vague",
                        title = "提醒客户",
                        anchorDateIso = "2026-03-21",
                        timeHint = "下午",
                        urgency = UrgencyEnum.L3_NORMAL
                    )
                )
            )
            advanceUntilIdle()
        }

        assertTrue(alarmScheduler.scheduledAlarms.isEmpty())
        assertEquals(1, promptCount)
    }

    @Test
    fun `processAudio Uni-M mixed exact and vague fragments schedule only exact reminders`() = runTest {
        exactAlarmPermissionGate.needsPrompt = true
        asrService.nextResult = AsrResult.Success("明天下午三点开会，明天提醒我回电话")
        enqueueMultiCreateResponse(
            """
            {
              "decision": "MULTI_CREATE",
              "fragments": [
                {
                  "title": "开会",
                  "mode": "EXACT",
                  "anchorKind": "NOW_DAY_OFFSET",
                  "relativeDayOffset": 1,
                  "clockTime": "15:00",
                  "durationMinutes": 60,
                  "urgency": "L2"
                },
                {
                  "title": "回电话",
                  "mode": "VAGUE",
                  "anchorKind": "ABSOLUTE",
                  "anchorDateIso": "2026-03-21",
                  "timeHint": "下午",
                  "urgency": "L3"
                }
              ]
            }
            """.trimIndent()
        )

        val promptCount = collectReminderPromptCount {
            viewModel.processAudio(File("dummy.wav"))
            advanceUntilIdle()
        }

        val tasks = taskRepository.queryByDateRange(timeProvider.today, timeProvider.today.plusDays(2)).snapshot()
            .filterIsInstance<ScheduledTask>()
        val exactTask = tasks.single { !it.isVague }
        val vagueTask = tasks.single { it.isVague }

        assertEquals(2, tasks.size)
        assertEquals(UrgencyLevel.buildCascade(UrgencyLevel.L2_IMPORTANT).size, alarmScheduler.getAlarmsForTask(exactTask.id).size)
        assertTrue(alarmScheduler.getAlarmsForTask(vagueTask.id).isEmpty())
        assertEquals(1, promptCount)
    }

    @Test
    fun `deleteItem cancels exact task reminders`() = runTest {
        viewModel.applyFastTrackResultForTesting(
            FastTrackResult.CreateTasks(
                CreateTasksParams(
                    unifiedId = "task-delete",
                    tasks = listOf(
                        TaskDefinition(
                            title = "客户会议",
                            startTimeIso = "2026-03-21T07:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.deleteItem("task-delete")
        advanceUntilIdle()

        assertTrue(alarmScheduler.cancelledTaskIds.contains("task-delete"))
        assertTrue(alarmScheduler.getAlarmsForTask("task-delete").isEmpty())
    }

    @Test
    fun `toggleDone cancels exact reminders and restore does not reschedule`() = runTest {
        viewModel.applyFastTrackResultForTesting(
            FastTrackResult.CreateTasks(
                CreateTasksParams(
                    unifiedId = "task-toggle",
                    tasks = listOf(
                        TaskDefinition(
                            title = "客户会议",
                            startTimeIso = "2026-03-21T07:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        )
                    )
                )
            )
        )
        advanceUntilIdle()

        viewModel.toggleDone("task-toggle")
        advanceUntilIdle()
        viewModel.toggleDone("task-toggle")
        advanceUntilIdle()

        assertEquals(1, alarmScheduler.cancelledTaskIds.count { it == "task-toggle" })
        assertTrue(alarmScheduler.getAlarmsForTask("task-toggle").isEmpty())
    }

    @Test
    fun `onReschedule cancels old reminder and schedules new cascade`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task-reschedule",
                timeDisplay = "10:00",
                title = "客户会议",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-20T02:00:00Z"),
                durationMinutes = 60
            )
        )
        alarmScheduler.scheduleCascade(
            taskId = taskId,
            taskTitle = "客户会议",
            eventTime = Instant.parse("2026-03-20T02:00:00Z"),
            cascade = UrgencyLevel.buildCascade(UrgencyLevel.L2_IMPORTANT)
        )
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "客户会议",
                    "startTimeIso": "2026-03-20T08:30:00Z",
                    "durationMinutes": 45,
                    "urgency": "L2"
                  }
                }
                """.trimIndent()
            )
        )

        viewModel.onReschedule(taskId, "改到今天下午四点半")
        advanceUntilIdle()

        val updated = taskRepository.getTask(taskId)
        assertNotNull(updated)
        val updatedTask = updated!!
        assertTrue(alarmScheduler.cancelledTaskIds.contains(taskId))
        assertTrue(updatedTask.hasAlarm)
        assertEquals(listOf("-30m", "0m"), updatedTask.alarmCascade)
        assertEquals(UrgencyLevel.buildCascade(UrgencyLevel.L2_IMPORTANT).size, alarmScheduler.getAlarmsForTask(taskId).size)
        assertTrue(alarmScheduler.getAlarmsForTask(taskId).all { it.triggerAt <= updatedTask.startTime })
        assertTrue(alarmScheduler.getAlarmsForTask(taskId).any { it.triggerAt == updatedTask.startTime })
    }

    @Test
    fun `exact alarm permission prompt emits only once per process while scheduling continues`() = runTest {
        exactAlarmPermissionGate.needsPrompt = true
        val promptEvents = mutableListOf<Unit>()
        val collector = launch {
            viewModel.exactAlarmPermissionNeeded.collect { promptEvents += Unit }
        }

        viewModel.applyFastTrackResultForTesting(
            FastTrackResult.CreateTasks(
                CreateTasksParams(
                    tasks = listOf(
                        TaskDefinition(
                            title = "客户会议",
                            startTimeIso = "2026-03-21T07:00:00Z",
                            durationMinutes = 60,
                            urgency = UrgencyEnum.L2_IMPORTANT
                        ),
                        TaskDefinition(
                            title = "客户回访",
                            startTimeIso = "2026-03-21T09:00:00Z",
                            durationMinutes = 30,
                            urgency = UrgencyEnum.L3_NORMAL
                        )
                    )
                )
            )
        )
        advanceUntilIdle()
        collector.cancel()

        assertEquals(1, promptEvents.size)
        assertEquals(
            UrgencyLevel.buildCascade(UrgencyLevel.L2_IMPORTANT).size +
                UrgencyLevel.buildCascade(UrgencyLevel.L3_NORMAL).size,
            alarmScheduler.scheduledAlarms.size
        )
    }

    private fun scheduledTask(
        id: String,
        title: String,
        startTime: Instant,
        urgencyLevel: UrgencyLevel,
        isDone: Boolean = false,
        hasConflict: Boolean = false
    ): ScheduledTask {
        return ScheduledTask(
            id = id,
            timeDisplay = "10:00",
            title = title,
            urgencyLevel = urgencyLevel,
            startTime = startTime,
            durationMinutes = 30,
            isDone = isDone,
            hasConflict = hasConflict
        )
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<List<T>>.snapshot(): List<T> = first()

    private suspend fun kotlinx.coroutines.CoroutineScope.collectReminderPromptCount(
        block: suspend () -> Unit
    ): Int {
        val promptEvents = mutableListOf<Unit>()
        val collector = launch {
            viewModel.exactAlarmPermissionNeeded.collect { promptEvents += Unit }
        }
        block()
        collector.cancel()
        return promptEvents.size
    }

    private fun enqueueNotMultiResponse(reason: String = "单任务") {
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_MULTI",
                  "reason": "$reason"
                }
                """.trimIndent()
            )
        )
    }

    private fun enqueueMultiCreateResponse(payload: String) {
        fakeExecutor.enqueueResponse(ExecutorResult.Success(payload))
    }

    private fun enqueueGlobalRescheduleExtraction(
        targetQuery: String,
        timeInstruction: String,
        targetPerson: String? = null,
        targetLocation: String? = null
    ) {
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "RESCHEDULE_TARGETED",
                  "targetQuery": "$targetQuery",
                  "targetPerson": ${targetPerson?.let { "\"$it\"" } ?: "null"},
                  "targetLocation": ${targetLocation?.let { "\"$it\"" } ?: "null"},
                  "timeInstruction": "$timeInstruction"
                }
                """.trimIndent()
            )
        )
    }

    private fun enqueueGlobalRescheduleUnsupported(reason: String) {
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "NOT_SUPPORTED",
                  "reason": "$reason"
                }
                """.trimIndent()
            )
        )
    }

    private fun enqueueExactCreateResponse(
        startTimeIso: String,
        title: String = "开会",
        durationMinutes: Int = 60,
        urgency: String = "L2"
    ) {
        enqueueNotMultiResponse()
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "EXACT_CREATE",
                  "task": {
                    "title": "$title",
                    "startTimeIso": "$startTimeIso",
                    "durationMinutes": $durationMinutes,
                    "urgency": "$urgency"
                  }
                }
                """.trimIndent()
            )
        )
    }

    private class SequenceScheduleBoard(
        results: List<ConflictResult>
    ) : ScheduleBoard {
        private val queue = ArrayDeque(results)
        private val _upcomingItems = kotlinx.coroutines.flow.MutableStateFlow<List<ScheduleItem>>(emptyList())

        override val upcomingItems: kotlinx.coroutines.flow.StateFlow<List<ScheduleItem>> = _upcomingItems

        override suspend fun checkConflict(
            proposedStart: Long,
            durationMinutes: Int,
            excludeId: String?
        ): ConflictResult {
            return queue.removeFirstOrNull() ?: ConflictResult.Clear
        }

        override suspend fun refresh() = Unit

        override suspend fun findLexicalMatch(targetQuery: String): ScheduleItem? = null

        override suspend fun resolveTarget(request: TargetResolutionRequest): TargetResolution {
            return TargetResolution.NoMatch(request.describeForFailure())
        }
    }

    private class DerivedConflictScheduleBoard(
        existingItems: List<ScheduleItem>
    ) : ScheduleBoard {
        private val _upcomingItems = kotlinx.coroutines.flow.MutableStateFlow(existingItems)
        override val upcomingItems: kotlinx.coroutines.flow.StateFlow<List<ScheduleItem>> = _upcomingItems

        override suspend fun checkConflict(
            proposedStart: Long,
            durationMinutes: Int,
            excludeId: String?
        ): ConflictResult {
            val overlaps = _upcomingItems.value.filter { item ->
                item.entryId != excludeId &&
                    !bypassesConflictEvaluation(item.urgencyLevel) &&
                    !item.isVague &&
                    item.conflictPolicy == com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE &&
                    overlapsInScheduleBoard(
                        proposedStart = proposedStart,
                        proposedDurationMinutes = durationMinutes,
                        existingStart = item.scheduledAt,
                        existingDurationMinutes = item.effectiveConflictDurationMinutes
                    )
            }
            return if (overlaps.isEmpty()) ConflictResult.Clear else ConflictResult.Conflict(overlaps)
        }

        override suspend fun refresh() = Unit

        override suspend fun findLexicalMatch(targetQuery: String): ScheduleItem? = null

        override suspend fun resolveTarget(request: TargetResolutionRequest): TargetResolution {
            return TargetResolution.NoMatch(request.describeForFailure())
        }
    }

    private class StubAsrService : AsrService {
        var nextResult: AsrResult = AsrResult.Success("")

        override suspend fun transcribe(file: File): AsrResult = nextResult

        override suspend fun isAvailable(): Boolean = true
    }

    private class FakeExactAlarmPermissionGate : ExactAlarmPermissionGate {
        var needsPrompt: Boolean = false
        private var prompted = false

        override fun shouldPromptForExactAlarm(): Boolean {
            if (!needsPrompt) return false
            if (prompted) return false
            prompted = true
            return true
        }
    }
}
