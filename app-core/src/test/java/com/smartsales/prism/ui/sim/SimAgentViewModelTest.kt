package com.smartsales.prism.ui.sim

import android.content.Context
import com.smartsales.core.pipeline.RealFollowUpRescheduleExtractionService
import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
import com.smartsales.core.pipeline.PromptCompiler
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.test.fakes.FakeActiveTaskRetrievalIndex
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.core.test.fakes.FakeUserProfileRepository
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.data.audio.DeviceSpeechFailureReason
import com.smartsales.prism.data.audio.DeviceSpeechMode
import com.smartsales.prism.data.audio.DeviceSpeechRecognitionResult
import com.smartsales.prism.data.audio.DeviceSpeechRecognizer
import com.smartsales.prism.data.audio.SIM_AUDIO_METADATA_FILENAME
import com.smartsales.prism.data.audio.SimAudioRepository
import com.smartsales.prism.data.audio.simArtifactFilename
import com.smartsales.prism.data.session.SimSessionRepository
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SchedulerFollowUpTaskSummary
import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuQuestionAnswer
import com.smartsales.prism.domain.tingwu.TingwuSpeakerSummary
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.domain.tingwu.TingwuSmartSummary
import java.time.Instant
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.smartsales.core.test.fakes.FakeAlarmScheduler

