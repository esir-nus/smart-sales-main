package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.config.SubscriptionConfig
import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake MemoryRepository — 内存中存储
 * 支持 Lazy Compaction: 活跃区/归档区通过查询时过滤实现
 */
@Singleton
class FakeMemoryRepository @Inject constructor() : MemoryRepository {
    
    private val entries = MutableStateFlow<List<MemoryEntry>>(emptyList())
    
    // 用于测试注入当前时间
    var currentTimeProvider: () -> Long = { System.currentTimeMillis() }
    
    override suspend fun getActiveEntries(sessionId: String): List<MemoryEntry> {
        return entries.value.filter { 
            !it.isArchived && it.sessionId == sessionId 
        }
    }
    
    /**
     * 获取活跃区条目（分层读取）
     * 逻辑: !isArchived OR scheduledAt > (now - windowDays)
     */
    override suspend fun getActiveEntries(sessionId: String, userTier: SubscriptionTier): List<MemoryEntry> {
        val windowDays = SubscriptionConfig.getHotWindowDays(userTier)
        val cutoff = currentTimeProvider() - (windowDays * 24 * 60 * 60 * 1000L)
        
        return entries.value.filter { entry ->
            entry.sessionId == sessionId && (
                !entry.isArchived || 
                (entry.scheduledAt != null && entry.scheduledAt > cutoff)
            )
        }
    }
    
    /**
     * 获取归档区条目（已归档且超出活跃区窗口）
     * 逻辑: isArchived AND scheduledAt <= (now - windowDays)
     */
    override suspend fun getArchivedEntries(sessionId: String, userTier: SubscriptionTier): List<MemoryEntry> {
        val windowDays = SubscriptionConfig.getHotWindowDays(userTier)
        val cutoff = currentTimeProvider() - (windowDays * 24 * 60 * 60 * 1000L)
        
        return entries.value.filter { entry ->
            entry.sessionId == sessionId &&
            entry.isArchived && 
            (entry.scheduledAt == null || entry.scheduledAt <= cutoff)
        }
    }
    
    override suspend fun search(query: String, limit: Int): List<MemoryEntry> {
        return entries.value
            .filter { it.content.contains(query, ignoreCase = true) }
            .take(limit)
    }
    
    override fun observeActiveEntries(sessionId: String): Flow<List<MemoryEntry>> {
        return entries.map { list ->
            list.filter { !it.isArchived && it.sessionId == sessionId }
        }
    }
    
    override suspend fun save(entry: MemoryEntry) {
        val current = entries.value.toMutableList()
        current.removeAll { it.entryId == entry.entryId }
        current.add(entry)
        entries.value = current
    }
    
    override suspend fun markAsArchived(entryId: String) {
        val current = entries.value.map { entry ->
            if (entry.entryId == entryId) entry.copy(isArchived = true)
            else entry
        }
        entries.value = current
    }
    
    // Test helper: clear all entries
    fun clear() {
        entries.value = emptyList()
    }
}
