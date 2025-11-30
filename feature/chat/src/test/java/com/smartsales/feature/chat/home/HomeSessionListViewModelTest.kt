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
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.chat.home.TranscriptionChatRequest
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeSessionListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var fakeHistoryRepository: FakeChatHistoryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        sessionRepository = FakeSessionRepository()
        fakeHistoryRepository = FakeChatHistoryRepository()
        viewModel = HomeScreenViewModel(
            aiChatService = FakeAiChatService(),
            aiSessionRepository = FakeAiSessionRepository(),
            deviceConnectionManager = FakeDeviceConnectionManager(),
            mediaSyncCoordinator = FakeMediaSyncCoordinator(),
            transcriptionCoordinator = FakeTranscriptionCoordinator(),
            quickSkillCatalog = FakeQuickSkillCatalog(),
            chatHistoryRepository = fakeHistoryRepository,
            sessionRepository = sessionRepository,
            userProfileRepository = FakeUserProfileRepository()
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
            updatedAtMillis = Long.MAX_VALUE
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
        val transcriptItem = sessions.first { it.id != "home-session" }
        assertTrue(transcriptItem.title.startsWith("通话分析 – demo.wav"))
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

    private class FakeAiChatService : AiChatService {
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flowOf()
    }

    private class FakeAiSessionRepository : com.smartsales.feature.chat.home.AiSessionRepository {
        override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> =
            emptyList()
    }

    private class FakeSessionRepository : SessionRepository {
        private val sessions = mutableMapOf<String, AiSessionSummary>()
        override val summaries: MutableStateFlow<List<AiSessionSummary>> =
            MutableStateFlow(emptyList())

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
            uploadPayload: AudioUploadPayload
        ): Result<String> = Result.Success("job-ignored")

        override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> =
            flowOf(AudioTranscriptionJobState.Completed(jobId, "## 完成"))
    }

    private class FakeUserProfileRepository : com.smartsales.feature.usercenter.data.UserProfileRepository {
        private var profile = com.smartsales.feature.usercenter.UserProfile(
            displayName = "会话用户",
            email = "session@example.com",
            isGuest = false
        )
        override suspend fun load(): com.smartsales.feature.usercenter.UserProfile = profile
        override suspend fun save(profile: com.smartsales.feature.usercenter.UserProfile) {
            this.profile = profile
        }

        override suspend fun clear() {
            profile = com.smartsales.feature.usercenter.UserProfile("", "", true)
        }
    }
}
