// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeViewModel.kt
// 模块：:feature:chat
// 说明：Home 页面状态与交互 ViewModel
// 作者：创建于 2025-12-15

package com.smartsales.feature.chat.home

import android.net.Uri
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import com.smartsales.domain.chat.AudioContextSummary
import com.smartsales.feature.chat.core.ChatHistoryItem
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.domain.config.QuickSkillCatalog
import com.smartsales.domain.config.QuickSkillDefinition
import com.smartsales.domain.config.QuickSkillId
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportOrchestrator
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.ExportNameResolver
import com.smartsales.core.metahub.ExportNameSource
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionTitlePolicy
import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.SessionMetadataLabelProvider
import com.smartsales.core.metahub.Provenance
import com.smartsales.core.metahub.RenamingTarget
import com.smartsales.core.metahub.setM3AcceptedName
import com.smartsales.feature.chat.home.CHAT_DEBUG_HUD_ENABLED
import com.smartsales.feature.chat.BuildConfig
import com.smartsales.feature.chat.history.toEntity
import com.smartsales.feature.chat.history.toUiModel
import com.smartsales.data.aicore.debug.DebugOrchestrator
import com.smartsales.data.aicore.debug.DebugSnapshot
import com.smartsales.domain.debug.DebugSessionMetadata
import com.smartsales.domain.export.ExportGateState
import com.smartsales.data.aicore.debug.TingwuTraceSnapshot
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.debug.XfyunTraceSnapshot
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.feature.chat.title.SessionTitleResolver
import com.smartsales.feature.chat.title.TitleResolver
import com.smartsales.domain.chat.TitleCandidate
import com.smartsales.domain.chat.TitleSource
import com.smartsales.feature.chat.AiSessionRepository as SessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.domain.error.toUserMessage
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.chat.ChatShareHandler
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.MediaClipStatus
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.usercenter.data.UserProfileRepository
import com.smartsales.feature.usercenter.UserProfile
import com.smartsales.feature.usercenter.SalesPersona
import com.smartsales.data.aicore.params.AiParaSettingsRepository
import com.smartsales.data.aicore.params.TranscriptionLaneSelector

import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.util.Optional
import com.smartsales.feature.chat.core.publisher.ChatPublisher
import com.smartsales.feature.chat.core.publisher.ChatPublisherImpl

import com.smartsales.feature.chat.core.publisher.GeneralChatV1Finalizer
import com.smartsales.feature.chat.core.publisher.ArtifactStatus
import com.smartsales.feature.chat.core.publisher.V1FinalizeResult
import com.smartsales.domain.stream.StreamingCoordinator
import com.smartsales.domain.transcription.V1BatchIndexPrefixGate
import com.smartsales.domain.transcription.V1TingwuWindowedChunkBuilder
import com.smartsales.feature.chat.core.v1.V1GeneralCompletionEvaluator
import com.smartsales.feature.chat.core.v1.V1GeneralRetryPolicy
import com.smartsales.feature.chat.core.v1.V1GeneralRetryEffects


private const val DEFAULT_SESSION_ID = "home-session"
private const val DEFAULT_SESSION_TITLE = SessionTitlePolicy.PLACEHOLDER_TITLE
private const val LONG_CONTENT_THRESHOLD = 240
private const val CONTEXT_LENGTH_LIMIT = 800
private const val CONTEXT_MESSAGE_LIMIT = 5
private const val ANALYSIS_CONTENT_LIMIT = 2400
private const val TAG = "HomeScreenVM"
private const val SMART_PLACEHOLDER_TEXT = "正在智能分析当前会话内容…"
private const val SMART_ANALYSIS_FAILURE_TEXT = "本次智能分析暂时不可用，请稍后重试。"

private typealias InputBucket = com.smartsales.domain.chat.InputBucket
private typealias AnalysisTarget = com.smartsales.domain.chat.AnalysisTarget

private fun defaultExportGateState(): ExportGateState {
    val resolution = ExportNameResolver.resolve(
        sessionId = DEFAULT_SESSION_ID,
        sessionTitle = null,
        isTitleUserEdited = null,
        meta = null
    )
    return ExportGateState(
        ready = false,
        reason = "智能分析未完成",
        resolvedName = resolution.baseName,
        nameSource = resolution.source
    )
}


/** 外部依赖（除 AiChatService 外保留原有 stub）。 */
interface AiSessionRepository {
    suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi>
}

