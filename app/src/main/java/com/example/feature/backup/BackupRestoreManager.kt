package com.example.feature.backup

import android.content.Context
import android.net.Uri
import com.example.core.database.CustomAlbumDao
import com.example.core.database.CustomAlbumEntity
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import com.example.core.database.SavedSearchDao
import com.example.core.database.SavedSearchEntity
import com.example.core.datastore.GalleryPreferences
import com.example.core.datastore.GalleryPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Serializable
data class BackupSavedSearchRecord(
    val name: String,
    val summary: String,
    val queryJson: String,
    val isPinned: Boolean,
)

@Serializable
data class BackupAlbumRecord(
    val name: String,
    val description: String,
    val isPinned: Boolean,
)

@Serializable
data class BackupMediaAnnotationRecord(
    val fileName: String,
    val userTags: String,
    val userNotes: String,
    val userRating: Int,
    val isFavorite: Boolean,
)

@Serializable
data class Gallery56BackupBundle(
    val schemaVersion: Int = 1,
    val appPackage: String = "com.sohomgiri.gallery56",
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val preferences: GalleryPreferences,
    val savedSearches: List<BackupSavedSearchRecord> = emptyList(),
    val customAlbums: List<BackupAlbumRecord> = emptyList(),
    val mediaAnnotations: List<BackupMediaAnnotationRecord> = emptyList(),
)

data class BackupValidationPreview(
    val bundle: Gallery56BackupBundle,
    val customThemeCount: Int,
    val savedSearchCount: Int,
    val albumCount: Int,
    val annotationCount: Int,
    val isValid: Boolean,
    val validationNote: String,
)

class BackupRestoreManager(
    private val context: Context,
    private val preferencesManager: GalleryPreferencesManager,
    private val savedSearchDao: SavedSearchDao,
    private val customAlbumDao: CustomAlbumDao,
    private val mediaIndexDao: MediaIndexDao,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun buildBackupJson(): String = withContext(Dispatchers.IO) {
        val prefs = preferencesManager.preferences.first().copy(
            vaultPinHash = "",
            vaultPinSalt = "",
            vaultFailedAttempts = 0,
            vaultLockoutUntilMillis = 0L,
        )
        val searches = savedSearchDao.observeAllSavedSearches().first().map {
            BackupSavedSearchRecord(it.name, it.summary, it.queryJson, it.isPinned)
        }
        val albums = customAlbumDao.getAllAlbumsSnapshot().map {
            BackupAlbumRecord(it.name, it.description, it.isPinned)
        }
        val annotations = mediaIndexDao.getAllMediaSnapshot()
            .filter { it.userTags.isNotBlank() || it.userNotes.isNotBlank() || it.userRating > 0 || it.isFavorite }
            .map {
                BackupMediaAnnotationRecord(
                    fileName = it.fileName,
                    userTags = it.userTags,
                    userNotes = it.userNotes,
                    userRating = it.userRating,
                    isFavorite = it.isFavorite,
                )
            }

        val bundle = Gallery56BackupBundle(
            preferences = prefs,
            savedSearches = searches,
            customAlbums = albums,
            mediaAnnotations = annotations,
        )
        json.encodeToString(bundle)
    }

    suspend fun exportJsonToUri(destUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = buildBackupJson()
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                out.write(payload.toByteArray(Charsets.UTF_8))
            } ?: return@withContext false
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun parseAndValidateBackupUri(srcUri: Uri): BackupValidationPreview? = withContext(Dispatchers.IO) {
        try {
            val raw = context.contentResolver.openInputStream(srcUri)?.use {
                it.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext null
            validateBackupJson(raw)
        } catch (_: Throwable) {
            null
        }
    }

    fun validateBackupJson(rawJson: String): BackupValidationPreview? {
        return try {
            val bundle = json.decodeFromString<Gallery56BackupBundle>(rawJson)
            val valid = bundle.schemaVersion in 1..2 && bundle.appPackage.contains("gallery56")
            BackupValidationPreview(
                bundle = bundle,
                customThemeCount = bundle.preferences.customThemes.size,
                savedSearchCount = bundle.savedSearches.size,
                albumCount = bundle.customAlbums.size,
                annotationCount = bundle.mediaAnnotations.size,
                isValid = valid,
                validationNote = if (valid) {
                    "Valid Gallery56 backup bundle (schema v${bundle.schemaVersion})"
                } else {
                    "Unsupported schema version (${bundle.schemaVersion})"
                },
            )
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun applyBackupBundle(bundle: Gallery56BackupBundle) = withContext(Dispatchers.IO) {
        preferencesManager.applyImportedSettings(bundle.preferences)

        val existingSearches = savedSearchDao.observeAllSavedSearches().first().map { it.name }.toSet()
        bundle.savedSearches.forEach { rec ->
            if (rec.name !in existingSearches) {
                savedSearchDao.upsert(
                    SavedSearchEntity(
                        name = rec.name,
                        summary = rec.summary,
                        queryJson = rec.queryJson,
                        isPinned = rec.isPinned,
                    ),
                )
            }
        }

        val existingAlbums = customAlbumDao.getAllAlbumsSnapshot().map { it.name }.toSet()
        bundle.customAlbums.forEach { rec ->
            if (rec.name !in existingAlbums) {
                customAlbumDao.upsertAlbum(
                    CustomAlbumEntity(
                        name = rec.name,
                        description = rec.description,
                        isPinned = rec.isPinned,
                    ),
                )
            }
        }

        val mediaByName = mediaIndexDao.getAllMediaSnapshot().associateBy { it.fileName }
        bundle.mediaAnnotations.forEach { ann ->
            val match = mediaByName[ann.fileName]
            if (match != null) {
                mediaIndexDao.updateUserMetadata(match.mediaStoreId, ann.userTags, ann.userNotes)
                mediaIndexDao.setRating(match.mediaStoreId, ann.userRating)
                mediaIndexDao.setFavorite(match.mediaStoreId, ann.isFavorite)
            }
        }
    }

    suspend fun exportSelectedMediaZip(destUri: Uri, items: List<MediaIndexEntity>): Int = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext 0
        var written = 0
        try {
            context.contentResolver.openOutputStream(destUri)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    val seenNames = HashSet<String>()
                    for (item in items) {
                        var entryName = item.displayName.ifBlank { "media_${item.mediaStoreId}" }
                        if (!seenNames.add(entryName)) {
                            entryName = "${item.mediaStoreId}_$entryName"
                        }
                        context.contentResolver.openInputStream(Uri.parse(item.contentUri))?.use { input ->
                            zip.putNextEntry(ZipEntry(entryName))
                            input.copyTo(zip)
                            zip.closeEntry()
                            written++
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            return@withContext written
        }
        written
    }
}
