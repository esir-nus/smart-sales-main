package com.smartsales.prism.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO — 调度任务数据访问
 */
@Dao
interface ScheduledTaskDao {
    @Query("SELECT * FROM scheduled_tasks WHERE startTimeMillis >= :startMs AND startTimeMillis < :endMs ORDER BY startTimeMillis ASC")
    fun getByDateRange(startMs: Long, endMs: Long): Flow<List<ScheduledTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScheduledTaskEntity)

    @Update
    suspend fun update(entity: ScheduledTaskEntity)

    @Query("SELECT * FROM scheduled_tasks WHERE taskId = :id")
    suspend fun getById(id: String): ScheduledTaskEntity?

    @Query("DELETE FROM scheduled_tasks WHERE taskId = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM scheduled_tasks WHERE hasAlarm = 1 AND isDone = 0 AND startTimeMillis > :nowMs")
    suspend fun getFutureTasksWithAlarm(nowMs: Long): List<ScheduledTaskEntity>

    @Query("SELECT * FROM scheduled_tasks WHERE isDone = 1 AND startTimeMillis >= :startMs ORDER BY startTimeMillis DESC LIMIT :limit")
    suspend fun getRecentCompleted(startMs: Long, limit: Int): List<ScheduledTaskEntity>
}
