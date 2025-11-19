package com.smartsales.feature.chat.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/persistence/AiSessionDatabase.kt
// 模块：:feature:chat
// 说明：提供会话摘要 DAO 的 Room 数据库
// 作者：创建于 2025-11-16
@Database(
    entities = [AiSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AiSessionDatabase : RoomDatabase() {
    abstract fun aiSessionDao(): AiSessionDao
}
