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

data class CustomAlbumWithItems(
    val album: CustomAlbumEntity,
    val items: List<MediaIndexEntity>,
)

data class PhysicalBucketGroup(
    val name: String,
    val relativePath: String,
    val items: List<MediaIndexEntity>,
)

data class AlbumsUiState(
    val customAlbums: List<CustomAlbumWithItems> = emptyList(),
    val physicalBuckets: List<PhysicalBucketGroup> = emptyList(),
    val allMedia: List<MediaIndexEntity> = emptyList(),
    val selectedCustomAlbumId: Long? = null,
    val selectedPhysicalBucketName: String? = null,
)

class AlbumsViewModel(
    private val customAlbumDao: CustomAlbumDao,
    private val mediaIndexDao: MediaIndexDao,
) : ViewModel() {

    private val selectedCustomIdFlow = MutableStateFlow<Long?>(null)
    private val selectedBucketNameFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AlbumsUiState> = combine(
        customAlbumDao.observeAllAlbums(),
        customAlbumDao.observeAllMembers(),
        mediaIndexDao.observeAllMedia(),
        selectedCustomIdFlow,
        selectedBucketNameFlow,
    ) { albums, members, allMedia, selCustom, selBucket ->
        val mediaById = allMedia.associateBy { it.mediaStoreId }
        val membersByAlbum = members.groupBy { it.albumId }

        val customWithItems = albums.map { album ->
            val items = membersByAlbum[album.albumId]
                .orEmpty()
                .mapNotNull { mediaById[it.mediaStoreId] }
            CustomAlbumWithItems(album = album, items = items)
        }

        val physical = allMedia
            .groupBy { it.albumName.ifBlank { "Internal" } }
            .map { (name, list) ->
                PhysicalBucketGroup(
                    name = name,
                    relativePath = list.firstOrNull()?.relativePath.orEmpty(),
                    items = list,
                )
            }
            .sortedByDescending { it.items.size }

        AlbumsUiState(
            customAlbums = customWithItems,
            physicalBuckets = physical,
            allMedia = allMedia,
            selectedCustomAlbumId = selCustom,
            selectedPhysicalBucketName = selBucket,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumsUiState(),
    )

    fun createAlbum(name: String, description: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            customAlbumDao.upsertAlbum(CustomAlbumEntity(name = clean, description = description.trim()))
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
    }

    fun openPhysicalBucket(bucketName: String?) {
        selectedBucketNameFlow.value = bucketName
        selectedCustomIdFlow.value = null
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
