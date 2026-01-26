package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake ModePublisher — 管理本地 UI 状态，用于 UI 开发
 */
class FakeModePublisher : ModePublisher {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    override val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    override suspend fun publish(result: ExecutorResult) {
        _uiState.value = UiState.Response(
            content = result.displayContent,
            structuredJson = result.structuredJson
        )
    }
    
    /**
     * 测试辅助方法：手动设置状态
     */
    fun setState(state: UiState) {
        _uiState.value = state
    }
}
