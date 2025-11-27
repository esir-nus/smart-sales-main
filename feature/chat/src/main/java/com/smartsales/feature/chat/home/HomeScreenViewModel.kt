package com.smartsales.feature.chat.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.Result
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.AudioContextSummary
import com.smartsales.feature.chat.core.ChatHistoryItem
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.history.toEntity
import com.smartsales.feature.chat.history.toUiModel
import com.smartsales.feature.chat.AiSessionRepository as SessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.MediaClipStatus
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

private const val DEFAULT_SESSION_ID = "home-session"

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
// 模块：:feature:chat
// 说明：HomeScreen 的 UiState 模型与 ViewModel，实现聊天/快捷技能/设备信息更新逻辑
// 作者：创建于 2025-11-20

/** UI 模型：代表 Home 页里的一条聊天气泡。 */
data class ChatMessageUi(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatMessageRole,
    val content: String,
    val timestampMillis: Long,
    val isStreaming: Boolean = false,
    val hasError: Boolean = false
)

/** 区分用户与助手消息（Home 层级）。 */
enum class ChatMessageRole { USER, ASSISTANT }

/** UI 模型：输入框下方的快捷技能。 */
data class QuickSkillUi(
    val id: QuickSkillId,
    val label: String,
    val description: String? = null,
    val isRecommended: Boolean = false
)

/** UI 模型：设备横幅的轻量快照。 */
data class DeviceSnapshotUi(
    val deviceName: String? = null,
    val statusText: String,
    val connectionState: DeviceConnectionStateUi,
    val wifiName: String? = null,
    val serviceAddress: String? = null,
    val errorSummary: String? = null
)

/** 设备连接状态（用于 Home 横幅）。 */
enum class DeviceConnectionStateUi { DISCONNECTED, CONNECTING, WAITING_FOR_NETWORK, CONNECTED, ERROR }

/** UI 模型：音频/转写状态摘要。 */
data class AudioSummaryUi(
    val headline: String,
    val detail: String? = null,
    val syncedCount: Int = 0,
    val pendingUploadCount: Int = 0,
    val pendingTranscriptionCount: Int = 0,
    val lastSyncedAtMillis: Long? = null
)

/** Home 发出的单次导航请求。 */
sealed class HomeNavigationRequest {
    data object DeviceSetup : HomeNavigationRequest()
    data object DeviceManager : HomeNavigationRequest()
    data object AudioFiles : HomeNavigationRequest()
    data object ChatHistory : HomeNavigationRequest()
    data object UserCenter : HomeNavigationRequest()
}

data class CurrentSessionUi(
    val id: String,
    val title: String,
    val isTranscription: Boolean
)

/** UI 模型：会话列表的一条摘要。 */
data class SessionListItemUi(
    val id: String,
    val title: String,
    val lastMessagePreview: String,
    val updatedAtMillis: Long,
    val isCurrent: Boolean,
    val isTranscription: Boolean
)

/** Home 页的不可变 UI 状态。 */
data class HomeUiState(
    val chatMessages: List<ChatMessageUi> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isStreaming: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val quickSkills: List<QuickSkillUi> = emptyList(),
    val selectedSkill: QuickSkillUi? = null,
    val deviceSnapshot: DeviceSnapshotUi? = null,
    val audioSummary: AudioSummaryUi? = null,
    val snackbarMessage: String? = null,
    val chatErrorMessage: String? = null,
    val navigationRequest: HomeNavigationRequest? = null,
    val sessionList: List<SessionListItemUi> = emptyList(),
    val currentSession: CurrentSessionUi = CurrentSessionUi(
        id = DEFAULT_SESSION_ID,
        title = "新的聊天",
        isTranscription = false
    )
)

/** 外部依赖（除 AiChatService 外保留原有 stub）。 */
interface AiSessionRepository {
    suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi>
}

