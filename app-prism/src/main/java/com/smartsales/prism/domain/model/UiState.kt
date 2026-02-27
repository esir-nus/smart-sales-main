package com.smartsales.prism.domain.model

import com.smartsales.prism.domain.pipeline.ExecutionPlan

/**
 * Pipeline UI 状态密封类
 * @see Prism-V1.md §2.2
 */
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Thinking(val hint: String? = null) : UiState()
    data class Streaming(val partialContent: String) : UiState()
    data class Response(val content: String, val structuredJson: String? = null, val suggestAnalyst: Boolean = false) : UiState()
    data class SchedulerTaskCreated(
        val taskId: String,           // 用于冲突检测排除自身
        val title: String,
        val dayOffset: Int,
        val scheduledAtMillis: Long,  // 用于冲突检测
        val durationMinutes: Int,     // 用于冲突检测
        val isReschedule: Boolean = false  // Wave 11: 改期标记（amber glow 信号）
    ) : UiState()

    /**
     * 多任务创建结果 — 传递完整的任务列表，解决 Head-Body 数据丢失
     */
    data class SchedulerMultiTaskCreated(
        val tasks: List<SchedulerTaskCreated>,
        val hasConflict: Boolean = false
    ) : UiState()
    
    data class PlanCard(val plan: ExecutionPlan, val completedSteps: Set<Int> = emptySet()) : UiState()
    
    /**
     * 轻量级反馈 — Toast 消息，不进入聊天历史
     */
    data class Toast(val message: String) : UiState()
    
    data class Error(val message: String, val retryable: Boolean = true) : UiState()
    
    /**
     * 等待用户澄清 — Phase 1 循环 / Phase 2 实体消歧
     * @param question 向用户展示的问题
     * @param clarificationType 澄清类型
     * @param candidates 候选选项 (用于实体消歧)
     */
    data class AwaitingClarification(
        val question: String,
        val clarificationType: ClarificationType,
        val candidates: List<CandidateOption> = emptyList()
    ) : UiState()

    // Analyst Mode V2 State
    data class MarkdownStrategyState(val markdownContent: String) : UiState()
}

/**
 * 澄清类型
 */
enum class ClarificationType {
    MISSING_TIME,       // Phase 1: 缺少时间
    MISSING_DURATION,   // Phase 1: 缺少时长
    AMBIGUOUS_PERSON,   // Phase 2: 多个人物候选
    AMBIGUOUS_LOCATION, // Phase 2: 多个地点候选
    NON_SCHEDULING_INTENT  // Wave 4.0: 非日程意图
}

/**
 * 候选选项 (用于实体消歧 UI)
 */
data class CandidateOption(
    val entityId: String,
    val displayName: String,
    val description: String? = null
)

