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
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.ConnectivityError
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.media.MediaClipStatus
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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

/** HomeScreenViewModel：驱动聊天、快捷技能以及设备/音频快照。 */
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val aiChatService: AiChatService,
    private val aiSessionRepository: AiSessionRepository,
    private val deviceConnectionManager: DeviceConnectionManager,
    private val mediaSyncCoordinator: MediaSyncCoordinator,
    private val quickSkillCatalog: QuickSkillCatalog
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    private var latestMediaSyncState: MediaSyncState? = null
    private val sessionId: String = "home-session"

    init {
        // 从 catalog 加载快捷技能到状态
        val skills = quickSkillCatalog.homeQuickSkills().map { it.toUiModel() }
        _uiState.update { it.copy(quickSkills = skills) }
        observeDeviceConnection()
        observeMediaSync()
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSendMessage() {
        sendMessageInternal(
            messageText = _uiState.value.inputText,
            quickSkill = null
        )
    }

    fun onSelectQuickSkill(skillId: QuickSkillId) {
        val definition = quickSkillCatalog.homeQuickSkills().firstOrNull { it.id == skillId }
        if (definition == null) {
            _uiState.update { it.copy(snackbarMessage = "无法识别的快捷技能") }
            return
        }
        // 快捷技能点击后复用共享发送 helper
        sendMessageInternal(
            messageText = definition.defaultPrompt,
            quickSkill = definition
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
            quickSkill = null
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

    // 共享的发送 helper：输入框和快捷技能都走这里
    private fun sendMessageInternal(
        messageText: String,
        quickSkill: QuickSkillDefinition?
    ) {
        val content = messageText.trim()
        if (content.isEmpty() || _uiState.value.isSending) return
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
            inputText = if (quickSkillId == null) "" else _uiState.value.inputText,
            isSending = true,
            isStreaming = true,
            snackbarMessage = null
        )
        _uiState.value = newState
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
