package com.smartsales.data.prismlib.db.dao

import androidx.room.*
import com.smartsales.data.prismlib.db.entities.RoomSession

@Dao
interface SessionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: RoomSession)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): RoomSession?

    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC")
    suspend fun getAll(): List<RoomSession>

    @Query("SELECT * FROM sessions WHERE isPinned = 1 ORDER BY updatedAt DESC")
    suspend fun getPinned(): List<RoomSession>

    @Query("UPDATE sessions SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)
}
