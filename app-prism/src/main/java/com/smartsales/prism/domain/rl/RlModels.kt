package com.smartsales.prism.domain.rl

import com.smartsales.prism.domain.habit.UserHabit

/**
 * RL 观察记录 — 从 LLM 结构化输出中提取的学习信号
 * 
 * 来源: docs/cerb/rl-module/spec.md L58-65
 */
data class RlObservation(
    val entityId: String?,          // null = 全局用户习惯, 否则为客户/实体 ID
    val key: String,                // 习惯键 (e.g., "preferred_meeting_time")
    val value: String,              // 观察值 (e.g., "morning")
    val source: ObservationSource,  // USER_INPUT 或 INFERRED
    val evidence: String?           // 原始文本 (调试用)
)

/**
 * 观察来源 — 影响习惯权重计算
 * 
 * Wave 1.5: 扩展为 3 值以支持正负反馈
 */
enum class ObservationSource {
    INFERRED,       // LLM 从上下文推断 (权重: 1x)
    USER_POSITIVE,  // 用户明确确认 (权重: 3x)
    USER_NEGATIVE   // 用户明确拒绝 (权重: -2x)
}

/**
 * 习惯上下文 — 用于 Context Builder 注入到 LLM 提示
 * 
 * 来源: docs/cerb/rl-module/spec.md L75-80
 */
data class HabitContext(
    val userHabits: List<UserHabit>,      // 全局偏好
    val clientHabits: List<UserHabit>,    // 特定实体偏好
    val suggestedDefaults: Map<String, String>  // Wave 3: 智能默认值
)
