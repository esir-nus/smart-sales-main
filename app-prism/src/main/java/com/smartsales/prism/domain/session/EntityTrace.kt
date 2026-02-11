package com.smartsales.prism.domain.session

/**
 * 会话内实体追踪记录
 *
 * 跟踪单个实体在当前会话中的状态和置信度。
 * 由 SessionWorkingSet 内部管理，不对外暴露。
 * 无超时 — 实体在会话期间保持 ACTIVE。
 */
data class EntityTrace(
    val entityId: String,
    val state: EntityState,
    val confidence: Float
)
