package com.smartsales.core.test.fakes

import com.smartsales.core.context.ChatTurn
import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.context.ModeMetadata
import com.smartsales.prism.domain.model.Mode

class FakeContextBuilder : ContextBuilder {
    
    var buildResult: EnhancedContext = EnhancedContext(
        userText = "",
        modeMetadata = ModeMetadata(Mode.ANALYST, "fake-session", 1),
        sessionHistory = emptyList(),
        currentDate = "2026-03-09",
        currentInstant = 0L,
        executedTools = emptySet()
    )

    private val history = mutableListOf<ChatTurn>()
    private var activeSessionId = "fake-session"

    override suspend fun build(
        userText: String,
        mode: Mode,
        resolvedEntityIds: List<String>,
        depth: ContextDepth
    ): EnhancedContext {
        return buildResult.copy(userText = userText, modeMetadata = buildResult.modeMetadata.copy(currentMode = mode))
    }

    override fun getSessionHistory(): List<ChatTurn> = history

    override suspend fun recordUserMessage(content: String) {
        history.add(ChatTurn("user", content))
    }

    override suspend fun recordAssistantMessage(content: String) {
        history.add(ChatTurn("assistant", content))
    }

    override fun resetSession() {
        history.clear()
        activeSessionId = "fake-session-reset"
    }

    override fun getActiveSessionId(): String = activeSessionId

    override fun loadSession(sessionId: String, history: List<ChatTurn>) {
        this.activeSessionId = sessionId
        this.history.clear()
        this.history.addAll(history)
    }

    override fun loadDocumentContext(payload: String) {
        // No-op for now unless explicitly needed in tests
    }
}
