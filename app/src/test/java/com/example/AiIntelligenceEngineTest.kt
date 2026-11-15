package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.FaceDetectionEntity
import com.example.core.database.FaceGroupEntity
import com.example.core.database.Gallery56Database
import com.example.core.database.MediaIndexEntity
import com.example.feature.intelligence.engine.FaceClusteringEngine
import com.example.feature.intelligence.engine.PerceptualHashAndQualityAnalyzer
import com.example.feature.intelligence.engine.SmartCollectionsEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiIntelligenceEngineTest {

    private lateinit var db: Gallery56Database
    private lateinit var faceEngine: FaceClusteringEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, Gallery56Database::class.java)
            .allowMainThreadQueries()
            .build()
        faceEngine = FaceClusteringEngine(context, db.faceDao(), db.mediaIndexDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `sharpness and dHash distinguish crisp vs flat blurry bitmaps`() {
        val sharpBmp = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
        val sharpPixels = IntArray(144 * 144) { idx ->
            val x = idx % 144
            val y = idx / 144
            if ((x / 4 + y / 4) % 2 == 0) Color.WHITE else Color.BLACK
        }
        sharpBmp.setPixels(sharpPixels, 0, 144, 0, 0, 144, 144)

        val flatBmp = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888)
        val flatPixels = IntArray(144 * 144) { Color.GRAY }
        flatBmp.setPixels(flatPixels, 0, 144, 0, 0, 144, 144)

        val sharpMetrics = PerceptualHashAndQualityAnalyzer.analyzeBitmap(sharpBmp)
        val flatMetrics = PerceptualHashAndQualityAnalyzer.analyzeBitmap(flatBmp)

        assertTrue(sharpMetrics.sharpnessScore > flatMetrics.sharpnessScore)
        assertFalse(sharpMetrics.isBlurry)
        assertTrue(flatMetrics.isBlurry)
        assertEquals(1f, PerceptualHashAndQualityAnalyzer.dHashSimilarity(sharpMetrics.dHash, sharpMetrics.dHash), 0.001f)

        sharpBmp.recycle()
        flatBmp.recycle()
    }

    @Test
    fun `smart collections detect SHA256 duplicates, burst groups, and best shots`() {
        val baseTime = 1_700_000_000_000L
        val items = listOf(
            MediaIndexEntity(
                mediaStoreId = 10L,
                contentUri = "content://10",
                fileName = "IMG_10.jpg",
                displayName = "IMG_10.jpg",
                mimeType = "image/jpeg",
                format = "JPEG",
                isVideo = false,
                bucketId = 1L,
                albumName = "Camera",
                relativePath = "DCIM/",
                dateTakenMillis = baseTime,
                dateModifiedMillis = baseTime,
                fileSizeBytes = 1000L,
                width = 1000,
                height = 1000,
                sha256Hash = "same_hash",
                dHash = 0x1111111111111111L,
                qualityScore = 88f,
                aiIndexedAtMillis = baseTime,
            ),
            MediaIndexEntity(
                mediaStoreId = 11L,
                contentUri = "content://11",
                fileName = "IMG_11_COPY.jpg",
                displayName = "IMG_11_COPY.jpg",
                mimeType = "image/jpeg",
                format = "JPEG",
                isVideo = false,
                bucketId = 1L,
                albumName = "Camera",
                relativePath = "DCIM/",
                dateTakenMillis = baseTime + 5_000L,
                dateModifiedMillis = baseTime + 5_000L,
                fileSizeBytes = 1000L,
                width = 1000,
                height = 1000,
                sha256Hash = "same_hash",
                dHash = 0x1111111111111111L,
                qualityScore = 70f,
                aiIndexedAtMillis = baseTime,
            ),
        )

        val summary = SmartCollectionsEngine.buildSummary(items, faceGroupCount = 0)
        assertEquals(1, summary.duplicateClusters.size)
        assertEquals(10L, summary.duplicateClusters.first().bestShotId)
        assertEquals(1, summary.burstGroups.size)
        assertTrue(summary.bestShots.any { it.mediaStoreId == 10L })
    }

    @Test
    fun `face clustering supports rename, merge, split, and privacy wipe`() = runTest {
        db.faceDao().upsertGroups(
            listOf(
                FaceGroupEntity(groupId = "g1", label = "", isUserNamed = false, faceCount = 1),
                FaceGroupEntity(groupId = "g2", label = "", isUserNamed = false, faceCount = 1),
            ),
        )
        db.faceDao().upsertFaces(
            listOf(
                FaceDetectionEntity("f1", 1L, "g1", 0f, 0f, 0.5f, 0.5f, "0.1", 0.5f, 0.9f, 0.9f, 0f, 0f),
                FaceDetectionEntity("f2", 2L, "g2", 0f, 0f, 0.5f, 0.5f, "0.2", 0.5f, 0.9f, 0.9f, 0f, 0f),
            ),
        )

        faceEngine.renameGroup("g1", "Sam")
        assertEquals("Sam", db.faceDao().getGroupById("g1")?.label)
        assertTrue(db.faceDao().getGroupById("g1")?.isUserNamed == true)

        faceEngine.mergeGroups("g2", "g1")
        assertEquals(2, db.faceDao().getGroupById("g1")?.faceCount)

        faceEngine.removeFaceFromGroup("f2")
        assertEquals(2, db.faceDao().observeAllGroups().first().size)

        faceEngine.deleteAllFaceData()
        assertTrue(db.faceDao().observeAllGroups().first().isEmpty())
        assertTrue(db.faceDao().observeAllFaces().first().isEmpty())
    }
}
