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
 * Version 4: EntityEntry 添加 nextAction 列
 * Version 5: ScheduledTask 添加 urgencyLevel 列
 * Version 6: 添加 sessions 表
 * Version 7: 添加 session_messages 表 (会话消息持久化)
 * Version 8: ScheduledTask 添加 hasConflict / isVague 列
 * Version 9: ScheduledTask 添加 conflictWithTaskId / conflictSummary 列
 * exportSchema = false: 不导出 schema JSON (简化 MVP)
 */
@Database(
    entities = [
        MemoryEntryEntity::class,
        EntityEntryEntity::class,
        UserHabitEntity::class,
        ScheduledTaskEntity::class,
        SessionEntity::class,
        MessageEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class PrismDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun entityDao(): EntityDao
    abstract fun userHabitDao(): UserHabitDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // EntityEntry 添加 nextAction 列
                db.execSQL("ALTER TABLE entity_entries ADD COLUMN nextAction TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ScheduledTask 添加 urgencyLevel 列
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN urgencyLevel TEXT NOT NULL DEFAULT 'L3_NORMAL'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 会话元数据表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        sessionId TEXT NOT NULL PRIMARY KEY,
                        clientName TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        linkedAudioId TEXT DEFAULT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 会话消息表 — 外键关联 sessions, 级联删除
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS session_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId TEXT NOT NULL,
                        isUser INTEGER NOT NULL DEFAULT 0,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        orderIndex INTEGER NOT NULL,
                        FOREIGN KEY (sessionId) REFERENCES sessions(sessionId) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_messages_sessionId ON session_messages(sessionId)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN hasConflict INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN isVague INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN conflictWithTaskId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE scheduled_tasks ADD COLUMN conflictSummary TEXT DEFAULT NULL")
            }
        }
    }
}
