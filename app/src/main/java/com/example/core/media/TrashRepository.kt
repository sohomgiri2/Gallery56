package com.example.core.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import com.example.core.database.TrashDao
import com.example.core.database.TrashItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class TrashRepository(
    private val context: Context,
    private val trashDao: TrashDao,
    private val mediaIndexDao: MediaIndexDao,
) {
    private fun trashDir(): File = File(context.filesDir, "trash_bin").apply { mkdirs() }

    suspend fun moveItemsToTrash(items: List<MediaIndexEntity>): Int = withContext(Dispatchers.IO) {
        var moved = 0
        for (item in items) {
            val trashId = "tr_${UUID.randomUUID().toString().take(10)}"
            val destFile = File(trashDir(), "${trashId}_${item.displayName}")
            val copied = try {
                context.contentResolver.openInputStream(Uri.parse(item.contentUri))?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                    true
                } ?: false
            } catch (_: Throwable) {
                false
            }
            if (copied && destFile.exists()) {
                trashDao.upsert(
                    TrashItemEntity(
                        trashId = trashId,
                        originalMediaStoreId = item.mediaStoreId,
                        originalUri = item.contentUri,
                        displayName = item.displayName,
                        mimeType = item.mimeType,
                        format = item.format,
                        isVideo = item.isVideo,
                        albumName = item.albumName,
                        cachedFilePath = destFile.absolutePath,
                        fileSizeBytes = item.fileSizeBytes.coerceAtLeast(destFile.length()),
                        width = item.width,
                        height = item.height,
                    ),
                )
                moved++
            }
        }
        if (moved > 0) {
            mediaIndexDao.deleteByIds(items.map { it.mediaStoreId })
        }
        moved
    }

    suspend fun restoreFromTrash(trashItem: TrashItemEntity): Boolean = withContext(Dispatchers.IO) {
        val file = File(trashItem.cachedFilePath)
        if (!file.exists()) {
            trashDao.deleteById(trashItem.trashId)
            return@withContext false
        }
        val restored = try {
            val collection = if (trashItem.isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val baseDir = if (trashItem.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, trashItem.displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, trashItem.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$baseDir/Gallery56_Restored")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val destUri = context.contentResolver.insert(collection, values) ?: return@withContext false
                FileInputStream(file).use { input ->
                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(destUri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(baseDir), "Gallery56_Restored").apply { mkdirs() }
                val dest = File(dir, trashItem.displayName)
                file.copyTo(dest, overwrite = true)
                true
            }
        } catch (_: Throwable) {
            false
        }

        if (restored) {
            file.delete()
            trashDao.deleteById(trashItem.trashId)
        }
        restored
    }

    suspend fun deletePermanently(trashItem: TrashItemEntity) = withContext(Dispatchers.IO) {
        File(trashItem.cachedFilePath).delete()
        trashDao.deleteById(trashItem.trashId)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        trashDao.getAllSnapshot().forEach { File(it.cachedFilePath).delete() }
        trashDao.deleteAll()
    }

    fun totalTrashBytes(): Long {
        return trashDir().listFiles()?.sumOf { it.length() } ?: 0L
    }
}
