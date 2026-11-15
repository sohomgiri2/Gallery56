package com.example.feature.intelligence.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.core.database.AiLabelScore
import com.example.core.database.AiObjectDetectionInfo
import com.example.core.database.MediaIndexEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

class OnDeviceAiAnalyzer(
    private val context: Context,
    private val faceClusteringEngine: FaceClusteringEngine,
) {

    companion object {
        const val CURRENT_INDEX_VERSION = 1
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val imageLabeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.55f)
                .build(),
        )
    }

    private val objectDetector by lazy {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build(),
        )
    }

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build(),
        )
    }

    suspend fun analyzeMediaItem(
        item: MediaIndexEntity,
        faceAnalysisEnabled: Boolean,
    ): MediaIndexEntity = withContext(Dispatchers.Default) {
        val uri = Uri.parse(item.contentUri)
        val sha256 = PerceptualHashAndQualityAnalyzer.computeSha256(context, uri)
        val bitmap = decodeAnalysisBitmap(uri, item.isVideo)

        if (bitmap == null) {
            return@withContext item.copy(
                sha256Hash = sha256.ifEmpty { item.sha256Hash },
                isScreenshot = PerceptualHashAndQualityAnalyzer.detectScreenshot(item),
                aiIndexedAtMillis = System.currentTimeMillis(),
                indexVersion = CURRENT_INDEX_VERSION,
            )
        }

        try {
            val quality = PerceptualHashAndQualityAnalyzer.analyzeBitmap(bitmap)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            // 1. Image Classification (ML Kit)
            val labels = try {
                imageLabeler.process(inputImage).await()
                    .sortedByDescending { it.confidence }
                    .take(10)
                    .map { AiLabelScore(it.text, it.confidence) }
            } catch (_: Throwable) {
                emptyList()
            }

            // 2. Object Recognition (ML Kit)
            val bw = bitmap.width.toFloat().coerceAtLeast(1f)
            val bh = bitmap.height.toFloat().coerceAtLeast(1f)
            val objects = try {
                objectDetector.process(inputImage).await().mapNotNull { obj ->
                    val topLabel = obj.labels.maxByOrNull { it.confidence }
                    val category = topLabel?.text ?: "Object"
                    val confidence = topLabel?.confidence ?: 0.5f
                    AiObjectDetectionInfo(
                        category = category,
                        confidence = confidence,
                        left = (obj.boundingBox.left / bw).coerceIn(0f, 1f),
                        top = (obj.boundingBox.top / bh).coerceIn(0f, 1f),
                        right = (obj.boundingBox.right / bw).coerceIn(0f, 1f),
                        bottom = (obj.boundingBox.bottom / bh).coerceIn(0f, 1f),
                    )
                }
            } catch (_: Throwable) {
                emptyList()
            }

            // 3. OCR Text Extraction (ML Kit)
            val ocrText = try {
                if (!item.isVideo) {
                    textRecognizer.process(inputImage).await().text.trim()
                } else ""
            } catch (_: Throwable) {
                ""
            }

            // 4. Privacy-Gated Face Detection (only if explicitly enabled by user)
            val faceCount = if (faceAnalysisEnabled && !item.isVideo) {
                try {
                    val detectedFaces = faceDetector.process(inputImage).await()
                    faceClusteringEngine.recordFacesForMedia(item.mediaStoreId, bitmap, detectedFaces)
                } catch (_: Throwable) {
                    0
                }
            } else {
                0
            }

            // 5. Screenshot & Document Detection
            val isScreenshot = PerceptualHashAndQualityAnalyzer.detectScreenshot(item)
            val docConfidence = computeDocumentConfidence(
                ocrText = ocrText,
                neutralBrightRatio = quality.neutralBrightRatio,
                labels = labels,
                faceCount = faceCount,
                isScreenshot = isScreenshot,
            )
            val isDocument = docConfidence >= 0.58f

            // 6. Deterministic Structured Caption from real signals only
            val caption = buildStructuredCaption(
                item = item,
                labels = labels,
                objects = objects,
                ocrText = ocrText,
                isScreenshot = isScreenshot,
                isDocument = isDocument,
                isBlurry = quality.isBlurry,
                faceCount = faceCount,
            )

            item.copy(
                sha256Hash = sha256,
                dHash = quality.dHash,
                colorHistogram = PerceptualHashAndQualityAnalyzer.serializeHistogram(quality.colorHistogram),
                sharpnessScore = quality.sharpnessScore,
                exposureScore = quality.exposureScore,
                qualityScore = quality.qualityScore,
                isBlurry = quality.isBlurry,
                isScreenshot = isScreenshot,
                isDocument = isDocument,
                documentConfidence = docConfidence,
                ocrText = ocrText,
                aiLabelsText = labels.joinToString(", ") { it.label },
                aiLabelsJson = json.encodeToString(labels),
                aiObjectsText = objects.joinToString(", ") { it.category }.distinctWords(),
                aiObjectsJson = json.encodeToString(objects),
                generatedCaption = caption,
                faceCount = faceCount,
                aiIndexedAtMillis = System.currentTimeMillis(),
                indexVersion = CURRENT_INDEX_VERSION,
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun computeDocumentConfidence(
        ocrText: String,
        neutralBrightRatio: Float,
        labels: List<AiLabelScore>,
        faceCount: Int,
        isScreenshot: Boolean,
    ): Float {
        if (isScreenshot) return 0f
        var score = 0f
        val charCount = ocrText.replace("\\s+".toRegex(), "").length
        val lineCount = ocrText.lines().count { it.isNotBlank() }
        if (charCount >= 40) score += 0.42f else if (charCount >= 16) score += 0.22f
        if (lineCount >= 4) score += 0.22f
        if (neutralBrightRatio >= 0.32f) score += 0.24f
        val hasDocLabel = labels.any {
            val l = it.label.lowercase(Locale.US)
            l.contains("paper") || l.contains("text") || l.contains("document") ||
                l.contains("receipt") || l.contains("font") || l.contains("poster")
        }
        if (hasDocLabel) score += 0.22f
        if (faceCount > 0) score -= 0.25f
        return score.coerceIn(0f, 1f)
    }

    private fun buildStructuredCaption(
        item: MediaIndexEntity,
        labels: List<AiLabelScore>,
        objects: List<AiObjectDetectionInfo>,
        ocrText: String,
        isScreenshot: Boolean,
        isDocument: Boolean,
        isBlurry: Boolean,
        faceCount: Int,
    ): String {
        val parts = mutableListOf<String>()
        val mediaNoun = if (item.isVideo) "Video" else when {
            isScreenshot -> "Screen capture"
            isDocument -> "Document scan"
            else -> "Photo"
        }
        val topLabels = labels.take(3).map { it.label }
        if (topLabels.isNotEmpty()) {
            parts.add("$mediaNoun featuring ${topLabels.joinToString(", ").lowercase(Locale.US)}")
        } else {
            parts.add("$mediaNoun in ${item.albumName}")
        }

        val objCategories = objects.map { it.category }.filter { it != "Object" }.distinct()
        if (objCategories.isNotEmpty()) {
            parts.add("objects: ${objCategories.joinToString(", ")}")
        }
        if (faceCount > 0) {
            parts.add(if (faceCount == 1) "1 detected face" else "$faceCount detected faces")
        }
        if (ocrText.isNotBlank()) {
            val snippet = ocrText.replace('\n', ' ').trim().take(64)
            parts.add("text \"$snippet${if (ocrText.length > 64) "…" else ""}\"")
        }
        if (item.cameraModel.isNotBlank()) {
            parts.add("shot on ${item.cameraModel}")
        }
        if (isBlurry) {
            parts.add("soft focus detected")
        }
        return parts.joinToString(" • ")
    }

    private fun decodeAnalysisBitmap(uri: Uri, isVideo: Boolean): Bitmap? {
        return try {
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                } finally {
                    retriever.release()
                }
            } else {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, boundsOpts)
                }
                val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
                var sampleSize = 1
                while (maxDim / sampleSize > 960) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun String.distinctWords(): String {
        return split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(", ")
    }
}
