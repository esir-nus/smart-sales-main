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
        // 预填充种子数据 — 模拟真实用户历史记忆，供 L3 测试使用
        // 场景1: 2月4日拜访华为见CTO
        // 场景2: 2月9日与 Professor Ameer 年度会议
        // 场景3: 1月28日王总生日送茶壶
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        
        // 基于当前日期(2026-02-08)计算相对时间
        val feb4 = now - (4 * dayMs)   // 约4天前
        val feb9 = now + (1 * dayMs)   // 约1天后
        val jan28 = now - (11 * dayMs) // 约11天前
        
        val seedEntries = listOf(
            // 场景1: 拜访华为 — 包含会面细节、跟进计划
            MemoryEntry(
                entryId = "seed-huawei-visit",
                sessionId = "session-feb04",
                content = "今天去华为坂田基地拜访，见了IT部门CTO李总。他们目前用的是竞品的CRM系统，合同明年3月到期。" +
                        "李总对我们的AI辅助功能很感兴趣，特别是智能会议摘要和客户画像分析。" +
                        "他提了两个顾虑：1) 数据安全合规（华为对数据主权要求很高），2) 和现有ERP系统的集成。" +
                        "约了下周三再做一次技术方案演示，需要带上解决方案架构师老陈。",
                entryType = MemoryEntryType.TASK_RECORD,
                createdAt = feb4,
                updatedAt = feb4,
                structuredJson = """{"relatedEntityIds":["huawei-001","li-cto-001"]}"""
            ),
            // 场景2: 与 Professor Ameer 年度会议（未来事件）
            MemoryEntry(
                entryId = "seed-ameer-meeting",
                sessionId = "session-feb08",
                content = "2月9日下午2点，和 Professor Ameer 的年度回顾会议。" +
                        "需要准备：1) 过去一年的合作成果总结（3个联合项目），2) 下一年的合作提案，3) NUS实验室参观安排。" +
                        "Ameer 教授比较关注AI在东南亚市场的应用场景，准备几个本地化案例。" +
                        "地点：NUS Computing 学院会议室 COM2-02-12。",
                entryType = MemoryEntryType.SCHEDULE_ITEM,
                createdAt = now,
                updatedAt = now,
                scheduledAt = feb9,
                structuredJson = """{"relatedEntityIds":["ameer-prof-001"]}"""
            ),
            // 场景3: 王总生日送茶壶 — 客户关系维护
            MemoryEntry(
                entryId = "seed-boss-wang-birthday",
                sessionId = "session-jan28",
                content = "今天是王总生日，送了一套景德镇手工青花瓷茶壶给他。王总很开心，" +
                        "聊到他最近在收藏紫砂壶，下次可以找个宜兴的壶送。" +
                        "他顺便提了一下Q2的预算申请已经批了，让我下周把正式报价单发过去。" +
                        "王总的助理小张说老板下个月要去日本出差两周（3月10-24日），报价要赶在那之前定下来。",
                entryType = MemoryEntryType.TASK_RECORD,
                createdAt = jan28,
                updatedAt = jan28,
                structuredJson = """{"relatedEntityIds":["wang-boss-001"]}"""
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
        Log.d("CoachMemory", "📦 markAsArchived(id=$entryId)")
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
    
    /**
     * 按实体 ID 查询（通过 structuredJson 中的 relatedEntityIds）
     * 用引号包裹 entityId 避免子串误匹配（如 "c-1" 不匹配 "c-10"）
     */
    override suspend fun getByEntityId(entityId: String, limit: Int): List<MemoryEntry> {
        val quoted = "\"$entityId\""
        val results = entries.value
            .filter { it.structuredJson?.contains(quoted) == true }
            .sortedByDescending { it.createdAt }
            .take(limit)
        Log.d("CoachMemory", "🔗 getByEntityId('$entityId') → ${results.size} hits")
        return results
    }
}

