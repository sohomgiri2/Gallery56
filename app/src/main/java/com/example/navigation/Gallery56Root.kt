package com.example.navigation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.datastore.NavigationStyle
import com.example.core.di.AppContainer
import com.example.feature.albums.AlbumsScreen
import com.example.feature.albums.AlbumsViewModel
import com.example.feature.backup.BackupRestoreScreen
import com.example.feature.collage.CollageStudioScreen
import com.example.feature.developer.DeveloperInfoScreen
import com.example.feature.editor.PhotoEditorScreen
import com.example.feature.forensics.MetadataViewerScreen
import com.example.feature.home.GalleryHomeScreen
import com.example.feature.home.GalleryHomeViewModel
import com.example.feature.intelligence.IntelligenceScreen
import com.example.feature.intelligence.IntelligenceViewModel
import com.example.feature.memories.MemoriesScreen
import com.example.feature.poster.PosterStudioScreen
import com.example.feature.scanner.DocumentScannerScreen
import com.example.feature.search.SearchScreen
import com.example.feature.search.SearchViewModel
import com.example.feature.storage.StorageManagerScreen
import com.example.feature.studio.CustomizationStudioScreen
import com.example.feature.studio.ToolsHubScreen
import com.example.feature.trash.RecentlyDeletedScreen
import com.example.feature.vault.PrivateVaultScreen
import com.example.feature.vault.PrivateVaultViewModel
import com.example.feature.videos.VideoPlayerScreen
import com.example.feature.viewer.MediaViewerScreen
import com.example.feature.viewer.MediaViewerViewModel
import com.example.ui.theme.Gallery56Theme
import com.example.ui.theme.GalleryThemePreset
import com.example.ui.theme.LocalGallery56Tokens
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object GalleryRoute
@Serializable data object AlbumsRoute
@Serializable data object SearchRoute
@Serializable data object AiStudioRoute
@Serializable data object ToolsHubRoute

@Serializable data class MediaViewerRoute(val mediaStoreId: Long)
@Serializable data class VideoPlayerRoute(val mediaStoreId: Long)
@Serializable data class PhotoEditorRoute(val mediaStoreId: Long = -1L)
@Serializable data class MetadataViewerRoute(val mediaStoreId: Long)
@Serializable data object PrivateVaultRoute
@Serializable data object RecentlyDeletedRoute
@Serializable data object CollageStudioRoute
@Serializable data object PosterStudioRoute
@Serializable data object DocumentScannerRoute
@Serializable data object StorageManagerRoute
@Serializable data object CustomizationStudioRoute
@Serializable data object MemoriesRoute
@Serializable data object BackupRestoreRoute
@Serializable data object DeveloperInfoRoute

