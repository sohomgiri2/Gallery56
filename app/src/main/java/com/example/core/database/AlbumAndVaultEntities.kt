package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "custom_albums")
data class CustomAlbumEntity(
    @PrimaryKey(autoGenerate = true) val albumId: Long = 0L,
    val name: String,
    val description: String = "",
    val coverUri: String? = null,
    val isPinned: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "custom_album_members",
    primaryKeys = ["albumId", "mediaStoreId"],
    indices = [Index(value = ["albumId"]), Index(value = ["mediaStoreId"])],
)
data class CustomAlbumMemberEntity(
    val albumId: Long,
    val mediaStoreId: Long,
    val addedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val vaultId: String,
    val originalMediaStoreId: Long?,
    val displayName: String,
    val mimeType: String,
    val isVideo: Boolean,
    val encryptedFilePath: String,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateTakenMillis: Long,
    val importedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "trash_items")
data class TrashItemEntity(
    @PrimaryKey val trashId: String,
    val originalMediaStoreId: Long,
    val originalUri: String,
    val displayName: String,
    val mimeType: String,
    val format: String,
    val isVideo: Boolean,
    val albumName: String,
    val cachedFilePath: String,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val deletedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "video_playback_state")
data class VideoPlaybackEntity(
    @PrimaryKey val mediaStoreId: Long,
    val positionMillis: Long,
    val durationMillis: Long,
    val playbackSpeed: Float = 1.0f,
    val updatedAtMillis: Long = System.currentTimeMillis(),
)
