package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.ModeMetadata
import com.smartsales.prism.domain.rl.HabitContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake ContextBuilder — 返回最小化上下文
 * Phase 1 占位实现
 */
@Singleton
class FakeContextBuilder @Inject constructor() : ContextBuilder {
    
    private val _sessionHistory = mutableListOf<ChatTurn>()
    
    override suspend fun build(userText: String, mode: Mode, resolvedEntityIds: List<String>): EnhancedContext {
        return EnhancedContext(
            userText = userText,
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = "fake-session-001",
                turnIndex = 1
            ),
            sessionHistory = _sessionHistory.toList(),
            habitContext = HabitContext(
                userHabits = emptyList(),
                clientHabits = emptyList(),
                suggestedDefaults = emptyMap()
            )
        )
    }
    
    override fun getSessionHistory(): List<ChatTurn> = _sessionHistory.toList()
    
    override suspend fun recordUserMessage(content: String) {
        _sessionHistory.add(ChatTurn(role = "user", content = content))
    }
    
    override suspend fun recordAssistantMessage(content: String) {
        _sessionHistory.add(ChatTurn(role = "assistant", content = content))
    }

    override fun resetSession() {
        _sessionHistory.clear()
    }

    override fun getActiveSessionId(): String = "fake-session-001"

    override fun loadSession(sessionId: String, history: List<ChatTurn>) {
        _sessionHistory.clear()
        _sessionHistory.addAll(history)
    }

    override fun loadDocumentContext(payload: String) {
        // No-op for fake
    }
}
