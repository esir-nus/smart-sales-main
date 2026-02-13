package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.ProfileChange
import com.smartsales.prism.domain.memory.ProfileUpdateResult
import com.smartsales.prism.domain.memory.UpsertResult
import java.util.UUID
import javax.inject.Inject

/**
 * Fake EntityWriter — 纯内存实现，用于消费端测试
 *
 * 不依赖 EntityRepository 或 ContextBuilder，
 * 直接在内存中操作实体数据。
 */
class FakeEntityWriter @Inject constructor() : EntityWriter {

    private val entities = mutableMapOf<String, EntityEntry>()

    /** 所有已记录的 ProfileChange，供测试断言 */
    val recordedChanges = mutableListOf<ProfileChange>()

    override suspend fun upsertFromClue(
        clue: String,
        resolvedId: String?,
        type: EntityType,
        source: String
    ): UpsertResult {
        require(clue.isNotBlank()) { "clue 不能为空白" }

        val existing = resolvedId?.let { entities[it] }
            ?: entities.values.firstOrNull { it.displayName.equals(clue, ignoreCase = true) }

        return if (existing != null) {
            val updated = existing.copy(
                displayName = clue,
                lastUpdatedAt = System.currentTimeMillis()
            )
            entities[updated.entityId] = updated
            UpsertResult(entityId = updated.entityId, isNew = false, displayName = clue)
        } else {
            val entityId = "fake-${UUID.randomUUID().toString().take(8)}"
            val entry = EntityEntry(
                entityId = entityId,
                entityType = type,
                displayName = clue,
                aliasesJson = "[]",
                attributesJson = "{}",
                lastUpdatedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            entities[entityId] = entry
            UpsertResult(entityId = entityId, isNew = true, displayName = clue)
        }
    }

    override suspend fun updateAttribute(entityId: String, key: String, value: String) {
        val existing = entities[entityId] ?: return
        entities[entityId] = existing.copy(lastUpdatedAt = System.currentTimeMillis())
    }

    override suspend fun registerAlias(entityId: String, alias: String) {
        // 静默操作
    }

    override suspend fun updateProfile(
        entityId: String,
        updates: Map<String, String?>
    ): ProfileUpdateResult {
        val existing = entities[entityId]
            ?: throw IllegalArgumentException("实体不存在: $entityId")

        val changes = updates.mapNotNull { (field, newValue) ->
            if (newValue != null) {
                ProfileChange(field = field, oldValue = existing.displayName, newValue = newValue)
            } else null
        }
        recordedChanges.addAll(changes)

        var updated = existing
        if (changes.any { it.field == "displayName" }) {
            val newName = changes.first { it.field == "displayName" }.newValue!!
            updated = updated.copy(displayName = newName)
        }
        if (changes.any { it.field == "nextAction" }) {
            val newAction = changes.first { it.field == "nextAction" }.newValue!!
            updated = updated.copy(nextAction = newAction)
        }
        entities[entityId] = updated

        return ProfileUpdateResult(entityId = entityId, changes = changes)
    }

    override suspend fun delete(entityId: String) {
        entities.remove(entityId)
    }

    // --- Test helpers ---

    fun getEntity(entityId: String): EntityEntry? = entities[entityId]
    fun getAllEntities(): Map<String, EntityEntry> = entities.toMap()
    fun clear() {
        entities.clear()
        recordedChanges.clear()
    }
}
