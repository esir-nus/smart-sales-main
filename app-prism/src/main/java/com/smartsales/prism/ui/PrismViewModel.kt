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
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history: StateFlow<List<ChatMessage>> = _history.asStateFlow()
    
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
}
