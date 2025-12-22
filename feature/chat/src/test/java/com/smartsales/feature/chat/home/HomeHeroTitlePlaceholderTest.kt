package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeHeroTitlePlaceholderTest.kt
// 模块：:feature:chat
// 说明：验证空白会话展示欢迎卡时标题回退为占位文本，确保后续自动改名生效
// 作者：创建于 2025-12-08

import android.net.Uri
import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionTitlePolicy
import com.smartsales.core.metahub.TokenUsage
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.data.aicore.ExportResult
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.InMemoryAiParaSettingsRepository
import com.smartsales.feature.chat.AiSessionRepository as SessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.history.ChatMessageEntity
import com.smartsales.feature.chat.title.SessionTitleResolver
import com.smartsales.feature.chat.home.AiSessionRepository as HomeMessageRepository
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.media.MediaClip
import com.smartsales.feature.media.MediaClipStatus
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.media.audiofiles.StoredAudio
import com.smartsales.feature.usercenter.UserProfile
import com.smartsales.feature.usercenter.data.UserProfileRepository
import com.smartsales.feature.chat.testutil.TestContext
import com.smartsales.feature.chat.testutil.buildNoopXfyunVoiceprintApi
import com.smartsales.feature.chat.testutil.NoopDebugOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
class HomeHeroTitlePlaceholderTest {

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
    fun `hero session forces placeholder title`() = runTest(dispatcher) {
        val sessionRepo = FakeSessionRepository(
            listOf(
                AiSessionSummary(
                    id = "home-session",
                    title = "罗总_报价跟进_12/08",
                    lastMessagePreview = "",
                    updatedAtMillis = 0L
                )
            )
        )
        val vm = buildViewModel(sessionRepo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.showWelcomeHero)
        assertTrue(state.chatMessages.isEmpty())
        assertTrue(SessionTitlePolicy.isPlaceholder(state.currentSession.title))
        assertEquals(SessionTitlePolicy.PLACEHOLDER_TITLE, state.currentSession.title)
        assertTrue(state.currentSession.id != "home-session")
        // 历史会话仍保留原始标题
        assertEquals("罗总_报价跟进_12/08", sessionRepo.findById("home-session")?.title)
    }

