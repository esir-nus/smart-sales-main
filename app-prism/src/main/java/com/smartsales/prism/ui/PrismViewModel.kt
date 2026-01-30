package com.smartsales.prism.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.pipeline.ExecutionPlan
import com.smartsales.prism.domain.pipeline.RetrievalScope
import com.smartsales.prism.domain.pipeline.DeliverableType
import javax.inject.Inject

/**
 * Prism ViewModel
 * 
 * Manages UI state for the Prism chat interface.
 * Uses Orchestrator (currently FakeOrchestrator) for processing.
 */
@HiltViewModel
class PrismViewModel @Inject constructor(
    private val orchestrator: Orchestrator,
    private val analystController: com.smartsales.prism.domain.pipeline.AnalystFlowController,
    private val activityController: com.smartsales.prism.domain.activity.AgentActivityController
) : ViewModel() {
    
    val currentMode: StateFlow<Mode> = orchestrator.currentMode
    
    // 代理活动状态（思考痕迹）
    val agentActivity: StateFlow<com.smartsales.prism.domain.activity.AgentActivity?> = activityController.activity
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // ... (rest of val props) ...
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

    // v2.6 Spec: Editable Session Title ("Executive Desk")
    private val _sessionTitle = MutableStateFlow("新对话") // Default: "New Session" (Localized)
    val sessionTitle: StateFlow<String> = _sessionTitle.asStateFlow()

    fun updateSessionTitle(newTitle: String) {
        if (newTitle.isNotBlank()) {
            _sessionTitle.value = newTitle.trim()
        }
    }

    init {
        // Observe Analyst FSM and map to UiState
        // ALSO persist AI responses to history so they don't vanish
        viewModelScope.launch {
            analystController.state.collect { fsmState ->
                when (fsmState) {
                    is com.smartsales.prism.domain.pipeline.AnalystState.Parsing -> {
                        _uiState.value = UiState.AnalystParsing(fsmState.currentTask, fsmState.progress)
                    }
                    is com.smartsales.prism.domain.pipeline.AnalystState.Planning -> {
                        _uiState.value = UiState.Thinking("Brain: " + fsmState.trace.lastOrNull().orEmpty())
                    }
                    is com.smartsales.prism.domain.pipeline.AnalystState.Proposal -> {
                        val proposalState = UiState.AnalystProposal(fsmState.plan)
                        _uiState.value = proposalState
                        // 持久化 Plan Card 到历史
                        val aiMsg = ChatMessage.Ai(
                            id = java.util.UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            uiState = proposalState
                        )
                        _history.value += aiMsg
                    }
                    is com.smartsales.prism.domain.pipeline.AnalystState.Executing -> {
                        _uiState.value = UiState.AnalystExecuting("执行计划中...")
                    }
                    is com.smartsales.prism.domain.pipeline.AnalystState.Result -> {
                        val resultState = UiState.AnalystResult(fsmState.artifact)
                        _uiState.value = resultState
                        // 持久化 Artifact Card 到历史
                        val aiMsg = ChatMessage.Ai(
                            id = java.util.UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            uiState = resultState
                        )
                        _history.value += aiMsg
                    }
                    else -> {} // Idle handled by standard flow
                }
            }
        }
    }

    /**
     * Start a new session (Clean Desk)
     * Clears history, resets title, sets state to Idle.
     */
    fun startNewSession() {
        _history.value = emptyList()
        _sessionTitle.value = "新对话"
        _inputText.value = ""
        _uiState.value = UiState.Idle
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
    
    fun send() {
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

        // Check for Analyst Plan Confirmation
        val currentState = uiState.value
        if (currentState is UiState.AnalystProposal) {
             // Mock Mode: Any input confirms the plan to keep the flow moving
             viewModelScope.launch {
                 analystController.confirmPlan()
             }
             return
        }

        // Analyst Mode Trigger
        if (currentMode.value == Mode.ANALYST) {
            _isSending.value = true // Show loading briefly
            viewModelScope.launch {
                analystController.startAnalysis(input)
                _isSending.value = false
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
                
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "未知错误"
                _uiState.value = UiState.Error(e.message ?: "未知错误")
            } finally {
                _isSending.value = false
            }
        }
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
}
