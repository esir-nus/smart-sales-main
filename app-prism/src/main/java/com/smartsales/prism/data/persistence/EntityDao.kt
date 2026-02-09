package com.smartsales.prism.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Entity DAO — 实体注册表 CRUD
 */
@Dao
interface EntityDao {
    /**
     * 按 ID 获取实体
     */
    @Query("SELECT * FROM entity_entries WHERE entityId = :entityId LIMIT 1")
    suspend fun getById(entityId: String): EntityEntryEntity?
    
    /**
     * 按别名查询 (LIKE 匹配 aliasesJson)
     */
    @Query("SELECT * FROM entity_entries WHERE aliasesJson LIKE '%\"' || :alias || '\"%'")
    suspend fun findByAlias(alias: String): List<EntityEntryEntity>
    
    /**
     * 按类型获取所有实体
     */
    @Query("SELECT * FROM entity_entries WHERE entityType = :entityType ORDER BY displayName")
    suspend fun getByType(entityType: String): List<EntityEntryEntity>
    
    /**
     * 插入/更新实体
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntityEntryEntity)
    
    /**
     * 搜索实体 (displayName + aliasesJson)
     */
    @Query("""
        SELECT * FROM entity_entries 
        WHERE displayName LIKE '%' || :query || '%' 
           OR aliasesJson LIKE '%' || :query || '%'
        ORDER BY displayName
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int): List<EntityEntryEntity>
    
    /**
     * 按账户 ID 获取关联实体 (contacts + deals)
     */
    @Query("SELECT * FROM entity_entries WHERE accountId = :accountId ORDER BY entityType, displayName")
    suspend fun getByAccountId(accountId: String): List<EntityEntryEntity>
    
    /**
     * 按 ID 删除实体
     */
    @Query("DELETE FROM entity_entries WHERE entityId = :entityId")
    suspend fun delete(entityId: String)
    
    /**
     * Test helper: 清空所有数据
     */
    @Query("DELETE FROM entity_entries")
    suspend fun deleteAll()
}
