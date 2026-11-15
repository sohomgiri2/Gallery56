package com.example.feature.intelligence.engine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.Gallery56App
import com.example.core.database.MediaIndexDao
import com.example.core.datastore.GalleryPreferencesManager
import com.example.core.media.DeviceMediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class IndexingProgressState(
    val isRunning: Boolean = false,
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentFileName: String = "",
    val lastCompletedAtMillis: Long = 0L,
    val statusMessage: String = "Ready for offline AI indexing",
) {
    val progressFraction: Float
        get() = if (totalCount > 0) (processedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
}

class IntelligenceCoordinator(
    private val context: Context,
    private val mediaRepository: DeviceMediaRepository,
    private val mediaIndexDao: MediaIndexDao,
    private val aiAnalyzer: OnDeviceAiAnalyzer,
    private val preferencesManager: GalleryPreferencesManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null

    private val _progress = MutableStateFlow(IndexingProgressState())
    val progress: StateFlow<IndexingProgressState> = _progress.asStateFlow()

    fun startIncrementalIndex(forceReanalyzeAll: Boolean = false) {
        if (activeJob?.isActive == true) return
        activeJob = scope.launch {
            runIndexingPass(forceReanalyzeAll)
        }
    }

    fun cancelIndexing() {
        activeJob?.cancel()
        activeJob = null
        _progress.value = _progress.value.copy(
            isRunning = false,
            statusMessage = "Indexing paused by user",
        )
    }

    fun scheduleBackgroundIndex() {
        val request = OneTimeWorkRequestBuilder<MediaIndexingWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "gallery56_offline_ai_index",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    suspend fun runIndexingPass(forceReanalyzeAll: Boolean = false) {
        try {
            _progress.value = IndexingProgressState(
                isRunning = true,
                statusMessage = "Syncing device MediaStore catalog...",
            )
            mediaRepository.syncDeviceMedia()

            val prefs = preferencesManager.preferences.first()
            val allMedia = mediaIndexDao.getAllMediaSnapshot()
            val targets = if (forceReanalyzeAll) {
                allMedia
            } else {
                allMedia.filter {
                    it.aiIndexedAtMillis == 0L ||
                        it.indexVersion < OnDeviceAiAnalyzer.CURRENT_INDEX_VERSION ||
                        (prefs.faceAnalysisEnabled && !it.isVideo && it.faceCount == 0 && it.aiIndexedAtMillis < prefs.lastFullIndexAtMillis)
                }
            }

            if (targets.isEmpty()) {
                val now = System.currentTimeMillis()
                preferencesManager.setLastFullIndexAt(now)
                _progress.value = IndexingProgressState(
                    isRunning = false,
                    processedCount = allMedia.size,
                    totalCount = allMedia.size,
                    lastCompletedAtMillis = now,
                    statusMessage = "All ${allMedia.size} items indexed & searchable offline",
                )
                return
            }

            _progress.value = IndexingProgressState(
                isRunning = true,
                processedCount = 0,
                totalCount = targets.size,
                statusMessage = "Analyzing ${targets.size} items on-device...",
            )

            targets.forEachIndexed { index, entity ->
                _progress.value = _progress.value.copy(
                    processedCount = index,
                    totalCount = targets.size,
                    currentFileName = entity.displayName,
                    statusMessage = "Indexing ${index + 1}/${targets.size}: ${entity.displayName}",
                )
                val analyzed = aiAnalyzer.analyzeMediaItem(entity, prefs.faceAnalysisEnabled)
                mediaIndexDao.upsert(analyzed)
            }

            // Update burst groupId & bestShot flags in DB
            val updatedMedia = mediaIndexDao.getAllMediaSnapshot()
            val summary = SmartCollectionsEngine.buildSummary(updatedMedia, 0)
            for (cluster in summary.burstGroups) {
                for (member in cluster.items) {
                    mediaIndexDao.updateGrouping(
                        id = member.mediaStoreId,
                        groupId = cluster.clusterId,
                        isBestShot = member.mediaStoreId == cluster.bestShotId,
                    )
                }
            }

            val completedAt = System.currentTimeMillis()
            preferencesManager.setLastFullIndexAt(completedAt)
            _progress.value = IndexingProgressState(
                isRunning = false,
                processedCount = targets.size,
                totalCount = targets.size,
                lastCompletedAtMillis = completedAt,
                statusMessage = "Indexed ${targets.size} items offline • Ready",
            )
        } catch (_: CancellationException) {
            _progress.value = _progress.value.copy(
                isRunning = false,
                statusMessage = "Indexing cancelled",
            )
        } catch (t: Throwable) {
            _progress.value = _progress.value.copy(
                isRunning = false,
                statusMessage = "Indexing stopped: ${t.message ?: "Unknown error"}",
            )
        }
    }
}

class MediaIndexingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? Gallery56App ?: return Result.failure()
        app.container.intelligenceCoordinator.runIndexingPass(forceReanalyzeAll = false)
        return Result.success()
    }
}