    private fun buildViewModel(sessionRepo: SessionRepository): HomeScreenViewModel {
        val ctx = TestContext()
        val metaHub = object : MetaHub {
            override suspend fun upsertSession(metadata: SessionMetadata) {}
            override suspend fun getSession(sessionId: String): SessionMetadata? = null
            override suspend fun upsertTranscript(metadata: TranscriptMetadata) {}
            override suspend fun getTranscriptBySession(sessionId: String): TranscriptMetadata? = null
            override suspend fun upsertExport(metadata: ExportMetadata) {}
            override suspend fun getExport(sessionId: String): ExportMetadata? = null
            override suspend fun logUsage(usage: TokenUsage) {}
        }
        return HomeScreenViewModel(
            appContext = ctx,
            homeOrchestrator = object : HomeOrchestrator {
                override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flowOf()
            },
            aiSessionRepository = object : HomeMessageRepository {
                override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> = emptyList()
            },
            deviceConnectionManager = object : DeviceConnectionManager {
                override val state: MutableStateFlow<ConnectionState> =
                    MutableStateFlow(ConnectionState.Disconnected)

                override fun selectPeripheral(peripheral: com.smartsales.feature.connectivity.BlePeripheral) = Unit
                override suspend fun startPairing(
                    peripheral: com.smartsales.feature.connectivity.BlePeripheral,
                    credentials: com.smartsales.feature.connectivity.WifiCredentials
                ): Result<Unit> = Result.Error(UnsupportedOperationException())

                override suspend fun retry(): Result<Unit> = Result.Error(UnsupportedOperationException())
                override fun forgetDevice() = Unit
                override suspend fun requestHotspotCredentials(): Result<com.smartsales.feature.connectivity.WifiCredentials> =
                    Result.Error(UnsupportedOperationException())

                override suspend fun queryNetworkStatus(): Result<com.smartsales.feature.connectivity.DeviceNetworkStatus> =
                    Result.Error(UnsupportedOperationException())

                override fun scheduleAutoReconnectIfNeeded() {}
                override fun forceReconnectNow() {}
            },
            mediaSyncCoordinator = object : MediaSyncCoordinator {
                override val state: MutableStateFlow<MediaSyncState> = MutableStateFlow(
                    MediaSyncState(
                        items = listOf(
                            MediaClip(
                                id = "clip",
                                title = "demo",
                                customer = "c",
                                recordedAtMillis = 0L,
                                durationSeconds = 10,
                                sourceDeviceName = "device",
                                status = MediaClipStatus.Ready,
                                transcriptSource = "ready"
                            )
                        )
                    )
                )

                override suspend fun triggerSync(): Result<Unit> = Result.Success(Unit)
            },
            transcriptionCoordinator = object : AudioTranscriptionCoordinator {
                override suspend fun uploadAudio(file: java.io.File): Result<AudioUploadPayload> =
                    Result.Error(UnsupportedOperationException())

                override suspend fun submitTranscription(
                    audioAssetName: String,
                    language: String,
                    uploadPayload: AudioUploadPayload,
                    sessionId: String?
                ): Result<String> = Result.Error(UnsupportedOperationException())

                override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> =
                    flowOf(AudioTranscriptionJobState.Idle)

                override fun observeBatches(jobId: String): Flow<AudioTranscriptionBatchEvent> =
                    emptyFlow()
            },
            audioStorageRepository = object : AudioStorageRepository {
                override val audios: Flow<List<StoredAudio>> = MutableStateFlow(emptyList())
                override suspend fun importFromDevice(
                    baseUrl: String,
                    file: com.smartsales.feature.media.devicemanager.DeviceMediaFile
                ): StoredAudio {
                    throw UnsupportedOperationException()
                }

                override suspend fun importFromPhone(uri: Uri): StoredAudio {
                    throw UnsupportedOperationException()
                }

                override suspend fun delete(audioId: String) {}
            },
            quickSkillCatalog = object : QuickSkillCatalog {
                override fun homeQuickSkills(): List<QuickSkillDefinition> = emptyList()
            },
            chatHistoryRepository = object : ChatHistoryRepository {
                override suspend fun loadLatestSession(sessionId: String): List<ChatMessageEntity> = emptyList()
                override suspend fun saveMessages(sessionId: String, messages: List<ChatMessageEntity>) = Unit
                override suspend fun deleteSession(sessionId: String) = Unit
            },
            sessionRepository = sessionRepo,
            sessionTitleResolver = SessionTitleResolver(metaHub),
            userProfileRepository = object : UserProfileRepository {
                private val state = MutableStateFlow(UserProfile(displayName = "", email = "", isGuest = true))
                override val profileFlow: MutableStateFlow<UserProfile> = state
                override suspend fun load(): UserProfile = state.value
                override suspend fun save(profile: UserProfile) {
                    state.value = profile
                }

                override suspend fun clear() {
                    state.value = UserProfile("", "", true)
                }
            },
            exportOrchestrator = object : ExportOrchestrator {
                override suspend fun exportPdf(
                    sessionId: String,
                    markdown: String,
                    sessionTitle: String?,
                    userName: String?
                ): Result<ExportResult> = Result.Success(ExportResult("demo.pdf", "application/pdf", ByteArray(0)))

                override suspend fun exportCsv(
                    sessionId: String,
                    sessionTitle: String?,
                    userName: String?
                ): Result<ExportResult> =
                    Result.Success(ExportResult("demo.csv", "text/csv", ByteArray(0)))
            },
            shareHandler = object : ChatShareHandler {
                override suspend fun copyMarkdown(markdown: String): Result<Unit> = Result.Success(Unit)
                override suspend fun copyAssistantReply(text: String): Result<Unit> = Result.Success(Unit)
                override suspend fun shareExport(result: ExportResult): Result<Unit> = Result.Success(Unit)
            },
            metaHub = metaHub,
            debugOrchestrator = NoopDebugOrchestrator(),
            xfyunTraceStore = XfyunTraceStore(),
            tingwuTraceStore = TingwuTraceStore(),
            aiParaSettingsRepository = InMemoryAiParaSettingsRepository(),
            xfyunVoiceprintApi = buildNoopXfyunVoiceprintApi(),
        )
    }

    private class FakeSessionRepository(
        initial: List<AiSessionSummary>
    ) : SessionRepository {
        private val mutex = Mutex()
        override val summaries: MutableStateFlow<List<AiSessionSummary>> = MutableStateFlow(initial)

        override suspend fun createNewChatSession(): AiSessionSummary {
            val summary = AiSessionSummary(
                id = "session-${summaries.value.size}",
                title = SessionTitlePolicy.newChatPlaceholder(),
                lastMessagePreview = "",
                updatedAtMillis = 0L
            )
            upsert(summary)
            return summary
        }

        override suspend fun upsert(summary: AiSessionSummary) {
            mutex.withLock {
                summaries.value = (summaries.value.filterNot { it.id == summary.id } + summary).sortedByDescending {
                    it.updatedAtMillis
                }
            }
        }

        override suspend fun delete(id: String) {
            mutex.withLock {
                summaries.value = summaries.value.filterNot { it.id == id }
            }
        }

        override suspend fun findById(id: String): AiSessionSummary? = summaries.value.firstOrNull { it.id == id }

        override suspend fun updateTitle(id: String, newTitle: String, isUserEdited: Boolean) {
            mutex.withLock {
                summaries.value = summaries.value.map { summary ->
                    if (summary.id == id) {
                        summary.copy(
                            title = newTitle,
                            isTitleUserEdited = summary.isTitleUserEdited || isUserEdited
                        )
                    } else summary
                }
            }
        }
    }
}
