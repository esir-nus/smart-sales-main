package com.smartsales.prism.domain.memory

/**
 * 实体仓库 — O(1) 实体查询
 * @see Entity Registry interface
 */
interface EntityRepository {
    /**
     * 按 ID 获取实体
     */
    suspend fun getById(entityId: String): EntityEntry?
    
    /**
     * 按别名查询（用于消歧）
     */
    suspend fun findByAlias(alias: String): List<EntityEntry>
    
    /**
     * 按类型获取所有实体
     */
    suspend fun getByType(entityType: EntityType): List<EntityEntry>
    
    /**
     * 保存/更新实体
     */
    suspend fun save(entry: EntityEntry)
    
    /**
     * 搜索实体
     */
    suspend fun search(query: String, limit: Int = 10): List<EntityEntry>
    
    /**
     * 按账户ID获取关联实体（联系人和交易）
     */
    suspend fun getByAccountId(accountId: String): List<EntityEntry>
    
    /**
     * 删除实体
     */
    suspend fun delete(entityId: String)
}

// Backwards compatibility alias (deprecated, will be removed)
@Deprecated("Use EntityRepository instead", ReplaceWith("EntityRepository"))
typealias RelevancyRepository = EntityRepository
