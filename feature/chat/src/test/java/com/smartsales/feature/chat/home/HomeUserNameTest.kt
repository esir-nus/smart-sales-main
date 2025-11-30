package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeUserNameTest.kt
// 模块：:feature:chat
// 说明：验证 HomeScreenViewModel 读取用户名称用于欢迎语
// 作者：创建于 2025-11-30

import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.history.ChatMessageEntity
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.media.MediaClipStatus
import com.smartsales.feature.media.MediaClip
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.chat.AiSessionRepository as SessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.usercenter.UserProfile
import com.smartsales.feature.usercenter.data.UserProfileRepository
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportManager
import com.smartsales.data.aicore.ExportResult
import com.smartsales.feature.chat.ChatShareHandler
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeUserNameTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uses display name when available`() = runTest(dispatcher) {
        val profileRepo = FakeUserProfileRepository(
            UserProfile(displayName = "小李", email = "xiao.li@example.com", isGuest = false)
        )
        val vm = buildViewModel(profileRepo)
        advanceUntilIdle()

        assertEquals("小李", vm.uiState.value.userName)
    }

    @Test
    fun `falls back to email prefix when no display name`() = runTest(dispatcher) {
        val profileRepo = FakeUserProfileRepository(
            UserProfile(displayName = "", email = "alice@example.com", isGuest = false)
        )
        val vm = buildViewModel(profileRepo)
        advanceUntilIdle()

        assertEquals("alice", vm.uiState.value.userName)
    }

    @Test
    fun `uses generic name when guest`() = runTest(dispatcher) {
        val profileRepo = FakeUserProfileRepository(
            UserProfile(displayName = "", email = "", isGuest = true)
        )
        val vm = buildViewModel(profileRepo)
        advanceUntilIdle()

        assertEquals("用户", vm.uiState.value.userName)
    }

    private fun buildViewModel(profileRepo: UserProfileRepository): HomeScreenViewModel {
        return HomeScreenViewModel(
            aiChatService = object : AiChatService {
                override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flowOf()
            },
            aiSessionRepository = object : AiSessionRepository {
                override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> = emptyList()
            },
            deviceConnectionManager = object : DeviceConnectionManager {
                override val state: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
                override fun selectPeripheral(peripheral: com.smartsales.feature.connectivity.BlePeripheral) = Unit
                override suspend fun startPairing(peripheral: com.smartsales.feature.connectivity.BlePeripheral, credentials: com.smartsales.feature.connectivity.WifiCredentials): com.smartsales.core.util.Result<Unit> =
                    com.smartsales.core.util.Result.Error(UnsupportedOperationException())

                override suspend fun retry(): com.smartsales.core.util.Result<Unit> =
                    com.smartsales.core.util.Result.Error(UnsupportedOperationException())

                override fun forgetDevice() = Unit
                override suspend fun requestHotspotCredentials(): com.smartsales.core.util.Result<com.smartsales.feature.connectivity.WifiCredentials> =
                    com.smartsales.core.util.Result.Error(UnsupportedOperationException())

                override suspend fun queryNetworkStatus(): com.smartsales.core.util.Result<com.smartsales.feature.connectivity.DeviceNetworkStatus> =
                    com.smartsales.core.util.Result.Error(UnsupportedOperationException())
            },
            mediaSyncCoordinator = object : MediaSyncCoordinator {
                override val state: MutableStateFlow<MediaSyncState> = MutableStateFlow(
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

                override suspend fun triggerSync(): com.smartsales.core.util.Result<Unit> = com.smartsales.core.util.Result.Success(Unit)
            },
            transcriptionCoordinator = object : AudioTranscriptionCoordinator {
                override suspend fun uploadAudio(file: java.io.File): com.smartsales.core.util.Result<AudioUploadPayload> =
                    com.smartsales.core.util.Result.Error(UnsupportedOperationException())

                override suspend fun submitTranscription(
                    audioAssetName: String,
                    language: String,
                    uploadPayload: AudioUploadPayload
                ): com.smartsales.core.util.Result<String> = com.smartsales.core.util.Result.Error(UnsupportedOperationException())

                override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> = flowOf(AudioTranscriptionJobState.Idle)
            },
            quickSkillCatalog = object : QuickSkillCatalog {
                override fun homeQuickSkills(): List<QuickSkillDefinition> = emptyList()
            },
            chatHistoryRepository = object : ChatHistoryRepository {
                override suspend fun loadLatestSession(sessionId: String): List<ChatMessageEntity> = emptyList()
                override suspend fun saveMessages(sessionId: String, messages: List<ChatMessageEntity>) = Unit
                override suspend fun deleteSession(sessionId: String) = Unit
            },
            sessionRepository = object : SessionRepository {
                override val summaries: MutableStateFlow<List<AiSessionSummary>> = MutableStateFlow(emptyList())
                override suspend fun upsert(summary: AiSessionSummary) {}
                override suspend fun delete(id: String) {}
                override suspend fun findById(id: String): AiSessionSummary? = null
            },
            userProfileRepository = profileRepo,
            exportManager = object : ExportManager {
                override suspend fun exportMarkdown(markdown: String, format: ExportFormat): com.smartsales.core.util.Result<ExportResult> =
                    com.smartsales.core.util.Result.Success(
                        ExportResult("demo.pdf", "application/pdf", ByteArray(0))
                    )
            },
            shareHandler = object : ChatShareHandler {
                override suspend fun copyMarkdown(markdown: String): com.smartsales.core.util.Result<Unit> =
                    com.smartsales.core.util.Result.Success(Unit)

                override suspend fun copyAssistantReply(text: String): com.smartsales.core.util.Result<Unit> =
                    com.smartsales.core.util.Result.Success(Unit)

                override suspend fun shareExport(result: ExportResult): com.smartsales.core.util.Result<Unit> =
                    com.smartsales.core.util.Result.Success(Unit)
            }
        )
    }

    private class FakeUserProfileRepository(
        private var profile: UserProfile
    ) : UserProfileRepository {
        override suspend fun load(): UserProfile = profile
        override suspend fun save(profile: UserProfile) {
            this.profile = profile
        }

        override suspend fun clear() {
            profile = UserProfile("", "", true)
        }
    }
}
