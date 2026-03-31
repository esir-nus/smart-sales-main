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
    @Query("SELECT * FROM scheduled_tasks WHERE startTimeMillis >= :startMs AND startTimeMillis < :endMs ORDER BY urgencyLevel ASC, startTimeMillis ASC")
    fun getByDateRange(startMs: Long, endMs: Long): Flow<List<ScheduledTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScheduledTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ScheduledTaskEntity>)

    @Update
    suspend fun update(entity: ScheduledTaskEntity)

    @Query("SELECT * FROM scheduled_tasks WHERE taskId = :id")
    suspend fun getById(id: String): ScheduledTaskEntity?

    @Query("""
        SELECT * FROM scheduled_tasks
        WHERE isDone = 0
        ORDER BY urgencyLevel ASC, startTimeMillis ASC
    """)
    suspend fun getActiveTasks(): List<ScheduledTaskEntity>

    @Query("DELETE FROM scheduled_tasks WHERE taskId = :id")
    suspend fun deleteById(id: String)

    @androidx.room.Transaction
    suspend fun reschedule(oldId: String, newEntity: ScheduledTaskEntity) {
        deleteById(oldId)
        insert(newEntity)
    }

    @Query("SELECT * FROM scheduled_tasks WHERE isDone = 0 AND isVague = 0 AND startTimeMillis > :nowMs")
    suspend fun getFutureExactTasksForReminderRestore(nowMs: Long): List<ScheduledTaskEntity>

    @Query("SELECT * FROM scheduled_tasks WHERE isDone = 1 AND startTimeMillis >= :startMs ORDER BY urgencyLevel ASC, startTimeMillis DESC LIMIT :limit")
    suspend fun getRecentCompleted(startMs: Long, limit: Int): List<ScheduledTaskEntity>

    @Query("""
        SELECT * FROM scheduled_tasks 
        WHERE keyPersonEntityId = :entityId 
          AND isDone = 0 
          AND urgencyLevel IN ('L1_CRITICAL', 'L2_IMPORTANT')
        ORDER BY 
          CASE urgencyLevel 
            WHEN 'L1_CRITICAL' THEN 0 
            WHEN 'L2_IMPORTANT' THEN 1 
          END,
          startTimeMillis ASC
        LIMIT 1
    """)
    suspend fun getTopUrgentActiveTask(entityId: String): ScheduledTaskEntity?

    @Query("SELECT * FROM scheduled_tasks WHERE keyPersonEntityId = :entityId ORDER BY startTimeMillis DESC")
    fun observeByEntityId(entityId: String): Flow<List<ScheduledTaskEntity>>
}
