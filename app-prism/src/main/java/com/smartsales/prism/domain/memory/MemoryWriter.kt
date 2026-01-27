package com.smartsales.prism.domain.memory

/**
 * 记忆写入器 — Fire-and-Forget 模式
 * @see Prism-V1.md §2.2 #5
 */
interface MemoryWriter {
    /**
     * 异步写入记忆条目（不阻塞 UI）
     * @param entry 待写入的记忆条目
     */
    fun write(entry: MemoryEntry)
    
    /**
     * 异步更新关联性条目
     * @param entry 待更新的关联性条目
     */
    fun updateRelevancy(entry: RelevancyEntry)
}
