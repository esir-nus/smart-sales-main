package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeTranscriptionTest.kt
// 模块：:feature:chat
// 说明：验证聊天界面处理音频转写导航请求的行为
// 作者：创建于 2025-11-21

import com.smartsales.core.util.Result
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
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
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.data.aicore.ExportResult
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.StoredAudio
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.smartsales.feature.chat.title.SessionTitleResolver
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.TokenUsage
import com.smartsales.feature.chat.ChatShareHandler
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.smartsales.feature.chat.testutil.TestContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeTranscriptionTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var transcriptionCoordinator: FakeTranscriptionCoordinator
    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var aiChatService: FakeAiChatService
    private val appContext = TestContext()
    private lateinit var metaHub: FakeMetaHub

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        transcriptionCoordinator = FakeTranscriptionCoordinator()
        aiChatService = FakeAiChatService()
        metaHub = FakeMetaHub()
        viewModel = HomeScreenViewModel(
            appContext = appContext,
            homeOrchestrator = FakeHomeOrchestrator(aiChatService),
            aiSessionRepository = FakeChatMessageRepository(),
            deviceConnectionManager = FakeDeviceConnectionManager(),
            mediaSyncCoordinator = FakeMediaSyncCoordinator(),
            transcriptionCoordinator = transcriptionCoordinator,
            audioStorageRepository = object : AudioStorageRepository {
                override val audios: Flow<List<StoredAudio>> = MutableStateFlow(emptyList())
                override suspend fun importFromDevice(baseUrl: String, file: com.smartsales.feature.media.devicemanager.DeviceMediaFile): StoredAudio {
                    throw UnsupportedOperationException()
                }
                override suspend fun importFromPhone(uri: android.net.Uri): StoredAudio {
                    throw UnsupportedOperationException()
                }

                override suspend fun delete(audioId: String) {}
            },
            quickSkillCatalog = FakeQuickSkillCatalog(),
            chatHistoryRepository = FakeChatHistoryRepository(),
            sessionRepository = FakeSessionRepository(),
            sessionTitleResolver = SessionTitleResolver(metaHub),
            userProfileRepository = FakeUserProfileRepository(),
            metaHub = metaHub,
            exportOrchestrator = FakeExportOrchestrator(),
            shareHandler = FakeShareHandler()
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
        assertFalse(viewModel.uiState.value.isInputBusy)
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

    private class FakeHomeOrchestrator(
        private val delegate: AiChatService
    ) : HomeOrchestrator {
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> =
            delegate.streamChat(request)
    }

    private class FakeUserProfileRepository : com.smartsales.feature.usercenter.data.UserProfileRepository {
        private val state = kotlinx.coroutines.flow.MutableStateFlow(
            com.smartsales.feature.usercenter.UserProfile(
                displayName = "测试用户",
                email = "user@example.com",
                isGuest = false
            )
        )
        override val profileFlow = state
        override suspend fun load(): com.smartsales.feature.usercenter.UserProfile = state.value
        override suspend fun save(profile: com.smartsales.feature.usercenter.UserProfile) {
            state.value = profile
        }

        override suspend fun clear() {
            state.value = com.smartsales.feature.usercenter.UserProfile("", "", true)
        }
    }

    private class FakeChatMessageRepository : com.smartsales.feature.chat.home.AiSessionRepository {
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

        override suspend fun updateTitle(id: String, newTitle: String) {
            sessions[id]?.let { existing ->
                val updated = existing.copy(title = newTitle)
                sessions[id] = updated
                summaries.value = sessions.values.toList()
            }
        }
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

        override fun scheduleAutoReconnectIfNeeded() {}
        override fun forceReconnectNow() {}
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
            uploadPayload: AudioUploadPayload,
            sessionId: String?
        ): Result<String> = Result.Success("job-ignored")

        override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> =
            jobs.getOrPut(jobId) { MutableStateFlow(AudioTranscriptionJobState.Idle) }
    }

    private class FakeExportOrchestrator : ExportOrchestrator {
        override suspend fun exportPdf(sessionId: String, markdown: String, userName: String?): Result<ExportResult> =
            Result.Success(ExportResult("demo.pdf", "application/pdf", ByteArray(0)))

        override suspend fun exportCsv(sessionId: String, userName: String?): Result<ExportResult> =
            Result.Success(ExportResult("demo.csv", "text/csv", ByteArray(0)))
    }

    private class FakeShareHandler : ChatShareHandler {
        override suspend fun copyMarkdown(markdown: String): Result<Unit> = Result.Success(Unit)
        override suspend fun copyAssistantReply(text: String): Result<Unit> = Result.Success(Unit)
        override suspend fun shareExport(result: ExportResult): Result<Unit> = Result.Success(Unit)
    }

    private class FakeMetaHub : MetaHub {
        override suspend fun upsertSession(metadata: SessionMetadata) {}
        override suspend fun getSession(sessionId: String): SessionMetadata? = null
        override suspend fun upsertTranscript(metadata: TranscriptMetadata) {}
        override suspend fun getTranscriptBySession(sessionId: String): TranscriptMetadata? = null
        override suspend fun upsertExport(metadata: ExportMetadata) {}
        override suspend fun getExport(sessionId: String): ExportMetadata? = null
        override suspend fun logUsage(usage: TokenUsage) {}
    }
}
