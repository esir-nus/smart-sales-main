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
    private val orchestrator: Orchestrator
) : ViewModel() {
    
    val currentMode: StateFlow<Mode> = orchestrator.currentMode
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // ... (rest of val props) ...
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
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
        }
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
        
        _isSending.value = true
        _inputText.value = ""
        _uiState.value = UiState.Loading // 临时状态，不存入历史
        
        viewModelScope.launch {
            try {
                val result = orchestrator.processInput(input)
                _uiState.value = UiState.Idle // 处理完成，回到 Idle
                
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
        val nextState = when (_uiState.value) {
            is UiState.Idle -> UiState.Thinking("Analyzing Context...")
            is UiState.Thinking -> UiState.PlanCard(
                plan = ExecutionPlan(
                    retrievalScope = RetrievalScope.HOT_ONLY,
                    deliverables = listOf(
                        DeliverableType.KEY_INSIGHT,
                        DeliverableType.CHAT_RESPONSE
                    )
                ),
                completedSteps = setOf(0)
            )
            is UiState.PlanCard -> UiState.Streaming("This is a simulated AI response stream... (Part 1)")
            is UiState.Streaming -> UiState.Response("This is a simulated AI response stream... (Part 1) [Completed]")
            else -> UiState.Idle
        }
        _uiState.value = nextState
    }
}
