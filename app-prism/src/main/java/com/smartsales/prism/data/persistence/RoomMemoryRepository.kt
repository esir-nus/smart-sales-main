package com.smartsales.prism.data.persistence

import android.util.Log
import com.smartsales.prism.domain.config.SubscriptionConfig
import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room 实现 — MemoryRepository
 * 
 * 无种子数据 (按 review 反馈)
 * Search 使用 LIKE (FTS4 技术债已记录)
 */
@Singleton
class RoomMemoryRepository @Inject constructor(
    private val db: PrismDatabase
) : MemoryRepository {
    
    private val dao = db.memoryDao()
    
    override suspend fun getActiveEntries(sessionId: String): List<MemoryEntry> {
        return dao.getActiveEntries(sessionId).map { it.toDomain() }
    }
    
    override suspend fun getActiveEntries(sessionId: String, userTier: SubscriptionTier): List<MemoryEntry> {
        val windowDays = SubscriptionConfig.getHotWindowDays(userTier)
        val cutoff = System.currentTimeMillis() - (windowDays * 24 * 60 * 60 * 1000L)
        return dao.getActiveEntriesWithWindow(sessionId, cutoff).map { it.toDomain() }
    }
    
    override suspend fun getArchivedEntries(sessionId: String, userTier: SubscriptionTier): List<MemoryEntry> {
        val windowDays = SubscriptionConfig.getHotWindowDays(userTier)
        val cutoff = System.currentTimeMillis() - (windowDays * 24 * 60 * 60 * 1000L)
        return dao.getArchivedEntries(sessionId, cutoff).map { it.toDomain() }
    }
    
    override suspend fun search(query: String, limit: Int): List<MemoryEntry> {
        return dao.search(query, limit).map { it.toDomain() }
    }
    
    override fun observeActiveEntries(sessionId: String): Flow<List<MemoryEntry>> {
        return dao.observeActiveEntries(sessionId).map { list -> list.map { it.toDomain() } }
    }
    
    override suspend fun save(entry: MemoryEntry) {
        dao.insert(entry.toEntity())
        Log.d("RoomMemory", "💾 保存: id=${entry.entryId} session=${entry.sessionId} type=${entry.entryType}")
    }
    
    override suspend fun markAsArchived(entryId: String) {
        dao.markAsArchived(entryId)
        Log.d("RoomMemory", "📦 归档: id=$entryId")
    }
    
    override suspend fun getByEntityId(entityId: String, limit: Int): List<MemoryEntry> {
        return dao.getByEntityId(entityId, limit).map { it.toDomain() }
    }
}
