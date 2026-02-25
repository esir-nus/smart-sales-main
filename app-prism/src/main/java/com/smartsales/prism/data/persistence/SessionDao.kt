package com.smartsales.prism.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 会话元数据 DAO
 *
 * 排序: 置顶优先，然后按时间倒序
 */
@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY isPinned DESC, timestamp DESC")
    fun getAll(): List<SessionEntity>

    /** 响应式查询 — Room 自动在表变更后重新发射 */
    @Query("SELECT * FROM sessions ORDER BY isPinned DESC, timestamp DESC")
    fun getAllFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE sessionId = :id")
    fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: SessionEntity)

    @Update
    fun update(entity: SessionEntity)

    @Query("DELETE FROM sessions WHERE sessionId = :id")
    fun delete(id: String)
}
