package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryWriter
import com.smartsales.prism.domain.memory.RelevancyEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake MemoryWriter — 内存中存储，不持久化
 * Phase 2 占位实现
 */
@Singleton
class FakeMemoryWriter @Inject constructor() : MemoryWriter {
    
    private val memoryEntries = mutableListOf<MemoryEntry>()
    private val relevancyEntries = mutableMapOf<String, RelevancyEntry>()
    
    override fun write(entry: MemoryEntry) {
        // Fire-and-forget 模拟：直接加入内存列表
        memoryEntries.add(entry)
    }
    
    override fun updateRelevancy(entry: RelevancyEntry) {
        // 更新或插入
        relevancyEntries[entry.entityId] = entry
    }
    
    // 测试辅助方法
    fun getWrittenEntries(): List<MemoryEntry> = memoryEntries.toList()
    fun getWrittenRelevancy(): Map<String, RelevancyEntry> = relevancyEntries.toMap()
}
