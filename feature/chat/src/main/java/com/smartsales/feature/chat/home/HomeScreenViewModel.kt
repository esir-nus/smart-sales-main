// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
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
import com.smartsales.feature.chat.home.debug.DebugSessionMetadata
import com.smartsales.feature.chat.home.export.ExportGateState
import com.smartsales.data.aicore.debug.TingwuTraceSnapshot
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.debug.XfyunTraceSnapshot
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.feature.chat.title.SessionTitleResolver
import com.smartsales.feature.chat.title.TitleCandidate
import com.smartsales.feature.chat.title.TitleResolver
import com.smartsales.feature.chat.title.TitleSource
import com.smartsales.feature.chat.AiSessionRepository as SessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
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
import com.smartsales.feature.chat.core.stream.ChatStreamCoordinator
import com.smartsales.feature.chat.core.transcription.V1BatchIndexPrefixGate
import com.smartsales.feature.chat.core.transcription.V1TingwuWindowedChunkBuilder
import com.smartsales.feature.chat.core.v1.V1GeneralCompletionEvaluator
import com.smartsales.feature.chat.core.v1.V1GeneralRetryPolicy
import com.smartsales.feature.chat.core.v1.V1GeneralRetryEffects

/*
 * HomeScreenViewModel.kt — NAV INDEX (for Codex / AI directed reads)
 *
 * 使用方式：在编辑器里搜索下面的锚点（例如：HSVM@PUBLISH）快速跳转。
 * 注意：不要依赖行号（会漂移），只依赖锚点。
 *
 * [HSVM@TOP]          文件顶部 / 常量 / 初始化
 * [HSVM@PUBLIC_API]   对外 public API（UI 调用入口）
 * [HSVM@ENTRY]        用户事件入口（send / event / intent）
 * [HSVM@STREAM]       Streaming / DisplayDelta 相关
 * [HSVM@PUBLISH]      V1 Chat 发布流水线（visible2user 提取 / MachineArtifact）
 * [HSVM@RETRY]        Retry loop / terminal 处理（含 maxRetries / FAILED）
 * [HSVM@META]         Metadata 写入路径（含 L3-only gating）
 * [HSVM@HELPERS]      strip/sanitize/parse 等 helper（严禁 JSON 泄露）
 * [HSVM@HUD]          DebugSnapshot / HUD / trace 输出
 */

// [HSVM@TOP] ===== 文件顶部 / 常量 / 初始化 =====
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

