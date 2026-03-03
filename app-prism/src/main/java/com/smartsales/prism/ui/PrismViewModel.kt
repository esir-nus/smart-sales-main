package com.smartsales.prism.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val orchestrator: Orchestrator,
    private val analystPipeline: com.smartsales.prism.domain.analyst.AnalystPipeline,
    private val toolRegistry: com.smartsales.prism.domain.analyst.ToolRegistry,
    private val activityController: com.smartsales.prism.domain.activity.AgentActivityController,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val contextBuilder: com.smartsales.prism.domain.pipeline.ContextBuilder,
    private val historyRepository: com.smartsales.prism.domain.repository.HistoryRepository,
    private val userProfileRepository: UserProfileRepository,
    private val audioRepository: com.smartsales.prism.domain.audio.AudioRepository,
    private val sessionTitleGenerator: com.smartsales.prism.domain.session.SessionTitleGenerator // Wave 4: Auto-Renaming
) : ViewModel() {
    
    // ------------------------------------------------------------------------
    // State Properties (Restored)
    // ------------------------------------------------------------------------
    
    val currentMode: StateFlow<Mode> = orchestrator.currentMode
    
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

        // V1/V2 Analyst FSM replacement: observe pipeline state for thinking indicators
        viewModelScope.launch {
            analystPipeline.state.collect { fsmState ->
                when (fsmState) {
                    com.smartsales.prism.domain.analyst.AnalystState.CONSULTING -> {
                        _uiState.value = UiState.Thinking("分析意图中...")
                    }
                    com.smartsales.prism.domain.analyst.AnalystState.INVESTIGATING -> {
                        _uiState.value = UiState.Thinking("深度分析中...")
                    }
                    com.smartsales.prism.domain.analyst.AnalystState.IDLE -> {
                        if (_uiState.value is UiState.Thinking) {
                            _uiState.value = UiState.Idle
                        }
                    }
                    else -> {}
                }
            }
        }

        // Hero Dashboard: 加载待办和已完成任务
        viewModelScope.launch {
            loadHeroDashboard()
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
    fun switchSession(sessionId: String, targetMode: Mode? = null, triggerAutoRename: Boolean = false) {
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
                
                if (targetMode != null && targetMode != currentMode.value) {
                    switchMode(targetMode)
                }
                
                if (triggerAutoRename) {
                    triggerAutoRename()
                }
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
    
    fun switchMode(mode: Mode) {
        viewModelScope.launch {
            orchestrator.switchMode(mode)
            _uiState.value = UiState.Idle
            // 显示模式切换提示
            val modeName = when (mode) {
                Mode.COACH -> "教练模式"
                Mode.ANALYST -> "分析师模式"
                Mode.SCHEDULER -> "日程模式"
            }
            _toastMessage.value = "已切换至$modeName"
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
        if (currentMode.value != Mode.ANALYST) return
        if (_isSending.value) return
        
        _isSending.value = true
        // Trigger execution phase with a simulated user confirmation
        val input = "确认执行" 
        
        // Don't add to visible history to keep chat clean
        
        viewModelScope.launch {
            try {
                // Pass history from ContextBuilder (which includes the AI's proposal)
                val response = analystPipeline.handleInput(input, _history.value.mapNotNull { msg ->
                    when (msg) {
                        is ChatMessage.User -> ChatTurn("user", msg.content)
                        is ChatMessage.Ai -> if (msg.uiState is UiState.Response) ChatTurn("assistant", msg.uiState.content) 
                                             else if (msg.uiState is UiState.MarkdownStrategyState) ChatTurn("assistant", "### \${msg.uiState.title}\\n\${msg.uiState.markdownContent}")
                                             else null
                    }
                })
                
                // Usually results in Result state (Analysis)
                if (response is com.smartsales.prism.domain.analyst.AnalystResponse.Analysis) {
                    val tools = toolRegistry.getAllTools()
                    val items = response.suggestedWorkflows.mapNotNull { suggestion ->
                        val tool = tools.find { it.id == suggestion.workflowId }
                        if (tool != null) {
                            com.smartsales.prism.domain.analyst.TaskBoardItem(
                                id = tool.id,
                                icon = tool.icon,
                                title = suggestion.label,
                                description = tool.description
                            )
                        } else null
                    }
                    _taskBoardItems.value = items
                    
                    val aiMsg = ChatMessage.Ai(
                        id = java.util.UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        uiState = UiState.Response(response.content)
                    )
                    _history.value += aiMsg
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
                val result = toolRegistry.executeTool(itemId, context)
                
                android.util.Log.d("TaskBoardTool", "✅ PrismViewModel: execution finished for \${item.title}")
                
                if (result.success) {
                    // Create a response message with the tool result payload
                    val content = "✅ **${item.title} 执行完毕**\n\n${result.previewText}"
                    val resultMessage = ChatMessage.Ai(
                        id = java.util.UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        uiState = UiState.Response(content)
                    )
                    _history.value += resultMessage
                } else {
                    _errorMessage.value = "工具执行失败: ${result.previewText}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "工具执行异常: ${e.message}"
            } finally {
                _uiState.value = UiState.Idle
                _isSending.value = false
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

        // Cerb Analyst Mode Open-Loop Pipeline
        if (currentMode.value == Mode.ANALYST) {
            _isSending.value = true
            viewModelScope.launch {
                // Map history properly (simplified for L2)
                val chatTurns = _history.value.mapNotNull { msg ->
                    when (msg) {
                        is ChatMessage.User -> ChatTurn("user", msg.content)
                        is ChatMessage.Ai -> if (msg.uiState is UiState.Response) ChatTurn("assistant", msg.uiState.content) else null
                    }
                }
                
                try {
                    val response = analystPipeline.handleInput(input, chatTurns)
                    
                    val finalUiState = when (response) {
                        is com.smartsales.prism.domain.analyst.AnalystResponse.Chat -> {
                            UiState.Response(response.content)
                        }
                        is com.smartsales.prism.domain.analyst.AnalystResponse.Plan -> {
                            UiState.MarkdownStrategyState(
                                title = response.title,
                                markdownContent = response.markdownContent
                            )
                        }
                        is com.smartsales.prism.domain.analyst.AnalystResponse.Analysis -> {
                            // Map WorkflowSuggestion to TaskBoardItem using ToolRegistry
                            val tools = toolRegistry.getAllTools()
                            val items = response.suggestedWorkflows.mapNotNull { suggestion ->
                                val tool = tools.find { it.id == suggestion.workflowId }
                                if (tool != null) {
                                    com.smartsales.prism.domain.analyst.TaskBoardItem(
                                        id = tool.id,
                                        icon = tool.icon,
                                        title = suggestion.label, // Use LLM's suggested label
                                        description = tool.description
                                    )
                                } else null
                            }
                            _taskBoardItems.value = items
                            
                            // Render analysis content into chat
                            UiState.Response(response.content)
                        }
                    }
                    
                    _uiState.value = UiState.Idle
                    val aiMsg = ChatMessage.Ai(
                        id = java.util.UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        uiState = finalUiState
                    )
                    _history.value += aiMsg
                } catch (e: Exception) {
                    _errorMessage.value = e.message ?: "Analyst error"
                } finally {
                    _isSending.value = false
                }
            }
            return
        }
        
        _isSending.value = true
        _uiState.value = UiState.Loading 
        
        viewModelScope.launch {
            try {
                val result = orchestrator.processInput(input)
                _uiState.value = UiState.Idle 
                
                // 2. 添加 AI 响应到历史
                val aiMsg = ChatMessage.Ai(
                    id = java.util.UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    uiState = result
                )
                _history.value += aiMsg
                
                // Wave 4: Auto-Renaming Trigger
                // If title is default ("新对话") AND we have a successful response, trigger renaming
                if (_sessionTitle.value == "新对话" && result is UiState.Response) {
                    triggerAutoRename()
                }
                
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "未知错误"
                _uiState.value = UiState.Error(e.message ?: "未知错误")
            } finally {
                _isSending.value = false
            }
        }
    }

    private fun triggerAutoRename() {
        val sid = currentSessionId ?: return
        val currentHistory = contextBuilder.getSessionHistory()
        
        // Don't rename on zero history (shouldn't happen here but safe guard)
        if (currentHistory.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val titleResult = sessionTitleGenerator.generateTitle(currentHistory)
                
                // Persist new title
                historyRepository.renameSession(sid, titleResult.clientName, titleResult.summary)
                
                // Update UI immediately
                withContext(Dispatchers.Main) {
                    _sessionTitle.value = titleResult.clientName
                }
                Log.d("PrismVM", "Auto-renamed session $sid to '${titleResult.clientName}'")
            } catch (e: Exception) {
                Log.w("PrismVM", "Auto-rename failed", e)
            }
        }
    }
    
    // ... (rest of methods)
}
