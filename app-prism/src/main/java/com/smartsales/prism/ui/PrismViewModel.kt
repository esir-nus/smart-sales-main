package com.smartsales.prism.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.unifiedpipeline.UnifiedPipeline
import com.smartsales.prism.domain.analyst.LightningRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.pipeline.ChatTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.domain.repository.UserProfileRepository
import javax.inject.Inject
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import com.smartsales.prism.domain.scheduler.UrgencyLevel

/**
 * Prism ViewModel
 * 
 * Manages UI state for the Prism chat interface.
 * Uses V2 Analyst Controller for Analyst mode.
 */
@HiltViewModel
class PrismViewModel @Inject constructor(
    private val unifiedPipeline: UnifiedPipeline,
    private val lightningRouter: LightningRouter,

    private val toolRegistry: com.smartsales.prism.domain.analyst.ToolRegistry,
    private val activityController: com.smartsales.prism.domain.activity.AgentActivityController,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val contextBuilder: com.smartsales.prism.domain.pipeline.ContextBuilder,
    private val historyRepository: com.smartsales.prism.domain.repository.HistoryRepository,
    private val userProfileRepository: UserProfileRepository,
    private val audioRepository: com.smartsales.prism.domain.audio.AudioRepository,
    private val sessionTitleGenerator: com.smartsales.prism.domain.session.SessionTitleGenerator, // Wave 4: Auto-Renaming
    private val eventBus: com.smartsales.prism.domain.system.SystemEventBus,
    private val mascotService: com.smartsales.prism.domain.mascot.MascotService
) : ViewModel() {
    
    // ------------------------------------------------------------------------
    // State Properties (Restored)
    // ------------------------------------------------------------------------
    
    // 代理活动状态（思考痕迹）
    val agentActivity: StateFlow<com.smartsales.prism.domain.activity.AgentActivity?> = activityController.activity
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Toast 消息（一次性事件）
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()
    
    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history: StateFlow<List<ChatMessage>> = _history.asStateFlow()

    // Wave 4: 当前活跃会话 ID
    private var currentSessionId: String? = null

    // V2: Task Board items
    private val _taskBoardItems = MutableStateFlow<List<com.smartsales.prism.domain.analyst.TaskBoardItem>>(emptyList())
    val taskBoardItems: StateFlow<List<com.smartsales.prism.domain.analyst.TaskBoardItem>> = _taskBoardItems.asStateFlow()

    // v2.6 Spec: Editable Session Title ("Executive Desk")
    private val _sessionTitle = MutableStateFlow("新对话") // Default: "New Session" (Localized)
    val sessionTitle: StateFlow<String> = _sessionTitle.asStateFlow()

    // Hero Dashboard 状态
    private val _heroUpcoming = MutableStateFlow<List<TimelineItemModel.Task>>(emptyList())
    val heroUpcoming: StateFlow<List<TimelineItemModel.Task>> = _heroUpcoming.asStateFlow()

    private val _heroAccomplished = MutableStateFlow<List<TimelineItemModel.Task>>(emptyList())
    val heroAccomplished: StateFlow<List<TimelineItemModel.Task>> = _heroAccomplished.asStateFlow()

    // 当前用户名 — 供 Shell 传递给 HistoryDrawer
    val currentDisplayName: String
        get() = userProfileRepository.profile.value.displayName

    // 动态问候：从 UserProfileRepository 读取 displayName
    val heroGreeting: StateFlow<String> = userProfileRepository.profile
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

    // ========================================================================
    // Wave 4: Mascot System I Integration
    // ========================================================================
    val mascotState: StateFlow<com.smartsales.prism.domain.mascot.MascotState> = mascotService.state

    fun interactWithMascot(interaction: com.smartsales.prism.domain.mascot.MascotInteraction) {
        viewModelScope.launch {
            mascotService.interact(interaction)
        }
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

    init {
        // Init: Start a fresh session so messages are persisted from the start
        startNewSession()

        // V1/V2 Analyst FSM replacement: observe pipeline state for thinking indicators (handled by unified pipeline now)

        // Hero Dashboard: 加载待办和已完成任务
        viewModelScope.launch {
            loadHeroDashboard()
        }

        // AppIdle Debounce Timer (Wave 3)
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(uiState, inputText) { state, _ ->
                Pair(state, null)
            }.collectLatest { (state, _) ->
                if (state is com.smartsales.prism.domain.model.UiState.Idle) {
                    kotlinx.coroutines.delay(15000) // 15 seconds
                    Log.d("PrismVM", "System idle for 15s, emitting AppIdle event")
                    eventBus.publish(com.smartsales.prism.domain.system.SystemEvent.AppIdle)
                }
            }
        }
    }

    private suspend fun loadHeroDashboard() {
        // 待办: 今天起3天内, 未完成, 按时间排序, 取前3
        val today = LocalDate.now()
        val allUpcoming = scheduledTaskRepository
            .queryByDateRange(today, today.plusDays(3))
            .first()
        _heroUpcoming.value = allUpcoming
            .filterIsInstance<TimelineItemModel.Task>()
            .filter { !it.isDone }
            .filter { it.urgencyLevel != UrgencyLevel.FIRE_OFF }
            .take(3)

        // 已完成: 最近7天, 取前2
        _heroAccomplished.value = scheduledTaskRepository.getRecentCompleted(2)
    }

    /** 外部触发刷新 Hero Dashboard（如关闭日程抽屉时） */
    fun refreshHeroDashboard() {
        viewModelScope.launch { loadHeroDashboard() }
    }

    /**
     * Start a new session (Clean Desk)
     * Clears history, resets title, sets state to Idle.
     */
    fun startNewSession() {
        // Wave 4: 先保存当前会话消息
        persistCurrentMessages()
        
        _history.value = emptyList()
        _sessionTitle.value = "新对话"
        _inputText.value = ""
        _uiState.value = UiState.Idle
        activityController.reset() // 清除持久化的 ThinkingBox
        
        // Wave 3: 真实会话重置
        contextBuilder.resetSession() // 重置内核 RAM

        
        // 创建新会话记录 (SSD)
        viewModelScope.launch(Dispatchers.IO) {
            currentSessionId = historyRepository.createSession("新对话", "新会话")
            Log.d("PrismVM", "New session: $currentSessionId")
        }
    }

    /**
     * 切换到历史会话 (Wave 4)
     * 从 SSD 加载消息，恢复 UI 和内核上下文
     */
    fun switchSession(sessionId: String, triggerAutoRename: Boolean = false) {
        // 先保存当前会话
        persistCurrentMessages()
        viewModelScope.launch(Dispatchers.IO) {
            val messages = historyRepository.getMessages(sessionId)
            val session = historyRepository.getSession(sessionId)
            // 恢复内核 RAM
            val chatTurns = messages.map { msg ->
                when (msg) {
                    is ChatMessage.User -> ChatTurn("user", msg.content)
                    is ChatMessage.Ai -> ChatTurn("assistant",
                        (msg.uiState as? UiState.Response)?.content ?: "")
                }
            }
            contextBuilder.loadSession(sessionId, chatTurns)
            
            // Wave 4: 注入临时文档上下文
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
                        Log.d("PrismVM", "📄 Injected documentContext for session: $sessionId from audio: $audioId")
                    }
                } catch (e: Exception) {
                    Log.e("PrismVM", "Failed to load documentContext", e)
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
            Log.d("PrismVM", "Switched to session: $sessionId, ${messages.size} messages")
        }
    }

    /**
     * 持久化当前会话消息到 SSD (Wave 4)
     * 清除旧消息再重新写入（简单但有效的 v1 方案）
     */
    private fun persistCurrentMessages() {
        val sid = currentSessionId ?: return
        val msgs = _history.value
        if (msgs.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.clearMessages(sid)
            msgs.forEachIndexed { idx, msg ->
                when (msg) {
                    is ChatMessage.User ->
                        historyRepository.saveMessage(sid, true, msg.content, idx)
                    is ChatMessage.Ai ->
                        historyRepository.saveMessage(sid, false,
                            (msg.uiState as? UiState.Response)?.content ?: "", idx)
                }
            }
            Log.d("PrismVM", "Persisted ${msgs.size} messages for session: $sid")
        }
    }
    

    fun clearToast() {
        _toastMessage.value = null
    }
    
    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Debug Tool: Cycle through UI States manually
     * Sequence: Idle -> Thinking -> PlanCard -> Response -> Idle
     */
    fun cycleDebugState() {
         // Deprecated by FSM
    }

    fun confirmAnalystPlan() {
        if (_isSending.value) return
        
        _isSending.value = true
        // Trigger execution phase with a simulated user confirmation
        val input = "确认执行" 
        
        // Don't add to visible history to keep chat clean
        
        viewModelScope.launch {
            try {
                // Phase 0 Intent
                val context = contextBuilder.build(input, Mode.ANALYST, depth = com.smartsales.prism.domain.pipeline.ContextDepth.MINIMAL)
                val routerResult = lightningRouter.evaluateIntent(context)
                
                val pipelineInput = com.smartsales.prism.domain.unifiedpipeline.PipelineInput(
                    rawText = input,
                    isVoice = false,
                    intent = routerResult?.queryQuality ?: com.smartsales.prism.domain.analyst.QueryQuality.DEEP_ANALYSIS
                )
                
                unifiedPipeline.processInput(pipelineInput).collect { result ->
                     when (result) {
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.ConversationalReply -> {
                             val ui = UiState.Response(result.text)
                             _uiState.value = ui
                             _history.value += ChatMessage.Ai(
                                 id = java.util.UUID.randomUUID().toString(),
                                 timestamp = System.currentTimeMillis(),
                                 uiState = ui
                             )
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.AutoRenameTriggered -> {
                             // Wave 4: Synchronous Auto-Renaming
                             if (_sessionTitle.value == "新对话") {
                                 updateSessionTitle(result.newTitle)
                             }
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.DisambiguationIntercepted -> {
                             _uiState.value = result.uiState
                             if (result.uiState !is UiState.Idle) {
                                 _history.value += ChatMessage.Ai(
                                     id = java.util.UUID.randomUUID().toString(),
                                     timestamp = System.currentTimeMillis(),
                                     uiState = result.uiState
                                 )
                             }
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.ClarificationNeeded -> {
                             val ui = UiState.Response(result.question)
                             _uiState.value = ui
                             _history.value += ChatMessage.Ai(
                                 id = java.util.UUID.randomUUID().toString(),
                                 timestamp = System.currentTimeMillis(),
                                 uiState = ui
                             )
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.ToolDispatch -> {
                             executeToolDirectly(result.toolId, result.params)
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.SchedulerTaskCreated -> {
                             val ui = UiState.Response("已为您创建日程：${result.title}")
                             _uiState.value = ui
                             _history.value += ChatMessage.Ai(
                                 id = java.util.UUID.randomUUID().toString(),
                                 timestamp = System.currentTimeMillis(),
                                 uiState = ui
                             )
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.SchedulerMultiTaskCreated -> {
                             val ui = UiState.Response("已为您创建 ${result.tasks.size} 个日程")
                             _uiState.value = ui
                             _history.value += ChatMessage.Ai(
                                 id = java.util.UUID.randomUUID().toString(),
                                 timestamp = System.currentTimeMillis(),
                                 uiState = ui
                             )
                         }
                     }
                }
                
                _uiState.value = UiState.Idle
                
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to execute plan"
            } finally {
                _isSending.value = false
            }
        }
    }

    fun amendAnalystPlan() {
        // Just reset the pipeline state to start over
        _uiState.value = UiState.Idle
        // Wait, the state machine in RealAnalystPipeline doesn't expose a "reset" or "back" action directly yet.
        // For now, we just tell the user to type their amendments.
        _toastMessage.value = "请在底部输入框输入您要修改的计划内容"
    }

    // ========================================================================
    // V2: Task Board Interaction
    // ========================================================================
    
    /**
     * Handle Task Board item selection (Phase 4: OS Execution Bypass)
     */
    fun selectTaskBoardItem(itemId: String) {
        if (_isSending.value) return
        
        val items = _taskBoardItems.value
        val item = items.find { it.id == itemId } ?: return
        
        _isSending.value = true
        _uiState.value = UiState.ExecutingTool(item.title)
        android.util.Log.d("TaskBoardTool", "🚀 PrismViewModel: Mounting ExecutingTool for \${item.title}")
        
        viewModelScope.launch {
            try {
                // Pass the current input text or empty string as context if needed (mostly mock for now)
                val context = _inputText.value
                val request = com.smartsales.prism.domain.analyst.PluginRequest(context, emptyMap())
                
                toolRegistry.executeTool(itemId, request).collect { state ->
                    withContext(Dispatchers.Main) {
                        if (state is UiState.Response || state is UiState.Error) {
                            if (state is UiState.Response) {
                                val resultMessage = ChatMessage.Ai(
                                    id = java.util.UUID.randomUUID().toString(),
                                    timestamp = System.currentTimeMillis(),
                                    uiState = state
                                )
                                _history.value += resultMessage
                            } else if (state is UiState.Error) {
                                _errorMessage.value = "工具执行失败: ${state.message}"
                            }
                            _uiState.value = UiState.Idle
                            _isSending.value = false
                        } else {
                            // Forward partial states
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

    /**
     * Reusable fast-path tool execution (used by TaskBoard clicks AND Expert Bypass)
     */
    private suspend fun executeToolDirectly(toolId: String, parameters: Map<String, Any> = emptyMap()) {
        val tools = toolRegistry.getAllTools()
        val tool = tools.find { it.id == toolId }
        val title = tool?.label ?: "未知工具"
        
        withContext(Dispatchers.Main) {
            _uiState.value = UiState.ExecutingTool(title)
        }
        
        try {
            val context = _inputText.value
            val request = com.smartsales.prism.domain.analyst.PluginRequest(context, parameters)
            
            toolRegistry.executeTool(toolId, request).collect { state ->
                withContext(Dispatchers.Main) {
                    if (state is UiState.Response || state is UiState.Error) {
                        if (state is UiState.Response) {
                            val resultMessage = ChatMessage.Ai(
                                id = java.util.UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                uiState = state
                            )
                            _history.value += resultMessage
                        } else if (state is UiState.Error) {
                            _errorMessage.value = state.message
                        }
                        _uiState.value = UiState.Idle
                    } else {
                        // Reflect intermediate plugin states (e.g. ExecutingTool with specific progress text)
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

    fun send() {
        if (_isSending.value) return
        val input = _inputText.value.trim()
        if (input.isBlank()) return
        
        // 1. 添加用户消息到历史
        val userMsg = ChatMessage.User(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = input
        )
        _history.value += userMsg
        _inputText.value = ""

        _isSending.value = true
        _uiState.value = UiState.Loading 
        
        viewModelScope.launch {
            try {
                val context = contextBuilder.build(input, Mode.ANALYST, depth = com.smartsales.prism.domain.pipeline.ContextDepth.MINIMAL)
                val routerResult = lightningRouter.evaluateIntent(context)
                
                if (routerResult?.queryQuality == com.smartsales.prism.domain.analyst.QueryQuality.NOISE) {
                    mascotService.interact(com.smartsales.prism.domain.mascot.MascotInteraction.Text(input))
                    _uiState.value = UiState.Idle
                    _isSending.value = false
                    return@launch
                } else if (routerResult?.queryQuality == com.smartsales.prism.domain.analyst.QueryQuality.GREETING) {
                    mascotService.interact(com.smartsales.prism.domain.mascot.MascotInteraction.Text(input))
                    _uiState.value = UiState.Idle
                    _isSending.value = false
                    return@launch
                } else if (routerResult?.queryQuality == com.smartsales.prism.domain.analyst.QueryQuality.VAGUE) {
                    val result = UiState.Response(routerResult.response ?: "Please provide more details.")
                    _uiState.value = result
                    _history.value += ChatMessage.Ai(
                        id = java.util.UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        uiState = result
                    )
                    _uiState.value = UiState.Idle
                    _isSending.value = false
                    return@launch
                }
                
                val pipelineInput = com.smartsales.prism.domain.unifiedpipeline.PipelineInput(
                    rawText = input,
                    isVoice = false,
                    intent = routerResult?.queryQuality ?: com.smartsales.prism.domain.analyst.QueryQuality.DEEP_ANALYSIS
                )
                
                unifiedPipeline.processInput(pipelineInput).collect { result ->
                     when (result) {
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.ConversationalReply -> {
                             val ui = UiState.Response(result.text)
                             _uiState.value = ui
                             _history.value += ChatMessage.Ai(
                                 id = java.util.UUID.randomUUID().toString(),
                                 timestamp = System.currentTimeMillis(),
                                 uiState = ui
                             )
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.AutoRenameTriggered -> {
                             // Wave 4: Synchronous Auto-Renaming from upstream Parser
                             if (_sessionTitle.value == "新对话") {
                                 updateSessionTitle(result.newTitle)
                             }
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.DisambiguationIntercepted -> {
                             _uiState.value = result.uiState
                             if (result.uiState !is UiState.Idle) {
                                 _history.value += ChatMessage.Ai(
                                     id = java.util.UUID.randomUUID().toString(),
                                     timestamp = System.currentTimeMillis(),
                                     uiState = result.uiState
                                 )
                             }
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.ClarificationNeeded -> {
                             val ui = UiState.Response(result.question)
                             _uiState.value = ui
                             _history.value += ChatMessage.Ai(
                                 id = java.util.UUID.randomUUID().toString(),
                                 timestamp = System.currentTimeMillis(),
                                 uiState = ui
                             )
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.ToolDispatch -> {
                             executeToolDirectly(result.toolId, result.params)
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.SchedulerTaskCreated -> {
                             val ui = UiState.Response("已为您创建日程：${result.title}")
                             _uiState.value = ui
                             _history.value += ChatMessage.Ai(
                                 id = java.util.UUID.randomUUID().toString(),
                                 timestamp = System.currentTimeMillis(),
                                 uiState = ui
                             )
                         }
                         is com.smartsales.prism.domain.unifiedpipeline.PipelineResult.SchedulerMultiTaskCreated -> {
                             val ui = UiState.Response("已为您创建 ${result.tasks.size} 个日程")
                             _uiState.value = ui
                             _history.value += ChatMessage.Ai(
                                 id = java.util.UUID.randomUUID().toString(),
                                 timestamp = System.currentTimeMillis(),
                                 uiState = ui
                             )
                         }
                     }
                }
                
                _uiState.value = UiState.Idle
                
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "未知错误"
                _uiState.value = UiState.Error(e.message ?: "未知错误")
            } finally {
                _isSending.value = false
            }
        }
    }


    
    // ... (rest of methods)
}
