package com.smartsales.data.prismlib.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartsales.data.prismlib.db.entities.RoomMemoryEntry

@Database(
    entities = [
        RoomMemoryEntry::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PrismDatabase : RoomDatabase() {
    // DAOs will be added in Chunk B
}