@OptIn(ExperimentalCoroutinesApi::class)
class SimAgentViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionRepository: SimSessionRepository
    private lateinit var taskRepository: FakeScheduledTaskRepository
    private lateinit var scheduleBoard: FakeScheduleBoard
    private lateinit var activeTaskRetrievalIndex: FakeActiveTaskRetrievalIndex
    private lateinit var alarmScheduler: FakeAlarmScheduler
    private lateinit var fakeExecutor: FakeExecutor
    private lateinit var userProfileRepository: FakeUserProfileRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var speechRecognizer: FakeDeviceSpeechRecognizer
    private lateinit var uniAExtractionService: RealUniAExtractionService
    private lateinit var globalRescheduleExtractionService: RealGlobalRescheduleExtractionService
    private lateinit var followUpRescheduleExtractionService: RealFollowUpRescheduleExtractionService

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = SimSessionRepository(tempFolder.root)
        taskRepository = FakeScheduledTaskRepository()
        scheduleBoard = FakeScheduleBoard()
        activeTaskRetrievalIndex = FakeActiveTaskRetrievalIndex()
        alarmScheduler = FakeAlarmScheduler()
        fakeExecutor = FakeExecutor()
        userProfileRepository = FakeUserProfileRepository()
        speechRecognizer = FakeDeviceSpeechRecognizer()
        timeProvider = FakeTimeProvider().apply {
            fixedInstant = Instant.parse("2026-03-22T08:00:00Z")
        }
        val schedulerLinter = SchedulerLinter(timeProvider)
        uniAExtractionService = RealUniAExtractionService(
            fakeExecutor,
            PromptCompiler(),
            schedulerLinter
        )
        globalRescheduleExtractionService = RealGlobalRescheduleExtractionService(
            fakeExecutor,
            PromptCompiler(),
            schedulerLinter
        )
        followUpRescheduleExtractionService = RealFollowUpRescheduleExtractionService(
            fakeExecutor,
            PromptCompiler(),
            schedulerLinter
        )
        SimFollowUpRescheduleShadowMetrics.resetForTest()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        PipelineValve.testInterceptor = null
        SimFollowUpRescheduleShadowMetrics.resetForTest()
    }

    @Test
    fun `cold start with empty sim store shows no seeded sessions`() {
        val viewModel = newViewModel()

        assertTrue(viewModel.groupedSessions.value.isEmpty())
        assertNull(viewModel.currentSessionId.value)
        assertTrue(viewModel.history.value.isEmpty())
    }

    @Test
    fun `hero greeting uses current profile display name`() = runTest {
        val viewModel = newViewModel()

        assertEquals("你好, Default User", viewModel.heroGreeting.value)
    }

    @Test
    fun `hero greeting reacts to profile display name updates and falls back when blank`() = runTest {
        val viewModel = newViewModel()

        userProfileRepository.updateProfile(
            userProfileRepository.profile.value.copy(displayName = "孙扬浩")
        )
        advanceUntilIdle()
        assertEquals("你好, 孙扬浩", viewModel.heroGreeting.value)

        userProfileRepository.updateProfile(
            userProfileRepository.profile.value.copy(displayName = "   ")
        )
        advanceUntilIdle()
        assertEquals("你好, SmartSales 用户", viewModel.heroGreeting.value)
    }

    @Test
    fun `voice draft success populates input without auto send`() = runTest {
        val viewModel = newViewModel()
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("帮我约周四下午两点")

        assertTrue(viewModel.startVoiceDraft())
        viewModel.finishVoiceDraft()
        advanceUntilIdle()

        assertEquals("帮我约周四下午两点", viewModel.inputText.value)
        assertFalse(viewModel.isSending.value)
        assertTrue(viewModel.history.value.isEmpty())
        assertFalse(viewModel.voiceDraftState.value.isRecording)
        assertFalse(viewModel.voiceDraftState.value.isProcessing)
    }

    @Test
    fun `voice draft permission grant auto starts tap to finish session`() {
        val viewModel = newViewModel()

        viewModel.onVoiceDraftPermissionRequested()
        assertTrue(viewModel.voiceDraftState.value.awaitingMicPermission)

        viewModel.onVoiceDraftPermissionResult(granted = true)

        val state = viewModel.voiceDraftState.value
        assertTrue(state.isRecording)
        assertFalse(state.awaitingMicPermission)
        assertEquals(SimVoiceDraftInteractionMode.TAP_TO_SEND, state.interactionMode)
        assertTrue(speechRecognizer.isListening())
    }

    @Test
    fun `voice draft starts recognizer in local asr fallback mode`() {
        val viewModel = newViewModel()

        assertTrue(viewModel.startVoiceDraft())

        assertEquals(
            DeviceSpeechMode.DEVICE_WITH_LOCAL_ASR_FALLBACK,
            speechRecognizer.lastMode
        )
    }

    @Test
    fun `voice draft no match resets without mutating history`() = runTest {
        val viewModel = newViewModel()
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Failure(
            reason = DeviceSpeechFailureReason.NO_MATCH,
            message = "没有识别到清晰语音"
        )

        assertTrue(viewModel.startVoiceDraft())
        viewModel.finishVoiceDraft()
        advanceUntilIdle()

        assertEquals("", viewModel.inputText.value)
        assertTrue(viewModel.history.value.isEmpty())
        assertFalse(viewModel.voiceDraftState.value.isRecording)
        assertFalse(viewModel.voiceDraftState.value.isProcessing)
        assertEquals("没有识别到清晰语音", viewModel.toastMessage.value)
    }

    @Test
    fun `cancel voice draft blocks late recognizer result from writing input`() = runTest {
        val viewModel = newViewModel()
        speechRecognizer.nextResult = DeviceSpeechRecognitionResult.Success("稍后到达")
        speechRecognizer.finishDelayMillis = 500L

        assertTrue(viewModel.startVoiceDraft())
        viewModel.finishVoiceDraft()
        viewModel.cancelVoiceDraft()
        advanceUntilIdle()

        assertEquals("", viewModel.inputText.value)
        assertFalse(viewModel.voiceDraftState.value.isRecording)
        assertFalse(viewModel.voiceDraftState.value.isProcessing)
    }

    @Test
    fun `selectAudioForChat pending binds immediately and shows thinking state`() {
        val viewModel = newViewModel()

        viewModel.selectAudioForChat(
            audioId = "audio_pending_1",
            title = "待转写录音",
            summary = null,
            entersPendingFlow = true
        )

        assertEquals("audio_pending_1", viewModel.currentLinkedAudioId.value)
        assertEquals("待转写录音", viewModel.sessionTitle.value)
        assertTrue(viewModel.uiState.value is UiState.Thinking)
        assertEquals(1, viewModel.history.value.size)

        val firstMessage = viewModel.history.value.single() as ChatMessage.Ai
        val response = firstMessage.uiState as UiState.Response
        assertTrue(response.content.contains("正在自动提交 Tingwu 转写任务"))
        assertTrue(response.content.contains("当前状态"))
        assertFalse(response.content.contains("点击开始转写"))
    }

    @Test
    fun `completePendingAudio appends ready response and clears active thinking`() {
        val viewModel = newViewModel()

        viewModel.selectAudioForChat(
            audioId = "audio_pending_2",
            title = "会议录音",
            summary = null,
            entersPendingFlow = true
        )

        viewModel.completePendingAudio("audio_pending_2")

        assertEquals(UiState.Idle, viewModel.uiState.value)
        assertEquals(2, viewModel.history.value.size)

        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        val response = lastMessage.uiState as UiState.Response
        assertTrue(response.content.contains("转写已完成"))
        assertTrue(response.content.contains("会议录音"))
    }

    @Test
    fun `appendCompletedAudioArtifacts writes durable artifact turn once`() {
        val viewModel = newViewModel()

        viewModel.selectAudioForChat(
            audioId = "audio_done_1",
            title = "客户录音",
            summary = "已有摘要",
            entersPendingFlow = false
        )

        val artifacts = TingwuJobArtifacts(
            transcriptMarkdown = "完整转写",
            smartSummary = TingwuSmartSummary(summary = "结构化摘要")
        )

        viewModel.appendCompletedAudioArtifacts("audio_done_1", artifacts)
        viewModel.appendCompletedAudioArtifacts("audio_done_1", artifacts)

        assertEquals(UiState.Idle, viewModel.uiState.value)
        assertEquals(2, viewModel.history.value.size)

        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        val state = lastMessage.uiState as UiState.AudioArtifacts
        assertEquals("audio_done_1", state.audioId)
        assertEquals("客户录音", state.title)
        assertTrue(state.artifactsJson.contains("完整转写"))
    }

    @Test
    fun `artifact transcript reveal state is keyed by message id and clears on session delete`() {
        val viewModel = newViewModel()

        val sessionId = viewModel.selectAudioForChat(
            audioId = "audio_done_2",
            title = "长转写录音",
            summary = "已有摘要",
            entersPendingFlow = false
        )

        viewModel.appendCompletedAudioArtifacts(
            "audio_done_2",
            TingwuJobArtifacts(transcriptMarkdown = "很长的转写内容")
        )

        val artifactMessage = viewModel.history.value.last() as ChatMessage.Ai
        viewModel.markArtifactTranscriptRevealConsumed(artifactMessage.id, isLongTranscript = true)

        assertEquals(
            SimAgentViewModel.ArtifactTranscriptRevealState(
                consumed = true,
                isLongTranscript = true
            ),
            viewModel.artifactTranscriptRevealState.value[artifactMessage.id]
        )

        viewModel.deleteSession(sessionId)

        assertEquals(null, viewModel.artifactTranscriptRevealState.value[artifactMessage.id])
    }

    @Test
    fun `startSeededSession creates fresh session with auto submitted first turn`() = runTest {
        val viewModel = newViewModel()
        val initialSessionCount = viewModel.groupedSessions.value.values.flatten().size

        viewModel.startSeededSession("i want to learn guitar")

        assertTrue(viewModel.isSending.value)
        assertEquals(1, viewModel.history.value.filterIsInstance<ChatMessage.User>().size)
        assertEquals(
            "i want to learn guitar",
            (viewModel.history.value.first() as ChatMessage.User).content
        )

        advanceUntilIdle()

        assertEquals(2, viewModel.history.value.size)
        assertTrue(viewModel.history.value.last() is ChatMessage.Ai)
        assertEquals(initialSessionCount + 1, viewModel.groupedSessions.value.values.flatten().size)
    }

    @Test
    fun `startSeededSession starts another fresh session instead of appending old history`() = runTest {
        val viewModel = newViewModel()
        val initialSessionCount = viewModel.groupedSessions.value.values.flatten().size

        viewModel.startSeededSession("i want to learn guitar")
        advanceUntilIdle()
        val firstHistory = viewModel.history.value

        viewModel.startSeededSession("learn piano")

        assertEquals(1, viewModel.history.value.filterIsInstance<ChatMessage.User>().size)
        assertEquals("learn piano", (viewModel.history.value.first() as ChatMessage.User).content)
        assertTrue(firstHistory != viewModel.history.value)

        advanceUntilIdle()

        assertEquals(2, viewModel.history.value.size)
        assertEquals(initialSessionCount + 2, viewModel.groupedSessions.value.values.flatten().size)
    }

    @Test
    fun `general send uses persona backed reply instead of audio only guidance`() = runTest {
        val viewModel = newViewModel()
        fakeExecutor.enqueueResponse(ExecutorResult.Success("你好，Default User。我会先按你的销售背景继续聊。"))

        viewModel.updateInput("你好")
        viewModel.send()
        advanceUntilIdle()

        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        val response = lastMessage.uiState as UiState.Response
        assertTrue(response.content.contains("Default User"))
        assertFalse(response.content.contains("只支持围绕已选录音继续讨论"))
        assertTrue(fakeExecutor.executedPrompts.last().contains("姓名：Default User"))
        assertTrue(fakeExecutor.executedPrompts.last().contains("角色：sales_rep"))
        assertTrue(fakeExecutor.executedPrompts.last().contains("用户刚刚说："))
    }

    @Test
    fun `general send with reschedule wording does not mutate scheduler state`() = runTest {
        taskRepository.insertTask(
            ScheduledTask(
                id = "task_general_boundary",
                timeDisplay = "16:00",
                title = "客户会议",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-22T08:00:00Z"),
                durationMinutes = 30
            )
        )
        val originalStartTime = taskRepository.getTask("task_general_boundary")!!.startTime
        val viewModel = newViewModel()
        fakeExecutor.enqueueResponse(ExecutorResult.Success("我可以帮你分析怎么改期，但当前普通聊天不会直接改日程。"))

        viewModel.updateInput("把客户会议改到今晚九点")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(originalStartTime, taskRepository.getTask("task_general_boundary")?.startTime)
        assertTrue(fakeExecutor.executedPrompts.last().contains("把客户会议改到今晚九点"))
        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertTrue((lastMessage.uiState as UiState.Response).content.contains("不会直接改日程"))
    }

    @Test
    fun `selectAudioForChat reuses current general session and preserves prior turns`() = runTest {
        writeAudioMetadata(
            AudioFile(
                id = "audio_attach_1",
                filename = "Attach.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )
        writeArtifacts(
            audioId = "audio_attach_1",
            artifacts = TingwuJobArtifacts(
                transcriptMarkdown = "客户问这周是否能完成报价。",
                smartSummary = TingwuSmartSummary(summary = "客户希望本周完成报价")
            )
        )
        val viewModel = newViewModel()
        fakeExecutor.enqueueResponse(ExecutorResult.Success("先继续普通聊天。"))

        viewModel.updateInput("先聊聊今天怎么跟客户开场")
        viewModel.send()
        advanceUntilIdle()
        val generalSessionId = viewModel.currentSessionId.value
        val historyBeforeAttach = viewModel.history.value.size

        viewModel.selectAudioForChat(
            audioId = "audio_attach_1",
            title = "Attach.wav",
            summary = "客户录音摘要",
            entersPendingFlow = false
        )
        advanceUntilIdle()

        assertEquals(generalSessionId, viewModel.currentSessionId.value)
        assertEquals("audio_attach_1", viewModel.currentLinkedAudioId.value)
        assertTrue(viewModel.history.value.size > historyBeforeAttach)
        val introMessage = viewModel.history.value[historyBeforeAttach] as ChatMessage.Ai
        val intro = introMessage.uiState as UiState.Response
        assertTrue(intro.content.contains("已接入《Attach.wav》"))
        assertFalse(intro.content.contains("客户录音摘要"))
        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertTrue(lastMessage.uiState is UiState.AudioArtifacts)
    }

    @Test
    fun `audio grounded send uses persisted artifacts as grounding`() = runTest {
        writeAudioMetadata(
            AudioFile(
                id = "audio_grounded_1",
                filename = "Grounded.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )
        writeArtifacts(
            audioId = "audio_grounded_1",
            artifacts = TingwuJobArtifacts(
                transcriptMarkdown = "客户说下周启动试点。",
                smartSummary = TingwuSmartSummary(
                    summary = "客户希望下周启动试点",
                    speakerSummaries = listOf(
                        TingwuSpeakerSummary(name = "客户", summary = "确认下周推进试点")
                    ),
                    questionAnswers = listOf(
                        TingwuQuestionAnswer(question = "什么时候启动？", answer = "下周")
                    )
                )
            )
        )
        fakeExecutor.enqueueResponse(ExecutorResult.Success("可以，录音里提到客户希望下周启动试点。"))
        val viewModel = newViewModel()

        viewModel.selectAudioForChat(
            audioId = "audio_grounded_1",
            title = "Grounded.wav",
            summary = "已有摘要",
            entersPendingFlow = false
        )
        advanceUntilIdle()
        viewModel.updateInput("客户什么时候启动？")
        viewModel.send()
        advanceUntilIdle()
        Thread.sleep(150)
        advanceUntilIdle()

        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        val response = lastMessage.uiState as UiState.Response
        assertTrue(response.content.contains("下周启动"))
        assertTrue(fakeExecutor.executedPrompts.last().contains("客户希望下周启动试点"))
        assertTrue(fakeExecutor.executedPrompts.last().contains("发言人总结"))
        assertTrue(fakeExecutor.executedPrompts.last().contains("问答回顾"))
        assertTrue(fakeExecutor.executedPrompts.last().contains("客户什么时候启动"))
    }

    @Test
    fun `audio grounded send with reschedule wording does not mutate scheduler state`() = runTest {
        taskRepository.insertTask(
            ScheduledTask(
                id = "task_audio_boundary",
                timeDisplay = "16:00",
                title = "客户会议",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-22T08:00:00Z"),
                durationMinutes = 30
            )
        )
        val originalStartTime = taskRepository.getTask("task_audio_boundary")!!.startTime
        writeAudioMetadata(
            AudioFile(
                id = "audio_grounded_boundary",
                filename = "Boundary.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )
        writeArtifacts(
            audioId = "audio_grounded_boundary",
            artifacts = TingwuJobArtifacts(
                transcriptMarkdown = "客户提到今晚可能需要调整会议时间。",
                smartSummary = TingwuSmartSummary(summary = "录音里提到会议可能改期")
            )
        )
        fakeExecutor.enqueueResponse(ExecutorResult.Success("我可以基于录音讨论改期建议，但当前录音聊天不会直接改日程。"))
        val viewModel = newViewModel()

        viewModel.selectAudioForChat(
            audioId = "audio_grounded_boundary",
            title = "Boundary.wav",
            summary = "已有摘要",
            entersPendingFlow = false
        )
        viewModel.updateInput("把客户会议改到今晚九点")
        viewModel.send()
        advanceUntilIdle()
        Thread.sleep(150)
        advanceUntilIdle()

        assertEquals(originalStartTime, taskRepository.getTask("task_audio_boundary")?.startTime)
        assertTrue(fakeExecutor.executedPrompts.last().contains("把客户会议改到今晚九点"))
        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertTrue((lastMessage.uiState as UiState.Response).content.contains("不会直接改日程"))
    }

    @Test
    fun `audio grounded send falls back to durable artifact history when repository copy is missing`() = runTest {
        writeAudioMetadata(
            AudioFile(
                id = "audio_grounded_2",
                filename = "Fallback.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )
        fakeExecutor.enqueueResponse(ExecutorResult.Success("可以，我会基于当前录音历史继续回答。"))
        val viewModel = newViewModel()

        viewModel.selectAudioForChat(
            audioId = "audio_grounded_2",
            title = "Fallback.wav",
            summary = "已有摘要",
            entersPendingFlow = false
        )
        viewModel.appendCompletedAudioArtifacts(
            "audio_grounded_2",
            TingwuJobArtifacts(
                transcriptMarkdown = "这是历史中的转写内容。",
                smartSummary = TingwuSmartSummary(summary = "这是历史中的摘要")
            )
        )

        viewModel.updateInput("继续说说重点")
        viewModel.send()
        advanceUntilIdle()
        Thread.sleep(150)
        advanceUntilIdle()

        assertTrue(fakeExecutor.executedPrompts.last().contains("这是历史中的转写内容"))
        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertTrue((lastMessage.uiState as UiState.Response).content.contains("继续回答"))
    }

    @Test
    fun `audio grounded send without artifacts returns explicit guidance error`() = runTest {
        writeAudioMetadata(
            AudioFile(
                id = "audio_grounded_3",
                filename = "Missing.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )
        val viewModel = newViewModel()

        viewModel.selectAudioForChat(
            audioId = "audio_grounded_3",
            title = "Missing.wav",
            summary = "已有摘要",
            entersPendingFlow = false
        )
        viewModel.updateInput("说一下这个录音")
        viewModel.send()
        advanceUntilIdle()
        Thread.sleep(150)
        advanceUntilIdle()

        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        val error = lastMessage.uiState as UiState.Error
        assertTrue(error.message.contains("尚未加载"))
        assertTrue(fakeExecutor.executedPrompts.isEmpty())
    }

    @Test
    fun `createBadgeSchedulerFollowUpSession persists task scoped session without auto selecting it`() = runTest {
        val viewModel = newViewModel()

        val sessionId = viewModel.createBadgeSchedulerFollowUpSession(
            threadId = "thread_1",
            transcript = "plan follow-up with client",
            tasks = listOf(
                SchedulerFollowUpTaskSummary(
                    taskId = "task_1",
                    title = "客户回访",
                    dayOffset = 0,
                    scheduledAtMillis = 123L,
                    durationMinutes = 30
                )
            )
        )

        assertNull(viewModel.currentSessionId.value)
        assertEquals(1, viewModel.groupedSessions.value.values.flatten().size)

        viewModel.switchSession(sessionId!!)

        assertEquals("thread_1", viewModel.currentSchedulerFollowUpContext.value?.sourceBadgeThreadId)
        assertEquals("task_1", viewModel.selectedSchedulerFollowUpTaskId.value)
        assertTrue(viewModel.history.value.single() is ChatMessage.Ai)
    }

    @Test
    fun `startNewSession clears currentSessionId`() = runTest {
        val viewModel = newViewModel()

        val sessionId = viewModel.startSeededSession("plan follow-up with client").let {
            viewModel.currentSessionId.value
        }
        assertEquals(sessionId, viewModel.currentSessionId.value)
        advanceUntilIdle()

        viewModel.startNewSession()

        assertEquals(null, viewModel.currentSessionId.value)
    }

    @Test
    fun `switchSession updates currentSessionId`() = runTest {
        val viewModel = newViewModel()

        viewModel.startSeededSession("first session")
        val firstSessionId = viewModel.currentSessionId.value
        advanceUntilIdle()
        viewModel.startSeededSession("second session")
        val secondSessionId = viewModel.currentSessionId.value
        advanceUntilIdle()

        viewModel.switchSession(firstSessionId!!)
        assertEquals(firstSessionId, viewModel.currentSessionId.value)

        viewModel.switchSession(secondSessionId!!)
        assertEquals(secondSessionId, viewModel.currentSessionId.value)
    }

    @Test
    fun `deleteSession clears currentSessionId when deleting active session`() = runTest {
        val viewModel = newViewModel()

        viewModel.startSeededSession("delete active session")
        val sessionId = viewModel.currentSessionId.value
        assertEquals(sessionId, viewModel.currentSessionId.value)
        advanceUntilIdle()

        viewModel.deleteSession(sessionId!!)

        assertEquals(null, viewModel.currentSessionId.value)
    }

    @Test
    fun `startSchedulerShelfSession emits telemetry and auto submits first turn`() = runTest {
        val viewModel = newViewModel()
        val checkpoints = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            checkpoints += checkpoint to summary
        }

        try {
            viewModel.startSchedulerShelfSession("i want to learn guitar")

            assertTrue(viewModel.isSending.value)
            assertEquals(
                "i want to learn guitar",
                (viewModel.history.value.first() as ChatMessage.User).content
            )
            assertTrue(
                checkpoints.contains(
                    PipelineValve.Checkpoint.UI_STATE_EMITTED to
                        SIM_SCHEDULER_SHELF_SESSION_STARTED_SUMMARY
                )
            )

            advanceUntilIdle()

            assertEquals(2, viewModel.history.value.size)
            assertTrue(viewModel.history.value.last() is ChatMessage.Ai)
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `generic startSeededSession does not emit scheduler shelf telemetry`() = runTest {
        val viewModel = newViewModel()
        val summaries = mutableListOf<String>()

        PipelineValve.testInterceptor = { _, _, summary ->
            summaries += summary
        }

        try {
            viewModel.startSeededSession("i want to learn guitar")
            advanceUntilIdle()

            assertFalse(summaries.contains(SIM_SCHEDULER_SHELF_SESSION_STARTED_SUMMARY))
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `reloaded view model restores durable audio artifact history without auto selecting session`() {
        writeAudioMetadata(
            AudioFile(
                id = "audio_reload_1",
                filename = "Reload.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )
        val firstAudioRepository = newAudioRepository()
        val firstViewModel = newViewModel(audioRepository = firstAudioRepository)
        val sessionId = firstViewModel.selectAudioForChat(
            audioId = "audio_reload_1",
            title = "客户录音",
            summary = "已有摘要",
            entersPendingFlow = false
        )
        firstViewModel.appendCompletedAudioArtifacts(
            "audio_reload_1",
            TingwuJobArtifacts(transcriptMarkdown = "完整转写")
        )

        val reloadedAudioRepository = newAudioRepository()
        val reloadedViewModel = newViewModel(audioRepository = reloadedAudioRepository)

        assertNull(reloadedViewModel.currentSessionId.value)
        assertEquals(sessionId, reloadedAudioRepository.getBoundSessionId("audio_reload_1"))
        assertEquals(1, reloadedViewModel.groupedSessions.value.values.flatten().size)

        reloadedViewModel.switchSession(sessionId)

        assertEquals(2, reloadedViewModel.history.value.size)
        val lastMessage = reloadedViewModel.history.value.last() as ChatMessage.Ai
        assertTrue(lastMessage.uiState is UiState.AudioArtifacts)
    }

    @Test
    fun `deleting linked session clears sim audio binding`() {
        writeAudioMetadata(
            AudioFile(
                id = "audio_delete_1",
                filename = "Delete.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )
        val audioRepository = newAudioRepository()
        val viewModel = newViewModel(audioRepository = audioRepository)

        val sessionId = viewModel.selectAudioForChat(
            audioId = "audio_delete_1",
            title = "删除录音",
            summary = "摘要",
            entersPendingFlow = false
        )
        assertEquals(sessionId, audioRepository.getBoundSessionId("audio_delete_1"))

        viewModel.deleteSession(sessionId)

        assertNull(audioRepository.getBoundSessionId("audio_delete_1"))
    }

    @Test
    fun `startup reconciliation clears dangling audio bound session id`() {
        writeAudioMetadata(
            AudioFile(
                id = "audio_orphan_1",
                filename = "Orphan.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED,
                boundSessionId = "missing-session"
            )
        )

        val audioRepository = newAudioRepository()
        newViewModel(audioRepository = audioRepository)

        assertNull(audioRepository.getBoundSessionId("audio_orphan_1"))
    }

    @Test
    fun `startup reconciliation restores missing audio binding from persisted linked session`() {
        writeAudioMetadata(
            AudioFile(
                id = "audio_restore_1",
                filename = "Restore.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )
        sessionRepository.saveSession(
            preview = SessionPreview(
                id = "session_restore_1",
                clientName = "恢复录音",
                summary = "摘要",
                timestamp = 123L,
                linkedAudioId = "audio_restore_1"
            ),
            messages = listOf(
                ChatMessage.Ai(
                    id = "msg_restore_1",
                    timestamp = 123L,
                    uiState = UiState.Response("历史消息")
                )
            )
        )

        val audioRepository = newAudioRepository()
        newViewModel(audioRepository = audioRepository)

        assertEquals("session_restore_1", audioRepository.getBoundSessionId("audio_restore_1"))
    }

    @Test
    fun `startup normalization keeps newest linked session for duplicated audio id`() {
        writeAudioMetadata(
            AudioFile(
                id = "audio_dup_1",
                filename = "Dup.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )
        sessionRepository.saveSession(
            preview = SessionPreview(
                id = "session_old",
                clientName = "旧会话",
                summary = "旧",
                timestamp = 100L,
                linkedAudioId = "audio_dup_1"
            ),
            messages = listOf(ChatMessage.Ai("old_msg", 100L, UiState.Response("old")))
        )
        sessionRepository.saveSession(
            preview = SessionPreview(
                id = "session_new",
                clientName = "新会话",
                summary = "新",
                timestamp = 200L,
                linkedAudioId = "audio_dup_1"
            ),
            messages = listOf(ChatMessage.Ai("new_msg", 200L, UiState.Response("new")))
        )

        val audioRepository = newAudioRepository()
        val viewModel = newViewModel(audioRepository = audioRepository)

        val previews = viewModel.groupedSessions.value.values.flatten().associateBy { it.id }
        assertEquals(null, previews.getValue("session_old").linkedAudioId)
        assertEquals("audio_dup_1", previews.getValue("session_new").linkedAudioId)
        assertEquals("session_new", audioRepository.getBoundSessionId("audio_dup_1"))
    }

    @Test
    fun `deleting bound audio unlinks session and keeps session alive`() {
        writeAudioMetadata(
            AudioFile(
                id = "audio_bound_delete_1",
                filename = "BoundDelete.wav",
                timeDisplay = "Now",
                source = AudioSource.PHONE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )

        val audioRepository = newAudioRepository()
        val viewModel = newViewModel(audioRepository = audioRepository)
        val sessionId = viewModel.selectAudioForChat(
            audioId = "audio_bound_delete_1",
            title = "绑定录音",
            summary = "摘要",
            entersPendingFlow = false
        )

        assertEquals("audio_bound_delete_1", viewModel.currentLinkedAudioId.value)
        assertEquals(sessionId, audioRepository.getBoundSessionId("audio_bound_delete_1"))

        viewModel.handleDeletedAudio("audio_bound_delete_1")
        audioRepository.deleteAudio("audio_bound_delete_1")

        val preview = viewModel.groupedSessions.value.values
            .flatten()
            .first { it.id == sessionId }

        assertNull(viewModel.currentLinkedAudioId.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
        assertNull(preview.linkedAudioId)
        assertEquals(SessionKind.GENERAL, preview.sessionKind)
        assertNull(audioRepository.getAudio("audio_bound_delete_1"))
        assertNull(audioRepository.getBoundSessionId("audio_bound_delete_1"))
    }

    @Test
    fun `scheduler follow up quick action toggles done for selected bound task`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task_follow_1",
                timeDisplay = "16:00",
                title = "客户回访",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-22T08:00:00Z"),
                durationMinutes = 30
            )
        )
        val viewModel = newViewModel()
        val sessionId = viewModel.createBadgeSchedulerFollowUpSession(
            threadId = "thread_follow_1",
            transcript = "提醒我下午回访客户",
            tasks = listOf(
                SchedulerFollowUpTaskSummary(
                    taskId = taskId,
                    title = "客户回访",
                    dayOffset = 0,
                    scheduledAtMillis = Instant.parse("2026-03-22T08:00:00Z").toEpochMilli(),
                    durationMinutes = 30
                )
            )
        )
        viewModel.switchSession(sessionId!!)

        viewModel.performSchedulerFollowUpQuickAction(SimSchedulerFollowUpQuickAction.MARK_DONE)
        advanceUntilIdle()

        assertTrue(taskRepository.getTask(taskId)?.isDone == true)
        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertTrue((lastMessage.uiState as UiState.Response).content.contains("已标记完成"))
    }

    @Test
    fun `debug badge scheduler follow up seeding persists live task records for single task flow`() = runTest {
        val viewModel = newViewModel()

        val sessionId = viewModel.createDebugBadgeSchedulerFollowUpSession(
            threadId = "thread_debug_1",
            transcript = "提醒我一会儿回访客户",
            tasks = listOf(
                SchedulerFollowUpTaskSummary(
                    taskId = "debug_follow_up_single",
                    title = "客户回访",
                    dayOffset = 0,
                    scheduledAtMillis = Instant.parse("2026-03-22T08:01:00Z").toEpochMilli(),
                    durationMinutes = 30
                )
            )
        )

        assertEquals(
            "客户回访",
            taskRepository.getTask("debug_follow_up_single")?.title
        )

        viewModel.switchSession(sessionId!!)
        viewModel.performSchedulerFollowUpQuickAction(SimSchedulerFollowUpQuickAction.MARK_DONE)
        advanceUntilIdle()

        assertTrue(taskRepository.getTask("debug_follow_up_single")?.isDone == true)
    }

    @Test
    fun `scheduler follow up send resolves target globally by explicit delta and records V2 shadow parity`() = runTest {
        val valveEvents = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            valveEvents += checkpoint to summary
        }
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task_follow_delta",
                timeDisplay = "16:00",
                title = "赶高铁",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-22T08:00:00Z"),
                durationMinutes = 30
            )
        )
        val viewModel = newViewModel()
        val sessionId = viewModel.createBadgeSchedulerFollowUpSession(
            threadId = "thread_follow_delta",
            transcript = "安排赶高铁",
            tasks = listOf(
                SchedulerFollowUpTaskSummary(
                    taskId = taskId,
                    title = "赶高铁",
                    dayOffset = 0,
                    scheduledAtMillis = Instant.parse("2026-03-22T08:00:00Z").toEpochMilli(),
                    durationMinutes = 30
                )
            )
        )
        viewModel.switchSession(sessionId!!)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.Resolved(taskId)

        enqueueGlobalRescheduleExtraction(
            targetQuery = "赶高铁",
            timeInstruction = "推迟1个小时"
        )
        enqueueFollowUpShadowDelta(minutes = 60)
        viewModel.updateInput("推迟1个小时")
        viewModel.send()
        advanceUntilIdle()

        val updated = taskRepository.getTask(taskId)
        assertEquals(Instant.parse("2026-03-22T09:00:00Z"), updated?.startTime)
        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertTrue((lastMessage.uiState as UiState.Response).content.contains("已改期：赶高铁"))
        assertTrue(fakeExecutor.executedPrompts.isNotEmpty())
        assertTrue(
            valveEvents.contains(
                PipelineValve.Checkpoint.UI_STATE_EMITTED to
                    SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_STARTED_SUMMARY
            )
        )
        assertTrue(
            valveEvents.contains(
                PipelineValve.Checkpoint.UI_STATE_EMITTED to
                    SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_PARITY_SUMMARY
            )
        )
    }

    @Test
    fun `scheduler follow up send keeps V1 write when V2 shadow reports unsupported and emits mismatch support`() = runTest {
        val valveEvents = mutableListOf<Pair<PipelineValve.Checkpoint, String>>()
        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            valveEvents += checkpoint to summary
        }
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task_follow_shadow_mismatch",
                timeDisplay = "16:00",
                title = "赶高铁",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-22T08:00:00Z"),
                durationMinutes = 30
            )
        )
        val viewModel = newViewModel()
        val sessionId = viewModel.createBadgeSchedulerFollowUpSession(
            threadId = "thread_follow_shadow_mismatch",
            transcript = "安排赶高铁",
            tasks = listOf(
                SchedulerFollowUpTaskSummary(
                    taskId = taskId,
                    title = "赶高铁",
                    dayOffset = 0,
                    scheduledAtMillis = Instant.parse("2026-03-22T08:00:00Z").toEpochMilli(),
                    durationMinutes = 30
                )
            )
        )
        viewModel.switchSession(sessionId!!)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.Resolved(taskId)

        enqueueGlobalRescheduleExtraction(
            targetQuery = "赶高铁",
            timeInstruction = "推迟1个小时"
        )
        enqueueFollowUpShadowUnsupported("shadow experiment does not support this")
        viewModel.updateInput("推迟1个小时")
        viewModel.send()
        advanceUntilIdle()

        val updated = taskRepository.getTask(taskId)
        assertEquals(Instant.parse("2026-03-22T09:00:00Z"), updated?.startTime)
        assertTrue(
            valveEvents.contains(
                PipelineValve.Checkpoint.UI_STATE_EMITTED to
                    SIM_BADGE_SCHEDULER_FOLLOW_UP_V2_SHADOW_MISMATCH_SUPPORT_SUMMARY
            )
        )
    }

    @Test
    fun `scheduler follow up send still supports absolute exact reschedule while V2 shadow runs`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task_follow_absolute",
                timeDisplay = "16:00",
                title = "客户回访",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-22T08:00:00Z"),
                durationMinutes = 30
            )
        )
        val viewModel = newViewModel()
        val sessionId = viewModel.createBadgeSchedulerFollowUpSession(
            threadId = "thread_follow_absolute",
            transcript = "安排客户回访",
            tasks = listOf(
                SchedulerFollowUpTaskSummary(
                    taskId = taskId,
                    title = "客户回访",
                    dayOffset = 0,
                    scheduledAtMillis = Instant.parse("2026-03-22T08:00:00Z").toEpochMilli(),
                    durationMinutes = 30
                )
            )
        )
        viewModel.switchSession(sessionId!!)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.Resolved(taskId)

        enqueueGlobalRescheduleExtraction(
            targetQuery = "客户回访",
            timeInstruction = "明天早上8点"
        )
        enqueueFollowUpShadowRelativeDayClock(dayOffset = 1, clockTime = "08:00")
        viewModel.updateInput("改到明天早上8点")
        viewModel.send()
        advanceUntilIdle()

        val updated = taskRepository.getTask(taskId)
        assertEquals(Instant.parse("2026-03-23T00:00:00Z"), updated?.startTime)
        assertEquals(30, updated?.durationMinutes)
        assertTrue(fakeExecutor.executedPrompts.isNotEmpty())
    }

    @Test
    fun `scheduler follow up send reschedules globally resolved conflicted task by explicit delta and recomputes conflict while V2 shadow runs`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task_follow_conflict",
                timeDisplay = "16:00",
                title = "赶高铁",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = Instant.parse("2026-03-22T08:00:00Z"),
                durationMinutes = 30,
                hasConflict = true
            )
        )
        scheduleBoard.nextConflictResult = com.smartsales.prism.domain.memory.ConflictResult.Conflict(
            overlaps = listOf(
                com.smartsales.prism.domain.memory.ScheduleItem(
                    entryId = "task_other",
                    title = "叫我吃饭",
                    scheduledAt = Instant.parse("2026-03-22T09:00:00Z").toEpochMilli(),
                    durationMinutes = 30,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        val viewModel = newViewModel()
        val sessionId = viewModel.createBadgeSchedulerFollowUpSession(
            threadId = "thread_follow_conflict",
            transcript = "安排赶高铁",
            tasks = listOf(
                SchedulerFollowUpTaskSummary(
                    taskId = taskId,
                    title = "赶高铁",
                    dayOffset = 0,
                    scheduledAtMillis = Instant.parse("2026-03-22T08:00:00Z").toEpochMilli(),
                    durationMinutes = 30
                )
            )
        )
        viewModel.switchSession(sessionId!!)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.Resolved(taskId)

        enqueueGlobalRescheduleExtraction(
            targetQuery = "赶高铁",
            timeInstruction = "推迟1个小时"
        )
        enqueueFollowUpShadowDelta(minutes = 60)
        viewModel.updateInput("推迟1个小时")
        viewModel.send()
        advanceUntilIdle()

        val updated = taskRepository.getTask(taskId)
        assertEquals(Instant.parse("2026-03-22T09:00:00Z"), updated?.startTime)
        assertTrue(updated?.hasConflict == true)
        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertTrue((lastMessage.uiState as UiState.Response).content.contains("注意：与「叫我吃饭」时间冲突"))
        assertTrue(fakeExecutor.executedPrompts.isNotEmpty())
    }

    @Test
    fun `scheduler follow up send keeps fire off task non conflicting on reschedule`() = runTest {
        val taskId = taskRepository.insertTask(
            ScheduledTask(
                id = "task_follow_fireoff",
                timeDisplay = "16:00",
                title = "提醒我喝水",
                urgencyLevel = UrgencyLevel.FIRE_OFF,
                startTime = Instant.parse("2026-03-22T08:00:00Z"),
                durationMinutes = 0
            )
        )
        scheduleBoard.nextConflictResult = com.smartsales.prism.domain.memory.ConflictResult.Conflict(
            overlaps = listOf(
                com.smartsales.prism.domain.memory.ScheduleItem(
                    entryId = "task_other",
                    title = "客户会议",
                    scheduledAt = Instant.parse("2026-03-22T09:00:00Z").toEpochMilli(),
                    durationMinutes = 60,
                    durationSource = com.smartsales.prism.domain.memory.DurationSource.DEFAULT,
                    conflictPolicy = com.smartsales.prism.domain.memory.ConflictPolicy.EXCLUSIVE
                )
            )
        )
        val viewModel = newViewModel()
        val sessionId = viewModel.createBadgeSchedulerFollowUpSession(
            threadId = "thread_follow_fireoff",
            transcript = "安排喝水提醒",
            tasks = listOf(
                SchedulerFollowUpTaskSummary(
                    taskId = taskId,
                    title = "提醒我喝水",
                    dayOffset = 0,
                    scheduledAtMillis = Instant.parse("2026-03-22T08:00:00Z").toEpochMilli(),
                    durationMinutes = 0
                )
            )
        )
        viewModel.switchSession(sessionId!!)
        activeTaskRetrievalIndex.nextResolveResult = ActiveTaskResolveResult.Resolved(taskId)

        enqueueGlobalRescheduleExtraction(
            targetQuery = "提醒我喝水",
            timeInstruction = "推迟1个小时"
        )
        enqueueFollowUpShadowDelta(minutes = 60)
        viewModel.updateInput("推迟1个小时")
        viewModel.send()
        advanceUntilIdle()

        val updated = taskRepository.getTask(taskId)
        assertEquals(Instant.parse("2026-03-22T09:00:00Z"), updated?.startTime)
        assertTrue(updated?.hasConflict == false)
        assertNull(scheduleBoard.lastDurationMinutes)
        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertFalse((lastMessage.uiState as UiState.Response).content.contains("注意："))
    }

    @Test
    fun `scheduler follow up send safely blocks mutation when global target extraction stays unsupported`() = runTest {
        taskRepository.insertTask(
            ScheduledTask(
                id = "task_follow_a",
                timeDisplay = "10:00",
                title = "客户A回访",
                urgencyLevel = UrgencyLevel.L3_NORMAL,
                startTime = Instant.parse("2026-03-22T02:00:00Z"),
                durationMinutes = 30
            )
        )
        taskRepository.insertTask(
            ScheduledTask(
                id = "task_follow_b",
                timeDisplay = "11:00",
                title = "客户B回访",
                urgencyLevel = UrgencyLevel.L3_NORMAL,
                startTime = Instant.parse("2026-03-22T03:00:00Z"),
                durationMinutes = 30
            )
        )
        val viewModel = newViewModel()
        val sessionId = viewModel.createBadgeSchedulerFollowUpSession(
            threadId = "thread_follow_2",
            transcript = "安排两个客户回访",
            tasks = listOf(
                SchedulerFollowUpTaskSummary("task_follow_a", "客户A回访", 0, Instant.parse("2026-03-22T02:00:00Z").toEpochMilli(), 30),
                SchedulerFollowUpTaskSummary("task_follow_b", "客户B回访", 0, Instant.parse("2026-03-22T03:00:00Z").toEpochMilli(), 30)
            )
        )
        viewModel.switchSession(sessionId!!)

        assertNull(viewModel.selectedSchedulerFollowUpTaskId.value)
        enqueueGlobalRescheduleExtractionUnsupported("target remains ambiguous")
        viewModel.updateInput("改到明天下午三点")
        viewModel.send()
        advanceUntilIdle()

        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertTrue((lastMessage.uiState as UiState.Response).content.contains("明确目标"))
    }

    private fun newViewModel(
        audioRepository: SimAudioRepository = newAudioRepository(),
        executor: FakeExecutor = fakeExecutor
    ): SimAgentViewModel {
        return SimAgentViewModel(
            sessionRepository = sessionRepository,
            audioRepository = audioRepository,
            speechRecognizer = speechRecognizer,
            taskRepository = taskRepository,
            scheduleBoard = scheduleBoard,
            activeTaskRetrievalIndex = activeTaskRetrievalIndex,
            alarmScheduler = alarmScheduler,
            uniAExtractionService = uniAExtractionService,
            globalRescheduleExtractionService = globalRescheduleExtractionService,
            followUpRescheduleExtractionService = followUpRescheduleExtractionService,
            executor = executor,
            userProfileRepository = userProfileRepository,
            timeProvider = timeProvider
        )
    }

    private class FakeDeviceSpeechRecognizer : DeviceSpeechRecognizer {
        var nextResult: DeviceSpeechRecognitionResult =
            DeviceSpeechRecognitionResult.Success("默认语音")
        var finishDelayMillis: Long = 0L
        var failStart = false
        var lastMode: DeviceSpeechMode? = null
        private var listening = false

        override fun startListening(mode: DeviceSpeechMode) {
            if (failStart) error("start failed")
            lastMode = mode
            listening = true
        }

        override suspend fun finishListening(): DeviceSpeechRecognitionResult {
            if (finishDelayMillis > 0L) {
                delay(finishDelayMillis)
            }
            listening = false
            return nextResult
        }

        override fun cancelListening() {
            listening = false
        }

        override fun isListening(): Boolean = listening
    }

    private fun enqueueFollowUpShadowDelta(minutes: Int) {
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "RESCHEDULE_EXACT",
                  "timeKind": "DELTA_FROM_TARGET",
                  "deltaFromTargetMinutes": $minutes
                }
                """.trimIndent()
            )
        )
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

    private fun enqueueGlobalRescheduleExtractionUnsupported(reason: String) {
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

    private fun enqueueFollowUpShadowRelativeDayClock(dayOffset: Int, clockTime: String) {
        fakeExecutor.enqueueResponse(
            ExecutorResult.Success(
                """
                {
                  "decision": "RESCHEDULE_EXACT",
                  "timeKind": "RELATIVE_DAY_CLOCK",
                  "relativeDayOffset": $dayOffset,
                  "clockTime": "$clockTime"
                }
                """.trimIndent()
            )
        )
    }

    private fun enqueueFollowUpShadowUnsupported(reason: String) {
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

    private fun newAudioRepository(): SimAudioRepository {
        val context: Context = mock()
        whenever(context.filesDir).thenReturn(tempFolder.root)
        return SimAudioRepository(
            context = context,
            connectivityBridge = mock<ConnectivityBridge>(),
            ossUploader = mock<OssUploader>(),
            tingwuPipeline = mock<TingwuPipeline>()
        )
    }

    private fun writeAudioMetadata(vararg entries: AudioFile) {
        File(tempFolder.root, SIM_AUDIO_METADATA_FILENAME).writeText(
            Json.encodeToString(entries.toList())
        )
    }

    private fun writeArtifacts(audioId: String, artifacts: TingwuJobArtifacts) {
        File(tempFolder.root, simArtifactFilename(audioId)).writeText(
            Json.encodeToString(artifacts)
        )
    }
}
