package com.smartsales.prism.domain.memory

import kotlinx.coroutines.flow.StateFlow

/**
 * 日程看板 — 冲突检测的快速索引
 * 
 * 专用于时间冲突检测，使用硬编码 Kotlin 逻辑，无需 LLM。
 * 这是 Memory Center 的第一个专用索引。
 * 
 * @see docs/cerb/memory-center/spec.md §ScheduleBoard
 */
interface ScheduleBoard {
    /**
     * 即将到来的日程项目 (响应式，始终最新)
     */
    val upcomingItems: StateFlow<List<ScheduleItem>>
    
    /**
     * 时间冲突检测 — 硬编码 Kotlin，无需 LLM，即时返回
     * 
     * @param proposedStart 提议的开始时间 (epoch millis)
     * @param durationMinutes 持续时间 (分钟)
     * @param excludeId 排除的任务ID (用于避免新创建的任务与自己冲突)
     * @return 冲突检测结果
     */
    suspend fun checkConflict(
        proposedStart: Long,
        durationMinutes: Int,
        excludeId: String? = null
    ): ConflictResult
    
    /**
     * 强制从 ScheduledTaskRepository 刷新
     */
    suspend fun refresh()
    
    /**
     * Path A (Wave 17): Lexical fuzzy match against upcoming items.
     * Evaluates strictly for 1 exact match (anti-hallucination).
     * @return The exactly matched ScheduleItem, or null if 0 or 2+ matches.
     */
    suspend fun findLexicalMatch(targetQuery: String): ScheduleItem? =
        when (val result = resolveTarget(TargetResolutionRequest(targetQuery = targetQuery))) {
            is TargetResolution.Resolved -> result.item
            else -> null
        }

    /**
     * Scheduler-owned target resolution.
     *
     * 用于改期/删除等需要定位既有任务的场景。
     * 允许实现使用标题、参与人、地点以及当前日期上下文做置信度判定，
     * 但低置信度或近似并列候选必须显式返回非 Resolved，避免误改错误任务。
     */
    suspend fun resolveTarget(request: TargetResolutionRequest): TargetResolution =
        findLexicalMatch(request.targetQuery)?.let(TargetResolution::Resolved)
            ?: TargetResolution.NoMatch(request.describeForFailure())
}

data class TargetResolutionRequest(
    val targetQuery: String = "",
    val targetPerson: String? = null,
    val targetLocation: String? = null,
    val preferredTaskIds: Set<String> = emptySet()
) {
    fun describeForFailure(): String {
        return listOfNotNull(
            targetQuery.takeIf { it.isNotBlank() },
            targetPerson?.takeIf { it.isNotBlank() },
            targetLocation?.takeIf { it.isNotBlank() }
        ).joinToString(" ").ifBlank { "<empty>" }
    }
}

sealed interface TargetResolution {
    data class Resolved(val item: ScheduleItem) : TargetResolution
    data class Ambiguous(
        val query: String,
        val candidateIds: List<String> = emptyList()
    ) : TargetResolution
    data class NoMatch(val query: String) : TargetResolution
}

/**
 * 冲突检测结果
 */
sealed class ConflictResult {
    /** 无冲突 — 时间段空闲 */
    data object Clear : ConflictResult()
    
    /** 检测到冲突 — 返回重叠的日程项 */
    data class Conflict(val overlaps: List<ScheduleItem>) : ConflictResult()
}
