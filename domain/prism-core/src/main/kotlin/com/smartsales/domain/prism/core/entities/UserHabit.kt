package com.smartsales.domain.prism.core.entities

/**
 * 用户习惯 — 自动学习的行为模式
 * @see Prism-V1.md §5.9
 */
data class UserHabit(
    val habitKey: String,           // "meeting_time", "verbosity"
    val habitValue: String,         // "morning", "concise"
    val entityId: String? = null,   // null = 全局习惯
    val isExplicit: Boolean = false,
    val confidence: Float = 0.3f,
    val observationCount: Int = 0,
    val rejectionCount: Int = 0,
    val lastObservedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