// [HSVM@PUBLIC_API] ===== 对外 public API（UI 调用入口） =====
/** HomeScreenViewModel：驱动聊天、快捷技能以及设备/音频快照。 */
/**
 * NAV: docs/index/HomeScreenViewModel.index.md
 * Tags: [HSVM:STREAMING_PIPELINE] [HSVM:RETRY_LOOP] [HSVM:CHAT_PUBLISH]
 *       [HSVM:TINGWU_BATCH_RELEASE] [HSVM:PREFIX_GATE] [HSVM:MACRO_WINDOW_FILTER]
 *       [HSVM:DEBUG_SNAPSHOT] [HSVM:SESSION_BOOTSTRAP]
 */
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
    private val metaHub: MetaHub,
    private val debugOrchestrator: DebugOrchestrator,
    private val xfyunTraceStore: XfyunTraceStore,
    private val tingwuTraceStore: TingwuTraceStore,
    private val aiParaSettingsRepository: AiParaSettingsRepository,

    private val exportCoordinator: com.smartsales.domain.export.ExportCoordinator,
    private val debugCoordinator: com.smartsales.domain.debug.DebugCoordinator,
    private val sessionsManager: com.smartsales.domain.sessions.SessionsManager,
    private val tingwuCoordinator: com.smartsales.domain.transcription.TranscriptionCoordinator,
    private val conversationViewModel: com.smartsales.feature.chat.conversation.ConversationViewModel,
    optionalConfig: Optional<AiCoreConfig> = Optional.empty(),
) : ViewModel(), com.smartsales.feature.chat.conversation.StreamingCallbacks {

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
    private var pendingExportAfterAnalysis: ExportFormat? = null
    private var exportAutoAnalysisInFlight: Boolean = false
    private val enableV1ChatPublisher = optionalConfig.orElse(AiCoreConfig()).enableV1ChatPublisher
    private val enableV1TingwuMacroWindowFilter =
        optionalConfig.orElse(AiCoreConfig()).enableV1TingwuMacroWindowFilter
    
    // Publishing infrastructure (moved from inline creation)
    private val chatPublisher: ChatPublisher = ChatPublisherImpl()
    private val v1Finalizer = GeneralChatV1Finalizer(chatPublisher)

    // [HSVM:SESSION_BOOTSTRAP]
    init {
        // 从 catalog 加载快捷技能到状态
        val skills = quickSkillDefinitions.map { it.toUiModel() }
        _uiState.update { it.copy(quickSkills = skills) }
        
        // P3.1.B1: Sync ConversationViewModel.state -> HomeUiState
        viewModelScope.launch {
            conversationViewModel.state.collect { conversationState ->
                _uiState.update { it.copy(inputText = conversationState.inputText) }
            }
        }
        
        // P3.1.B2: Migration bridge for message sync (TODO: remove in P3.8)
        conversationViewModel.onMessagesChanged = { messages ->
            _uiState.update { it.copy(chatMessages = messages) }
        }
        
        // P3.8: Migration bridge for SmartAnalysis state sync (TODO: remove in P3.9)
        conversationViewModel.onSmartAnalysisModeChanged = { mode, goal ->
            _uiState.update { it.copy(
                isSmartAnalysisMode = mode,
                smartAnalysisGoal = goal
            ) }
        }
        
        observeDeviceConnection()
        observeMediaSync()
        observeSessions()
        loadUserProfile()
        viewModelScope.launch { prepareInitialSession() }
        firstAssistantProcessed = false
    }

    // [HSVM@ENTRY] ===== 用户事件入口（send / event / intent） =====
    fun onInputChanged(text: String) {
        // P3.1.B1: Dispatch to ConversationReducer
        conversationViewModel.dispatch(
            com.smartsales.feature.chat.conversation.ConversationIntent.InputChanged(text)
        )
        // State sync handled by collector in init
    }

    fun onInputFocusChanged(focused: Boolean) {
        _uiState.update { it.copy(isInputFocused = focused) }
    }

    fun onSendMessage() {
        val state = _uiState.value
        if (state.isSending) return
        val selectedSkill = state.selectedSkill?.id
        
        // Export skills bypass chat flow
        when (selectedSkill) {
            QuickSkillId.EXPORT_PDF -> {
                onExportPdfClicked()
                return
            }
            QuickSkillId.EXPORT_CSV -> {
                onExportCsvClicked()
                return
            }
            else -> Unit
        }
        
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
        
        // P3.1.B2: Dispatch to ConversationReducer for normal chat
        conversationViewModel.dispatch(
            com.smartsales.feature.chat.conversation.ConversationIntent.SendMessage(System.currentTimeMillis()),
            scope = viewModelScope,
            context = com.smartsales.feature.chat.conversation.SendContext(
                sessionId = sessionId,
                salesPersona = _uiState.value.salesPersona,
                isFirstAssistant = !firstAssistantProcessed
            )
        )
    }

    private fun handleSmartAnalysisSend(rawInput: String) {
        if (_uiState.value.isSending) return
        val goal = rawInput.trim()
        
        // P3.8: Dispatch SmartAnalysis through Reducer
        conversationViewModel.dispatch(
            com.smartsales.feature.chat.conversation.ConversationIntent.SendSmartAnalysis(
                timestamp = System.currentTimeMillis(),
                goal = goal
            ),
            scope = viewModelScope,
            analysisContext = com.smartsales.feature.chat.conversation.SmartAnalysisContext(
                sessionId = sessionId,
                messages = _uiState.value.chatMessages,
                salesPersona = _uiState.value.salesPersona
            )
        )
    }

    private fun startSmartAnalysisForExport(): Boolean {
        if (_uiState.value.isSending || _uiState.value.isStreaming || exportAutoAnalysisInFlight) {
            return false
        }
        val target = com.smartsales.domain.chat.InputClassifier.findSmartAnalysisPrimaryContent("", _uiState.value.chatMessages)
        if (target == null) {
            _uiState.update { it.copy(snackbarMessage = "内容太少，无法智能分析，已取消导出") }
            return false
        }
        val contextContent = com.smartsales.domain.chat.InputClassifier.findContextForAnalysis(target.content, _uiState.value.chatMessages)
        val userMessage = buildSmartAnalysisUserMessage(
            mainContent = target.content,
            context = contextContent,
            goal = "通用分析"
        )
        exportAutoAnalysisInFlight = true
        // 说明：导出触发的智能分析不清空输入框，避免打断用户输入。
        sendMessageInternal(
            messageText = userMessage,
            skillOverride = QuickSkillId.SMART_ANALYSIS,
            userDisplayText = "智能分析（为导出准备）",
            isAutoAnalysis = true,
            preserveInputText = true
        )
        return true
    }

    fun onSmartAnalysisClicked() {
        // 直接复用智能分析主流程
        val input = _uiState.value.inputText
        handleSmartAnalysisSend(input)
    }

    fun onExportPdfClicked() {
        onExportRequested(ExportFormat.PDF)
    }

    fun onExportCsvClicked() {
        onExportRequested(ExportFormat.CSV)
    }

    private fun onExportRequested(format: ExportFormat) {
        if (_uiState.value.exportInProgress) return
        viewModelScope.launch {
            // Check export gate via ExportViewModel
            val gate = exportCoordinator.checkExportGate(sessionId)
            
            if (gate.ready) {
                // Gate passed: prepare markdown and export
                pendingExportAfterAnalysis = null
                exportAutoAnalysisInFlight = false
                val analysisMarkdown = findSmartAnalysisMarkdownForExport()
                val markdown = if (!analysisMarkdown.isNullOrBlank()) {
                    wrapSmartAnalysisForExport(analysisMarkdown)
                } else {
                    buildTranscriptMarkdown(_uiState.value.chatMessages)
                }
                _uiState.update { it.copy(exportInProgress = true) }
                val result = exportCoordinator.performExport(sessionId, format, _uiState.value.userName, markdown)
                when (result) {
                    is Result.Success -> _uiState.update { it.copy(exportInProgress = false) }
                    is Result.Error -> _uiState.update { 
                        it.copy(
                            exportInProgress = false,
                            chatErrorMessage = result.throwable.message ?: "导出失败"
                        )
                    }
                }
                return@launch
            }

            // Gate not ready: start smart analysis and queue export
            pendingExportAfterAnalysis = format
            val formatLabel = if (format == ExportFormat.PDF) "PDF" else "CSV"
            if (exportAutoAnalysisInFlight || _uiState.value.isSending || _uiState.value.isStreaming) {
                _uiState.update { it.copy(snackbarMessage = "智能分析进行中，完成后将自动导出${formatLabel}") }
                return@launch
            }
            if (startSmartAnalysisForExport()) {
                _uiState.update { it.copy(snackbarMessage = "已开始智能分析，完成后将自动导出${formatLabel}") }
            } else {
                pendingExportAfterAnalysis = null
            }
        }
    }

    fun onSelectQuickSkill(skillId: QuickSkillId) {
        if (_uiState.value.isSending || _uiState.value.isStreaming) return
        if (skillId == QuickSkillId.EXPORT_PDF || skillId == QuickSkillId.EXPORT_CSV) {
            // 导出快捷技能为立即动作：不走“选中 + 发送”流程。
            val format = if (skillId == QuickSkillId.EXPORT_PDF) ExportFormat.PDF else ExportFormat.CSV
            onExportRequested(format)
            return
        }
        val definition = quickSkillDefinitionsById[skillId]
        if (definition == null) {
            _uiState.update { it.copy(snackbarMessage = "无法识别的快捷技能") }
            return
        }
        when (skillId) {
            QuickSkillId.SMART_ANALYSIS -> {
                _uiState.update { state ->
                    val toggled = !state.isSmartAnalysisMode
                    state.copy(
                        isSmartAnalysisMode = toggled,
                        selectedSkill = if (toggled) definition.toUiModel() else null,
                        inputText = state.inputText
                    )
                }
            }
            else -> {
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

    private fun maybeStartPendingExportAnalysis() {
        val pending = pendingExportAfterAnalysis ?: return
        if (exportAutoAnalysisInFlight || _uiState.value.isSending || _uiState.value.isStreaming) return
        // 说明：导出被排队时，等待对话空闲后再自动触发智能分析。
        viewModelScope.launch {
            val gate = exportCoordinator.checkExportGate(sessionId)
            if (gate.ready) {
                // Gate ready: trigger export immediately
                pendingExportAfterAnalysis = null
                val analysisMarkdown = findSmartAnalysisMarkdownForExport()
                val markdown = if (!analysisMarkdown.isNullOrBlank()) {
                    wrapSmartAnalysisForExport(analysisMarkdown)
                } else {
                    buildTranscriptMarkdown(_uiState.value.chatMessages)
                }
                _uiState.update { it.copy(exportInProgress = true) }
                val result = exportCoordinator.performExport(sessionId, pending, _uiState.value.userName, markdown)
                when (result) {
                    is Result.Success -> _uiState.update { it.copy(exportInProgress = false) }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            exportInProgress = false,
                            chatErrorMessage = result.throwable.message ?: "导出失败"
                        )
                    }
                }
                return@launch
            }
            val formatLabel = if (pending == ExportFormat.PDF) "PDF" else "CSV"
            if (startSmartAnalysisForExport()) {
                _uiState.update { it.copy(snackbarMessage = "已开始智能分析，完成后将自动导出${formatLabel}") }
            } else {
                pendingExportAfterAnalysis = null
            }
        }
    }

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

    /**
     * 处理用户上传的本地音频，保存到本地并提交 Tingwu 转写。
     * 复用当前聊天 sessionId，避免为每次音频上传创建新会话。
     */
    fun onAudioFilePicked(uri: Uri) {
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            activelyTranscribing = true
            // 重要：转写进行中不展示恢复提示
            _uiState.update { it.copy(showAudioRecoveryHint = false, audioRecoveryHintStartedAt = null) }
            persistAudioTaskStartedMarker(startedAt)
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
                uploadPayload = uploadPayload,
                sessionId = sessionId
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(isInputBusy = false, isBusy = false, snackbarMessage = "音频已上传，正在转写…") }
                    onTranscriptionRequested(
                        TranscriptionChatRequest(
                            jobId = submit.data,
                            fileName = stored.displayName,
                            recordingId = stored.id,
                            sessionId = sessionId
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
            this@HomeScreenViewModel.sessionId = targetSessionId
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
                // Start transcription observation via TranscriptionViewModel
                tingwuCoordinator.reset()
                tingwuCoordinator.startTranscription(request.jobId)
                // Observe processed batches
                launch {
                    tingwuCoordinator.observeProcessedBatches(request.jobId).collect { batch ->
                        handleProcessedBatch(batch)
                    }
                }
                // Observe job state
                launch {
                    tingwuCoordinator.observeJob(request.jobId).collectLatest { state ->
                        when (state) {
                            AudioTranscriptionJobState.Idle -> Unit
                            is AudioTranscriptionJobState.InProgress -> updateAssistantMessage(introId) { msg ->
                                val raw = "正在转写音频文件 ${request.fileName} ... ${state.progressPercent}%"
                                val display = displayAssistantText(raw = raw, sanitized = raw)
                                msg.copy(
                                    content = display,
                                    rawContent = raw,
                                    sanitizedContent = raw,
                                    isStreaming = true
                                )
                            }

                            is AudioTranscriptionJobState.Completed -> {
                                updateAssistantMessage(introId, persistAfterUpdate = true) { msg ->
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
                                persistAudioTaskFinishedMarker(System.currentTimeMillis())
                            }

                            is AudioTranscriptionJobState.Failed -> {
                                val reason = state.reason.ifBlank { "转写失败" }
                                updateAssistantMessage(introId, persistAfterUpdate = true) { msg ->
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
                                refreshAudioRecoveryHintFromMetaHub()
                                _uiState.update { it.copy(snackbarMessage = it.snackbarMessage ?: reason) }
                            }
                        }
                    }
                }
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
            mergeTranscriptionChunks(
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

    private fun mergeTranscriptionChunks(existing: String, incoming: String): String {
        if (existing.isBlank()) return incoming
        if (incoming.isBlank()) return existing
        // 重要：伪流式只做追加，避免重复或乱序。
        val separator = when {
            existing.endsWith("\n") || incoming.startsWith("\n") -> ""
            else -> "\n"
        }
        return existing + separator + incoming
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
            refreshAudioRecoveryHint(meta)
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
                firstAssistantProcessed = false
                lastTranscriptionJobId = null
                tingwuCoordinator.reset()
                latestAnalysisMarkdown = null
                hasShownLowInfoHint = false
                hasShownAnalysisExportHint = false
                _uiState.update { it.copy(isSmartAnalysisMode = false, selectedSkill = null) }
                _uiState.update { it.copy(isSmartAnalysisMode = false, selectedSkill = null) }
                updateSessionListSelection()
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

    fun setQuickSkills(skills: List<QuickSkillUi>) {
        _uiState.update { it.copy(quickSkills = skills) }
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
            updateSessionListSelection()
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
            sessionsManager.sessionList.collectLatest { list ->
                updateSessionListSelection(list)
            }
        }
        viewModelScope.launch {
            sessionsManager.uiState.collectLatest { state ->
                _uiState.update {
                    it.copy(
                        historyActionSession = state.historyActionSession,
                        showHistoryRenameDialog = state.showHistoryRenameDialog,
                        historyRenameText = state.historyRenameText
                    )
                }
            }
        }
    }

    private fun updateSessionListSelection(list: List<SessionListItemUi>? = null) {
        val targetList = list ?: _uiState.value.sessionList
        val currentId = sessionId
        val mapped = targetList.map { item ->
            item.copy(isCurrent = item.id == currentId)
        }
        _uiState.update { it.copy(sessionList = mapped) }
    }

    fun onHistorySessionLongPress(sessionId: String) {
        viewModelScope.launch {
            sessionsManager.onHistorySessionLongPress(sessionId)
        }
    }

    fun onHistoryActionDismiss() {
        sessionsManager.onHistoryActionDismiss()
    }

    fun onHistoryActionRenameStart() {
        sessionsManager.onHistoryActionRenameStart()
    }

    fun onHistoryRenameTextChange(text: String) {
        sessionsManager.onHistoryRenameTextChange(text)
    }

    fun onHistorySessionPinToggle(sessionId: String) {
        viewModelScope.launch {
            sessionsManager.onHistorySessionPinToggle(sessionId)
        }
    }

    fun onHistorySessionRenameConfirmed(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            val updatedTitle = sessionsManager.onHistorySessionRenameConfirmed(sessionId, newTitle)
            if (updatedTitle != null && sessionId == this@HomeScreenViewModel.sessionId) {
                // 如果当前会话被重命名，同步更新当前会话标题
                _uiState.update {
                    it.copy(currentSession = it.currentSession.copy(title = updatedTitle))
                }
            }
        }
    }

    fun onHistorySessionDelete(sessionId: String) {
        viewModelScope.launch {
            val result = sessionsManager.onHistorySessionDelete(sessionId, this@HomeScreenViewModel.sessionId)
            if (result is com.smartsales.domain.sessions.SessionsManager.DeleteResult.CurrentSessionDeleted) {
                val next = result.nextSession
                this@HomeScreenViewModel.sessionId = next.id
                firstAssistantProcessed = false
                _uiState.update {
                    it.copy(
                        currentSession = CurrentSessionUi(
                            id = next.id,
                            title = next.title,
                            isTranscription = next.isTranscription
                        ),
                        chatMessages = emptyList(),
                        showWelcomeHero = true
                    )
                }
                loadSession(next.id)
            }
        }
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

    // 共享的发送 helper：输入框和快捷技能都走这里
    private fun sendMessageInternal(
        messageText: String,
        skillOverride: QuickSkillId? = null,
        userDisplayText: String? = null,
        onCompleted: (String) -> Unit = {},
        onCompletedTransform: ((String) -> String)? = null,
        isAutoAnalysis: Boolean = false,
        preserveInputText: Boolean = false
    ) {
        val content = messageText.trim()
        if (content.isEmpty() || _uiState.value.isSending || _uiState.value.isStreaming) return
        _uiState.update { it.copy(chatErrorMessage = null) }
        val quickSkill = (skillOverride ?: _uiState.value.selectedSkill?.id)?.let { id ->
            quickSkillDefinitionsById[id]
        }
        val quickSkillId = quickSkill?.id
        val userMessage = createUserMessage(userDisplayText ?: content)
        val isSmartAnalysis = quickSkillId == QuickSkillId.SMART_ANALYSIS
        val assistantPlaceholder = if (isSmartAnalysis) {
            createAssistantPlaceholder(
                content = SMART_PLACEHOLDER_TEXT,
                isSmartAnalysis = true
            )
        } else {
            createAssistantPlaceholder()
        }
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
            inputText = if (preserveInputText) _uiState.value.inputText else "",
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
        startStreamingResponse(
            com.smartsales.feature.chat.conversation.StreamingContext(
                request = request,
                assistantId = assistantPlaceholder.id,
                onCompleted = onCompleted,
                onCompletedTransform = onCompletedTransform,
                isAutoAnalysis = isAutoAnalysis
            )
        )
    }

    // [HSVM@STREAM] ===== Streaming / DisplayDelta 相关 =====
    // [HSVM:STREAMING_PIPELINE]
    /** 启动 streaming，更新最后一条助手气泡。Delegates to ConversationVM. */
    private var streamingDeduplicator = StreamingDeduplicator()
    
    private fun startStreamingResponse(context: com.smartsales.feature.chat.conversation.StreamingContext) {
        val request = context.request
        val assistantId = context.assistantId
        val isSmartAnalysis = request.quickSkillId == "SMART_ANALYSIS"
        streamingDeduplicator = StreamingDeduplicator()

        // Set context for callbacks
        currentStreamingRequest = request
        currentOnCompleted = context.onCompleted
        currentOnCompletedTransform = context.onCompletedTransform
        currentIsAutoAnalysis = context.isAutoAnalysis

        if (isSmartAnalysis) {
            updateAssistantMessage(assistantId) { msg ->
                msg.copy(content = SMART_PLACEHOLDER_TEXT, isStreaming = true)
            }
        }

        // V1 retry config: only for general chat
        val v1RetryConfig = if (enableV1ChatPublisher && request.quickSkillId == null && !isSmartAnalysis) {
            com.smartsales.feature.chat.conversation.V1RetryConfig(maxRetries = 2, enableReasonAware = true)
        } else null

        // Delegate to ConversationViewModel with all V1 retry logic centralized there
        conversationViewModel.startStreaming(
            context = context,
            callbacks = this,
            scope = viewModelScope,
            v1RetryConfig = v1RetryConfig
        )
    }

    // [HSVM@PUBLISH] ===== V1 Chat 发布流水线 =====
    // [HSVM:CHAT_PUBLISH]
    private suspend fun handleStreamCompleted(
        request: ChatRequest,
        assistantId: String,
        rawFullText: String,
        onCompleted: (String) -> Unit,
        onCompletedTransform: ((String) -> String)?,
        isAutoAnalysis: Boolean,
        isSmartAnalysis: Boolean
    ) {
        // 完成：关闭 streaming，最终清理和去重
        val isGeneralChat = request.quickSkillId == null
        val isFirstGeneralReply = isGeneralChat && request.isFirstAssistantReply && !firstAssistantProcessed
        val useV1Publisher = enableV1ChatPublisher && isGeneralChat && !isSmartAnalysis
        // V1 模式：仅由 Publisher 决定可见文本，禁止启发式提取
        val v1Result = if (useV1Publisher) v1Finalizer.finalize(rawFullText) else null

        // 首条 GENERAL 回复处理开始时，无条件标记为已处理（确保后续回复不会被当作首条）
        if (isGeneralChat && isFirstGeneralReply) {
            firstAssistantProcessed = true
        }

        // 提取 GENERAL 的 channels（Visible2User 和 Metadata）
        val channels = if (isGeneralChat && !useV1Publisher) {
            extractGeneralChannels(rawFullText)
        } else {
            GeneralChannels(null, null, null)
        }
        val metadataJson = channels.metadataJson

        // 仅首条 GENERAL 回复允许解析并写入元数据
        val generalMeta = if (!useV1Publisher && isFirstGeneralReply) {
            // V1 模式不走旧的元数据解析，避免启发式 JSON 写入
            handleGeneralChatMetadata(rawFullText, metadataJson)
        } else {
            null
        }
        val v1Meta = if (useV1Publisher && isFirstGeneralReply) {
            handleV1GeneralChatMetadata(v1Result)
        } else {
            null
        }

        val latestMeta = if (useV1Publisher) {
            v1Meta ?: runCatching { metaHub.getSession(sessionId) }.getOrNull()
        } else {
            generalMeta ?: runCatching { metaHub.getSession(sessionId) }.getOrNull()
        }

        // <Visible2User> 驱动显示，rawContent 保留原始文本（含标签）
        val visibleText = channels.visibleText
        // Wave 9: Delegate display text resolution to domain layer
        val cleaned = com.smartsales.domain.chat.ChatPublisher.resolveDisplayText(
            rawFullText = rawFullText,
            visibleText = visibleText,
            isSmartAnalysis = isSmartAnalysis,
            useV1Publisher = useV1Publisher,
            v1VisibleMarkdown = v1Result?.visibleMarkdown,
            onCompletedTransform = if (isSmartAnalysis) null else { text -> 
                val transformed = onCompletedTransform?.invoke(text) ?: text
                applyGeneralOutputGuards(transformed)
            }
        )
        val isSmartFailure = isSmartAnalysis && cleaned.trim() == SMART_ANALYSIS_FAILURE_TEXT
        if (isSmartAnalysis && !isSmartFailure) {
            // 根据是否是导出前自动分析，区分来源
            val source = if (pendingExportAfterAnalysis != null) {
                AnalysisSource.SMART_ANALYSIS_AUTO
            } else {
                AnalysisSource.SMART_ANALYSIS_USER
            }
            persistLatestAnalysisMarker(source, assistantId)
            onAnalysisCompleted(cleaned, assistantId)
        } else if (isSmartAnalysis && isSmartFailure) {
            pendingExportAfterAnalysis = null
            _uiState.update { it.copy(exportInProgress = false) }
        }
        if (isSmartAnalysis && isAutoAnalysis) {
            exportAutoAnalysisInFlight = false
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
            val display = displayAssistantText(raw = rawFullText, sanitized = cleaned)
            msg.copy(
                content = display,
                rawContent = rawFullText, // 保留原始文本（含所有标签）
                sanitizedContent = cleaned,
                isStreaming = false
            )
        }
        onCompleted(cleaned)
        // 顺序保持不变，确保状态与展示一致
        _uiState.update { it.copy(isSending = false, isStreaming = false, isInputBusy = false, isBusy = false) }
        // 说明：若导出被排队，当前对话完成后再尝试触发自动分析。
        maybeStartPendingExportAnalysis()
        if (isFirstGeneralReply) {
            // 自动标题仅在首条助手回复时尝试，优先 Rename 渠道，缺失时回退元数据
            maybeResolveSessionTitle(latestMeta, channels.renameCandidate)
        }
        val previewText = latestMeta?.shortSummary?.takeIf { it.isNotBlank() } ?: cleaned
        viewModelScope.launch {
            updateSessionSummary(previewText)
            updateSessionListSelection()
        }
    }

    private fun handleStreamError(
        request: ChatRequest,
        assistantId: String,
        throwable: Throwable,
        isAutoAnalysis: Boolean,
        isSmartAnalysis: Boolean
    ) {
        // 错误：标记消息失败并弹出提示
        warnLog(
            event = "chat_stream_error",
            data = mapOf(
                "sessionId" to sessionId,
                "mode" to (request.quickSkillId ?: "GENERAL_CHAT")
            ),
            throwable = throwable
        )
        if (isSmartAnalysis) {
            val errorText = throwable.message?.takeIf { it.isNotBlank() }
                ?: SMART_ANALYSIS_FAILURE_TEXT
            updateAssistantMessage(assistantId, persistAfterUpdate = true) { msg ->
                msg.copy(
                    content = errorText,
                    rawContent = errorText,
                    sanitizedContent = errorText,
                    hasError = true,
                    isStreaming = false
                )
            }
        } else {
            updateAssistantMessage(assistantId, persistAfterUpdate = true) { msg ->
                msg.copy(hasError = true, isStreaming = false)
            }
        }
        if (isSmartAnalysis && isAutoAnalysis) {
            exportAutoAnalysisInFlight = false
        }
        pendingExportAfterAnalysis = null
        _uiState.update {
            it.copy(
                isSending = false,
                isStreaming = false,
                isInputBusy = false,
                isBusy = false,
                exportInProgress = false,
                chatErrorMessage = throwable.message ?: "AI 回复失败"
            )
        }
        // 说明：若导出被排队，失败后也尝试触发自动分析。
        maybeStartPendingExportAnalysis()
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
        val metadataJson: String?,
        val renameCandidate: TitleCandidate?
    )

    /** 提取 GENERAL 回复中的 channels */
    private fun extractGeneralChannels(raw: String): GeneralChannels {
        // Delegate extraction to domain layer (Wave 8A consolidation)
        val channels = com.smartsales.domain.chat.ChatPublisher.extractChannels(raw)
        val rename = parseRenameCandidate(raw, TitleSource.GENERAL)
        return GeneralChannels(
            visibleText = channels.visibleText,
            metadataJson = channels.metadataJson,
            renameCandidate = rename
        )
    }

    /** 解析 <Rename> 标签，提取 Name 与 Title6，供自动改名使用。 */
    private fun parseRenameCandidate(raw: String, source: TitleSource): TitleCandidate? {
        val blockRegex = Regex("<\\s*Rename\\s*>([\\s\\S]*?)<\\s*/\\s*Rename\\s*>", RegexOption.IGNORE_CASE)
        val block = blockRegex.find(raw)?.groupValues?.getOrNull(1)?.trim() ?: return null
        val nameRegex = Regex("<\\s*Name\\s*>([\\s\\S]*?)<\\s*/\\s*Name\\s*>", RegexOption.IGNORE_CASE)
        val titleRegex = Regex("<\\s*Title6\\s*>([\\s\\S]*?)<\\s*/\\s*Title6\\s*>", RegexOption.IGNORE_CASE)
        val name = nameRegex.find(block)?.groupValues?.getOrNull(1)?.trim()
        val title6 = titleRegex.find(block)?.groupValues?.getOrNull(1)?.trim()
        if (name.isNullOrBlank() && title6.isNullOrBlank()) return null
        return TitleCandidate(
            name = name?.takeIf { it.isNotBlank() },
            title6 = title6?.takeIf { it.isNotBlank() },
            source = source,
            createdAt = System.currentTimeMillis()
        )
    }

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

    private fun latestUserContent(): String? =
        _uiState.value.chatMessages.lastOrNull { it.role == ChatMessageRole.USER }?.content?.trim()

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

    private suspend fun persistLatestAnalysisMarker(
        source: AnalysisSource,
        messageId: String
    ) {
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val base = existing ?: SessionMetadata(sessionId = sessionId)

        val updated = base.copy(
            latestMajorAnalysisMessageId = messageId,
            latestMajorAnalysisAt = System.currentTimeMillis(),
            latestMajorAnalysisSource = source
        )

        runCatching { metaHub.upsertSession(updated) }
            .onSuccess {
                updateDebugSessionMetadata(updated)
                // Export gate is now checked on-demand via exportViewModel.checkExportGate()
            }
    }

    private fun refreshAudioRecoveryHint(meta: SessionMetadata?) {
        val startedAt = meta?.lastAudioTaskStartedAt
        val finishedAt = meta?.lastAudioTaskFinishedAt
        val dismissedAt = meta?.audioRecoveryHintDismissedForStartedAt
        // 重要：仅按标记字段判断，且转写进行中必须抑制提示
        val show = startedAt != null &&
            finishedAt == null &&
            dismissedAt != startedAt &&
            !activelyTranscribing
        _uiState.update {
            it.copy(
                showAudioRecoveryHint = show,
                audioRecoveryHintStartedAt = if (show) startedAt else null
            )
        }
    }

    private suspend fun refreshAudioRecoveryHintFromMetaHub() {
        val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        refreshAudioRecoveryHint(meta)
    }

    private suspend fun persistAudioTaskStartedMarker(startedAt: Long) {
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val base = existing ?: SessionMetadata(sessionId = sessionId)
        val updated = base.copy(
            lastAudioTaskStartedAt = startedAt,
            lastAudioTaskFinishedAt = null,
            audioRecoveryHintDismissedForStartedAt = null
        )
        runCatching { metaHub.upsertSession(updated) }
            .onSuccess { refreshAudioRecoveryHint(updated) }
    }

    private suspend fun persistAudioTaskFinishedMarker(finishedAt: Long) {
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val base = existing ?: SessionMetadata(sessionId = sessionId)
        val updated = base.copy(lastAudioTaskFinishedAt = finishedAt)
        runCatching { metaHub.upsertSession(updated) }
            .onSuccess { refreshAudioRecoveryHint(updated) }
    }

    fun dismissAudioRecoveryHint() {
        val startedAt = _uiState.value.audioRecoveryHintStartedAt ?: return
        viewModelScope.launch {
            val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            val base = existing ?: SessionMetadata(sessionId = sessionId)
            // 重要：关闭提示与 startedAt 绑定，避免影响下一次转写
            val updated = base.copy(audioRecoveryHintDismissedForStartedAt = startedAt)
            runCatching { metaHub.upsertSession(updated) }
                .onSuccess { refreshAudioRecoveryHint(updated) }
        }
    }

    private fun onAnalysisCompleted(summary: String, messageId: String) {
        latestAnalysisMarkdown = summary
        latestAnalysisMessageId = messageId

        if (!hasShownAnalysisExportHint) {
            hasShownAnalysisExportHint = true
            // 导出提示改为非对话层面的 UI（例如顶部 banner / icon 提示），
            // 不再插入额外的对话气泡，避免打断会话流和影响"单条智能分析卡片"结构。
        }

        pendingExportAfterAnalysis?.let { format ->
            pendingExportAfterAnalysis = null
            exportAutoAnalysisInFlight = false
            viewModelScope.launch {
                val markdown = wrapSmartAnalysisForExport(summary)
                _uiState.update { it.copy(exportInProgress = true) }
                val result = exportCoordinator.performExport(sessionId, format, _uiState.value.userName, markdown)
                when (result) {
                    is Result.Success -> _uiState.update { it.copy(exportInProgress = false) }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            exportInProgress = false,
                            chatErrorMessage = result.throwable.message ?: "导出失败"
                        )
                    }
                }
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

    private fun buildSmartAnalysisUserMessage(
        mainContent: String,
        context: String?,
        goal: String
    ): String =
        com.smartsales.domain.chat.ChatMessageBuilder.buildSmartAnalysisUserMessage(mainContent, context, goal)

    // Per Orchestrator-V1 Section 5.2: Publisher renders only <visible2user> content, no heuristic cleanup

    /**
     * 处理 GENERAL 首条回复的元数据：
     * - 优先尝试 <Metadata> 标签内的 JSON，失败则回退到末尾 JSON 块（兼容旧格式）
     * - 仅当存在有效字段时写入 MetaHub
     */
    // [HSVM@META] ===== Metadata 写入路径（含 L3-only gating） =====
    private suspend fun handleGeneralChatMetadata(
        rawFullText: String,
        metadataJson: String?
    ): SessionMetadata? {
        val candidates = buildList {
            metadataJson?.takeIf { it.isNotBlank() }?.let { add(it) }
            com.smartsales.domain.chat.MetadataParser.findLastJsonBlock(rawFullText)?.text?.let { tail ->
                if (tail != metadataJson) add(tail)
            }
        }
        val parsed = candidates.firstNotNullOfOrNull { com.smartsales.domain.chat.MetadataParser.parseGeneralChatMetadata(it, sessionId) }
        val metadata = parsed?.takeIf { it.hasMeaningfulGeneralFields() } ?: return null
        val patch = metadata.copy(
            latestMajorAnalysisSource = AnalysisSource.GENERAL_FIRST_REPLY,
            latestMajorAnalysisAt = System.currentTimeMillis()
        )
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val merged = existing?.mergeWith(patch) ?: patch
        runCatching { metaHub.upsertSession(merged) }
            .onSuccess { updateDebugSessionMetadata(merged) }
            .onFailure {
                warnLog(
                    event = "metahub_upsert_failed",
                    data = mapOf("sessionId" to sessionId, "target" to "session"),
                    throwable = it
                )
                appendDebugNote("sessionMetaUpsertFailed")
            }
        return merged
    }

    private suspend fun handleV1GeneralChatMetadata(
        result: V1FinalizeResult?
    ): SessionMetadata? {
        if (result == null) return null
        // 仅 L3 才允许写入元数据：与 V1 解析器输入一致，避免 L1/L2 误触发更新。
        // INVALID/FAILED 必须禁止写入，避免污染 MetadataHub 的确定性状态。
        if (result.artifactStatus != ArtifactStatus.VALID) return null
        val artifactJson = result.artifactJson?.takeIf { it.isNotBlank() } ?: return null
        val obj = runCatching { org.json.JSONObject(artifactJson) }.getOrNull() ?: return null
        if (obj.optString("mode") != "L3") return null
        // 禁止启发式/legacy <Metadata> 触发写入；V1 仅允许 MachineArtifact.metadataPatch。
        val patchObj = obj.optJSONObject("metadataPatch") ?: return null
        val metadata = com.smartsales.domain.chat.MetadataParser.parseGeneralChatMetadata(patchObj.toString(), sessionId)
            ?.takeIf { it.hasMeaningfulGeneralFields() }
            ?: return null
        val patch = metadata.copy(
            latestMajorAnalysisSource = AnalysisSource.GENERAL_FIRST_REPLY,
            latestMajorAnalysisAt = System.currentTimeMillis()
        )
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val merged = existing?.mergeWith(patch) ?: patch
        runCatching { metaHub.upsertSession(merged) }
            .onSuccess { updateDebugSessionMetadata(merged) }
            .onFailure {
                warnLog(
                    event = "metahub_upsert_failed",
                    data = mapOf("sessionId" to sessionId, "target" to "session"),
                    throwable = it
                )
                appendDebugNote("sessionMetaUpsertFailed")
            }
        return merged
    }

    private fun SessionMetadata.hasMeaningfulGeneralFields(): Boolean =
        !mainPerson.isNullOrBlank() ||
            !shortSummary.isNullOrBlank() ||
            !summaryTitle6Chars.isNullOrBlank() ||
            !location.isNullOrBlank()

    
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
        updateSessionListSelection()
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

    // [HSVM@STREAMING_CALLBACKS] ===== StreamingCallbacks Implementation =====
    // P3.9.2: Extract callbacks from inline lambdas for decoupling
    
    override fun onDelta(assistantId: String, token: String) {
        // Note: SmartAnalysis check happens at coordinator level
        updateAssistantMessage(assistantId) { msg ->
            val merged = StreamingDeduplicator().mergeSnapshot(
                current = msg.content,
                incoming = token
            )
            msg.copy(
                content = merged,
                isStreaming = true
            )
        }
    }
    
    override suspend fun onCompleted(assistantId: String, rawFullText: String) {
        // Delegate to existing completion handler
        // Note: Request context must be captured externally
        handleStreamCompleted(
            request = currentStreamingRequest ?: return,
            assistantId = assistantId,
            rawFullText = rawFullText,
            onCompleted = currentOnCompleted ?: {},
            onCompletedTransform = currentOnCompletedTransform,
            isAutoAnalysis = currentIsAutoAnalysis,
            isSmartAnalysis = currentStreamingRequest?.quickSkillId == "SMART_ANALYSIS"
        )
    }
    
    override fun onError(assistantId: String, throwable: Throwable) {
        // Delegate to existing error handler
        handleStreamError(
            request = currentStreamingRequest ?: // fallback to empty request
                ChatRequest(
                    sessionId = sessionId,
                    userMessage = "",
                    quickSkillId = null,
                    audioContextSummary = null,
                    history = emptyList(),
                    isFirstAssistantReply = false,
                    persona = null
                ),
            assistantId = assistantId,
            throwable = throwable,
            isAutoAnalysis = currentIsAutoAnalysis,
            isSmartAnalysis = false
        )
    }
    
    // Temporary state holders for streaming context
    // TODO(P3.9.3): Remove when startStreamingResponse moves to ConversationVM
    private var currentStreamingRequest: ChatRequest? = null
    private var currentOnCompleted: ((String) -> Unit)? = null
    private var currentOnCompletedTransform: ((String) -> String)? = null
    private var currentIsAutoAnalysis: Boolean = false
}
