package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAlbumDao {
    @Query("SELECT * FROM custom_albums ORDER BY isPinned DESC, sortOrder ASC, createdAtMillis DESC")
    fun observeAllAlbums(): Flow<List<CustomAlbumEntity>>

    @Query("SELECT * FROM custom_albums ORDER BY isPinned DESC, sortOrder ASC, createdAtMillis DESC")
    suspend fun getAllAlbumsSnapshot(): List<CustomAlbumEntity>

    @Query("SELECT * FROM custom_album_members")
    fun observeAllMembers(): Flow<List<CustomAlbumMemberEntity>>

    @Query("SELECT mediaStoreId FROM custom_album_members WHERE albumId = :albumId")
    suspend fun getMediaIdsInAlbum(albumId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(album: CustomAlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMembers(members: List<CustomAlbumMemberEntity>)

    @Query("UPDATE custom_albums SET name = :name WHERE albumId = :albumId")
    suspend fun renameAlbum(albumId: Long, name: String)

    @Query("UPDATE custom_albums SET isPinned = :pinned WHERE albumId = :albumId")
    suspend fun setPinned(albumId: Long, pinned: Boolean)

    @Query("UPDATE custom_albums SET coverUri = :coverUri WHERE albumId = :albumId")
    suspend fun setCoverUri(albumId: Long, coverUri: String?)

    @Query("UPDATE custom_albums SET colorLabelHex = :colorHex WHERE albumId = :albumId")
    suspend fun updateColorLabel(albumId: Long, colorHex: String)

    @Query("UPDATE custom_albums SET sortOrder = :sortOrder WHERE albumId = :albumId")
    suspend fun updateSortOrder(albumId: Long, sortOrder: Int)

    @Query("DELETE FROM custom_album_members WHERE albumId = :albumId AND mediaStoreId = :mediaStoreId")
    suspend fun removeMember(albumId: Long, mediaStoreId: Long)

    @Query("DELETE FROM custom_album_members WHERE albumId = :albumId")
    suspend fun deleteAllMembersOfAlbum(albumId: Long)

    @Query("DELETE FROM custom_albums WHERE albumId = :albumId")
    suspend fun deleteAlbum(albumId: Long)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY importedAtMillis DESC")
    fun observeAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items ORDER BY importedAtMillis DESC")
    suspend fun getAllSnapshot(): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items WHERE vaultId = :vaultId LIMIT 1")
    suspend fun getById(vaultId: String): VaultItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE vaultId = :vaultId")
    suspend fun deleteById(vaultId: String)
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedAtMillis DESC")
    fun observeAllTrashItems(): Flow<List<TrashItemEntity>>

    @Query("SELECT * FROM trash_items ORDER BY deletedAtMillis DESC")
    suspend fun getAllSnapshot(): List<TrashItemEntity>

    @Query("SELECT * FROM trash_items WHERE trashId = :trashId LIMIT 1")
    suspend fun getById(trashId: String): TrashItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TrashItemEntity)

    @Query("DELETE FROM trash_items WHERE trashId = :trashId")
    suspend fun deleteById(trashId: String)

    @Query("DELETE FROM trash_items")
    suspend fun deleteAll()
}

@Dao
interface VideoPlaybackDao {
    @Query("SELECT * FROM video_playback_state WHERE mediaStoreId = :mediaStoreId LIMIT 1")
    suspend fun getState(mediaStoreId: Long): VideoPlaybackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: VideoPlaybackEntity)
}
