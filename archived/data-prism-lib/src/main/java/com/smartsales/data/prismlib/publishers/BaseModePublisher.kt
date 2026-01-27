package com.smartsales.data.prismlib.publishers

import com.smartsales.domain.prism.core.ModePublisher
import com.smartsales.domain.prism.core.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ModePublisher 基类 — 提供模拟流式输出的共享逻辑
 * @see Prism-V1.md §3.1 Buffered Streaming Flow
 */
abstract class BaseModePublisher : ModePublisher {

    protected val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    override val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * 模拟流式输出动画
     * 按块逐步发送内容，模拟打字效果
     * 
     * @param content 完整内容
     * @param chunkSize 每次发送的字符数（默认 5）
     * @param delayMs 每块之间的延迟（默认 20ms）
     */
    protected suspend fun simulateStreaming(
        content: String,
        chunkSize: Int = CHUNK_SIZE,
        delayMs: Long = DELAY_MS
    ) {
        val builder = StringBuilder()
        var index = 0
        
        while (index < content.length) {
            val end = minOf(index + chunkSize, content.length)
            builder.append(content.substring(index, end))
            _uiState.value = UiState.Streaming(builder.toString())
            delay(delayMs)
            index = end
        }
    }

    /**
     * 设置 Thinking 状态
     */
    protected fun setThinking(hint: String? = null) {
        _uiState.value = UiState.Thinking(hint)
    }

    /**
     * 设置 Loading 状态
     */
    protected fun setLoading() {
        _uiState.value = UiState.Loading
    }

    /**
     * 设置 Idle 状态
     */
    protected fun setIdle() {
        _uiState.value = UiState.Idle
    }

    /**
     * 设置 Error 状态
     */
    protected fun setError(message: String, retryable: Boolean = true) {
        _uiState.value = UiState.Error(message, retryable)
    }

    companion object {
        private const val CHUNK_SIZE = 5       // 每次发送字符数
        private const val DELAY_MS = 20L       // 延迟毫秒
    }
}
