package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeGeneralMetadataFlowTest.kt
// 模块：:feature:chat
// 说明：验证 GENERAL 首条回复附带 JSON 尾巴时，能够解析元数据、写入 MetaHub 并触发占位标题自动改名
// 作者：创建于 2025-12-09

import android.net.Uri
import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionTitlePolicy
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.data.aicore.ExportResult
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.InMemoryAiParaSettingsRepository
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.feature.chat.InMemoryAiSessionRepository
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.history.ChatMessageEntity
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.chat.title.SessionTitleResolver
import com.smartsales.feature.chat.title.TitleCandidate
import com.smartsales.feature.chat.title.TitleResolver
import com.smartsales.feature.chat.title.TitleSource
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.media.MediaClip
import com.smartsales.feature.media.MediaClipStatus
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.media.audiofiles.AudioOrigin
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioUploadPayload
import com.smartsales.feature.media.audiofiles.StoredAudio
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import com.smartsales.feature.usercenter.UserProfile
import com.smartsales.feature.usercenter.data.UserProfileRepository
import com.smartsales.feature.chat.testutil.TestContext
import com.smartsales.feature.chat.testutil.buildNoopXfyunVoiceprintApi
import com.smartsales.feature.chat.testutil.NoopDebugOrchestrator
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeGeneralMetadataFlowTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var orchestrator: QueueingHomeOrchestrator
    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var metaHub: InMemoryMetaHub
    private lateinit var sessionRepository: InMemoryAiSessionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        orchestrator = QueueingHomeOrchestrator()
        metaHub = InMemoryMetaHub()
        sessionRepository = InMemoryAiSessionRepository()
        viewModel = HomeScreenViewModel(
            appContext = TestContext(),
            homeOrchestrator = orchestrator,
            aiSessionRepository = FakeAiSessionRepository(),
            deviceConnectionManager = FakeDeviceConnectionManager(),
            mediaSyncCoordinator = FakeMediaSyncCoordinator(),
            transcriptionCoordinator = FakeTranscriptionCoordinator(),
            audioStorageRepository = FakeAudioStorageRepository(),
            quickSkillCatalog = EmptyQuickSkillCatalog(),
            chatHistoryRepository = FakeChatHistoryRepository(),
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
    fun `general first reply with json tail updates metadata and auto title`() = runTest(dispatcher) {
        val jsonTail =
            """{"main_person":"罗总","short_summary":"首次到店沟通","summary_title_6chars":"罗总首沟","location":"上海"}"""
        orchestrator.enqueue(
            ChatStreamEvent.Completed("好的，我会帮你梳理重点。\n$jsonTail")
        )

        viewModel.onInputChanged("客户询问报价和试驾安排")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.currentSession.id
        val stored = metaHub.getSession(sessionId)
        assertNotNull(stored)
        stored!!
        assertEquals("罗总", stored.mainPerson)
        assertEquals("首次到店沟通", stored.shortSummary)
        assertEquals("罗总首沟", stored.summaryTitle6Chars)
        assertEquals(AnalysisSource.GENERAL_FIRST_REPLY, stored.latestMajorAnalysisSource)
        assertNotNull(stored.latestMajorAnalysisAt)

        val title = sessionRepository.findById(sessionId)?.title
        assertNotNull(title)
        assertFalse(SessionTitlePolicy.isPlaceholder(title))
        assertTrue(title!!.contains("罗总"))
    }

    @Test
    fun `second general reply json is ignored when first reply has no metadata`() = runTest(dispatcher) {
        val jsonTail =
            """{"main_person":"李总","short_summary":"补充了预算范围","summary_title_6chars":"预算沟通"}"""
        orchestrator.enqueue(ChatStreamEvent.Completed("好的，我再想想。")) // 第一次无 JSON

        viewModel.onInputChanged("客户打招呼")
        viewModel.onSendMessage()
        advanceUntilIdle()

        orchestrator.enqueue(ChatStreamEvent.Completed("这是补充信息。\n$jsonTail"))
        viewModel.onInputChanged("客户补充预算信息")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.currentSession.id
        val stored = metaHub.getSession(sessionId)
        assertNull("首条无 JSON 且仅首条解析规则下，不应写入元数据", stored)

        val title = sessionRepository.findById(sessionId)?.title
        assertNotNull(title)
        assertTrue("首条助手无元数据时不应自动改名", SessionTitlePolicy.isPlaceholder(title))
    }

    @Test
    fun `general replies without json keep placeholder and skip metadata`() = runTest(dispatcher) {
        orchestrator.enqueue(ChatStreamEvent.Completed("好的，已收到。"))
        viewModel.onInputChanged("客户询问价格和交期")
        viewModel.onSendMessage()
        advanceUntilIdle()

        orchestrator.enqueue(ChatStreamEvent.Completed("再确认一下。"))
        viewModel.onInputChanged("客户补充需要发样品")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.currentSession.id
        val stored = metaHub.getSession(sessionId)
        assertEquals("无有效 JSON 不写入元数据", null, stored)

        val title = sessionRepository.findById(sessionId)?.title
        assertNotNull(title)
        assertTrue(SessionTitlePolicy.isPlaceholder(title))
    }

    @Test
    fun `metadata tag is preferred over trailing json`() = runTest(dispatcher) {
        val tagged = """
            <Visible2User>这条只展示给用户</Visible2User>
            <Metadata>{"main_person":"李总","short_summary":"带标签元数据","summary_title_6chars":"李总标签"}</Metadata>
        """.trimIndent()
        orchestrator.enqueue(ChatStreamEvent.Completed(tagged))

        viewModel.onInputChanged("客户询问方案")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.currentSession.id
        val stored = metaHub.getSession(sessionId)
        assertNotNull(stored)
        stored!!
        assertEquals("李总", stored.mainPerson)
        assertEquals("带标签元数据", stored.shortSummary)
        assertEquals("李总标签", stored.summaryTitle6Chars)
    }

    @Test
    fun `invalid metadata tag falls back to last json`() = runTest(dispatcher) {
        val text = """
            <Visible2User>请参考</Visible2User>
            <Metadata>{invalid-json}</Metadata>
            {"main_person":"赵总","short_summary":"尾部元数据"}
        """.trimIndent()
        orchestrator.enqueue(ChatStreamEvent.Completed(text))

        viewModel.onInputChanged("客户追加信息")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.currentSession.id
        val stored = metaHub.getSession(sessionId)
        assertNotNull(stored)
        stored!!
        assertEquals("赵总", stored.mainPerson)
        assertEquals("尾部元数据", stored.shortSummary)
    }

    @Test
    fun `general bubble hides json tail`() = runTest(dispatcher) {
        val jsonTail =
            """{"main_person":"罗总","short_summary":"首次到店沟通","summary_title_6chars":"罗总首沟"}"""
        orchestrator.enqueue(ChatStreamEvent.Completed("这里是自然语言内容。\n$jsonTail"))

        viewModel.onInputChanged("客户生成总结")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.chatMessages.lastOrNull { it.role == ChatMessageRole.ASSISTANT }
        assertNotNull(assistant)
        assistant!!
        assertFalse(assistant.content.contains("{"))
        assertFalse(assistant.content.contains("main_person"))
        assertTrue(assistant.content.contains("自然语言内容"))
    }

    @Test
    fun `only first general reply parses metadata`() = runTest(dispatcher) {
        val firstJson =
            """{"main_person":"罗总","short_summary":"首次到店沟通","summary_title_6chars":"罗总首沟"}"""
        val secondJson =
            """{"main_person":"李总","short_summary":"补充预算","summary_title_6chars":"预算沟通"}"""
        orchestrator.enqueue(ChatStreamEvent.Completed("首条带 JSON\n$firstJson"))
        viewModel.onInputChanged("客户第一次沟通")
        viewModel.onSendMessage()
        advanceUntilIdle()

        orchestrator.enqueue(ChatStreamEvent.Completed("二条不应触发元数据\n$secondJson"))
        viewModel.onInputChanged("客户补充预算信息")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.currentSession.id
        val stored = metaHub.getSession(sessionId)
        assertEquals("罗总", stored?.mainPerson)
        assertEquals(AnalysisSource.GENERAL_FIRST_REPLY, stored?.latestMajorAnalysisSource)
    }

    @Test
    fun `manual rename flag survives session summary refresh`() = runTest(dispatcher) {
        advanceUntilIdle()
        val sessionId = viewModel.uiState.value.currentSession.id

        sessionRepository.updateTitle(sessionId, "自定义标题", isUserEdited = true)
        advanceUntilIdle()

        orchestrator.enqueue(ChatStreamEvent.Completed("助手回复"))
        viewModel.onInputChanged("用户提问")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val stored = sessionRepository.findById(sessionId)
        assertNotNull(stored)
        assertTrue(stored!!.isTitleUserEdited)

        val candidate = TitleCandidate(
            name = "李总",
            title6 = "预算沟通",
            source = TitleSource.GENERAL,
            createdAt = System.currentTimeMillis()
        )
        val resolved = TitleResolver.resolveTitle(stored, candidate, SessionMetadata(sessionId))
        assertNull(resolved)
    }

    @Test
    fun `history snippet prefers metadata short summary`() = runTest(dispatcher) {
        val jsonTail =
            """{"main_person":"罗总","short_summary":"采购进展讨论","summary_title_6chars":"采购进展"}"""
        orchestrator.enqueue(ChatStreamEvent.Completed("这里是自然语言内容。\n$jsonTail"))

        viewModel.onInputChanged("客户生成总结")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val sessionId = viewModel.uiState.value.currentSession.id
        val item = viewModel.uiState.value.sessionList.firstOrNull { it.id == sessionId }
        assertNotNull(item)
        assertEquals("采购进展讨论", item?.lastMessagePreview)
    }

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

    private class EmptyQuickSkillCatalog : QuickSkillCatalog {
        override fun homeQuickSkills(): List<QuickSkillDefinition> = emptyList()
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

        override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> = flowOf(AudioTranscriptionJobState.Idle)
    }

    private class FakeAudioStorageRepository : AudioStorageRepository {
        override val audios: Flow<List<StoredAudio>> = MutableStateFlow(emptyList())
        override suspend fun importFromDevice(baseUrl: String, file: DeviceMediaFile): StoredAudio {
            throw UnsupportedOperationException()
        }

        override suspend fun importFromPhone(uri: Uri): StoredAudio {
            throw UnsupportedOperationException()
        }

        override suspend fun delete(audioId: String) {}
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
        ): Result<ExportResult> = Result.Success(ExportResult("demo.csv", "text/csv", ByteArray(0)))
    }

    private class FakeShareHandler : ChatShareHandler {
        override suspend fun copyMarkdown(markdown: String): Result<Unit> = Result.Success(Unit)
        override suspend fun copyAssistantReply(text: String): Result<Unit> = Result.Success(Unit)
        override suspend fun shareExport(result: ExportResult): Result<Unit> = Result.Success(Unit)
    }
}
