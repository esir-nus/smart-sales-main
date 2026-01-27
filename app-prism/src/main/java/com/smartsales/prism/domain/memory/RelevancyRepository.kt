package com.smartsales.prism.domain.memory

/**
 * 关联性仓库 — O(1) 实体查询
 * @see Prism-V1.md §5.2
 */
interface RelevancyRepository {
    /**
     * 按 ID 获取实体
     */
    suspend fun getById(entityId: String): RelevancyEntry?
    
    /**
     * 按别名查询（用于消歧）
     */
    suspend fun findByAlias(alias: String): List<RelevancyEntry>
    
    /**
     * 按类型获取所有实体
     */
    suspend fun getByType(entityType: EntityType): List<RelevancyEntry>
    
    /**
     * 保存/更新实体
     */
    suspend fun save(entry: RelevancyEntry)
    
    /**
     * 搜索实体
     */
    suspend fun search(query: String, limit: Int = 10): List<RelevancyEntry>
}
