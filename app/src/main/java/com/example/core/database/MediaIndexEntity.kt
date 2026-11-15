package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class AiLabelScore(
    val label: String,
    val confidence: Float,
)

@Serializable
data class AiObjectDetectionInfo(
    val category: String,
    val confidence: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

@Entity(
    tableName = "media_index",
    indices = [
        Index(value = ["dateTakenMillis"]),
        Index(value = ["albumName"]),
        Index(value = ["relativePath"]),
        Index(value = ["format"]),
        Index(value = ["fileSizeBytes"]),
        Index(value = ["userRating"]),
        Index(value = ["isFavorite"]),
        Index(value = ["hasGps"]),
        Index(value = ["isScreenshot"]),
        Index(value = ["isDocument"]),
        Index(value = ["isBlurry"]),
        Index(value = ["sha256Hash"]),
        Index(value = ["groupId"]),
    ],
)
data class MediaIndexEntity(
    @PrimaryKey val mediaStoreId: Long,
    val contentUri: String,
    val fileName: String,
    val displayName: String,
    val mimeType: String,
    val format: String,
    val isVideo: Boolean,
    val bucketId: Long,
    val albumName: String,
    val relativePath: String,
    val dateTakenMillis: Long,
    val dateModifiedMillis: Long,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMillis: Long = 0L,
    // EXIF & Hardware Metadata
    val cameraMake: String = "",
    val cameraModel: String = "",
    val aperture: String = "",
    val iso: Int = 0,
    val exposureTime: String = "",
    val focalLength: String = "",
    val orientation: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasGps: Boolean = false,
    // Visual / Forensic / Quality Signals
    val sha256Hash: String = "",
    val dHash: Long = 0L,
    val colorHistogram: String = "",
    val sharpnessScore: Float = 0f,
    val exposureScore: Float = 0f,
    val qualityScore: Float = 0f,
    val isBlurry: Boolean = false,
    val isScreenshot: Boolean = false,
    val isDocument: Boolean = false,
    val documentConfidence: Float = 0f,
    // On-device AI Extraction
    val ocrText: String = "",
    val aiLabelsText: String = "",
    val aiLabelsJson: String = "[]",
    val aiObjectsText: String = "",
    val aiObjectsJson: String = "[]",
    val generatedCaption: String = "",
    // User Asset Metadata
    val userTags: String = "",
    val userNotes: String = "",
    val userRating: Int = 0,
    val isFavorite: Boolean = false,
    // Grouping & Faces
    val groupId: String = "",
    val isBestShot: Boolean = false,
    val faceCount: Int = 0,
    val aiIndexedAtMillis: Long = 0L,
    val indexVersion: Int = 0,
) {
    val megaPixels: Float
        get() = if (width > 0 && height > 0) (width.toLong() * height.toLong()) / 1_000_000f else 0f

    val resolutionLabel: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else "Unknown"
}
