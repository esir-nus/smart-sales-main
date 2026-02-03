package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.RelevancyEntry
import com.smartsales.prism.domain.memory.RelevancyRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake RelevancyRepository — 内存中存储
 * Phase 2 占位实现
 */
@Singleton
class FakeRelevancyRepository @Inject constructor() : RelevancyRepository {
    
    private val entries = mutableMapOf<String, RelevancyEntry>()
    
    init {
        // 测试数据: 消歧场景
        val now = System.currentTimeMillis()
        
        // "张总" → 1 match (Auto-resolve scenario)
        entries["z-001"] = RelevancyEntry(
            entityId = "z-001",
            entityType = EntityType.PERSON,
            displayName = "张伟",
            aliasesJson = """["张总", "老张", "张伟"]""",
            lastUpdatedAt = now,
            createdAt = now
        )
        
        // "王总" → 3 matches (Picker scenario)
        entries["w-001"] = RelevancyEntry(
            entityId = "w-001",
            entityType = EntityType.PERSON,
            displayName = "王明",
            aliasesJson = """["王总", "王明"]""",
            lastUpdatedAt = now,
            createdAt = now
        )
        entries["w-002"] = RelevancyEntry(
            entityId = "w-002",
            entityType = EntityType.PERSON,
            displayName = "王华",
            aliasesJson = """["王总", "王华"]""",
            lastUpdatedAt = now,
            createdAt = now
        )
        entries["w-003"] = RelevancyEntry(
            entityId = "w-003",
            entityType = EntityType.PERSON,
            displayName = "王军",
            aliasesJson = """["王总", "王军"]""",
            lastUpdatedAt = now,
            createdAt = now
        )
        
        // "李总" → 0 matches (NotFound scenario) — no entry
    }
    
    override suspend fun getById(entityId: String): RelevancyEntry? {
        return entries[entityId]
    }
    
    override suspend fun findByAlias(alias: String): List<RelevancyEntry> {
        return entries.values.filter { entry ->
            entry.aliasesJson.contains(alias, ignoreCase = true)
        }
    }
    
    override suspend fun getByType(entityType: EntityType): List<RelevancyEntry> {
        return entries.values.filter { it.entityType == entityType }
    }
    
    override suspend fun save(entry: RelevancyEntry) {
        entries[entry.entityId] = entry
    }
    
    override suspend fun search(query: String, limit: Int): List<RelevancyEntry> {
        return entries.values
            .filter { 
                it.displayName.contains(query, ignoreCase = true) ||
                it.aliasesJson.contains(query, ignoreCase = true)
            }
            .take(limit)
    }
}
