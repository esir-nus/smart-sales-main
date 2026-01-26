package com.smartsales.data.prismlib.db.dao

import androidx.room.*
import com.smartsales.data.prismlib.db.entities.RoomMemoryEntry

@Dao
interface MemoryEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RoomMemoryEntry)

    @Query("SELECT * FROM memory_entries WHERE id = :id")
    suspend fun getById(id: String): RoomMemoryEntry?

    @Query("SELECT * FROM memory_entries WHERE isArchived = 0 ORDER BY updatedAt DESC")
    suspend fun getHotZone(): List<RoomMemoryEntry>

    @Query("SELECT * FROM memory_entries WHERE isArchived = 1 ORDER BY updatedAt DESC")
    suspend fun getCementZone(): List<RoomMemoryEntry>

    @Query("UPDATE memory_entries SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: String)
    
    @Query("SELECT * FROM memory_entries WHERE sessionId = :sessionId")
    suspend fun getBySession(sessionId: String): List<RoomMemoryEntry>
}
