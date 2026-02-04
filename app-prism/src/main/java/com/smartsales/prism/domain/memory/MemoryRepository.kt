package com.smartsales.prism.domain.memory

import com.smartsales.prism.domain.config.SubscriptionTier
import kotlinx.coroutines.flow.Flow

/**
 * 记忆仓库 — Hot/Cement Zone 访问接口
 * @see Prism-V1.md §5.1
 */
interface MemoryRepository {
    /**
     * 获取热区条目（活跃条目 + 14天内日程）
     */
    suspend fun getHotEntries(sessionId: String): List<MemoryEntry>
    
    /**
     * 获取热区条目（分层读取）
     * @param userTier 用户订阅层级，决定热区窗口大小
     */
    suspend fun getHotEntries(sessionId: String, userTier: SubscriptionTier): List<MemoryEntry>
    
    /**
     * 获取水泥区条目（已归档且超出热区窗口）
     * @param userTier 用户订阅层级，决定热区窗口边界
     */
    suspend fun getCementEntries(sessionId: String, userTier: SubscriptionTier): List<MemoryEntry>
    
    /**
     * 搜索记忆
     * @param query 查询关键词
     * @param limit 返回数量上限
     */
    suspend fun search(query: String, limit: Int = 10): List<MemoryEntry>
    
    /**
     * 观察热区变化
     */
    fun observeHotEntries(sessionId: String): Flow<List<MemoryEntry>>
    
    /**
     * 保存条目
     */
    suspend fun save(entry: MemoryEntry)
    
    /**
     * 归档条目（Hot → Cement）
     */
    suspend fun archive(entryId: String)
}
