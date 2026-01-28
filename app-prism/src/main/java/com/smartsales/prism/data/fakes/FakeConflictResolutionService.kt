package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.scheduler.ConflictResolutionService
import com.smartsales.prism.domain.scheduler.ResolutionResult
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Conflict Resolution Service — 骨架开发
 * AI思考延迟在这里，不在UI层
 */
@Singleton
class FakeConflictResolutionService @Inject constructor() : ConflictResolutionService {
    
    override suspend fun resolveConflict(userMessage: String): ResolutionResult {
        delay(1000) // AI思考模拟
        delay(1500) // 阅读时间
        return ResolutionResult(
            reply = "好的，已更新。",
            resolved = true
        )
    }
}
