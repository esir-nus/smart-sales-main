package com.smartsales.prism.domain.memory

import com.smartsales.prism.domain.config.SubscriptionTier
import kotlinx.coroutines.flow.Flow

/**
 * 记忆仓库 — Active/Archived Zone 访问接口
 * @see Memory Center interface
 */
interface MemoryRepository {
    /**
     * 获取活跃区条目（活跃条目 + 14天内日程）
     */
    suspend fun getActiveEntries(sessionId: String): List<MemoryEntry>
    
    /**
     * 获取活跃区条目（分层读取）
     * @param userTier 用户订阅层级，决定活跃区窗口大小
     */
    suspend fun getActiveEntries(sessionId: String, userTier: SubscriptionTier): List<MemoryEntry>
    
    /**
     * 获取归档区条目（已归档且超出活跃区窗口）
     * @param userTier 用户订阅层级，决定活跃区窗口边界
     */
    suspend fun getArchivedEntries(sessionId: String, userTier: SubscriptionTier): List<MemoryEntry>
    
    /**
     * 搜索记忆
     * @param query 查询关键词
     * @param limit 返回数量上限
     */
    suspend fun search(query: String, limit: Int = 10): List<MemoryEntry>
    
    /**
     * 观察活跃区变化
     */
    fun observeActiveEntries(sessionId: String): Flow<List<MemoryEntry>>
    
    /**
     * 保存条目
     */
    suspend fun save(entry: MemoryEntry)
    
    /**
     * 标记为已归档
     */
    suspend fun markAsArchived(entryId: String)
    
    /**
     * 按实体 ID 查询关联记忆（通过 structuredJson 中的 relatedEntityIds）
     * @param entityId 实体 ID
     * @param limit 返回数量上限
     */
    suspend fun getByEntityId(entityId: String, limit: Int = 50): List<MemoryEntry>

    /**
     * 观察实体的关联记忆
     */
    fun observeByEntityId(entityId: String): Flow<List<MemoryEntry>>
}

// Backwards compatibility aliases (deprecated, will be removed)
@Deprecated("Use getActiveEntries instead")
suspend fun MemoryRepository.getHotEntries(sessionId: String) = getActiveEntries(sessionId)

@Deprecated("Use getActiveEntries instead")
suspend fun MemoryRepository.getHotEntries(sessionId: String, userTier: SubscriptionTier) = getActiveEntries(sessionId, userTier)

@Deprecated("Use getArchivedEntries instead")
suspend fun MemoryRepository.getCementEntries(sessionId: String, userTier: SubscriptionTier) = getArchivedEntries(sessionId, userTier)

@Deprecated("Use observeActiveEntries instead")
fun MemoryRepository.observeHotEntries(sessionId: String) = observeActiveEntries(sessionId)

@Deprecated("Use markAsArchived instead")
suspend fun MemoryRepository.archive(entryId: String) = markAsArchived(entryId)
