package com.smartsales.prism.domain.habit

/**
 * # 用户习惯领域模型
 *
 * 表示用户的行为偏好模式,用于个性化建议和智能默认值。
 *
 * ## Wave 1.5: 4-Rule Weighting Model
 * - Rule 1: Frequency (inferredCount)
 * - Rule 2: Explicit Positive (explicitPositive, 3x weight)
 * - Rule 3: Explicit Negative (explicitNegative, -2x weight)
 * - Rule 4: Time Decay (lastObservedAt, half-life ~30 days)
 *
 * ## 字段说明
 * - `habitKey`: 习惯类别 (e.g., "preferred_meeting_time")
 * - `habitValue`: 偏好值 (e.g., "morning")
 * - `entityId`: 特定实体的习惯 (null = 全局习惯)
 * - `inferredCount`: LLM 推断次数 (权重: 1x)
 * - `explicitPositive`: 用户明确确认次数 (权重: 3x)
 * - `explicitNegative`: 用户明确拒绝次数 (权重: -2x)
 * - `lastObservedAt`: 最后观察时间戳 (用于时间衰减)
 * - `createdAt`: 创建时间戳
 *
 * **注意**: 置信度不再存储,而是在查询时计算 (考虑时间衰减)
 *
 * ## 来源
 * Spec: docs/cerb/user-habit/spec.md L16-32
 */
data class UserHabit(
    val habitKey: String,
    val habitValue: String,
    val entityId: String?,
    
    // Rule 1: Frequency (LLM inferred)
    val inferredCount: Int = 0,
    
    // Rule 2: Explicit positive
    val explicitPositive: Int = 0,
    
    // Rule 3: Explicit negative
    val explicitNegative: Int = 0,
    
    // Rule 4: Recency
    val lastObservedAt: Long,
    val createdAt: Long
)
