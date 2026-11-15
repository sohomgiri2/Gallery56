package com.example.core.media

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.core.database.MediaIndexEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object MediaStoreOperations {

    suspend fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        subFolder: String = "Gallery56",
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 95,
    ): Uri? = withContext(Dispatchers.IO) {
        val ext = when (format) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP_LOSSY, Bitmap.CompressFormat.WEBP_LOSSLESS -> "webp"
            else -> "jpg"
        }
        val mime = when (format) {
            Bitmap.CompressFormat.PNG -> "image/png"
            Bitmap.CompressFormat.WEBP_LOSSY, Bitmap.CompressFormat.WEBP_LOSSLESS -> "image/webp"
            else -> "image/jpeg"
        }
        val safeName = if (fileName.endsWith(".$ext", ignoreCase = true)) fileName else "$fileName.$ext"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, safeName)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$subFolder")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext null
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(format, quality, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    subFolder,
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(format, quality, out)
                }
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, safeName)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    @Suppress("DEPRECATION")
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun copyMediaToAlbumFolder(
        context: Context,
        item: MediaIndexEntity,
        targetFolder: String,
    ): Uri? = withContext(Dispatchers.IO) {
        val sourceUri = Uri.parse(item.contentUri)
        val cleanFolder = targetFolder.trim().ifBlank { "Gallery56_Copied" }
        try {
            val baseDir = if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            val collection = if (item.isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "copy_${item.displayName}")
                    put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$baseDir/$cleanFolder")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val destUri = context.contentResolver.insert(collection, values) ?: return@withContext null
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(destUri, values, null, null)
                destUri
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(baseDir), cleanFolder).apply { mkdirs() }
                val destFile = File(dir, "copy_${item.displayName}")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(destFile)
            }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun stripGpsAndSavePrivacyCopy(
        context: Context,
        item: MediaIndexEntity,
    ): Uri? = withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext null
        try {
            val bmp = context.contentResolver.openInputStream(Uri.parse(item.contentUri))?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@withContext null
            try {
                saveBitmapToMediaStore(
                    context = context,
                    bitmap = bmp,
                    fileName = "clean_${item.displayName.substringBeforeLast('.')}",
                    subFolder = "Gallery56_CleanExport",
                    format = Bitmap.CompressFormat.JPEG,
                    quality = 96,
                )
            } finally {
                bmp.recycle()
            }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun setAsWallpaper(context: Context, uriString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val bmp = context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@withContext false
            try {
                WallpaperManager.getInstance(context).setBitmap(bmp)
                true
            } finally {
                bmp.recycle()
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun createMediaStoreDeleteIntentSender(context: Context, uris: List<Uri>): IntentSender? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    fun shareMultipleMedia(context: Context, items: List<MediaIndexEntity>) {
        if (items.isEmpty()) return
        val uris = ArrayList(items.map { Uri.parse(it.contentUri) })
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = items.first().mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share ${items.size} item(s) via Gallery56"))
    }
}
