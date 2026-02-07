package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.config.SubscriptionConfig
import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * Fake MemoryRepository — 内存中存储
 * 支持 Lazy Compaction: 活跃区/归档区通过查询时过滤实现
 */
@Singleton
class FakeMemoryRepository @Inject constructor() : MemoryRepository {
    
    private val entries = MutableStateFlow<List<MemoryEntry>>(emptyList())
    
    // 用于测试注入当前时间
    var currentTimeProvider: () -> Long = { System.currentTimeMillis() }
    
    init {
        // 预填充种子数据，供 Coach Wave 3 记忆搜索 L2 测试使用
        val now = System.currentTimeMillis()
        val seedEntries = listOf(
            MemoryEntry(
                entryId = "seed-price-1",
                sessionId = "old-session-1",
                content = "价格异议处理：先别急着降价，问一句'跟谁比贵了？'揭示真实顾虑。如果是预算问题，谈分期或缩小范围做MVP。",
                entryType = MemoryEntryType.ASSISTANT_RESPONSE,
                createdAt = now - 86400000,
                updatedAt = now - 86400000
            ),
            MemoryEntry(
                entryId = "seed-negotiation-1",
                sessionId = "old-session-2",
                content = "谈判技巧：用'假设成交法'推进——'如果价格合适，您希望什么时候开始？'让客户进入决策框架。",
                entryType = MemoryEntryType.ASSISTANT_RESPONSE,
                createdAt = now - 172800000,
                updatedAt = now - 172800000
            ),
            MemoryEntry(
                entryId = "seed-followup-1",
                sessionId = "old-session-3",
                content = "跟进时间：首次会面后24小时内发感谢邮件，48小时内发方案，1周后电话跟进。超过2周未回复，降低优先级。",
                entryType = MemoryEntryType.ASSISTANT_RESPONSE,
                createdAt = now - 259200000,
                updatedAt = now - 259200000
            )
        )
        entries.value = seedEntries
        Log.d("CoachMemory", "🌱 FakeMemoryRepository seeded with ${seedEntries.size} entries: ${seedEntries.map { it.entryId }}")
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
        Log.d("CoachMemory", "💾 MemoryRepository.save(id=${entry.entryId}, type=${entry.entryType}, content='${entry.content.take(40)}...')")
        val current = entries.value.toMutableList()
        current.removeAll { it.entryId == entry.entryId }
        current.add(entry)
        entries.value = current
        Log.d("CoachMemory", "💾 MemoryRepository total entries: ${entries.value.size}")
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

