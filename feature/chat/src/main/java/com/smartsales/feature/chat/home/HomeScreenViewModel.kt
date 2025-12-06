package com.smartsales.feature.chat.home

import android.net.Uri
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.Result
import com.smartsales.feature.chat.core.AudioContextSummary
import com.smartsales.feature.chat.core.ChatHistoryItem
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.history.toEntity
import com.smartsales.feature.chat.history.toUiModel
import com.smartsales.feature.chat.title.SessionTitleResolver
import com.smartsales.feature.chat.AiSessionRepository as SessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.MediaClipStatus
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.usercenter.data.UserProfileRepository
import com.smartsales.feature.usercenter.UserProfile
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionStage
import com.smartsales.core.metahub.RiskLevel
import com.smartsales.feature.chat.home.CHAT_DEBUG_HUD_ENABLED
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_SESSION_ID = "home-session"
private const val DEFAULT_SESSION_TITLE = "新的聊天"
private const val LONG_CONTENT_THRESHOLD = 240
private const val CONTEXT_LENGTH_LIMIT = 800
private const val CONTEXT_MESSAGE_LIMIT = 5
private const val ANALYSIS_CONTENT_LIMIT = 2400
private const val TAG = "HomeScreenVM"

private enum class InputBucket { NOISE, SHORT_RELEVANT, RICH }
private data class AnalysisTarget(val content: String, val source: String)

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

data class DebugSessionMetadata(
    val sessionId: String,
    val title: String,
    val mainPerson: String? = null,
    val shortSummary: String? = null,
    val summaryTitle6Chars: String? = null,
    val notes: List<String> = emptyList()
)

/** Home 页的不可变 UI 状态。 */
data class HomeUiState(
    val chatMessages: List<ChatMessageUi> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isStreaming: Boolean = false,
    val isInputBusy: Boolean = false,
    val isBusy: Boolean = false,
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
    ),
    val userName: String = "用户",
    val exportInProgress: Boolean = false,
    val showWelcomeHero: Boolean = true,
    val isSmartAnalysisMode: Boolean = false,
    val showDebugMetadata: Boolean = false,
    val debugSessionMetadata: DebugSessionMetadata? = null
)

/** 外部依赖（除 AiChatService 外保留原有 stub）。 */
interface AiSessionRepository {
    suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi>
}

