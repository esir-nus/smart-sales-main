package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.Session

/**
 * 会话仓库
 */
interface SessionsRepository {
    suspend fun getAll(): List<Session>
    suspend fun getById(id: String): Session?
    suspend fun getPinned(): List<Session>
    suspend fun insert(session: Session)
    suspend fun update(session: Session)
    suspend fun delete(id: String)
    suspend fun pin(id: String, isPinned: Boolean)
}
