package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeExportActionsTest.kt
// 模块：:feature:chat
// 说明：验证 Home 智能分析与导出 PDF 走现有 AI 与 ExportOrchestrator 流程
// 作者：创建于 2025-12-01

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.data.aicore.ExportResult
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.chat.core.DefaultQuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.title.SessionTitleResolver
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.history.ChatMessageEntity
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.usercenter.UserProfile
import com.smartsales.feature.usercenter.data.UserProfileRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertNotNull
import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.metahub.ExportMetadata
import com.smartsales.core.metahub.TokenUsage
import android.net.Uri
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.StoredAudio
import com.smartsales.feature.chat.testutil.TestContext

@OptIn(ExperimentalCoroutinesApi::class)
class HomeExportActionsTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var aiChatService: RecordingAiChatService
    private lateinit var exportOrchestrator: RecordingExportOrchestrator
    private lateinit var shareHandler: RecordingShareHandler
    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var homeOrchestrator: HomeOrchestrator
    private val appContext = TestContext()
    private lateinit var metaHub: FakeMetaHub

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        aiChatService = RecordingAiChatService()
        homeOrchestrator = RecordingHomeOrchestrator(aiChatService)
        exportOrchestrator = RecordingExportOrchestrator()
        shareHandler = RecordingShareHandler()
        metaHub = FakeMetaHub()
        viewModel = HomeScreenViewModel(
            appContext = appContext,
            homeOrchestrator = homeOrchestrator,
            aiSessionRepository = object : AiSessionRepository {
                override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> = emptyList()
            },
            deviceConnectionManager = object : DeviceConnectionManager {
                override val state: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
                override fun selectPeripheral(peripheral: com.smartsales.feature.connectivity.BlePeripheral) = Unit
                override suspend fun startPairing(peripheral: com.smartsales.feature.connectivity.BlePeripheral, credentials: WifiCredentials): Result<Unit> =
                    Result.Error(UnsupportedOperationException())

                override suspend fun retry(): Result<Unit> = Result.Error(UnsupportedOperationException())
                override fun forgetDevice() = Unit
                override suspend fun requestHotspotCredentials(): Result<WifiCredentials> = Result.Error(UnsupportedOperationException())
                override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> = Result.Error(UnsupportedOperationException())
                override fun scheduleAutoReconnectIfNeeded() {}
                override fun forceReconnectNow() {}
            },
            mediaSyncCoordinator = object : MediaSyncCoordinator {
                override val state: MutableStateFlow<MediaSyncState> = MutableStateFlow(MediaSyncState())
                override suspend fun triggerSync(): Result<Unit> = Result.Success(Unit)
            },
            transcriptionCoordinator = object : AudioTranscriptionCoordinator {
                override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> = Result.Error(UnsupportedOperationException())
                override suspend fun submitTranscription(
                    audioAssetName: String,
                    language: String,
                    uploadPayload: AudioUploadPayload,
                    sessionId: String?
                ): Result<String> =
                    Result.Error(UnsupportedOperationException())

                override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> = flowOf(AudioTranscriptionJobState.Idle)
            },
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
            quickSkillCatalog = DefaultQuickSkillCatalog(),
            chatHistoryRepository = object : ChatHistoryRepository {
                override suspend fun loadLatestSession(sessionId: String): List<ChatMessageEntity> = emptyList()
                override suspend fun saveMessages(sessionId: String, messages: List<ChatMessageEntity>) = Unit
                override suspend fun deleteSession(sessionId: String) = Unit
            },
            sessionRepository = object : com.smartsales.feature.chat.AiSessionRepository {
                override val summaries: MutableStateFlow<List<AiSessionSummary>> = MutableStateFlow(emptyList())
                override suspend fun createNewChatSession(): AiSessionSummary {
                    val summary = AiSessionSummary(
                        id = "session-1",
                        title = com.smartsales.core.metahub.SessionTitlePolicy.newChatPlaceholder(),
                        lastMessagePreview = "",
                        updatedAtMillis = 0L
                    )
                    upsert(summary)
                    return summary
                }
                override suspend fun upsert(summary: AiSessionSummary) {}
                override suspend fun delete(id: String) {}
                override suspend fun findById(id: String): AiSessionSummary? = null
                override suspend fun updateTitle(id: String, newTitle: String, isUserEdited: Boolean) {}
            },
            sessionTitleResolver = SessionTitleResolver(metaHub),
            userProfileRepository = object : UserProfileRepository {
                private val state = MutableStateFlow(UserProfile("测试", "test@example.com", false))
                override val profileFlow = state
                override suspend fun load(): UserProfile = state.value
                override suspend fun save(profile: UserProfile) {}
                override suspend fun clear() {}
            },
            metaHub = metaHub,
            exportOrchestrator = exportOrchestrator,
            shareHandler = shareHandler,
            xfyunTraceStore = XfyunTraceStore()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `smart analysis uses quick skill and appends assistant message`() = runTest(dispatcher) {
        val longInput = "请帮我总结汽车行业的市场趋势和风险要点。".repeat(15)
        viewModel.onInputChanged(longInput)
        viewModel.onSmartAnalysisClicked()
        viewModel.onSendMessage()
        advanceUntilIdle()

        assertEquals("SMART_ANALYSIS", aiChatService.lastRequest?.quickSkillId)
        assertTrue(
            viewModel.uiState.value.chatMessages.any {
                it.role == ChatMessageRole.ASSISTANT && !it.isStreaming
            }
        )
    }

    @Test
    fun `quick skill prefills prompt without sending`() = runTest(dispatcher) {
        viewModel.onSelectQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)

        assertEquals(
            "请总结当前对话的要点，并给出关键结论与建议。",
            viewModel.uiState.value.inputText
        )
        assertEquals(QuickSkillId.SUMMARIZE_LAST_MEETING, viewModel.uiState.value.selectedSkill?.id)
        assertTrue(viewModel.uiState.value.chatMessages.isEmpty())
    }

    @Test
    fun `export pdf uses analysis markdown and shares`() = runTest(dispatcher) {
        val longInput = "请帮我总结汽车行业的市场趋势和风险要点。".repeat(15)
        viewModel.onInputChanged(longInput)
        viewModel.onSmartAnalysisClicked()
        viewModel.onSendMessage()
        advanceUntilIdle()

        viewModel.onExportPdfClicked()
        advanceUntilIdle()

        assertEquals("智能分析结果\n\n分析结果", exportOrchestrator.lastPdfMarkdown)
        assertTrue(shareHandler.shared)
        assertTrue(!viewModel.uiState.value.exportInProgress)
    }

    @Test
    fun `export csv uses analysis markdown and shares`() = runTest(dispatcher) {
        val longInput = "请帮我总结汽车行业的市场趋势和风险要点。".repeat(15)
        viewModel.onInputChanged(longInput)
        viewModel.onSmartAnalysisClicked()
        viewModel.onSendMessage()
        advanceUntilIdle()

        viewModel.onExportCsvClicked()
        advanceUntilIdle()

        assertEquals(ExportFormat.CSV, exportOrchestrator.lastFormat)
        assertTrue(shareHandler.shared)
        assertTrue(!viewModel.uiState.value.exportInProgress)
    }

    @Test
    fun `export reuses cached analysis without rerun`() = runTest(dispatcher) {
        val longInput = "请帮我总结汽车行业的市场趋势和风险要点。".repeat(15)
        viewModel.onInputChanged(longInput)
        viewModel.onSmartAnalysisClicked()
        viewModel.onSendMessage()
        advanceUntilIdle()

        val callsAfterAnalysis = aiChatService.callCount

        viewModel.onExportPdfClicked()
        advanceUntilIdle()
        viewModel.onExportPdfClicked()
        advanceUntilIdle()

        assertEquals(callsAfterAnalysis, aiChatService.callCount)
    }

    @Test
    fun `analysis completion persists latest analysis marker to metahub`() = runTest(dispatcher) {
        metaHub.session = SessionMetadata(
            sessionId = "home-session",
            mainPerson = "罗总",
            summaryTitle6Chars = "初始标题"
        )
        val longInput = "请帮我总结汽车行业的市场趋势和风险要点。".repeat(20)
        viewModel.onInputChanged(longInput)
        viewModel.onSmartAnalysisClicked()
        viewModel.onSendMessage()
        advanceUntilIdle()

        waitForMetaHubUpdate()
        val saved = metaHub.session!!
        assertEquals("罗总", saved.mainPerson)
        assertNotNull(saved.latestMajorAnalysisMessageId)
        assertEquals(AnalysisSource.SMART_ANALYSIS_USER, saved.latestMajorAnalysisSource)
        assertTrue((saved.latestMajorAnalysisAt ?: 0L) > 0L)
    }

    @Test
    fun `auto analysis persists marker to metahub with auto source`() = runTest(dispatcher) {
        metaHub.session = SessionMetadata(sessionId = "home-session")
        val longInput = "自动分析触发内容".repeat(40)
        viewModel.onInputChanged(longInput)
        viewModel.onSendMessage()
        advanceUntilIdle()

        viewModel.onExportPdfClicked()
        advanceUntilIdle()
        waitForMetaHubUpdate()

        assertEquals(2, aiChatService.callCount)
        val saved = metaHub.session!!
        println("auto analysis saved meta=$saved")
        assertNotNull(saved.latestMajorAnalysisMessageId)
        assertEquals(AnalysisSource.SMART_ANALYSIS_AUTO, saved.latestMajorAnalysisSource)
        assertTrue((saved.latestMajorAnalysisAt ?: 0L) > 0L)
    }

    @Test
    fun `export skips auto analysis when metahub has latest analysis but vm cache empty`() = runTest(dispatcher) {
        metaHub.session = SessionMetadata(
            sessionId = "home-session",
            latestMajorAnalysisMessageId = "m1",
            lastUpdatedAt = 1L
        )

        viewModel.onExportPdfClicked()
        advanceUntilIdle()

        assertEquals(0, aiChatService.callCount)
        assertEquals(null, exportOrchestrator.lastFormat)
        assertTrue(viewModel.uiState.value.snackbarMessage?.contains("历史分析") == true)
    }

    private suspend fun waitForMetaHubUpdate() {
        repeat(20) {
            if (metaHub.session?.latestMajorAnalysisMessageId != null) return
            delay(10)
        }
    }

    @Test
    fun `export runs auto smart analysis once when no analysis and long content`() = runTest(dispatcher) {
        val longInput = "这是一个很长的对话片段，用于导出前自动分析。".repeat(20)
        viewModel.onInputChanged(longInput)
        viewModel.onSendMessage()
        advanceUntilIdle()

        val callsBeforeExport = aiChatService.callCount

        viewModel.onExportPdfClicked()
        advanceUntilIdle()

        assertEquals(callsBeforeExport + 1, aiChatService.callCount)
        assertEquals(ExportFormat.PDF, exportOrchestrator.lastFormat)
        assertEquals(1, exportOrchestrator.pdfCallCount)
        assertTrue(exportOrchestrator.lastPdfMarkdown?.contains("智能分析结果") == true)
        assertTrue(shareHandler.shared)
        assertTrue(!viewModel.uiState.value.exportInProgress)
    }

    @Test
    fun `low information input skips ai call and hints only once`() = runTest(dispatcher) {
        viewModel.onInputChanged("是")
        viewModel.onSendMessage()
        advanceUntilIdle()

        assertEquals(null, aiChatService.lastRequest)
        val assistantCountFirst = viewModel.uiState.value.chatMessages.count { it.role == ChatMessageRole.ASSISTANT }
        assertEquals(1, assistantCountFirst)

        viewModel.onInputChanged("好的")
        viewModel.onSendMessage()
        advanceUntilIdle()

        assertEquals(null, aiChatService.lastRequest)
        val assistantCountSecond = viewModel.uiState.value.chatMessages.count { it.role == ChatMessageRole.ASSISTANT }
        assertEquals(1, assistantCountSecond)
        val userCount = viewModel.uiState.value.chatMessages.count { it.role == ChatMessageRole.USER }
        assertEquals(0, userCount)
    }

    @Test
    fun `smart analysis with no analyzable content shows fallback without sending`() = runTest(dispatcher) {
        viewModel.onSelectQuickSkill(QuickSkillId.SMART_ANALYSIS)
        viewModel.onSendMessage()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSmartAnalysisMode)
        val assistantFallback = viewModel.uiState.value.chatMessages.lastOrNull { it.role == ChatMessageRole.ASSISTANT }
        assertTrue(assistantFallback?.content?.contains("内容太少") == true)
        assertEquals(null, aiChatService.lastRequest)
    }

    private class RecordingAiChatService : AiChatService {
        var lastRequest: ChatRequest? = null
        var callCount: Int = 0
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
            callCount += 1
            lastRequest = request
            return flow {
                emit(ChatStreamEvent.Completed("分析结果"))
            }
        }
    }

    private class RecordingHomeOrchestrator(
        private val delegate: AiChatService
    ) : HomeOrchestrator {
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> =
            delegate.streamChat(request)
    }

    private class RecordingExportOrchestrator : ExportOrchestrator {
        var lastPdfMarkdown: String? = null
        var lastFormat: ExportFormat? = null
        var pdfCallCount = 0
        var csvCallCount = 0
        override suspend fun exportPdf(
            sessionId: String,
            markdown: String,
            sessionTitle: String?,
            userName: String?
        ): Result<ExportResult> {
            lastPdfMarkdown = markdown
            lastFormat = ExportFormat.PDF
            pdfCallCount += 1
            return Result.Success(ExportResult("demo.pdf", "application/pdf", ByteArray(0)))
        }

        override suspend fun exportCsv(
            sessionId: String,
            sessionTitle: String?,
            userName: String?
        ): Result<ExportResult> {
            lastFormat = ExportFormat.CSV
            csvCallCount += 1
            return Result.Success(ExportResult("demo.csv", "text/csv", ByteArray(0)))
        }
    }

    private class RecordingShareHandler : ChatShareHandler {
        var shared: Boolean = false
        override suspend fun copyMarkdown(markdown: String): Result<Unit> = Result.Success(Unit)
        override suspend fun copyAssistantReply(text: String): Result<Unit> = Result.Success(Unit)
        override suspend fun shareExport(result: ExportResult): Result<Unit> {
            shared = true
            return Result.Success(Unit)
        }
    }

    private class FakeMetaHub : MetaHub {
        var session: SessionMetadata? = null
        override suspend fun upsertSession(metadata: SessionMetadata) {
            session = session?.mergeWith(metadata) ?: metadata
        }
        override suspend fun getSession(sessionId: String): SessionMetadata? = session
        override suspend fun upsertTranscript(metadata: TranscriptMetadata) {}
        override suspend fun getTranscriptBySession(sessionId: String): TranscriptMetadata? = null
        override suspend fun upsertExport(metadata: ExportMetadata) {}
        override suspend fun getExport(sessionId: String): ExportMetadata? = null
        override suspend fun logUsage(usage: TokenUsage) {}
    }
}
