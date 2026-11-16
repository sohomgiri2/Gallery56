package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.Gallery56Database
import com.example.core.database.MediaIndexEntity
import com.example.core.datastore.GalleryLayoutId
import com.example.core.datastore.GalleryPreferencesManager
import com.example.core.datastore.NavigationStyle
import com.example.core.datastore.SavedEditorFilterPreset
import com.example.core.security.VaultCryptoManager
import com.example.feature.editor.CropAspectPreset
import com.example.feature.editor.EditorBrushStroke
import com.example.feature.editor.EditorCurvesPreset
import com.example.feature.editor.EditorDuotonePreset
import com.example.feature.editor.EditorFilterPreset
import com.example.feature.editor.EditorFramePreset
import com.example.feature.editor.EditorTextLayer
import com.example.feature.editor.PhotoEditState
import com.example.feature.editor.PhotoEditorEngine
import com.example.feature.forensics.FileIntegrityEngine
import com.example.feature.forensics.FileIntegrityStatus
import com.example.feature.smart.SmartCollectionsEngine
import com.example.feature.videos.VideoProcessingEngine
import com.example.ui.theme.CustomThemeProfile
import com.example.ui.theme.GalleryThemePreset
import com.example.ui.theme.ensureReadableTextColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Gallery56UpgradeTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `all 14 gallery layouts exist and lookup falls back safely`() {
        assertEquals(14, GalleryLayoutId.entries.size)
        assertEquals(GalleryLayoutId.MASONRY_FLOW, GalleryLayoutId.fromId("masonry_flow"))
        assertEquals(GalleryLayoutId.AMOLED_PURE, GalleryLayoutId.fromId("amoled_pure"))
        assertEquals(GalleryLayoutId.CLASSIC_GRID, GalleryLayoutId.fromId("unknown_id"))
    }

    @Test
    fun `all 22 theme presets and contrast helper guarantee WCAG readability`() {
        assertEquals(22, GalleryThemePreset.entries.size)
        GalleryThemePreset.entries.forEach { preset ->
            val resolvedPrimary = ensureReadableTextColor(preset.textPrimary, preset.surface, preset.isDark)
            assertEquals(preset.textPrimary, resolvedPrimary)
        }
        // Intentionally unreadable dark text on dark surface must be corrected to high-contrast light ink
        val darkBg = androidx.compose.ui.graphics.Color(0xFF0A0E14)
        val badDarkInk = androidx.compose.ui.graphics.Color(0xFF141820)
        val fixed = ensureReadableTextColor(badDarkInk, darkBg, isDarkSurface = true)
        val brighter = maxOf(fixed.luminance(), darkBg.luminance()) + 0.05f
        val darker = minOf(fixed.luminance(), darkBg.luminance()) + 0.05f
        assertTrue((brighter / darker) >= 4.5f)
    }

    @Test
    fun `room MIGRATION_2_3 preserves v2 rows and adds v3 columns`() {
        val dbName = "test_migration_2_3_${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS custom_albums (
                                albumId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                description TEXT NOT NULL,
                                coverUri TEXT,
                                isPinned INTEGER NOT NULL,
                                createdAtMillis INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS media_index (
                                mediaStoreId INTEGER PRIMARY KEY NOT NULL,
                                contentUri TEXT NOT NULL,
                                fileName TEXT NOT NULL,
                                displayName TEXT NOT NULL,
                                mimeType TEXT NOT NULL,
                                format TEXT NOT NULL,
                                isVideo INTEGER NOT NULL,
                                bucketId INTEGER NOT NULL,
                                albumName TEXT NOT NULL,
                                relativePath TEXT NOT NULL,
                                dateTakenMillis INTEGER NOT NULL,
                                dateModifiedMillis INTEGER NOT NULL,
                                fileSizeBytes INTEGER NOT NULL,
                                width INTEGER NOT NULL,
                                height INTEGER NOT NULL,
                                durationMillis INTEGER NOT NULL,
                                cameraMake TEXT NOT NULL,
                                cameraModel TEXT NOT NULL,
                                aperture TEXT NOT NULL,
                                iso INTEGER NOT NULL,
                                exposureTime TEXT NOT NULL,
                                focalLength TEXT NOT NULL,
                                orientation INTEGER NOT NULL,
                                latitude REAL,
                                longitude REAL,
                                hasGps INTEGER NOT NULL,
                                sha256Hash TEXT NOT NULL,
                                dHash INTEGER NOT NULL,
                                colorHistogram TEXT NOT NULL,
                                sharpnessScore REAL NOT NULL,
                                exposureScore REAL NOT NULL,
                                qualityScore REAL NOT NULL,
                                isBlurry INTEGER NOT NULL,
                                isScreenshot INTEGER NOT NULL,
                                isDocument INTEGER NOT NULL,
                                documentConfidence REAL NOT NULL,
                                ocrText TEXT NOT NULL,
                                aiLabelsText TEXT NOT NULL,
                                aiLabelsJson TEXT NOT NULL,
                                aiObjectsText TEXT NOT NULL,
                                aiObjectsJson TEXT NOT NULL,
                                generatedCaption TEXT NOT NULL,
                                userTags TEXT NOT NULL,
                                userNotes TEXT NOT NULL,
                                userRating INTEGER NOT NULL,
                                isFavorite INTEGER NOT NULL,
                                groupId TEXT NOT NULL,
                                isBestShot INTEGER NOT NULL,
                                faceCount INTEGER NOT NULL,
                                aiIndexedAtMillis INTEGER NOT NULL,
                                indexVersion INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build(),
        )

        val v2Db = helper.writableDatabase
        v2Db.execSQL(
            "INSERT INTO custom_albums (albumId, name, description, coverUri, isPinned, createdAtMillis) VALUES (1, 'Legacy Album', 'Keep me', NULL, 1, 1000)",
        )
        v2Db.execSQL(
            """
            INSERT INTO media_index (
                mediaStoreId, contentUri, fileName, displayName, mimeType, format, isVideo,
                bucketId, albumName, relativePath, dateTakenMillis, dateModifiedMillis, fileSizeBytes,
                width, height, durationMillis, cameraMake, cameraModel, aperture, iso, exposureTime,
                focalLength, orientation, latitude, longitude, hasGps, sha256Hash, dHash, colorHistogram,
                sharpnessScore, exposureScore, qualityScore, isBlurry, isScreenshot, isDocument,
                documentConfidence, ocrText, aiLabelsText, aiLabelsJson, aiObjectsText, aiObjectsJson,
                generatedCaption, userTags, userNotes, userRating, isFavorite, groupId, isBestShot,
                faceCount, aiIndexedAtMillis, indexVersion
            ) VALUES (
                42, 'content://media/42', 'IMG_42.jpg', 'IMG_42.jpg', 'image/jpeg', 'JPEG', 0,
                1, 'Camera', 'DCIM/Camera/', 1700000, 1700000, 4096,
                1920, 1080, 0, 'Google', 'Pixel', 'f/1.8', 100, '1/120',
                '24mm', 0, NULL, NULL, 0, 'abc123', 99, '',
                0.8, 0.7, 88.0, 0, 0, 0,
                0.0, '', '', '[]', '', '[]',
                '', 'travel', 'saved', 5, 1, '', 0,
                0, 1700000, 1
            )
            """.trimIndent(),
        )
        helper.close()

        val sqlDb = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
        assertEquals(2, sqlDb.version)
        sqlDb.close()

        val migratedHelper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        if (oldVersion == 2 && newVersion == 3) {
                            Gallery56Database.MIGRATION_2_3.migrate(db)
                        }
                    }
                })
                .build(),
        )

        val v3Db = migratedHelper.writableDatabase
        v3Db.query("SELECT name, colorLabelHex, sortOrder FROM custom_albums WHERE albumId = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Legacy Album", c.getString(0))
            assertEquals("", c.getString(1))
            assertEquals(0, c.getInt(2))
        }
        v3Db.query("SELECT displayName, lastViewedAtMillis, lastEditedAtMillis FROM media_index WHERE mediaStoreId = 42").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("IMG_42.jpg", c.getString(0))
            assertEquals(0L, c.getLong(1))
            assertEquals(0L, c.getLong(2))
        }
        migratedHelper.close()
        dbFile.delete()
    }

    @Test
    fun `preferences manager persists layouts nav styles pinned tools presets and resets defaults`() = runTest {
        val prefs = GalleryPreferencesManager(context)
        prefs.setGalleryLayoutId(GalleryLayoutId.MASONRY_FLOW.id)
        prefs.setNavigationStyle(NavigationStyle.FLOATING_ROUNDED)
        prefs.setGridColumns(5)
        prefs.saveCustomEditorPreset(
            SavedEditorFilterPreset(
                id = "preset_1",
                name = "Cinema Warm",
                brightness = 12f,
                contrast = 1.2f,
                saturation = 1.1f,
                temperature = 18f,
            ),
        )
        prefs.recordRecentStudioTool("video_studio")
        prefs.togglePinnedStudioTool("video_studio")
        prefs.saveCustomTheme(CustomThemeProfile(id = "custom_p", name = "Neon Mint"))

        val saved = prefs.preferences.first()
        assertEquals(GalleryLayoutId.MASONRY_FLOW.id, saved.galleryLayoutId)
        assertEquals(NavigationStyle.FLOATING_ROUNDED, saved.navigationStyle)
        assertEquals(5, saved.gridColumns)
        assertTrue(saved.customEditorPresets.any { it.id == "preset_1" })
        assertTrue("video_studio" in saved.recentStudioTools)
        assertTrue("video_studio" in saved.pinnedStudioTools)
        assertEquals("custom_p", saved.customThemeProfileOrNull()?.id)

        prefs.resetAppearanceDefaults()
        val reset = prefs.preferences.first()
        assertEquals(GalleryLayoutId.CLASSIC_GRID.id, reset.galleryLayoutId)
        assertEquals(3, reset.gridColumns)
        assertEquals(NavigationStyle.GLASS, reset.navigationStyle)
        assertEquals("", reset.activeCustomThemeId)
    }

    private fun com.example.core.datastore.GalleryPreferences.customThemeProfileOrNull(): CustomThemeProfile? =
        customThemes.firstOrNull { it.id == activeCustomThemeId }

    @Test
    fun `photo editor 2_0 renders pro adjustments layers brush resize and frame cleanly`() {
        val src = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.rgb(120, 160, 200))

        val out = PhotoEditorEngine.renderEditedBitmap(
            source = src,
            state = PhotoEditState(
                cropPreset = CropAspectPreset.SQUARE,
                resizeScale = 0.5f,
                sharpness = 0.4f,
                vignette = 0.5f,
                grain = 0.25f,
                curvesPreset = EditorCurvesPreset.S_CURVE,
                duotone = EditorDuotonePreset.CYAN_COBALT,
                filter = EditorFilterPreset.VIVID,
                framePreset = EditorFramePreset.POLAROID,
                watermarkText = "GALLERY56",
                textLayers = listOf(EditorTextLayer(id = "t1", text = "STUDIO")),
                brushStrokes = listOf(
                    EditorBrushStroke(
                        points = listOf(0.1f to 0.1f, 0.8f to 0.8f),
                        colorInt = Color.RED,
                    ),
                ),
            ),
        )

        assertTrue(out.width > 0 && out.height > 0)
        // Resized down to 0.5x square (60x60) plus polaroid frame padding -> still <= 100px wide
        assertTrue(out.width <= 100)
        out.recycle()
        src.recycle()
    }

    @Test
    fun `video processing engine encodes valid GIF89a header and trailer`() {
        val f1 = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
        val f2 = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }

        val bytes = VideoProcessingEngine.encodeAnimatedGif(listOf(f1, f2), delayMs = 120)
        f1.recycle()
        f2.recycle()

        assertTrue(bytes.size > 24)
        val header = String(bytes.copyOfRange(0, 6), Charsets.US_ASCII)
        assertEquals("GIF89a", header)
        assertEquals(0x3B.toByte(), bytes.last())
    }

    @Test
    fun `smart collections groups bursts and computes RGB histogram palette`() {
        val baseTime = 1_700_000_000_000L
        val items = listOf(
            sampleMedia(1L, "Camera", baseTime),
            sampleMedia(2L, "Camera", baseTime + 1_200L),
            sampleMedia(3L, "Camera", baseTime + 2_400L),
            sampleMedia(4L, "Trips", baseTime + 30_000L),
        )
        val clusters = SmartCollectionsEngine.detectBurstGroups(items)
        assertEquals(1, clusters.size)
        assertEquals(3, clusters.first().items.size)

        val bmp = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(220, 40, 80))
        val report = SmartCollectionsEngine.analyzeBitmapColors(bmp)
        bmp.recycle()
        assertEquals(32, report.redBins.size)
        assertTrue(report.redBins.sum() > 0)
        assertTrue(report.dominantColors.isNotEmpty())
    }

    @Test
    fun `file integrity engine reports missing URIs safely and vault crypto preserves bytes`() = runTest {
        val missing = sampleMedia(999L, "Camera", 1_000L, uri = "content://nonexistent/missing/999")
        val audit = FileIntegrityEngine.verifyMediaItem(context, missing)
        assertEquals(FileIntegrityStatus.UNREADABLE_MISSING, audit.status)

        val vault = VaultCryptoManager(context)
        val temp = File.createTempFile("g56_test", ".bin", context.cacheDir)
        try {
            val data = "Gallery56_Cipher_Check".encodeToByteArray()
            val enc = vault.encryptBytesToVault(data, "upgrade_test")
            assertNotNull(enc)
            val dec = vault.decryptVaultFileToBytes(enc!!.absolutePath)
            assertEquals("Gallery56_Cipher_Check", dec?.decodeToString())
            vault.deleteEncryptedFile(enc.absolutePath)
            assertFalse(enc.exists())
        } finally {
            temp.delete()
        }
    }

    private fun sampleMedia(
        id: Long,
        album: String,
        dateTakenMillis: Long,
        uri: String = "content://media/external/images/media/$id",
    ) = MediaIndexEntity(
        mediaStoreId = id,
        contentUri = uri,
        fileName = "IMG_$id.jpg",
        displayName = "IMG_$id.jpg",
        mimeType = "image/jpeg",
        format = "JPEG",
        isVideo = false,
        bucketId = 1L,
        albumName = album,
        relativePath = "DCIM/$album/",
        dateTakenMillis = dateTakenMillis,
        dateModifiedMillis = dateTakenMillis,
        fileSizeBytes = 1024L,
        width = 1080,
        height = 1080,
    )
}