/** HomeViewModel：驱动聊天、快捷技能以及设备/音频快照。 */
@HiltViewModel
class HomeViewModel @Inject constructor(
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
    private val metaHub: MetaHub,
    private val debugOrchestrator: DebugOrchestrator,
    private val xfyunTraceStore: XfyunTraceStore,
    private val tingwuTraceStore: TingwuTraceStore,
    private val aiParaSettingsRepository: AiParaSettingsRepository,

    private val exportCoordinator: com.smartsales.domain.export.ExportCoordinator,
    private val debugCoordinator: com.smartsales.domain.debug.DebugCoordinator,
    private val sessionsManager: com.smartsales.domain.sessions.SessionsManager,
    private val tingwuCoordinator: com.smartsales.domain.transcription.TranscriptionCoordinator,
    private val mediaInputCoordinator: com.smartsales.feature.chat.platform.MediaInputCoordinator,

    private val chatCoordinator: com.smartsales.domain.chat.ChatCoordinator,
    private val chatMetadataCoordinator: com.smartsales.domain.chat.ChatMetadataCoordinator,
    optionalConfig: Optional<AiCoreConfig> = Optional.empty(),
) : ViewModel() {

    private val quickSkillDefinitions = quickSkillCatalog.homeQuickSkills()
    private val quickSkillDefinitionsById = quickSkillDefinitions.associateBy { it.id }
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    private var latestMediaSyncState: MediaSyncState? = null
    private var sessionId: String = DEFAULT_SESSION_ID
    private var initialSessionPrepared: Boolean = false
    private var hasShownLowInfoHint: Boolean = false
    // 低信息智能分析提示是否已经出现过
    private var hasShownLowInfoSmartAnalysisHint: Boolean = false
    private var hasShownAnalysisExportHint: Boolean = false
    private var lastTranscriptionJobId: String? = null // kept for debug snapshot
    private var activelyTranscribing: Boolean = false // kept for recovery hint logic
    // 标记当前会话是否已处理首条助手回复，用于“首条决定标题”规则
    private var firstAssistantProcessed: Boolean = false
    private var latestAnalysisMarkdown: String? = null
    private var latestAnalysisMessageId: String? = null

    private val enableV1ChatPublisher = optionalConfig.orElse(AiCoreConfig()).enableV1ChatPublisher
    private val enableV1TingwuMacroWindowFilter =
        optionalConfig.orElse(AiCoreConfig()).enableV1TingwuMacroWindowFilter
    
    // Publishing infrastructure (moved from inline creation)
    private val chatPublisher: ChatPublisher = ChatPublisherImpl()
    private val v1Finalizer = GeneralChatV1Finalizer(chatPublisher)


    init {
        // 从 catalog 加载快捷技能到状态
        val skills = quickSkillDefinitions.map { it.toUiModel() }
        _uiState.update { it.copy(quickSkills = skills) }
        
        // Set ChatCoordinator scope
        (chatCoordinator as? com.smartsales.domain.chat.ChatCoordinatorImpl)?.setScope(viewModelScope)
        
        // Collect ChatCoordinator events (Wave 4: Event-based decoupling)
        viewModelScope.launch {
            chatCoordinator.chatEvents.collect { event ->
                when (event) {
                    is com.smartsales.domain.chat.ChatEvent.StreamStarted -> {
                        // Create assistant placeholder and mark streaming active
                        val placeholder = ChatMessageUi(
                            id = event.assistantId,
                            role = ChatMessageRole.ASSISTANT,
                            content = "",
                            timestampMillis = System.currentTimeMillis(),
                            isStreaming = true
                        )
                        _uiState.update {
                            it.copy(
                                chatMessages = it.chatMessages + placeholder,
                                isStreaming = true
                            )
                        }
                        updateWaveState()
                    }
                    is com.smartsales.domain.chat.ChatEvent.StreamDelta -> {
                        // Update assistant message with token
                        updateAssistantMessageWithToken(event.assistantId, event.token)
                    }
                    is com.smartsales.domain.chat.ChatEvent.StreamCompleted -> {
                        // Finalize message with completion result
                        handleChatEventCompleted(event.result)
                    }
                    is com.smartsales.domain.chat.ChatEvent.StreamError -> {
                        // Display error
                        handleChatEventError(event.assistantId, event.error)
                    }
                }
            }
        }
        

        
        
        
        loadUserProfile()
        viewModelScope.launch { prepareInitialSession() }
        firstAssistantProcessed = false
    }


    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
        updateWaveState()
    }

    fun onInputFocusChanged(focused: Boolean) {
        _uiState.update { it.copy(isInputFocused = focused) }
        updateWaveState()
    }

    fun onSendMessage() {
        val state = _uiState.value
        if (state.isSending) return
        val selectedSkill = state.selectedSkill?.id
        
        // Export skills are handled by ExportViewModel via UI callbacks
        
        val rawInput = state.inputText
        val content = rawInput.trim()
        
        // SmartAnalysis has special handling (deferred to P3.8)
        if (_uiState.value.isSmartAnalysisMode) {
            handleSmartAnalysisSend(content)
            return
        }
        
        val resolvedInput = when {
            content.isNotEmpty() -> content
            selectedSkill != null -> {
                quickSkillDefinitionsById[selectedSkill]?.defaultPrompt?.takeIf { it.isNotBlank() }
            }
            else -> null
        }
        if (resolvedInput.isNullOrBlank()) return
        
        // 低信息输入：不调用模型，避免无意义消耗与噪声对话
        if (selectedSkill == null && com.smartsales.domain.chat.InputClassifier.isLowInfoGeneralChatInput(resolvedInput)) {
            handleLowInfoGeneralChatInput()
            return
        }
        
        // Wave 4: Use ChatCoordinator for streaming (replaces ConversationViewModel path)
        val audioCtx = buildAudioContextSummary()
        val params = com.smartsales.domain.chat.SendMessageParams(
            sessionId = sessionId,
            userMessage = resolvedInput,
            skillId = selectedSkill?.name,
            audioContext = audioCtx?.let {
                com.smartsales.domain.chat.AudioContextSummary(
                    readyClipCount = it.readyClipCount,
                    pendingClipCount = it.pendingClipCount,
                    hasTranscripts = it.hasTranscripts,
                    note = it.note
                )
            },
            chatHistory = state.chatMessages.map { msg ->
                com.smartsales.domain.chat.ChatHistoryMessage(
                    role = when (msg.role) {
                        ChatMessageRole.USER -> com.smartsales.domain.chat.MessageRole.USER
                        ChatMessageRole.ASSISTANT -> com.smartsales.domain.chat.MessageRole.ASSISTANT
                    },
                    content = msg.sanitizedContent ?: msg.content
                )
            },
            isFirstAssistantReply = !firstAssistantProcessed,
            persona = state.salesPersona,
            timestamp = System.currentTimeMillis()
        )
        
        // Create user message UI
        val userMsg = ChatMessageUi(
            id = nextMessageId(),
            role = ChatMessageRole.USER,
            content = resolvedInput,
            timestampMillis = System.currentTimeMillis()
        )
        
        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg,
                isSending = true,
                showWelcomeHero = false,
                inputText = "" // Clear input after sending
            )
        }
        updateWaveState()
        
        // Invoke ChatCoordinator
        chatCoordinator.sendMessage(params)
    }


    private fun handleSmartAnalysisSend(rawInput: String) {
        if (_uiState.value.isSending) return
        val goal = rawInput.trim()
        
        // Wave 5: Use ChatCoordinator for SmartAnalysis (replaces ConversationViewModel path)
        val params = com.smartsales.domain.chat.SmartAnalysisParams(
            sessionId = sessionId,
            goal = goal,
            chatHistory = _uiState.value.chatMessages.map { msg ->
                com.smartsales.domain.chat.ChatHistoryMessage(
                    role = when (msg.role) {
                        ChatMessageRole.USER -> com.smartsales.domain.chat.MessageRole.USER
                        ChatMessageRole.ASSISTANT -> com.smartsales.domain.chat.MessageRole.ASSISTANT
                    },
                    content = msg.sanitizedContent ?: msg.content
                )
            },
            persona = _uiState.value.salesPersona,
            timestamp = System.currentTimeMillis()
        )
        
        chatCoordinator.sendSmartAnalysis(params)
    }



    fun onSmartAnalysisClicked() {
        // 直接复用智能分析主流程
        val input = _uiState.value.inputText
        handleSmartAnalysisSend(input)
    }


    /**
     * Get markdown for export (called by ExportViewModel).
     * Returns smart analysis markdown if available, otherwise builds from transcript.
     */
    fun getExportMarkdown(): String {
        val analysisMarkdown = findSmartAnalysisMarkdownForExport()
        return if (!analysisMarkdown.isNullOrBlank()) {
            wrapSmartAnalysisForExport(analysisMarkdown)
        } else {
            buildTranscriptMarkdown(_uiState.value.chatMessages)
        }
    }


    fun onSelectQuickSkill(skillId: QuickSkillId) {
        if (_uiState.value.isSending || _uiState.value.isStreaming) return
        
        // 15.3 Real Actions: Export is handled via specialized callbacks (onExportPdfClicked),
        // but if they come through here, we should delegate or ignore.
        if (skillId == QuickSkillId.EXPORT_PDF || skillId == QuickSkillId.EXPORT_CSV) {
            return
        }

        val definition = quickSkillDefinitionsById[skillId]
        if (definition == null) {
            _uiState.update { it.copy(snackbarMessage = "无法识别的快捷技能") }
            return
        }
        
        when (skillId) {
            QuickSkillId.SMART_ANALYSIS -> {
                // 15.3 Toggle Smart Analysis Mode
                _uiState.update { state ->
                    val toggled = !state.isSmartAnalysisMode
                    state.copy(
                        isSmartAnalysisMode = toggled,
                        selectedSkill = if (toggled) definition.toUiModel() else null,
                        inputText = if (toggled) "" else state.inputText // Clear input on toggle off? Or keep?
                    )
                }
            }
            else -> {
                // Standard Quick Skill: Pre-fill input
                _uiState.update { state ->
                    state.copy(
                        selectedSkill = definition.toUiModel(),
                        isSmartAnalysisMode = false,
                        inputText = definition.defaultPrompt,
                        chatErrorMessage = null
                    )
                }
            }
        }
    }

    private fun wrapSmartAnalysisForExport(body: String): String =
        com.smartsales.domain.chat.ChatMessageBuilder.wrapSmartAnalysisForExport(body)




    private fun findSmartAnalysisMarkdownForExport(): String? {
        latestAnalysisMarkdown?.takeIf { it.isNotBlank() }?.let { return it }
        val messageId = latestAnalysisMessageId
        if (messageId != null) {
            val match = _uiState.value.chatMessages.firstOrNull { it.id == messageId }
            val content = match?.sanitizedContent ?: match?.content
            if (!content.isNullOrBlank()) return content
        }
        // 重要：兜底只取已标记为智能分析的气泡，避免误导导出内容来源。
        val fallback = _uiState.value.chatMessages
            .lastOrNull { it.isSmartAnalysis }
            ?.sanitizedContent
            ?: _uiState.value.chatMessages.lastOrNull { it.isSmartAnalysis }?.content
        return fallback?.takeIf { it.isNotBlank() }
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



    /** 处理用户上传图片，保存并触发占位分析。 */
    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, isInputBusy = true, snackbarMessage = null, showWelcomeHero = false) }
            
            when (val result = mediaInputCoordinator.handleImagePick(uri)) {
                is Result.Success -> {
                    val saved = result.data
                    val placeholderId = nextMessageId()
                    val placeholder = ChatMessageUi(
                        id = placeholderId,
                        role = ChatMessageRole.ASSISTANT,
                        content = "已上传图片：${saved.name}，正在分析…",
                        rawContent = "已上传图片：${saved.name}，正在分析…",
                        sanitizedContent = "已上传图片：${saved.name}，正在分析…",
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
                        val raw = "图片分析占位：${saved.name}。图像解析接口接入后将提供详细描述。"
                        val display = displayAssistantText(raw = raw, sanitized = raw)
                        msg.copy(
                            content = display,
                            rawContent = raw,
                            sanitizedContent = raw,
                            isStreaming = false
                        )
                    }
                    _uiState.update { it.copy(isBusy = false, isInputBusy = false) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            isInputBusy = false,
                            snackbarMessage = result.throwable.message ?: "图片处理失败"
                        )
                    }
                }
            }
        }
    }


    fun onTranscriptionRequested(request: TranscriptionChatRequest) {
        viewModelScope.launch {
            lastTranscriptionJobId = request.jobId
            tingwuCoordinator.reset()
            debugCoordinator.refreshTraces()
            val transcript = request.transcriptMarkdown ?: request.transcriptPreview
            val targetSessionId = request.sessionId ?: DEFAULT_SESSION_ID
            val existing = sessionRepository.findById(targetSessionId)

            val baseTitle = when {
                existing?.title?.let { SessionTitlePolicy.isPlaceholder(it) } == true -> DEFAULT_SESSION_TITLE
                existing?.title != null -> existing.title
                else -> DEFAULT_SESSION_TITLE
            }
            val newTitle = baseTitle

            val preview = extractTranscriptPreview(request.transcriptMarkdown)

            val updated = (existing ?: AiSessionSummary(
                id = targetSessionId,
                title = newTitle,
                updatedAtMillis = System.currentTimeMillis(),
                lastMessagePreview = preview,
                isTranscription = true,
                pinned = false
            )).copy(
                title = newTitle,
                lastMessagePreview = preview,
                updatedAtMillis = System.currentTimeMillis(),
                isTranscription = true
            )

            sessionRepository.upsert(updated)
            
            val existingHistory = chatHistoryRepository.loadLatestSession(targetSessionId)
            this@HomeViewModel.sessionId = targetSessionId
            firstAssistantProcessed = false
            hasShownLowInfoHint = false
            hasShownAnalysisExportHint = false
            _uiState.update {
                it.copy(
                    currentSession = CurrentSessionUi(
                        id = targetSessionId,
                        title = newTitle,
                        isTranscription = updated.isTranscription
                    ),
                    chatMessages = existingHistory.map { item -> item.toUiModel() },
                    isLoadingHistory = false,
                    showWelcomeHero = false
                )
            }
            existingHistory.lastOrNull()?.content?.let { updateSessionSummary(it) }
            if (!transcript.isNullOrBlank()) {
                // Tingwu 结果复用：缓存命中时不重复“转写完成/逐字稿”，仅提示一次或直接给逐字稿
                val messages = mutableListOf<ChatMessageUi>()
                val noticeText = if (request.isFromCache) {
                    "已为你加载历史转写，可以直接总结要点或生成跟进话术。"
                } else {
                    "通话分析 · 已为你加载录音 ${request.fileName} 的转写内容，可以直接总结要点或生成跟进话术。"
                }
                val existingContents = _uiState.value.chatMessages.map { it.content }
                if (!existingContents.contains(noticeText)) {
                    messages += ChatMessageUi(
                        id = nextMessageId(),
                        role = ChatMessageRole.ASSISTANT,
                        content = noticeText,
                        rawContent = noticeText,
                        sanitizedContent = noticeText,
                        timestampMillis = System.currentTimeMillis()
                    )
                }
                if (!existingContents.contains(transcript)) {
                    messages += ChatMessageUi(
                        id = nextMessageId(),
                        role = ChatMessageRole.ASSISTANT,
                        content = transcript,
                        rawContent = transcript,
                        sanitizedContent = transcript,
                        timestampMillis = System.currentTimeMillis()
                    )
                }
                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages + messages,
                        snackbarMessage = null,
                        showWelcomeHero = false
                    )
                }
                persistMessagesAsync()
                updateSessionSummary(transcript)
            } else {
                val introNotice = ChatMessageUi(
                    id = nextMessageId(),
                    role = ChatMessageRole.ASSISTANT,
                    content = "通话分析 · 已为你加载录音 ${request.fileName} 的转写内容，我可以帮你总结对话、提炼要点或生成跟进话术。",
                    rawContent = "通话分析 · 已为你加载录音 ${request.fileName} 的转写内容，我可以帮你总结对话、提炼要点或生成跟进话术。",
                    sanitizedContent = "通话分析 · 已为你加载录音 ${request.fileName} 的转写内容，我可以帮你总结对话、提炼要点或生成跟进话术。",
                    timestampMillis = System.currentTimeMillis()
                )
                _uiState.update { state ->
                    state.copy(
                        chatMessages = state.chatMessages + introNotice,
                        showWelcomeHero = false
                    )
                }
                persistMessagesAsync()
                val introId = nextMessageId()
                val progressMessage = ChatMessageUi(
                    id = introId,
                    role = ChatMessageRole.ASSISTANT,
                    content = "正在转写音频文件 ${request.fileName} ...",
                    rawContent = "正在转写音频文件 ${request.fileName} ...",
                    sanitizedContent = "正在转写音频文件 ${request.fileName} ...",
                    timestampMillis = System.currentTimeMillis(),
                    isStreaming = true
                )
                _uiState.update {
                    it.copy(
                        chatMessages = _uiState.value.chatMessages + progressMessage,
                        snackbarMessage = null,
                        showWelcomeHero = false
                    )
                }
                persistMessagesAsync()
                // Delegate to TranscriptionCoordinator
                tingwuCoordinator.reset()
                tingwuCoordinator.startTranscription(request.jobId)
                tingwuCoordinator.runTranscription(
                    jobId = request.jobId,
                    fileName = request.fileName,
                    progressMessageId = introId,
                    onProgressUpdate = { percent, messageId ->
                        updateAssistantMessage(messageId) { msg ->
                            val raw = "正在转写音频文件 ${request.fileName} ... $percent%"
                            val display = displayAssistantText(raw = raw, sanitized = raw)
                            msg.copy(
                                content = display,
                                rawContent = raw,
                                sanitizedContent = raw,
                                isStreaming = true
                            )
                        }
                    },
                    onBatchReceived = { batch -> handleProcessedBatch(batch) },
                    onCompleted = { messageId ->
                        updateAssistantMessage(messageId, persistAfterUpdate = true) { msg ->
                            val raw = "转写完成：${request.fileName}"
                            val display = displayAssistantText(raw = raw, sanitized = raw)
                            msg.copy(
                                content = display,
                                rawContent = raw,
                                sanitizedContent = raw,
                                isStreaming = false
                            )
                        }
                        activelyTranscribing = false
                    },
                    onFailed = { reason, messageId ->
                        updateAssistantMessage(messageId, persistAfterUpdate = true) { msg ->
                            val raw = "转写失败：$reason"
                            val display = displayAssistantText(raw = raw, sanitized = raw)
                            msg.copy(
                                content = display,
                                rawContent = raw,
                                sanitizedContent = raw,
                                isStreaming = false,
                                hasError = true
                            )
                        }
                        activelyTranscribing = false
                        _uiState.update { it.copy(snackbarMessage = it.snackbarMessage ?: reason) }
                    }
                )
            }
        }
    }

    // Simplified batch handler - receives processed batches from TranscriptionViewModel
    private fun handleProcessedBatch(batch: com.smartsales.feature.chat.home.transcription.ProcessedBatch) {
        val effectiveChunk = batch.effectiveChunk
        val isFinal = batch.isFinal
        val currentMessageId = tingwuCoordinator.state.value.currentMessageId

        // Skip empty chunks unless we have an existing message
        if (effectiveChunk.isBlank() && currentMessageId == null) {
            if (isFinal) {
                tingwuCoordinator.markFinal()
            }
            return
        }

        val merged = if (currentMessageId == null) {
            effectiveChunk
        } else {
            tingwuCoordinator.mergeChunks(
                existing = _uiState.value.chatMessages.firstOrNull { it.id == currentMessageId }?.rawContent
                    ?: _uiState.value.chatMessages.firstOrNull { it.id == currentMessageId }?.content
                    ?: "",
                incoming = effectiveChunk
            )
        }

        if (currentMessageId == null) {
            // Create new message
            val messageId = nextMessageId()
            tingwuCoordinator.setCurrentMessageId(messageId)
            val display = displayAssistantText(raw = merged, sanitized = merged)
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + ChatMessageUi(
                        id = messageId,
                        role = ChatMessageRole.ASSISTANT,
                        content = display,
                        rawContent = merged,
                        sanitizedContent = merged,
                        timestampMillis = System.currentTimeMillis(),
                        isStreaming = !isFinal
                    ),
                    showWelcomeHero = false
                )
            }
            if (isFinal) {
                persistMessagesAsync()
                viewModelScope.launch { updateSessionSummary(merged) }
            }
        } else {
            // Update existing message
            updateAssistantMessage(currentMessageId, persistAfterUpdate = isFinal) { msg ->
                val display = displayAssistantText(raw = merged, sanitized = merged)
                msg.copy(
                    content = display,
                    rawContent = merged,
                    sanitizedContent = merged,
                    isStreaming = !isFinal
                )
            }
            if (isFinal) {
                viewModelScope.launch { updateSessionSummary(merged) }
            }
        }

        if (isFinal) {
            tingwuCoordinator.markFinal()
        }

        if (CHAT_DEBUG_HUD_ENABLED && _uiState.value.showDebugMetadata) {
            debugCoordinator.refreshTraces()
        }
    }


    fun onOpenDrawer() {
        _uiState.update { it.copy(navigationRequest = HomeNavigationRequest.ChatHistory) }
    }

    fun onTapProfile() {
        _uiState.update { it.copy(navigationRequest = HomeNavigationRequest.UserCenter) }
    }



    fun onDismissError() {
        _uiState.update { it.copy(snackbarMessage = null, chatErrorMessage = null) }
        updateWaveState()
    }

    /**
     * Update the current session's title (called when SessionListViewModel emits TitleRenamed).
     */
    fun updateCurrentSessionTitle(newTitle: String) {
        _uiState.update {
            it.copy(currentSession = it.currentSession.copy(title = newTitle))
        }
    }

    fun onNavigationConsumed() {
        _uiState.update { it.copy(navigationRequest = null) }
    }

    fun toggleDebugMetadata() {
        debugCoordinator.toggleDebugPanel()
        val visible = debugCoordinator.debugState.value.visible
        _uiState.update { it.copy(showDebugMetadata = visible) }
        if (visible) {
            // 打开时刷新调试数据
            viewModelScope.launch {
                val sessionTitle = _uiState.value.currentSession.title
                val summary = sessionRepository.findById(sessionId)
                debugCoordinator.refreshSessionMetadata(sessionId, sessionTitle)
                debugCoordinator.refreshDebugSnapshot(
                    sessionId = sessionId,
                    jobId = lastTranscriptionJobId,
                    sessionTitle = sessionTitle,
                    isTitleUserEdited = summary?.isTitleUserEdited
                )
                debugCoordinator.refreshTraces()
                // Wire traces from DebugUiState to HomeUiState for HUD display
                val debugState = debugCoordinator.debugState.value
                _uiState.update {
                    it.copy(
                        tingwuTrace = debugState.tingwuTrace,
                        xfyunTrace = debugState.xfyunTrace,
                        debugSnapshot = debugState.snapshot
                    )
                }
            }
        }
    }

    fun setShowRawAssistantOutput(showRaw: Boolean) {
        val displayList = applyAssistantDisplay(_uiState.value.chatMessages, showRaw)
        _uiState.update {
            it.copy(
                showRawAssistantOutput = if (BuildConfig.DEBUG) showRaw else false,
                chatMessages = displayList
            )
        }
    }

    fun refreshXfyunTrace() {
        if (!CHAT_DEBUG_HUD_ENABLED) return
        debugCoordinator.refreshTraces()
        // Wire traces from DebugUiState to HomeUiState for HUD display
        val debugState = debugCoordinator.debugState.value
        _uiState.update {
            it.copy(
                tingwuTrace = debugState.tingwuTrace,
                xfyunTrace = debugState.xfyunTrace,
                debugSnapshot = debugState.snapshot
            )
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
        val stageLabel = meta?.stage?.let { SessionMetadataLabelProvider.stageLabel(it) }
        val riskLabel = meta?.riskLevel?.let { SessionMetadataLabelProvider.riskLabel(it) }
        val tagsLabel = meta?.tags
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                SessionMetadataLabelProvider.tagsLabel(
                    tags = it,
                    limit = Int.MAX_VALUE,
                    delimiter = "、",
                    maxLength = Int.MAX_VALUE,
                    sort = true
                )
            }
        val latestSourceLabel = meta?.latestMajorAnalysisSource
            ?.let { SessionMetadataLabelProvider.sourceLabel(it) }
        val latestAtLabel = SessionMetadataLabelProvider
            .timeLabel(meta?.latestMajorAnalysisAt)
            .takeIf { it.isNotBlank() }
        val reasoning = buildSmartReasoningStrip(meta)
        val laneDecision = TranscriptionLaneSelector.resolve(aiParaSettingsRepository.snapshot())
        val debug = DebugSessionMetadata(
            sessionId = sessionId,
            title = title,
            mainPerson = meta?.mainPerson,
            shortSummary = meta?.shortSummary,
            summaryTitle6Chars = meta?.summaryTitle6Chars,
            stageLabel = stageLabel,
            riskLabel = riskLabel,
            tagsLabel = tagsLabel,
            latestSourceLabel = latestSourceLabel,
            latestAtLabel = latestAtLabel,
            // 重要：HUD 需展示转写链路选择与禁用原因，避免“看起来切了但实际没生效”。
            transcriptionProviderRequested = laneDecision.requestedProvider,
            transcriptionProviderSelected = laneDecision.selectedProvider,
            transcriptionProviderDisabledReason = laneDecision.disabledReason,
            transcriptionXfyunEnabledSetting = laneDecision.xfyunEnabledSetting,
            notes = mergedNotes
        )
        val messageId = meta?.latestMajorAnalysisMessageId
        if (!messageId.isNullOrBlank()) {
            latestAnalysisMessageId = messageId
        }
        _uiState.update {
            val updatedMessages = if (messageId != null) {
                it.chatMessages.map { msg ->
                    if (msg.id == messageId) msg.copy(isSmartAnalysis = true) else msg
                }
            } else {
                it.chatMessages
            }
            it.copy(
                debugSessionMetadata = debug,
                smartReasoningText = reasoning,
                chatMessages = updatedMessages
            )
        }
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
            if (sessionId != this@HomeViewModel.sessionId) {
                this@HomeViewModel.sessionId = sessionId
                firstAssistantProcessed = false
                lastTranscriptionJobId = null
                tingwuCoordinator.reset()
                latestAnalysisMarkdown = null
                hasShownLowInfoHint = false
                hasShownAnalysisExportHint = false
                _uiState.update { it.copy(isSmartAnalysisMode = false, selectedSkill = null) }
                _uiState.update { it.copy(isSmartAnalysisMode = false, selectedSkill = null) }
                loadSession(sessionId)
            }
            if (!allowHero) {
                _uiState.update { it.copy(showWelcomeHero = false) }
            }
            val summary = ensureSessionSummary(sessionId, titleOverride, isTranscription)
            _uiState.update {
                it.copy(
                    currentSession = CurrentSessionUi(
                        id = sessionId,
                        title = summary.title,
                        isTranscription = summary.isTranscription || isTranscription
                    ),
                    showWelcomeHero = if (allowHero) it.showWelcomeHero else false
                )
            }
            refreshDebugSessionMetadata()
            debugCoordinator.refreshTraces()
        }
    }



    fun onNewChatClicked() {
        viewModelScope.launch {
            val newSession = sessionsManager.createNewSession()
            sessionId = newSession.id
            firstAssistantProcessed = false
            latestAnalysisMarkdown = null
            hasShownLowInfoHint = false
            hasShownAnalysisExportHint = false
            _uiState.update { it.copy(isSmartAnalysisMode = false, selectedSkill = null) }
            _uiState.update {
                it.copy(
                    currentSession = CurrentSessionUi(
                        id = newSession.id,
                        title = newSession.title,
                        isTranscription = false
                    ),
                    chatMessages = emptyList(),
                    isLoadingHistory = false,
                    showWelcomeHero = true
                )
            }
            updateDebugSessionMetadata(null)
            // Export gate is now checked on-demand via exportViewModel.checkExportGate()
            chatHistoryRepository.saveMessages(newSession.id, emptyList())
            chatHistoryRepository.saveMessages(newSession.id, emptyList())
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
                _uiState.update { it.copy(isLoadingHistory = false, showWelcomeHero = true) }
                enforcePlaceholderForHeroIfNeeded()
            }
        }
    }

    private fun persistMessagesAsync() {
        val snapshot = _uiState.value.chatMessages.map { it.toEntity(sessionId) }
        viewModelScope.launch {
            chatHistoryRepository.saveMessages(sessionId, snapshot)
        }
    }








    private fun updateAssistantMessage(
        assistantId: String,
        persistAfterUpdate: Boolean = false,
        transformer: (ChatMessageUi) -> ChatMessageUi
    ) {
        val current = _uiState.value
        val index = current.chatMessages.indexOfFirst { it.id == assistantId }

        if (index == -1) {
            // ✅ 找不到就直接返回，避免 RuntimeException
            return
        }

        val updatedList = current.chatMessages.toMutableList()
        val old = updatedList[index]
        updatedList[index] = transformer(old)

        _uiState.update {
            it.copy(chatMessages = updatedList)
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
        val display = displayAssistantText(raw = content, sanitized = content)
        val message = ChatMessageUi(
            id = nextMessageId(),
            role = ChatMessageRole.ASSISTANT,
            content = display,
            rawContent = content,
            sanitizedContent = content,
            timestampMillis = System.currentTimeMillis(),
            isStreaming = isStreaming,
            hasError = hasError
        )
        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + message,
                showWelcomeHero = false
            )
        }
        persistMessagesAsync()
        viewModelScope.launch {
            updateSessionSummary(message.sanitizedContent ?: message.content)
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
                content = ui.sanitizedContent ?: ui.content
            )
        }
        return ChatRequest(
            sessionId = sessionId,
            userMessage = userMessage,
            quickSkillId = mapSkillToMode(skillId),
            audioContextSummary = audioContext,
            history = history,
            isFirstAssistantReply = !firstAssistantProcessed,
            persona = _uiState.value.salesPersona
        )
    }

    private fun buildTranscriptMarkdown(messages: List<ChatMessageUi>): String =
        com.smartsales.domain.chat.ChatMessageBuilder.buildTranscriptMarkdown(messages)

    private fun createUserMessage(content: String): ChatMessageUi = ChatMessageUi(
        id = nextMessageId(),
        role = ChatMessageRole.USER,
        content = content,
        rawContent = content,
        sanitizedContent = content,
        timestampMillis = System.currentTimeMillis()
    )

    private fun createAssistantPlaceholder(
        content: String = "",
        isSmartAnalysis: Boolean = false
    ): ChatMessageUi = ChatMessageUi(
        id = nextMessageId(),
        role = ChatMessageRole.ASSISTANT,
        content = content,
        rawContent = content,
        sanitizedContent = content,
        timestampMillis = System.currentTimeMillis(),
        isStreaming = true,
        isSmartAnalysis = isSmartAnalysis
    )

    private fun shouldShowRaw(): Boolean = BuildConfig.DEBUG && _uiState.value.showRawAssistantOutput

    private fun displayAssistantText(
        raw: String?,
        sanitized: String?
    ): String {
        val rawCandidate = if (shouldShowRaw()) raw else null
        return rawCandidate ?: sanitized ?: raw.orEmpty()
    }

    /** GENERAL 回复的 channels 数据（Visible2User 和 Metadata） */
    private data class GeneralChannels(
        val visibleText: String?,
        val metadataJson: String?
        // Note: renameCandidate moved to ChatCoordinator (M0 consolidation)
    )

    /** 提取 GENERAL 回复中的 channels */
    private fun extractGeneralChannels(raw: String): GeneralChannels {
        // Delegate extraction to domain layer (Wave 8A consolidation)
        // Note: Rename candidate extraction now happens in ChatCoordinatorImpl
        val channels = com.smartsales.domain.chat.ChatPublisher.extractChannels(raw)
        return GeneralChannels(
            visibleText = channels.visibleText,
            metadataJson = channels.metadataJson
        )
    }

    // NOTE: parseRenameCandidate moved to ChatCoordinatorImpl (M0 consolidation)
    // See: domain/chat/ChatCoordinatorImpl.kt:parseRenameCandidate()

    private fun applyAssistantDisplay(
        messages: List<ChatMessageUi>,
        showRaw: Boolean
    ): List<ChatMessageUi> {
        val allowRaw = BuildConfig.DEBUG && showRaw
        return messages.map { msg ->
            if (msg.role != ChatMessageRole.ASSISTANT) return@map msg
            val display = if (allowRaw) {
                msg.rawContent ?: msg.content
            } else {
                msg.sanitizedContent ?: msg.content
            }
            msg.copy(content = display)
        }
    }



    private fun handleLowInfoGeneralChatInput() {
        // 清空输入框，避免用户反复点击发送同一段内容
        _uiState.update { it.copy(inputText = "") }
        if (hasShownLowInfoHint) return
        hasShownLowInfoHint = true
        appendAssistantMessage("内容太少，我还无法理解你的需求。你可以补充：客户背景、问题、你的目标。")
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


    private fun formatSyncTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private fun buildSmartReasoningStrip(meta: SessionMetadata?): String? =
        ReasoningStripFormatter.build(
            metadata = meta,
            labelProvider = SessionMetadataLabelProvider
        )

    override fun onCleared() {
        super.onCleared()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            userProfileRepository.profileFlow.collect { profile ->
                _uiState.update { it.copy(userName = deriveUserName(profile), salesPersona = profile.salesPersona) }
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

    // NOTE: handleGeneralChatMetadata, handleV1GeneralChatMetadata, persistLatestAnalysisMarker
    // moved to ChatMetadataCoordinatorImpl (2026-01-11 extraction)

    private fun onAnalysisCompleted(summary: String, messageId: String) {
        latestAnalysisMarkdown = summary
        latestAnalysisMessageId = messageId

        if (!hasShownAnalysisExportHint) {
            hasShownAnalysisExportHint = true
            // 导出提示改为非对话层面的 UI（例如顶部 banner / icon 提示），
            // 不再插入额外的对话气泡，避免打断会话流和影响"单条智能分析卡片"结构。
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
        titleOverride: String? = null,
        isTranscription: Boolean = false
    ): AiSessionSummary {
        val existing = sessionRepository.findById(sessionId)
        val resolvedTitle = when {
            titleOverride != null -> titleOverride
            existing?.title?.let { SessionTitlePolicy.isPlaceholder(it) } == true -> DEFAULT_SESSION_TITLE
            else -> existing?.title ?: DEFAULT_SESSION_TITLE
        }
        val resolvedIsTranscription = existing?.isTranscription
            ?: isTranscription
            || (existing?.title?.startsWith(SessionTitlePolicy.LEGACY_TRANSCRIPTION_PREFIX) == true)
        val preview = existing?.lastMessagePreview ?: ""
        val summary = AiSessionSummary(
            id = sessionId,
            title = resolvedTitle,
            lastMessagePreview = preview,
            updatedAtMillis = existing?.updatedAtMillis ?: System.currentTimeMillis(),
            isTitleUserEdited = existing?.isTitleUserEdited ?: false,
            isTranscription = resolvedIsTranscription,
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
            isTitleUserEdited = existing?.isTitleUserEdited ?: false,
            isTranscription = existing?.isTranscription ?: _uiState.value.currentSession.isTranscription,
            pinned = existing?.pinned ?: false
        )
        sessionRepository.upsert(summary)
    }

    private suspend fun maybeResolveSessionTitle(
        meta: SessionMetadata?,
        candidate: TitleCandidate?
    ) {
        val existingSummary = sessionRepository.findById(sessionId)
        val resolved = TitleResolver.resolveTitle(existingSummary, candidate, meta)
        if (resolved.isNullOrBlank()) return
        sessionRepository.updateTitle(sessionId, resolved)
        _uiState.update { state ->
            val updatedCurrent = if (state.currentSession.id == sessionId) {
                state.currentSession.copy(title = resolved)
            } else {
                state.currentSession
            }
            state.copy(currentSession = updatedCurrent)
        }
    }

    private suspend fun prepareInitialSession() {
        if (initialSessionPrepared) return
        initialSessionPrepared = true
        val newSession = sessionsManager.createNewSession()
        sessionId = newSession.id
        firstAssistantProcessed = false
        chatHistoryRepository.saveMessages(newSession.id, emptyList())
        _uiState.update {
            it.copy(
                currentSession = CurrentSessionUi(
                    id = newSession.id,
                    title = newSession.title,
                    isTranscription = newSession.isTranscription
                ),
                chatMessages = emptyList(),
                isLoadingHistory = false,
                showWelcomeHero = true
            )
        }
        updateDebugSessionMetadata(null)
        // Export gate is now checked on-demand via exportViewModel.checkExportGate()
    }

    private suspend fun enforcePlaceholderForHeroIfNeeded() {
        val state = _uiState.value
        if (!state.showWelcomeHero || state.chatMessages.isNotEmpty()) return
        val existing = sessionRepository.findById(sessionId)
        if (existing?.isTitleUserEdited == true) return
        if (SessionTitlePolicy.isPlaceholder(state.currentSession.title)) return
        val placeholder = SessionTitlePolicy.newChatPlaceholder()
        sessionRepository.updateTitle(sessionId, placeholder)
        _uiState.update {
            it.copy(currentSession = it.currentSession.copy(title = placeholder))
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

    private fun extractTranscriptPreview(markdown: String?): String {
        if (markdown.isNullOrBlank()) return ""

        // Take the first non-blank line, strip leading markdown headings like "#", "##"
        val firstLine = markdown.lines()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?: return ""

        return firstLine
            .removePrefix("#")
            .removePrefix("#")
            .trim()
    }


    // Live streaming state for ChatCoordinator events
    private var streamingDeduplicator = StreamingDeduplicator()

    // ====== ChatCoordinator Event Handlers (Wave 4) ======

    private fun updateAssistantMessageWithToken(assistantId: String, token: String) {
        updateAssistantMessage(assistantId) { msg ->
            val merged = streamingDeduplicator.mergeSnapshot(
                current = msg.content,
                incoming = token
            )
            msg.copy(
                content = merged,
                isStreaming = true
            )
        }
    }

    private suspend fun handleChatEventCompleted(result: com.smartsales.domain.chat.ChatCompletionResult) {
        // Update message with final content
        updateAssistantMessage(result.assistantId, persistAfterUpdate = true) { msg ->
            val display = displayAssistantText(raw = result.rawFullText, sanitized = result.displayText)
            msg.copy(
                content = display,
                rawContent = result.rawFullText,
                sanitizedContent = result.displayText,
                isStreaming = false,
                isSmartAnalysis = result.isSmartAnalysis
            )
        }

        // Update metadata if present (first reply)
        if (result.metadata != null) {
            updateDebugSessionMetadata(result.metadata)
        }

        // Handle title candidate if present (now uses domain type directly, M0 consolidation)
        if (result.titleCandidate != null) {
            maybeResolveSessionTitle(result.metadata, result.titleCandidate)
        }

        // Update UI state
        _uiState.update { 
            it.copy(
                isSending = false, 
                isStreaming = false, 
                isInputBusy = false, 
                isBusy = false
            ) 
        }
        updateWaveState()

        // Update session summary
        val previewText = result.metadata?.shortSummary?.takeIf { it.isNotBlank() } ?: result.displayText
        updateSessionSummary(previewText)

        // Handle SmartAnalysis completion
        if (result.isSmartAnalysis) {
            val updatedMeta = chatMetadataCoordinator.persistLatestAnalysisMarker(
                sessionId, AnalysisSource.SMART_ANALYSIS_USER, result.assistantId
            )
            if (updatedMeta != null) updateDebugSessionMetadata(updatedMeta)
            onAnalysisCompleted(result.displayText, result.assistantId)
        }
    }

    private fun handleChatEventError(assistantId: String, error: com.smartsales.domain.error.ChatError) {
        val errorMessage = error.toUserMessage()
        updateAssistantMessage(assistantId, persistAfterUpdate = true) { msg ->
            msg.copy(content = errorMessage, hasError = true, isStreaming = false)
        }

        _uiState.update {
            it.copy(
                isSending = false,
                isStreaming = false,
                isInputBusy = false,
                isBusy = false,
                chatErrorMessage = error.toUserMessage()
            )
        }
        updateWaveState()
    }

    // Wave Sync Helper
    private fun calculateWaveState(
        isSending: Boolean,
        isStreaming: Boolean,
        hasError: Boolean,
        inputText: String,
        isInputFocused: Boolean
    ): com.smartsales.feature.chat.home.components.MotionState {
        return when {
            hasError -> com.smartsales.feature.chat.home.components.MotionState.Error
            isStreaming -> com.smartsales.feature.chat.home.components.MotionState.Listening // Streaming = Listening/Absorbing
            isSending -> com.smartsales.feature.chat.home.components.MotionState.Thinking // Sending = Thinking/Processing
            // If user is typing or focused, we could be "Listening" but for now let's keep it Idle or handle externally
            isInputFocused && inputText.isNotBlank() -> com.smartsales.feature.chat.home.components.MotionState.Idle // subtle pulsing?
            else -> com.smartsales.feature.chat.home.components.MotionState.Idle
        }
    }

    private fun updateWaveState() {
        _uiState.update { current ->
            current.copy(
                waveState = calculateWaveState(
                    isSending = current.isSending,
                    isStreaming = current.isStreaming,
                    hasError = current.chatErrorMessage != null,
                    inputText = current.inputText,
                    isInputFocused = current.isInputFocused
                )
            )
        }
    }
}


