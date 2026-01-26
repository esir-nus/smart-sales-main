package com.smartsales.data.prismlib.db.dao

import androidx.room.*
import com.smartsales.data.prismlib.db.entities.RoomUserProfile
import com.smartsales.data.prismlib.db.entities.RoomUserHabit
import com.smartsales.data.prismlib.db.entities.RoomInspiration

@Dao
interface UserProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: RoomUserProfile)

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    suspend fun getById(userId: String): RoomUserProfile?
}

@Dao
interface UserHabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: RoomUserHabit)

    @Query("SELECT * FROM user_habits WHERE habitKey = :key")
    suspend fun getByKey(key: String): RoomUserHabit?

    @Query("SELECT * FROM user_habits WHERE entityId = :entityId")
    suspend fun getForEntity(entityId: String): List<RoomUserHabit>

    @Query("DELETE FROM user_habits WHERE habitKey = :key")
    suspend fun delete(key: String)
}

@Dao
interface InspirationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inspiration: RoomInspiration)

    @Query("SELECT * FROM inspirations WHERE id = :id")
    suspend fun getById(id: String): RoomInspiration?

    @Query("SELECT * FROM inspirations ORDER BY createdAt DESC")
    suspend fun getAll(): List<RoomInspiration>
    
    @Query("DELETE FROM inspirations WHERE id = :id")
    suspend fun delete(id: String)
}
