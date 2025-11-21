package com.smartsales.feature.chat.history

import androidx.room.Database
import androidx.room.RoomDatabase

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryDatabase.kt
// 模块：:feature:chat
// 说明：Home 聊天记录的 Room 数据库
// 作者：创建于 2025-11-22
@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatHistoryDatabase : RoomDatabase() {
    abstract fun chatHistoryDao(): ChatHistoryDao
}
