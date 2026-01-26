package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake Orchestrator — 返回硬编码结果，用于 UI 开发
 */
class FakeOrchestrator : Orchestrator {
    
    private val _currentMode = MutableStateFlow(Mode.COACH)
    override val currentMode: StateFlow<Mode> = _currentMode.asStateFlow()
    
    override suspend fun processUserIntent(input: String): OrchestratorResult {
        val executorResult = ExecutorResult(
            displayContent = "这是对「$input」的模拟回复。当前模式：${_currentMode.value}",
            structuredJson = null,
            toolResults = emptyList(),
            usage = TokenUsage(promptTokens = 100, completionTokens = 50)
        )
        
        return OrchestratorResult(
            mode = _currentMode.value,
            executorResult = executorResult,
            memoryWriteTriggered = false,
            uiState = UiState.Response(executorResult.displayContent, null)
        )
    }
    
    override suspend fun switchMode(newMode: Mode) {
        _currentMode.value = newMode
    }
}
