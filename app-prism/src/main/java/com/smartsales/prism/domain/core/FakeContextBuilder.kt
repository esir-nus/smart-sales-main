package com.smartsales.prism.domain.core

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake ContextBuilder — 返回最小化上下文
 * Phase 1 占位实现
 */
@Singleton
class FakeContextBuilder @Inject constructor() : ContextBuilder {
    
    override suspend fun build(userText: String, mode: Mode): EnhancedContext {
        return EnhancedContext(
            userText = userText,
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = "fake-session-001",
                turnIndex = 1
            )
        )
    }
}