private data class TopLevelDestination<T : Any>(
    val route: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

@Composable
fun Gallery56Root(appContainer: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val homeViewModel: GalleryHomeViewModel = viewModel(
        factory = GalleryHomeViewModel.Factory(
            context = context,
            mediaRepository = appContainer.mediaRepository,
            mediaIndexDao = appContainer.database.mediaIndexDao(),
            customAlbumDao = appContainer.database.customAlbumDao(),
            trashRepository = appContainer.trashRepository,
            vaultCryptoManager = appContainer.vaultCryptoManager,
            vaultDao = appContainer.database.vaultDao(),
            preferencesManager = appContainer.preferencesManager,
            intelligenceCoordinator = appContainer.intelligenceCoordinator,
        ),
    )

    val albumsViewModel: AlbumsViewModel = viewModel(
        factory = AlbumsViewModel.Factory(
            customAlbumDao = appContainer.database.customAlbumDao(),
            mediaIndexDao = appContainer.database.mediaIndexDao(),
        ),
    )

    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(
            searchEngine = appContainer.advancedSearchEngine,
            mediaIndexDao = appContainer.database.mediaIndexDao(),
            faceDao = appContainer.database.faceDao(),
            savedSearchDao = appContainer.database.savedSearchDao(),
        ),
    )

    val intelligenceViewModel: IntelligenceViewModel = viewModel(
        factory = IntelligenceViewModel.Factory(
            context = context,
            mediaIndexDao = appContainer.database.mediaIndexDao(),
            faceDao = appContainer.database.faceDao(),
            preferencesManager = appContainer.preferencesManager,
            coordinator = appContainer.intelligenceCoordinator,
            faceClusteringEngine = appContainer.faceClusteringEngine,
        ),
    )

    val vaultViewModel: PrivateVaultViewModel = viewModel(
        factory = PrivateVaultViewModel.Factory(
            context = context,
            vaultDao = appContainer.database.vaultDao(),
            mediaIndexDao = appContainer.database.mediaIndexDao(),
            vaultCryptoManager = appContainer.vaultCryptoManager,
            preferencesManager = appContainer.preferencesManager,
        ),
    )

    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val albumsState by albumsViewModel.uiState.collectAsStateWithLifecycle()
    val searchState by searchViewModel.uiState.collectAsStateWithLifecycle()
    val aiState by intelligenceViewModel.uiState.collectAsStateWithLifecycle()
    val vaultState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val allMediaSnapshot by appContainer.database.mediaIndexDao()
        .observeAllMedia()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val trashItems by appContainer.database.trashDao()
        .observeAllTrashItems()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val vaultItems by appContainer.database.vaultDao()
        .observeAllVaultItems()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val prefs = homeState.preferences
    val themePreset = GalleryThemePreset.fromId(prefs.themeId)
    val activeCustomProfile = remember(prefs.activeCustomThemeId, prefs.customThemes) {
        prefs.customThemes.firstOrNull { it.id == prefs.activeCustomThemeId }
    }

    Gallery56Theme(
        preset = themePreset,
        customProfile = activeCustomProfile,
        customAccentHexOverride = prefs.customAccentHexOverride,
        cornerRadiusDp = prefs.cornerRadiusDp,
        gridSpacingDp = prefs.gridSpacingDp,
        reducedMotion = prefs.reducedMotion,
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDest = navBackStackEntry?.destination

        val isTopLevelRoute = currentDest?.let { dest ->
            dest.hasRoute(GalleryRoute::class) ||
                dest.hasRoute(AlbumsRoute::class) ||
                dest.hasRoute(SearchRoute::class) ||
                dest.hasRoute(AiStudioRoute::class) ||
                dest.hasRoute(ToolsHubRoute::class)
        } ?: true
        val isEdgeToEdgeViewer = currentDest?.let { dest ->
            dest.hasRoute(MediaViewerRoute::class) || dest.hasRoute(VideoPlayerRoute::class)
        } ?: false

        val destinations = listOf(
            TopLevelDestination(
                route = GalleryRoute,
                label = "Gallery",
                selectedIcon = Icons.Rounded.PhotoLibrary,
                unselectedIcon = Icons.Outlined.PhotoLibrary,
                testTag = "nav_gallery",
            ),
            TopLevelDestination(
                route = AlbumsRoute,
                label = "Albums",
                selectedIcon = Icons.Rounded.PhotoAlbum,
                unselectedIcon = Icons.Outlined.PhotoAlbum,
                testTag = "nav_albums",
            ),
            TopLevelDestination(
                route = SearchRoute,
                label = "Search",
                selectedIcon = Icons.Rounded.Search,
                unselectedIcon = Icons.Outlined.Search,
                testTag = "nav_search",
            ),
            TopLevelDestination(
                route = AiStudioRoute,
                label = "AI Hub",
                selectedIcon = Icons.Rounded.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                testTag = "nav_ai",
            ),
            TopLevelDestination(
                route = ToolsHubRoute,
                label = "Studio",
                selectedIcon = Icons.Rounded.Build,
                unselectedIcon = Icons.Outlined.Build,
                testTag = "nav_studio",
            ),
        )

        fun navigateTopLevel(route: Any) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        val ambientBackground = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.background,
            ),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientBackground),
        ) {
            val useNavigationRail = maxWidth >= 680.dp && isTopLevelRoute

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = if (isEdgeToEdgeViewer) WindowInsets(0) else WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!useNavigationRail && isTopLevelRoute) {
                        if (prefs.navigationStyle == NavigationStyle.FLOATING_PILL) {
                            val tokens = LocalGallery56Tokens.current
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(28.dp),
                                    color = tokens.glassSurface,
                                    tonalElevation = 6.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, tokens.glassBorder, RoundedCornerShape(28.dp)),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        destinations.forEach { dest ->
                                            val selected = currentDest?.hasRoute(dest.route::class) == true
                                            IconButton(
                                                onClick = { navigateTopLevel(dest.route) },
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(18.dp))
                                                    .background(
                                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                        else Color.Transparent,
                                                    )
                                                    .testTag(dest.testTag),
                                            ) {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.label,
                                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            ) {
                                destinations.forEach { dest ->
                                    val selected = currentDest?.hasRoute(dest.route::class) == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label,
                                            )
                                        },
                                        label = { Text(dest.label, maxLines = 1) },
                                        modifier = Modifier.testTag(dest.testTag),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            destinations.forEach { dest ->
                                val selected = currentDest?.hasRoute(dest.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label,
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag(dest.testTag),
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = GalleryRoute,
                            modifier = if (isEdgeToEdgeViewer) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 1040.dp)
                            },
                        ) {
                            composable<GalleryRoute> {
                                GalleryHomeScreen(
                                    uiState = homeState,
                                    onRefreshSync = { homeViewModel.refreshPermissionsAndSync(triggerAi = true) },
                                    onImportPickerUris = homeViewModel::importPickerUris,
                                    onSelectBucket = homeViewModel::selectBucket,
                                    onSelectAlbum = homeViewModel::selectAlbum,
                                    onSelectSortOrder = homeViewModel::setSortOrder,
                                    onCycleGridColumns = homeViewModel::setGridColumns,
                                    onToggleSelectionMode = homeViewModel::toggleSelectionMode,
                                    onToggleSelectItem = homeViewModel::toggleSelectItem,
                                    onSelectAllVisible = homeViewModel::selectAllVisible,
                                    onFavoriteSelected = homeViewModel::favoriteSelected,
                                    onAddSelectedToAlbum = homeViewModel::addSelectedToCustomAlbum,
                                    onCopySelectedToFolder = homeViewModel::copySelectedToFolder,
                                    onMoveSelectedToTrash = homeViewModel::moveSelectedToTrash,
                                    onMoveSelectedToVault = { homeViewModel.moveSelectedToVault() },
                                    onDismissStatusNote = homeViewModel::clearStatusNote,
                                    onOpenMedia = { id -> navController.navigate(MediaViewerRoute(id)) },
                                    onOpenSearch = { navigateTopLevel(SearchRoute) },
                                    onOpenAiStudio = { navigateTopLevel(AiStudioRoute) },
                                )
                            }

                            composable<AlbumsRoute> {
                                AlbumsScreen(
                                    uiState = albumsState,
                                    gridColumns = prefs.gridColumns,
                                    onCreateAlbum = albumsViewModel::createAlbum,
                                    onRenameAlbum = albumsViewModel::renameAlbum,
                                    onTogglePin = albumsViewModel::togglePinAlbum,
                                    onAddItemsToAlbum = albumsViewModel::addItemsToAlbum,
                                    onRemoveItemFromAlbum = albumsViewModel::removeItemFromAlbum,
                                    onDeleteAlbum = albumsViewModel::deleteAlbum,
                                    onOpenCustomAlbum = albumsViewModel::openCustomAlbum,
                                    onOpenPhysicalBucket = albumsViewModel::openPhysicalBucket,
                                    onOpenMedia = { id -> navController.navigate(MediaViewerRoute(id)) },
                                )
                            }

                            composable<SearchRoute> {
                                SearchScreen(
                                    uiState = searchState,
                                    gridColumns = prefs.gridColumns,
                                    onUpdateQuery = searchViewModel::updateQuery,
                                    onResetFilters = searchViewModel::resetFilters,
                                    onSaveSearch = searchViewModel::saveCurrentSearch,
                                    onApplySavedSearch = searchViewModel::applySavedSearch,
                                    onTogglePinSavedSearch = searchViewModel::togglePinSavedSearch,
                                    onDeleteSavedSearch = searchViewModel::deleteSavedSearch,
                                    onOpenMedia = { id -> navController.navigate(MediaViewerRoute(id)) },
                                )
                            }

                            composable<AiStudioRoute> {
                                IntelligenceScreen(
                                    uiState = aiState,
                                    onStartIndex = intelligenceViewModel::startIndexing,
                                    onScheduleBackgroundIndex = intelligenceViewModel::scheduleBackgroundIndexing,
                                    onCancelIndex = intelligenceViewModel::cancelIndexing,
                                    onToggleFaceAnalysis = intelligenceViewModel::setFaceAnalysisEnabled,
                                    onRenameFaceGroup = intelligenceViewModel::renameFaceGroup,
                                    onMergeFaceGroups = intelligenceViewModel::mergeFaceGroups,
                                    onRemoveFaceFromGroup = intelligenceViewModel::removeFaceFromGroup,
                                    onDeleteFaceGroup = intelligenceViewModel::deleteFaceGroup,
                                    onDeleteAllFaceData = intelligenceViewModel::deleteAllFaceData,
                                    onFilterByFaceGroup = { groupId ->
                                        searchViewModel.filterByFaceGroup(groupId)
                                        navigateTopLevel(SearchRoute)
                                    },
                                    onOpenSmartRecommendation = { tag ->
                                        searchViewModel.applySmartRecommendationFilter(tag)
                                        navigateTopLevel(SearchRoute)
                                    },
                                    onOpenMedia = { id -> navController.navigate(MediaViewerRoute(id)) },
                                )
                            }

                            composable<ToolsHubRoute> {
                                ToolsHubScreen(
                                    totalMediaCount = aiState.totalMediaCount,
                                    aiIndexedCount = aiState.aiIndexedCount,
                                    vaultCount = vaultItems.size,
                                    trashCount = trashItems.size,
                                    onOpenAlbums = { navigateTopLevel(AlbumsRoute) },
                                    onOpenMemories = { navController.navigate(MemoriesRoute) },
                                    onOpenPrivateVault = { navController.navigate(PrivateVaultRoute) },
                                    onOpenPhotoEditor = { navController.navigate(PhotoEditorRoute(-1L)) },
                                    onOpenCollageStudio = { navController.navigate(CollageStudioRoute) },
                                    onOpenPosterStudio = { navController.navigate(PosterStudioRoute) },
                                    onOpenDocumentScanner = { navController.navigate(DocumentScannerRoute) },
                                    onOpenStorageManager = { navController.navigate(StorageManagerRoute) },
                                    onOpenRecentlyDeleted = { navController.navigate(RecentlyDeletedRoute) },
                                    onOpenCustomizationStudio = { navController.navigate(CustomizationStudioRoute) },
                                    onOpenBackupRestore = { navController.navigate(BackupRestoreRoute) },
                                    onOpenAiHub = { navigateTopLevel(AiStudioRoute) },
                                    onOpenDeveloperInfo = { navController.navigate(DeveloperInfoRoute) },
                                )
                            }

                            composable<MediaViewerRoute> { backStackEntry ->
                                val route: MediaViewerRoute = backStackEntry.toRoute()
                                val viewerViewModel: MediaViewerViewModel = viewModel(
                                    key = "viewer_${route.mediaStoreId}",
                                    factory = MediaViewerViewModel.Factory(
                                        initialMediaStoreId = route.mediaStoreId,
                                        mediaIndexDao = appContainer.database.mediaIndexDao(),
                                        faceDao = appContainer.database.faceDao(),
                                        aiAnalyzer = appContainer.aiAnalyzer,
                                        preferencesManager = appContainer.preferencesManager,
                                        trashRepository = appContainer.trashRepository,
                                        vaultCryptoManager = appContainer.vaultCryptoManager,
                                        vaultDao = appContainer.database.vaultDao(),
                                    ),
                                )
                                val viewerState by viewerViewModel.uiState.collectAsStateWithLifecycle()

                                MediaViewerScreen(
                                    initialMediaId = route.mediaStoreId,
                                    allMedia = allMediaSnapshot,
                                    uiState = viewerState,
                                    onActiveMediaChanged = viewerViewModel::setActiveMediaId,
                                    onBack = { navController.popBackStack() },
                                    onToggleFavorite = viewerViewModel::toggleFavorite,
                                    onSetRating = viewerViewModel::setRating,
                                    onSaveTagsAndNotes = viewerViewModel::saveUserMetadata,
                                    onAnalyzeNow = viewerViewModel::analyzeSingleItemNow,
                                    onFindSimilar = { mediaId ->
                                        searchViewModel.startVisualSearch(mediaId)
                                        navController.popBackStack()
                                        navigateTopLevel(SearchRoute)
                                    },
                                    onEditPhoto = { id -> navController.navigate(PhotoEditorRoute(id)) },
                                    onOpenVideoPlayer = { id -> navController.navigate(VideoPlayerRoute(id)) },
                                    onOpenMetadataViewer = { id -> navController.navigate(MetadataViewerRoute(id)) },
                                    onMoveToVault = { item ->
                                        viewerViewModel.moveToVault(item) { ok ->
                                            Toast.makeText(
                                                context,
                                                if (ok) "Moved to Private Vault (AES-256)" else "Vault encryption failed",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            if (ok) navController.popBackStack()
                                        }
                                    },
                                    onMoveToTrash = { item ->
                                        viewerViewModel.moveToTrash(item) {
                                            Toast.makeText(context, "Moved to Recently Deleted", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    },
                                    onOpenMedia = { nextId ->
                                        navController.navigate(MediaViewerRoute(nextId))
                                    },
                                )
                            }

                            composable<VideoPlayerRoute> { backStackEntry ->
                                val route: VideoPlayerRoute = backStackEntry.toRoute()
                                val videoItem = allMediaSnapshot.firstOrNull { it.mediaStoreId == route.mediaStoreId }
                                VideoPlayerScreen(
                                    media = videoItem,
                                    videoPlaybackDao = appContainer.database.videoPlaybackDao(),
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<PhotoEditorRoute> { backStackEntry ->
                                val route: PhotoEditorRoute = backStackEntry.toRoute()
                                val targetUri = allMediaSnapshot
                                    .firstOrNull { it.mediaStoreId == route.mediaStoreId }
                                    ?.contentUri
                                PhotoEditorScreen(
                                    initialUriString = targetUri,
                                    onBack = { navController.popBackStack() },
                                    onSavedToMediaStore = {
                                        homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                    },
                                )
                            }

                            composable<MetadataViewerRoute> { backStackEntry ->
                                val route: MetadataViewerRoute = backStackEntry.toRoute()
                                val item = allMediaSnapshot.firstOrNull { it.mediaStoreId == route.mediaStoreId }
                                MetadataViewerScreen(
                                    media = item,
                                    onBack = { navController.popBackStack() },
                                    onRefreshMediaStore = {
                                        homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                    },
                                )
                            }

                            composable<PrivateVaultRoute> {
                                PrivateVaultScreen(
                                    uiState = vaultState,
                                    onCreatePin = vaultViewModel::createPin,
                                    onUnlockWithPin = vaultViewModel::unlockWithPin,
                                    onBiometricSuccess = vaultViewModel::unlockViaBiometricSuccess,
                                    onLockNow = vaultViewModel::lockNow,
                                    onSetBiometricEnabled = vaultViewModel::setBiometricEnabled,
                                    onSetAutoLockSeconds = vaultViewModel::setAutoLockSeconds,
                                    onImportPickerUris = vaultViewModel::importPickerUris,
                                    onImportLibraryItems = vaultViewModel::importLibraryItems,
                                    onOpenPreview = vaultViewModel::openDecryptedPreview,
                                    onClosePreview = vaultViewModel::closePreview,
                                    onExportFromVault = { item ->
                                        vaultViewModel.exportFromVault(item) {
                                            homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                        }
                                    },
                                    onDeletePermanently = vaultViewModel::deleteFromVaultPermanently,
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<RecentlyDeletedRoute> {
                                RecentlyDeletedScreen(
                                    trashItems = trashItems,
                                    trashRepository = appContainer.trashRepository,
                                    onRestoredMedia = {
                                        homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                    },
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<CollageStudioRoute> {
                                CollageStudioScreen(
                                    allMedia = allMediaSnapshot,
                                    onBack = { navController.popBackStack() },
                                    onSavedToMediaStore = {
                                        homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                    },
                                )
                            }

                            composable<PosterStudioRoute> {
                                PosterStudioScreen(
                                    allMedia = allMediaSnapshot,
                                    onBack = { navController.popBackStack() },
                                    onSavedToMediaStore = {
                                        homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                    },
                                )
                            }

                            composable<DocumentScannerRoute> {
                                DocumentScannerScreen(
                                    onBack = { navController.popBackStack() },
                                    onSavedImageToMediaStore = {
                                        homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                    },
                                )
                            }

                            composable<StorageManagerRoute> {
                                StorageManagerScreen(
                                    allMedia = allMediaSnapshot,
                                    vaultCryptoManager = appContainer.vaultCryptoManager,
                                    trashRepository = appContainer.trashRepository,
                                    onOpenMedia = { id -> navController.navigate(MediaViewerRoute(id)) },
                                    onMoveToTrash = { item ->
                                        scope.launch {
                                            appContainer.trashRepository.moveItemsToTrash(listOf(item))
                                            Toast.makeText(context, "Moved ${item.displayName} to trash", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<MemoriesRoute> {
                                MemoriesScreen(
                                    allMedia = allMediaSnapshot,
                                    onOpenMedia = { id -> navController.navigate(MediaViewerRoute(id)) },
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<CustomizationStudioRoute> {
                                CustomizationStudioScreen(
                                    preferences = prefs,
                                    onSelectPresetTheme = { id ->
                                        scope.launch { appContainer.preferencesManager.setThemeId(id) }
                                    },
                                    onSetCustomAccentHex = { hex ->
                                        scope.launch { appContainer.preferencesManager.setCustomAccentHex(hex) }
                                    },
                                    onSaveCustomTheme = { profile ->
                                        scope.launch { appContainer.preferencesManager.saveCustomTheme(profile) }
                                    },
                                    onActivateCustomTheme = { id ->
                                        scope.launch { appContainer.preferencesManager.activateCustomTheme(id) }
                                    },
                                    onDeleteCustomTheme = { id ->
                                        scope.launch { appContainer.preferencesManager.deleteCustomTheme(id) }
                                    },
                                    onSelectGridColumns = { cols ->
                                        scope.launch { appContainer.preferencesManager.setGridColumns(cols) }
                                    },
                                    onSetCornerRadiusDp = { dpVal ->
                                        scope.launch { appContainer.preferencesManager.setCornerRadiusDp(dpVal) }
                                    },
                                    onSetGridSpacingDp = { dpVal ->
                                        scope.launch { appContainer.preferencesManager.setGridSpacingDp(dpVal) }
                                    },
                                    onSetReducedMotion = { reduced ->
                                        scope.launch { appContainer.preferencesManager.setReducedMotion(reduced) }
                                    },
                                    onSetNavigationStyle = { style ->
                                        scope.launch { appContainer.preferencesManager.setNavigationStyle(style) }
                                    },
                                    onToggleAutoIndex = { enabled ->
                                        scope.launch { appContainer.preferencesManager.setAutoIndexOnLaunch(enabled) }
                                    },
                                    onToggleSemanticDefault = { enabled ->
                                        scope.launch { appContainer.preferencesManager.setSemanticSearchEnabled(enabled) }
                                        searchViewModel.updateQuery { it.copy(useSemanticExpansion = enabled) }
                                    },
                                    onResetDefaults = {
                                        scope.launch {
                                            appContainer.preferencesManager.setThemeId(GalleryThemePreset.OBSIDIAN_TITANIUM.id)
                                            appContainer.preferencesManager.setCustomAccentHex("")
                                            appContainer.preferencesManager.setGridColumns(3)
                                            appContainer.preferencesManager.setCornerRadiusDp(14)
                                            appContainer.preferencesManager.setGridSpacingDp(8)
                                            appContainer.preferencesManager.setReducedMotion(false)
                                            appContainer.preferencesManager.setNavigationStyle(NavigationStyle.STANDARD_BAR)
                                        }
                                    },
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<BackupRestoreRoute> {
                                BackupRestoreScreen(
                                    allMedia = allMediaSnapshot,
                                    backupRestoreManager = appContainer.backupRestoreManager,
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<DeveloperInfoRoute> {
                                DeveloperInfoScreen(
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
