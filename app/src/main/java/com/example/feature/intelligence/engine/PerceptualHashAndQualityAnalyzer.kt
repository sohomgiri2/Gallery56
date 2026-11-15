package com.example.feature.intelligence.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.example.core.database.MediaIndexEntity
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

data class VisualQualityAnalysis(
    val dHash: Long,
    val colorHistogram: FloatArray,
    val sharpnessScore: Float,
    val exposureScore: Float,
    val qualityScore: Float,
    val isBlurry: Boolean,
    val neutralBrightRatio: Float,
)

object PerceptualHashAndQualityAnalyzer {

    const val BLUR_SHARPNESS_THRESHOLD = 42f

    fun computeSha256(context: Context, uri: Uri): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int
                var totalRead = 0L
                val maxBytesForFastHash = 32L * 1024L * 1024L // cap stream read at 32MB for responsiveness
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalRead >= maxBytesForFastHash) break
                }
            } ?: return ""
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {
            ""
        }
    }

    fun analyzeBitmap(bitmap: Bitmap): VisualQualityAnalysis {
        val dHash = computeDHash(bitmap)
        val histogram = computeColorHistogram(bitmap)

        val targetSize = 144
        val scaled = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        val w = scaled.width
        val h = scaled.height
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)

        val luma = FloatArray(w * h)
        var lumaSum = 0.0
        var neutralBrightCount = 0
        var clippedCount = 0

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val y = (0.299f * r + 0.587f * g + 0.114f * b)
            luma[i] = y
            lumaSum += y

            val maxC = maxOf(r, g, b)
            val minC = minOf(r, g, b)
            if (y > 185f && (maxC - minC) < 28) {
                neutralBrightCount++
            }
            if (y < 8f || y > 249f) {
                clippedCount++
            }
        }

        // Laplacian variance for sharpness / blur detection
        var lapSum = 0.0
        var lapSqSum = 0.0
        var lapCount = 0
        for (row in 1 until h - 1) {
            val rowOffset = row * w
            for (col in 1 until w - 1) {
                val idx = rowOffset + col
                val lap = (
                    -luma[idx - w] - luma[idx - 1] + 4f * luma[idx] - luma[idx + 1] - luma[idx + w]
                    ).toDouble()
                lapSum += lap
                lapSqSum += lap * lap
                lapCount++
            }
        }
        val meanLap = if (lapCount > 0) lapSum / lapCount else 0.0
        val varianceLap = if (lapCount > 0) (lapSqSum / lapCount - meanLap * meanLap).coerceAtLeast(0.0) else 0.0
        val sharpness = sqrt(varianceLap).toFloat() * 4.2f

        val meanLumaNorm = (lumaSum / pixels.size.coerceAtLeast(1)) / 255.0
        val exposureBalance = (1.0 - abs(meanLumaNorm - 0.48) * 1.8).coerceIn(0.0, 1.0)
        val clipPenalty = (clippedCount.toDouble() / pixels.size.coerceAtLeast(1)) * 0.6
        val exposureScore = (exposureBalance - clipPenalty).coerceIn(0.0, 1.0).toFloat()

        val sharpnessNorm = (sharpness / 160f).coerceIn(0f, 1f)
        val qualityScore = ((sharpnessNorm * 0.65f + exposureScore * 0.35f) * 100f).coerceIn(0f, 100f)
        val isBlurry = sharpness < BLUR_SHARPNESS_THRESHOLD
        val neutralBrightRatio = neutralBrightCount.toFloat() / pixels.size.coerceAtLeast(1)

        if (scaled !== bitmap) scaled.recycle()

        return VisualQualityAnalysis(
            dHash = dHash,
            colorHistogram = histogram,
            sharpnessScore = sharpness,
            exposureScore = exposureScore,
            qualityScore = qualityScore,
            isBlurry = isBlurry,
            neutralBrightRatio = neutralBrightRatio,
        )
    }

    fun computeDHash(bitmap: Bitmap): Long {
        val resized = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L
        var bitIndex = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val left = lumaOf(resized.getPixel(x, y))
                val right = lumaOf(resized.getPixel(x + 1, y))
                if (left > right) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        if (resized !== bitmap) resized.recycle()
        return hash
    }

    fun computeColorHistogram(bitmap: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        // 4 bins R + 4 bins G + 4 bins B + 4 bins Luma = 16 floats
        val bins = FloatArray(16)
        val pixels = IntArray(32 * 32)
        resized.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        for (p in pixels) {
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val y = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            bins[(r / 64).coerceIn(0, 3)] += 1f
            bins[4 + (g / 64).coerceIn(0, 3)] += 1f
            bins[8 + (b / 64).coerceIn(0, 3)] += 1f
            bins[12 + (y / 64).coerceIn(0, 3)] += 1f
        }
        var norm = 0f
        for (v in bins) norm += v * v
        val denom = sqrt(norm).coerceAtLeast(1e-5f)
        for (i in bins.indices) bins[i] /= denom
        if (resized !== bitmap) resized.recycle()
        return bins
    }

    fun serializeHistogram(bins: FloatArray): String =
        bins.joinToString(",") { String.format(Locale.US, "%.4f", it) }

    fun parseHistogram(raw: String): FloatArray? {
        if (raw.isBlank()) return null
        val parts = raw.split(",")
        if (parts.size != 16) return null
        return FloatArray(16) { idx -> parts[idx].toFloatOrNull() ?: 0f }
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    fun dHashSimilarity(a: Long, b: Long): Float =
        1f - (hammingDistance(a, b).toFloat() / 64f)

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        var magA = 0f
        var magB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            magA += a[i] * a[i]
            magB += b[i] * b[i]
        }
        val denom = sqrt(magA) * sqrt(magB)
        return if (denom > 1e-6f) (dot / denom).coerceIn(0f, 1f) else 0f
    }

    fun visualSimilarityScore(source: MediaIndexEntity, candidate: MediaIndexEntity): Float {
        if (source.mediaStoreId == candidate.mediaStoreId) return 1f
        val dHashSim = if (source.dHash != 0L && candidate.dHash != 0L) {
            dHashSimilarity(source.dHash, candidate.dHash)
        } else 0f
        val histA = parseHistogram(source.colorHistogram)
        val histB = parseHistogram(candidate.colorHistogram)
        val colorSim = if (histA != null && histB != null) cosineSimilarity(histA, histB) else 0f
        return (dHashSim * 0.62f + colorSim * 0.38f).coerceIn(0f, 1f)
    }

    fun detectScreenshot(media: MediaIndexEntity): Boolean {
        val lowerPath = media.relativePath.lowercase(Locale.US)
        val lowerName = media.fileName.lowercase(Locale.US)
        val lowerAlbum = media.albumName.lowercase(Locale.US)
        if (lowerPath.contains("screenshot") || lowerAlbum.contains("screenshot") || lowerName.contains("screenshot")) {
            return true
        }
        val hasCameraExif = media.cameraMake.isNotBlank() || media.cameraModel.isNotBlank()
        val isPng = media.format.equals("PNG", ignoreCase = true)
        val tallScreenRatio = if (media.width > 0 && media.height > 0) {
            val ratio = maxOf(media.width, media.height).toFloat() / minOf(media.width, media.height).toFloat()
            ratio in 1.75f..2.4f
        } else false
        return isPng && !hasCameraExif && tallScreenRatio
    }

    private fun lumaOf(pixel: Int): Float {
        return 0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)
    }
}
