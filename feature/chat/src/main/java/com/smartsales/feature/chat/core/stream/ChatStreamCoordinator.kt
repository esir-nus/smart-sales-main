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

class ChatStreamCoordinator(
    private val streamSource: (ChatRequest) -> Flow<ChatStreamEvent>,
) {
    fun start(
        scope: CoroutineScope,
        request: ChatRequest,
        onDelta: suspend (String) -> Unit,
        onCompleted: suspend (String) -> Unit,
        onError: suspend (Throwable) -> Unit,
    ) {
        // 通过函数注入流来源，便于测试与替换
        scope.launch {
            streamSource(request).collect { event ->
                when (event) {
                    is ChatStreamEvent.Delta -> onDelta(event.token)
                    is ChatStreamEvent.Completed -> onCompleted(event.fullText)
                    is ChatStreamEvent.Error -> onError(event.throwable)
                }
            }
        }
    }
}
