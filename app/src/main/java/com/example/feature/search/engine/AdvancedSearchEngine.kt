package com.example.feature.search.engine

import com.example.core.database.FaceDao
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import com.example.feature.intelligence.engine.PerceptualHashAndQualityAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class AdvancedSearchEngine(
    private val mediaIndexDao: MediaIndexDao,
    private val faceDao: FaceDao,
) {

    suspend fun search(query: SearchQuery): SearchExecutionReport = withContext(Dispatchers.Default) {
        val startNs = System.nanoTime()

        val allMedia = mediaIndexDao.getAllMediaSnapshot()
        val rawTokens = SemanticConceptGraph.tokenize(query.textQuery)
        val expandedTokens = SemanticConceptGraph.expandTokens(rawTokens, query.useSemanticExpansion)
        val semanticOnlyTokens = expandedTokens.filterNot { it in rawTokens }

        // Use Room FTS4 index for fast text candidate recall when searching across ALL scope
        val ftsMatchedIds: Set<Long>? = if (rawTokens.isNotEmpty() && query.scope == SearchScope.ALL) {
            val ftsExpr = SemanticConceptGraph.buildSafeFtsMatchQuery(expandedTokens)
            if (ftsExpr != null) {
                try {
                    mediaIndexDao.searchFts(ftsExpr).map { it.mediaStoreId }.toHashSet()
                } catch (_: Throwable) {
                    null
                }
            } else null
        } else null

        val faceFilteredIds: Set<Long>? = if (query.faceGroupIds.isNotEmpty()) {
            faceDao.getMediaIdsForGroups(query.faceGroupIds).toHashSet()
        } else null

        val referenceMedia: MediaIndexEntity? = query.similarToMediaId?.let { id ->
            allMedia.firstOrNull { it.mediaStoreId == id } ?: mediaIndexDao.getById(id)
        }

        val scored = ArrayList<ScoredMediaResult>(allMedia.size)

        for (item in allMedia) {
            if (!matchesStructuredFilters(item, query, faceFilteredIds)) continue

            // Visual similarity filter
            var simPercent: Int? = null
            if (referenceMedia != null) {
                val sim = PerceptualHashAndQualityAnalyzer.visualSimilarityScore(referenceMedia, item)
                if (item.mediaStoreId != referenceMedia.mediaStoreId && sim < query.similarityThreshold) {
                    continue
                }
                simPercent = (sim * 100f).toInt().coerceIn(0, 100)
            }

            // GPS radius filter
            var distanceKm: Double? = null
            if (query.gpsCenterLat != null && query.gpsCenterLon != null) {
                val lat = item.latitude
                val lon = item.longitude
                if (lat == null || lon == null) continue
                distanceKm = haversineKm(query.gpsCenterLat, query.gpsCenterLon, lat, lon)
                val maxRadius = query.gpsRadiusKm ?: 25.0
                if (distanceKm > maxRadius) continue
            }

            // Text & Semantic match evaluation
            val reasons = mutableListOf<String>()
            var relevanceScore = 0f

            if (rawTokens.isNotEmpty()) {
                val matchResult = scoreTextMatch(
                    item = item,
                    scope = query.scope,
                    rawTokens = rawTokens,
                    semanticTokens = semanticOnlyTokens,
                    ftsHit = ftsMatchedIds?.contains(item.mediaStoreId) == true,
                )
                if (matchResult.score <= 0f) continue
                relevanceScore += matchResult.score
                reasons.addAll(matchResult.reasons)
            } else {
                relevanceScore += 10f
            }

            if (simPercent != null) {
                relevanceScore += simPercent * 0.8f
                reasons.add("$simPercent% Visual Match")
            }
            if (distanceKm != null) {
                reasons.add(String.format(Locale.US, "%.1f km GPS", distanceKm))
            }
            if (query.ocrQuery.isNotBlank()) reasons.add("OCR filter")
            if (query.requiredTags.isNotEmpty()) reasons.add("Tag match")
            if (query.requiredAiLabels.isNotEmpty()) reasons.add("AI label")
            if (query.faceGroupIds.isNotEmpty()) reasons.add("Face group")
            if (item.isBestShot) relevanceScore += 4f

            scored.add(
                ScoredMediaResult(
                    media = item,
                    score = relevanceScore,
                    matchReasons = reasons.distinct().take(4),
                    similarityPercent = simPercent,
                    gpsDistanceKm = distanceKm,
                ),
            )
        }

        val sorted = when (query.sortBy) {
            SearchSortOption.RELEVANCE -> scored.sortedWith(
                compareByDescending<ScoredMediaResult> { it.score }
                    .thenByDescending { it.media.dateTakenMillis },
            )
            SearchSortOption.DATE_DESC -> scored.sortedByDescending { it.media.dateTakenMillis }
            SearchSortOption.DATE_ASC -> scored.sortedBy { it.media.dateTakenMillis }
            SearchSortOption.SIZE_DESC -> scored.sortedByDescending { it.media.fileSizeBytes }
            SearchSortOption.QUALITY_DESC -> scored.sortedByDescending { it.media.qualityScore }
            SearchSortOption.RATING_DESC -> scored.sortedWith(
                compareByDescending<ScoredMediaResult> { it.media.userRating }
                    .thenByDescending { it.media.isFavorite }
                    .thenByDescending { it.media.dateTakenMillis },
            )
            SearchSortOption.NAME_ASC -> scored.sortedBy { it.media.fileName.lowercase(Locale.US) }
        }

        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
        SearchExecutionReport(
            results = sorted,
            totalIndexedCount = allMedia.size,
            latencyMillis = elapsedMs,
            expandedTerms = semanticOnlyTokens.take(10),
        )
    }

    private fun matchesStructuredFilters(
        item: MediaIndexEntity,
        q: SearchQuery,
        faceFilteredIds: Set<Long>?,
    ): Boolean {
        when (q.mediaType) {
            MediaTypeFilter.PHOTOS -> if (item.isVideo) return false
            MediaTypeFilter.VIDEOS -> if (!item.isVideo) return false
            MediaTypeFilter.ALL -> Unit
        }

        if (q.fileNameFilter.isNotBlank() &&
            !item.fileName.contains(q.fileNameFilter.trim(), ignoreCase = true)
        ) {
            return false
        }

        if (q.albums.isNotEmpty() && q.albums.none { it.equals(item.albumName, ignoreCase = true) }) {
            return false
        }

        if (q.folders.isNotEmpty() && q.folders.none { item.relativePath.contains(it, ignoreCase = true) }) {
            return false
        }

        if (q.formats.isNotEmpty() && q.formats.none { it.equals(item.format, ignoreCase = true) }) {
            return false
        }

        if (q.dateFromMillis != null && item.dateTakenMillis < q.dateFromMillis) return false
        if (q.dateToMillis != null && item.dateTakenMillis > q.dateToMillis) return false

        if (q.minFileSizeBytes != null && item.fileSizeBytes < q.minFileSizeBytes) return false
        if (q.maxFileSizeBytes != null && item.fileSizeBytes > q.maxFileSizeBytes) return false

        if (q.minMegaPixels != null && item.megaPixels < q.minMegaPixels) return false

        when (q.orientation) {
            OrientationFilter.LANDSCAPE -> if (item.width <= item.height || item.width == 0) return false
            OrientationFilter.PORTRAIT -> if (item.height <= item.width || item.height == 0) return false
            OrientationFilter.SQUARE -> if (item.width == 0 || kotlin.math.abs(item.width - item.height) > item.width * 0.08f) return false
            OrientationFilter.ANY -> Unit
        }

        if (q.cameraModelQuery.isNotBlank()) {
            val camStr = "${item.cameraMake} ${item.cameraModel}".trim()
            if (!camStr.contains(q.cameraModelQuery.trim(), ignoreCase = true)) return false
        }

        if (q.minIso != null && item.iso < q.minIso) return false
        if (q.maxIso != null && (item.iso == 0 || item.iso > q.maxIso)) return false

        if (q.requiredTags.isNotEmpty()) {
            val itemTags = item.userTags.lowercase(Locale.US)
            if (q.requiredTags.any { !itemTags.contains(it.trim().lowercase(Locale.US)) }) return false
        }

        if (q.minRating > 0 && item.userRating < q.minRating) return false
        if (q.favoritesOnly && !item.isFavorite) return false

        if (q.hasOcrTextOnly && item.ocrText.isBlank()) return false
        if (q.ocrQuery.isNotBlank() && !item.ocrText.contains(q.ocrQuery.trim(), ignoreCase = true)) {
            return false
        }

        if (q.requiredAiLabels.isNotEmpty()) {
            val combinedAi = "${item.aiLabelsText} ${item.aiObjectsText}".lowercase(Locale.US)
            if (q.requiredAiLabels.any { !combinedAi.contains(it.trim().lowercase(Locale.US)) }) {
                return false
            }
        }

        if (q.documentsOnly && !item.isDocument) return false
        if (q.screenshotsOnly && !item.isScreenshot) return false
        if (q.blurryOnly && !item.isBlurry) return false
        if (q.excludeBlurry && item.isBlurry) return false
        if (q.hasGpsOnly && !item.hasGps) return false

        if (faceFilteredIds != null && item.mediaStoreId !in faceFilteredIds) return false

        return true
    }

    private data class TextMatchEvaluation(
        val score: Float,
        val reasons: List<String>,
    )

    private fun scoreTextMatch(
        item: MediaIndexEntity,
        scope: SearchScope,
        rawTokens: List<String>,
        semanticTokens: List<String>,
        ftsHit: Boolean,
    ): TextMatchEvaluation {
        val fileLower = "${item.fileName} ${item.albumName} ${item.relativePath} ${item.format}".lowercase(Locale.US)
        val ocrLower = item.ocrText.lowercase(Locale.US)
        val aiLower = "${item.aiLabelsText} ${item.aiObjectsText} ${item.generatedCaption}".lowercase(Locale.US)
        val tagsLower = "${item.userTags} ${item.userNotes}".lowercase(Locale.US)
        val exifLower = "${item.cameraMake} ${item.cameraModel} ${item.aperture} ${item.focalLength}".lowercase(Locale.US)

        var score = 0f
        val reasons = mutableListOf<String>()

        fun evaluateTokens(tokens: List<String>, weight: Float, isSemantic: Boolean) {
            for (token in tokens) {
                when (scope) {
                    SearchScope.FILE_NAME -> {
                        if (fileLower.contains(token)) {
                            score += 18f * weight
                            reasons.add(if (isSemantic) "Concept: $token" else "Name/Folder")
                        }
                    }
                    SearchScope.OCR_TEXT -> {
                        if (ocrLower.contains(token)) {
                            score += 22f * weight
                            reasons.add("OCR: $token")
                        }
                    }
                    SearchScope.AI_LABELS -> {
                        if (aiLower.contains(token)) {
                            score += 20f * weight
                            reasons.add(if (isSemantic) "Semantic AI: $token" else "AI: $token")
                        }
                    }
                    SearchScope.USER_TAGS -> {
                        if (tagsLower.contains(token)) {
                            score += 24f * weight
                            reasons.add("Tag: $token")
                        }
                    }
                    SearchScope.EXIF_CAMERA -> {
                        if (exifLower.contains(token)) {
                            score += 18f * weight
                            reasons.add("EXIF: $token")
                        }
                    }
                    SearchScope.ALL -> {
                        if (tagsLower.contains(token)) {
                            score += 24f * weight
                            reasons.add("Tag: $token")
                        }
                        if (fileLower.contains(token)) {
                            score += 18f * weight
                            reasons.add("File/Album")
                        }
                        if (aiLower.contains(token)) {
                            score += 17f * weight
                            reasons.add(if (isSemantic) "Semantic: $token" else "AI: $token")
                        }
                        if (ocrLower.contains(token)) {
                            score += 16f * weight
                            reasons.add("OCR Text")
                        }
                        if (exifLower.contains(token)) {
                            score += 14f * weight
                            reasons.add("Camera EXIF")
                        }
                    }
                }
            }
        }

        evaluateTokens(rawTokens, weight = 1.0f, isSemantic = false)
        if (semanticTokens.isNotEmpty()) {
            evaluateTokens(semanticTokens, weight = 0.55f, isSemantic = true)
        }
        if (ftsHit && score == 0f) {
            score += 8f
            reasons.add("FTS4 Index")
        }

        return TextMatchEvaluation(score, reasons)
    }

    companion object {
        fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
            return r * c
        }
    }
}
