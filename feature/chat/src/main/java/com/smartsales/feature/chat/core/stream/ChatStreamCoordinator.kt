package com.smartsales.feature.chat.core.stream

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/stream/ChatStreamCoordinator.kt
// Module: :feature:chat
// Summary: Streaming coordinator for chat event routing.
// Author: created on 2025-12-29

import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

sealed interface CompletionDecision {
    object Accept : CompletionDecision
    object Retry : CompletionDecision
    object Terminal : CompletionDecision
}

class ChatStreamCoordinator(
    private val streamSource: (ChatRequest) -> Flow<ChatStreamEvent>,
) {
    fun start(
        scope: CoroutineScope,
        request: ChatRequest,
        onDelta: suspend (String) -> Unit,
        onCompleted: suspend (String) -> Unit,
        onError: suspend (Throwable) -> Unit,
        maxRetries: Int = 0,
        completionEvaluator: (suspend (String, Int) -> CompletionDecision)? = null,
        requestProvider: ((Int) -> ChatRequest)? = null,
        onRetryStart: suspend (Int) -> Unit = { _ -> },
        onTerminal: suspend (String, Int) -> Unit = { _, _ -> },
    ) {
        // 通过函数注入流来源，便于测试与替换
        if (maxRetries <= 0 || completionEvaluator == null) {
            scope.launch {
                streamSource(request).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Delta -> onDelta(event.token)
                        is ChatStreamEvent.Completed -> onCompleted(event.fullText)
                        is ChatStreamEvent.Error -> onError(event.throwable)
                    }
                }
            }
            return
        }
        scope.launch {
            // 将重试框架收敛到协调器，避免 VM 处理多次流式与状态分支
            // maxRetries=2 表示最多重试 2 次，总共 3 次尝试（0/1/2）
            var attempt = 0
            while (true) {
                var completedText: String? = null
                var error: Throwable? = null
                // requestProvider 仅用于尝试级请求定制；未提供时保持原有请求与行为
                val attemptRequest = requestProvider?.invoke(attempt) ?: request
                streamSource(attemptRequest).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Delta -> onDelta(event.token)
                        is ChatStreamEvent.Completed -> completedText = event.fullText
                        is ChatStreamEvent.Error -> error = event.throwable
                    }
                }
                if (error != null) {
                    onError(error!!)
                    return@launch
                }
                val fullText = completedText ?: return@launch
                when (completionEvaluator(fullText, attempt)) {
                    CompletionDecision.Accept -> {
                        onCompleted(fullText)
                        return@launch
                    }
                    CompletionDecision.Retry -> {
                        if (attempt >= maxRetries) {
                            onTerminal(fullText, attempt)
                            return@launch
                        }
                        attempt += 1
                        onRetryStart(attempt)
                    }
                    CompletionDecision.Terminal -> {
                        onTerminal(fullText, attempt)
                        return@launch
                    }
                }
            }
        }
    }
}
