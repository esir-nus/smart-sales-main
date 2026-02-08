package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.crm.ActivityType
import com.smartsales.prism.domain.crm.ClientProfileHub
import com.smartsales.prism.domain.crm.EntitySnapshot
import com.smartsales.prism.domain.crm.FocusedContext
import com.smartsales.prism.domain.crm.QuickContext
import com.smartsales.prism.domain.crm.UnifiedActivity
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.rl.HabitContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake ClientProfileHub — 内存中存储
 * Wave 2: 支持 MemoryRepository 驱动的时间线聚合
 */
@Singleton
class FakeClientProfileHub @Inject constructor(
    private val entityRepository: EntityRepository,
    private val memoryRepository: MemoryRepository
) : ClientProfileHub {
    
    override suspend fun getQuickContext(entityIds: List<String>): QuickContext {
        val snapshots = entityIds.mapNotNull { id ->
            entityRepository.getById(id)?.let { entity ->
                id to EntitySnapshot(
                    entityId = entity.entityId,
                    displayName = entity.displayName,
                    entityType = entity.entityType,
                    lastActivity = null
                )
            }
        }.toMap()
        
        return QuickContext(
            entitySnapshots = snapshots,
            recentActivities = emptyList(),
            suggestedNextSteps = emptyList()
        )
    }
    
    override suspend fun getFocusedContext(entityId: String): FocusedContext {
        val entity = entityRepository.getById(entityId)
            ?: throw IllegalArgumentException("Entity not found: $entityId")
        
        val relatedContacts = if (entity.entityType.name == "ACCOUNT") {
            entityRepository.getByAccountId(entityId)
                .filter { it.entityType.name == "CONTACT" }
        } else {
            emptyList()
        }
        
        val relatedDeals = if (entity.entityType.name == "ACCOUNT") {
            entityRepository.getByAccountId(entityId)
                .filter { it.entityType.name == "DEAL" }
        } else {
            emptyList()
        }
        
        return FocusedContext(
            entity = entity,
            relatedContacts = relatedContacts,
            relatedDeals = relatedDeals,
            timeline = getUnifiedTimeline(entityId),
            habitContext = HabitContext(
                userHabits = emptyList(),
                clientHabits = emptyList(),
                suggestedDefaults = emptyMap()
            )
        )
    }
    
    override suspend fun getUnifiedTimeline(entityId: String): List<UnifiedActivity> {
        return memoryRepository.getByEntityId(entityId)
            .map { it.toUnifiedActivity() }
    }
}

/**
 * MemoryEntry → UnifiedActivity 类型映射
 */
private fun MemoryEntry.toUnifiedActivity(): UnifiedActivity {
    val entityIds = parseRelatedEntityIds(structuredJson)
    
    return UnifiedActivity(
        id = entryId,
        type = entryType.toActivityType(),
        timestamp = createdAt,
        summary = content,
        location = null,
        assetId = null,
        relatedEntityIds = entityIds
    )
}

/**
 * MemoryEntryType → ActivityType 枚举映射
 */
private fun MemoryEntryType.toActivityType(): ActivityType = when (this) {
    MemoryEntryType.SCHEDULE_ITEM -> ActivityType.MEETING
    MemoryEntryType.TASK_RECORD -> ActivityType.TASK_COMPLETED
    MemoryEntryType.INSPIRATION -> ActivityType.NOTE
    MemoryEntryType.USER_MESSAGE -> ActivityType.NOTE
    MemoryEntryType.ASSISTANT_RESPONSE -> ActivityType.NOTE
}

/**
 * 从 structuredJson 解析 relatedEntityIds
 */
private fun parseRelatedEntityIds(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val obj = JSONObject(json)
        val arr = obj.optJSONArray("relatedEntityIds") ?: return emptyList()
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) {
        emptyList()
    }
}
