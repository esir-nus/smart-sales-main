package com.smartsales.prism.domain.crm

import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.rl.HabitContext

/**
 * 实体快照 — 轻量级摘要
 */
data class EntitySnapshot(
    val entityId: String,
    val displayName: String,
    val entityType: EntityType,
    val lastActivity: com.smartsales.prism.domain.memory.MemoryEntry?
)

/**
 * 聚焦上下文 — 单个实体深度分析
 */
data class FocusedContext(
    val entity: EntityEntry,
    val relatedContacts: List<EntityEntry>,
    val relatedDeals: List<EntityEntry>,
    val activityState: ProfileActivityState,
    val habitContext: HabitContext
)

/**
 * 时间轴与待办聚合状态
 */
data class ProfileActivityState(
    val actionableItems: List<com.smartsales.prism.domain.scheduler.TimelineItemModel.Task>,
    val factualItems: List<com.smartsales.prism.domain.memory.MemoryEntry>
)

/**
 * 快速上下文 — "6秒速览"
 */
data class QuickContext(
    val entitySnapshots: Map<String, EntitySnapshot>,
    val recentActivities: List<com.smartsales.prism.domain.memory.MemoryEntry>,
    val suggestedNextSteps: List<String>
)


