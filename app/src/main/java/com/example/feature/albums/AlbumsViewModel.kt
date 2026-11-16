package com.example.feature.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.CustomAlbumDao
import com.example.core.database.CustomAlbumEntity
import com.example.core.database.CustomAlbumMemberEntity
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AlbumSortMode(val label: String) {
    PINNED_FIRST("Pinned"),
    CUSTOM_ORDER("Custom Order"),
    NAME_ASC("Name A-Z"),
    COUNT_DESC("Most Items"),
    DATE_DESC("Newest"),
}

enum class SmartAlbumType(val id: String, val label: String) {
    FAVORITES("favorites", "Favorites"),
    VIDEOS("videos", "Videos"),
    SCREENSHOTS("screenshots", "Screenshots"),
    LARGE_FILES("large_files", "Large Files"),
    RECENTLY_VIEWED("recently_viewed", "Recently Viewed"),
    RECENTLY_EDITED("recently_edited", "Recently Edited"),
}

data class CustomAlbumWithItems(
    val album: CustomAlbumEntity,
    val items: List<MediaIndexEntity>,
)

data class PhysicalBucketGroup(
    val name: String,
    val relativePath: String,
    val items: List<MediaIndexEntity>,
)

data class SmartAlbumGroup(
    val type: SmartAlbumType,
    val items: List<MediaIndexEntity>,
)

data class AlbumsUiState(
    val customAlbums: List<CustomAlbumWithItems> = emptyList(),
    val physicalBuckets: List<PhysicalBucketGroup> = emptyList(),
    val smartAlbums: List<SmartAlbumGroup> = emptyList(),
    val allMedia: List<MediaIndexEntity> = emptyList(),
    val sortMode: AlbumSortMode = AlbumSortMode.PINNED_FIRST,
    val selectedCustomAlbumId: Long? = null,
    val selectedPhysicalBucketName: String? = null,
    val selectedSmartAlbumType: SmartAlbumType? = null,
)

