package com.smartsales.prism.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Prism 数据库 — 单一真相来源
 * 
 * Version 1: 初始 schema (MemoryEntry, EntityEntry, UserHabit)
 * Version 2: 添加 ScheduledTask (Room 替代 CalendarProvider)
 * Version 3: MemoryEntry 添加 workflow 列 (Wave 4)
 * exportSchema = false: 不导出 schema JSON (简化 MVP)
 */
@Database(
    entities = [
        MemoryEntryEntity::class,
        EntityEntryEntity::class,
        UserHabitEntity::class,
        ScheduledTaskEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PrismDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun entityDao(): EntityDao
    abstract fun userHabitDao(): UserHabitDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Wave 4: Rich MemoryEntry Schema — 6 nullable columns
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN workflow TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN title TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN completedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN outcomeStatus TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN outcomeJson TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN payloadJson TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN displayContent TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE memory_entries ADD COLUMN artifactsJson TEXT DEFAULT NULL")
            }
        }
    }
}
