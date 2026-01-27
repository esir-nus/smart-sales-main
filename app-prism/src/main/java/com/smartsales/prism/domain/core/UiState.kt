package com.smartsales.prism.domain.core

/**
 * Pipeline UI 状态密封类
 * @see Prism-V1.md §2.2
 */
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Thinking(val hint: String? = null) : UiState()
    data class Streaming(val partialContent: String) : UiState()
    data class Response(val content: String) : UiState()
    data class Error(val message: String, val retryable: Boolean = true) : UiState()
}
