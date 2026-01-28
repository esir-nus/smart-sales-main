package com.smartsales.prism.domain.scheduler

/**
 * Conflict Resolution Service — AI冲突解决
 * Fake I/O pattern: 延迟在Fake实现中，不在UI
 */
interface ConflictResolutionService {
    /**
     * 解决日程冲突
     */
    suspend fun resolveConflict(userMessage: String): ResolutionResult
}

/**
 * 解决结果
 */
data class ResolutionResult(
    val reply: String,
    val resolved: Boolean
)
