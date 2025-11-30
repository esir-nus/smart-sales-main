package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeExportActionsTest.kt
// 模块：:feature:chat
// 说明：验证 Home 智能分析与导出 PDF 走现有 AI 与 ExportManager 流程
// 作者：创建于 2025-12-01

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportManager
import com.smartsales.data.aicore.ExportResult
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.DefaultQuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillId
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeExportActionsTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var aiChatService: RecordingAiChatService
    private lateinit var exportManager: RecordingExportManager
    private lateinit var shareHandler: RecordingShareHandler
    private lateinit var viewModel: HomeScreenViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        aiChatService = RecordingAiChatService()
        exportManager = RecordingExportManager()
        shareHandler = RecordingShareHandler()
        viewModel = HomeScreenViewModel(
            aiChatService = aiChatService,
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
            },
            mediaSyncCoordinator = object : MediaSyncCoordinator {
                override val state: MutableStateFlow<MediaSyncState> = MutableStateFlow(MediaSyncState())
                override suspend fun triggerSync(): Result<Unit> = Result.Success(Unit)
            },
            transcriptionCoordinator = object : AudioTranscriptionCoordinator {
                override suspend fun uploadAudio(file: File): Result<AudioUploadPayload> = Result.Error(UnsupportedOperationException())
                override suspend fun submitTranscription(audioAssetName: String, language: String, uploadPayload: AudioUploadPayload): Result<String> =
                    Result.Error(UnsupportedOperationException())

                override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> = flowOf(AudioTranscriptionJobState.Idle)
            },
            quickSkillCatalog = DefaultQuickSkillCatalog(),
            chatHistoryRepository = object : ChatHistoryRepository {
                override suspend fun loadLatestSession(sessionId: String): List<ChatMessageEntity> = emptyList()
                override suspend fun saveMessages(sessionId: String, messages: List<ChatMessageEntity>) = Unit
                override suspend fun deleteSession(sessionId: String) = Unit
            },
            sessionRepository = object : com.smartsales.feature.chat.AiSessionRepository {
                override val summaries: MutableStateFlow<List<AiSessionSummary>> = MutableStateFlow(emptyList())
                override suspend fun upsert(summary: AiSessionSummary) {}
                override suspend fun delete(id: String) {}
                override suspend fun findById(id: String): AiSessionSummary? = null
            },
            userProfileRepository = object : UserProfileRepository {
                override suspend fun load(): UserProfile = UserProfile("测试", "test@example.com", false)
                override suspend fun save(profile: UserProfile) {}
                override suspend fun clear() {}
            },
            exportManager = exportManager,
            shareHandler = shareHandler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `smart analysis uses quick skill and appends assistant message`() = runTest(dispatcher) {
        viewModel.onSmartAnalysisClicked()
        advanceUntilIdle()

        assertEquals(QuickSkillId.SUMMARIZE_LAST_MEETING, aiChatService.lastRequest?.quickSkillId)
        assertTrue(viewModel.uiState.value.chatMessages.any { it.role == ChatMessageRole.ASSISTANT && !it.isStreaming })
    }

    @Test
    fun `export pdf uses analysis markdown and shares`() = runTest(dispatcher) {
        viewModel.onSmartAnalysisClicked()
        advanceUntilIdle()

        viewModel.onExportPdfClicked()
        advanceUntilIdle()

        assertEquals("分析结果", exportManager.lastMarkdown)
        assertTrue(shareHandler.shared)
        assertTrue(!viewModel.uiState.value.exportInProgress)
    }

    private class RecordingAiChatService : AiChatService {
        var lastRequest: ChatRequest? = null
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
            lastRequest = request
            return flow {
                emit(ChatStreamEvent.Completed("分析结果"))
            }
        }
    }

    private class RecordingExportManager : ExportManager {
        var lastMarkdown: String? = null
        override suspend fun exportMarkdown(markdown: String, format: ExportFormat): Result<ExportResult> {
            lastMarkdown = markdown
            return Result.Success(ExportResult("demo.pdf", "application/pdf", ByteArray(0)))
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
}
