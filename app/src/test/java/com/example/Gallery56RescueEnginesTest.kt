package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.CustomAlbumEntity
import com.example.core.database.CustomAlbumMemberEntity
import com.example.core.database.Gallery56Database
import com.example.core.database.MediaIndexEntity
import com.example.core.database.SavedSearchEntity
import com.example.core.database.TrashItemEntity
import com.example.core.database.VideoPlaybackEntity
import com.example.core.datastore.GalleryPreferencesManager
import com.example.core.security.VaultCryptoManager
import com.example.feature.backup.BackupRestoreManager
import com.example.feature.editor.CropAspectPreset
import com.example.feature.editor.EditorFilterPreset
import com.example.feature.editor.EditorTextLayer
import com.example.feature.editor.PhotoEditState
import com.example.feature.editor.PhotoEditorEngine
import com.example.ui.theme.CustomThemeProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Gallery56RescueEnginesTest {

    private lateinit var context: Context
    private lateinit var db: Gallery56Database

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, Gallery56Database::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `vault crypto hashes salted PIN and encrypts decrypts bytes cleanly`() = runTest {
        val vault = VaultCryptoManager(context)
        val salt = vault.generateSaltBase64()
        val hash = vault.hashPin("5690", salt)

        assertTrue(vault.verifyPin("5690", hash, salt))
        assertFalse(vault.verifyPin("0000", hash, salt))

        val secretPayload = "Gallery56_PrivateVault_BytePayload_2026".encodeToByteArray()
        val encFile = vault.encryptBytesToVault(secretPayload, "test_vault_item")
        assertNotNull(encFile)
        assertTrue(encFile!!.exists())

        val decrypted = vault.decryptVaultFileToBytes(encFile.absolutePath)
        assertNotNull(decrypted)
        assertArrayEquals(secretPayload, decrypted)

        vault.deleteEncryptedFile(encFile.absolutePath)
        assertFalse(encFile.exists())
    }

    @Test
    fun `photo editor rotates crops and renders filters without mutating source`() {
        val src = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        src.eraseColor(Color.rgb(180, 90, 40))

        val edited = PhotoEditorEngine.renderEditedBitmap(
            source = src,
            state = PhotoEditState(
                rotationDegrees = 90,
                cropPreset = CropAspectPreset.SQUARE,
                filter = EditorFilterPreset.NOIR,
                textLayers = listOf(EditorTextLayer(id = "t_rescue", text = "G56")),
            ),
        )

        assertEquals(100, edited.width)
        assertEquals(100, edited.height)
        assertEquals(200, src.width)
        assertEquals(100, src.height)

        edited.recycle()
        src.recycle()
    }

    @Test
    fun `backup manager exports validates and restores custom themes albums and annotations`() = runTest {
        val prefsManager = GalleryPreferencesManager(context)
        prefsManager.saveCustomTheme(
            CustomThemeProfile(
                id = "custom_test_1",
                name = "Cyber Obsidian",
                primaryHex = "#00F5D4",
            ),
        )
        db.savedSearchDao().upsert(
            SavedSearchEntity(name = "RAW Night", summary = "RAW shots", queryJson = "{}", isPinned = true),
        )
        db.customAlbumDao().upsertAlbum(
            CustomAlbumEntity(name = "Studio Selects", description = "Favorites", isPinned = true),
        )
        db.mediaIndexDao().upsert(
            MediaIndexEntity(
                mediaStoreId = 501L,
                contentUri = "content://media/501",
                fileName = "PORTRAIT_501.jpg",
                displayName = "PORTRAIT_501.jpg",
                mimeType = "image/jpeg",
                format = "JPEG",
                isVideo = false,
                bucketId = 1L,
                albumName = "Camera",
                relativePath = "DCIM/Camera/",
                dateTakenMillis = 1_700_000_000_000L,
                dateModifiedMillis = 1_700_000_000_000L,
                fileSizeBytes = 2_000_000L,
                width = 4000,
                height = 3000,
                userTags = "editorial, studio",
                userNotes = "Hero portrait",
                userRating = 5,
                isFavorite = true,
            ),
        )

        val backupManager = BackupRestoreManager(
            context = context,
            preferencesManager = prefsManager,
            savedSearchDao = db.savedSearchDao(),
            customAlbumDao = db.customAlbumDao(),
            mediaIndexDao = db.mediaIndexDao(),
        )

        val jsonPayload = backupManager.buildBackupJson()
        val preview = backupManager.validateBackupJson(jsonPayload)
        assertNotNull(preview)
        assertTrue(preview!!.isValid)
        assertEquals(1, preview.customThemeCount)
        assertEquals(1, preview.savedSearchCount)
        assertEquals(1, preview.albumCount)
        assertEquals(1, preview.annotationCount)

        // Reset favorite & verify restore brings it back
        db.mediaIndexDao().setFavorite(501L, false)
        backupManager.applyBackupBundle(preview.bundle)
        val restoredItem = db.mediaIndexDao().getById(501L)
        assertTrue(restoredItem?.isFavorite == true)
        assertEquals(5, restoredItem?.userRating)
    }

    @Test
    fun `albums trash and video playback DAOs persist state accurately`() = runTest {
        val albumId = db.customAlbumDao().upsertAlbum(CustomAlbumEntity(name = "Trips"))
        db.customAlbumDao().addMembers(listOf(CustomAlbumMemberEntity(albumId = albumId, mediaStoreId = 99L)))
        assertEquals(listOf(99L), db.customAlbumDao().getMediaIdsInAlbum(albumId))

        db.trashDao().upsert(
            TrashItemEntity(
                trashId = "tr_1",
                originalMediaStoreId = 99L,
                originalUri = "content://media/99",
                displayName = "old.jpg",
                mimeType = "image/jpeg",
                format = "JPEG",
                isVideo = false,
                albumName = "Camera",
                cachedFilePath = "/tmp/tr_1_old.jpg",
                fileSizeBytes = 1024L,
                width = 800,
                height = 600,
            ),
        )
        assertEquals(1, db.trashDao().observeAllTrashItems().first().size)

        db.videoPlaybackDao().upsert(
            VideoPlaybackEntity(
                mediaStoreId = 777L,
                positionMillis = 42_500L,
                durationMillis = 120_000L,
                playbackSpeed = 1.5f,
            ),
        )
        val resumeState = db.videoPlaybackDao().getState(777L)
        assertEquals(42_500L, resumeState?.positionMillis)
        assertEquals(1.5f, resumeState?.playbackSpeed)
    }
}
