package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeSmartStreamingViewModelTest.kt
// 模块：:feature:chat
// 说明：验证 SMART_ANALYSIS 在 ViewModel 层的占位与流式行为符合 V4：无 Delta 展示，Completed 替换占位
// 作者：创建于 2025-12-08

import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportResult
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.core.QuickSkillId
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
import com.smartsales.feature.media.audiofiles.AudioOrigin
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.media.audiofiles.StoredAudio
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import android.net.Uri
import com.smartsales.feature.usercenter.UserProfile
import com.smartsales.feature.usercenter.data.UserProfileRepository
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.InMemoryAiParaSettingsRepository
import com.smartsales.feature.chat.testutil.TestContext
import com.smartsales.feature.chat.testutil.buildNoopXfyunVoiceprintApi
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeSmartStreamingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var orchestrator: QueueingHomeOrchestrator
    private lateinit var viewModel: HomeScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        orchestrator = QueueingHomeOrchestrator()
        val metaHub = InMemoryMetaHub()
        viewModel = HomeScreenViewModel(
            appContext = TestContext(),
            homeOrchestrator = orchestrator,
            aiSessionRepository = FakeAiSessionRepository(),
            deviceConnectionManager = FakeDeviceConnectionManager(),
            mediaSyncCoordinator = FakeMediaSyncCoordinator(),
            transcriptionCoordinator = FakeTranscriptionCoordinator(),
            audioStorageRepository = FakeAudioStorageRepository(),
            quickSkillCatalog = SmartOnlyQuickSkillCatalog(),
            chatHistoryRepository = FakeChatHistoryRepository(),
            sessionRepository = FakeSessionRepository(),
            sessionTitleResolver = SessionTitleResolver(metaHub),
            userProfileRepository = FakeUserProfileRepository(),
            metaHub = metaHub,
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
    fun `smart analysis inserts placeholder immediately`() = runTest(dispatcher) {
        orchestrator.enqueue(ChatStreamEvent.Completed("## 会话概要\n- 完成"))

        triggerSmartSend()

        val assistants = assistants()
        assertEquals(1, assistants.size)
        assertEquals("正在智能分析当前会话内容…", assistants.first().content)
        assertEquals(true, assistants.first().isStreaming)

        advanceUntilIdle()
    }

    @Test
    fun `smart analysis ignores deltas and only shows completed text`() = runTest(dispatcher) {
        orchestrator.enqueue(
            ChatStreamEvent.Delta("流式噪声"),
            ChatStreamEvent.Completed("## 会话概要\n- 最终结果")
        )

        triggerSmartSend()
        advanceUntilIdle()

        val assistants = assistants()
        assertEquals(1, assistants.size)
        assertEquals("## 会话概要\n- 最终结果", assistants.first().content)
        assertFalse(assistants.first().content.contains("流式噪声"))
    }

    @Test
    fun `smart analysis completed replaces placeholder in place`() = runTest(dispatcher) {
        orchestrator.enqueue(ChatStreamEvent.Completed("## 会话概要\n- 内容 A"))

        triggerSmartSend()
        val placeholderId = assistants().first().id
        advanceUntilIdle()

        val assistants = assistants()
        assertEquals(1, assistants.size)
        assertEquals(placeholderId, assistants.first().id)
        assertEquals("## 会话概要\n- 内容 A", assistants.first().content)
        assertFalse(assistants.first().isStreaming)
    }

    @Test
    fun `smart analysis failure replaces placeholder text`() = runTest(dispatcher) {
        val failure = "本次智能分析暂时不可用，请稍后重试。"
        orchestrator.enqueue(ChatStreamEvent.Completed(failure))

        triggerSmartSend()
        advanceUntilIdle()

        val assistants = assistants()
        assertEquals(1, assistants.size)
        assertEquals(failure, assistants.first().content)
        assertFalse(assistants.first().isStreaming)
    }

    private fun triggerSmartSend() {
        viewModel.onSelectQuickSkill(QuickSkillId.SMART_ANALYSIS)
        viewModel.onInputChanged("客户对话".repeat(150)) // 长文本，确保走智能分析主路径
        viewModel.onSendMessage()
    }

    private fun assistants(): List<ChatMessageUi> =
        viewModel.uiState.value.chatMessages.filter { it.role == ChatMessageRole.ASSISTANT }

    private class QueueingHomeOrchestrator : HomeOrchestrator {
        private val queue: ArrayDeque<List<ChatStreamEvent>> = ArrayDeque()

        fun enqueue(vararg events: ChatStreamEvent) {
            queue.addLast(events.toList())
        }

        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flow {
            val events = queue.removeFirstOrNull().orEmpty()
            events.forEach { emit(it) }
        }
    }

    private class SmartOnlyQuickSkillCatalog : QuickSkillCatalog {
        override fun homeQuickSkills(): List<QuickSkillDefinition> = listOf(
            QuickSkillDefinition(
                id = QuickSkillId.SMART_ANALYSIS,
                label = "智能分析",
                description = null,
                defaultPrompt = "",
                isRecommended = false,
                requiresAudioContext = false
            )
        )
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
        
        override suspend fun importFromDevice(baseUrl: String, file: DeviceMediaFile): StoredAudio {
            return StoredAudio(
                id = "fake-device",
                displayName = file.name,
                sizeBytes = 0L,
                durationMillis = null,
                timestampMillis = System.currentTimeMillis(),
                origin = AudioOrigin.DEVICE,
                localUri = Uri.parse("file://fake-device")
            )
        }

        override suspend fun importFromPhone(uri: Uri): StoredAudio {
            return StoredAudio(
                id = "fake-phone",
                displayName = "fake-phone-audio",
                sizeBytes = 0L,
                durationMillis = null,
                timestampMillis = System.currentTimeMillis(),
                origin = AudioOrigin.PHONE,
                localUri = uri
            )
        }

        override suspend fun delete(audioId: String) {
            // no-op
        }
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
                sessions[id] = existing.copy(title = newTitle)
                summaries.value = sessions.values.toList()
            }
        }
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        private val state = MutableStateFlow(
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
