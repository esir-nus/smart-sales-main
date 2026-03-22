package com.smartsales.prism.ui.sim

import android.content.Context
import com.smartsales.core.pipeline.PromptCompiler
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.test.fakes.FakeExecutor
import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.core.test.fakes.FakeUserProfileRepository
import com.smartsales.data.oss.OssUploader
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
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.domain.tingwu.TingwuSmartSummary
import java.time.Instant
import java.io.File
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
    private lateinit var alarmScheduler: FakeAlarmScheduler
    private lateinit var fakeExecutor: FakeExecutor
    private lateinit var userProfileRepository: FakeUserProfileRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var uniAExtractionService: RealUniAExtractionService

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = SimSessionRepository(tempFolder.root)
        taskRepository = FakeScheduledTaskRepository()
        scheduleBoard = FakeScheduleBoard()
        alarmScheduler = FakeAlarmScheduler()
        fakeExecutor = FakeExecutor()
        userProfileRepository = FakeUserProfileRepository()
        timeProvider = FakeTimeProvider().apply {
            fixedInstant = Instant.parse("2026-03-22T08:00:00Z")
        }
        uniAExtractionService = RealUniAExtractionService(
            fakeExecutor,
            PromptCompiler(),
            SchedulerLinter(timeProvider)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cold start with empty sim store shows no seeded sessions`() {
        val viewModel = newViewModel()

        assertTrue(viewModel.groupedSessions.value.isEmpty())
        assertNull(viewModel.currentSessionId.value)
        assertTrue(viewModel.history.value.isEmpty())
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

        assertEquals(generalSessionId, viewModel.currentSessionId.value)
        assertEquals("audio_attach_1", viewModel.currentLinkedAudioId.value)
        assertTrue(viewModel.history.value.size > historyBeforeAttach)
        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        val response = lastMessage.uiState as UiState.Response
        assertTrue(response.content.contains("已接入《Attach.wav》"))
        assertTrue(response.content.contains("客户录音摘要"))
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
                smartSummary = TingwuSmartSummary(summary = "客户希望下周启动试点")
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
        viewModel.updateInput("客户什么时候启动？")
        viewModel.send()
        advanceUntilIdle()
        Thread.sleep(150)
        advanceUntilIdle()

        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        val response = lastMessage.uiState as UiState.Response
        assertTrue(response.content.contains("下周启动"))
        assertTrue(fakeExecutor.executedPrompts.last().contains("客户希望下周启动试点"))
        assertTrue(fakeExecutor.executedPrompts.last().contains("客户什么时候启动"))
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
    fun `scheduler follow up send safely blocks mutation when multi task selection is missing`() = runTest {
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
        viewModel.updateInput("改到明天下午三点")
        viewModel.send()
        advanceUntilIdle()

        val lastMessage = viewModel.history.value.last() as ChatMessage.Ai
        assertTrue((lastMessage.uiState as UiState.Response).content.contains("请先选择"))
    }

    private fun newViewModel(
        audioRepository: SimAudioRepository = newAudioRepository(),
        executor: FakeExecutor = fakeExecutor
    ): SimAgentViewModel {
        return SimAgentViewModel(
            sessionRepository = sessionRepository,
            audioRepository = audioRepository,
            taskRepository = taskRepository,
            scheduleBoard = scheduleBoard,
            alarmScheduler = alarmScheduler,
            uniAExtractionService = uniAExtractionService,
            executor = executor,
            userProfileRepository = userProfileRepository,
            timeProvider = timeProvider
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
