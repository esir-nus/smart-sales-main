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
import com.smartsales.prism.domain.scheduler.ScheduledTask
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
import kotlinx.coroutines.Job

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
) : ViewModel(), IAgentViewModel {

    // ------------------------------------------------------------------------
    // UI States
    // ------------------------------------------------------------------------
    override val agentActivity = activityController.activity

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    override val uiState = _uiState.onEach { state ->
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.UI_STATE_EMITTED,
            payloadSize = state.javaClass.simpleName.hashCode(),
            summary = "AgentViewModel emitted ${state.javaClass.simpleName}",
            rawDataDump = state.toString()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Idle)

    private val _inputText = MutableStateFlow("")
    override val inputText = _inputText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    override val isSending = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage = _errorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    override val toastMessage = _toastMessage.asStateFlow()

    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val history = _history.asStateFlow()

    // Task Board integration (mocked data collection)
    private val _taskBoardItems = MutableStateFlow<List<com.smartsales.prism.domain.analyst.TaskBoardItem>>(emptyList())
    override val taskBoardItems = _taskBoardItems.asStateFlow()

    private val _sessionTitle = MutableStateFlow("新对话")
    override val sessionTitle = _sessionTitle.asStateFlow()

    private val _heroUpcoming = MutableStateFlow<List<ScheduledTask>>(emptyList())
    override val heroUpcoming = _heroUpcoming.asStateFlow()

    private val _heroAccomplished = MutableStateFlow<List<ScheduledTask>>(emptyList())
    override val heroAccomplished = _heroAccomplished.asStateFlow()

    override val mascotState = mascotService.state

    private var currentSessionId: String? = null
    private var sessionBootstrapJob: Job? = null

    override val currentDisplayName: String
        get() = userProfileRepository.profile.value.displayName

    override val heroGreeting = userProfileRepository.profile
        .map { profile ->
            val hour = java.time.LocalTime.now().hour
            val timeGreeting = when {
                hour < 5 -> "夜深了"
                hour < 12 -> "早上好"
                hour < 18 -> "下午好"
                else -> "晚上好"
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
            .filterIsInstance<ScheduledTask>()
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
        _errorMessage.value = null
        _taskBoardItems.value = emptyList()
        activityController.reset()
        contextBuilder.resetSession()
        currentSessionId = null
        sessionBootstrapJob?.cancel()
        
        sessionBootstrapJob = viewModelScope.launch(Dispatchers.IO) {
            val sessionId = historyRepository.createSession("新对话", "新会话")
            contextBuilder.loadSession(sessionId, emptyList())
            currentSessionId = sessionId
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

    private suspend fun ensureActiveSessionReady(): String {
        sessionBootstrapJob?.join()
        currentSessionId?.let { return it }

        return withContext(Dispatchers.IO) {
            val sessionId = historyRepository.createSession("新对话", "新会话")
            contextBuilder.loadSession(sessionId, emptyList())
            currentSessionId = sessionId
            sessionId
        }
    }

    private suspend fun appendUserTurn(content: String) {
        ensureActiveSessionReady()
        withContext(Dispatchers.IO) {
            contextBuilder.recordUserMessage(content)
        }
        _history.value += ChatMessage.User(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = content
        )
    }

    private fun sessionMemoryText(uiState: UiState): String? {
        return when (uiState) {
            is UiState.Response -> uiState.content
            is UiState.AwaitingClarification -> buildString {
                append(uiState.question)
                if (uiState.candidates.isNotEmpty()) {
                    append("\n候选项: ")
                    append(uiState.candidates.joinToString(" / ") { candidate -> candidate.displayName })
                }
            }
            UiState.BadgeDelegationHint -> "该请求需要通过胸牌端继续完成。"
            is UiState.Error -> uiState.message
            else -> null
        }
    }

    private suspend fun appendAssistantTurn(uiState: UiState) {
        ensureActiveSessionReady()
        sessionMemoryText(uiState)?.let { content ->
            withContext(Dispatchers.IO) {
                contextBuilder.recordAssistantMessage(content)
            }
        }
        _history.value += ChatMessage.Ai(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            uiState = uiState
        )
    }

    override fun clearToast() { _toastMessage.value = null }
    override fun updateInput(text: String) { _inputText.value = text }
    override fun clearError() { _errorMessage.value = null }
    fun cycleDebugState() { } // Deprecated
    override fun amendAnalystPlan() {
        _uiState.value = UiState.Idle
        _toastMessage.value = "请在底部输入框输入您要修改的计划内容"
    }

    override fun interactWithMascot(interaction: MascotInteraction) {
        viewModelScope.launch { mascotService.interact(interaction) }
    }

    override fun updateSessionTitle(newTitle: String) {
        val title = newTitle.trim()
        if (title.isBlank()) return
        _sessionTitle.value = title
        
        val sid = currentSessionId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val session = historyRepository.getSession(sid) ?: return@launch
            historyRepository.renameSession(sid, title, session.summary)
        }
    }

    override fun selectTaskBoardItem(itemId: String) {
        if (_isSending.value) return
        val items = _taskBoardItems.value
        val item = items.find { it.id == itemId } ?: return
        
        _isSending.value = true
        _uiState.value = UiState.ExecutingTool(item.title)
        _errorMessage.value = null
        _taskBoardItems.value = emptyList()
        
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
                                appendAssistantTurn(state)
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
                            appendAssistantTurn(state)
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

    override fun confirmAnalystPlan() {
        if (_isSending.value) return
        _isSending.value = true
        _errorMessage.value = null
        _taskBoardItems.value = emptyList()
        val input = "确认执行" 
        
        viewModelScope.launch {
            try {
                appendUserTurn(input)
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

    override fun send() {
        if (_isSending.value) return
        val input = _inputText.value.trim()
        if (input.isBlank()) return
        _inputText.value = ""
        _isSending.value = true
        _uiState.value = UiState.Loading 
        _errorMessage.value = null
        _taskBoardItems.value = emptyList()
        
        viewModelScope.launch {
            try {
                appendUserTurn(input)
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
            is PipelineResult.PathACommitted -> {
                _uiState.value = UiState.Idle
            }
            is PipelineResult.Progress -> {
                _uiState.value = UiState.Thinking(hint = result.message)
            }
            is PipelineResult.ConversationalReply -> {
                val ui = UiState.Response(result.text)
                appendAssistantTurn(ui)
                _uiState.value = UiState.Idle // Clear active state
            }
            is PipelineResult.AutoRenameTriggered -> {
                if (_sessionTitle.value == "新对话") {
                    updateSessionTitle(result.newTitle)
                }
            }
            is PipelineResult.DisambiguationIntercepted -> {
                if (result.uiState !is UiState.Idle) {
                    appendAssistantTurn(result.uiState)
                }
                _uiState.value = UiState.Idle // Clear active state
            }
            is PipelineResult.ClarificationNeeded -> {
                val ui = UiState.Response(result.question)
                appendAssistantTurn(ui)
                _uiState.value = UiState.Idle
            }
            is PipelineResult.TaskCommandProposal -> {
                val message = when (val command = result.command) {
                    is com.smartsales.core.pipeline.SchedulerTaskCommand.CreateTasks ->
                        "已为您起草日程创建，请点击卡片确认。"
                    is com.smartsales.core.pipeline.SchedulerTaskCommand.DeleteTask ->
                        "已为您起草日程删除，请点击卡片确认。"
                    is com.smartsales.core.pipeline.SchedulerTaskCommand.RescheduleTask ->
                        "已为您起草日程改期，请点击卡片确认。"
                }
                appendAssistantTurn(UiState.Response(message))
                _uiState.value = UiState.Idle
            }
            is PipelineResult.ToolRecommendation -> {
                val tools = toolRegistry.getAllTools()
                val items = result.recommendations.mapNotNull { rec ->
                    val tool = tools.find { it.id == rec.workflowId } ?: return@mapNotNull null
                    com.smartsales.prism.domain.analyst.TaskBoardItem(
                        id = tool.id,
                        icon = tool.icon,
                        title = tool.label,
                        description = tool.description
                    )
                }
                _taskBoardItems.value = items

                val ui = UiState.Response("我发现了几个可以帮您执行的工具，请在任务板确认运行。")
                appendAssistantTurn(ui)
                _uiState.value = UiState.Idle
            }
            is PipelineResult.ToolDispatch -> {
                executeToolDirectly(result.toolId, result.params)
            }

            is PipelineResult.MutationProposal -> {
                val mutationStr = if (result.profileMutations.isNotEmpty()) {
                    "更新字段 [" + result.profileMutations.joinToString(", ") { "${it.field} -> ${it.value}" } + "]"
                } else ""
                
                val combined = listOf(mutationStr).filter { it.isNotBlank() }.joinToString(" 并")
                
                val ui = UiState.Response("已为您起草更新：$combined。请点击卡片确认。")
                appendAssistantTurn(ui)
                _uiState.value = UiState.Idle
            }
            is PipelineResult.PluginExecutionStarted -> {
                _uiState.value = UiState.ExecutingTool(result.toolId)
            }
            is PipelineResult.PluginExecutionEmittedState -> {
                if (result.uiState is UiState.Response) {
                    appendAssistantTurn(result.uiState)
                }
                _uiState.value = result.uiState
            }
            is PipelineResult.MascotIntercepted -> {
                Log.d("AgentVM", "Intent intercepted by Mascot. Dropping to Idle.")
                _uiState.value = UiState.Idle 
            }
            is PipelineResult.BadgeDelegationIntercepted -> {
                Log.d("AgentVM", "Hardware delegation intercepted. Emitting BadgeDelegationHint.")
                val ui = UiState.BadgeDelegationHint
                appendAssistantTurn(ui)
                _uiState.value = UiState.Idle 
            }
        }
    }

    // ------------------------------------------------------------------------
    // Debug Testing
    // ------------------------------------------------------------------------
    override fun debugRunScenario(scenario: String) {
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
            "BADGE_DELEGATION_HINT" -> {
                android.util.Log.d("AgentVM", "🧪 Injecting simulated Badge Delegation Hint")
                val ui = UiState.BadgeDelegationHint
                
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
