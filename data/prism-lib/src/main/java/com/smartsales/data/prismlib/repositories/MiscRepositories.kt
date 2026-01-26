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
    override suspend fun insert(profile: UserProfile) {
        dao.insert(RoomUserProfile.fromDomain(profile))
    }
    override suspend fun getById(userId: String): UserProfile? {
        return dao.getById(userId)?.toDomain()
    }
}

@Singleton
class RoomUserHabitRepository @Inject constructor(
    private val dao: UserHabitDao
) : UserHabitRepository {
    override suspend fun upsert(habit: UserHabit) {
        dao.upsert(RoomUserHabit.fromDomain(habit))
    }
    override suspend fun getByKey(key: String): UserHabit? {
        return dao.getByKey(key)?.toDomain()
    }
    override suspend fun getForEntity(entityId: String): List<UserHabit> {
        return dao.getForEntity(entityId).map { it.toDomain() }
    }
    override suspend fun delete(key: String) {
        dao.delete(key)
    }
}

@Singleton
class RoomInspirationRepository @Inject constructor(
    private val dao: InspirationDao
) : InspirationRepository {
    override suspend fun insert(inspiration: Inspiration) {
        dao.insert(RoomInspiration.fromDomain(inspiration))
    }
    override suspend fun getById(id: String): Inspiration? {
        return dao.getById(id)?.toDomain()
    }
    override suspend fun getAll(): List<Inspiration> {
        return dao.getAll().map { it.toDomain() }
    }
    override suspend fun delete(id: String) {
        dao.delete(id)
    }
}
