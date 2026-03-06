package com.smartsales.prism.data.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * 会话消息 DAO
 */
@Dao
interface MessageDao {
    @Query("SELECT * FROM session_messages WHERE sessionId = :sid ORDER BY orderIndex")
    fun getBySession(sid: String): List<MessageEntity>

    @Insert
    fun insert(entity: MessageEntity)

    @Query("DELETE FROM session_messages WHERE sessionId = :sid")
    fun deleteBySession(sid: String)
}
