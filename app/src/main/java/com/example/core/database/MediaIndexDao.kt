package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaIndexDao {

    @Query("SELECT * FROM media_index ORDER BY dateTakenMillis DESC")
    fun observeAllMedia(): Flow<List<MediaIndexEntity>>

    @Query("SELECT * FROM media_index ORDER BY dateTakenMillis DESC")
    suspend fun getAllMediaSnapshot(): List<MediaIndexEntity>

    @Query("SELECT * FROM media_index WHERE mediaStoreId = :id LIMIT 1")
    fun observeById(id: Long): Flow<MediaIndexEntity?>

    @Query("SELECT * FROM media_index WHERE mediaStoreId = :id LIMIT 1")
    suspend fun getById(id: Long): MediaIndexEntity?

    @Query(
        """
        SELECT media_index.* FROM media_index
        JOIN media_search_fts ON media_index.rowid = media_search_fts.rowid
        WHERE media_search_fts MATCH :ftsQuery
        ORDER BY media_index.dateTakenMillis DESC
        """
    )
    suspend fun searchFts(ftsQuery: String): List<MediaIndexEntity>

    @Query("SELECT DISTINCT albumName FROM media_index WHERE albumName != '' ORDER BY albumName ASC")
    fun observeDistinctAlbums(): Flow<List<String>>

    @Query("SELECT DISTINCT relativePath FROM media_index WHERE relativePath != '' ORDER BY relativePath ASC")
    fun observeDistinctFolders(): Flow<List<String>>

    @Query("SELECT DISTINCT format FROM media_index WHERE format != '' ORDER BY format ASC")
    fun observeDistinctFormats(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM media_index")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_index WHERE aiIndexedAtMillis > 0")
    fun observeAiIndexedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_index WHERE ocrText != ''")
    fun observeOcrCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_index WHERE hasGps = 1")
    fun observeGpsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MediaIndexEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaIndexEntity)

    @Query("UPDATE media_index SET isFavorite = :favorite WHERE mediaStoreId = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE media_index SET userRating = :rating WHERE mediaStoreId = :id")
    suspend fun setRating(id: Long, rating: Int)

    @Query("UPDATE media_index SET userTags = :tags, userNotes = :notes WHERE mediaStoreId = :id")
    suspend fun updateUserMetadata(id: Long, tags: String, notes: String)

    @Query("UPDATE media_index SET groupId = :groupId, isBestShot = :isBestShot WHERE mediaStoreId = :id")
    suspend fun updateGrouping(id: Long, groupId: String, isBestShot: Boolean)

    @Query("UPDATE media_index SET faceCount = 0")
    suspend fun clearFaceCounts()

    @Query("DELETE FROM media_index WHERE mediaStoreId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_index WHERE mediaStoreId NOT IN (:activeIds)")
    suspend fun deleteMissingMedia(activeIds: List<Long>)

    @Query("DELETE FROM media_index")
    suspend fun deleteAll()
}
