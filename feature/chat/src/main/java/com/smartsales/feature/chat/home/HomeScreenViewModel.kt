package com.smartsales.feature.chat.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatHistoryItem
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.core.QuickSkillId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
// 模块：:feature:chat
// 说明：HomeScreen 的 UiState 模型与 ViewModel，实现聊天/快捷技能/设备信息更新逻辑
// 作者：创建于 2025-11-20

/** UI 模型：代表 Home 页里的一条聊天气泡。 */
data class ChatMessageUi(
    val id: String,
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
    val deviceName: String,
    val connectionState: DeviceConnectionStateUi,
    val wifiName: String?,
    val errorSummary: String? = null
)

/** 设备连接状态（用于 Home 横幅）。 */
enum class DeviceConnectionStateUi { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

/** UI 模型：音频/转写状态摘要。 */
data class AudioSummaryUi(
    val headline: String,
    val detail: String? = null
)

/** Home 发出的单次导航请求。 */
sealed class HomeNavigationRequest {
    data object DeviceSetup : HomeNavigationRequest()
    data object DeviceManager : HomeNavigationRequest()
    data object AudioFiles : HomeNavigationRequest()
    data object ChatHistory : HomeNavigationRequest()
    data object UserCenter : HomeNavigationRequest()
}

/** Home 页的不可变 UI 状态。 */
data class HomeUiState(
    val chatMessages: List<ChatMessageUi> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isStreaming: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val quickSkills: List<QuickSkillUi> = emptyList(),
    val deviceSnapshot: DeviceSnapshotUi? = null,
    val audioSummary: AudioSummaryUi? = null,
    val snackbarMessage: String? = null,
    val navigationRequest: HomeNavigationRequest? = null
)

/** 外部依赖（除 AiChatService 外保留原有 stub）。 */
interface AiSessionRepository {
    suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi>
}

interface DeviceConnectionManager {
    fun snapshot(): DeviceSnapshotUi?
}

interface MediaSyncCoordinator {
    fun audioSummary(): AudioSummaryUi?
}

/** HomeScreenViewModel：驱动聊天、快捷技能以及设备/音频快照。 */
class HomeScreenViewModel(
    private val aiChatService: AiChatService,
    private val aiSessionRepository: AiSessionRepository,
    private val deviceConnectionManager: DeviceConnectionManager,
    private val mediaSyncCoordinator: MediaSyncCoordinator,
    private val quickSkillCatalog: QuickSkillCatalog,
    private val sessionId: String = "home-session"
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        // 初始化快捷技能列表
        val skills = quickSkillCatalog.homeQuickSkills().map { it.toUiModel() }
        _uiState.update { it.copy(quickSkills = skills) }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSendMessage() {
        sendMessageInternal(
            messageText = _uiState.value.inputText,
            quickSkillId = null
        )
    }

    fun onSelectQuickSkill(skillId: QuickSkillId) {
        val definition = quickSkillCatalog.homeQuickSkills().firstOrNull { it.id == skillId }
        if (definition == null) {
            _uiState.update { it.copy(snackbarMessage = "无法识别的快捷技能") }
            return
        }
        // 快捷技能直接复用 send 流程
        sendMessageInternal(
            messageText = definition.defaultPrompt,
            quickSkillId = definition.id
        )
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
        val device = deviceConnectionManager.snapshot()
        val audio = mediaSyncCoordinator.audioSummary()
        _uiState.update { it.copy(deviceSnapshot = device, audioSummary = audio) }
    }

    fun onTapDeviceBanner() {
        val snapshot = _uiState.value.deviceSnapshot
        val destination = when (snapshot?.connectionState) {
            DeviceConnectionStateUi.CONNECTED -> HomeNavigationRequest.DeviceManager
            DeviceConnectionStateUi.CONNECTING,
            DeviceConnectionStateUi.DISCONNECTED,
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
            quickSkillId = null
        )
    }

    fun onDismissError() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onNavigationConsumed() {
        _uiState.update { it.copy(navigationRequest = null) }
    }

    fun setQuickSkills(skills: List<QuickSkillUi>) {
        _uiState.update { it.copy(quickSkills = skills) }
    }

    /** 统一的发送入口，输入或快捷技能均复用。 */
    private fun sendMessageInternal(
        messageText: String,
        quickSkillId: QuickSkillId?
    ) {
        val content = messageText.trim()
        if (content.isEmpty() || _uiState.value.isSending) return
        val userMessage = createUserMessage(content)
        val assistantPlaceholder = createAssistantPlaceholder()
        val newState = _uiState.value.copy(
            chatMessages = _uiState.value.chatMessages + userMessage + assistantPlaceholder,
            inputText = if (quickSkillId == null) "" else _uiState.value.inputText,
            isSending = true,
            isStreaming = true,
            snackbarMessage = null
        )
        _uiState.value = newState
        val request = buildChatRequest(content, quickSkillId, newState.chatMessages)
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
                        updateAssistantMessage(assistantId) { msg ->
                            msg.copy(content = event.fullText, isStreaming = false)
                        }
                        _uiState.update { it.copy(isSending = false, isStreaming = false) }
                    }
                    is ChatStreamEvent.Error -> {
                        // 错误：标记消息失败并弹出提示
                        updateAssistantMessage(assistantId) { msg ->
                            msg.copy(hasError = true, isStreaming = false)
                        }
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                isStreaming = false,
                                snackbarMessage = event.throwable.message ?: "AI 回复失败"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateAssistantMessage(
        assistantId: String,
        transformer: (ChatMessageUi) -> ChatMessageUi
    ) {
        _uiState.update { state ->
            val updated = state.chatMessages.map { msg ->
                if (msg.id == assistantId) transformer(msg) else msg
            }
            state.copy(chatMessages = updated)
        }
    }

    private fun buildChatRequest(
        userMessage: String,
        skillId: QuickSkillId?,
        historySource: List<ChatMessageUi>
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
            audioContextSummary = null, // TODO: 根据 Tingwu/录音摘要补充
            history = history
        )
    }

    private fun createUserMessage(content: String): ChatMessageUi = ChatMessageUi(
        id = generateId(),
        role = ChatMessageRole.USER,
        content = content,
        timestampMillis = System.currentTimeMillis()
    )

    private fun createAssistantPlaceholder(): ChatMessageUi = ChatMessageUi(
        id = generateId(),
        role = ChatMessageRole.ASSISTANT,
        content = "",
        timestampMillis = System.currentTimeMillis(),
        isStreaming = true
    )

    private fun QuickSkillDefinition.toUiModel(): QuickSkillUi = QuickSkillUi(
        id = id,
        label = label,
        description = description,
        isRecommended = isRecommended
    )

    private fun generateId(): String = "msg-${'$'}{System.currentTimeMillis()}"

    private fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
        value = transform(value)
    }
}
