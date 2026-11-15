package com.example.feature.intelligence.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.example.core.database.FaceDao
import com.example.core.database.FaceDetectionEntity
import com.example.core.database.FaceGroupEntity
import com.example.core.database.MediaIndexDao
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

class FaceClusteringEngine(
    private val context: Context,
    private val faceDao: FaceDao,
    private val mediaIndexDao: MediaIndexDao,
) {

    private fun faceCropDir(): File {
        return File(context.filesDir, "face_crops").apply { mkdirs() }
    }

    suspend fun recordFacesForMedia(
        mediaStoreId: Long,
        sourceBitmap: Bitmap,
        faces: List<Face>,
    ): Int = withContext(Dispatchers.IO) {
        // Replace old faces for this media item before inserting updated detections
        val existingForMedia = faceDao.getFacesForMedia(mediaStoreId)
        existingForMedia.forEach { old ->
            old.cropPath?.let { path -> File(path).delete() }
        }
        faceDao.deleteFacesForMedia(mediaStoreId)

        if (faces.isEmpty()) {
            refreshAllGroupCounts()
            return@withContext 0
        }

        val existingGroups = faceDao.getAllGroupsSnapshot().toMutableList()
        val existingFaces = faceDao.getAllFacesSnapshot()
        val groupVectors = buildGroupAverageVectors(existingGroups, existingFaces)

        val newFaces = mutableListOf<FaceDetectionEntity>()
        val w = sourceBitmap.width.toFloat().coerceAtLeast(1f)
        val h = sourceBitmap.height.toFloat().coerceAtLeast(1f)

        faces.forEachIndexed { index, face ->
            val box = face.boundingBox
            val normLeft = (box.left / w).coerceIn(0f, 1f)
            val normTop = (box.top / h).coerceIn(0f, 1f)
            val normRight = (box.right / w).coerceIn(0f, 1f)
            val normBottom = (box.bottom / h).coerceIn(0f, 1f)

            val cropFile = saveFaceCrop(sourceBitmap, box, mediaStoreId, index)
            val featureVec = extractGeometricAndCropFeature(face, sourceBitmap)
            val matchedGroupId = findBestMatchingGroup(featureVec, groupVectors)

            val groupId = if (matchedGroupId != null) {
                matchedGroupId
            } else {
                val createdId = "fg_${UUID.randomUUID().toString().take(8)}"
                val newGroup = FaceGroupEntity(
                    groupId = createdId,
                    label = "",
                    isUserNamed = false,
                    coverCropPath = cropFile?.absolutePath,
                    faceCount = 1,
                )
                faceDao.upsertGroup(newGroup)
                existingGroups.add(newGroup)
                groupVectors[createdId] = featureVec
                createdId
            }

            newFaces.add(
                FaceDetectionEntity(
                    faceId = "${mediaStoreId}_$index",
                    mediaStoreId = mediaStoreId,
                    groupId = groupId,
                    left = normLeft,
                    top = normTop,
                    right = normRight,
                    bottom = normBottom,
                    featureVector = serializeVec(featureVec),
                    smilingProbability = face.smilingProbability ?: -1f,
                    leftEyeOpenProbability = face.leftEyeOpenProbability ?: -1f,
                    rightEyeOpenProbability = face.rightEyeOpenProbability ?: -1f,
                    headEulerY = face.headEulerAngleY,
                    headEulerZ = face.headEulerAngleZ,
                    cropPath = cropFile?.absolutePath,
                ),
            )
        }

        faceDao.upsertFaces(newFaces)
        refreshAllGroupCounts()
        newFaces.size
    }

    suspend fun renameGroup(groupId: String, newLabel: String) = withContext(Dispatchers.IO) {
        val trimmed = newLabel.trim()
        faceDao.renameGroup(groupId, trimmed, isUserNamed = trimmed.isNotEmpty())
    }

    suspend fun mergeGroups(sourceGroupId: String, targetGroupId: String) = withContext(Dispatchers.IO) {
        if (sourceGroupId == targetGroupId) return@withContext
        faceDao.reassignGroupFaces(sourceGroupId, targetGroupId)
        faceDao.deleteGroup(sourceGroupId)
        refreshAllGroupCounts()
    }

    suspend fun removeFaceFromGroup(faceId: String) = withContext(Dispatchers.IO) {
        val face = faceDao.getAllFacesSnapshot().firstOrNull { it.faceId == faceId } ?: return@withContext
        val newGroupId = "fg_${UUID.randomUUID().toString().take(8)}"
        faceDao.upsertGroup(
            FaceGroupEntity(
                groupId = newGroupId,
                label = "",
                isUserNamed = false,
                coverCropPath = face.cropPath,
                faceCount = 1,
            ),
        )
        faceDao.moveFaceToGroup(faceId, newGroupId)
        refreshAllGroupCounts()
    }

    suspend fun deleteGroup(groupId: String) = withContext(Dispatchers.IO) {
        val faces = faceDao.getFacesInGroup(groupId)
        faces.forEach { it.cropPath?.let { path -> File(path).delete() } }
        faceDao.deleteFacesInGroup(groupId)
        faceDao.deleteGroup(groupId)
        refreshAllGroupCounts()
    }

    suspend fun deleteAllFaceData() = withContext(Dispatchers.IO) {
        faceDao.deleteAllFaces()
        faceDao.deleteAllGroups()
        mediaIndexDao.clearFaceCounts()
        val dir = faceCropDir()
        dir.listFiles()?.forEach { it.delete() }
    }

    suspend fun refreshAllGroupCounts() = withContext(Dispatchers.IO) {
        val groups = faceDao.getAllGroupsSnapshot()
        val allFaces = faceDao.getAllFacesSnapshot()
        val byGroup = allFaces.groupBy { it.groupId }
        val updated = mutableListOf<FaceGroupEntity>()
        for (group in groups) {
            val members = byGroup[group.groupId].orEmpty()
            if (members.isEmpty()) {
                faceDao.deleteGroup(group.groupId)
            } else {
                val cover = group.coverCropPath?.takeIf { File(it).exists() }
                    ?: members.firstOrNull { it.cropPath != null }?.cropPath
                updated.add(group.copy(faceCount = members.size, coverCropPath = cover))
            }
        }
        if (updated.isNotEmpty()) {
            faceDao.upsertGroups(updated)
        }
    }

    private fun extractGeometricAndCropFeature(face: Face, bitmap: Bitmap): FloatArray {
        val box = face.boundingBox
        val bw = box.width().toFloat().coerceAtLeast(1f)
        val bh = box.height().toFloat().coerceAtLeast(1f)

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position

        val vec = FloatArray(12)
        vec[0] = (bw / bh).coerceIn(0.4f, 1.8f)

        if (leftEye != null && rightEye != null) {
            val eyeDx = rightEye.x - leftEye.x
            val eyeDy = rightEye.y - leftEye.y
            val eyeDist = sqrt(eyeDx * eyeDx + eyeDy * eyeDy)
            vec[1] = (eyeDist / bw).coerceIn(0.1f, 0.9f)
            val eyeMidY = (leftEye.y + rightEye.y) / 2f
            val eyeMidX = (leftEye.x + rightEye.x) / 2f
            if (nose != null) {
                vec[2] = ((nose.y - eyeMidY) / bh).coerceIn(0.05f, 0.7f)
                vec[3] = (abs(nose.x - eyeMidX) / bw).coerceIn(0f, 0.5f)
            }
            if (mouthBottom != null) {
                vec[4] = ((mouthBottom.y - eyeMidY) / bh).coerceIn(0.15f, 0.95f)
            }
        }
        if (mouthLeft != null && mouthRight != null) {
            val mDx = mouthRight.x - mouthLeft.x
            val mDy = mouthRight.y - mouthLeft.y
            vec[5] = (sqrt(mDx * mDx + mDy * mDy) / bw).coerceIn(0.1f, 0.9f)
        }

        // Crop central skin/hair luma & RGB balance (6 floats) for local visual grouping
        val left = box.left.coerceIn(0, bitmap.width - 1)
        val top = box.top.coerceIn(0, bitmap.height - 1)
        val cropW = box.width().coerceIn(1, bitmap.width - left)
        val cropH = box.height().coerceIn(1, bitmap.height - top)
        val crop = Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
        val small = Bitmap.createScaledBitmap(crop, 8, 8, true)
        var rSum = 0f
        var gSum = 0f
        var bSum = 0f
        var topDarkSum = 0f
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val p = small.getPixel(x, y)
                val r = Color.red(p) / 255f
                val g = Color.green(p) / 255f
                val b = Color.blue(p) / 255f
                rSum += r
                gSum += g
                bSum += b
                if (y < 2) topDarkSum += (0.299f * r + 0.587f * g + 0.114f * b)
            }
        }
        vec[6] = rSum / 64f
        vec[7] = gSum / 64f
        vec[8] = bSum / 64f
        vec[9] = topDarkSum / 16f
        vec[10] = (face.headEulerAngleY / 45f).coerceIn(-1f, 1f)
        vec[11] = (face.headEulerAngleZ / 45f).coerceIn(-1f, 1f)

        if (small !== crop) small.recycle()
        if (crop !== bitmap) crop.recycle()

        // L2-normalize
        var mag = 0f
        for (v in vec) mag += v * v
        val denom = sqrt(mag).coerceAtLeast(1e-5f)
        for (i in vec.indices) vec[i] /= denom
        return vec
    }

    private fun buildGroupAverageVectors(
        groups: List<FaceGroupEntity>,
        faces: List<FaceDetectionEntity>,
    ): MutableMap<String, FloatArray> {
        val map = mutableMapOf<String, FloatArray>()
        val byGroup = faces.groupBy { it.groupId }
        for (group in groups) {
            val members = byGroup[group.groupId].orEmpty()
            if (members.isEmpty()) continue
            val avg = FloatArray(12)
            var count = 0
            for (m in members) {
                val vec = parseVec(m.featureVector) ?: continue
                for (i in avg.indices) avg[i] += vec[i]
                count++
            }
            if (count > 0) {
                for (i in avg.indices) avg[i] /= count.toFloat()
                map[group.groupId] = avg
            }
        }
        return map
    }

    private fun findBestMatchingGroup(
        vec: FloatArray,
        groupVectors: Map<String, FloatArray>,
    ): String? {
        var bestGroup: String? = null
        var bestSim = 0.89f // local geometric + appearance clustering threshold
        for ((groupId, centroid) in groupVectors) {
            val sim = PerceptualHashAndQualityAnalyzer.cosineSimilarity(vec, centroid)
            if (sim > bestSim) {
                bestSim = sim
                bestGroup = groupId
            }
        }
        return bestGroup
    }

    private fun saveFaceCrop(
        source: Bitmap,
        box: android.graphics.Rect,
        mediaStoreId: Long,
        index: Int,
    ): File? {
        return try {
            val padX = (box.width() * 0.18f).toInt()
            val padY = (box.height() * 0.18f).toInt()
            val left = (box.left - padX).coerceIn(0, source.width - 1)
            val top = (box.top - padY).coerceIn(0, source.height - 1)
            val right = (box.right + padX).coerceIn(left + 1, source.width)
            val bottom = (box.bottom + padY).coerceIn(top + 1, source.height)
            val crop = Bitmap.createBitmap(source, left, top, right - left, bottom - top)
            val thumb = Bitmap.createScaledBitmap(crop, 160, 160, true)
            val file = File(faceCropDir(), "face_${mediaStoreId}_$index.jpg")
            FileOutputStream(file).use { out ->
                thumb.compress(Bitmap.CompressFormat.JPEG, 86, out)
            }
            if (thumb !== crop) thumb.recycle()
            crop.recycle()
            file
        } catch (_: Throwable) {
            null
        }
    }

    private fun serializeVec(vec: FloatArray): String =
        vec.joinToString(",") { String.format(Locale.US, "%.4f", it) }

    private fun parseVec(raw: String): FloatArray? {
        val parts = raw.split(",")
        if (parts.size != 12) return null
        return FloatArray(12) { idx -> parts[idx].toFloatOrNull() ?: 0f }
    }
}
