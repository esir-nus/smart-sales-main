package com.smartsales.feature.chat.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/persistence/AiSessionDao.kt
// 模块：:feature:chat
// 说明：提供会话摘要的增删查能力
// 作者：创建于 2025-11-16
@Dao
interface AiSessionDao {
    @Query("SELECT * FROM ai_session ORDER BY updated_at DESC LIMIT :limit")
    fun observeSummaries(limit: Int = 30): Flow<List<AiSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AiSessionEntity)

    @Query("DELETE FROM ai_session WHERE session_id = :sessionId")
    suspend fun deleteById(sessionId: String)

    @Query("SELECT * FROM ai_session WHERE session_id = :sessionId LIMIT 1")
    suspend fun findById(sessionId: String): AiSessionEntity?
}
