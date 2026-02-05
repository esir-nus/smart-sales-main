package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryWriter
import com.smartsales.prism.domain.memory.EntityEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake MemoryWriter — 内存中存储，不持久化
 * Phase 2 占位实现
 */
@Singleton
class FakeMemoryWriter @Inject constructor() : MemoryWriter {
    
    private val memoryEntries = mutableListOf<MemoryEntry>()
    private val entityEntries = mutableMapOf<String, EntityEntry>()
    
    override fun write(entry: MemoryEntry) {
        // Fire-and-forget 模拟：直接加入内存列表
        memoryEntries.add(entry)
    }
    
    override fun updateRelevancy(entry: EntityEntry) {
        // 更新或插入
        entityEntries[entry.entityId] = entry
    }
    
    // 测试辅助方法
    fun getWrittenEntries(): List<MemoryEntry> = memoryEntries.toList()
    fun getWrittenRelevancy(): Map<String, EntityEntry> = entityEntries.toMap()
}

