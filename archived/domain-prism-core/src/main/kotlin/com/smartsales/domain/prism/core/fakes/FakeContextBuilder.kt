package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.*

/**
 * Fake ContextBuilder — 返回基础上下文，用于 UI 开发
 */
class FakeContextBuilder : ContextBuilder {
    
    override suspend fun buildContext(userText: String, mode: Mode): EnhancedContext {
        return EnhancedContext(
            userText = userText,
            audioTranscripts = emptyList(),
            imageAnalysis = emptyList(),
            memoryHits = listOf(
                MemoryHit(
                    entryId = "fake-001",
                    snippet = "模拟的历史记录片段",
                    score = 0.85f
                )
            ),
            entityContext = mapOf(
                "z-001" to EntityRef("z-001", "张总")
            ),
            userProfile = UserProfileSnapshot(
                displayName = "测试用户",
                preferredLanguage = "zh-CN"
            ),
            userHabits = listOf(
                UserHabitSnapshot("meeting_time", "morning", 0.8f)
            ),
            sessionCacheSnapshot = null,
            mode = mode
        )
    }
}
