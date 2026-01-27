package com.smartsales.data.prismlib.db.dao

import androidx.room.*
import com.smartsales.data.prismlib.db.entities.RoomRelevancyEntry

@Dao
interface RelevancyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RoomRelevancyEntry)

    @Query("SELECT * FROM relevancy_entries WHERE entityId = :id")
    suspend fun getById(id: String): RoomRelevancyEntry?

    @Query("DELETE FROM relevancy_entries WHERE entityId = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM relevancy_entries")
    suspend fun getAll(): List<RoomRelevancyEntry>
    
    // Note: Complex querying by alias usually done in Repo logic by fetching all, 
    // or we can try a LIKE query on JSON if supported, but getAll() + filtering in memory 
    // is safer for complex JSON structures unless we normalize aliases table.
    // For Phase 2, getAll() is acceptable given small data size.
}
