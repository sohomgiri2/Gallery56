package com.example.feature.vault

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import com.example.core.database.VaultDao
import com.example.core.database.VaultItemEntity
import com.example.core.datastore.GalleryPreferences
import com.example.core.datastore.GalleryPreferencesManager
import com.example.core.security.VaultCryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class PrivateVaultUiState(
    val preferences: GalleryPreferences = GalleryPreferences(),
    val isUnlocked: Boolean = false,
    val items: List<VaultItemEntity> = emptyList(),
    val libraryPhotos: List<MediaIndexEntity> = emptyList(),
    val errorNote: String? = null,
    val statusNote: String? = null,
    val lockoutRemainingSeconds: Int = 0,
    val decryptedPreviewBitmap: Bitmap? = null,
    val previewItem: VaultItemEntity? = null,
    val totalVaultBytes: Long = 0L,
)

class PrivateVaultViewModel(
    private val appContext: Context,
    private val vaultDao: VaultDao,
    private val mediaIndexDao: MediaIndexDao,
    private val vaultCryptoManager: VaultCryptoManager,
    private val preferencesManager: GalleryPreferencesManager,
) : ViewModel() {

    private val isUnlockedFlow = MutableStateFlow(false)
    private val errorNoteFlow = MutableStateFlow<String?>(null)
    private val statusNoteFlow = MutableStateFlow<String?>(null)
    private val lockoutSecFlow = MutableStateFlow(0)
    private val previewItemFlow = MutableStateFlow<VaultItemEntity?>(null)
    private val previewBitmapFlow = MutableStateFlow<Bitmap?>(null)

    val uiState: StateFlow<PrivateVaultUiState> = combine(
        preferencesManager.preferences,
        isUnlockedFlow,
        vaultDao.observeAllVaultItems(),
        mediaIndexDao.observeAllMedia(),
        errorNoteFlow,
        statusNoteFlow,
        lockoutSecFlow,
        previewItemFlow,
        previewBitmapFlow,
    ) { arr ->
        val prefs = arr[0] as GalleryPreferences
        val unlocked = arr[1] as Boolean
        @Suppress("UNCHECKED_CAST")
        val items = arr[2] as List<VaultItemEntity>
        @Suppress("UNCHECKED_CAST")
        val allMedia = arr[3] as List<MediaIndexEntity>
        val err = arr[4] as String?
        val status = arr[5] as String?
        val lockSec = arr[6] as Int
        val previewItem = arr[7] as VaultItemEntity?
        val previewBmp = arr[8] as Bitmap?

        PrivateVaultUiState(
            preferences = prefs,
            isUnlocked = unlocked,
            items = if (unlocked) items else emptyList(),
            libraryPhotos = if (unlocked) allMedia.take(120) else emptyList(),
            errorNote = err,
            statusNote = status,
            lockoutRemainingSeconds = lockSec,
            decryptedPreviewBitmap = previewBmp,
            previewItem = previewItem,
            totalVaultBytes = items.sumOf { it.fileSizeBytes },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrivateVaultUiState(),
    )

    init {
        viewModelScope.launch {
            while (true) {
                val until = uiState.value.preferences.vaultLockoutUntilMillis
                val rem = ((until - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
                lockoutSecFlow.value = rem
                delay(1000L)
            }
        }
    }

    fun createPin(pin: String, confirmPin: String) {
        val clean = pin.trim()
        if (clean.length < 4 || clean.length > 8 || !clean.all { it.isDigit() }) {
            errorNoteFlow.value = "PIN must be 4 to 8 digits."
            return
        }
        if (clean != confirmPin.trim()) {
            errorNoteFlow.value = "PIN confirmation does not match."
            return
        }
        viewModelScope.launch {
            val salt = vaultCryptoManager.generateSaltBase64()
            val hash = vaultCryptoManager.hashPin(clean, salt)
            preferencesManager.setVaultPin(hash, salt)
            errorNoteFlow.value = null
            statusNoteFlow.value = "Vault PIN configured (AES-256-GCM active)"
            isUnlockedFlow.value = true
        }
    }

    fun unlockWithPin(pin: String) {
        val prefs = uiState.value.preferences
        val now = System.currentTimeMillis()
        if (prefs.vaultLockoutUntilMillis > now) {
            errorNoteFlow.value = "Locked out for ${((prefs.vaultLockoutUntilMillis - now) / 1000L).coerceAtLeast(1)}s"
            return
        }
        viewModelScope.launch {
            val ok = vaultCryptoManager.verifyPin(pin.trim(), prefs.vaultPinHash, prefs.vaultPinSalt)
            preferencesManager.recordVaultAuthAttempt(success = ok)
            if (ok) {
                errorNoteFlow.value = null
                isUnlockedFlow.value = true
            } else {
                val attempts = prefs.vaultFailedAttempts + 1
                errorNoteFlow.value = if (attempts >= 5) {
                    "Too many failed attempts. Locked for 30s."
                } else {
                    "Incorrect PIN ($attempts/5 attempts)"
                }
            }
        }
    }

    fun unlockViaBiometricSuccess() {
        viewModelScope.launch {
            preferencesManager.recordVaultAuthAttempt(success = true)
            errorNoteFlow.value = null
            isUnlockedFlow.value = true
        }
    }

    fun lockNow() {
        closePreview()
        isUnlockedFlow.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setVaultBiometricEnabled(enabled) }
    }

    fun setAutoLockSeconds(seconds: Int) {
        viewModelScope.launch { preferencesManager.setVaultAutoLockSeconds(seconds) }
    }

    fun importPickerUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var imported = 0
            for (uri in uris) {
                val vaultId = "v_${UUID.randomUUID().toString().take(10)}"
                val outFile = vaultCryptoManager.encryptUriToVault(uri, vaultId)
                if (outFile != null) {
                    val mime = appContext.contentResolver.getType(uri) ?: "image/jpeg"
                    val isVideo = mime.startsWith("video/")
                    vaultDao.upsert(
                        VaultItemEntity(
                            vaultId = vaultId,
                            originalMediaStoreId = null,
                            displayName = "vault_${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}",
                            mimeType = mime,
                            isVideo = isVideo,
                            encryptedFilePath = outFile.absolutePath,
                            fileSizeBytes = outFile.length(),
                            width = 0,
                            height = 0,
                            dateTakenMillis = System.currentTimeMillis(),
                        ),
                    )
                    imported++
                }
            }
            statusNoteFlow.value = "Encrypted $imported item(s) with AndroidKeyStore AES-256-GCM"
        }
    }

    fun importLibraryItems(items: List<MediaIndexEntity>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            var imported = 0
            val importedIds = mutableListOf<Long>()
            for (item in items) {
                val vaultId = "v_${UUID.randomUUID().toString().take(10)}"
                val outFile = vaultCryptoManager.encryptUriToVault(Uri.parse(item.contentUri), vaultId)
                if (outFile != null) {
                    vaultDao.upsert(
                        VaultItemEntity(
                            vaultId = vaultId,
                            originalMediaStoreId = item.mediaStoreId,
                            displayName = item.displayName,
                            mimeType = item.mimeType,
                            isVideo = item.isVideo,
                            encryptedFilePath = outFile.absolutePath,
                            fileSizeBytes = item.fileSizeBytes.coerceAtLeast(outFile.length()),
                            width = item.width,
                            height = item.height,
                            dateTakenMillis = item.dateTakenMillis,
                        ),
                    )
                    importedIds.add(item.mediaStoreId)
                    imported++
                }
            }
            if (importedIds.isNotEmpty()) {
                mediaIndexDao.deleteByIds(importedIds)
            }
            statusNoteFlow.value = "Encrypted $imported library item(s) into Private Vault"
        }
    }

    fun openDecryptedPreview(item: VaultItemEntity) {
        viewModelScope.launch {
            val bytes = vaultCryptoManager.decryptVaultFileToBytes(item.encryptedFilePath)
            if (bytes == null) {
                errorNoteFlow.value = "Failed to decrypt vault file."
                return@launch
            }
            val bmp = if (!item.isVideo) {
                withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            } else {
                null
            }
            previewBitmapFlow.value?.recycle()
            previewBitmapFlow.value = bmp
            previewItemFlow.value = item
        }
    }

    fun closePreview() {
        previewBitmapFlow.value?.recycle()
        previewBitmapFlow.value = null
        previewItemFlow.value = null
    }

    fun exportFromVault(item: VaultItemEntity, onCompleted: () -> Unit) {
        viewModelScope.launch {
            val bytes = vaultCryptoManager.decryptVaultFileToBytes(item.encryptedFilePath)
            if (bytes == null) {
                errorNoteFlow.value = "Decryption failed during export."
                return@launch
            }
            val exported = withContext(Dispatchers.IO) {
                writeBytesToMediaStore(item, bytes)
            }
            if (exported) {
                vaultCryptoManager.deleteEncryptedFile(item.encryptedFilePath)
                vaultDao.deleteById(item.vaultId)
                closePreview()
                statusNoteFlow.value = "Exported ${item.displayName} back to device gallery"
                onCompleted()
            } else {
                errorNoteFlow.value = "Failed to export file to MediaStore."
            }
        }
    }

    fun deleteFromVaultPermanently(item: VaultItemEntity) {
        viewModelScope.launch {
            vaultCryptoManager.deleteEncryptedFile(item.encryptedFilePath)
            vaultDao.deleteById(item.vaultId)
            closePreview()
            statusNoteFlow.value = "Permanently shredded ${item.displayName}"
        }
    }

    fun clearMessages() {
        errorNoteFlow.value = null
        statusNoteFlow.value = null
    }

    private fun writeBytesToMediaStore(item: VaultItemEntity, bytes: ByteArray): Boolean {
        return try {
            val collection = if (item.isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val baseDir = if (item.isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$baseDir/Gallery56_VaultExport")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = appContext.contentResolver.insert(collection, values) ?: return false
                appContext.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                appContext.contentResolver.update(uri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(baseDir), "Gallery56_VaultExport").apply { mkdirs() }
                val dest = File(dir, item.displayName)
                FileOutputStream(dest).use { it.write(bytes) }
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    class Factory(
        private val context: Context,
        private val vaultDao: VaultDao,
        private val mediaIndexDao: MediaIndexDao,
        private val vaultCryptoManager: VaultCryptoManager,
        private val preferencesManager: GalleryPreferencesManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrivateVaultViewModel(
                appContext = context.applicationContext,
                vaultDao = vaultDao,
                mediaIndexDao = mediaIndexDao,
                vaultCryptoManager = vaultCryptoManager,
                preferencesManager = preferencesManager,
            ) as T
        }
    }
}