/** HomeScreenViewModel：驱动聊天、快捷技能以及设备/音频快照。 */
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val aiChatService: AiChatService,
    private val aiSessionRepository: AiSessionRepository,
    private val deviceConnectionManager: DeviceConnectionManager,
    private val mediaSyncCoordinator: MediaSyncCoordinator,
    private val transcriptionCoordinator: AudioTranscriptionCoordinator,
    private val quickSkillCatalog: QuickSkillCatalog,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val quickSkillDefinitions = quickSkillCatalog.homeQuickSkills()
    private val quickSkillDefinitionsById = quickSkillDefinitions.associateBy { it.id }
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    private var latestMediaSyncState: MediaSyncState? = null
    private var sessionId: String = DEFAULT_SESSION_ID
    private var latestSessionSummaries: List<AiSessionSummary> = emptyList()
    private var pendingSkillId: QuickSkillId? = null
    private var transcriptionJob: Job? = null

    init {
        // 从 catalog 加载快捷技能到状态
        val skills = quickSkillDefinitions.map { it.toUiModel() }
        _uiState.update { it.copy(quickSkills = skills) }
        setSession(DEFAULT_SESSION_ID)
        observeDeviceConnection()
        observeMediaSync()
        observeSessions()
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSendMessage() {
        sendMessageInternal(
            messageText = _uiState.value.inputText
        )
    }

    fun onSelectQuickSkill(skillId: QuickSkillId) {
        val definition = quickSkillDefinitionsById[skillId]
        if (definition == null) {
            _uiState.update { it.copy(snackbarMessage = "无法识别的快捷技能") }
            return
        }
        // 当前激活的快捷技能，用于展示顶部 Chip 和构造快捷提示词
        _uiState.update { state ->
            state.copy(selectedSkill = definition.toUiModel())
        }
        sendQuickSkill(definition)
    }

    fun onLoadMoreHistory() {
        if (_uiState.value.isLoadingHistory) return
        _uiState.update { it.copy(isLoadingHistory = true) }
        val topId = _uiState.value.chatMessages.firstOrNull()?.id
        viewModelScope.launch {
            runCatching { aiSessionRepository.loadOlderMessages(topId) }
                .onSuccess { older ->
                    _uiState.update {
                        it.copy(
                            chatMessages = older + it.chatMessages,
                            isLoadingHistory = false
                        )
                    }
                    persistMessagesAsync()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            snackbarMessage = error.message ?: "历史记录加载失败"
                        )
                    }
                }
        }
    }

    fun onRefreshDeviceAndAudio() {
        // Home 仅触发轻量刷新，真实状态仍由各模块推送
        viewModelScope.launch {
            when (val result = deviceConnectionManager.queryNetworkStatus()) {
                is Result.Error -> {
                    _uiState.update { state ->
                        val message = result.throwable.message ?: "刷新设备状态失败"
                        state.copy(snackbarMessage = state.snackbarMessage ?: message)
                    }
                }

                else -> Unit
            }
            when (val syncResult = mediaSyncCoordinator.triggerSync()) {
                is Result.Error -> {
                    _uiState.update { state ->
                        val message = syncResult.throwable.message ?: "刷新音频状态失败"
                        state.copy(snackbarMessage = state.snackbarMessage ?: message)
                    }
                }

                else -> Unit
            }
        }
    }

    fun onTapDeviceBanner() {
        val snapshot = _uiState.value.deviceSnapshot
        val destination = when (snapshot?.connectionState) {
            DeviceConnectionStateUi.CONNECTED -> HomeNavigationRequest.DeviceManager
            DeviceConnectionStateUi.CONNECTING,
            DeviceConnectionStateUi.DISCONNECTED,
            DeviceConnectionStateUi.WAITING_FOR_NETWORK,
            DeviceConnectionStateUi.ERROR,
            null -> HomeNavigationRequest.DeviceSetup
        }
        _uiState.update { it.copy(navigationRequest = destination) }
    }

    fun onTapAudioSummary() {
        if (_uiState.value.audioSummary != null) {
            _uiState.update { it.copy(navigationRequest = HomeNavigationRequest.AudioFiles) }
        }
    }

    fun onTranscriptionRequested(request: TranscriptionChatRequest) {
        viewModelScope.launch {
            val transcript = request.transcriptMarkdown ?: request.transcriptPreview
            val targetSessionId = request.sessionId ?: "session-${UUID.randomUUID()}"
            val title = "通话分析 – ${request.fileName}".take(40)
            val existing = chatHistoryRepository.loadLatestSession(targetSessionId)
            this@HomeScreenViewModel.sessionId = targetSessionId
            ensureSessionSummary(targetSessionId, title)
            _uiState.update {
                it.copy(
                    currentSession = CurrentSessionUi(
                        id = targetSessionId,
                        title = title,
                        isTranscription = true
                    ),
                    chatMessages = existing.map { item -> item.toUiModel() },
                    isLoadingHistory = false
                )
            }
            existing.lastOrNull()?.content?.let { updateSessionSummary(it) }
            if (!transcript.isNullOrBlank() && existing.isEmpty()) {
                val contextMessage = ChatMessageUi(
                    id = nextMessageId(),
                    role = ChatMessageRole.ASSISTANT,
                    content = buildTranscriptContext(request.fileName, transcript),
                    timestampMillis = System.currentTimeMillis()
                )
                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages + contextMessage,
                        snackbarMessage = null
                    )
                }
                persistMessagesAsync()
                updateSessionSummary(contextMessage.content)
                return@launch
            }
            if (transcript.isNullOrBlank()) {
                val introId = nextMessageId()
                val introMessage = ChatMessageUi(
                    id = introId,
                    role = ChatMessageRole.ASSISTANT,
                    content = "正在转写音频文件 ${request.fileName} ...",
                    timestampMillis = System.currentTimeMillis(),
                    isStreaming = true
                )
                _uiState.update { it.copy(chatMessages = _uiState.value.chatMessages + introMessage, snackbarMessage = null) }
                persistMessagesAsync()
                transcriptionJob?.cancel()
                transcriptionJob = launch {
                    transcriptionCoordinator.observeJob(request.jobId).collectLatest { state ->
                        when (state) {
                            AudioTranscriptionJobState.Idle -> Unit
                            is AudioTranscriptionJobState.InProgress -> updateAssistantMessage(introId) { msg ->
                                msg.copy(
                                    content = "正在转写音频文件 ${request.fileName} ... ${state.progressPercent}%",
                                    isStreaming = true
                                )
                            }

                            is AudioTranscriptionJobState.Completed -> {
                                updateAssistantMessage(introId, persistAfterUpdate = true) { msg ->
                                    msg.copy(
                                        content = "转写完成：${request.fileName}",
                                        isStreaming = false
                                    )
                                }
                                appendAssistantMessage(state.transcriptMarkdown)
                            }

                            is AudioTranscriptionJobState.Failed -> {
                                val reason = state.reason.ifBlank { "转写失败" }
                                updateAssistantMessage(introId, persistAfterUpdate = true) { msg ->
                                    msg.copy(
                                        content = "转写失败：$reason",
                                        isStreaming = false,
                                        hasError = true
                                    )
                                }
                                _uiState.update { it.copy(snackbarMessage = it.snackbarMessage ?: reason) }
                            }
                        }
                    }
                }
            }
        }
    }

    fun onOpenDrawer() {
        _uiState.update { it.copy(navigationRequest = HomeNavigationRequest.ChatHistory) }
    }

    fun onTapProfile() {
        _uiState.update { it.copy(navigationRequest = HomeNavigationRequest.UserCenter) }
    }

    fun onRetryMessage(messageId: String) {
        val failed = _uiState.value.chatMessages.firstOrNull { it.id == messageId } ?: return
        sendMessageInternal(
            messageText = failed.content,
        )
    }

    fun onDismissError() {
        _uiState.update { it.copy(snackbarMessage = null, chatErrorMessage = null) }
    }

    fun onNavigationConsumed() {
        _uiState.update { it.copy(navigationRequest = null) }
    }

    fun setSession(sessionId: String, titleOverride: String? = null, isTranscription: Boolean = false) {
        viewModelScope.launch {
            if (sessionId != this@HomeScreenViewModel.sessionId) {
                this@HomeScreenViewModel.sessionId = sessionId
                applySessionList()
                loadSession(sessionId)
            }
            val summary = ensureSessionSummary(sessionId, titleOverride)
            _uiState.update {
                it.copy(
                    currentSession = CurrentSessionUi(
                        id = sessionId,
                        title = summary.title,
                        isTranscription = isTranscription
                    )
                )
            }
        }
    }

    fun setQuickSkills(skills: List<QuickSkillUi>) {
        _uiState.update { it.copy(quickSkills = skills) }
    }

    fun clearSelectedSkill() {
        pendingSkillId = null
        _uiState.update { it.copy(selectedSkill = null) }
    }

    fun onNewChatClicked() {
        viewModelScope.launch {
            val newSessionId = "session-${UUID.randomUUID()}"
            sessionId = newSessionId
            val summary = ensureSessionSummary(newSessionId, titleOverride = "新的聊天")
            _uiState.update {
                it.copy(
                    currentSession = CurrentSessionUi(
                        id = newSessionId,
                        title = summary.title,
                        isTranscription = false
                    ),
                    chatMessages = emptyList(),
                    isLoadingHistory = false
                )
            }
            chatHistoryRepository.saveMessages(newSessionId, emptyList())
            applySessionList()
        }
    }

    private fun observeDeviceConnection() {
        // 监听连接模块输出，只读映射为 Home 的 snapshot 提示
        applyDeviceSnapshot(deviceConnectionManager.state.value)
        viewModelScope.launch {
            deviceConnectionManager.state.collectLatest { connection ->
                applyDeviceSnapshot(connection)
            }
        }
    }

    private fun applyDeviceSnapshot(connection: ConnectionState) {
        val snapshot = connection.toDeviceSnapshot()
        _uiState.update { state ->
            val shouldSurfaceError =
                snapshot.connectionState == DeviceConnectionStateUi.ERROR &&
                    snapshot.errorSummary != null &&
                    state.snackbarMessage == null
            state.copy(
                deviceSnapshot = snapshot,
                snackbarMessage = if (shouldSurfaceError) snapshot.errorSummary else state.snackbarMessage
            )
        }
    }

    private fun observeMediaSync() {
        // 监听媒体同步状态，提示录音/转写摘要
        applyAudioSummary(mediaSyncCoordinator.state.value)
        viewModelScope.launch {
            mediaSyncCoordinator.state.collectLatest { syncState ->
                applyAudioSummary(syncState)
            }
        }
    }

    private fun observeSessions() {
        viewModelScope.launch {
            sessionRepository.summaries.collectLatest { summaries ->
                latestSessionSummaries = summaries
                applySessionList()
            }
        }
    }

    private fun applySessionList() {
        val mapped = latestSessionSummaries.map { summary ->
            SessionListItemUi(
                id = summary.id,
                title = summary.title,
                lastMessagePreview = summary.lastMessagePreview,
                updatedAtMillis = summary.updatedAtMillis,
                isCurrent = summary.id == sessionId,
                isTranscription = summary.title.startsWith("通话分析 –")
            )
        }
        _uiState.update { it.copy(sessionList = mapped) }
    }

    private fun applyAudioSummary(syncState: MediaSyncState) {
        latestMediaSyncState = syncState
        val summary = syncState.toAudioSummaryUi()
        _uiState.update { state ->
            val shouldSurfaceError =
                syncState.errorMessage != null && state.snackbarMessage == null
            state.copy(
                audioSummary = summary,
                snackbarMessage = if (shouldSurfaceError) syncState.errorMessage else state.snackbarMessage
            )
        }
    }

    private fun loadSession(targetSessionId: String) {
        _uiState.update { it.copy(isLoadingHistory = true, chatMessages = emptyList()) }
        viewModelScope.launch {
            val saved = chatHistoryRepository.loadLatestSession(targetSessionId)
            if (saved.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        chatMessages = saved.map { item -> item.toUiModel() },
                        isLoadingHistory = false
                    )
                }
                saved.lastOrNull()?.content?.let { preview ->
                    updateSessionSummary(preview)
                }
            } else {
                _uiState.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    private fun persistMessagesAsync() {
        val snapshot = _uiState.value.chatMessages.map { it.toEntity(sessionId) }
        viewModelScope.launch {
            chatHistoryRepository.saveMessages(sessionId, snapshot)
        }
    }

    // 共享的发送 helper：输入框和快捷技能都走这里
    private fun sendMessageInternal(
        messageText: String,
    ) {
        val content = messageText.trim()
        if (content.isEmpty() || _uiState.value.isSending) return
        _uiState.update { it.copy(chatErrorMessage = null) }
        val quickSkill = pendingSkillId?.let { id ->
            quickSkillDefinitionsById[id]
        }
        if (quickSkill != null) {
            pendingSkillId = null
        }
        val quickSkillId = quickSkill?.id
        val userMessage = createUserMessage(content)
        val assistantPlaceholder = createAssistantPlaceholder()
        val audioContext = when {
            quickSkill?.requiresAudioContext == true -> {
                buildAudioContextSummary() ?: run {
                    // 没有最新录音可用时仍放行，但给出提示
                    _uiState.update { state ->
                        if (state.snackbarMessage == null) {
                            state.copy(snackbarMessage = "暂无可用音频上下文，已发送文本请求")
                        } else state
                    }
                    null
                }
            }

            else -> null
        }
        val newState = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + userMessage + assistantPlaceholder,
            inputText = "",
            isSending = true,
            isStreaming = true,
            snackbarMessage = null
        )
        _uiState.value = newState
        persistMessagesAsync()
        viewModelScope.launch {
            updateSessionSummary(userMessage.content)
        }
        val request = buildChatRequest(content, quickSkillId, newState.chatMessages, audioContext)
        startStreamingResponse(request, assistantPlaceholder.id)
    }

    /** 启动 streaming，更新最后一条助手气泡。 */
    private fun startStreamingResponse(request: ChatRequest, assistantId: String) {
        viewModelScope.launch {
            aiChatService.streamChat(request).collect { event ->
                when (event) {
                    is ChatStreamEvent.Delta -> {
                        // 处理 streaming token，追加到当前助手消息
                        updateAssistantMessage(assistantId) { msg ->
                            msg.copy(content = msg.content + event.token)
                        }
                    }

                    is ChatStreamEvent.Completed -> {
                        // 完成：关闭 streaming，写入完整文本
                        updateAssistantMessage(assistantId, persistAfterUpdate = true) { msg ->
                            msg.copy(content = event.fullText, isStreaming = false)
                        }
                        _uiState.update { it.copy(isSending = false, isStreaming = false) }
                        viewModelScope.launch {
                            updateSessionSummary(event.fullText)
                            applySessionList()
                        }
                    }

                    is ChatStreamEvent.Error -> {
                        // 错误：标记消息失败并弹出提示
                        updateAssistantMessage(assistantId, persistAfterUpdate = true) { msg ->
                            msg.copy(hasError = true, isStreaming = false)
                        }
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                isStreaming = false,
                                chatErrorMessage = event.throwable.message ?: "AI 回复失败"
                            )
                        }
                    }
                }
            }
        }
    }

    /** 快捷技能直接发起一次虚拟用户请求并触发助手回复。 */
    private fun sendQuickSkill(definition: QuickSkillDefinition) {
        val content = definition.defaultPrompt
        val userMessage = createUserMessage(content)
        val assistantPlaceholder = createAssistantPlaceholder()
        val audioContext = if (definition.requiresAudioContext) {
            buildAudioContextSummary()
        } else {
            null
        }
        val newState = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + userMessage + assistantPlaceholder,
            inputText = "",
            isSending = true,
            isStreaming = true,
            snackbarMessage = null
        )
        _uiState.value = newState
        persistMessagesAsync()
        viewModelScope.launch {
            updateSessionSummary(userMessage.content)
        }
        val request = buildChatRequest(content, definition.id, newState.chatMessages, audioContext)
        startStreamingResponse(request, assistantPlaceholder.id)
    }

    private fun updateAssistantMessage(
        assistantId: String,
        persistAfterUpdate: Boolean = false,
        transformer: (ChatMessageUi) -> ChatMessageUi
    ) {
        _uiState.update { state ->
            val updated = state.chatMessages.map { msg ->
                if (msg.id == assistantId) transformer(msg) else msg
            }
            state.copy(chatMessages = updated)
        }
        if (persistAfterUpdate) {
            persistMessagesAsync()
        }
    }

    private fun appendAssistantMessage(
        content: String,
        isStreaming: Boolean = false,
        hasError: Boolean = false
    ) {
        val message = ChatMessageUi(
            id = nextMessageId(),
            role = ChatMessageRole.ASSISTANT,
            content = content,
            timestampMillis = System.currentTimeMillis(),
            isStreaming = isStreaming,
            hasError = hasError
        )
        _uiState.update { it.copy(chatMessages = it.chatMessages + message) }
        persistMessagesAsync()
        viewModelScope.launch {
            updateSessionSummary(message.content)
        }
    }

    private fun buildChatRequest(
        userMessage: String,
        skillId: QuickSkillId?,
        historySource: List<ChatMessageUi>,
        audioContext: AudioContextSummary?
    ): ChatRequest {
        val history = historySource.map { ui ->
            ChatHistoryItem(
                role = if (ui.role == ChatMessageRole.USER) ChatRole.USER else ChatRole.ASSISTANT,
                content = ui.content
            )
        }
        return ChatRequest(
            sessionId = sessionId,
            userMessage = userMessage,
            quickSkillId = skillId?.name,
            audioContextSummary = audioContext,
            history = history
        )
    }

    private fun createUserMessage(content: String): ChatMessageUi = ChatMessageUi(
        id = nextMessageId(),
        role = ChatMessageRole.USER,
        content = content,
        timestampMillis = System.currentTimeMillis()
    )

    private fun createAssistantPlaceholder(): ChatMessageUi = ChatMessageUi(
        id = nextMessageId(),
        role = ChatMessageRole.ASSISTANT,
        content = "",
        timestampMillis = System.currentTimeMillis(),
        isStreaming = true
    )

    /** 根据已订阅的媒体同步状态构建轻量音频上下文，仅供聊天请求使用。 */
    private fun buildAudioContextSummary(): AudioContextSummary? {
        val state = latestMediaSyncState ?: return null
        val readyClips = state.items.filter { it.status == MediaClipStatus.Ready }
        val readyCount = readyClips.size
        val pendingUpload = state.items.count { it.status == MediaClipStatus.Uploading }
        val pendingTranscription = readyClips.count { it.transcriptSource.isNullOrBlank() }
        val pendingTotal = pendingUpload + pendingTranscription
        if (readyCount == 0 && pendingTotal == 0) return null
        val hasTranscripts = readyClips.any { !it.transcriptSource.isNullOrBlank() }
        val note = when {
            readyCount > 0 && hasTranscripts -> "最近 ${readyCount} 条录音已转写，可生成会议总结。"
            readyCount > 0 -> "有 ${readyCount} 条录音可用于总结。"
            pendingTotal > 0 -> "${pendingTotal} 条录音仍在上传或转写。"
            else -> null
        }
        return AudioContextSummary(
            readyClipCount = readyCount,
            pendingClipCount = pendingTotal,
            hasTranscripts = hasTranscripts,
            note = note
        )
    }

    private fun ConnectionState.toDeviceSnapshot(): DeviceSnapshotUi = when (this) {
        ConnectionState.Disconnected -> DeviceSnapshotUi(
            statusText = "未连接",
            connectionState = DeviceConnectionStateUi.DISCONNECTED
        )

        is ConnectionState.Pairing -> DeviceSnapshotUi(
            deviceName = deviceName,
            statusText = "正在配对 ${deviceName} (${progressPercent}%)",
            connectionState = DeviceConnectionStateUi.CONNECTING
        )

        is ConnectionState.Connected -> DeviceSnapshotUi(
            deviceName = session.peripheralName,
            statusText = "BLE 已连接，等待设备联网...",
            connectionState = DeviceConnectionStateUi.WAITING_FOR_NETWORK
        )

        is ConnectionState.WifiProvisioned -> DeviceSnapshotUi(
            deviceName = session.peripheralName,
            statusText = "${session.peripheralName} 已上线",
            connectionState = DeviceConnectionStateUi.CONNECTED,
            wifiName = status.wifiSsid
        )

        is ConnectionState.Syncing -> DeviceSnapshotUi(
            deviceName = session.peripheralName,
            statusText = "${session.peripheralName} 正在保持连接",
            connectionState = DeviceConnectionStateUi.CONNECTED,
            wifiName = status.wifiSsid
        )

        is ConnectionState.Error -> DeviceSnapshotUi(
            statusText = "连接异常",
            connectionState = DeviceConnectionStateUi.ERROR,
            errorSummary = error.toReadableMessage()
        )
    }

    private fun ConnectivityError.toReadableMessage(): String = when (this) {
        is ConnectivityError.PairingInProgress -> "配对冲突：${deviceName} 已在使用"
        is ConnectivityError.ProvisioningFailed -> reason
        is ConnectivityError.PermissionDenied -> "缺少权限：${permissions.joinToString()}"
        is ConnectivityError.Timeout -> "连接超时，请稍后重试"
        is ConnectivityError.Transport -> reason
        ConnectivityError.MissingSession -> "当前没有有效的配对会话"
    }

    private fun MediaSyncState.toAudioSummaryUi(): AudioSummaryUi? {
        val readyCount = items.count { it.status == MediaClipStatus.Ready }
        val uploadingCount = items.count { it.status == MediaClipStatus.Uploading }
        val failedCount = items.count { it.status == MediaClipStatus.Failed }
        val pendingTranscriptions = items.count {
            it.status == MediaClipStatus.Ready && it.transcriptSource == null
        }
        if (!syncing && readyCount == 0 && uploadingCount == 0 && failedCount == 0) {
            return null
        }
        val headline = when {
            uploadingCount > 0 -> "${uploadingCount} 条录音待上传"
            syncing -> "正在同步录音..."
            failedCount > 0 -> "${failedCount} 条录音同步失败"
            readyCount > 0 -> "${readyCount} 条录音已就绪"
            else -> "暂无录音"
        }
        val detailParts = mutableListOf<String>()
        if (readyCount > 0 && uploadingCount > 0) {
            detailParts += "${readyCount} 条已完成"
        }
        if (pendingTranscriptions > 0) {
            detailParts += "${pendingTranscriptions} 条待转写"
        }
        if (failedCount > 0) {
            detailParts += "${failedCount} 条失败"
        }
        val detail = detailParts.takeIf { it.isNotEmpty() }?.joinToString("，")
            ?: lastSyncedAtMillis?.let { "上次同步 ${formatSyncTime(it)}" }
        return AudioSummaryUi(
            headline = headline,
            detail = detail,
            syncedCount = readyCount,
            pendingUploadCount = uploadingCount,
            pendingTranscriptionCount = pendingTranscriptions,
            lastSyncedAtMillis = lastSyncedAtMillis
        )
    }

    private fun formatSyncTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    override fun onCleared() {
        super.onCleared()
        transcriptionJob?.cancel()
    }

    private fun buildSkillConfirmation(definition: QuickSkillDefinition): String {
        val action = definition.label.lowercase(Locale.getDefault())
        return "Got it — I'm ready to $action. Tell me what you'd like to add."
    }

    private fun QuickSkillDefinition.toUiModel(): QuickSkillUi = QuickSkillUi(
        id = id,
        label = label,
        description = description,
        isRecommended = isRecommended
    )

    private fun nextMessageId(): String = "msg-${UUID.randomUUID()}"

    private fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
        value = transform(value)
    }

    companion object {
        private fun buildTranscriptContext(fileName: String, transcript: String): String {
            return buildString {
                append("你是一名销售助理。以下是通话记录，请帮我分析、总结或提出行动建议。\n")
                append("文件：").append(fileName).append("\n")
                append(transcript)
            }
        }
    }

    private suspend fun ensureSessionSummary(
        sessionId: String,
        titleOverride: String? = null
    ): AiSessionSummary {
        val existing = sessionRepository.findById(sessionId)
        val title = titleOverride ?: existing?.title ?: "新的聊天"
        val preview = existing?.lastMessagePreview ?: ""
        val summary = AiSessionSummary(
            id = sessionId,
            title = title,
            lastMessagePreview = preview,
            updatedAtMillis = existing?.updatedAtMillis ?: System.currentTimeMillis(),
            pinned = existing?.pinned ?: false
        )
        sessionRepository.upsert(summary)
        return summary
    }

    private suspend fun updateSessionSummary(lastPreview: String) {
        val existing = sessionRepository.findById(sessionId)
        val summary = AiSessionSummary(
            id = sessionId,
            title = existing?.title ?: _uiState.value.currentSession.title,
            lastMessagePreview = lastPreview,
            updatedAtMillis = System.currentTimeMillis(),
            pinned = existing?.pinned ?: false
        )
        sessionRepository.upsert(summary)
    }
}
