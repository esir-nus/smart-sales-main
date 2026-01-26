package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.RelevancyEntry

/**
 * Relevancy Library 仓库
 * @see Prism-V1.md §5.2
 */
interface RelevancyRepository {
    suspend fun getByEntityId(id: String): RelevancyEntry?
    suspend fun findByAlias(alias: String): List<RelevancyEntry>
    suspend fun getAll(): List<RelevancyEntry>
    suspend fun upsert(entry: RelevancyEntry)
    suspend fun delete(entityId: String)
}
