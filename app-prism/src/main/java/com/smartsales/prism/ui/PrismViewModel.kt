package com.smartsales.prism.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.core.Mode
import com.smartsales.prism.domain.core.Orchestrator
import com.smartsales.prism.domain.core.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
        
        _isSending.value = true
        _inputText.value = ""
        _uiState.value = UiState.Loading
        
        viewModelScope.launch {
            try {
                val result = orchestrator.processInput(input)
                _uiState.value = result
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
