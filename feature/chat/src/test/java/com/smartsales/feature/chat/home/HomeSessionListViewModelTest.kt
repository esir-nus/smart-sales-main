package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeSessionListViewModelTest.kt
// 模块：:feature:chat
// 说明：验证 Home 层会话列表的加载、排序与转写会话标记逻辑
// 作者：创建于 2025-11-25

import com.smartsales.core.util.Result
import com.smartsales.feature.chat.AiSessionRepository as SessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.chat.title.SessionTitleResolver
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.history.ChatMessageEntity
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.media.MediaClipStatus
import com.smartsales.feature.media.MediaClip
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.chat.home.TranscriptionChatRequest
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.StoredAudio
import com.smartsales.feature.chat.testutil.TestContext
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.TokenUsage
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.data.aicore.ExportResult
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.InMemoryAiParaSettingsRepository
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.core.metahub.SessionTitlePolicy
import com.smartsales.feature.chat.testutil.buildNoopXfyunVoiceprintApi
import com.smartsales.feature.chat.testutil.NoopDebugOrchestrator

@OptIn(ExperimentalCoroutinesApi::class)
class HomeSessionListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var fakeHistoryRepository: FakeChatHistoryRepository
    private lateinit var aiChatService: FakeAiChatService
    private val appContext = TestContext()
    private lateinit var metaHub: FakeMetaHub

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        sessionRepository = FakeSessionRepository()
        fakeHistoryRepository = FakeChatHistoryRepository()
        aiChatService = FakeAiChatService()
        metaHub = FakeMetaHub()
        viewModel = HomeScreenViewModel(
            appContext = appContext,
            homeOrchestrator = FakeHomeOrchestrator(aiChatService),
            aiSessionRepository = FakeHomeSessionMessageRepository(),
            deviceConnectionManager = FakeDeviceConnectionManager(),
            mediaSyncCoordinator = FakeMediaSyncCoordinator(),
            transcriptionCoordinator = FakeTranscriptionCoordinator(),
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
            chatHistoryRepository = fakeHistoryRepository,
            sessionRepository = sessionRepository,
            sessionTitleResolver = SessionTitleResolver(metaHub),
            userProfileRepository = FakeUserProfileRepository(),
            metaHub = metaHub,
            debugOrchestrator = NoopDebugOrchestrator(),
            exportOrchestrator = FakeExportOrchestrator(),
            shareHandler = FakeShareHandler(),
            xfyunTraceStore = XfyunTraceStore(),
            tingwuTraceStore = TingwuTraceStore(),
            aiParaSettingsRepository = InMemoryAiParaSettingsRepository(),
            xfyunVoiceprintApi = buildNoopXfyunVoiceprintApi(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `session list maps summaries with ordering and flags`() = runTest(dispatcher) {
        val manual = AiSessionSummary(
            id = "manual-session",
            title = "手动会话",
            lastMessagePreview = "你好",
            updatedAtMillis = Long.MAX_VALUE - 1
        )
        val transcript = AiSessionSummary(
            id = "call-session",
            title = "通话分析 – 客户A",
            lastMessagePreview = "结论",
            updatedAtMillis = Long.MAX_VALUE,
            isTranscription = true
        )
        sessionRepository.upsert(manual)
        sessionRepository.upsert(transcript)

        advanceUntilIdle()

        val sessions = viewModel.uiState.value.sessionList
        assertTrue(sessions.first().id == "call-session")
        assertTrue(sessions.first().isTranscription)
        val manualItem = sessions.first { it.id == "manual-session" }
        assertTrue(!manualItem.isTranscription)
    }

    @Test
    fun `transcription request updates session list title and preview`() = runTest(dispatcher) {
        viewModel.onTranscriptionRequested(
            TranscriptionChatRequest(
                jobId = "job-1",
                fileName = "demo.wav",
                transcriptMarkdown = "## 录音摘要"
            )
        )

        advanceUntilIdle()

        val sessions = viewModel.uiState.value.sessionList
        val transcriptItem = sessions.first { it.id == "home-session" }
        assertEquals(SessionTitlePolicy.PLACEHOLDER_TITLE, transcriptItem.title)
        assertTrue(transcriptItem.isTranscription)
        assertTrue(transcriptItem.lastMessagePreview.contains("录音摘要"))
    }

    @Test
    fun `new chat creates manual session and switches current`() = runTest(dispatcher) {
        advanceUntilIdle()
        val firstId = viewModel.uiState.value.currentSession.id

        viewModel.onNewChatClicked()
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertTrue(state.currentSession.id != firstId)
        assertTrue(!state.currentSession.isTranscription)
        assertTrue(state.chatMessages.isEmpty())
        assertTrue(state.sessionList.any { it.id == firstId })
        assertTrue(state.sessionList.first { it.id == state.currentSession.id }.isCurrent)
        assertEquals(null, state.chatErrorMessage)
    }

    @Test
    fun `multiple new chats produce distinct sessions`() = runTest(dispatcher) {
        viewModel.onNewChatClicked()
        viewModel.onNewChatClicked()
        advanceUntilIdle()
        val ids = viewModel.uiState.value.sessionList.map { it.id }.toSet()
        assertTrue(ids.size >= 3) // 包含默认会话
    }

    @Test
    fun `history pin toggle moves session to top`() = runTest(dispatcher) {
        val second = sessionRepository.createNewChatSession()
        sessionRepository.upsert(
            second.copy(
                updatedAtMillis = sessionRepository.summaries.value.first().updatedAtMillis - 10
            )
        )
        advanceUntilIdle()

        viewModel.onHistorySessionPinToggle(second.id)
        advanceUntilIdle()

        val sessions = viewModel.uiState.value.sessionList
        assertEquals(second.id, sessions.first().id)
        assertTrue(sessionRepository.findById(second.id)?.pinned == true)
    }

    @Test
    fun `history rename sets user edited flag`() = runTest(dispatcher) {
        val currentId = viewModel.uiState.value.currentSession.id
        viewModel.onHistorySessionRenameConfirmed(currentId, "手动改名")
        advanceUntilIdle()

        val updated = sessionRepository.findById(currentId)
        assertEquals("手动改名", updated?.title)
        assertTrue(updated?.isTitleUserEdited == true)
    }

    @Test
    fun `history delete switches away from removed current session`() = runTest(dispatcher) {
        val originalId = viewModel.uiState.value.currentSession.id
        val other = sessionRepository.createNewChatSession()
        sessionRepository.upsert(other)
        advanceUntilIdle()

        viewModel.onHistorySessionDelete(originalId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.currentSession.id != originalId)
        assertTrue(state.sessionList.none { it.id == originalId })
    }

    private class FakeAiChatService : AiChatService {
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flowOf()
    }

    private class FakeHomeSessionMessageRepository : com.smartsales.feature.chat.home.AiSessionRepository {
        override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> =
            emptyList()
    }

    private class FakeSessionRepository : SessionRepository {
        private val sessions = mutableMapOf<String, AiSessionSummary>()
        override val summaries: MutableStateFlow<List<AiSessionSummary>> =
            MutableStateFlow(emptyList())

        override suspend fun createNewChatSession(): AiSessionSummary {
            val summary = AiSessionSummary(
                id = "session-${sessions.size}",
                title = SessionTitlePolicy.newChatPlaceholder(),
                lastMessagePreview = "",
                updatedAtMillis = 0L
            )
            upsert(summary)
            return summary
        }

        override suspend fun upsert(summary: AiSessionSummary) {
            sessions[summary.id] = summary
            summaries.value = sessions.values.sortedWith(
                compareByDescending<AiSessionSummary> { it.pinned }
                    .thenByDescending { it.updatedAtMillis }
            )
        }

        override suspend fun delete(id: String) {
            sessions.remove(id)
            summaries.value = sessions.values.toList()
        }

        override suspend fun findById(id: String): AiSessionSummary? = sessions[id]

        override suspend fun updateTitle(id: String, newTitle: String, isUserEdited: Boolean) {
            sessions[id]?.let { existing ->
                val updated = existing.copy(
                    title = newTitle,
                    isTitleUserEdited = existing.isTitleUserEdited || isUserEdited
                )
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
            MutableStateFlow(
                MediaSyncState(
                    items = listOf(
                        MediaClip(
                            id = "1",
                            title = "demo",
                            customer = "c",
                            recordedAtMillis = 0L,
                            durationSeconds = 10,
                            sourceDeviceName = "dev",
                            status = MediaClipStatus.Ready,
                            transcriptSource = "ready"
                        )
                    )
                )
            )

        override suspend fun triggerSync(): Result<Unit> = Result.Success(Unit)
    }

    private class FakeQuickSkillCatalog : QuickSkillCatalog {
        override fun homeQuickSkills(): List<QuickSkillDefinition> = emptyList()
    }

    private class FakeHomeOrchestrator(
        private val delegate: AiChatService
    ) : HomeOrchestrator {
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> =
            delegate.streamChat(request)
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
        override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> =
            Result.Error(UnsupportedOperationException())

        override suspend fun submitTranscription(
            audioAssetName: String,
            language: String,
            uploadPayload: AudioUploadPayload,
            sessionId: String?
        ): Result<String> = Result.Success("job-ignored")

        override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> =
            flowOf(AudioTranscriptionJobState.Completed(jobId, "## 完成"))

        override fun observeBatches(jobId: String): Flow<AudioTranscriptionBatchEvent> =
            emptyFlow()
    }

    private class FakeUserProfileRepository : com.smartsales.feature.usercenter.data.UserProfileRepository {
        private val state = kotlinx.coroutines.flow.MutableStateFlow(
            com.smartsales.feature.usercenter.UserProfile(
                displayName = "会话用户",
                email = "session@example.com",
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

    private class FakeExportOrchestrator : ExportOrchestrator {
        override suspend fun exportPdf(sessionId: String, markdown: String, sessionTitle: String?, userName: String?): Result<ExportResult> =
            Result.Success(ExportResult("demo.pdf", "application/pdf", ByteArray(0)))

        override suspend fun exportCsv(sessionId: String, sessionTitle: String?, userName: String?): Result<ExportResult> =
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
