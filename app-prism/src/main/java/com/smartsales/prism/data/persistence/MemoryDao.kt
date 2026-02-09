package com.smartsales.prism.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Memory DAO — 记忆条目 CRUD
 */
@Dao
interface MemoryDao {
    /**
     * 获取活跃区条目 (仅非归档)
     */
    @Query("SELECT * FROM memory_entries WHERE sessionId = :sessionId AND isArchived = 0 ORDER BY createdAt DESC")
    suspend fun getActiveEntries(sessionId: String): List<MemoryEntryEntity>
    
    /**
     * 获取活跃区条目 (分层: 非归档 OR 在窗口内)
     * @param cutoffMs 窗口截止时间戳 (now - windowDays * 86400000)
     */
    @Query("""
        SELECT * FROM memory_entries 
        WHERE sessionId = :sessionId 
          AND (isArchived = 0 OR (scheduledAt IS NOT NULL AND scheduledAt > :cutoffMs))
        ORDER BY createdAt DESC
    """)
    suspend fun getActiveEntriesWithWindow(sessionId: String, cutoffMs: Long): List<MemoryEntryEntity>
    
    /**
     * 获取归档区条目 (已归档且超出窗口)
     */
    @Query("""
        SELECT * FROM memory_entries 
        WHERE sessionId = :sessionId 
          AND isArchived = 1 
          AND (scheduledAt IS NULL OR scheduledAt <= :cutoffMs)
        ORDER BY createdAt DESC
    """)
    suspend fun getArchivedEntries(sessionId: String, cutoffMs: Long): List<MemoryEntryEntity>
    
    /**
     * 搜索记忆 (LIKE 模糊匹配)
     * Tech Debt: 中文搜索质量低于分词，考虑 FTS4
     */
    @Query("SELECT * FROM memory_entries WHERE content LIKE '%' || :query || '%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun search(query: String, limit: Int): List<MemoryEntryEntity>
    
    /**
     * 观察活跃区变化
     */
    @Query("SELECT * FROM memory_entries WHERE sessionId = :sessionId AND isArchived = 0 ORDER BY createdAt DESC")
    fun observeActiveEntries(sessionId: String): Flow<List<MemoryEntryEntity>>
    
    /**
     * 插入/更新条目 (REPLACE on conflict)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MemoryEntryEntity)
    
    /**
     * 标记为已归档
     */
    @Query("UPDATE memory_entries SET isArchived = 1 WHERE entryId = :entryId")
    suspend fun markAsArchived(entryId: String)
    
    /**
     * 按实体 ID 查询 (引号包裹避免子串误匹配)
     */
    @Query("SELECT * FROM memory_entries WHERE structuredJson LIKE '%\"' || :entityId || '\"%' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getByEntityId(entityId: String, limit: Int): List<MemoryEntryEntity>
    
    /**
     * Test helper: 清空所有数据
     */
    @Query("DELETE FROM memory_entries")
    suspend fun deleteAll()
}
