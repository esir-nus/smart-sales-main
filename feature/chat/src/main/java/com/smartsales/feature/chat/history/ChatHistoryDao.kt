package com.smartsales.feature.chat.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryDao.kt
// 模块：:feature:chat
// 说明：提供聊天记录的查询与替换操作
// 作者：创建于 2025-11-22
@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun loadMessages(sessionId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
