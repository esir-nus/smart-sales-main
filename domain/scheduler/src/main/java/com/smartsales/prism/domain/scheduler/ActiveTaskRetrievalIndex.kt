package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.memory.TargetResolutionRequest

/**
 * 活跃任务检索索引。
 * 说明：基于调度器持久任务真相构建全局 follow-up 检索候选，不拥有写入权。
 */
interface ActiveTaskRetrievalIndex {

    /**
     * 基于当前输入构建活跃任务短名单，用于全局改期提取 Prompt。
     */
    suspend fun buildShortlist(
        transcript: String,
        limit: Int = 8
    ): List<ActiveTaskContext>

    /**
     * 对模型建议的目标进行索引侧复核。
     * 只有建议任务与调度器索引证据一致时才可返回 Resolved。
     */
    suspend fun resolveTarget(
        target: TargetResolutionRequest,
        suggestedTaskId: String? = null
    ): ActiveTaskResolveResult

    /**
     * 按输入里的钟点锚点解析唯一活跃任务；用于全局改期里的时间锚点改名。
     */
    suspend fun resolveTargetByClockAnchor(
        clockCue: String,
        nowIso: String,
        timezone: String,
        displayedDateIso: String? = null
    ): ActiveTaskResolveResult
}

data class ActiveTaskContext(
    val taskId: String,
    val title: String,
    val timeSummary: String,
    val isVague: Boolean,
    val keyPerson: String? = null,
    val location: String? = null,
    val notesDigest: String? = null
)

sealed interface ActiveTaskResolveResult {
    data class Resolved(val taskId: String) : ActiveTaskResolveResult
    data class Ambiguous(
        val query: String,
        val candidateIds: List<String> = emptyList()
    ) : ActiveTaskResolveResult
    data class NoMatch(val query: String) : ActiveTaskResolveResult
}
