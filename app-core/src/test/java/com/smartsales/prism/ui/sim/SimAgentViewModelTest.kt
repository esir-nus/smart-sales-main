package com.smartsales.prism.ui.sim

import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuSmartSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimAgentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectAudioForChat pending binds immediately and shows thinking state`() {
        val viewModel = SimAgentViewModel()

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
        val viewModel = SimAgentViewModel()

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
        val viewModel = SimAgentViewModel()

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
        val viewModel = SimAgentViewModel()

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
        val viewModel = SimAgentViewModel()
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
        val viewModel = SimAgentViewModel()
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
    fun `startSchedulerShelfSession emits telemetry and auto submits first turn`() = runTest {
        val viewModel = SimAgentViewModel()
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
        val viewModel = SimAgentViewModel()
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
}
