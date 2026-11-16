package com.example.feature.viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AiLabelScore
import com.example.core.database.AiObjectDetectionInfo
import com.example.core.database.FaceDao
import com.example.core.database.FaceDetectionEntity
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import com.example.core.database.VaultDao
import com.example.core.database.VaultItemEntity
import com.example.core.datastore.GalleryPreferencesManager
import com.example.core.media.TrashRepository
import com.example.core.security.VaultCryptoManager
import com.example.feature.intelligence.engine.OnDeviceAiAnalyzer
import com.example.feature.intelligence.engine.PerceptualHashAndQualityAnalyzer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID

data class MediaViewerUiState(
    val media: MediaIndexEntity? = null,
    val aiLabels: List<AiLabelScore> = emptyList(),
    val aiObjects: List<AiObjectDetectionInfo> = emptyList(),
    val faces: List<FaceDetectionEntity> = emptyList(),
    val visuallySimilar: List<MediaIndexEntity> = emptyList(),
    val isAnalyzingNow: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewerViewModel(
    initialMediaStoreId: Long,
    private val mediaIndexDao: MediaIndexDao,
    private val faceDao: FaceDao,
    private val aiAnalyzer: OnDeviceAiAnalyzer,
    private val preferencesManager: GalleryPreferencesManager,
    private val trashRepository: TrashRepository,
    private val vaultCryptoManager: VaultCryptoManager,
    private val vaultDao: VaultDao,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private val activeMediaIdFlow = MutableStateFlow(initialMediaStoreId)
    private val analyzingNowFlow = MutableStateFlow(false)

    private val activeItemFlow = activeMediaIdFlow.flatMapLatest { id ->
        mediaIndexDao.observeById(id)
    }

    val uiState: StateFlow<MediaViewerUiState> = combine(
        activeItemFlow,
        mediaIndexDao.observeAllMedia(),
        faceDao.observeAllFaces(),
        analyzingNowFlow,
    ) { item, allMedia, allFaces, analyzing ->
        if (item == null) {
            MediaViewerUiState(isAnalyzingNow = analyzing)
        } else {
            val labels = try {
                json.decodeFromString<List<AiLabelScore>>(item.aiLabelsJson)
            } catch (_: Throwable) {
                emptyList()
            }
            val objects = try {
                json.decodeFromString<List<AiObjectDetectionInfo>>(item.aiObjectsJson)
            } catch (_: Throwable) {
                emptyList()
            }
            val itemFaces = allFaces.filter { it.mediaStoreId == item.mediaStoreId }
            val similar = if (item.dHash != 0L) {
                allMedia
                    .filter { it.mediaStoreId != item.mediaStoreId && it.dHash != 0L }
                    .map { it to PerceptualHashAndQualityAnalyzer.visualSimilarityScore(item, it) }
                    .filter { it.second >= 0.72f }
                    .sortedByDescending { it.second }
                    .take(8)
                    .map { it.first }
            } else emptyList()

            MediaViewerUiState(
                media = item,
                aiLabels = labels,
                aiObjects = objects,
                faces = itemFaces,
                visuallySimilar = similar,
                isAnalyzingNow = analyzing,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaViewerUiState(),
    )

    init {
        if (initialMediaStoreId > 0L) {
            viewModelScope.launch {
                mediaIndexDao.markViewed(initialMediaStoreId, System.currentTimeMillis())
            }
        }
    }

    fun setActiveMediaId(id: Long) {
        if (activeMediaIdFlow.value != id) {
            activeMediaIdFlow.value = id
            viewModelScope.launch {
                mediaIndexDao.markViewed(id, System.currentTimeMillis())
            }
        }
    }

    fun toggleFavorite() {
        val current = uiState.value.media ?: return
        viewModelScope.launch {
            mediaIndexDao.setFavorite(current.mediaStoreId, !current.isFavorite)
        }
    }

    fun setRating(stars: Int) {
        val current = uiState.value.media ?: return
        viewModelScope.launch {
            mediaIndexDao.setRating(current.mediaStoreId, stars.coerceIn(0, 5))
        }
    }

    fun saveUserMetadata(tags: String, notes: String) {
        val current = uiState.value.media ?: return
        viewModelScope.launch {
            mediaIndexDao.updateUserMetadata(
                id = current.mediaStoreId,
                tags = tags.trim(),
                notes = notes.trim(),
            )
        }
    }

    fun analyzeSingleItemNow() {
        val current = uiState.value.media ?: return
        if (analyzingNowFlow.value) return
        viewModelScope.launch {
            analyzingNowFlow.value = true
            val prefs = preferencesManager.preferences.first()
            val updated = aiAnalyzer.analyzeMediaItem(current, prefs.faceAnalysisEnabled)
            mediaIndexDao.upsert(updated)
            analyzingNowFlow.value = false
        }
    }

    fun moveToTrash(item: MediaIndexEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            trashRepository.moveItemsToTrash(listOf(item))
            onDone()
        }
    }

    fun moveToVault(item: MediaIndexEntity, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
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
                mediaIndexDao.deleteByIds(listOf(item.mediaStoreId))
                onDone(true)
            } else {
                onDone(false)
            }
        }
    }

    class Factory(
        private val initialMediaStoreId: Long,
        private val mediaIndexDao: MediaIndexDao,
        private val faceDao: FaceDao,
        private val aiAnalyzer: OnDeviceAiAnalyzer,
        private val preferencesManager: GalleryPreferencesManager,
        private val trashRepository: TrashRepository,
        private val vaultCryptoManager: VaultCryptoManager,
        private val vaultDao: VaultDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MediaViewerViewModel(
                initialMediaStoreId = initialMediaStoreId,
                mediaIndexDao = mediaIndexDao,
                faceDao = faceDao,
                aiAnalyzer = aiAnalyzer,
                preferencesManager = preferencesManager,
                trashRepository = trashRepository,
                vaultCryptoManager = vaultCryptoManager,
                vaultDao = vaultDao,
            ) as T
        }
    }
}
