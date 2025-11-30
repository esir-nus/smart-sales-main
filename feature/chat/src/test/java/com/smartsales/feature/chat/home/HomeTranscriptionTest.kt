package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeTranscriptionTest.kt
// 模块：:feature:chat
// 说明：验证聊天界面处理音频转写导航请求的行为
// 作者：创建于 2025-11-21

import com.smartsales.core.util.Result
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.history.ChatMessageEntity
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.AiSessionRepository as SessionRepository
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeTranscriptionTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var transcriptionCoordinator: FakeTranscriptionCoordinator
    private lateinit var viewModel: HomeScreenViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        transcriptionCoordinator = FakeTranscriptionCoordinator()
        viewModel = HomeScreenViewModel(
            aiChatService = FakeAiChatService(),
            aiSessionRepository = FakeAiSessionRepository(),
            deviceConnectionManager = FakeDeviceConnectionManager(),
            mediaSyncCoordinator = FakeMediaSyncCoordinator(),
            transcriptionCoordinator = transcriptionCoordinator,
            quickSkillCatalog = FakeQuickSkillCatalog(),
            chatHistoryRepository = FakeChatHistoryRepository(),
            sessionRepository = FakeSessionRepository()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `transcription request adds progress and final transcript`() = runTest(dispatcher) {
        transcriptionCoordinator.registerJob("job-1")

        viewModel.onTranscriptionRequested(
            TranscriptionChatRequest(
                jobId = "job-1",
                fileName = "demo.wav"
            )
        )

        advanceUntilIdle()
        assertTrue(
            viewModel.uiState.value.chatMessages.any { it.content.contains("demo.wav") && it.isStreaming }
        )

        transcriptionCoordinator.emit(
            "job-1",
            AudioTranscriptionJobState.Completed("job-1", "## 转写结果")
        )
        advanceUntilIdle()

        assertTrue(
            viewModel.uiState.value.chatMessages.any { it.content.contains("## 转写结果") }
        )
    }

    @Test
    fun `transcription request injects intro and transcript context`() = runTest(dispatcher) {
        viewModel.onTranscriptionRequested(
            TranscriptionChatRequest(
                jobId = "job-2",
                fileName = "call-1.wav",
                transcriptMarkdown = "## 内容\n- 要点"
            )
        )

        advanceUntilIdle()

        val messages = viewModel.uiState.value.chatMessages
        assertTrue(messages.any { it.content.contains("通话分析") && it.content.contains("call-1.wav") })
        assertTrue(messages.any { it.content.contains("已加载录音") && it.content.contains("要点") })
    }

    private class FakeAiChatService : AiChatService {
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flowOf()
    }

    private class FakeAiSessionRepository : AiSessionRepository {
        override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> =
            emptyList()
    }

    private class FakeSessionRepository : SessionRepository {
        private val sessions = mutableMapOf<String, AiSessionSummary>()
        override val summaries: MutableStateFlow<List<AiSessionSummary>> =
            MutableStateFlow(emptyList())

        override suspend fun upsert(summary: AiSessionSummary) {
            sessions[summary.id] = summary
            summaries.value = sessions.values.toList()
        }

        override suspend fun delete(id: String) {
            sessions.remove(id)
            summaries.value = sessions.values.toList()
        }

        override suspend fun findById(id: String): AiSessionSummary? = sessions[id]
    }

    private class FakeDeviceConnectionManager : DeviceConnectionManager {
        override val state: MutableStateFlow<ConnectionState> =
            MutableStateFlow(ConnectionState.Disconnected)

        override fun selectPeripheral(peripheral: BlePeripheral) = Unit
        override suspend fun startPairing(peripheral: BlePeripheral, credentials: WifiCredentials): Result<Unit> =
            Result.Error(UnsupportedOperationException())

        override suspend fun retry(): Result<Unit> = Result.Error(UnsupportedOperationException())
        override fun forgetDevice() = Unit
        override suspend fun requestHotspotCredentials(): Result<WifiCredentials> =
            Result.Error(UnsupportedOperationException())

        override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> =
            Result.Error(UnsupportedOperationException())
    }

    private class FakeMediaSyncCoordinator : MediaSyncCoordinator {
        override val state: MutableStateFlow<MediaSyncState> =
            MutableStateFlow(MediaSyncState())

        override suspend fun triggerSync(): Result<Unit> = Result.Success(Unit)
    }

    private class FakeQuickSkillCatalog : QuickSkillCatalog {
        override fun homeQuickSkills(): List<QuickSkillDefinition> = emptyList()
    }

    private class FakeChatHistoryRepository : ChatHistoryRepository {
        private var stored: List<ChatMessageEntity> = emptyList()
        override suspend fun loadLatestSession(sessionId: String): List<ChatMessageEntity> = stored

        override suspend fun saveMessages(sessionId: String, messages: List<ChatMessageEntity>) {
            stored = messages
        }

        override suspend fun deleteSession(sessionId: String) {
            stored = emptyList()
        }
    }

    private class FakeTranscriptionCoordinator : AudioTranscriptionCoordinator {
        private val jobs = mutableMapOf<String, MutableStateFlow<AudioTranscriptionJobState>>()

        fun registerJob(jobId: String) {
            jobs[jobId] = MutableStateFlow(AudioTranscriptionJobState.InProgress(jobId, 5))
        }

        fun emit(jobId: String, state: AudioTranscriptionJobState) {
            jobs[jobId]?.value = state
        }

        override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> =
            Result.Error(UnsupportedOperationException())

        override suspend fun submitTranscription(
            audioAssetName: String,
            language: String,
            uploadPayload: AudioUploadPayload
        ): Result<String> = Result.Success("job-ignored")

        override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> =
            jobs.getOrPut(jobId) { MutableStateFlow(AudioTranscriptionJobState.Idle) }
    }
}