class AlbumsViewModel(
    private val customAlbumDao: CustomAlbumDao,
    private val mediaIndexDao: MediaIndexDao,
) : ViewModel() {

    private val selectedCustomIdFlow = MutableStateFlow<Long?>(null)
    private val selectedBucketNameFlow = MutableStateFlow<String?>(null)
    private val selectedSmartAlbumFlow = MutableStateFlow<SmartAlbumType?>(null)
    private val sortModeFlow = MutableStateFlow(AlbumSortMode.PINNED_FIRST)

    private data class AlbumSelections(
        val sortMode: AlbumSortMode,
        val customId: Long?,
        val bucketName: String?,
        val smartType: SmartAlbumType?,
    )

    private val selectionsFlow = combine(
        sortModeFlow,
        selectedCustomIdFlow,
        selectedBucketNameFlow,
        selectedSmartAlbumFlow,
    ) { sortMode, customId, bucketName, smartType ->
        AlbumSelections(sortMode, customId, bucketName, smartType)
    }

    val uiState: StateFlow<AlbumsUiState> = combine(
        customAlbumDao.observeAllAlbums(),
        customAlbumDao.observeAllMembers(),
        mediaIndexDao.observeAllMedia(),
        selectionsFlow,
    ) { albums, members, allMedia, sel ->
        val mediaById = allMedia.associateBy { it.mediaStoreId }
        val membersByAlbum = members.groupBy { it.albumId }

        val customRaw = albums.map { album ->
            val items = membersByAlbum[album.albumId]
                .orEmpty()
                .mapNotNull { mediaById[it.mediaStoreId] }
            CustomAlbumWithItems(album = album, items = items)
        }

        val customSorted = when (sel.sortMode) {
            AlbumSortMode.PINNED_FIRST -> customRaw.sortedWith(
                compareByDescending<CustomAlbumWithItems> { it.album.isPinned }
                    .thenBy { it.album.sortOrder }
                    .thenByDescending { it.album.createdAtMillis },
            )
            AlbumSortMode.CUSTOM_ORDER -> customRaw.sortedWith(
                compareBy<CustomAlbumWithItems> { it.album.sortOrder }
                    .thenByDescending { it.album.createdAtMillis },
            )
            AlbumSortMode.NAME_ASC -> customRaw.sortedBy { it.album.name.lowercase() }
            AlbumSortMode.COUNT_DESC -> customRaw.sortedByDescending { it.items.size }
            AlbumSortMode.DATE_DESC -> customRaw.sortedByDescending { it.album.createdAtMillis }
        }

        val physicalRaw = allMedia
            .groupBy { it.albumName.ifBlank { "Internal" } }
            .map { (name, list) ->
                PhysicalBucketGroup(
                    name = name,
                    relativePath = list.firstOrNull()?.relativePath.orEmpty(),
                    items = list,
                )
            }
        val physicalSorted = when (sel.sortMode) {
            AlbumSortMode.NAME_ASC -> physicalRaw.sortedBy { it.name.lowercase() }
            AlbumSortMode.DATE_DESC -> physicalRaw.sortedByDescending { g -> g.items.maxOfOrNull { it.dateTakenMillis } ?: 0L }
            else -> physicalRaw.sortedByDescending { it.items.size }
        }

        val smart = listOf(
            SmartAlbumGroup(
                type = SmartAlbumType.FAVORITES,
                items = allMedia.filter { it.isFavorite },
            ),
            SmartAlbumGroup(
                type = SmartAlbumType.VIDEOS,
                items = allMedia.filter { it.isVideo },
            ),
            SmartAlbumGroup(
                type = SmartAlbumType.SCREENSHOTS,
                items = allMedia.filter {
                    it.isScreenshot || it.albumName.contains("screenshot", ignoreCase = true)
                },
            ),
            SmartAlbumGroup(
                type = SmartAlbumType.LARGE_FILES,
                items = allMedia.filter { it.fileSizeBytes >= 5L * 1024L * 1024L }
                    .sortedByDescending { it.fileSizeBytes },
            ),
            SmartAlbumGroup(
                type = SmartAlbumType.RECENTLY_VIEWED,
                items = allMedia.filter { it.lastViewedAtMillis > 0L }
                    .sortedByDescending { it.lastViewedAtMillis },
            ),
            SmartAlbumGroup(
                type = SmartAlbumType.RECENTLY_EDITED,
                items = allMedia.filter { it.lastEditedAtMillis > 0L }
                    .sortedByDescending { it.lastEditedAtMillis },
            ),
        )

        AlbumsUiState(
            customAlbums = customSorted,
            physicalBuckets = physicalSorted,
            smartAlbums = smart,
            allMedia = allMedia,
            sortMode = sel.sortMode,
            selectedCustomAlbumId = sel.customId,
            selectedPhysicalBucketName = sel.bucketName,
            selectedSmartAlbumType = sel.smartType,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumsUiState(),
    )

    fun setSortMode(mode: AlbumSortMode) {
        sortModeFlow.value = mode
    }

    fun createAlbum(name: String, description: String, colorHex: String = "") {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val existing = customAlbumDao.getAllAlbumsSnapshot()
            val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
            customAlbumDao.upsertAlbum(
                CustomAlbumEntity(
                    name = clean,
                    description = description.trim(),
                    colorLabelHex = colorHex.trim(),
                    sortOrder = nextOrder,
                ),
            )
        }
    }

    fun renameAlbum(albumId: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { customAlbumDao.renameAlbum(albumId, clean) }
    }

    fun togglePinAlbum(album: CustomAlbumEntity) {
        viewModelScope.launch { customAlbumDao.setPinned(album.albumId, !album.isPinned) }
    }

    fun setAlbumCover(albumId: Long, coverUri: String?) {
        viewModelScope.launch { customAlbumDao.setCoverUri(albumId, coverUri) }
    }

    fun setAlbumColorLabel(albumId: Long, colorHex: String) {
        viewModelScope.launch { customAlbumDao.updateColorLabel(albumId, colorHex) }
    }

    fun moveAlbumOrder(albumId: Long, delta: Int) {
        viewModelScope.launch {
            val ordered = customAlbumDao.getAllAlbumsSnapshot().toMutableList()
            val index = ordered.indexOfFirst { it.albumId == albumId }
            if (index < 0) return@launch
            val target = (index + delta).coerceIn(0, ordered.lastIndex)
            if (index == target) return@launch
            val moved = ordered.removeAt(index)
            ordered.add(target, moved)
            ordered.forEachIndexed { idx, entity ->
                customAlbumDao.updateSortOrder(entity.albumId, idx)
            }
            sortModeFlow.value = AlbumSortMode.CUSTOM_ORDER
        }
    }

    fun addItemsToAlbum(albumId: Long, mediaIds: List<Long>) {
        if (mediaIds.isEmpty()) return
        viewModelScope.launch {
            customAlbumDao.addMembers(mediaIds.map { CustomAlbumMemberEntity(albumId, it) })
        }
    }

    fun removeItemFromAlbum(albumId: Long, mediaStoreId: Long) {
        viewModelScope.launch { customAlbumDao.removeMember(albumId, mediaStoreId) }
    }

    fun deleteAlbum(albumId: Long) {
        viewModelScope.launch {
            customAlbumDao.deleteAllMembersOfAlbum(albumId)
            customAlbumDao.deleteAlbum(albumId)
            if (selectedCustomIdFlow.value == albumId) {
                selectedCustomIdFlow.value = null
            }
        }
    }

    fun openCustomAlbum(albumId: Long?) {
        selectedCustomIdFlow.value = albumId
        selectedBucketNameFlow.value = null
        selectedSmartAlbumFlow.value = null
    }

    fun openPhysicalBucket(bucketName: String?) {
        selectedBucketNameFlow.value = bucketName
        selectedCustomIdFlow.value = null
        selectedSmartAlbumFlow.value = null
    }

    fun openSmartAlbum(type: SmartAlbumType?) {
        selectedSmartAlbumFlow.value = type
        selectedCustomIdFlow.value = null
        selectedBucketNameFlow.value = null
    }

    class Factory(
        private val customAlbumDao: CustomAlbumDao,
        private val mediaIndexDao: MediaIndexDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlbumsViewModel(customAlbumDao, mediaIndexDao) as T
        }
    }
}
