package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSearchDao {

    @Query("SELECT * FROM saved_searches ORDER BY isPinned DESC, lastUsedMillis DESC")
    fun observeAllSavedSearches(): Flow<List<SavedSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(savedSearch: SavedSearchEntity): Long

    @Query("UPDATE saved_searches SET lastUsedMillis = :timestamp WHERE id = :id")
    suspend fun touchLastUsed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE saved_searches SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE saved_searches SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM saved_searches WHERE id = :id")
    suspend fun deleteById(id: Long)
}
