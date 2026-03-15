package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.config.SubscriptionConfig
import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
/**
 * Fake MemoryRepository — 内存中存储
 * 支持 Lazy Compaction: 活跃区/归档区通过查询时过滤实现
 */
@Singleton
class FakeMemoryRepository @Inject constructor() : MemoryRepository {
    
    private val mutex = Mutex()
    private val entries = MutableStateFlow<List<MemoryEntry>>(emptyList())
    
    // 用于测试注入当前时间
    var currentTimeProvider: () -> Long = { System.currentTimeMillis() }
    
    init {
        // NOTE: No hardcoded test data. Tests must seed their own data using TestFixtures.
        Log.d("CoachMemory", "🌱 FakeMemoryRepository initialized (Clean Blank Slate)")
    }
    
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
            val scheduledAt = entry.scheduledAt
            entry.sessionId == sessionId && (
                !entry.isArchived || 
                (scheduledAt != null && scheduledAt > cutoff)
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
            val scheduledAt = entry.scheduledAt
            entry.sessionId == sessionId &&
            entry.isArchived && 
            (scheduledAt == null || scheduledAt <= cutoff)
        }
    }
    
    /**
     * 关键词匹配搜索（模拟向量搜索的近似行为）
     * 将查询拆分为关键词，按匹配数量排序
     */
    override suspend fun search(query: String, limit: Int): List<MemoryEntry> {
        // 提取2字及以上的关键词（跳过单字虚词）
        val keywords = query.chunked(2).filter { it.length == 2 }
        Log.d("CoachMemory", "📚 MemoryRepository.search('$query') → keywords=${keywords.take(5)}")
        
        val scored = entries.value.mapNotNull { entry ->
            val matchCount = keywords.count { kw -> entry.content.contains(kw, ignoreCase = true) }
            if (matchCount > 0) entry to matchCount else null
        }.sortedByDescending { it.second }
        
        val results = scored.take(limit).map { it.first }
        Log.d("CoachMemory", "📚 MemoryRepository.search → ${results.size} hits: ${scored.take(limit).map { "${it.first.entryId}(score=${it.second})" }}")
        return results
    }
    
    override fun observeActiveEntries(sessionId: String): Flow<List<MemoryEntry>> {
        return entries.map { list ->
            list.filter { !it.isArchived && it.sessionId == sessionId }
        }
    }
    
    override suspend fun save(entry: MemoryEntry) {
        mutex.withLock {
            Log.d("CoachMemory", "💾 MemoryRepository.save(id=${entry.entryId}, type=${entry.entryType}, content='${entry.content.take(40)}...')")
            val current = entries.value.toMutableList()
            current.removeAll { it.entryId == entry.entryId }
            current.add(entry)
            entries.value = current
            Log.d("CoachMemory", "💾 MemoryRepository total entries: ${entries.value.size}")
        }
    }
    
    override suspend fun markAsArchived(entryId: String) {
        mutex.withLock {
            Log.d("CoachMemory", "📦 markAsArchived(id=$entryId)")
            val current = entries.value.map { entry ->
                if (entry.entryId == entryId) entry.copy(isArchived = true)
                else entry
            }
            entries.value = current
        }
    }
    
    // Test helper: clear all entries
    fun clear() {
        entries.value = emptyList()
    }
    
    override suspend fun getByEntityId(entityId: String, limit: Int): List<MemoryEntry> {
        val quoted = "\"$entityId\""
        val results = entries.value
            .filter { it.structuredJson?.contains(quoted) == true }
            .sortedByDescending { it.createdAt }
            .take(limit)
        Log.d("CoachMemory", "🔗 getByEntityId('$entityId') → ${results.size} hits")
        return results
    }
    
    override fun observeByEntityId(entityId: String): Flow<List<MemoryEntry>> {
        val quoted = "\"$entityId\""
        return entries.map { list ->
            list.filter { it.structuredJson?.contains(quoted) == true }
                .sortedByDescending { it.createdAt }
        }
    }
    
    override fun observeByTypeAndDateRange(entryType: MemoryEntryType, startMs: Long, endMs: Long): Flow<List<MemoryEntry>> {
        return entries.map { list ->
            list.filter { 
                val scheduledAt = it.scheduledAt
                it.entryType == entryType && 
                scheduledAt != null && 
                scheduledAt >= startMs && 
                scheduledAt < endMs 
            }.sortedBy { it.scheduledAt }
        }
    }
    
    // Test helper: get all entries
    fun getAll(limit: Int): List<MemoryEntry> {
        return entries.value.take(limit)
    }
}

