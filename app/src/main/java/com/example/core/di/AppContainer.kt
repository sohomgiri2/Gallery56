package com.example.core.di

import android.content.Context
import com.example.core.database.Gallery56Database
import com.example.core.datastore.GalleryPreferencesManager
import com.example.core.media.DeviceMediaRepository
import com.example.core.media.TrashRepository
import com.example.core.security.VaultCryptoManager
import com.example.feature.backup.BackupRestoreManager
import com.example.feature.intelligence.engine.FaceClusteringEngine
import com.example.feature.intelligence.engine.IntelligenceCoordinator
import com.example.feature.intelligence.engine.OnDeviceAiAnalyzer
import com.example.feature.search.engine.AdvancedSearchEngine

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: Gallery56Database by lazy {
        Gallery56Database.getInstance(appContext)
    }

    val preferencesManager: GalleryPreferencesManager by lazy {
        GalleryPreferencesManager(appContext)
    }

    val mediaRepository: DeviceMediaRepository by lazy {
        DeviceMediaRepository(
            context = appContext,
            mediaIndexDao = database.mediaIndexDao(),
        )
    }

    val vaultCryptoManager: VaultCryptoManager by lazy {
        VaultCryptoManager(appContext)
    }

    val trashRepository: TrashRepository by lazy {
        TrashRepository(
            context = appContext,
            trashDao = database.trashDao(),
            mediaIndexDao = database.mediaIndexDao(),
        )
    }

    val backupRestoreManager: BackupRestoreManager by lazy {
        BackupRestoreManager(
            context = appContext,
            preferencesManager = preferencesManager,
            savedSearchDao = database.savedSearchDao(),
            customAlbumDao = database.customAlbumDao(),
            mediaIndexDao = database.mediaIndexDao(),
        )
    }

    val faceClusteringEngine: FaceClusteringEngine by lazy {
        FaceClusteringEngine(
            context = appContext,
            faceDao = database.faceDao(),
            mediaIndexDao = database.mediaIndexDao(),
        )
    }

    val aiAnalyzer: OnDeviceAiAnalyzer by lazy {
        OnDeviceAiAnalyzer(
            context = appContext,
            faceClusteringEngine = faceClusteringEngine,
        )
    }

    val intelligenceCoordinator: IntelligenceCoordinator by lazy {
        IntelligenceCoordinator(
            context = appContext,
            mediaRepository = mediaRepository,
            mediaIndexDao = database.mediaIndexDao(),
            aiAnalyzer = aiAnalyzer,
            preferencesManager = preferencesManager,
        )
    }

    val advancedSearchEngine: AdvancedSearchEngine by lazy {
        AdvancedSearchEngine(
            mediaIndexDao = database.mediaIndexDao(),
            faceDao = database.faceDao(),
        )
    }
}
