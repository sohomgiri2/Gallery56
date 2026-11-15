package com.example.feature.intelligence.engine

import com.example.core.database.MediaIndexEntity
import java.util.Locale
import kotlin.math.abs

data class PhotoCluster(
    val clusterId: String,
    val title: String,
    val subtitle: String,
    val items: List<MediaIndexEntity>,
    val bestShotId: Long?,
)

data class SmartAlbumRecommendation(
    val id: String,
    val title: String,
    val reason: String,
    val count: Int,
    val filterTag: String,
    val previewItems: List<MediaIndexEntity>,
)

data class IntelligenceSummary(
    val duplicateClusters: List<PhotoCluster> = emptyList(),
    val similarClusters: List<PhotoCluster> = emptyList(),
    val burstGroups: List<PhotoCluster> = emptyList(),
    val bestShots: List<MediaIndexEntity> = emptyList(),
    val blurryPhotos: List<MediaIndexEntity> = emptyList(),
    val documents: List<MediaIndexEntity> = emptyList(),
    val screenshots: List<MediaIndexEntity> = emptyList(),
    val recommendations: List<SmartAlbumRecommendation> = emptyList(),
)

object SmartCollectionsEngine {

    fun buildSummary(allMedia: List<MediaIndexEntity>, faceGroupCount: Int): IntelligenceSummary {
        if (allMedia.isEmpty()) return IntelligenceSummary()

        // 1. Exact SHA-256 Duplicates
        val duplicateClusters = allMedia
            .filter { it.sha256Hash.isNotBlank() }
            .groupBy { it.sha256Hash }
            .values
            .filter { it.size >= 2 }
            .mapIndexed { idx, group ->
                val sorted = group.sortedByDescending { it.qualityScore }
                PhotoCluster(
                    clusterId = "dup_$idx",
                    title = "Exact Duplicate #${idx + 1}",
                    subtitle = "${group.size} identical files • SHA-256 match",
                    items = sorted,
                    bestShotId = sorted.firstOrNull()?.mediaStoreId,
                )
            }

        // 2. Visually Similar Photos (dHash Hamming <= 10 & color cosine >= 0.88)
        val indexedImages = allMedia.filter { !it.isVideo && it.dHash != 0L }
        val visitedSimilar = HashSet<Long>()
        val similarClusters = mutableListOf<PhotoCluster>()
        for (i in indexedImages.indices) {
            val anchor = indexedImages[i]
            if (anchor.mediaStoreId in visitedSimilar) continue
            val group = mutableListOf(anchor)
            for (j in i + 1 until indexedImages.size) {
                val cand = indexedImages[j]
                if (cand.mediaStoreId in visitedSimilar) continue
                if (anchor.sha256Hash.isNotEmpty() && anchor.sha256Hash == cand.sha256Hash) continue
                val hamming = PerceptualHashAndQualityAnalyzer.hammingDistance(anchor.dHash, cand.dHash)
                val sim = PerceptualHashAndQualityAnalyzer.visualSimilarityScore(anchor, cand)
                if (hamming <= 10 || sim >= 0.88f) {
                    group.add(cand)
                    visitedSimilar.add(cand.mediaStoreId)
                }
            }
            if (group.size >= 2) {
                visitedSimilar.add(anchor.mediaStoreId)
                val ranked = group.sortedByDescending { it.qualityScore }
                similarClusters.add(
                    PhotoCluster(
                        clusterId = "sim_${similarClusters.size}",
                        title = "Similar Series #${similarClusters.size + 1}",
                        subtitle = "${group.size} visually aligned frames",
                        items = ranked,
                        bestShotId = ranked.firstOrNull()?.mediaStoreId,
                    ),
                )
            }
        }

        // 3. Temporal + Visual Burst / Event Grouping & Best-Shot Selection
        val sortedByTime = indexedImages.sortedBy { it.dateTakenMillis }
        val burstGroups = mutableListOf<PhotoCluster>()
        val currentBurst = mutableListOf<MediaIndexEntity>()
        for (item in sortedByTime) {
            if (currentBurst.isEmpty()) {
                currentBurst.add(item)
            } else {
                val prev = currentBurst.last()
                val deltaMillis = abs(item.dateTakenMillis - prev.dateTakenMillis)
                val hamming = PerceptualHashAndQualityAnalyzer.hammingDistance(item.dHash, prev.dHash)
                if (deltaMillis <= 90_000L && hamming <= 16) {
                    currentBurst.add(item)
                } else {
                    if (currentBurst.size >= 2) {
                        burstGroups.add(buildBurstCluster(burstGroups.size, currentBurst.toList()))
                    }
                    currentBurst.clear()
                    currentBurst.add(item)
                }
            }
        }
        if (currentBurst.size >= 2) {
            burstGroups.add(buildBurstCluster(burstGroups.size, currentBurst.toList()))
        }

        val bestShots = (burstGroups + similarClusters)
            .mapNotNull { cluster -> cluster.items.firstOrNull { it.mediaStoreId == cluster.bestShotId } }
            .distinctBy { it.mediaStoreId }
            .ifEmpty {
                indexedImages
                    .filter { !it.isBlurry && it.qualityScore >= 60f }
                    .sortedByDescending { it.qualityScore }
                    .take(12)
            }

        val blurry = allMedia.filter { !it.isVideo && it.aiIndexedAtMillis > 0L && it.isBlurry }
            .sortedBy { it.sharpnessScore }
        val documents = allMedia.filter { it.isDocument }.sortedByDescending { it.documentConfidence }
        val screenshots = allMedia.filter { it.isScreenshot }.sortedByDescending { it.dateTakenMillis }

        // 4. Smart Album Recommendations
        val recs = mutableListOf<SmartAlbumRecommendation>()
        if (documents.isNotEmpty()) {
            recs.add(
                SmartAlbumRecommendation(
                    id = "rec_docs",
                    title = "Documents & Receipts",
                    reason = "Detected via on-device OCR density & paper contrast",
                    count = documents.size,
                    filterTag = "documents",
                    previewItems = documents.take(4),
                ),
            )
        }
        if (screenshots.isNotEmpty()) {
            recs.add(
                SmartAlbumRecommendation(
                    id = "rec_screenshots",
                    title = "Screen Captures",
                    reason = "Auto-classified from folder/aspect/EXIF profile",
                    count = screenshots.size,
                    filterTag = "screenshots",
                    previewItems = screenshots.take(4),
                ),
            )
        }
        if (bestShots.isNotEmpty()) {
            recs.add(
                SmartAlbumRecommendation(
                    id = "rec_best",
                    title = "Best Shots",
                    reason = "Highest Laplacian sharpness & balanced exposure",
                    count = bestShots.size,
                    filterTag = "best_shots",
                    previewItems = bestShots.take(4),
                ),
            )
        }
        if (blurry.isNotEmpty()) {
            recs.add(
                SmartAlbumRecommendation(
                    id = "rec_blurry",
                    title = "Soft / Blurry Review",
                    reason = "Low edge-variance frames flagged for review",
                    count = blurry.size,
                    filterTag = "blurry",
                    previewItems = blurry.take(4),
                ),
            )
        }
        val natureItems = allMedia.filter { item ->
            val labels = item.aiLabelsText.lowercase(Locale.US)
            labels.contains("sky") || labels.contains("mountain") || labels.contains("tree") ||
                labels.contains("nature") || labels.contains("plant") || labels.contains("water")
        }
        if (natureItems.isNotEmpty()) {
            recs.add(
                SmartAlbumRecommendation(
                    id = "rec_nature",
                    title = "Outdoors & Scenery",
                    reason = "Indexed from ML Kit landscape & nature labels",
                    count = natureItems.size,
                    filterTag = "nature",
                    previewItems = natureItems.take(4),
                ),
            )
        }
        val gpsItems = allMedia.filter { it.hasGps }
        if (gpsItems.isNotEmpty()) {
            recs.add(
                SmartAlbumRecommendation(
                    id = "rec_gps",
                    title = "Geo-Tagged Captures",
                    reason = "EXIF GPS coordinates available offline",
                    count = gpsItems.size,
                    filterTag = "gps",
                    previewItems = gpsItems.take(4),
                ),
            )
        }
        if (faceGroupCount > 0) {
            val peopleItems = allMedia.filter { it.faceCount > 0 }
            if (peopleItems.isNotEmpty()) {
                recs.add(
                    SmartAlbumRecommendation(
                        id = "rec_people",
                        title = "Portraits & People",
                        reason = "$faceGroupCount local face clusters organized on-device",
                        count = peopleItems.size,
                        filterTag = "people",
                        previewItems = peopleItems.take(4),
                    ),
                )
            }
        }

        return IntelligenceSummary(
            duplicateClusters = duplicateClusters,
            similarClusters = similarClusters,
            burstGroups = burstGroups,
            bestShots = bestShots,
            blurryPhotos = blurry,
            documents = documents,
            screenshots = screenshots,
            recommendations = recs,
        )
    }

    private fun buildBurstCluster(index: Int, rawItems: List<MediaIndexEntity>): PhotoCluster {
        val ranked = rawItems.sortedByDescending { it.qualityScore + (if (it.faceCount > 0) 8f else 0f) }
        return PhotoCluster(
            clusterId = "burst_$index",
            title = "Photo Group #${index + 1}",
            subtitle = "${rawItems.size} shots • Best score ${ranked.first().qualityScore.toInt()}/100",
            items = ranked,
            bestShotId = ranked.firstOrNull()?.mediaStoreId,
        )
    }
}
