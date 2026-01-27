package com.smartsales.data.prismlib.repositories

import com.smartsales.data.prismlib.db.dao.InspirationDao
import com.smartsales.data.prismlib.db.dao.UserHabitDao
import com.smartsales.data.prismlib.db.dao.UserProfileDao
import com.smartsales.data.prismlib.db.entities.RoomInspiration
import com.smartsales.data.prismlib.db.entities.RoomUserHabit
import com.smartsales.data.prismlib.db.entities.RoomUserProfile
import com.smartsales.domain.prism.core.entities.Inspiration
import com.smartsales.domain.prism.core.entities.UserHabit
import com.smartsales.domain.prism.core.entities.UserProfile
import com.smartsales.domain.prism.core.repositories.InspirationRepository
import com.smartsales.domain.prism.core.repositories.UserHabitRepository
import com.smartsales.domain.prism.core.repositories.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomUserProfileRepository @Inject constructor(
    private val dao: UserProfileDao
) : UserProfileRepository {
    override suspend fun get(): UserProfile? {
        return dao.get()?.toDomain()
    }
    override suspend fun save(profile: UserProfile) {
        dao.insert(RoomUserProfile.fromDomain(profile))
    }
}

@Singleton
class RoomUserHabitRepository @Inject constructor(
    private val dao: UserHabitDao
) : UserHabitRepository {
    override suspend fun getAll(): List<UserHabit> {
        return dao.getAll().map { it.toDomain() }
    }
    override suspend fun getByKey(habitKey: String): UserHabit? {
        return dao.getByKey(habitKey)?.toDomain()
    }
    override suspend fun getForEntity(entityId: String): List<UserHabit> {
        return dao.getForEntity(entityId).map { it.toDomain() }
    }
    override suspend fun upsert(habit: UserHabit) {
        dao.upsert(RoomUserHabit.fromDomain(habit))
    }
    override suspend fun delete(habitKey: String) {
        dao.delete(habitKey)
    }
}

@Singleton
class RoomInspirationRepository @Inject constructor(
    private val dao: InspirationDao
) : InspirationRepository {
    override suspend fun getAll(): List<Inspiration> {
        return dao.getAll().map { it.toDomain() }
    }
    override suspend fun getById(id: String): Inspiration? {
        return dao.getById(id)?.toDomain()
    }
    override suspend fun getUnpromoted(): List<Inspiration> {
        return dao.getUnpromoted().map { it.toDomain() }
    }
    override suspend fun insert(inspiration: Inspiration) {
        dao.insert(RoomInspiration.fromDomain(inspiration))
    }
    override suspend fun promoteToTask(id: String, taskId: String) {
        dao.promoteToTask(id, taskId)
    }
    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}
