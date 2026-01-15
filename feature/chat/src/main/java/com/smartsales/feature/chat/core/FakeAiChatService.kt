package com.smartsales.feature.chat.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Fake implementation for testing. Supports stubbing responses.
 */
class FakeAiChatService : AiChatService {
    var stubResponse: String = "Fake response"
    var stubError: Throwable? = null
    val streamChatCalls = mutableListOf<ChatRequest>()
    
    override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flow {
        streamChatCalls.add(request)
        stubError?.let {
            emit(ChatStreamEvent.Error(it))
            return@flow
        }
        emit(ChatStreamEvent.Delta(stubResponse))
        emit(ChatStreamEvent.Completed(stubResponse))
    }
    
    fun reset() {
        stubResponse = "Fake response"
        stubError = null
        streamChatCalls.clear()
    }
}
