package com.example.feature.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.CustomAlbumDao
import com.example.core.database.CustomAlbumEntity
import com.example.core.database.CustomAlbumMemberEntity
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import com.example.core.database.VaultDao
import com.example.core.database.VaultItemEntity
import com.example.core.datastore.GalleryPreferences
import com.example.core.datastore.GalleryPreferencesManager
import com.example.core.media.DeviceMediaRepository
import com.example.core.media.MediaAccessLevel
import com.example.core.media.MediaPermissionHelper
import com.example.core.media.MediaStoreOperations
import com.example.core.media.TrashRepository
import com.example.core.security.VaultCryptoManager
import com.example.feature.intelligence.engine.IntelligenceCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class GalleryQuickBucket(val label: String) {
    ALL("All Media"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    RECENTLY_ADDED("Recently Added"),
    CAMERA("Camera"),
    SCREENSHOTS("Screenshots"),
    DOWNLOADS("Downloads"),
    FAVORITES("Favorites"),
    DOCUMENTS("Documents"),
    GEO("Geo-Tagged"),
}

enum class GallerySortOrder(val label: String) {
    DATE_DESC("Newest"),
    DATE_ASC("Oldest"),
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    SIZE_DESC("Largest"),
}

data class AlbumPreview(
    val name: String,
    val count: Int,
    val coverUri: String?,
)

data class GalleryHomeUiState(
    val accessLevel: MediaAccessLevel = MediaAccessLevel.DENIED,
    val hasMediaLocation: Boolean = false,
    val selectedBucket: GalleryQuickBucket = GalleryQuickBucket.ALL,
    val selectedAlbum: String? = null,
    val sortOrder: GallerySortOrder = GallerySortOrder.DATE_DESC,
    val items: List<MediaIndexEntity> = emptyList(),
    val totalCount: Int = 0,
    val albums: List<AlbumPreview> = emptyList(),
    val customAlbums: List<CustomAlbumEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val preferences: GalleryPreferences = GalleryPreferences(),
    val isSyncing: Boolean = false,
    val statusNote: String? = null,
)

class GalleryHomeViewModel(
    private val appContext: Context,
    private val mediaRepository: DeviceMediaRepository,
    private val mediaIndexDao: MediaIndexDao,
    private val customAlbumDao: CustomAlbumDao,
    private val trashRepository: TrashRepository,
    private val vaultCryptoManager: VaultCryptoManager,
    private val vaultDao: VaultDao,
    private val preferencesManager: GalleryPreferencesManager,
    private val intelligenceCoordinator: IntelligenceCoordinator,
) : ViewModel() {

    private val accessLevelFlow = MutableStateFlow(MediaPermissionHelper.checkAccessLevel(appContext))
    private val hasLocationFlow = MutableStateFlow(MediaPermissionHelper.hasMediaLocationAccess(appContext))
    private val selectedBucketFlow = MutableStateFlow(GalleryQuickBucket.ALL)
    private val selectedAlbumFlow = MutableStateFlow<String?>(null)
    private val sortOrderFlow = MutableStateFlow(GallerySortOrder.DATE_DESC)
    private val selectedIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val selectionModeFlow = MutableStateFlow(false)
    private val isSyncingFlow = MutableStateFlow(false)
    private val statusNoteFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GalleryHomeUiState> = combine(
        mediaIndexDao.observeAllMedia(),
        customAlbumDao.observeAllAlbums(),
        accessLevelFlow,
        selectedBucketFlow,
        selectedAlbumFlow,
        sortOrderFlow,
        selectedIdsFlow,
        selectionModeFlow,
        preferencesManager.preferences,
        isSyncingFlow,
        statusNoteFlow,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allMedia = values[0] as List<MediaIndexEntity>
        @Suppress("UNCHECKED_CAST")
        val customAlbums = values[1] as List<CustomAlbumEntity>
        val access = values[2] as MediaAccessLevel
        val bucket = values[3] as GalleryQuickBucket
        val album = values[4] as String?
        val sort = values[5] as GallerySortOrder
        @Suppress("UNCHECKED_CAST")
        val selectedIds = values[6] as Set<Long>
        val selectionMode = values[7] as Boolean
        val prefs = values[8] as GalleryPreferences
        val syncing = values[9] as Boolean
        val note = values[10] as String?

        val recentCutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val filtered = allMedia.filter { item ->
            val pathLower = item.relativePath.lowercase(Locale.US)
            val albumLower = item.albumName.lowercase(Locale.US)
            val bucketOk = when (bucket) {
                GalleryQuickBucket.ALL -> true
                GalleryQuickBucket.PHOTOS -> !item.isVideo
                GalleryQuickBucket.VIDEOS -> item.isVideo
                GalleryQuickBucket.RECENTLY_ADDED -> item.dateTakenMillis >= recentCutoff || item.dateModifiedMillis >= recentCutoff
                GalleryQuickBucket.CAMERA -> pathLower.contains("dcim") || albumLower == "camera"
                GalleryQuickBucket.SCREENSHOTS -> item.isScreenshot
                GalleryQuickBucket.DOWNLOADS -> pathLower.contains("download") || albumLower.contains("download")
                GalleryQuickBucket.FAVORITES -> item.isFavorite
                GalleryQuickBucket.DOCUMENTS -> item.isDocument
                GalleryQuickBucket.GEO -> item.hasGps
            }
            val albumOk = album == null || item.albumName.equals(album, ignoreCase = true)
            bucketOk && albumOk
        }

        val sorted = when (sort) {
            GallerySortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateTakenMillis }
            GallerySortOrder.DATE_ASC -> filtered.sortedBy { it.dateTakenMillis }
            GallerySortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase(Locale.US) }
            GallerySortOrder.NAME_DESC -> filtered.sortedByDescending { it.displayName.lowercase(Locale.US) }
            GallerySortOrder.SIZE_DESC -> filtered.sortedByDescending { it.fileSizeBytes }
        }

        val albumPreviews = allMedia
            .groupBy { it.albumName.ifBlank { "Internal" } }
            .map { (name, list) ->
                AlbumPreview(
                    name = name,
                    count = list.size,
                    coverUri = list.firstOrNull()?.contentUri,
                )
            }
            .sortedByDescending { it.count }

        GalleryHomeUiState(
            accessLevel = access,
            hasMediaLocation = hasLocationFlow.value,
            selectedBucket = bucket,
            selectedAlbum = album,
            sortOrder = sort,
            items = sorted,
            totalCount = allMedia.size,
            albums = albumPreviews,
            customAlbums = customAlbums,
            selectedIds = selectedIds,
            isSelectionMode = selectionMode,
            preferences = prefs,
            isSyncing = syncing,
            statusNote = note,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryHomeUiState(
            accessLevel = MediaPermissionHelper.checkAccessLevel(appContext),
            hasMediaLocation = MediaPermissionHelper.hasMediaLocationAccess(appContext),
        ),
    )

    init {
        refreshPermissionsAndSync(triggerAi = true)
    }

    fun refreshPermissionsAndSync(triggerAi: Boolean = false) {
        val level = MediaPermissionHelper.checkAccessLevel(appContext)
        accessLevelFlow.value = level
        hasLocationFlow.value = MediaPermissionHelper.hasMediaLocationAccess(appContext)
        if (level != MediaAccessLevel.DENIED) {
            viewModelScope.launch {
                isSyncingFlow.value = true
                val summary = mediaRepository.syncDeviceMedia()
                isSyncingFlow.value = false
                val prefs = preferencesManager.preferences.first()
                if (triggerAi && prefs.autoIndexOnLaunch && summary.totalDiscovered > 0) {
                    intelligenceCoordinator.startIncrementalIndex(forceReanalyzeAll = false)
                }
            }
        }
    }

    fun importPickerUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isSyncingFlow.value = true
            val count = mediaRepository.importPickerUris(uris)
            isSyncingFlow.value = false
            statusNoteFlow.value = "Imported $count item(s) via Android Photo Picker"
            if (count > 0) {
                intelligenceCoordinator.startIncrementalIndex(forceReanalyzeAll = false)
            }
        }
    }

    fun selectBucket(bucket: GalleryQuickBucket) {
        selectedBucketFlow.value = bucket
    }

    fun selectAlbum(albumName: String?) {
        selectedAlbumFlow.value = if (selectedAlbumFlow.value == albumName) null else albumName
    }

    fun setSortOrder(order: GallerySortOrder) {
        sortOrderFlow.value = order
    }

    fun setGridColumns(cols: Int) {
        viewModelScope.launch { preferencesManager.setGridColumns(cols) }
    }

    fun toggleSelectionMode(active: Boolean? = null) {
        val next = active ?: !selectionModeFlow.value
        selectionModeFlow.value = next
        if (!next) {
            selectedIdsFlow.value = emptySet()
        }
    }

    fun toggleSelectItem(mediaStoreId: Long) {
        val current = selectedIdsFlow.value
        val next = if (mediaStoreId in current) current - mediaStoreId else current + mediaStoreId
        selectedIdsFlow.value = next
        if (next.isNotEmpty()) {
            selectionModeFlow.value = true
        }
    }

    fun selectAllVisible() {
        selectedIdsFlow.value = uiState.value.items.map { it.mediaStoreId }.toSet()
        selectionModeFlow.value = true
    }

    fun favoriteSelected(favorite: Boolean) {
        val ids = selectedIdsFlow.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { mediaIndexDao.setFavorite(it, favorite) }
            statusNoteFlow.value = "Updated ${ids.size} favorite(s)"
            toggleSelectionMode(false)
        }
    }

    fun addSelectedToCustomAlbum(albumId: Long) {
        val ids = selectedIdsFlow.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val members = ids.map { CustomAlbumMemberEntity(albumId = albumId, mediaStoreId = it) }
            customAlbumDao.addMembers(members)
            statusNoteFlow.value = "Added ${ids.size} item(s) to album"
            toggleSelectionMode(false)
        }
    }

    fun copySelectedToFolder(targetFolder: String) {
        val selected = uiState.value.items.filter { it.mediaStoreId in selectedIdsFlow.value }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            var copied = 0
            selected.forEach { item ->
                if (MediaStoreOperations.copyMediaToAlbumFolder(appContext, item, targetFolder) != null) {
                    copied++
                }
            }
            mediaRepository.syncDeviceMedia()
            statusNoteFlow.value = "Copied $copied item(s) to $targetFolder"
            toggleSelectionMode(false)
        }
    }

    fun moveSelectedToTrash() {
        val selected = uiState.value.items.filter { it.mediaStoreId in selectedIdsFlow.value }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val count = trashRepository.moveItemsToTrash(selected)
            statusNoteFlow.value = "Moved $count item(s) to Recently Deleted"
            toggleSelectionMode(false)
        }
    }

    fun moveSelectedToVault(onDone: (Int) -> Unit = {}) {
        val selected = uiState.value.items.filter { it.mediaStoreId in selectedIdsFlow.value }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            var encrypted = 0
            for (item in selected) {
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
                    encrypted++
                }
            }
            if (encrypted > 0) {
                mediaIndexDao.deleteByIds(selected.map { it.mediaStoreId })
            }
            statusNoteFlow.value = "Encrypted $encrypted item(s) into Private Vault"
            toggleSelectionMode(false)
            onDone(encrypted)
        }
    }

    fun getSelectedMediaItems(): List<MediaIndexEntity> {
        val ids = selectedIdsFlow.value
        return uiState.value.items.filter { it.mediaStoreId in ids }
    }

    fun clearStatusNote() {
        statusNoteFlow.value = null
    }

    class Factory(
        private val context: Context,
        private val mediaRepository: DeviceMediaRepository,
        private val mediaIndexDao: MediaIndexDao,
        private val customAlbumDao: CustomAlbumDao,
        private val trashRepository: TrashRepository,
        private val vaultCryptoManager: VaultCryptoManager,
        private val vaultDao: VaultDao,
        private val preferencesManager: GalleryPreferencesManager,
        private val intelligenceCoordinator: IntelligenceCoordinator,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryHomeViewModel(
                appContext = context.applicationContext,
                mediaRepository = mediaRepository,
                mediaIndexDao = mediaIndexDao,
                customAlbumDao = customAlbumDao,
                trashRepository = trashRepository,
                vaultCryptoManager = vaultCryptoManager,
                vaultDao = vaultDao,
                preferencesManager = preferencesManager,
                intelligenceCoordinator = intelligenceCoordinator,
            ) as T
        }
    }
}
