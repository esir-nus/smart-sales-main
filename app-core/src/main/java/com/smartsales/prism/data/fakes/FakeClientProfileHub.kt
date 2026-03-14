package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.crm.ClientProfileHub
import com.smartsales.prism.domain.crm.EntitySnapshot
import com.smartsales.prism.domain.crm.FocusedContext
import com.smartsales.prism.domain.crm.QuickContext
import com.smartsales.prism.domain.crm.ProfileActivityState
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
            activityState = ProfileActivityState(emptyList(), emptyList()),
            habitContext = HabitContext(
                userHabits = emptyList(),
                clientHabits = emptyList(),
                suggestedDefaults = emptyMap()
            )
        )
    }
    
    override suspend fun observeProfileActivityState(entityId: String): kotlinx.coroutines.flow.Flow<ProfileActivityState> {
        return kotlinx.coroutines.flow.flowOf(ProfileActivityState(emptyList(), emptyList()))
    }
}
