package com.smartsales.feature.chat.home.orchestrator

import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Fake implementation for testing. Supports stubbing responses.
 */
class FakeHomeOrchestrator : HomeOrchestrator {
    var stubResponse: String = "Fake orchestrator response"
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
        stubResponse = "Fake orchestrator response"
        stubError = null
        streamChatCalls.clear()
    }
}
