package com.example.core.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

data class ExtractedExifData(
    val cameraMake: String = "",
    val cameraModel: String = "",
    val aperture: String = "",
    val iso: Int = 0,
    val exposureTime: String = "",
    val focalLength: String = "",
    val orientation: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val exifDateTakenMillis: Long? = null,
)

object ExifMetadataExtractor {

    fun extract(context: Context, uri: Uri, includeOriginalLocation: Boolean): ExtractedExifData {
        val targetUri = if (includeOriginalLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                MediaStore.setRequireOriginal(uri)
            } catch (_: Throwable) {
                uri
            }
        } else {
            uri
        }

        return try {
            context.contentResolver.openInputStream(targetUri)?.use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
                val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, -1.0)
                val apertureStr = if (fNumber > 0) String.format(Locale.US, "f/%.1f", fNumber) else ""
                val isoStr = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED)
                val iso = isoStr?.toIntOrNull() ?: 0
                val exposureDouble = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, -1.0)
                val exposureStr = when {
                    exposureDouble <= 0.0 -> ""
                    exposureDouble < 1.0 -> "1/${(1.0 / exposureDouble).roundToInt().coerceAtLeast(1)}s"
                    else -> String.format(Locale.US, "%.1fs", exposureDouble)
                }
                val focalDouble = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, -1.0)
                val focalStr = if (focalDouble > 0) "${focalDouble.roundToInt()}mm" else ""
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
                val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                val latLong = exif.latLong
                val dateOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?.let { parseExifDate(it) }

                ExtractedExifData(
                    cameraMake = make,
                    cameraModel = model,
                    aperture = apertureStr,
                    iso = iso,
                    exposureTime = exposureStr,
                    focalLength = focalStr,
                    orientation = orientation,
                    width = width,
                    height = height,
                    latitude = latLong?.getOrNull(0),
                    longitude = latLong?.getOrNull(1),
                    exifDateTakenMillis = dateOriginal,
                )
            } ?: ExtractedExifData()
        } catch (_: Throwable) {
            ExtractedExifData()
        }
    }

    fun inferFormat(mimeType: String, fileName: String): String {
        val lowerMime = mimeType.lowercase(Locale.US)
        val ext = fileName.substringAfterLast('.', "").uppercase(Locale.US)
        return when {
            lowerMime.contains("jpeg") || lowerMime.contains("jpg") -> "JPEG"
            lowerMime.contains("png") -> "PNG"
            lowerMime.contains("webp") -> "WebP"
            lowerMime.contains("heic") -> "HEIC"
            lowerMime.contains("heif") -> "HEIF"
            lowerMime.contains("avif") -> "AVIF"
            lowerMime.contains("dng") || lowerMime.contains("raw") -> "RAW"
            lowerMime.contains("gif") -> "GIF"
            lowerMime.contains("mp4") -> "MP4"
            lowerMime.contains("quicktime") || ext == "MOV" -> "MOV"
            lowerMime.contains("matroska") || ext == "MKV" -> "MKV"
            lowerMime.contains("webm") -> "WebM"
            ext.isNotBlank() -> ext
            else -> "MEDIA"
        }
    }

    private fun parseExifDate(raw: String): Long? {
        return try {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(raw)?.time
        } catch (_: Throwable) {
            null
        }
    }
}
