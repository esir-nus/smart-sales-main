package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeStreamingDedupTest.kt
// 模块：:feature:chat
// 说明：验证 Home 流式去重逻辑，确保 Delta 与 Completed 不重复追加
// 作者：创建于 2025-12-06

import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportResult
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.history.ChatMessageEntity
import com.smartsales.feature.chat.title.SessionTitleResolver
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.media.audiofiles.StoredAudio
import com.smartsales.feature.usercenter.UserProfile
import com.smartsales.feature.usercenter.data.UserProfileRepository
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.feature.chat.testutil.TestContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeStreamingDedupTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var orchestrator: FakeHomeOrchestrator
    private lateinit var viewModel: HomeScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        orchestrator = FakeHomeOrchestrator()
        val metaHub = InMemoryMetaHub()
        viewModel = HomeScreenViewModel(
            appContext = TestContext(),
            homeOrchestrator = orchestrator,
            aiSessionRepository = FakeAiSessionRepository(),
            deviceConnectionManager = FakeDeviceConnectionManager(),
            mediaSyncCoordinator = FakeMediaSyncCoordinator(),
            transcriptionCoordinator = FakeTranscriptionCoordinator(),
            audioStorageRepository = FakeAudioStorageRepository(),
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
    fun `delta only keeps single assistant bubble`() = runTest(dispatcher) {
        orchestrator.enqueue(
            ChatStreamEvent.Delta("a"),
            ChatStreamEvent.Delta("b")
        )
        viewModel.onInputChanged("客户需要报价")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val assistants = viewModel.uiState.value.chatMessages.filter { it.role == ChatMessageRole.ASSISTANT }
        assertEquals(1, assistants.size)
        assertEquals("ab", assistants.first().content)
    }

    @Test
    fun `delta then completed replaces content once`() = runTest(dispatcher) {
        orchestrator.enqueue(
            ChatStreamEvent.Delta("he"),
            ChatStreamEvent.Delta("hel"),
            ChatStreamEvent.Completed("hello")
        )
        viewModel.onInputChanged("客户询问产品报价")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val assistants = viewModel.uiState.value.chatMessages.filter { it.role == ChatMessageRole.ASSISTANT }
        assertEquals(1, assistants.size)
        assertEquals("hello", assistants.first().content)
    }

    @Test
    fun `debug toggle switches between sanitized and raw assistant text`() = runTest(dispatcher) {
        val rawReply = "重复重复重复"
        orchestrator.enqueue(ChatStreamEvent.Completed(rawReply))

        viewModel.onInputChanged("客户询问产品报价")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.chatMessages.first { it.role == ChatMessageRole.ASSISTANT }
        val raw = assistant.rawContent
        val sanitized = assistant.sanitizedContent ?: assistant.content

        viewModel.setShowRawAssistantOutput(true)
        val rawDisplay = viewModel.uiState.value.chatMessages.first { it.role == ChatMessageRole.ASSISTANT }.content
        assertEquals(raw, rawDisplay)

        viewModel.setShowRawAssistantOutput(false)
        val sanitizedDisplay = viewModel.uiState.value.chatMessages.first { it.role == ChatMessageRole.ASSISTANT }.content
        assertEquals(sanitized, sanitizedDisplay)
    }

    @Test
    fun `multiple rounds keep one assistant bubble per turn`() = runTest(dispatcher) {
        orchestrator.enqueue(
            ChatStreamEvent.Delta("hi"),
            ChatStreamEvent.Completed("hi there")
        )
        viewModel.onInputChanged("客户询问第一次报价")
        viewModel.onSendMessage()

        orchestrator.enqueue(
            ChatStreamEvent.Delta("second"),
            ChatStreamEvent.Completed("second reply")
        )
        viewModel.onInputChanged("客户再次跟进报价")
        viewModel.onSendMessage()

        advanceUntilIdle()

        val assistants = viewModel.uiState.value.chatMessages.filter { it.role == ChatMessageRole.ASSISTANT }
        assertEquals(2, assistants.size)
        assertEquals("hi there", assistants[0].content)
        assertEquals("second reply", assistants[1].content)
    }

    private class FakeHomeOrchestrator : HomeOrchestrator {
        private val queue: ArrayDeque<List<ChatStreamEvent>> = ArrayDeque()

        fun enqueue(vararg events: ChatStreamEvent) {
            queue.addLast(events.toList())
        }

        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flow {
            val events = queue.removeFirstOrNull().orEmpty()
            events.forEach { emit(it) }
        }
    }

    private class FakeAiSessionRepository : AiSessionRepository {
        override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> = emptyList()
    }

    private class FakeDeviceConnectionManager : DeviceConnectionManager {
        override val state: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
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
        override val state: MutableStateFlow<MediaSyncState> = MutableStateFlow(MediaSyncState())
        override suspend fun triggerSync(): Result<Unit> = Result.Success(Unit)
    }

    private class FakeTranscriptionCoordinator : AudioTranscriptionCoordinator {
        override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> =
            Result.Error(UnsupportedOperationException())

        override suspend fun submitTranscription(
            audioAssetName: String,
            language: String,
            uploadPayload: AudioUploadPayload,
            sessionId: String?
        ): Result<String> = Result.Error(UnsupportedOperationException())

        override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> =
            flowOf(AudioTranscriptionJobState.Idle)
    }

    private class FakeAudioStorageRepository : AudioStorageRepository {
        override val audios: Flow<List<StoredAudio>> = flowOf(emptyList())
        override suspend fun importFromDevice(
            baseUrl: String,
            file: com.smartsales.feature.media.devicemanager.DeviceMediaFile
        ): StoredAudio {
            throw UnsupportedOperationException()
        }

        override suspend fun importFromPhone(uri: android.net.Uri): StoredAudio {
            throw UnsupportedOperationException()
        }

        override suspend fun delete(audioId: String) {}
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

    private class FakeSessionRepository : com.smartsales.feature.chat.AiSessionRepository {
        private val sessions = mutableMapOf<String, AiSessionSummary>()
        override val summaries: MutableStateFlow<List<AiSessionSummary>> = MutableStateFlow(emptyList())

        override suspend fun createNewChatSession(): AiSessionSummary {
            val summary = AiSessionSummary(
                id = "session-${sessions.size}",
                title = com.smartsales.core.metahub.SessionTitlePolicy.newChatPlaceholder(),
                lastMessagePreview = "",
                updatedAtMillis = 0L
            )
            upsert(summary)
            return summary
        }

        override suspend fun upsert(summary: AiSessionSummary) {
            sessions[summary.id] = summary
            summaries.value = sessions.values.toList()
        }

        override suspend fun delete(id: String) {
            sessions.remove(id)
            summaries.value = sessions.values.toList()
        }

        override suspend fun findById(id: String): AiSessionSummary? = sessions[id]

        override suspend fun updateTitle(id: String, newTitle: String, isUserEdited: Boolean) {
            sessions[id]?.let { existing ->
                sessions[id] = existing.copy(
                    title = newTitle,
                    isTitleUserEdited = existing.isTitleUserEdited || isUserEdited
                )
                summaries.value = sessions.values.toList()
            }
        }
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        private val state = kotlinx.coroutines.flow.MutableStateFlow(
            UserProfile("Tester", "tester@example.com", false)
        )
        override val profileFlow = state
        override suspend fun load(): UserProfile = state.value
        override suspend fun save(profile: UserProfile) {}
        override suspend fun clear() {}
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

}
