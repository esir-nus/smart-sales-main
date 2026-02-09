package com.smartsales.prism.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Prism 数据库 — 单一真相来源
 * 
 * Version 1: 初始 schema (MemoryEntry, EntityEntry, UserHabit)
 * Version 2: 添加 ScheduledTask (Room 替代 CalendarProvider)
 * exportSchema = false: 不导出 schema JSON (简化 MVP)
 */
@Database(
    entities = [
        MemoryEntryEntity::class,
        EntityEntryEntity::class,
        UserHabitEntity::class,
        ScheduledTaskEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PrismDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun entityDao(): EntityDao
    abstract fun userHabitDao(): UserHabitDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao
}
