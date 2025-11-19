package com.smartsales.feature.chat.persistence

import androidx.room.RoomDatabase

// 文件：feature/chat/src/debug/java/com/smartsales/feature/chat/persistence/DebugAiSessionDatabase.kt
// 模块：:feature:chat
// 说明：仅为 JVM 单测提供 RoomDatabase 占位，避免缺少 sqlite 依赖
// 作者：创建于 2025-11-16
abstract class DebugAiSessionDatabase : RoomDatabase()
