package com.smartsales.domain.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Fake implementation for testing. Supports stubbing and call tracking.
 */
class FakeChatCoordinator : ChatCoordinator {
    private val _chatEvents = MutableSharedFlow<ChatEvent>()
    override val chatEvents: Flow<ChatEvent> = _chatEvents

    val sendMessageCalls = mutableListOf<SendMessageParams>()
    val sendSmartAnalysisCalls = mutableListOf<SmartAnalysisParams>()
    var resetStreamCalls = 0

    override fun sendMessage(params: SendMessageParams) {
        sendMessageCalls.add(params)
    }

    override fun sendSmartAnalysis(params: SmartAnalysisParams) {
        sendSmartAnalysisCalls.add(params)
    }

    override fun resetStream() {
        resetStreamCalls++
    }

    suspend fun emitEvent(event: ChatEvent) {
        _chatEvents.emit(event)
    }

    fun reset() {
        sendMessageCalls.clear()
        sendSmartAnalysisCalls.clear()
        resetStreamCalls = 0
    }
}
