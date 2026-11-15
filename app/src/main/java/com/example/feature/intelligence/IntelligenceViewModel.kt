package com.example.feature.intelligence

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.FaceDao
import com.example.core.database.FaceDetectionEntity
import com.example.core.database.FaceGroupEntity
import com.example.core.database.MediaIndexDao
import com.example.core.database.MediaIndexEntity
import com.example.core.datastore.GalleryPreferences
import com.example.core.datastore.GalleryPreferencesManager
import com.example.feature.intelligence.engine.FaceClusteringEngine
import com.example.feature.intelligence.engine.IndexingProgressState
import com.example.feature.intelligence.engine.IntelligenceCoordinator
import com.example.feature.intelligence.engine.IntelligenceSummary
import com.example.feature.intelligence.engine.ModelAvailabilityChecker
import com.example.feature.intelligence.engine.OnDeviceModelStatus
import com.example.feature.intelligence.engine.SmartCollectionsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IntelligenceUiState(
    val preferences: GalleryPreferences = GalleryPreferences(),
    val indexingProgress: IndexingProgressState = IndexingProgressState(),
    val summary: IntelligenceSummary = IntelligenceSummary(),
    val faceGroups: List<FaceGroupEntity> = emptyList(),
    val faceDetections: List<FaceDetectionEntity> = emptyList(),
    val modelStatuses: List<OnDeviceModelStatus> = emptyList(),
    val totalMediaCount: Int = 0,
    val aiIndexedCount: Int = 0,
    val ocrCount: Int = 0,
    val gpsCount: Int = 0,
)

class IntelligenceViewModel(
    private val appContext: Context,
    private val mediaIndexDao: MediaIndexDao,
    private val faceDao: FaceDao,
    private val preferencesManager: GalleryPreferencesManager,
    private val coordinator: IntelligenceCoordinator,
    private val faceClusteringEngine: FaceClusteringEngine,
) : ViewModel() {

    val uiState: StateFlow<IntelligenceUiState> = combine(
        preferencesManager.preferences,
        coordinator.progress,
        mediaIndexDao.observeAllMedia(),
        faceDao.observeAllGroups(),
        faceDao.observeAllFaces(),
    ) { prefs, progress, allMedia, groups, faces ->
        val summary = withContext(Dispatchers.Default) {
            SmartCollectionsEngine.buildSummary(allMedia, groups.size)
        }
        val models = ModelAvailabilityChecker.inspectModels(appContext, prefs.faceAnalysisEnabled)
        IntelligenceUiState(
            preferences = prefs,
            indexingProgress = progress,
            summary = summary,
            faceGroups = groups,
            faceDetections = faces,
            modelStatuses = models,
            totalMediaCount = allMedia.size,
            aiIndexedCount = allMedia.count { it.aiIndexedAtMillis > 0L },
            ocrCount = allMedia.count { it.ocrText.isNotBlank() },
            gpsCount = allMedia.count { it.hasGps },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IntelligenceUiState(
            modelStatuses = ModelAvailabilityChecker.inspectModels(appContext, faceAnalysisEnabled = false),
        ),
    )

    fun startIndexing(forceReanalyzeAll: Boolean = false) {
        coordinator.startIncrementalIndex(forceReanalyzeAll = forceReanalyzeAll)
    }

    fun scheduleBackgroundIndexing() {
        coordinator.scheduleBackgroundIndex()
    }

    fun cancelIndexing() {
        coordinator.cancelIndexing()
    }

    fun setFaceAnalysisEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setFaceAnalysisEnabled(enabled)
            if (enabled) {
                preferencesManager.setFaceGroupingEnabled(true)
            }
        }
    }

    fun renameFaceGroup(groupId: String, newName: String) {
        viewModelScope.launch {
            faceClusteringEngine.renameGroup(groupId, newName)
        }
    }

    fun mergeFaceGroups(sourceGroupId: String, targetGroupId: String) {
        viewModelScope.launch {
            faceClusteringEngine.mergeGroups(sourceGroupId, targetGroupId)
        }
    }

    fun removeFaceFromGroup(faceId: String) {
        viewModelScope.launch {
            faceClusteringEngine.removeFaceFromGroup(faceId)
        }
    }

    fun deleteFaceGroup(groupId: String) {
        viewModelScope.launch {
            faceClusteringEngine.deleteGroup(groupId)
        }
    }

    fun deleteAllFaceData() {
        viewModelScope.launch {
            faceClusteringEngine.deleteAllFaceData()
            preferencesManager.setFaceAnalysisEnabled(false)
        }
    }

    class Factory(
        private val context: Context,
        private val mediaIndexDao: MediaIndexDao,
        private val faceDao: FaceDao,
        private val preferencesManager: GalleryPreferencesManager,
        private val coordinator: IntelligenceCoordinator,
        private val faceClusteringEngine: FaceClusteringEngine,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IntelligenceViewModel(
                appContext = context.applicationContext,
                mediaIndexDao = mediaIndexDao,
                faceDao = faceDao,
                preferencesManager = preferencesManager,
                coordinator = coordinator,
                faceClusteringEngine = faceClusteringEngine,
            ) as T
        }
    }
}
