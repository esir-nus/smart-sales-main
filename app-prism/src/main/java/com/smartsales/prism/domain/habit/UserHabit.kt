package com.smartsales.prism.domain.habit

/**
 * # 用户习惯领域模型
 *
 * 表示用户的行为偏好模式,用于个性化建议和智能默认值。
 *
 * ## 字段说明
 * - `habitKey`: 习惯类别 (e.g., "preferred_meeting_time")
 * - `habitValue`: 偏好值 (e.g., "morning")
 * - `entityId`: 特定实体的习惯 (null = 全局习惯)
 * - `isExplicit`: true = 用户明确设置, false = 系统推断
 * - `confidence`: 置信度 (0.0-1.0), 计算公式: obs / (obs + rej)
 * - `observationCount`: 观察次数
 * - `rejectionCount`: 拒绝次数 (用户覆盖建议)
 * - `lastObservedAt`: 最后观察时间戳
 * - `createdAt`: 创建时间戳
 *
 * ## 来源
 * Spec: docs/cerb/user-habit/spec.md L16-26
 */
data class UserHabit(
    val habitKey: String,
    val habitValue: String,
    val entityId: String?,
    val isExplicit: Boolean,
    val confidence: Float,
    val observationCount: Int,
    val rejectionCount: Int,
    val lastObservedAt: Long,
    val createdAt: Long
)
