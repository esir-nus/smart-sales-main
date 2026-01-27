package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.Inspiration

/**
 * 灵感卡片仓库
 */
interface InspirationRepository {
    suspend fun getAll(): List<Inspiration>
    suspend fun getById(id: String): Inspiration?
    suspend fun getUnpromoted(): List<Inspiration>
    suspend fun insert(inspiration: Inspiration)
    suspend fun promoteToTask(id: String, taskId: String)
    suspend fun delete(id: String)
}
