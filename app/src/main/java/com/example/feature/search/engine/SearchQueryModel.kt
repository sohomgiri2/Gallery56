package com.example.feature.search.engine

import com.example.core.database.MediaIndexEntity
import kotlinx.serialization.Serializable

@Serializable
enum class SearchScope(val label: String) {
    ALL("Everything"),
    FILE_NAME("File Name"),
    OCR_TEXT("OCR Text"),
    AI_LABELS("AI Labels & Objects"),
    USER_TAGS("Tags & Notes"),
    EXIF_CAMERA("EXIF & Camera"),
}

@Serializable
enum class MediaTypeFilter(val label: String) {
    ALL("All Media"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
}

@Serializable
enum class OrientationFilter(val label: String) {
    ANY("Any"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait"),
    SQUARE("Square"),
}

@Serializable
enum class SearchSortOption(val label: String) {
    RELEVANCE("Relevance"),
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    SIZE_DESC("Largest Size"),
    QUALITY_DESC("Best Quality"),
    RATING_DESC("Highest Rated"),
    NAME_ASC("Name A–Z"),
}

@Serializable
data class SearchQuery(
    val textQuery: String = "",
    val useSemanticExpansion: Boolean = true,
    val scope: SearchScope = SearchScope.ALL,
    val mediaType: MediaTypeFilter = MediaTypeFilter.ALL,
    val fileNameFilter: String = "",
    val albums: List<String> = emptyList(),
    val folders: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val dateFromMillis: Long? = null,
    val dateToMillis: Long? = null,
    val minFileSizeBytes: Long? = null,
    val maxFileSizeBytes: Long? = null,
    val minMegaPixels: Float? = null,
    val orientation: OrientationFilter = OrientationFilter.ANY,
    val cameraModelQuery: String = "",
    val minIso: Int? = null,
    val maxIso: Int? = null,
    val requiredTags: List<String> = emptyList(),
    val minRating: Int = 0,
    val favoritesOnly: Boolean = false,
    val ocrQuery: String = "",
    val hasOcrTextOnly: Boolean = false,
    val requiredAiLabels: List<String> = emptyList(),
    val documentsOnly: Boolean = false,
    val screenshotsOnly: Boolean = false,
    val blurryOnly: Boolean = false,
    val excludeBlurry: Boolean = false,
    val hasGpsOnly: Boolean = false,
    val gpsCenterLat: Double? = null,
    val gpsCenterLon: Double? = null,
    val gpsRadiusKm: Double? = null,
    val faceGroupIds: List<String> = emptyList(),
    val similarToMediaId: Long? = null,
    val similarityThreshold: Float = 0.72f,
    val sortBy: SearchSortOption = SearchSortOption.RELEVANCE,
) {
    fun activeFilterCount(): Int {
        var count = 0
        if (mediaType != MediaTypeFilter.ALL) count++
        if (fileNameFilter.isNotBlank()) count++
        if (albums.isNotEmpty()) count++
        if (folders.isNotEmpty()) count++
        if (formats.isNotEmpty()) count++
        if (dateFromMillis != null || dateToMillis != null) count++
        if (minFileSizeBytes != null || maxFileSizeBytes != null) count++
        if (minMegaPixels != null || orientation != OrientationFilter.ANY) count++
        if (cameraModelQuery.isNotBlank() || minIso != null || maxIso != null) count++
        if (requiredTags.isNotEmpty()) count++
        if (minRating > 0) count++
        if (favoritesOnly) count++
        if (ocrQuery.isNotBlank() || hasOcrTextOnly) count++
        if (requiredAiLabels.isNotEmpty()) count++
        if (documentsOnly) count++
        if (screenshotsOnly) count++
        if (blurryOnly || excludeBlurry) count++
        if (hasGpsOnly || (gpsCenterLat != null && gpsCenterLon != null)) count++
        if (faceGroupIds.isNotEmpty()) count++
        if (similarToMediaId != null) count++
        return count
    }

    fun buildHumanSummary(): String {
        val parts = mutableListOf<String>()
        if (textQuery.isNotBlank()) parts.add("\"$textQuery\"")
        if (mediaType != MediaTypeFilter.ALL) parts.add(mediaType.label)
        if (albums.isNotEmpty()) parts.add("Albums: ${albums.joinToString()}")
        if (formats.isNotEmpty()) parts.add(formats.joinToString("/"))
        if (minRating > 0) parts.add("${minRating}★+")
        if (favoritesOnly) parts.add("Favorites")
        if (hasGpsOnly || gpsCenterLat != null) parts.add("GPS")
        if (documentsOnly) parts.add("Documents")
        if (screenshotsOnly) parts.add("Screenshots")
        if (similarToMediaId != null) parts.add("Visual Match")
        if (faceGroupIds.isNotEmpty()) parts.add("${faceGroupIds.size} Person(s)")
        return if (parts.isEmpty()) "All Media" else parts.take(4).joinToString(" • ")
    }
}

data class ScoredMediaResult(
    val media: MediaIndexEntity,
    val score: Float,
    val matchReasons: List<String>,
    val similarityPercent: Int? = null,
    val gpsDistanceKm: Double? = null,
)

data class SearchExecutionReport(
    val results: List<ScoredMediaResult>,
    val totalIndexedCount: Int,
    val latencyMillis: Long,
    val expandedTerms: List<String>,
)
