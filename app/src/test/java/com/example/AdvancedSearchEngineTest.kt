package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.FaceDetectionEntity
import com.example.core.database.FaceGroupEntity
import com.example.core.database.Gallery56Database
import com.example.core.database.MediaIndexEntity
import com.example.core.database.SavedSearchEntity
import com.example.feature.search.engine.AdvancedSearchEngine
import com.example.feature.search.engine.OrientationFilter
import com.example.feature.search.engine.SearchQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdvancedSearchEngineTest {

    private lateinit var db: Gallery56Database
    private lateinit var engine: AdvancedSearchEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, Gallery56Database::class.java)
            .allowMainThreadQueries()
            .build()
        engine = AdvancedSearchEngine(db.mediaIndexDao(), db.faceDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `offline FTS4 and semantic concept search matches OCR, AI labels, EXIF, and tags`() = runTest {
        val sampleItems = listOf(
            MediaIndexEntity(
                mediaStoreId = 1L,
                contentUri = "content://media/1",
                fileName = "IMG_GOLDEN_RETRIEVER.jpg",
                displayName = "IMG_GOLDEN_RETRIEVER.jpg",
                mimeType = "image/jpeg",
                format = "JPEG",
                isVideo = false,
                bucketId = 10L,
                albumName = "Camera",
                relativePath = "DCIM/Camera/",
                dateTakenMillis = 1_700_000_000_000L,
                dateModifiedMillis = 1_700_000_000_000L,
                fileSizeBytes = 4_200_000L,
                width = 4000,
                height = 3000,
                cameraMake = "Google",
                cameraModel = "Pixel 9 Pro",
                iso = 120,
                latitude = 37.7749,
                longitude = -122.4194,
                hasGps = true,
                dHash = 0x0F0F0F0F0F0F0F0FL,
                colorHistogram = "0.5,0.2,0.1,0.2,0.3,0.3,0.2,0.2,0.1,0.2,0.3,0.4,0.2,0.3,0.3,0.2",
                aiLabelsText = "Dog, Outdoor, Grass",
                userTags = "weekend, park",
                userRating = 5,
                isFavorite = true,
            ),
            MediaIndexEntity(
                mediaStoreId = 2L,
                contentUri = "content://media/2",
                fileName = "DOC_CAFE_BILL.png",
                displayName = "DOC_CAFE_BILL.png",
                mimeType = "image/png",
                format = "PNG",
                isVideo = false,
                bucketId = 20L,
                albumName = "Documents",
                relativePath = "Pictures/Documents/",
                dateTakenMillis = 1_700_100_000_000L,
                dateModifiedMillis = 1_700_100_000_000L,
                fileSizeBytes = 650_000L,
                width = 1800,
                height = 2400,
                ocrText = "COFFEE HOUSE INVOICE TOTAL $18.40",
                isDocument = true,
                documentConfidence = 0.92f,
                userRating = 3,
            ),
            MediaIndexEntity(
                mediaStoreId = 3L,
                contentUri = "content://media/3",
                fileName = "VID_NIGHT_CITY.mp4",
                displayName = "VID_NIGHT_CITY.mp4",
                mimeType = "video/mp4",
                format = "MP4",
                isVideo = true,
                bucketId = 30L,
                albumName = "Movies",
                relativePath = "Movies/",
                dateTakenMillis = 1_700_200_000_000L,
                dateModifiedMillis = 1_700_200_000_000L,
                fileSizeBytes = 25_000_000L,
                width = 1920,
                height = 1080,
                durationMillis = 15_000L,
                latitude = 34.0522,
                longitude = -118.2437,
                hasGps = true,
            ),
        )
        db.mediaIndexDao().upsertAll(sampleItems)

        // Semantic search: "pet" expands to "dog"
        val petReport = engine.search(SearchQuery(textQuery = "pet", useSemanticExpansion = true))
        assertEquals(1, petReport.results.size)
        assertEquals(1L, petReport.results.first().media.mediaStoreId)

        // OCR search: "invoice"
        val ocrReport = engine.search(SearchQuery(ocrQuery = "invoice", hasOcrTextOnly = true))
        assertEquals(1, ocrReport.results.size)
        assertEquals(2L, ocrReport.results.first().media.mediaStoreId)

        // EXIF + Resolution + Rating + Orientation
        val exifReport = engine.search(
            SearchQuery(
                cameraModelQuery = "Pixel",
                minMegaPixels = 10f,
                orientation = OrientationFilter.LANDSCAPE,
                minRating = 5,
                favoritesOnly = true,
            ),
        )
        assertEquals(1, exifReport.results.size)
        assertEquals(1L, exifReport.results.first().media.mediaStoreId)

        // GPS Radius (within 10 km of San Francisco)
        val gpsReport = engine.search(
            SearchQuery(
                gpsCenterLat = 37.78,
                gpsCenterLon = -122.41,
                gpsRadiusKm = 10.0,
            ),
        )
        assertEquals(1, gpsReport.results.size)
        assertEquals(1L, gpsReport.results.first().media.mediaStoreId)

        // Face-group filtering
        db.faceDao().upsertGroup(FaceGroupEntity(groupId = "fg_alpha", label = "Alex", isUserNamed = true, faceCount = 1))
        db.faceDao().upsertFaces(
            listOf(
                FaceDetectionEntity(
                    faceId = "1_0",
                    mediaStoreId = 1L,
                    groupId = "fg_alpha",
                    left = 0.1f,
                    top = 0.1f,
                    right = 0.4f,
                    bottom = 0.4f,
                    featureVector = "0.1,0.2,0.3,0.1,0.2,0.3,0.4,0.3,0.2,0.1,0.0,0.0",
                    smilingProbability = 0.9f,
                    leftEyeOpenProbability = 0.9f,
                    rightEyeOpenProbability = 0.9f,
                    headEulerY = 0f,
                    headEulerZ = 0f,
                ),
            ),
        )
        val faceFiltered = engine.search(SearchQuery(faceGroupIds = listOf("fg_alpha")))
        assertEquals(1, faceFiltered.results.size)
        assertEquals(1L, faceFiltered.results.first().media.mediaStoreId)

        // Saved Search persistence
        db.savedSearchDao().upsert(
            SavedSearchEntity(name = "Pixel Favorites", summary = "Pixel • 5★", queryJson = "{}"),
        )
        val saved = db.savedSearchDao().observeAllSavedSearches().first()
        assertTrue(saved.any { it.name == "Pixel Favorites" })
    }
}
