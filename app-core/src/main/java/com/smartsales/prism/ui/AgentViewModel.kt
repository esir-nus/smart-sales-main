package com.smartsales.prism.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.smartsales.prism.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.core.pipeline.AgentActivityController
import com.smartsales.core.pipeline.MascotService
import com.smartsales.core.pipeline.MascotInteraction
import com.smartsales.core.pipeline.MascotState
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.ToolResult
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PluginGateway
import com.smartsales.core.context.ChatTurn
import com.smartsales.core.context.ContextBuilder
import com.smartsales.prism.domain.system.SystemEventBus
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import javax.inject.Inject
import java.time.LocalDate

/**
 * AgentViewModel (Layer 4/5 Presentation)
 * 
 * Replaces the monolithic PrismViewModel. 
 * Exclusively responsible for bridging UI state with the Layer 3 IntentOrchestrator.
 * It contains NO routing logic.
 */
@HiltViewModel
class AgentViewModel @Inject constructor(
    private val intentOrchestrator: IntentOrchestrator, // Layer 3 Gateway!
    private val historyRepository: HistoryRepository,
    private val userProfileRepository: UserProfileRepository,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val activityController: AgentActivityController,
    private val mascotService: MascotService,
    private val eventBus: SystemEventBus,
    private val audioRepository: AudioRepository,
    private val contextBuilder: ContextBuilder,
    private val toolRegistry: ToolRegistry
) : ViewModel() {

    // ------------------------------------------------------------------------
    // UI States
    // ------------------------------------------------------------------------
    val agentActivity = activityController.activity

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history = _history.asStateFlow()

    // Task Board integration (mocked data collection)
    private val _taskBoardItems = MutableStateFlow<List<com.smartsales.prism.domain.analyst.TaskBoardItem>>(emptyList())
    val taskBoardItems = _taskBoardItems.asStateFlow()

    private val _sessionTitle = MutableStateFlow("新对话")
    val sessionTitle = _sessionTitle.asStateFlow()

    private val _heroUpcoming = MutableStateFlow<List<TimelineItemModel.Task>>(emptyList())
    val heroUpcoming = _heroUpcoming.asStateFlow()

    private val _heroAccomplished = MutableStateFlow<List<TimelineItemModel.Task>>(emptyList())
    val heroAccomplished = _heroAccomplished.asStateFlow()

    val mascotState = mascotService.state

    private var currentSessionId: String? = null

    val currentDisplayName: String
        get() = userProfileRepository.profile.value.displayName

    val heroGreeting = userProfileRepository.profile
        .map { profile ->
            val hour = java.time.LocalTime.now().hour
            val timeGreeting = when {
                hour < 5 -> "🌙 夜深了"
                hour < 12 -> "☀️ 早上好"
                hour < 18 -> "✨ 下午好"
                else -> "🌙 晚上好"
            }
            "$timeGreeting, ${profile.displayName}"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        startNewSession()
        
        viewModelScope.launch {
            loadHeroDashboard()
        }

        viewModelScope.launch {
            combine(uiState, inputText) { state, _ -> state }
                .collectLatest { state ->
                    if (state is UiState.Idle) {
                        kotlinx.coroutines.delay(15000)
                        eventBus.publish(com.smartsales.prism.domain.system.SystemEvent.AppIdle)
                    }
                }
        }
    }

    private suspend fun loadHeroDashboard() {
        val today = LocalDate.now()
        val allUpcoming = scheduledTaskRepository
            .queryByDateRange(today, today.plusDays(3))
            .first()
        _heroUpcoming.value = allUpcoming
            .filterIsInstance<TimelineItemModel.Task>()
            .filter { !it.isDone }
            .filter { it.urgencyLevel != UrgencyLevel.FIRE_OFF }
            .take(3)

        _heroAccomplished.value = scheduledTaskRepository.getRecentCompleted(2)
    }

    fun refreshHeroDashboard() {
        viewModelScope.launch { loadHeroDashboard() }
    }

    fun startNewSession() {
        persistCurrentMessages()
        
        _history.value = emptyList()
        _sessionTitle.value = "新对话"
        _inputText.value = ""
        _uiState.value = UiState.Idle
        activityController.reset()
        contextBuilder.resetSession()
        
        viewModelScope.launch(Dispatchers.IO) {
            currentSessionId = historyRepository.createSession("新对话", "新会话")
        }
    }

    fun switchSession(sessionId: String, triggerAutoRename: Boolean = false) {
        persistCurrentMessages()
        viewModelScope.launch(Dispatchers.IO) {
            val messages = historyRepository.getMessages(sessionId)
            val session = historyRepository.getSession(sessionId)
            
            val chatTurns = messages.map { msg ->
                when (msg) {
                    is ChatMessage.User -> ChatTurn("user", msg.content)
                    is ChatMessage.Ai -> ChatTurn("assistant", (msg.uiState as? UiState.Response)?.content ?: "")
                }
            }
            contextBuilder.loadSession(sessionId, chatTurns)
            
            val audioId = session?.linkedAudioId
            if (audioId != null) {
                try {
                    val artifacts = audioRepository.getArtifacts(audioId)
                    if (artifacts != null) {
                        val payload = buildString {
                            artifacts.smartSummary?.summary?.takeIf { it.isNotBlank() }?.let {
                                append("**系统摘要总结**\n")
                                append(it)
                                append("\n\n")
                            }
                            val transcript = artifacts.transcriptMarkdown ?: ""
                            if (transcript.isNotBlank()) {
                                append("**详细转写原文**\n")
                                append(transcript)
                            }
                        }
                        contextBuilder.loadDocumentContext(payload)
                    }
                } catch (e: Exception) {
                    Log.e("AgentVM", "Failed to load documentContext", e)
                }
            }

            withContext(Dispatchers.Main) {
                currentSessionId = sessionId
                _history.value = messages
                _sessionTitle.value = session?.clientName ?: "对话"
                _uiState.value = UiState.Idle
                _inputText.value = ""
                activityController.reset()
            }
        }
    }

    private fun persistCurrentMessages() {
        // Architecture update: Kernel (ContextBuilder) now auto-syncs chat turns to HistoryRepository 
        // via KernelWriteBack when IntentOrchestrator generates responses.
        // We no longer double-write from the UI layer.
    }

    fun clearToast() { _toastMessage.value = null }
    fun updateInput(text: String) { _inputText.value = text }
    fun clearError() { _errorMessage.value = null }
    fun cycleDebugState() { } // Deprecated
    fun amendAnalystPlan() {
        _uiState.value = UiState.Idle
        _toastMessage.value = "请在底部输入框输入您要修改的计划内容"
    }

    fun interactWithMascot(interaction: MascotInteraction) {
        viewModelScope.launch { mascotService.interact(interaction) }
    }

    fun updateSessionTitle(newTitle: String) {
        val title = newTitle.trim()
        if (title.isBlank()) return
        _sessionTitle.value = title
        
        val sid = currentSessionId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = historyRepository.getSession(sid) ?: return@launch
            historyRepository.renameSession(sid, title, session.summary)
        }
    }

    fun selectTaskBoardItem(itemId: String) {
        if (_isSending.value) return
        val items = _taskBoardItems.value
        val item = items.find { it.id == itemId } ?: return
        
        _isSending.value = true
        _uiState.value = UiState.ExecutingTool(item.title)
        
        viewModelScope.launch {
            try {
                val context = _inputText.value
                val request = PluginRequest(context, emptyMap())
                val gateway = object : PluginGateway {
                    override suspend fun getSessionHistory(turns: Int) = ""
                    override suspend fun appendToHistory(message: String) {}
                    override suspend fun emitProgress(message: String) {}
                }
                
                toolRegistry.executeTool(itemId, request, gateway).collect { state ->
                    withContext(Dispatchers.Main) {
                        if (state is UiState.Response || state is UiState.Error) {
                            if (state is UiState.Response) {
                                _history.value += ChatMessage.Ai(
                                    id = java.util.UUID.randomUUID().toString(),
                                    timestamp = System.currentTimeMillis(),
                                    uiState = state
                                )
                            } else if (state is UiState.Error) {
                                _errorMessage.value = "工具执行失败: ${state.message}"
                            }
                            _uiState.value = UiState.Idle
                            _isSending.value = false
                        } else {
                            _uiState.value = state
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "工具执行异常: ${e.message}"
                _uiState.value = UiState.Idle
                _isSending.value = false
            }
        }
    }

    private suspend fun executeToolDirectly(toolId: String, parameters: Map<String, Any> = emptyMap()) {
        val tools = toolRegistry.getAllTools()
        val tool = tools.find { it.id == toolId }
        val title = tool?.label ?: "未知工具"
        
        withContext(Dispatchers.Main) { _uiState.value = UiState.ExecutingTool(title) }
        
        try {
            val context = _inputText.value
            val request = PluginRequest(context, parameters)
            val gateway = object : PluginGateway {
                override suspend fun getSessionHistory(turns: Int) = ""
                override suspend fun appendToHistory(message: String) {}
                override suspend fun emitProgress(message: String) {}
            }
            
            toolRegistry.executeTool(toolId, request, gateway).collect { state ->
                withContext(Dispatchers.Main) {
                    if (state is UiState.Response || state is UiState.Error) {
                        if (state is UiState.Response) {
                            _history.value += ChatMessage.Ai(
                                id = java.util.UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                uiState = state
                            )
                        } else if (state is UiState.Error) {
                            _errorMessage.value = state.message
                        }
                        _uiState.value = UiState.Idle
                    } else {
                        _uiState.value = state
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _errorMessage.value = "工具执行异常: ${e.message}"
                _uiState.value = UiState.Idle
            }
        }
    }

    fun confirmAnalystPlan() {
        if (_isSending.value) return
        _isSending.value = true
        val input = "确认执行" 
        
        viewModelScope.launch {
            try {
                // Now delegates to the pure L3 IntentOrchestrator
                intentOrchestrator.processInput(input).collect { result ->
                     handlePipelineResult(result)
                }
                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to execute plan"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun send() {
        if (_isSending.value) return
        val input = _inputText.value.trim()
        if (input.isBlank()) return
        
        _history.value += ChatMessage.User(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = input
        )
        _inputText.value = ""
        _isSending.value = true
        _uiState.value = UiState.Loading 
        
        viewModelScope.launch {
            try {
                // The magic of Nuke & Pave: all routing logic is gone. 
                // Only clean delegation remains.
                intentOrchestrator.processInput(input).collect { result ->
                    handlePipelineResult(result)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "未知错误"
                _uiState.value = UiState.Error(e.message ?: "未知错误")
            } finally {
                if (_uiState.value is UiState.Loading) {
                    _uiState.value = UiState.Idle
                }
                _isSending.value = false
            }
        }
    }

    private suspend fun handlePipelineResult(result: PipelineResult) {
        when (result) {
            is PipelineResult.Progress -> {
                _uiState.value = UiState.Thinking(hint = result.message)
            }
            is PipelineResult.ConversationalReply -> {
                val ui = UiState.Response(result.text)
                _history.value += ChatMessage.Ai(
                    id = java.util.UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    uiState = ui
                )
                _uiState.value = UiState.Idle // Clear active state
            }
            is PipelineResult.AutoRenameTriggered -> {
                if (_sessionTitle.value == "新对话") {
                    updateSessionTitle(result.newTitle)
                }
            }
            is PipelineResult.DisambiguationIntercepted -> {
                if (result.uiState !is UiState.Idle) {
                    _history.value += ChatMessage.Ai(
                        id = java.util.UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        uiState = result.uiState
                    )
                }
                _uiState.value = UiState.Idle // Clear active state
            }
            is PipelineResult.ClarificationNeeded -> {
                val ui = UiState.Response(result.question)
                _history.value += ChatMessage.Ai(
                    id = java.util.UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    uiState = ui
                )
                _uiState.value = UiState.Idle
            }
            is PipelineResult.ToolDispatch -> {
                executeToolDirectly(result.toolId, result.params)
            }
            is PipelineResult.SchedulerTaskCreated -> {
                android.util.Log.w("AgentVM", "Deprecated: Received SchedulerTaskCreated routing. Use MutationProposal instead.")
            }
            is PipelineResult.SchedulerMultiTaskCreated -> {
                android.util.Log.w("AgentVM", "Deprecated: Received SchedulerMultiTaskCreated routing. Use MutationProposal instead.")
            }
            is PipelineResult.MutationProposal -> {
                // T3 Open-Loop Defense: Render a proposal card instead of mutating
                // In a full implementation, this would translate the Task domain/profile models into a UI card.
                // For now, we simulate the proposal with a conversational response.
                val prefix = if (result.isConflict) "⚠️ 有时间冲突。" else ""
                val taskStr = result.task?.let { "调度会议 [${it.title}]" } ?: ""
                val mutationStr = if (result.profileMutations.isNotEmpty()) {
                    "更新字段 [" + result.profileMutations.joinToString(", ") { "${it.field} -> ${it.value}" } + "]"
                } else ""
                
                val combined = listOf(taskStr, mutationStr).filter { it.isNotBlank() }.joinToString(" 并")
                
                val ui = UiState.Response("$prefix 已为您起草更新：$combined。请点击卡片确认。")
                _history.value += ChatMessage.Ai(
                    id = java.util.UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    uiState = ui
                )
                _uiState.value = UiState.Idle
            }
        }
    }

    // ------------------------------------------------------------------------
    // Debug Testing
    // ------------------------------------------------------------------------
    fun debugRunScenario(scenario: String) {
        when (scenario) {
            "MARKDOWN_BUBBLE" -> {
                val markdownContent = """
                    ### 周会分析报告
                    
                    以下是为您准备的客户跟进分析：
                    
                    * **流失预警**: A客户上周未拜访。
                    * **高意向**: B客户主动索要了报价单。
                    
                    **建议行动**：
                    请尽快安排针对A客户的回访，并准备B客户的合同草案。
                """.trimIndent()
                
                val ui = UiState.MarkdownStrategyState(
                    title = "深度分析完成",
                    markdownContent = markdownContent
                )
                
                _uiState.value = ui
                _history.value += ChatMessage.Ai(
                    id = java.util.UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    uiState = ui
                )
            }
            "CLARIFICATION_BUBBLE" -> {
                android.util.Log.d("AgentVM", "🧪 Injecting UiState.AwaitingClarification")
                val ui = UiState.AwaitingClarification(
                    question = "抱歉主理人，我需要一点细节，请问您想订个多长时间的会议？",
                    clarificationType = com.smartsales.prism.domain.model.ClarificationType.MISSING_DURATION
                )
                _uiState.value = ui
                _history.value += ChatMessage.Ai(
                    id = java.util.UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    uiState = ui
                )
            }

            "MULTI_INTENT_PROPOSAL" -> {
                android.util.Log.d("AgentVM", "🧪 Injecting simulated Multi-Intent Proposal")
                val ui = UiState.Response("已为您起草更新：调度会议 [与张总沟通价格] 并更新字段 [dealStage -> Won]。请点击卡片确认。")
                
                _uiState.value = ui
                _history.value += ChatMessage.Ai(
                    id = java.util.UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    uiState = ui
                )
            }
        }
    }
}
