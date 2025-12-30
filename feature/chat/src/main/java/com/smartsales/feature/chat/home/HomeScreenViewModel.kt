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
import com.smartsales.data.aicore.params.applyXfyunVoiceprintTestPreset
import com.smartsales.data.aicore.params.enableVoiceprintAndAddFeatureId
import com.smartsales.data.aicore.params.removeVoiceprintFeatureId
import com.smartsales.data.aicore.xfyun.XfyunVoiceprintApi
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
import com.smartsales.feature.chat.core.stream.ChatStreamCoordinator
import com.smartsales.feature.chat.core.transcription.V1BatchIndexPrefixGate
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

private enum class InputBucket { NOISE, SHORT_RELEVANT, RICH }
private data class AnalysisTarget(val content: String, val source: String)

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

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
// 模块：:feature:chat
// 说明：HomeScreen 的 UiState 模型与 ViewModel，实现聊天/快捷技能/设备信息更新逻辑
// 作者：创建于 2025-11-20

/** UI 模型：代表 Home 页里的一条聊天气泡。 */
data class ChatMessageUi(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatMessageRole,
    val content: String,
    val rawContent: String? = null,
    val sanitizedContent: String? = null,
    val timestampMillis: Long,
    val isStreaming: Boolean = false,
    val hasError: Boolean = false,
    val isSmartAnalysis: Boolean = false
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

/** 导出门禁状态：仅当智能分析就绪时允许导出。 */
data class ExportGateState(
    val ready: Boolean,
    val reason: String,
    val resolvedName: String,
    val nameSource: ExportNameSource
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
    val isTranscription: Boolean,
    val pinned: Boolean = false
)

data class DebugSessionMetadata(
    val sessionId: String,
    val title: String,
    val mainPerson: String? = null,
    val shortSummary: String? = null,
    val summaryTitle6Chars: String? = null,
    val stageLabel: String? = null,
    val riskLabel: String? = null,
    val tagsLabel: String? = null,
    val latestSourceLabel: String? = null,
    val latestAtLabel: String? = null,
    val transcriptionProviderRequested: String? = null,
    val transcriptionProviderSelected: String? = null,
    val transcriptionProviderDisabledReason: String? = null,
    val transcriptionXfyunEnabledSetting: Boolean? = null,
    val notes: List<String> = emptyList()
)

/**
 * 说明：声纹开发者工具（Voiceprint Lab）的 UI 状态（仅内存态）。
 *
 * 重要安全要求：
 * - 严禁在 ViewModel 内保存音频 base64（敏感数据）；base64 只由 UI 临时持有并在请求后清空。
 */
data class VoiceprintLabUiState(
    val registerInProgress: Boolean = false,
    val deleteInProgress: Boolean = false,
    val lastFeatureId: String? = null,
    val lastMessage: String? = null,
    val lastApiHost: String? = null,
    val lastApiPath: String? = null,
    val lastHttpCode: Int? = null,
    val lastBusinessCode: String? = null,
    val lastBusinessDesc: String? = null,
    // 仅用于提示当前设置概况；不包含 featureIds 明文。
    val enabledSetting: Boolean? = null,
    val configuredFeatureIdCount: Int? = null,
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
    val showAudioRecoveryHint: Boolean = false,
    val audioRecoveryHintStartedAt: Long? = null,
    val snackbarMessage: String? = null,
    val chatErrorMessage: String? = null,
    val navigationRequest: HomeNavigationRequest? = null,
    val sessionList: List<SessionListItemUi> = emptyList(),
    val currentSession: CurrentSessionUi = CurrentSessionUi(
        id = DEFAULT_SESSION_ID,
        title = DEFAULT_SESSION_TITLE,
        isTranscription = false
    ),
    val userName: String = "用户",
    val exportInProgress: Boolean = false,
    val exportGateState: ExportGateState = defaultExportGateState(),
    val showWelcomeHero: Boolean = true,
    val isSmartAnalysisMode: Boolean = false,
    val showDebugMetadata: Boolean = false,
    val debugSessionMetadata: DebugSessionMetadata? = null,
    val debugSnapshot: DebugSnapshot? = null,
    val xfyunTrace: XfyunTraceSnapshot? = null,
    val tingwuTrace: TingwuTraceSnapshot? = null,
    val voiceprintLab: VoiceprintLabUiState = VoiceprintLabUiState(),
    val smartReasoningText: String? = null,
    val isInputFocused: Boolean = false,
    val salesPersona: SalesPersona? = null,
    val showRawAssistantOutput: Boolean = false,
    val historyActionSession: SessionListItemUi? = null,
    val showHistoryRenameDialog: Boolean = false,
    val historyRenameText: String = ""
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
    private val metaHub: MetaHub,
    private val debugOrchestrator: DebugOrchestrator,
    private val xfyunTraceStore: XfyunTraceStore,
    private val tingwuTraceStore: TingwuTraceStore,
    private val aiParaSettingsRepository: AiParaSettingsRepository,
    private val xfyunVoiceprintApi: XfyunVoiceprintApi,
    optionalConfig: Optional<AiCoreConfig> = Optional.empty(),
) : ViewModel() {

    private val quickSkillDefinitions = quickSkillCatalog.homeQuickSkills()
    private val quickSkillDefinitionsById = quickSkillDefinitions.associateBy { it.id }
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    private var latestMediaSyncState: MediaSyncState? = null
    private var sessionId: String = DEFAULT_SESSION_ID
    private var latestSessionSummaries: List<AiSessionSummary> = emptyList()
    private var initialSessionPrepared: Boolean = false
    private var hasShownLowInfoHint: Boolean = false
    // 低信息智能分析提示是否已经出现过
    private var hasShownLowInfoSmartAnalysisHint: Boolean = false
    private var hasShownAnalysisExportHint: Boolean = false
    private var transcriptionJob: Job? = null
    private var transcriptionBatchJob: Job? = null
    private var lastTranscriptionJobId: String? = null
    private var transcriptionBatchMessageId: String? = null
    private var transcriptionBatchFinal: Boolean = false
    private val transcriptionBatchGate = V1BatchIndexPrefixGate<AudioTranscriptionBatchEvent.BatchReleased>()
    // 标记本次转写是否仍在进行中，用于抑制中途恢复提示
    private var activelyTranscribing: Boolean = false
    // 标记当前会话是否已处理首条助手回复，用于“首条决定标题”规则
    private var firstAssistantProcessed: Boolean = false
    private var latestAnalysisMarkdown: String? = null
    private var latestAnalysisMessageId: String? = null
    private var pendingExportAfterAnalysis: ExportFormat? = null
    private var exportAutoAnalysisInFlight: Boolean = false
    private val enableV1ChatPublisher = optionalConfig.orElse(AiCoreConfig()).enableV1ChatPublisher
    private val chatPublisher: ChatPublisher = ChatPublisherImpl()
    private val v1Finalizer = GeneralChatV1Finalizer(chatPublisher)
    private val chatStreamCoordinator = ChatStreamCoordinator { req -> homeOrchestrator.streamChat(req) }

    init {
        // 从 catalog 加载快捷技能到状态
        val skills = quickSkillDefinitions.map { it.toUiModel() }
        _uiState.update { it.copy(quickSkills = skills) }
        observeDeviceConnection()
        observeMediaSync()
        observeSessions()
        loadUserProfile()
        viewModelScope.launch { prepareInitialSession() }
        firstAssistantProcessed = false
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onInputFocusChanged(focused: Boolean) {
        _uiState.update { it.copy(isInputFocused = focused) }
    }

    fun onSendMessage() {
        val state = _uiState.value
        if (state.isSending) return
        val selectedSkill = state.selectedSkill?.id
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
        if (selectedSkill == null && isLowInfoGeneralChatInput(resolvedInput)) {
            handleLowInfoGeneralChatInput()
            return
        }
        sendMessageInternal(
            messageText = resolvedInput,
            skillOverride = selectedSkill
        )
    }

    private fun handleSmartAnalysisSend(rawInput: String) {
        if (_uiState.value.isSending) return
        val goal = rawInput.trim()
        val bucket = classifyUserInput(goal.ifBlank { _uiState.value.inputText }, _uiState.value.chatMessages)
        val target = findSmartAnalysisPrimaryContent(goal)
        if (target == null) {
            if (!hasShownLowInfoSmartAnalysisHint) {
                hasShownLowInfoSmartAnalysisHint = true
                // 低信息：用助手气泡提示一次，避免触发模型调用
                appendAssistantMessage("内容太少，无法智能分析，请先粘贴对话或纪要再试。")
            }
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
                    preface?.let {
                        append(it.trim()).append("\n\n")
                    }
                    append(body.trim())
                }.trim()
            }
        )
    }

    private fun startSmartAnalysisForExport(): Boolean {
        if (_uiState.value.isSending || _uiState.value.isStreaming || exportAutoAnalysisInFlight) {
            return false
        }
        val target = findSmartAnalysisPrimaryContent("")
        if (target == null) {
            _uiState.update { it.copy(snackbarMessage = "内容太少，无法智能分析，已取消导出") }
            return false
        }
        val contextContent = findContextForAnalysis(target.content)
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
            val summary = sessionRepository.findById(sessionId)
            val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            val gate = resolveExportGateState(sessionId, summary, meta)
            _uiState.update { it.copy(exportGateState = gate) }
            if (gate.ready) {
                pendingExportAfterAnalysis = null
                exportAutoAnalysisInFlight = false
                exportMarkdown(format)
                return@launch
            }

            // 未就绪：记录最新导出请求（最后一次点击优先），完成智能分析后自动导出。
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
        buildString {
            append("智能分析结果\n\n")
            append(body.trim())
        }.trim()

    private fun resolveExportGateState(
        sessionId: String,
        summary: AiSessionSummary?,
        meta: SessionMetadata?
    ): ExportGateState {
        // 重要：导出必须等待智能分析完成；未就绪时只做提示，不触发导出副作用。
        val ready = meta?.latestMajorAnalysisMessageId != null
        val reason = if (ready) "" else "需先完成智能分析"
        val resolution = ExportNameResolver.resolve(
            sessionId = sessionId,
            sessionTitle = summary?.title,
            isTitleUserEdited = summary?.isTitleUserEdited,
            meta = meta
        )
        return ExportGateState(
            ready = ready,
            reason = reason,
            resolvedName = resolution.baseName,
            nameSource = resolution.source
        )
    }

    private fun refreshExportGateState() {
        val currentSessionId = sessionId
        viewModelScope.launch {
            val summary = sessionRepository.findById(currentSessionId)
            val meta = runCatching { metaHub.getSession(currentSessionId) }.getOrNull()
            val gate = resolveExportGateState(currentSessionId, summary, meta)
            _uiState.update { it.copy(exportGateState = gate) }
        }
    }

    private fun maybeStartPendingExportAnalysis() {
        val pending = pendingExportAfterAnalysis ?: return
        if (exportAutoAnalysisInFlight || _uiState.value.isSending || _uiState.value.isStreaming) return
        // 说明：导出被排队时，等待对话空闲后再自动触发智能分析。
        viewModelScope.launch {
            val summary = sessionRepository.findById(sessionId)
            val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            val gate = resolveExportGateState(sessionId, summary, meta)
            _uiState.update { it.copy(exportGateState = gate) }
            if (gate.ready) {
                pendingExportAfterAnalysis = null
                exportMarkdown(pending)
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

    private fun exportMarkdown(format: ExportFormat) {
        if (_uiState.value.exportInProgress) return
        viewModelScope.launch {
            // 检查 MetaHub 分析状态
            val summary = sessionRepository.findById(sessionId)
            val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            val gate = resolveExportGateState(sessionId, summary, meta)
            _uiState.update { it.copy(exportGateState = gate) }
            if (!gate.ready) {
                _uiState.update { it.copy(snackbarMessage = gate.reason.ifBlank { "需先完成智能分析" }) }
                return@launch
            }
            val hasMetaAnalysis = meta?.latestMajorAnalysisMessageId != null
            val cachedAnalysis = findSmartAnalysisMarkdownForExport()

            when {
                !cachedAnalysis.isNullOrBlank() -> {
                    // 直接导出：使用缓存分析 markdown
                    val markdown = wrapSmartAnalysisForExport(cachedAnalysis)
                    performExport(format, markdownOverride = markdown)
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
                    _uiState.update {
                        it.copy(
                            exportInProgress = false,
                            snackbarMessage = "智能分析未完成，暂不可导出。"
                        )
                    }
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
        val sessionTitle = _uiState.value.exportGateState.resolvedName
        val result = when (format) {
            ExportFormat.PDF -> exportOrchestrator.exportPdf(sessionId, markdown, sessionTitle, _uiState.value.userName)
            ExportFormat.CSV -> exportOrchestrator.exportCsv(sessionId, sessionTitle, _uiState.value.userName)
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
            resetTranscriptionBatchState()
            refreshDebugSnapshot()
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
                transcriptionBatchJob?.cancel()
                transcriptionBatchJob = launch {
                    transcriptionCoordinator.observeBatches(request.jobId).collectLatest { event ->
                        when (event) {
                            is AudioTranscriptionBatchEvent.BatchReleased ->
                                handleTranscriptionBatchRelease(event)
                        }
                    }
                }
                transcriptionJob?.cancel()
                transcriptionJob = launch {
                    transcriptionCoordinator.observeJob(request.jobId).collectLatest { state ->
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

    private fun handleTranscriptionBatchRelease(
        event: AudioTranscriptionBatchEvent.BatchReleased
    ) {
        // 说明：v1Window/timedSegments 为可选透传字段，后续用于时间锚定/窗口过滤，当前不改变展示逻辑。
        if (transcriptionBatchFinal) {
            warnLog(
                event = "transcription_batch_after_final",
                data = mapOf("jobId" to event.jobId, "batchIndex" to event.batchIndex)
            )
            return
        }
        // 重要：记录伪流式批次计划与进度，供 HUD Section 3 观察。
        tingwuTraceStore.record(
            batchPlanRule = event.ruleLabel,
            batchPlanBatchSize = event.batchSize,
            batchPlanTotalBatches = event.totalBatches,
            batchPlanCurrentBatchIndex = event.batchIndex
        )
        // V1：按 batchIndex 发布连续前缀，乱序批次先缓存再释放
        val releasables = transcriptionBatchGate.offer(event.batchIndex, event)
        if (releasables.isEmpty()) {
            return
        }
        for (released in releasables) {
            val isFinal = released.isFinal
            val existingId = transcriptionBatchMessageId
            val merged = if (existingId == null) {
                released.markdownChunk
            } else {
                mergeTranscriptionChunks(
                    existing = _uiState.value.chatMessages.firstOrNull { it.id == existingId }?.rawContent
                        ?: _uiState.value.chatMessages.firstOrNull { it.id == existingId }?.content
                        ?: "",
                    incoming = released.markdownChunk
                )
            }
            if (existingId == null) {
                val messageId = nextMessageId()
                transcriptionBatchMessageId = messageId
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
                updateAssistantMessage(existingId, persistAfterUpdate = isFinal) { msg ->
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
                // 重要：完成后冻结说话人标签/文本，忽略后续批次。
                transcriptionBatchFinal = true
            }
        }
        if (CHAT_DEBUG_HUD_ENABLED && _uiState.value.showDebugMetadata) {
            refreshDebugSnapshot()
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

    private fun resetTranscriptionBatchState() {
        transcriptionBatchJob?.cancel()
        transcriptionBatchJob = null
        transcriptionBatchMessageId = null
        transcriptionBatchFinal = false
        transcriptionBatchGate.reset()
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
            refreshDebugSnapshot()
            refreshExportGateState()
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
        _uiState.update {
            it.copy(
                xfyunTrace = xfyunTraceStore.getSnapshot(),
                // 重要：HUD 打开时同时刷新 Tingwu 调试快照，便于确认默认链路。
                tingwuTrace = tingwuTraceStore.getSnapshot()
            )
        }
        refreshDebugSnapshot()
    }

    private fun refreshDebugSnapshot() {
        if (!CHAT_DEBUG_HUD_ENABLED || !_uiState.value.showDebugMetadata) return
        val currentSession = sessionId
        val currentJobId = lastTranscriptionJobId
        viewModelScope.launch {
            val summary = sessionRepository.findById(currentSession)
            val snapshot = runCatching {
                debugOrchestrator.getDebugSnapshot(
                    sessionId = currentSession,
                    jobId = currentJobId,
                    sessionTitle = summary?.title ?: _uiState.value.currentSession.title,
                    isTitleUserEdited = summary?.isTitleUserEdited
                )
            }.getOrElse { error ->
                // 重要：HUD 的调试快照失败时要 fail-soft，避免阻断调试面板展示。
                DebugSnapshot(
                    section1EffectiveRunText = "DebugSnapshot failed: ${error.message ?: "unknown"}",
                    section2RawTranscriptionText = "(missing: debug snapshot unavailable)",
                    section3PreprocessedText = "(missing: debug snapshot unavailable)",
                    sessionId = currentSession,
                    jobId = currentJobId,
                )
            }
            _uiState.update { it.copy(debugSnapshot = snapshot) }
        }
    }

    fun registerVoiceprintBase64(
        audioDataBase64: String,
        audioType: String,
        uid: String?,
    ) {
        if (!BuildConfig.DEBUG) return
        // 重要：base64 属于敏感音频数据；这里只做请求转发，绝不保存到 ViewModel/日志。
        val normalizedBase64 = audioDataBase64.filterNot { it.isWhitespace() }
        if (normalizedBase64.isBlank()) {
            _uiState.update {
                it.copy(
                    voiceprintLab = it.voiceprintLab.copy(
                        registerInProgress = false,
                        lastMessage = "No base64 loaded.",
                    )
                )
            }
            return
        }
        val type = audioType.trim()
        if (type.isBlank()) {
            _uiState.update {
                it.copy(
                    voiceprintLab = it.voiceprintLab.copy(
                        registerInProgress = false,
                        lastMessage = "audio_type is blank.",
                    )
                )
            }
            return
        }
        _uiState.update { it.copy(voiceprintLab = it.voiceprintLab.copy(registerInProgress = true, lastMessage = null)) }
        viewModelScope.launch {
            val result = runCatching {
                xfyunVoiceprintApi.registerBase64(
                    audioDataBase64 = normalizedBase64,
                    audioType = type,
                    uid = uid?.trim()?.takeIf { it.isNotBlank() },
                )
            }
            val snapshot = aiParaSettingsRepository.snapshot().transcription.xfyun.voiceprint
            _uiState.update { state ->
                val evidence = when (val error = result.exceptionOrNull()) {
                    is XfyunVoiceprintApi.VoiceprintCallException -> error.evidence
                    else -> result.getOrNull()?.evidence
                }
                state.copy(
                    voiceprintLab = state.voiceprintLab.copy(
                        registerInProgress = false,
                        lastFeatureId = result.getOrNull()?.featureId,
                        lastMessage = result.exceptionOrNull()?.message
                            ?: "Register OK. Copy feature_id then enable voiceprint.",
                        lastApiHost = evidence?.baseUrlHost,
                        lastApiPath = evidence?.path,
                        lastHttpCode = evidence?.httpCode,
                        lastBusinessCode = evidence?.businessCode,
                        lastBusinessDesc = evidence?.businessDesc,
                        enabledSetting = snapshot.enabled,
                        configuredFeatureIdCount = snapshot.featureIds.size,
                    )
                )
            }
        }
    }

    fun enableVoiceprintAndAddLastFeatureId() {
        if (!BuildConfig.DEBUG) return
        val featureId = _uiState.value.voiceprintLab.lastFeatureId?.trim().orEmpty()
        if (featureId.isBlank()) {
            _uiState.update { it.copy(voiceprintLab = it.voiceprintLab.copy(lastMessage = "No feature_id to apply.")) }
            return
        }
        // 重要：只写入 featureId（敏感标识）；绝不写入/保存音频 base64。
        aiParaSettingsRepository.enableVoiceprintAndAddFeatureId(featureId)
        val snapshot = aiParaSettingsRepository.snapshot().transcription.xfyun.voiceprint
        _uiState.update { state ->
            state.copy(
                voiceprintLab = state.voiceprintLab.copy(
                    enabledSetting = snapshot.enabled,
                    configuredFeatureIdCount = snapshot.featureIds.size,
                    lastMessage = "Applied: enabled=true, featureIdsCount=${snapshot.featureIds.size}",
                )
            )
        }
    }

    fun applyXfyunVoiceprintTestPreset() {
        if (!BuildConfig.DEBUG) return
        val lastFeatureId = _uiState.value.voiceprintLab.lastFeatureId
        aiParaSettingsRepository.applyXfyunVoiceprintTestPreset(lastFeatureId = lastFeatureId)
        val snapshot = aiParaSettingsRepository.snapshot().transcription.xfyun.voiceprint
        _uiState.update { state ->
            state.copy(
                voiceprintLab = state.voiceprintLab.copy(
                    enabledSetting = snapshot.enabled,
                    configuredFeatureIdCount = snapshot.featureIds.size,
                    lastMessage = "XFyun VP Test Preset applied.",
                )
            )
        }
    }

    fun deleteVoiceprintFeatureId(featureId: String, removeFromSettings: Boolean) {
        if (!BuildConfig.DEBUG) return
        val id = featureId.trim()
        if (id.isBlank()) {
            _uiState.update { it.copy(voiceprintLab = it.voiceprintLab.copy(lastMessage = "feature_id is blank.")) }
            return
        }
        _uiState.update { it.copy(voiceprintLab = it.voiceprintLab.copy(deleteInProgress = true, lastMessage = null)) }
        viewModelScope.launch {
            val result = runCatching { xfyunVoiceprintApi.deleteFeatureId(id) }
            if (result.isSuccess && removeFromSettings) {
                aiParaSettingsRepository.removeVoiceprintFeatureId(id)
            }
            val snapshot = aiParaSettingsRepository.snapshot().transcription.xfyun.voiceprint
            _uiState.update { state ->
                val evidence = when (val error = result.exceptionOrNull()) {
                    is XfyunVoiceprintApi.VoiceprintCallException -> error.evidence
                    else -> result.getOrNull()
                }
                state.copy(
                    voiceprintLab = state.voiceprintLab.copy(
                        deleteInProgress = false,
                        enabledSetting = snapshot.enabled,
                        configuredFeatureIdCount = snapshot.featureIds.size,
                        lastMessage = result.exceptionOrNull()?.message
                            ?: "Delete OK. removeFromSettings=$removeFromSettings",
                        lastApiHost = evidence?.baseUrlHost,
                        lastApiPath = evidence?.path,
                        lastHttpCode = evidence?.httpCode,
                        lastBusinessCode = evidence?.businessCode,
                        lastBusinessDesc = evidence?.businessDesc,
                    )
                )
            }
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
                resetTranscriptionBatchState()
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
            refreshDebugSnapshot()
        }
    }

    fun setQuickSkills(skills: List<QuickSkillUi>) {
        _uiState.update { it.copy(quickSkills = skills) }
    }

    fun onHistorySessionLongPress(sessionId: String) {
        val target = _uiState.value.sessionList.firstOrNull { it.id == sessionId } ?: return
        _uiState.update {
            it.copy(
                historyActionSession = target,
                showHistoryRenameDialog = false,
                historyRenameText = target.title
            )
        }
    }

    fun onHistoryActionDismiss() {
        _uiState.update {
            it.copy(
                historyActionSession = null,
                showHistoryRenameDialog = false,
                historyRenameText = ""
            )
        }
    }

    fun onHistoryActionRenameStart() {
        val target = _uiState.value.historyActionSession ?: return
        _uiState.update {
            it.copy(
                showHistoryRenameDialog = true,
                historyRenameText = target.title
            )
        }
    }

    fun onHistoryRenameTextChange(text: String) {
        _uiState.update { it.copy(historyRenameText = text) }
    }

    fun onHistorySessionPinToggle(sessionId: String) {
        viewModelScope.launch {
            val existing = sessionRepository.findById(sessionId) ?: return@launch
            val toggled = existing.copy(pinned = !existing.pinned)
            sessionRepository.upsert(toggled)
            latestSessionSummaries = latestSessionSummaries.map { summary ->
                if (summary.id == sessionId) toggled else summary
            }
            applySessionList()
            _uiState.update { state ->
                val current = state.currentSession
                val updatedCurrent = if (current.id == sessionId) {
                    current.copy()
                } else current
                state.copy(currentSession = updatedCurrent)
            }
            onHistoryActionDismiss()
        }
    }

    fun onHistorySessionRenameConfirmed(sessionId: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            sessionRepository.updateTitle(sessionId, trimmed, isUserEdited = true)
            // 用户确认改名：写入 M3 accepted，后续候选不得覆盖
            metaHub.setM3AcceptedName(
                sessionId = sessionId,
                target = RenamingTarget.SESSION_TITLE,
                name = trimmed,
                prov = Provenance(
                    source = "user_rename",
                    updatedAt = System.currentTimeMillis()
                )
            )
            latestSessionSummaries = latestSessionSummaries.map { summary ->
                if (summary.id == sessionId) {
                    summary.copy(title = trimmed, isTitleUserEdited = true)
                } else summary
            }
            applySessionList()
            _uiState.update { state ->
                val current = state.currentSession
                val updatedCurrent = if (current.id == sessionId) {
                    current.copy(title = trimmed)
                } else current
                state.copy(
                    currentSession = updatedCurrent,
                    historyActionSession = state.historyActionSession?.takeIf { it.id == sessionId }?.copy(title = trimmed),
                    showHistoryRenameDialog = false,
                    historyRenameText = ""
                )
            }
        }
    }

    fun onHistorySessionDelete(sessionId: String) {
        viewModelScope.launch {
            runCatching { sessionRepository.delete(sessionId) }
            runCatching { chatHistoryRepository.deleteSession(sessionId) }
            latestSessionSummaries = latestSessionSummaries.filterNot { it.id == sessionId }
            applySessionList()
            val deletedCurrent = sessionId == this@HomeScreenViewModel.sessionId
            onHistoryActionDismiss()
            if (deletedCurrent) {
                val next = latestSessionSummaries.firstOrNull()
                    ?: sessionRepository.createNewChatSession().also { created ->
                        latestSessionSummaries = latestSessionSummaries + created
                    }
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

    fun onNewChatClicked() {
        viewModelScope.launch {
            val newSession = sessionRepository.createNewChatSession()
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
            refreshExportGateState()
            chatHistoryRepository.saveMessages(newSession.id, emptyList())
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
                refreshExportGateState()
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
                isTranscription = summary.isTranscription ||
                    summary.title.startsWith(SessionTitlePolicy.LEGACY_TRANSCRIPTION_PREFIX),
                pinned = summary.pinned
            )
        }
        _uiState.update { state ->
            val updatedActionSession = state.historyActionSession?.let { current ->
                mapped.firstOrNull { it.id == current.id }
            }
            state.copy(
                sessionList = mapped,
                historyActionSession = updatedActionSession
            )
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
            request = request,
            assistantId = assistantPlaceholder.id,
            onCompleted = onCompleted,
            onCompletedTransform = onCompletedTransform,
            isAutoAnalysis = isAutoAnalysis
        )
    }

    /** 启动 streaming，更新最后一条助手气泡。 */
    private fun startStreamingResponse(
        request: ChatRequest,
        assistantId: String,
        onCompleted: (String) -> Unit = {},
        onCompletedTransform: ((String) -> String)? = null,
        isAutoAnalysis: Boolean = false
    ) {
        val isSmartAnalysis = request.quickSkillId == "SMART_ANALYSIS"
        var streamingDeduplicator = StreamingDeduplicator()

        if (isSmartAnalysis) {
            // 本地占位提示，避免流式展示脏文本
            updateAssistantMessage(assistantId) { msg ->
                msg.copy(content = SMART_PLACEHOLDER_TEXT, isStreaming = true)
            }
        }

        val v1RetryEnabled = enableV1ChatPublisher && request.quickSkillId == null && !isSmartAnalysis
        val v1MaxRetries = if (v1RetryEnabled) 2 else 0
        val v1RetryActive = v1RetryEnabled && v1MaxRetries > 0
        var v1TerminalFailureReason: String? = null
        var v1TerminalArtifactStatus: ArtifactStatus? = null
        // V1 GENERAL：修复指令只进请求，不进 UI；未启用时保持原样
        val requestProvider = if (v1RetryActive) {
            val repairInstruction = V1GeneralRetryPolicy.buildRepairInstruction()
            val provider: (Int) -> ChatRequest = { attempt: Int ->
                if (attempt == 0) request else request.copy(
                    userMessage = request.userMessage + "\n\n" + repairInstruction
                )
            }
            provider
        } else {
            null
        }
        // V1：先验收再发布，避免中间失败结果污染 UI（行为保持不变）
        val completionEvaluator: (suspend (String, Int) -> com.smartsales.feature.chat.core.stream.CompletionDecision)? = if (v1RetryActive) {
            val evaluator = V1GeneralCompletionEvaluator(v1Finalizer)
            val provider: suspend (String, Int) -> com.smartsales.feature.chat.core.stream.CompletionDecision = { rawFullText, attempt ->
                // V1 合约检查：MachineArtifact 必须有效，否则触发重试
                // missing_json_fence 属于不可修复类错误，开启 reason-aware 避免无意义重试，提升稳定性
                val eval = evaluator.evaluate(
                    rawFullText = rawFullText,
                    attempt = attempt,
                    maxRetries = v1MaxRetries,
                    enableReasonAwareRetry = true
                )
                if (eval.decision == com.smartsales.feature.chat.core.stream.CompletionDecision.Terminal) {
                    v1TerminalFailureReason = eval.finalizeResult.failureReason
                    v1TerminalArtifactStatus = eval.finalizeResult.artifactStatus
                }
                eval.decision
            }
            provider
        } else {
            null
        }
        // V1：行为保持不变，仅封装副作用以缩短 ViewModel 逻辑
        val v1RetryEffects = if (v1RetryActive) {
            V1GeneralRetryEffects(
                resetDeduper = {
                    // 重试前清空占位内容并重置去重器，避免 UI 闪烁/残留
                    streamingDeduplicator = StreamingDeduplicator()
                },
                resetPlaceholder = {
                    updateAssistantMessage(assistantId) { msg ->
                        msg.copy(
                            content = "",
                            rawContent = "",
                            sanitizedContent = "",
                            isStreaming = true
                        )
                    }
                },
                publishTerminal = { fullText ->
                    handleStreamCompleted(
                        request = request,
                        assistantId = assistantId,
                        rawFullText = fullText,
                        onCompleted = onCompleted,
                        onCompletedTransform = onCompletedTransform,
                        isAutoAnalysis = isAutoAnalysis,
                        isSmartAnalysis = isSmartAnalysis
                    )
                }
            )
        } else {
            null
        }
        val onRetryStart: suspend (Int) -> Unit = if (v1RetryActive) {
            { attempt ->
                // 只记录 reason code/次数；严禁记录任何用户/模型原文
                Log.d(
                    TAG,
                    "event=v1_general_retry_start, " +
                        "nextAttempt=$attempt, " +
                        "maxRetries=$v1MaxRetries, " +
                        "lastFailureReason=$v1TerminalFailureReason"
                )
                requireNotNull(v1RetryEffects).onRetryStart(attempt)
            }
        } else {
            { _: Int -> }
        }
        val onTerminal: suspend (String, Int) -> Unit = if (v1RetryActive) {
            { fullText, attempt ->
                // 终止发布点仅记录原因码与重试信息，严禁输出任何用户/模型原文，避免隐私泄露
                Log.d(
                    TAG,
                    "event=v1_general_terminal_publish, " +
                        "failureReason=$v1TerminalFailureReason, " +
                        "attempt=$attempt, " +
                        "maxRetries=$v1MaxRetries, " +
                        "reasonAware=true, " +
                        "artifactStatus=$v1TerminalArtifactStatus"
                )
                requireNotNull(v1RetryEffects).onTerminal(fullText, attempt)
            }
        } else {
            { _: String, _: Int -> }
        }

        // 抽出流式协调器，降低 ViewModel 复杂度，便于后续 V1 规则演进
        chatStreamCoordinator.start(
            scope = viewModelScope,
            request = request,
            onDelta = { token ->
                if (!isSmartAnalysis) {
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
            },
            onCompleted = { fullText ->
                if (v1RetryActive && enableV1ChatPublisher && request.quickSkillId == null && !isSmartAnalysis) {
                    // 仅记录发布事件与次数信息，严禁输出任何用户/模型原文
                    Log.d(
                        TAG,
                        "event=v1_general_accept_publish, " +
                            "attempt=unknown, " +
                            "maxRetries=$v1MaxRetries, " +
                            "failureReason=null"
                    )
                }
                handleStreamCompleted(
                    request = request,
                    assistantId = assistantId,
                    rawFullText = fullText,
                    onCompleted = onCompleted,
                    onCompletedTransform = onCompletedTransform,
                    isAutoAnalysis = isAutoAnalysis,
                    isSmartAnalysis = isSmartAnalysis
                )
            },
            onError = { throwable ->
                handleStreamError(
                    request = request,
                    assistantId = assistantId,
                    throwable = throwable,
                    isAutoAnalysis = isAutoAnalysis,
                    isSmartAnalysis = isSmartAnalysis
                )
            },
            maxRetries = v1MaxRetries,
            completionEvaluator = completionEvaluator,
            requestProvider = requestProvider,
            onRetryStart = onRetryStart,
            onTerminal = onTerminal
        )
    }

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

        val latestMeta = if (useV1Publisher) {
            runCatching { metaHub.getSession(sessionId) }.getOrNull()
        } else {
            generalMeta ?: runCatching { metaHub.getSession(sessionId) }.getOrNull()
        }

        // <Visible2User> 驱动显示，rawContent 保留原始文本（含标签）
        val visibleText = channels.visibleText
        val displaySource = visibleText ?: rawFullText
        val cleaned = if (useV1Publisher) {
            v1Result?.visibleMarkdown.orEmpty()
        } else {
            val sanitized = if (isSmartAnalysis) rawFullText else sanitizeAssistantOutput(displaySource, isSmartAnalysis)
            // SMART_ANALYSIS 已由 Orchestrator 生成最终 Markdown，这里直接透传
            if (isSmartAnalysis) {
                sanitized
            } else {
                val base = onCompletedTransform?.invoke(sanitized) ?: sanitized
                applyGeneralOutputGuards(base)
            }
        }
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
            applySessionList()
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

    private fun buildTranscriptMarkdown(messages: List<ChatMessageUi>): String {
        if (messages.isEmpty()) return ""
        val builder = StringBuilder()
        builder.append("# 对话记录\n\n")
        messages.forEach { msg ->
            val role = if (msg.role == ChatMessageRole.USER) "用户" else "助手"
            val text = msg.sanitizedContent ?: msg.content
            builder.append("- **$role**：$text\n")
        }
        return builder.toString()
    }

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

    /** 提取 <Visible2User> 内部文本，若不存在则返回 null。 */
    private fun extractVisible2User(raw: String): String? {
        val regex = Regex("<\\s*Visible2User\\s*>([\\s\\S]*?)<\\s*/\\s*Visible2User\\s*>", RegexOption.IGNORE_CASE)
        val match = regex.find(raw) ?: return null
        return match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /** 提取 <Metadata> 包裹的 JSON 文本，若不存在则返回 null。 */
    private fun extractMetadataJson(raw: String): String? {
        val regex = Regex("<\\s*Metadata\\s*>([\\s\\S]*?)<\\s*/\\s*Metadata\\s*>", RegexOption.IGNORE_CASE)
        val match = regex.find(raw) ?: return null
        return match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /** GENERAL 回复的 channels 数据（Visible2User 和 Metadata） */
    private data class GeneralChannels(
        val visibleText: String?,
        val metadataJson: String?,
        val renameCandidate: TitleCandidate?
    )

    /** 提取 GENERAL 回复中的 channels */
    private fun extractGeneralChannels(raw: String): GeneralChannels {
        val visibleText = extractVisible2User(raw)
        val metadataJson = extractMetadataJson(raw)
        val rename = parseRenameCandidate(raw, TitleSource.GENERAL)
        return GeneralChannels(
            visibleText = visibleText,
            metadataJson = metadataJson,
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

    private fun isLowInfoGeneralChatInput(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.getDefault())
        if (normalized.isBlank()) return true
        val acknowledgements = setOf("是", "好的", "好", "嗯", "嗯嗯", "ok", "okay", "收到")
        return acknowledgements.contains(normalized)
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
        transcriptionJob?.cancel()
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
                refreshExportGateState()
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
            viewModelScope.launch {
                val markdown = wrapSmartAnalysisForExport(summary)
                performExport(format, markdownOverride = markdown)
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
        // 去掉 channel 标签但保留其内容（用于 fallback 模式）
        val withoutTags = raw
            .replace("<Visible2User>", "")
            .replace("</Visible2User>", "")
            .replace("<Metadata>", "")
            .replace("</Metadata>", "")
        
        // 预处理：先剥离 JSON block（元数据仅供 MetaHub 使用，不展示给用户）
        val withoutJson = stripJsonBlocks(withoutTags)

        // 第一步：修复编号混乱（如 "11)1)"、"2)1)" 等）
        val fixedNumbering = fixNumberingIssues(withoutJson)
        
        // 第二步：检测并清理内容累积模式（更激进的清理）
        // 使用字符级别的检测，确保彻底清理
        val cleanedAccumulation = cleanAccumulatedContentWithCharLevelDetection(fixedNumbering)

        // 第三步：剥离可能泄露的规则标题/提示语，仅针对 GENERAL
        val echoCleaned = if (isSmartAnalysis) cleanedAccumulation else stripPromptEchoSections(cleanedAccumulation)
        // 同时去掉未消费的标签，避免 GENERAL fallback 时展示标签
        val tagCleaned = if (isSmartAnalysis) echoCleaned else stripTagContainers(echoCleaned)
        
        // 第四步：标准去重处理
        val lines = tagCleaned.lines()
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
        val normalized = dedupeNumberedLists(joined)
        return if (isSmartAnalysis) normalized else collapseDuplicateClauses(normalized)
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
     * 去除模型可能回显的规则/提示语块，避免在 GENERAL 气泡里出现「历史对话」「最新问题」等标题。
     */
    private fun stripPromptEchoSections(text: String): String {
        val markers = listOf(
            "# 你的销售画像",
            "## 输入摘要",
            "## 行为（GENERAL",
            "## 行为（SMART",
            "## 安全与口吻",
            "历史对话：",
            "最新问题："
        )
        val personaBullets = listOf("岗位：", "行业：", "主要沟通渠道：", "经验水平：", "表达风格：")
        val echoPrefixes = listOf(
            "- 用户",
            "用户："
        )
        val lines = text.lines()
        val output = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            val hit = markers.any { marker -> trimmed.startsWith(marker) } ||
                personaBullets.any { bullet -> trimmed.startsWith("- $bullet") } ||
                echoPrefixes.any { prefix -> trimmed.startsWith(prefix) }
            if (hit) continue
            output += line
        }
        return output.dropWhile { it.isBlank() }.joinToString("\n").trim()
    }

    /**
     * 去掉未消费的标签容器（Visible2User/Metadata/Reasoning/DocReference），仅保留其内部文本。
     * 用于 GENERAL fallback 场景，避免标签本身出现在展示文本。
     */
    private fun stripTagContainers(text: String): String {
        var result = text
        val tags = listOf("Visible2User", "Metadata", "Reasoning", "DocReference")
        tags.forEach { tag ->
            val regex = Regex("<\\s*$tag\\s*>([\\s\\S]*?)<\\s*/\\s*$tag\\s*>", RegexOption.IGNORE_CASE)
            result = regex.replace(result) { matchResult ->
                matchResult.groupValues.getOrNull(1) ?: ""
            }
        }
        return result
    }

    /**
     * 轻量句子去重：针对 GENERAL，将完全相同的句子/短语仅保留一次，避免短答复自我重复。
     */
    private fun collapseDuplicateClauses(text: String): String {
        if (text.isBlank()) return text
        val segments = text.split(Regex("(?<=[。！？!?；;\\n])"))
        val seen = mutableSetOf<String>()
        val kept = mutableListOf<String>()
        segments.forEach { seg ->
            val trimmed = seg.trim()
            if (trimmed.isEmpty()) return@forEach
            val key = normalizeSentence(trimmed)
            if (key.isEmpty()) return@forEach
            if (seen.add(key)) {
                kept += trimmed
            }
        }
        return kept.joinToString("\n").trim()
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

    /**
     * 处理 GENERAL 首条回复的元数据：
     * - 优先尝试 <Metadata> 标签内的 JSON，失败则回退到末尾 JSON 块（兼容旧格式）
     * - 仅当存在有效字段时写入 MetaHub
     */
    private suspend fun handleGeneralChatMetadata(
        rawFullText: String,
        metadataJson: String?
    ): SessionMetadata? {
        val candidates = buildList {
            metadataJson?.takeIf { it.isNotBlank() }?.let { add(it) }
            findLastJsonBlock(rawFullText)?.text?.let { tail ->
                if (tail != metadataJson) add(tail)
            }
        }
        val parsed = candidates.firstNotNullOfOrNull { parseGeneralChatMetadata(it, sessionId) }
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

    private data class JsonBlock(val text: String, val startIndex: Int)

    // TODO: Replace with Orchestrator-MetadataHub V2 spec metadata types when available
    private fun parseGeneralChatMetadata(jsonText: String, sessionId: String): SessionMetadata? = runCatching {
        val obj = org.json.JSONObject(jsonText)
        val summary6 = obj.optString("summary_title_6chars").takeIf { it.isNotBlank() }?.take(6)
        val summary8 = obj.optString("summary_title_8chars").takeIf { it.isNotBlank() }?.take(8)
        SessionMetadata(
            sessionId = sessionId,
            mainPerson = obj.optString("main_person").takeIf { it.isNotBlank() },
            shortSummary = obj.optString("short_summary").takeIf { it.isNotBlank() },
            summaryTitle6Chars = summary6 ?: summary8?.take(6),
            location = obj.optString("location").takeIf { it.isNotBlank() }
        )
    }.getOrNull()

    private fun findLastJsonBlock(text: String): JsonBlock? {
        // 优先提取 fenced JSON（```json ... ```），否则尝试抓取末尾裸 JSON 对象
        val fencedRegex = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val fenced = fencedRegex.findAll(text).lastOrNull()
        val fencedContent = fenced?.groupValues?.getOrNull(1)?.trim()
        if (!fencedContent.isNullOrBlank() && fencedContent.startsWith("{") && fencedContent.endsWith("}")) {
            val braceStart = text.indexOf('{', fenced.range.first)
            return JsonBlock(fencedContent, if (braceStart >= 0) braceStart else fenced.range.first)
        }
        val anyFenceRegex = Regex("```\\s*([\\s\\S]*?)```")
        val anyFence = anyFenceRegex.findAll(text).lastOrNull()
        val anyContent = anyFence?.groupValues?.getOrNull(1)?.trim()
        if (!anyContent.isNullOrBlank() && anyContent.startsWith("{") && anyContent.endsWith("}")) {
            val braceStart = text.indexOf('{', anyFence.range.first)
            return JsonBlock(anyContent, if (braceStart >= 0) braceStart else anyFence.range.first)
        }
        val trimmed = text.trimEnd()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return JsonBlock(trimmed, text.lastIndexOf('{'))

        // 从末尾向前寻找最后一对完整的大括号，确保 JSON 位于文本末尾
        fun findMatchingEnd(content: String, start: Int): Int {
            var depth = 0
            for (i in start until content.length) {
                when (content[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }
            return -1
        }

        var start = text.lastIndexOf('{')
        while (start >= 0) {
            val end = findMatchingEnd(text, start)
            if (end > start && text.substring(end + 1).trim().isEmpty()) {
                val block = text.substring(start, end + 1)
                return JsonBlock(block, start)
            }
            start = text.lastIndexOf('{', start - 1)
        }
        return null
    }

    private fun SessionMetadata.hasMeaningfulGeneralFields(): Boolean =
        !mainPerson.isNullOrBlank() ||
            !shortSummary.isNullOrBlank() ||
            !summaryTitle6Chars.isNullOrBlank() ||
            !location.isNullOrBlank()

    private fun stripTrailingJsonFromGeneralReply(fullText: String): String {
        val jsonBlock = findLastJsonBlock(fullText) ?: return fullText
        val isValidJson = runCatching { org.json.JSONObject(jsonBlock.text) }.isSuccess
        if (!isValidJson) return fullText
        return fullText.substring(0, jsonBlock.startIndex).trimEnd()
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
        applySessionList()
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
        val newSession = sessionRepository.createNewChatSession()
        sessionId = newSession.id
        firstAssistantProcessed = false
        latestSessionSummaries =
            (latestSessionSummaries.filterNot { it.id == newSession.id } + newSession)
        applySessionList()
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
        refreshExportGateState()
    }

    private suspend fun enforcePlaceholderForHeroIfNeeded() {
        val state = _uiState.value
        if (!state.showWelcomeHero || state.chatMessages.isNotEmpty()) return
        val existing = sessionRepository.findById(sessionId)
        if (existing?.isTitleUserEdited == true) return
        if (SessionTitlePolicy.isPlaceholder(state.currentSession.title)) return
        val placeholder = SessionTitlePolicy.newChatPlaceholder()
        sessionRepository.updateTitle(sessionId, placeholder)
        latestSessionSummaries = latestSessionSummaries.map { summary ->
            if (summary.id == sessionId) summary.copy(title = placeholder) else summary
        }
        applySessionList()
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
}
