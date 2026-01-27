package com.smartsales.data.prismlib.repositories

import com.smartsales.data.prismlib.db.dao.SessionsDao
import com.smartsales.data.prismlib.db.entities.RoomSession
import com.smartsales.domain.prism.core.entities.Session
import com.smartsales.domain.prism.core.repositories.SessionsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSessionsRepository @Inject constructor(
    private val dao: SessionsDao
) : SessionsRepository {

    override suspend fun insert(session: Session) {
        dao.insert(RoomSession.fromDomain(session))
    }

    override suspend fun getById(id: String): Session? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getAll(): List<Session> {
        return dao.getAll().map { it.toDomain() }
    }

    override suspend fun pin(id: String, isPinned: Boolean) {
        dao.setPinned(id, isPinned)
    }

    override suspend fun getPinned(): List<Session> {
        return dao.getPinned().map { it.toDomain() }
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }

    override suspend fun update(session: Session) {
        dao.insert(RoomSession.fromDomain(session)) // REPLACE via upsert
    }
}