/** HomeScreenViewModel：驱动聊天、快捷技能以及设备/音频快照。 */
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val homeOrchestrator: HomeOrchestrator,
    private val aiSessionRepository: AiSessionRepository,
    private val deviceConnectionManager: DeviceConnectionManager,
    private val mediaSyncCoordinator: MediaSyncCoordinator,
    private val transcriptionCoordinator: AudioTranscriptionCoordinator,
    private val audioStorageRepository: AudioStorageRepository,
    private val quickSkillCatalog: QuickSkillCatalog,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val sessionRepository: SessionRepository,
    private val sessionTitleResolver: SessionTitleResolver,
    private val userProfileRepository: UserProfileRepository,
    private val exportOrchestrator: ExportOrchestrator,
    private val shareHandler: ChatShareHandler,
    private val metaHub: MetaHub
) : ViewModel() {

    private val quickSkillDefinitions = quickSkillCatalog.homeQuickSkills()
    private val quickSkillDefinitionsById = quickSkillDefinitions.associateBy { it.id }
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    private var latestMediaSyncState: MediaSyncState? = null
    private var sessionId: String = DEFAULT_SESSION_ID
    private var latestSessionSummaries: List<AiSessionSummary> = emptyList()
    private var hasShownLowInfoHint: Boolean = false
    private var hasShownAnalysisExportHint: Boolean = false
    private var transcriptionJob: Job? = null
    private var latestAnalysisMarkdown: String? = null
    private var latestAnalysisMessageId: String? = null
    private var pendingExportAfterAnalysis: ExportFormat? = null

    init {
        // 从 catalog 加载快捷技能到状态
        val skills = quickSkillDefinitions.map { it.toUiModel() }
        _uiState.update { it.copy(quickSkills = skills) }
        setSession(DEFAULT_SESSION_ID)
        observeDeviceConnection()
        observeMediaSync()
        observeSessions()
        loadUserProfile()
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSendMessage() {
        val rawInput = _uiState.value.inputText
        val content = rawInput.trim()
        if (_uiState.value.isBusy) return
        if (_uiState.value.isSmartAnalysisMode) {
            handleSmartAnalysisSend(content)
            return
        }
        if (content.isEmpty()) return
        sendMessageInternal(
            messageText = content
        )
    }

    private fun handleSmartAnalysisSend(rawInput: String) {
        if (_uiState.value.isSending) return
        val goal = rawInput.trim()
        val bucket = classifyUserInput(goal.ifBlank { _uiState.value.inputText }, _uiState.value.chatMessages)
        val target = findSmartAnalysisPrimaryContent(goal)
        if (target == null) {
            appendAssistantMessage(
                content = "当前对话内容太少，我没法做有价值的智能分析。\n建议先粘贴一段对话、邮件或会议记录，然后再点击「智能分析」。"
            )
            _uiState.update { it.copy(isSmartAnalysisMode = false, selectedSkill = null, inputText = "") }
            return
        }
        val isLowInfoGoal = goal.isNotBlank() && isLowInfoAnalysisInput(goal)
        val analysisGoal = if (goal.isNotBlank() && !isLowInfoGoal) goal else "通用分析"
        val hasCustomGoal = goal.isNotBlank() && !isLowInfoGoal
        val preface = if (goal.isBlank() || isLowInfoGoal || bucket == InputBucket.NOISE) {
            "提示：你没有额外说明分析目标，我会基于最近的一段对话或内容做通用智能分析。"
        } else null
        debugLog(
            event = "smart_analysis_request_start",
            data = mapOf(
                "sessionId" to sessionId,
                "bucket" to bucket.name,
                "primarySource" to target.source
            )
        )
        val contextContent = findContextForAnalysis(target.content)
        val userMessage = buildSmartAnalysisUserMessage(
            mainContent = target.content,
            context = contextContent,
            goal = analysisGoal
        )
        _uiState.update { it.copy(isSmartAnalysisMode = false, selectedSkill = null, inputText = "") }
        sendMessageInternal(
            messageText = userMessage,
            skillOverride = QuickSkillId.SMART_ANALYSIS,
            userDisplayText = if (hasCustomGoal) {
                "智能分析：${goal.take(60)}"
            } else {
                "智能分析（通用）"
            },
            onCompletedTransform = { body ->
                buildString {
                    append("智能分析结果\n\n")
                    preface?.let {
                        append(it.trim()).append("\n\n")
                    }
                    append(body.trim())
                }.trim()
            }
        )
    }

    fun onSmartAnalysisClicked() {
        // 兼容旧入口：改为开启/关闭智能分析模式，不直接发送
        onSelectQuickSkill(QuickSkillId.SMART_ANALYSIS)
    }

    fun onExportPdfClicked() {
        exportMarkdown(ExportFormat.PDF)
    }

    fun onExportCsvClicked() {
        exportMarkdown(ExportFormat.CSV)
    }

    fun onSelectQuickSkill(skillId: QuickSkillId) {
        val definition = quickSkillDefinitionsById[skillId]
        if (definition == null) {
            _uiState.update { it.copy(snackbarMessage = "无法识别的快捷技能") }
            return
        }
        when (skillId) {
            QuickSkillId.SMART_ANALYSIS -> {
                if (_uiState.value.isBusy) return
        _uiState.update { state ->
            val toggled = !state.isSmartAnalysisMode
            state.copy(
                isSmartAnalysisMode = toggled,
                selectedSkill = if (toggled) definition.toUiModel() else null,
                inputText = state.inputText
            )
        }
    }
            QuickSkillId.EXPORT_PDF -> {
                onExportPdfClicked()
            }
            QuickSkillId.EXPORT_CSV -> {
                onExportCsvClicked()
            }
            else -> {
                _uiState.update { state ->
                    state.copy(
                        selectedSkill = definition.toUiModel(),
                        inputText = definition.defaultPrompt,
                        chatErrorMessage = null
                    )
                }
            }
        }
    }

    private fun exportMarkdown(format: ExportFormat) {
        if (_uiState.value.exportInProgress) return
        viewModelScope.launch {
            // 检查 MetaHub 分析状态
            val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            val hasMetaAnalysis = meta?.latestMajorAnalysisMessageId != null
            val cachedAnalysis = latestAnalysisMarkdown

            when {
                !cachedAnalysis.isNullOrBlank() -> {
                    // 直接导出：使用缓存分析 markdown
                    performExport(format, markdownOverride = cachedAnalysis)
                }
                hasMetaAnalysis -> {
                    // MetaHub 认为有分析，但 VM 没有缓存文本
                    // 不自动重跑，给轻量提示 + 退回
                    _uiState.update {
                        it.copy(
                            exportInProgress = false,
                            snackbarMessage = "检测到历史分析记录，如需导出，请重新运行一次智能分析。"
                        )
                    }
                    // 可选：退回到对话导出（逐字稿），或直接 return
                    return@launch
                }
                else -> {
                    // 没有任何分析记录，走现有 "自动 SMART_ANALYSIS 然后导出" 路径
                    val (mainContent, context) = findLatestLongContent()
                    if (mainContent == null) {
                        performExport(format, markdownOverride = null)
                        return@launch
                    }
                    pendingExportAfterAnalysis = format
                    _uiState.update { it.copy(exportInProgress = true, chatErrorMessage = null) }
                    val autoGoal = "导出前自动分析"
                    val userMessage = buildSmartAnalysisUserMessage(
                        mainContent = mainContent,
                        context = context,
                        goal = autoGoal
                    )
                    sendMessageInternal(
                        messageText = userMessage,
                        skillOverride = QuickSkillId.SMART_ANALYSIS,
                        userDisplayText = "智能分析（导出前自动生成）",
                        onCompleted = {},
                        onCompletedTransform = { body ->
                            buildString {
                                append("智能分析结果\n\n")
                                append(body.trim())
                            }.trim()
                        },
                        isAutoAnalysis = true
                    )
                }
            }
        }
    }

    private suspend fun performExport(format: ExportFormat, markdownOverride: String? = null) {
        val markdown = markdownOverride ?: latestAnalysisMarkdown ?: buildTranscriptMarkdown(_uiState.value.chatMessages)
        if (format == ExportFormat.PDF && markdown.isBlank()) {
            _uiState.update { it.copy(exportInProgress = false, snackbarMessage = "暂无可导出的内容") }
            return
        }
        _uiState.update { it.copy(exportInProgress = true, chatErrorMessage = null) }
        val result = when (format) {
            ExportFormat.PDF -> exportOrchestrator.exportPdf(sessionId, markdown)
            ExportFormat.CSV -> exportOrchestrator.exportCsv(sessionId)
        }
        when (result) {
            is Result.Success -> {
                when (val share = shareHandler.shareExport(result.data)) {
                    is Result.Success -> _uiState.update { it.copy(exportInProgress = false) }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            exportInProgress = false,
                            chatErrorMessage = share.throwable.message ?: "分享失败"
                        )
                    }
                }
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        exportInProgress = false,
                        chatErrorMessage = result.throwable.message ?: "导出失败"
                    )
                }
            }
        }
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
        _uiState.update { it.copy(navigationRequest = HomeNavigationRequest.AudioFiles) }
    }

    /** 处理用户上传的本地音频，保存到本地并提交 Tingwu 转写。 */
    fun onAudioFilePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInputBusy = true, isBusy = true, snackbarMessage = null, showWelcomeHero = false) }
            val stored = withContext(Dispatchers.IO) {
                runCatching { audioStorageRepository.importFromPhone(uri) }
            }.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isInputBusy = false,
                        isBusy = false,
                        snackbarMessage = error.message ?: "导入音频失败"
                    )
                }
                return@launch
            }
            val localPath = stored.localUri.path
            if (localPath.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isInputBusy = false,
                        isBusy = false,
                        snackbarMessage = "找不到本地音频路径"
                    )
                }
                return@launch
            }
            val uploadPayload = when (val upload = transcriptionCoordinator.uploadAudio(File(localPath))) {
                is Result.Success -> upload.data
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isInputBusy = false,
                            isBusy = false,
                            snackbarMessage = upload.throwable.message ?: "上传音频失败"
                        )
                    }
                    return@launch
                }
            }
            when (val submit = transcriptionCoordinator.submitTranscription(
                audioAssetName = stored.displayName,
                language = "zh-CN",
                uploadPayload = uploadPayload
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(isInputBusy = false, isBusy = false, snackbarMessage = "音频已上传，正在转写…") }
                    onTranscriptionRequested(
                        TranscriptionChatRequest(
                            jobId = submit.data,
                            fileName = stored.displayName,
                            recordingId = stored.id
                        )
                    )
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isInputBusy = false,
                            isBusy = false,
                            snackbarMessage = submit.throwable.message ?: "提交转写失败"
                        )
                    }
                }
            }
        }
    }

    /** 处理用户上传图片，保存并触发占位分析。 */
    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, isInputBusy = true, snackbarMessage = null, showWelcomeHero = false) }
            val saved = withContext(Dispatchers.IO) {
                runCatching { saveImageToCache(uri) }
            }.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isInputBusy = false,
                        snackbarMessage = error.message ?: "保存图片失败"
                    )
                }
                return@launch
            }
            val placeholderId = nextMessageId()
            val placeholder = ChatMessageUi(
                id = placeholderId,
                role = ChatMessageRole.ASSISTANT,
                content = "已上传图片：${saved.name}，正在分析…",
                timestampMillis = System.currentTimeMillis(),
                isStreaming = true
            )
            _uiState.update { state ->
                state.copy(
                    chatMessages = state.chatMessages + placeholder,
                    snackbarMessage = state.snackbarMessage
                )
            }
            persistMessagesAsync()
            // TODO: 接入 DashScope 图像分析，当前为占位分析
            delay(800)
            updateAssistantMessage(placeholderId, persistAfterUpdate = true) { msg ->
                msg.copy(
                    content = "图片分析占位：${saved.name}。图像解析接口接入后将提供详细描述。",
                    isStreaming = false
                )
            }
            _uiState.update { it.copy(isBusy = false, isInputBusy = false) }
        }
    }

    fun onTranscriptionRequested(request: TranscriptionChatRequest) {
        viewModelScope.launch {
            val transcript = request.transcriptMarkdown ?: request.transcriptPreview
            val targetSessionId = request.sessionId ?: "session-${UUID.randomUUID()}"
            val title = "通话分析 – ${request.fileName}".take(40)
            val previewText = (request.transcriptMarkdown ?: request.transcriptPreview)
                .orEmpty()
                .replace(Regex("^#+\\s*"), "")
                .take(50)
            val existing = chatHistoryRepository.loadLatestSession(targetSessionId)
            this@HomeScreenViewModel.sessionId = targetSessionId
            hasShownLowInfoHint = false
            hasShownAnalysisExportHint = false
            ensureSessionSummary(targetSessionId, title)
            if (previewText.isNotBlank()) {
                updateSessionSummary(previewText)
            }
            _uiState.update {
                it.copy(
                    currentSession = CurrentSessionUi(
                        id = targetSessionId,
                        title = title,
                        isTranscription = true
                    ),
                    chatMessages = existing.map { item -> item.toUiModel() },
                    isLoadingHistory = false,
                    showWelcomeHero = false
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
            }
            val introMessage = ChatMessageUi(
                id = nextMessageId(),
                role = ChatMessageRole.ASSISTANT,
                content = "通话分析 · 已为你加载录音 ${request.fileName} 的转写内容，我可以帮你总结对话、提炼要点或生成跟进话术。",
                timestampMillis = System.currentTimeMillis()
            )
            _uiState.update { state ->
                state.copy(chatMessages = state.chatMessages + introMessage)
            }
            persistMessagesAsync()
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

    fun toggleDebugMetadata() {
        val newValue = !_uiState.value.showDebugMetadata
        _uiState.update { it.copy(showDebugMetadata = newValue) }
        if (newValue) {
            // 打开时刷新一次 MetaHub，关闭时保留现有数据即可
            refreshDebugSessionMetadata()
        }
    }

    private fun updateDebugSessionMetadata(
        meta: SessionMetadata?,
        extraNotes: List<String> = emptyList()
    ) {
        val title = _uiState.value.currentSession.title
        val existingNotes = _uiState.value.debugSessionMetadata
            ?.takeIf { it.sessionId == sessionId }
            ?.notes
            .orEmpty()
        val mergedNotes = (existingNotes + extraNotes).distinct()
        val debug = DebugSessionMetadata(
            sessionId = sessionId,
            title = title,
            mainPerson = meta?.mainPerson,
            shortSummary = meta?.shortSummary,
            summaryTitle6Chars = meta?.summaryTitle6Chars,
            notes = mergedNotes
        )
        _uiState.update { it.copy(debugSessionMetadata = debug) }
    }

    private fun refreshDebugSessionMetadata() {
        viewModelScope.launch {
            val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            updateDebugSessionMetadata(meta)
        }
    }

    private fun appendDebugNote(note: String) {
        if (!CHAT_DEBUG_HUD_ENABLED) return
        val currentDebug = _uiState.value.debugSessionMetadata
        val updated = currentDebug?.copy(notes = (currentDebug.notes + note).distinct())
            ?: DebugSessionMetadata(
                sessionId = sessionId,
                title = _uiState.value.currentSession.title,
                notes = listOf(note)
            )
        _uiState.update { it.copy(debugSessionMetadata = updated) }
    }

    fun setSession(
        sessionId: String,
        titleOverride: String? = null,
        isTranscription: Boolean = false,
        allowHero: Boolean = true
    ) {
        viewModelScope.launch {
            if (sessionId != this@HomeScreenViewModel.sessionId) {
                this@HomeScreenViewModel.sessionId = sessionId
                latestAnalysisMarkdown = null
                hasShownLowInfoHint = false
                hasShownAnalysisExportHint = false
                _uiState.update { it.copy(isSmartAnalysisMode = false, selectedSkill = null) }
                applySessionList()
                loadSession(sessionId)
            }
            if (!allowHero) {
                _uiState.update { it.copy(showWelcomeHero = false) }
            }
            val summary = ensureSessionSummary(sessionId, titleOverride)
            _uiState.update {
                it.copy(
                    currentSession = CurrentSessionUi(
                        id = sessionId,
                        title = summary.title,
                        isTranscription = isTranscription
                    ),
                    showWelcomeHero = if (allowHero) it.showWelcomeHero else false
                )
            }
            refreshDebugSessionMetadata()
        }
    }

    fun setQuickSkills(skills: List<QuickSkillUi>) {
        _uiState.update { it.copy(quickSkills = skills) }
    }

    fun onNewChatClicked() {
        viewModelScope.launch {
            val newSessionId = "session-${UUID.randomUUID()}"
            sessionId = newSessionId
            latestAnalysisMarkdown = null
            hasShownLowInfoHint = false
            hasShownAnalysisExportHint = false
            _uiState.update { it.copy(isSmartAnalysisMode = false, selectedSkill = null) }
            val summary = ensureSessionSummary(newSessionId, titleOverride = "新的聊天")
            _uiState.update {
                it.copy(
                    currentSession = CurrentSessionUi(
                        id = newSessionId,
                        title = summary.title,
                        isTranscription = false
                    ),
                    chatMessages = emptyList(),
                    isLoadingHistory = false,
                    showWelcomeHero = true
                )
            }
            updateDebugSessionMetadata(null)
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
                        isLoadingHistory = false,
                        showWelcomeHero = false
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
        skillOverride: QuickSkillId? = null,
        userDisplayText: String? = null,
        onCompleted: (String) -> Unit = {},
        onCompletedTransform: ((String) -> String)? = null,
        isAutoAnalysis: Boolean = false
    ) {
        val content = messageText.trim()
        if (content.isEmpty() || _uiState.value.isBusy) return
        _uiState.update { it.copy(chatErrorMessage = null) }
        val quickSkill = (skillOverride ?: _uiState.value.selectedSkill?.id)?.let { id ->
            quickSkillDefinitionsById[id]
        }
        val quickSkillId = quickSkill?.id
        if (quickSkillId == null) {
            when (classifyUserInput(content, _uiState.value.chatMessages)) {
                InputBucket.NOISE -> {
                    _uiState.update {
                        it.copy(
                            inputText = "",
                            isSending = false,
                            isStreaming = false,
                            isInputBusy = false,
                            isBusy = false,
                            snackbarMessage = null,
                            chatErrorMessage = null,
                            showWelcomeHero = false
                        )
                    }
                    appendAssistantMessage(
                        content = "我主要帮你分析销售对话、邮件或会议纪要。请粘贴一段对话、邮件，或描述你与客户的沟通场景，我再帮你分析。"
                    )
                    return
                }

                InputBucket.SHORT_RELEVANT,
                InputBucket.RICH -> Unit
            }
        }
        val userMessage = createUserMessage(userDisplayText ?: content)
        val assistantPlaceholder = createAssistantPlaceholder()
        // 先在旧消息列表上判断是否已有助手回复，避免占位气泡干扰
        val hadAssistantReplyBefore = _uiState.value.chatMessages.any { it.role == ChatMessageRole.ASSISTANT }
        val isFirstAssistantReply = !hadAssistantReplyBefore
        debugLog(
            event = "chat_request_start",
            data = mapOf(
                "sessionId" to sessionId,
                "mode" to (quickSkillId ?: "GENERAL_CHAT"),
                "isFirstReply" to isFirstAssistantReply,
                "inputLength" to content.length,
                "isAutoAnalysis" to isAutoAnalysis
            )
        )
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
            isInputBusy = true,
            isBusy = true,
            snackbarMessage = null,
            showWelcomeHero = false
        )
        _uiState.value = newState
        persistMessagesAsync()
        viewModelScope.launch {
            updateSessionSummary(userMessage.content)
        }
        val request = buildChatRequest(content, quickSkillId, newState.chatMessages, audioContext, isAutoAnalysis)
        val enrichedRequest = request.copy(
            isFirstAssistantReply = isFirstAssistantReply
        )
        startStreamingResponse(
            request = enrichedRequest,
            assistantId = assistantPlaceholder.id,
            onCompleted = onCompleted,
            onCompletedTransform = onCompletedTransform
        )
    }

    /** 启动 streaming，更新最后一条助手气泡。 */
    private fun startStreamingResponse(
        request: ChatRequest,
        assistantId: String,
        onCompleted: (String) -> Unit = {},
        onCompletedTransform: ((String) -> String)? = null
    ) {
        val isSmartAnalysis = request.quickSkillId == "SMART_ANALYSIS"
        val streamingDeduplicator = StreamingDeduplicator()

        viewModelScope.launch {
            homeOrchestrator.streamChat(request).collect { event ->
                when (event) {
                    is ChatStreamEvent.Delta -> {
                        // 处理 streaming token，实时去重
                        updateAssistantMessage(assistantId) { msg ->
                            val updated = streamingDeduplicator.mergeSnapshot(msg.content, event.token)
                            msg.copy(content = updated)
                        }
                    }
                    is ChatStreamEvent.Completed -> {
                        // 完成：关闭 streaming，最终清理和去重
                        val rawFullText = event.fullText
                        val shouldParseSessionMetadata =
                            request.quickSkillId == null && request.isFirstAssistantReply
                        if (shouldParseSessionMetadata) {
                            handleGeneralChatMetadata(rawFullText)
                        }
                        if (isSmartAnalysis) {
                            handleSmartAnalysisMetadata(rawFullText)
                        }
                        val sanitized = sanitizeAssistantOutput(rawFullText, isSmartAnalysis)
                        val cleaned = onCompletedTransform?.invoke(sanitized) ?: sanitized
                        if (isSmartAnalysis) {
                            onAnalysisCompleted(cleaned, assistantId)
                        }
                        debugLog(
                            event = "chat_stream_completed",
                            data = mapOf(
                                "sessionId" to sessionId,
                                "mode" to (request.quickSkillId ?: "GENERAL_CHAT"),
                                "isFirstReply" to request.isFirstAssistantReply,
                                "assistantTextLength" to cleaned.length
                            )
                        )
                        updateAssistantMessage(assistantId, persistAfterUpdate = true) { msg ->
                            msg.copy(content = cleaned, isStreaming = false)
                        }
                        onCompleted(cleaned)
                        _uiState.update { it.copy(isSending = false, isStreaming = false, isInputBusy = false, isBusy = false) }
                        viewModelScope.launch {
                            updateSessionSummary(cleaned)
                            applySessionList()
                            maybeGenerateSessionTitle(request, cleaned)
                        }
                    }
                    is ChatStreamEvent.Error -> {
                        // 错误：标记消息失败并弹出提示
                        warnLog(
                            event = "chat_stream_error",
                            data = mapOf(
                                "sessionId" to sessionId,
                                "mode" to (request.quickSkillId ?: "GENERAL_CHAT")
                            ),
                            throwable = event.throwable
                        )
                        updateAssistantMessage(assistantId, persistAfterUpdate = true) { msg ->
                            msg.copy(hasError = true, isStreaming = false)
                        }
                        pendingExportAfterAnalysis = null
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                isStreaming = false,
                                isInputBusy = false,
                                isBusy = false,
                                exportInProgress = false,
                                chatErrorMessage = event.throwable.message ?: "AI 回复失败"
                            )
                        }
                    }
                }
            }
        }
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
        audioContext: AudioContextSummary?,
        isAutoAnalysis: Boolean
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
            quickSkillId = mapSkillToMode(skillId),
            audioContextSummary = audioContext,
            history = history
        )
    }

    private fun buildTranscriptMarkdown(messages: List<ChatMessageUi>): String {
        if (messages.isEmpty()) return ""
        val builder = StringBuilder()
        builder.append("# 对话记录\n\n")
        messages.forEach { msg ->
            val role = if (msg.role == ChatMessageRole.USER) "用户" else "助手"
            builder.append("- **$role**：${msg.content}\n")
        }
        return builder.toString()
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

    private fun latestUserContent(): String? =
        _uiState.value.chatMessages.lastOrNull { it.role == ChatMessageRole.USER }?.content?.trim()

    private fun classifyUserInput(
        text: String,
        history: List<ChatMessageUi>
    ): InputBucket {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return InputBucket.NOISE
        val normalized = trimmed.lowercase(Locale.getDefault())
        val keywords = listOf("客户", "会议", "报价", "合同", "跟进", "电话", "录音", "邮件", "沟通", "销售")
        val hasKeyword = keywords.any { normalized.contains(it) }
        val hasLongHistory = history.any { it.content.length >= LONG_CONTENT_THRESHOLD }
        val hasTranscript = history.any { it.role == ChatMessageRole.ASSISTANT && it.content.contains("通话分析") }
        if (normalized.length <= 4 && !hasKeyword) {
            return InputBucket.NOISE
        }
        if (normalized.length <= 8 && !hasKeyword && !hasLongHistory) {
            return InputBucket.NOISE
        }
        if (trimmed.length >= LONG_CONTENT_THRESHOLD || hasLongHistory || hasTranscript) {
            return InputBucket.RICH
        }
        if (hasKeyword) return InputBucket.SHORT_RELEVANT
        return InputBucket.SHORT_RELEVANT
    }

    private fun isLowInfoAnalysisInput(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.getDefault())
        if (normalized.isEmpty()) return true
        if (normalized.length <= 3) {
            val stop = setOf("的", "啊", "么", "吗", "吧", "哦", "呢", "hi", "hi!", "ok", "?", "？", "分析", "看看")
            if (stop.contains(normalized)) return true
        }
        val greetings = setOf("你好", "您好", "hello", "hi", "你是谁", "嘿", "早", "下午好", "晚上好")
        if (greetings.contains(normalized)) return true
        val filler = setOf("分析", "看看", "随便", "随便看看", "简单看看", "简单分析")
        if (filler.contains(normalized)) return true
        if (normalized.length <= 4 && normalized.all { it.isWhitespace() || it in listOf('的', '啊', '吗', '呢', '?', '？') }) {
            return true
        }
        return false
    }

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
        ConnectionState.NeedsSetup -> DeviceSnapshotUi(
            statusText = "设备未配网，点击开始配置",
            connectionState = DeviceConnectionStateUi.DISCONNECTED
        )

        ConnectionState.Disconnected -> DeviceSnapshotUi(
            statusText = "设备未连接，点击开始配网",
            connectionState = DeviceConnectionStateUi.DISCONNECTED
        )

        is ConnectionState.Pairing -> DeviceSnapshotUi(
            deviceName = deviceName,
            statusText = "正在连接设备，请保持靠近",
            connectionState = DeviceConnectionStateUi.CONNECTING
        )

        is ConnectionState.AutoReconnecting -> DeviceSnapshotUi(
            statusText = "正在自动重连设备…",
            connectionState = DeviceConnectionStateUi.CONNECTING
        )

        is ConnectionState.Connected -> DeviceSnapshotUi(
            deviceName = session.peripheralName,
            statusText = "设备已连接，等待联网",
            connectionState = DeviceConnectionStateUi.WAITING_FOR_NETWORK
        )

        is ConnectionState.WifiProvisioned -> DeviceSnapshotUi(
            deviceName = session.peripheralName,
            statusText = "设备已上线，网络：${status.wifiSsid}",
            connectionState = DeviceConnectionStateUi.CONNECTED,
            wifiName = status.wifiSsid
        )

        is ConnectionState.Syncing -> DeviceSnapshotUi(
            deviceName = session.peripheralName,
            statusText = "设备在线，网络：${status.wifiSsid}",
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
        is ConnectivityError.EndpointUnreachable -> reason.ifBlank { "设备服务不可达" }
        is ConnectivityError.DeviceNotFound -> "未找到设备 ${deviceId}"
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

    private fun loadUserProfile() {
        viewModelScope.launch {
            runCatching { userProfileRepository.load() }
                .onSuccess { profile ->
                    _uiState.update { it.copy(userName = deriveUserName(profile)) }
                }
        }
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

    private fun onAnalysisCompleted(summary: String, messageId: String) {
        latestAnalysisMarkdown = summary
        latestAnalysisMessageId = messageId
        if (!hasShownAnalysisExportHint) {
            hasShownAnalysisExportHint = true
            appendAssistantMessage(
                content = "智能分析完成，如需分享可直接导出 PDF 或 CSV。"
            )
        }
        pendingExportAfterAnalysis?.let { format ->
            pendingExportAfterAnalysis = null
            viewModelScope.launch {
                performExport(format, markdownOverride = summary)
            }
        }
    }

    private fun findLatestLongContent(): Pair<String?, String?> {
        val messages = _uiState.value.chatMessages.asReversed()
        var primary: String? = null
        val contextChunks = mutableListOf<String>()
        var contextLength = 0
        for (msg in messages) {
            val text = msg.content.trim()
            if (text.isEmpty()) continue
            if (primary == null && text.length >= LONG_CONTENT_THRESHOLD) {
                primary = text
                continue
            }
            if (contextChunks.size < CONTEXT_MESSAGE_LIMIT && contextLength < CONTEXT_LENGTH_LIMIT) {
                val toAdd = text.take(CONTEXT_LENGTH_LIMIT - contextLength)
                contextChunks += toAdd
                contextLength += toAdd.length
            } else {
                break
            }
        }
        return primary to contextChunks.asReversed().joinToString("\n")
    }

    private fun findSmartAnalysisPrimaryContent(currentInput: String): AnalysisTarget? {
        val trimmed = currentInput.trim()
        if (trimmed.length >= LONG_CONTENT_THRESHOLD) {
            return AnalysisTarget(trimmed, "user_input")
        }
        val messages = _uiState.value.chatMessages.asReversed()
        val transcript = messages.firstOrNull { msg ->
            msg.role == ChatMessageRole.ASSISTANT &&
                msg.content.length >= LONG_CONTENT_THRESHOLD &&
                (msg.content.contains("转写") || msg.content.contains("通话") || msg.content.contains("录音"))
        }
        if (transcript != null) return AnalysisTarget(transcript.content, "transcript")
        val longHistory = messages.firstOrNull { it.content.length >= LONG_CONTENT_THRESHOLD }
        return longHistory?.let { AnalysisTarget(it.content, "history_long") }
    }

    private fun findContextForAnalysis(primaryContent: String): String? {
        val messages = _uiState.value.chatMessages.asReversed()
        val contextChunks = mutableListOf<String>()
        var contextLength = 0
        messages.forEach { msg ->
            val text = msg.content.trim()
            if (text.isEmpty() || text == primaryContent) return@forEach
            if (contextChunks.size >= CONTEXT_MESSAGE_LIMIT || contextLength >= CONTEXT_LENGTH_LIMIT) return@forEach
            val toAdd = text.take(CONTEXT_LENGTH_LIMIT - contextLength)
            contextChunks += toAdd
            contextLength += toAdd.length
        }
        return contextChunks.asReversed().joinToString("\n").takeIf { it.isNotBlank() }
    }

    private fun buildSmartAnalysisUserMessage(
        mainContent: String,
        context: String?,
        goal: String
    ): String {
        val cappedContent = mainContent.trim().take(ANALYSIS_CONTENT_LIMIT)
        val builder = StringBuilder()
        builder.appendLine("分析目标：").appendLine(goal.trim()).appendLine()
        builder.appendLine("主体内容（供重点分析）：").appendLine(cappedContent).appendLine()
        context?.takeIf { it.isNotBlank() }?.let {
            builder.appendLine("最近上下文：").appendLine(it.trim()).appendLine()
        }
        builder.appendLine("请基于上述主体内容完成结构化智能分析。")
        return builder.toString().trim()
    }

    private fun sanitizeAssistantOutput(raw: String, isSmartAnalysis: Boolean = false): String {
        // 预处理：先剥离 JSON block（元数据仅供 MetaHub 使用，不展示给用户）
        val withoutJson = stripJsonBlocks(raw)

        // 第一步：修复编号混乱（如 "11)1)"、"2)1)" 等）
        val fixedNumbering = fixNumberingIssues(withoutJson)
        
        // 第二步：检测并清理内容累积模式（更激进的清理）
        // 使用字符级别的检测，确保彻底清理
        val cleanedAccumulation = cleanAccumulatedContentWithCharLevelDetection(fixedNumbering)
        
        // 第三步：标准去重处理
        val lines = cleanedAccumulation.lines()
        val result = mutableListOf<String>()
        val seenHeadings = mutableSetOf<String>()
        val seenBulletsByHeading = mutableMapOf<String, MutableSet<String>>()
        val seenSentences = mutableSetOf<String>() // 句子级别去重
        var currentHeading: String? = null
        
        // 更健壮的标题检测正则
        val insightHeadingRegex = Regex("^结构化洞察[：:：]?\\s*$")
        val actionHeadingRegex = Regex("^下一步行动[：:：]?\\s*$")
        
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (result.lastOrNull()?.isNotBlank() == true) {
                    result.add("")
                }
                return@forEach
            }

            // 粗略过滤明显的标签段（这些应作为元数据存在，而非直接展示）
            val lower = trimmed.lowercase(Locale.getDefault())
            if (lower.startsWith("标签：") || lower.startsWith("tags:")) {
                return@forEach
            }
            
            // 使用正则匹配标题，更灵活
            val heading = when {
                insightHeadingRegex.matches(trimmed) -> "结构化洞察："
                actionHeadingRegex.matches(trimmed) -> "下一步行动："
                trimmed.contains("结构化洞察") && trimmed.length <= 10 -> "结构化洞察："
                trimmed.contains("下一步行动") && trimmed.length <= 10 -> "下一步行动："
                else -> null
            }
            
            if (heading != null) {
                if (seenHeadings.contains(heading)) {
                    currentHeading = heading
                    return@forEach
                }
                seenHeadings += heading
                currentHeading = heading
                result.add(heading)
                return@forEach
            }
            
            // 处理要点（- 开头）
            if (trimmed.startsWith("- ") || trimmed.startsWith("• ") || trimmed.startsWith("· ")) {
                val headingKey = currentHeading ?: "default"
                val seenBullets = seenBulletsByHeading.getOrPut(headingKey) { mutableSetOf() }
                var bullet = trimmed.removePrefix("- ").removePrefix("• ").removePrefix("· ").trim()
                
                // 修复编号：移除混乱的编号模式
                bullet = bullet.replace(Regex("^\\d+[)）]\\s*"), "")
                    .replace(Regex("\\d+[)）]\\s*"), "")
                    .trim()
                
                // 归一化要点内容用于去重
                val normalizedBullet = normalizeSentence(bullet)
                
                // 检查是否重复（归一化后匹配）
                val isDuplicate = seenBullets.any { existing ->
                    normalizeSentence(existing) == normalizedBullet
                }
                
                // 检查内容累积：如果包含前面见过的长句子
                val isAccumulated = seenSentences.any { existing ->
                    existing.length > 15 && normalizedBullet.contains(existing)
                }
                
                // SMART_ANALYSIS 最多 5 条，普通消息最多 4 条
                val maxBullets = if (isSmartAnalysis) 5 else 4
                if (seenBullets.size >= maxBullets || isDuplicate || isAccumulated) return@forEach
                
                seenBullets += bullet
                seenSentences += normalizedBullet
                result.add("- $bullet")
                return@forEach
            }
            
            // 普通文本行：句子级别去重
            val normalizedLine = normalizeSentence(trimmed)
            
            // 检查完全重复
            if (seenSentences.contains(normalizedLine)) {
                return@forEach
            }
            
            // 检查内容累积：如果包含前面见过的长句子
            val isAccumulated = seenSentences.any { existing ->
                existing.length > 15 && normalizedLine.contains(existing)
            }
            if (isAccumulated) {
                return@forEach
            }
            
            seenSentences += normalizedLine
            result.add(trimmed)
        }
        
        val normalizedLines = result.dropWhile { it.isBlank() }

        // 段落级去重：以空行分段，避免整段重复出现
        val paragraphs = mutableListOf<String>()
        val buffer = StringBuilder()
        for (line in normalizedLines) {
            if (line.isBlank()) {
                if (buffer.isNotEmpty()) {
                    paragraphs += buffer.toString().trimEnd()
                    buffer.clear()
                }
            } else {
                if (buffer.isNotEmpty()) buffer.append('\n')
                buffer.append(line)
            }
        }
        if (buffer.isNotEmpty()) {
            paragraphs += buffer.toString().trimEnd()
        }

        val seenParas = mutableSetOf<String>()
        val uniqueParas = mutableListOf<String>()
        for (para in paragraphs) {
            val norm = para.lowercase(Locale.getDefault())
                .replace(Regex("\\s+"), "")
            if (seenParas.add(norm)) {
                uniqueParas += para
            }
        }

        val joined = uniqueParas.joinToString("\n\n").trimEnd()
        return dedupeNumberedLists(joined)
    }

    /**
     * 针对编号列表块的去重：保留最后一块，移除重复的整段列表。
     */
    private fun dedupeNumberedLists(text: String): String {
        val lines = text.lines()
        val numRegex = Regex("^(\\d+)[\\.)）]\\s*(.*)$")

        data class Block(val start: Int, val end: Int, val lines: List<String>, val key: String)

        val blocks = mutableListOf<Block>()
        var index = 0
        while (index < lines.size) {
            val match = numRegex.matchEntire(lines[index].trim())
            if (match == null) {
                index++
                continue
            }
            val start = index
            val blockLines = mutableListOf<String>()
            while (index < lines.size) {
                val innerMatch = numRegex.matchEntire(lines[index].trim()) ?: break
                blockLines += lines[index]
                index++
            }
            val key = blockLines.joinToString("|") { line ->
                val body = numRegex.matchEntire(line.trim())?.groupValues?.get(2).orEmpty()
                body.lowercase(Locale.getDefault())
                    .replace(Regex("\\s+"), "")
                    .replace(Regex("[\\p{Punct}]"), "")
            }
            blocks += Block(start = start, end = index - 1, lines = blockLines, key = key)
        }

        // 同一 signature 仅保留最后一块
        val keepByKey = mutableMapOf<String, Block>()
        for (block in blocks) {
            keepByKey[block.key] = block
        }
        val keepBlocks = keepByKey.values.toSet()

        val output = mutableListOf<String>()
        var i = 0
        var blockCursor = 0
        while (i < lines.size) {
            val block = blocks.getOrNull(blockCursor)
            if (block != null && i == block.start) {
                if (block in keepBlocks) {
                    output.addAll(block.lines)
                }
                i = block.end + 1
                blockCursor++
            } else {
                val line = lines[i]
                if (line.isBlank() && output.lastOrNull().isNullOrBlank()) {
                    i++
                    continue
                }
                output += line
                i++
            }
        }

        // 去掉末尾空行
        while (output.lastOrNull()?.isBlank() == true) {
            output.removeAt(output.lastIndex)
        }

        return output.joinToString("\n")
    }

    /**
     * 剥离助手回复中的 JSON block，仅保留适合用户阅读的自然语言部分。
     * 支持 ```json fenced 块与裸 JSON 对象。
     */
    private fun stripJsonBlocks(text: String): String {
        var result = text

        // 1) 去掉 ```json fenced 区块
        val fencedJsonRegex = Regex("```json[\\s\\S]*?```", RegexOption.IGNORE_CASE)
        result = result.replace(fencedJsonRegex, "").trim()

        // 2) 如果整段就是一个 JSON 对象，直接视为纯元数据，返回空
        val trimmed = result.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return ""
        }

        // 3) 如果文本中尾部包含一个顶层 JSON，对用户价值不大，只保留 JSON 之前/之后的自然语言
        val start = trimmed.indexOf('{')
        if (start >= 0) {
            var depth = 0
            var endIndex = -1
            for (i in start until trimmed.length) {
                when (trimmed[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            endIndex = i
                            break
                        }
                    }
                }
            }
            if (endIndex > start) {
                val beforeJson = trimmed.substring(0, start).trimEnd()
                val afterJson = trimmed.substring(endIndex + 1).trimStart()
                result = listOf(beforeJson, afterJson)
                    .filter { it.isNotBlank() }
                    .joinToString("\n\n")
            }
        }

        return result
    }
    
    /**
     * 修复编号混乱问题（如 "11)1)"、"2)1)"、"11) 用户1)" 等）
     */
    private fun fixNumberingIssues(text: String): String {
        // 按行处理，每行单独修复
        return text.lines().joinToString("\n") { line ->
            var fixed = line
            
            // 检测渐进式累积模式：如 "11) 用户1) 用户询问1) 用户询问..."
            // 如果一行包含多个编号模式，很可能是累积
            val numberPattern = Regex("\\d+[)）]")
            val numberMatches = numberPattern.findAll(fixed).toList()
            
            if (numberMatches.size > 1) {
                // 找到所有编号位置
                val numbers = numberMatches.map { it.value }
                
                // 如果编号数量 >= 2，尝试提取最后一个编号后的内容
                // 但也要考虑可能是正常的列表格式（如 "1) 第一点 2) 第二点"）
                // 所以我们需要更智能的判断：如果编号之间没有明显的分隔（如换行或长文本），可能是累积
                
                // 检查是否是累积模式：编号之间只有短文本（< 10 字符）
                var isAccumulated = false
                for (i in 0 until numberMatches.size - 1) {
                    val start = numberMatches[i].range.last + 1
                    val end = numberMatches[i + 1].range.first
                    val between = fixed.substring(start, end).trim()
                    // 如果编号之间的文本很短（< 10 字符），可能是累积
                    if (between.length < 10 && !between.contains("。") && !between.contains("，")) {
                        isAccumulated = true
                        break
                    }
                }
                
                if (isAccumulated) {
                    // 累积模式：只保留最后一个编号及其后的内容
                    val lastMatch = numberMatches.last()
                    fixed = fixed.substring(lastMatch.range.first)
                } else {
                    // 正常列表格式：移除重复的编号前缀，只保留第一个
                    // 但保留所有内容
                    val firstMatch = numberMatches.first()
                    val afterFirst = fixed.substring(firstMatch.range.last + 1)
                    // 移除后续的编号前缀
                    fixed = firstMatch.value + " " + afterFirst.replace(Regex("\\d+[)）]\\s*"), "")
                }
            }
            
            // 修复重复编号模式：如 "11)1)" -> "1)"
            fixed = fixed.replace(Regex("(\\d+)[)）]\\s*(\\d+)[)）]"), "$1)")
            
            // 修复编号混乱的标题：如 "2)核心洞察1)" -> "核心洞察："
            fixed = fixed.replace(Regex("\\d+[)）]\\s*(核心洞察|下一步行动)\\s*\\d*[)）]?"), "$1：")
            
            fixed
        }
    }
    
    /**
     * 使用字符级别检测清理内容累积模式（用于最终后处理）
     * 更彻底地检测和清理累积内容
     */
    private fun cleanAccumulatedContentWithCharLevelDetection(text: String): String {
        val lines = text.lines()
        if (lines.size < 2) return text
        
        val result = mutableListOf<String>()
        val seenCharSequences = mutableSetOf<String>() // 字符序列集合
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (result.lastOrNull()?.isNotBlank() == true) {
                    result.add("")
                }
                continue
            }
            
            // 字符级别的累积检测：检查当前行是否包含前面行的字符序列
            val normalized = normalizeSentence(trimmed)
            var isAccumulated = false
            var maxMatchedLength = 0
            
            // 检查是否包含前面见过的长字符序列（> 20 字符）
            for (seenSeq in seenCharSequences) {
                if (seenSeq.length > 20 && normalized.contains(seenSeq)) {
                    isAccumulated = true
                    maxMatchedLength = maxOf(maxMatchedLength, seenSeq.length)
                }
            }
            
            // 如果匹配的字符序列超过当前行的 40%，认为是累积
            val accumulationRatio = if (normalized.isNotEmpty()) {
                maxMatchedLength.toDouble() / normalized.length
            } else {
                0.0
            }
            
            if (isAccumulated && accumulationRatio > 0.4) {
                // 尝试提取新内容：移除所有见过的字符序列
                var newContent = trimmed
                var removedAny = false
                
                // 按长度降序处理，先移除最长的匹配
                val sortedSeen = seenCharSequences.sortedByDescending { it.length }
                for (seenSeq in sortedSeen) {
                    if (seenSeq.length > 20) {
                        // 尝试在原始文本中找到对应的部分并移除
                        val originalSeq = findOriginalSequence(trimmed, seenSeq)
                        if (originalSeq != null) {
                            newContent = newContent.replace(originalSeq, "")
                            removedAny = true
                        }
                    }
                }
                newContent = newContent.trim()
                
                // 如果成功提取了新内容，保留
                if (removedAny && newContent.length > 5 && newContent.length < trimmed.length * 0.7) {
                    val newNormalized = normalizeSentence(newContent)
                    if (!seenCharSequences.contains(newNormalized)) {
                        seenCharSequences += newNormalized
                        result.add(newContent)
                    }
                }
                // 否则跳过这一行
            } else {
                // 正常行，直接添加
                seenCharSequences += normalized
                result.add(line)
            }
        }
        
        return result.joinToString("\n")
    }
    
    /**
     * 在原始文本中查找归一化字符序列对应的原始序列
     */
    private fun findOriginalSequence(original: String, normalizedSeq: String): String? {
        // 使用滑动窗口查找最长的匹配
        val windowSize = normalizedSeq.length * 2 // 考虑标点和空格
        var bestMatch: String? = null
        var bestMatchLength = 0
        
        for (i in 0 until original.length - windowSize) {
            val candidate = original.substring(i, minOf(i + windowSize, original.length))
            val candidateNormalized = normalizeSentence(candidate)
            
            // 检查候选文本的归一化版本是否包含目标序列
            if (candidateNormalized.contains(normalizedSeq)) {
                // 找到匹配，尝试精确定位
                val normalizedIndex = candidateNormalized.indexOf(normalizedSeq)
                if (normalizedIndex >= 0) {
                    // 找到对应的原始位置
                    val startPos = findCharIndexInOriginalForClass(candidate, normalizedIndex)
                    val endPos = findCharIndexInOriginalForClass(candidate, normalizedIndex + normalizedSeq.length)
                    if (startPos >= 0 && endPos > startPos) {
                        val match = candidate.substring(startPos, endPos)
                        if (match.length > bestMatchLength) {
                            bestMatch = match
                            bestMatchLength = match.length
                        }
                    } else if (startPos >= 0) {
                        // 如果只找到了起始位置，尝试扩展
                        val extendedEnd = minOf(startPos + normalizedSeq.length * 3, candidate.length)
                        val extendedMatch = candidate.substring(startPos, extendedEnd)
                        val extendedNormalized = normalizeSentence(extendedMatch)
                        if (extendedNormalized.contains(normalizedSeq) && extendedMatch.length > bestMatchLength) {
                            bestMatch = extendedMatch
                            bestMatchLength = extendedMatch.length
                        }
                    }
                }
            }
        }
        
        return bestMatch
    }
    
    /**
     * 清理内容累积模式：如果一行包含前面多行的内容，进行清理
     * 检测渐进式累积模式：如 "A) B) C) D)" 这种渐进式累积
     */
    private fun cleanAccumulatedContent(text: String): String {
        val lines = text.lines()
        if (lines.size < 2) return text
        
        val result = mutableListOf<String>()
        val seenSentences = mutableListOf<String>()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (result.lastOrNull()?.isNotBlank() == true) {
                    result.add("")
                }
                continue
            }
            
            // 检测渐进式累积模式：编号) 文本编号) 文本编号) 文本
            val numberPattern = Regex("\\d+[)）]")
            val numberMatches = numberPattern.findAll(trimmed).toList()
            
            // 如果包含多个编号，检查是否是累积模式
            var isProgressiveAccumulation = false
            if (numberMatches.size >= 2) {
                // 检查编号之间的文本是否逐渐累积
                var previousText = ""
                for (i in 0 until numberMatches.size - 1) {
                    val start = numberMatches[i].range.last + 1
                    val end = numberMatches[i + 1].range.first
                    val currentText = trimmed.substring(start, end).trim()
                    
                    // 如果当前文本包含前面的文本，可能是累积
                    if (previousText.isNotEmpty() && currentText.contains(previousText)) {
                        isProgressiveAccumulation = true
                        break
                    }
                    previousText = currentText
                }
            }
            
            if (isProgressiveAccumulation) {
                // 渐进式累积：只保留最后一个编号及其后的内容
                val lastMatch = numberMatches.last()
                val newContent = trimmed.substring(lastMatch.range.first).trim()
                if (newContent.isNotEmpty()) {
                    result.add(newContent)
                    seenSentences += normalizeSentence(newContent)
                }
                continue
            }
            
            val normalized = normalizeSentence(trimmed)
            
            // 检查是否是累积模式：如果当前行包含前面见过的多个长句子
            val accumulatedCount = seenSentences.count { existing ->
                existing.length > 15 && normalized.contains(existing)
            }
            
            // 计算重复内容的比例
            var totalRepeatedLength = 0
            seenSentences.forEach { existing ->
                if (existing.length > 15 && normalized.contains(existing)) {
                    totalRepeatedLength += existing.length
                }
            }
            val repetitionRatio = if (normalized.isNotEmpty()) {
                totalRepeatedLength.toDouble() / normalized.length
            } else {
                0.0
            }
            
            // 更激进的清理阈值：如果包含 1 个或以上的前面句子，或者重复比例 > 50%
            if (accumulatedCount >= 1 || repetitionRatio > 0.5) {
                // 尝试提取新内容：移除所有见过的句子
                var newContent = trimmed
                var removedAny = false
                seenSentences.forEach { existing ->
                    if (existing.length > 15) {
                        // 尝试在原始文本中找到并移除重复部分
                        val originalExisting = findOriginalSentence(trimmed, existing)
                        if (originalExisting != null && originalExisting.length > 15) {
                            newContent = newContent.replace(originalExisting, "")
                            removedAny = true
                        }
                    }
                }
                newContent = newContent.trim()
                
                // 如果还有新内容，保留；否则跳过
                if (newContent.length > 5 && (removedAny || newContent.length < trimmed.length * 0.7)) {
                    result.add(newContent)
                    seenSentences += normalizeSentence(newContent)
                }
            } else {
                // 正常行，直接添加
                result.add(line)
                seenSentences += normalized
            }
        }
        
        return result.joinToString("\n")
    }
    
    /**
     * 在原始文本中找到归一化索引对应的字符位置（外部类版本）
     */
    private fun findCharIndexInOriginalForClass(original: String, normalizedIndex: Int): Int {
        var normalizedCount = 0
        for (i in original.indices) {
            val char = original[i]
            if (!char.isWhitespace() && !Regex("[\\p{Punct}]").matches(char.toString())) {
                normalizedCount++
                if (normalizedCount > normalizedIndex) {
                    return i
                }
            }
        }
        return -1
    }
    
    /**
     * 在原始文本中查找归一化后的句子对应的原始句子
     */
    private fun findOriginalSentence(original: String, normalized: String): String? {
        // 简单的查找：尝试在原始文本中找到包含归一化内容的部分
        val words = normalized.chunked(3) // 将归一化内容分成3字符的块
        if (words.isEmpty()) return null
        
        // 查找第一个词块在原始文本中的位置
        val firstChunk = words.first()
        var startIndex = -1
        for (i in 0 until original.length - firstChunk.length) {
            val normalizedSubstring = normalizeSentence(original.substring(i, i + firstChunk.length))
            if (normalizedSubstring.contains(firstChunk)) {
                startIndex = i
                break
            }
        }
        
        if (startIndex == -1) return null
        
        // 从起始位置向后查找，直到找到完整的匹配
        var endIndex = startIndex + firstChunk.length
        while (endIndex < original.length) {
            val candidate = original.substring(startIndex, endIndex)
            val candidateNormalized = normalizeSentence(candidate)
            if (candidateNormalized.contains(normalized)) {
                return candidate
            }
            if (candidateNormalized.length > normalized.length * 1.5) {
                // 如果候选文本已经比归一化文本长很多，停止查找
                break
            }
            endIndex++
        }
        
        return null
    }
    
    /**
     * 归一化句子用于比较（去除标点、空格、编号等）
     */
    private fun normalizeSentence(sentence: String): String {
        // 移除编号模式
        val withoutNumbers = sentence.replace(Regex("^\\d+[)）]\\s*"), "")
            .replace(Regex("\\d+[)）]\\s*"), "")
        
        // 归一化：转小写，去除标点和多余空格
        return withoutNumbers.lowercase(Locale.getDefault())
            .replace(Regex("[\\p{Punct}\\s]+"), "")
    }

    private suspend fun handleGeneralChatMetadata(rawFullText: String) {
        val jsonText = extractLastJsonBlock(rawFullText)
        if (jsonText.isNullOrBlank()) {
            debugLog(
                event = "metadata_parse_skipped",
                data = mapOf("sessionId" to sessionId, "reason" to "no_json_found")
            )
            return
        }
        val metadata = parseGeneralChatMetadata(jsonText, sessionId)
        if (metadata == null) {
            warnLog(
                event = "general_chat_metadata_parse_failed",
                data = mapOf("sessionId" to sessionId, "reason" to "json_parse_null")
            )
            appendDebugNote("generalChatMetadataParseFailed")
            return
        }
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val merged = existing?.mergeWith(metadata) ?: metadata
        debugLog(
            event = "general_chat_metadata_parsed",
            data = mapOf(
                "sessionId" to sessionId,
                "mainPerson" to merged.mainPerson,
                "shortSummary" to merged.shortSummary,
                "title6" to merged.summaryTitle6Chars
            )
        )
        runCatching { metaHub.upsertSession(merged) }
            .onSuccess {
                debugLog(
                    event = "session_metadata_upsert",
                    data = mapOf(
                        "sessionId" to sessionId,
                        "mainPerson" to merged.mainPerson,
                        "shortSummary" to merged.shortSummary,
                        "title6" to merged.summaryTitle6Chars
                    )
                )
                updateTitleFromMetadata(merged)
            }
            .onFailure {
                warnLog(
                    event = "metahub_upsert_failed",
                    data = mapOf(
                        "sessionId" to sessionId,
                        "target" to "session"
                    ),
                    throwable = it
                )
                appendDebugNote("sessionMetaUpsertFailed")
            }
        updateDebugSessionMetadata(merged)
    }

    // TODO: Replace with Orchestrator-MetadataHub V2 spec metadata types when available
    private fun parseGeneralChatMetadata(jsonText: String, sessionId: String): SessionMetadata? = runCatching {
        val obj = org.json.JSONObject(jsonText)
        SessionMetadata(
            sessionId = sessionId,
            mainPerson = obj.optString("main_person").takeIf { it.isNotBlank() },
            shortSummary = obj.optString("short_summary").takeIf { it.isNotBlank() },
            summaryTitle6Chars = obj.optString("summary_title_6chars").takeIf { it.isNotBlank() }?.take(6),
            location = obj.optString("location").takeIf { it.isNotBlank() }
        )
    }.getOrNull()

    private suspend fun handleSmartAnalysisMetadata(rawFullText: String) {
        val jsonText = extractLastJsonBlock(rawFullText)
        if (jsonText.isNullOrBlank()) {
            debugLog(
                event = "smart_analysis_metadata_skipped",
                data = mapOf("sessionId" to sessionId, "reason" to "no_json_found")
            )
            return
        }
        val metadata = parseSmartAnalysisMetadata(jsonText, sessionId)
        if (metadata == null) {
            warnLog(
                event = "smart_analysis_metadata_parse_failed",
                data = mapOf("sessionId" to sessionId, "reason" to "json_parse_null")
            )
            appendDebugNote("smartAnalysisMetadataParseFailed")
            return
        }
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val merged = existing?.mergeWith(metadata) ?: metadata
        debugLog(
            event = "smart_analysis_metadata_parsed",
            data = mapOf(
                "sessionId" to sessionId,
                "mainPerson" to merged.mainPerson,
                "shortSummary" to merged.shortSummary,
                "title6" to merged.summaryTitle6Chars
            )
        )
        runCatching { metaHub.upsertSession(merged) }
            .onSuccess {
                debugLog(
                    event = "smart_analysis_meta_upsert",
                    data = mapOf(
                        "sessionId" to sessionId,
                        "mainPerson" to merged.mainPerson,
                        "shortSummary" to merged.shortSummary
                    )
                )
                updateTitleFromMetadata(merged)
            }
            .onFailure {
                warnLog(
                    event = "smart_analysis_meta_upsert_failed",
                    data = mapOf("sessionId" to sessionId),
                    throwable = it
                )
                appendDebugNote("smartAnalysisMetaUpsertFailed")
            }
        updateDebugSessionMetadata(merged)
    }

    // TODO: Replace with Orchestrator-MetadataHub V2 spec metadata types when available
    private fun parseSmartAnalysisMetadata(jsonText: String, sessionId: String): SessionMetadata? = runCatching {
        val obj = org.json.JSONObject(jsonText)
        val stageStr = obj.optString("stage").takeIf { it.isNotBlank() }
        val riskStr = obj.optString("risk_level").takeIf { it.isNotBlank() }
        SessionMetadata(
            sessionId = sessionId,
            mainPerson = obj.optString("main_person").takeIf { it.isNotBlank() },
            shortSummary = obj.optString("short_summary").takeIf { it.isNotBlank() },
            summaryTitle6Chars = obj.optString("summary_title_6chars").takeIf { it.isNotBlank() }?.take(6),
            location = obj.optString("location").takeIf { it.isNotBlank() },
            stage = stageStr?.let { 
                try { SessionStage.valueOf(it.uppercase()) } catch (e: IllegalArgumentException) { null }
            },
            riskLevel = riskStr?.let {
                try { RiskLevel.valueOf(it.uppercase()) } catch (e: IllegalArgumentException) { null }
            }
        )
    }.getOrNull()

    private fun extractLastJsonBlock(text: String): String? {
        val fenced = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            .findAll(text)
            .lastOrNull()?.groupValues?.getOrNull(1)?.trim()
        if (!fenced.isNullOrBlank()) return fenced
        val anyFence = Regex("```\\s*([\\s\\S]*?)```")
            .findAll(text)
            .lastOrNull()?.groupValues?.getOrNull(1)?.trim()
        if (!anyFence.isNullOrBlank()) return anyFence
        val trimmed = text.trim()
        return if (trimmed.startsWith("{") && trimmed.endsWith("}")) trimmed else null
    }
    
    /**
     * 根据问候语内容返回友好的回复
     */
    private fun getGreetingResponse(input: String): String {
        val normalized = input.trim().lowercase(Locale.getDefault())
        
        return when {
            normalized == "你好" || normalized == "您好" || normalized == "hello" || normalized == "hi" || normalized == "hey" || normalized == "嗨" -> {
                "你好！我是您的销售助手，可以帮助您：\n" +
                "• 分析客户对话和会议记录\n" +
                "• 生成销售话术和建议\n" +
                "• 总结工作日报\n" +
                "• 分析行业趋势和竞争情况\n\n" +
                "请告诉我您需要什么帮助？"
            }
            normalized == "你是谁" || normalized == "你是谁？" || normalized.contains("who are you") -> {
                "我是您的智能销售助手，专注于帮助销售团队提升工作效率。\n\n" +
                "我可以帮您：\n" +
                "• 智能分析：分析客户对话、会议记录，提取关键洞察\n" +
                "• 话术辅导：根据场景生成专业的销售话术\n" +
                "• 日报生成：整理工作摘要、行动项和风险提醒\n" +
                "• 趋势分析：分析行业动态和竞争格局\n\n" +
                "有什么我可以帮您的吗？"
            }
            normalized.contains("早上好") || normalized.contains("good morning") -> {
                "早上好！新的一天开始了，有什么销售相关的问题需要我帮助吗？"
            }
            normalized.contains("下午好") || normalized.contains("good afternoon") -> {
                "下午好！今天的工作进展如何？需要我帮您分析什么吗？"
            }
            normalized.contains("晚上好") || normalized.contains("good evening") -> {
                "晚上好！今天辛苦了，有什么需要我帮您总结或分析的吗？"
            }
            else -> {
                "我还不太确定你的需求，能再多提供一些上下文或问题细节吗？\n\n" +
                "我可以帮您：\n" +
                "• 分析客户对话或会议记录\n" +
                "• 生成销售话术和建议\n" +
                "• 总结工作日报\n" +
                "• 分析行业趋势"
            }
        }
    }
    
    /**
     * 流式去重器，在流式过程中实时去重，避免用户看到重复内容
     * 使用增量处理：只处理新增的内容，而不是重新处理整个累积内容
     */
    private class StreamingDeduplicator {
        fun mergeSnapshot(previous: String, snapshot: String): String {
            if (previous.isEmpty()) return snapshot
            if (snapshot.isEmpty()) return previous
            if (snapshot.startsWith(previous)) return snapshot
            if (previous.startsWith(snapshot)) return previous

            val overlap = findOverlap(previous, snapshot)
            return previous + snapshot.drop(overlap)
        }

        private fun findOverlap(a: String, b: String): Int {
            val max = minOf(a.length, b.length)
            for (len in max downTo 1) {
                if (a.endsWith(b.substring(0, len))) {
                    return len
                }
            }
            return 0
        }
    }

    private fun debugLog(event: String, data: Map<String, Any?> = emptyMap()) {
        if (!CHAT_DEBUG_HUD_ENABLED) return
        Log.d(TAG, formatLog(event, data))
    }

    private fun warnLog(
        event: String,
        data: Map<String, Any?> = emptyMap(),
        throwable: Throwable? = null
    ) {
        Log.w(TAG, formatLog(event, data), throwable)
    }

    private fun formatLog(event: String, data: Map<String, Any?>): String {
        if (data.isEmpty()) return "event=$event"
        val payload = data.entries.joinToString(", ") { entry -> "${entry.key}=${entry.value}" }
        return "event=$event, $payload"
    }

    companion object {
        private fun mapSkillToMode(skillId: QuickSkillId?): String? = when (skillId) {
            QuickSkillId.SUMMARIZE_LAST_MEETING -> "SUMMARY"
            QuickSkillId.EXTRACT_ACTION_ITEMS -> "OBJECTION_ANALYSIS"
            QuickSkillId.WRITE_FOLLOWUP_EMAIL -> "COACHING"
            QuickSkillId.PREP_NEXT_MEETING -> "DAILY_REPORT"
            QuickSkillId.SMART_ANALYSIS -> "SMART_ANALYSIS"
            else -> null
        }

        private fun buildTranscriptContext(fileName: String, transcript: String): String {
            return buildString {
                append("已加载录音 ").append(fileName).append(" 的转写内容。以下为通话文本，结合上下文回答用户问题：\n")
                append(transcript)
            }
        }

        internal fun deriveUserName(profile: UserProfile): String {
            if (!profile.displayName.isNullOrBlank()) return profile.displayName
            if (!profile.email.isNullOrBlank()) {
                val prefix = profile.email.substringBefore("@").ifBlank { profile.email }
                if (prefix.isNotBlank()) return prefix
            }
            return "用户"
        }
    }

    private suspend fun ensureSessionSummary(
        sessionId: String,
        titleOverride: String? = null
    ): AiSessionSummary {
        val existing = sessionRepository.findById(sessionId)
        val title = titleOverride ?: existing?.title ?: DEFAULT_SESSION_TITLE
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

    private fun buildTitleFromMetadata(meta: SessionMetadata, updatedAt: Long): String {
        val date = SimpleDateFormat("MM/dd", Locale.CHINA).format(Date(updatedAt))
        val person = meta.mainPerson?.takeIf { it.isNotBlank() } ?: "未知客户"
        val title = meta.summaryTitle6Chars?.takeIf { it.isNotBlank() }?.take(6) ?: "销售咨询"
        return "${date}_${person}_${title}"
    }

    private suspend fun maybeGenerateSessionTitle(request: ChatRequest, assistantText: String) {
        if (!request.isFirstAssistantReply) return
        if (request.quickSkillId != null) return
        val existingSummary = sessionRepository.findById(sessionId)
        if (existingSummary != null && existingSummary.title != DEFAULT_SESSION_TITLE) return
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val createdAt = existingSummary?.updatedAtMillis ?: System.currentTimeMillis()
        val title = meta?.let { buildTitleFromMetadata(it, createdAt) }
            ?: run {
                val firstUserMessage = _uiState.value.chatMessages
                    .firstOrNull { it.role == ChatMessageRole.USER }
                    ?.content
                    ?: return
                sessionTitleResolver.resolveTitle(
                    sessionId = sessionId,
                    updatedAtMillis = createdAt,
                    firstUserMessage = firstUserMessage,
                    firstAssistantMessage = assistantText
                )
            }
        sessionRepository.updateTitle(sessionId, title)
        applySessionList()
        _uiState.update { state ->
            val updatedCurrent = if (state.currentSession.id == sessionId) {
                state.currentSession.copy(title = title)
            } else {
                state.currentSession
            }
            state.copy(currentSession = updatedCurrent)
        }
        updateDebugSessionMetadata(meta)
    }

    private suspend fun updateTitleFromMetadata(meta: SessionMetadata) {
        val existingSummary = sessionRepository.findById(sessionId)
        if (existingSummary != null && existingSummary.title != DEFAULT_SESSION_TITLE) return
        val createdAt = existingSummary?.updatedAtMillis ?: meta.lastUpdatedAt
        val title = buildTitleFromMetadata(meta, createdAt)
        runCatching {
            sessionRepository.updateTitle(sessionId, title)
            applySessionList()
            _uiState.update { state ->
                val updatedCurrent = if (state.currentSession.id == sessionId) {
                    state.currentSession.copy(title = title)
                } else {
                    state.currentSession
                }
                state.copy(currentSession = updatedCurrent)
            }
            debugLog(
                event = "session_title_updated_from_metadata",
                data = mapOf("sessionId" to sessionId, "title" to title)
            )
        }.onFailure {
            warnLog(
                event = "session_title_update_failed",
                data = mapOf("sessionId" to sessionId, "title" to title),
                throwable = it
            )
            appendDebugNote("sessionTitleUpdateFailed")
        }
    }

    private fun saveImageToCache(uri: Uri): File {
        val cacheDir = File(appContext.cacheDir, "images").apply { mkdirs() }
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "image-${System.currentTimeMillis()}.jpg"
        val outFile = File(cacheDir, name)
        val resolver = appContext.contentResolver
        resolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("无法读取所选图片")
        return outFile
    }
}
