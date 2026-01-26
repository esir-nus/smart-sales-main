package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.MemoryEntryEntity

/**
 * Memory Entry 仓库 — Hot/Cement Zone 访问
 * @see Prism-V1.md §5.1, §5.7
 */
interface MemoryEntryRepository {
    suspend fun getById(id: String): MemoryEntryEntity?
    suspend fun getHotZone(): List<MemoryEntryEntity>
    suspend fun getCementZone(): List<MemoryEntryEntity>
    suspend fun getBySessionId(sessionId: String): List<MemoryEntryEntity>
    suspend fun insert(entry: MemoryEntryEntity)
    suspend fun update(entry: MemoryEntryEntity)
    suspend fun archive(id: String)
    suspend fun delete(id: String)
}
