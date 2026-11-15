package com.example.core.media

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class MediaSyncSummary(
    val totalDiscovered: Int,
    val imageCount: Int,
    val videoCount: Int,
    val accessLevel: MediaAccessLevel,
)

class DeviceMediaRepository(
    private val context: Context,
    private val mediaIndexDao: MediaIndexDao,
) {

    suspend fun syncDeviceMedia(): MediaSyncSummary = withContext(Dispatchers.IO) {
        val accessLevel = MediaPermissionHelper.checkAccessLevel(context)
        if (accessLevel == MediaAccessLevel.DENIED) {
            return@withContext MediaSyncSummary(0, 0, 0, accessLevel)
        }

        val existingMap = mediaIndexDao.getAllMediaSnapshot().associateBy { it.mediaStoreId }
        val discovered = mutableListOf<MediaIndexEntity>()
        val includeGps = MediaPermissionHelper.hasMediaLocationAccess(context)

        val images = queryImages(existingMap, includeGps)
        val videos = queryVideos(existingMap, includeGps)
        discovered.addAll(images)
        discovered.addAll(videos)

        // Preserve any Photo-Picker imported items (negative synthetic IDs)
        val pickerItems = existingMap.values.filter { it.mediaStoreId < 0L }

        if (discovered.isNotEmpty()) {
            mediaIndexDao.upsertAll(discovered)
        }

        val activeIds = (discovered.map { it.mediaStoreId } + pickerItems.map { it.mediaStoreId }).distinct()
        if (activeIds.isNotEmpty() && accessLevel == MediaAccessLevel.FULL_ACCESS) {
            // Only prune missing MediaStore rows when full access is active so partial access doesn't wipe picked items
            if (activeIds.size <= 900) {
                mediaIndexDao.deleteMissingMedia(activeIds)
            }
        }

        MediaSyncSummary(
            totalDiscovered = discovered.size + pickerItems.size,
            imageCount = images.size,
            videoCount = videos.size,
            accessLevel = accessLevel,
        )
    }

    suspend fun importPickerUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0
        val includeGps = MediaPermissionHelper.hasMediaLocationAccess(context)
        val imported = mutableListOf<MediaIndexEntity>()

        for (uri in uris) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Throwable) {
                // Non-persistable picker URIs remain readable for current session
            }

            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val isVideo = mimeType.startsWith("video/", ignoreCase = true)
            var displayName = uri.lastPathSegment ?: "Picked_Media"
            var fileSize = 0L

            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                }
            }

            val exif = if (!isVideo) {
                ExifMetadataExtractor.extract(context, uri, includeGps)
            } else {
                ExtractedExifData()
            }

            val syntheticId = -kotlin.math.abs(uri.toString().hashCode().toLong()).coerceAtLeast(1L)
            val now = System.currentTimeMillis()
            val format = ExifMetadataExtractor.inferFormat(mimeType, displayName)
            val lowerName = displayName.lowercase(Locale.US)
            val looksLikeScreenshot = lowerName.contains("screenshot") || lowerName.contains("screen_shot")

            imported.add(
                MediaIndexEntity(
                    mediaStoreId = syntheticId,
                    contentUri = uri.toString(),
                    fileName = displayName,
                    displayName = displayName,
                    mimeType = mimeType.ifBlank { if (isVideo) "video/mp4" else "image/jpeg" },
                    format = format,
                    isVideo = isVideo,
                    bucketId = -1L,
                    albumName = "Photo Picker",
                    relativePath = "PhotoPicker/",
                    dateTakenMillis = exif.exifDateTakenMillis ?: now,
                    dateModifiedMillis = now,
                    fileSizeBytes = fileSize,
                    width = exif.width,
                    height = exif.height,
                    cameraMake = exif.cameraMake,
                    cameraModel = exif.cameraModel,
                    aperture = exif.aperture,
                    iso = exif.iso,
                    exposureTime = exif.exposureTime,
                    focalLength = exif.focalLength,
                    orientation = exif.orientation,
                    latitude = exif.latitude,
                    longitude = exif.longitude,
                    hasGps = exif.latitude != null && exif.longitude != null,
                    isScreenshot = looksLikeScreenshot,
                ),
            )
        }

        if (imported.isNotEmpty()) {
            mediaIndexDao.upsertAll(imported)
        }
        imported.size
    }

    private fun queryImages(
        existingMap: Map<Long, MediaIndexEntity>,
        includeGps: Boolean,
    ): List<MediaIndexEntity> {
        val result = mutableListOf<MediaIndexEntity>()
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.MIME_TYPE)
            add(MediaStore.Images.Media.SIZE)
            add(MediaStore.Images.Media.WIDTH)
            add(MediaStore.Images.Media.HEIGHT)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.DATE_MODIFIED)
            add(MediaStore.Images.Media.BUCKET_ID)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
                add(MediaStore.Images.Media.IS_FAVORITE)
            }
        }.toTypedArray()

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            } else -1
            val favCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Images.Media.IS_FAVORITE)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val displayName = cursor.getString(nameCol) ?: "IMG_$id"
                val mimeType = cursor.getString(mimeCol) ?: "image/jpeg"
                val fileSize = cursor.getLong(sizeCol)
                var width = cursor.getInt(widthCol)
                var height = cursor.getInt(heightCol)
                val modMillis = cursor.getLong(modCol) * 1000L
                val dateTaken = cursor.getLong(takenCol).let { if (it > 0L) it else modMillis }
                val bucketId = cursor.getLong(bucketIdCol)
                val albumName = cursor.getString(bucketNameCol) ?: "Internal"
                val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) ?: "$albumName/" else "$albumName/"
                val osFav = if (favCol >= 0) cursor.getInt(favCol) == 1 else false

                val existing = existingMap[id]
                val unchanged = existing != null && existing.dateModifiedMillis == modMillis && existing.fileSizeBytes == fileSize

                val exif = if (unchanged && existing != null && existing.cameraModel.isNotEmpty()) {
                    null
                } else {
                    ExifMetadataExtractor.extract(context, uri, includeGps)
                }

                if (width <= 0 && exif != null && exif.width > 0) width = exif.width
                if (height <= 0 && exif != null && exif.height > 0) height = exif.height

                val lowerPath = relPath.lowercase(Locale.US)
                val lowerName = displayName.lowercase(Locale.US)
                val isScreenshot = existing?.isScreenshot == true ||
                    lowerPath.contains("screenshot") ||
                    albumName.equals("Screenshots", ignoreCase = true) ||
                    lowerName.startsWith("screenshot")

                val entity = if (existing != null) {
                    existing.copy(
                        contentUri = uri.toString(),
                        fileName = displayName,
                        displayName = displayName,
                        mimeType = mimeType,
                        format = ExifMetadataExtractor.inferFormat(mimeType, displayName),
                        bucketId = bucketId,
                        albumName = albumName,
                        relativePath = relPath,
                        dateTakenMillis = dateTaken,
                        dateModifiedMillis = modMillis,
                        fileSizeBytes = fileSize,
                        width = width,
                        height = height,
                        isFavorite = existing.isFavorite || osFav,
                        isScreenshot = isScreenshot,
                        cameraMake = exif?.cameraMake ?: existing.cameraMake,
                        cameraModel = exif?.cameraModel ?: existing.cameraModel,
                        aperture = exif?.aperture ?: existing.aperture,
                        iso = exif?.iso ?: existing.iso,
                        exposureTime = exif?.exposureTime ?: existing.exposureTime,
                        focalLength = exif?.focalLength ?: existing.focalLength,
                        orientation = exif?.orientation ?: existing.orientation,
                        latitude = exif?.latitude ?: existing.latitude,
                        longitude = exif?.longitude ?: existing.longitude,
                        hasGps = (exif?.latitude ?: existing.latitude) != null &&
                            (exif?.longitude ?: existing.longitude) != null,
                    )
                } else {
                    MediaIndexEntity(
                        mediaStoreId = id,
                        contentUri = uri.toString(),
                        fileName = displayName,
                        displayName = displayName,
                        mimeType = mimeType,
                        format = ExifMetadataExtractor.inferFormat(mimeType, displayName),
                        isVideo = false,
                        bucketId = bucketId,
                        albumName = albumName,
                        relativePath = relPath,
                        dateTakenMillis = dateTaken,
                        dateModifiedMillis = modMillis,
                        fileSizeBytes = fileSize,
                        width = width,
                        height = height,
                        cameraMake = exif?.cameraMake.orEmpty(),
                        cameraModel = exif?.cameraModel.orEmpty(),
                        aperture = exif?.aperture.orEmpty(),
                        iso = exif?.iso ?: 0,
                        exposureTime = exif?.exposureTime.orEmpty(),
                        focalLength = exif?.focalLength.orEmpty(),
                        orientation = exif?.orientation ?: 0,
                        latitude = exif?.latitude,
                        longitude = exif?.longitude,
                        hasGps = exif?.latitude != null && exif.longitude != null,
                        isScreenshot = isScreenshot,
                        isFavorite = osFav,
                    )
                }
                result.add(entity)
            }
        }
        return result
    }

    private fun queryVideos(
        existingMap: Map<Long, MediaIndexEntity>,
        includeGps: Boolean,
    ): List<MediaIndexEntity> {
        val result = mutableListOf<MediaIndexEntity>()
        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.MIME_TYPE)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.WIDTH)
            add(MediaStore.Video.Media.HEIGHT)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.DATE_TAKEN)
            add(MediaStore.Video.Media.DATE_MODIFIED)
            add(MediaStore.Video.Media.BUCKET_ID)
            add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
                add(MediaStore.Video.Media.IS_FAVORITE)
            }
        }.toTypedArray()

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
            } else -1
            val favCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Video.Media.IS_FAVORITE)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val displayName = cursor.getString(nameCol) ?: "VID_$id"
                val mimeType = cursor.getString(mimeCol) ?: "video/mp4"
                val fileSize = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val duration = cursor.getLong(durCol)
                val modMillis = cursor.getLong(modCol) * 1000L
                val dateTaken = cursor.getLong(takenCol).let { if (it > 0L) it else modMillis }
                val bucketId = cursor.getLong(bucketIdCol)
                val albumName = cursor.getString(bucketNameCol) ?: "Videos"
                val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) ?: "$albumName/" else "$albumName/"
                val osFav = if (favCol >= 0) cursor.getInt(favCol) == 1 else false

                val existing = existingMap[id]
                val entity = if (existing != null) {
                    existing.copy(
                        contentUri = uri.toString(),
                        fileName = displayName,
                        displayName = displayName,
                        mimeType = mimeType,
                        format = ExifMetadataExtractor.inferFormat(mimeType, displayName),
                        isVideo = true,
                        bucketId = bucketId,
                        albumName = albumName,
                        relativePath = relPath,
                        dateTakenMillis = dateTaken,
                        dateModifiedMillis = modMillis,
                        fileSizeBytes = fileSize,
                        width = width,
                        height = height,
                        durationMillis = duration,
                        isFavorite = existing.isFavorite || osFav,
                    )
                } else {
                    MediaIndexEntity(
                        mediaStoreId = id,
                        contentUri = uri.toString(),
                        fileName = displayName,
                        displayName = displayName,
                        mimeType = mimeType,
                        format = ExifMetadataExtractor.inferFormat(mimeType, displayName),
                        isVideo = true,
                        bucketId = bucketId,
                        albumName = albumName,
                        relativePath = relPath,
                        dateTakenMillis = dateTaken,
                        dateModifiedMillis = modMillis,
                        fileSizeBytes = fileSize,
                        width = width,
                        height = height,
                        durationMillis = duration,
                        isFavorite = osFav,
                    )
                }
                result.add(entity)
            }
        }
        return result
    }
}
