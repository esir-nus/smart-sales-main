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
    val lastActivity: UnifiedActivity?
)

/**
 * 聚焦上下文 — 单个实体深度分析
 */
data class FocusedContext(
    val entity: EntityEntry,
    val relatedContacts: List<EntityEntry>,
    val relatedDeals: List<EntityEntry>,
    val timeline: List<UnifiedActivity>,
    val habitContext: HabitContext
)

/**
 * 快速上下文 — "6秒速览"
 */
data class QuickContext(
    val entitySnapshots: Map<String, EntitySnapshot>,
    val recentActivities: List<UnifiedActivity>,
    val suggestedNextSteps: List<String>
)

/**
 * 统一活动条目 — 时间轴项
 */
data class UnifiedActivity(
    val id: String,
    val type: ActivityType,
    val timestamp: Long,
    val summary: String,
    val location: String?,
    val assetId: String?,
    val relatedEntityIds: List<String>
)

/**
 * 活动类型
 */
enum class ActivityType {
    MEETING,
    CALL,
    NOTE,
    ARTIFACT_GENERATED,
    DEAL_STAGE_CHANGE,
    TASK_COMPLETED,
    // Wave 2: 变更感知 Profile 跟踪
    NAME_CHANGE,
    TITLE_CHANGE,
    COMPANY_CHANGE,
    ROLE_CHANGE
}
