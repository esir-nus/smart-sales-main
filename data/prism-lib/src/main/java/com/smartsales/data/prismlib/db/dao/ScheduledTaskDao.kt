package com.smartsales.data.prismlib.db.dao

import androidx.room.*
import com.smartsales.data.prismlib.db.entities.RoomScheduledTask

@Dao
interface ScheduledTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: RoomScheduledTask)

    @Query("SELECT * FROM scheduled_tasks WHERE id = :id")
    suspend fun getById(id: String): RoomScheduledTask?

    @Query("SELECT * FROM scheduled_tasks WHERE scheduledAt >= :now ORDER BY scheduledAt ASC LIMIT :limit")
    suspend fun getUpcoming(now: Long, limit: Int): List<RoomScheduledTask>
    
    @Query("SELECT * FROM scheduled_tasks WHERE scheduledAt BETWEEN :start AND :end ORDER BY scheduledAt ASC")
    suspend fun getForDateRange(start: Long, end: Long): List<RoomScheduledTask>

    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun delete(id: String)
}
