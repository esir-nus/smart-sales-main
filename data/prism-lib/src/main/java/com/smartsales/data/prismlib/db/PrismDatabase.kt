package com.smartsales.data.prismlib.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartsales.data.prismlib.db.dao.*
import com.smartsales.data.prismlib.db.entities.*


@Database(
    entities = [
        RoomMemoryEntry::class,
        RoomRelevancyEntry::class,
        RoomSession::class,
        RoomScheduledTask::class,
        RoomUserProfile::class,
        RoomUserHabit::class,
        RoomInspiration::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PrismDatabase : RoomDatabase() {
    abstract fun memoryEntryDao(): MemoryEntryDao
    abstract fun relevancyDao(): RelevancyDao
    abstract fun sessionsDao(): SessionsDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun userHabitDao(): UserHabitDao
    abstract fun inspirationDao(): InspirationDao
}



