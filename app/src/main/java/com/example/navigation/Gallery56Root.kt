package com.example.navigation

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.datastore.GalleryLayoutId
import com.example.core.datastore.NavigationStyle
import com.example.core.di.AppContainer
import com.example.feature.albums.AlbumsScreen
import com.example.feature.albums.AlbumsViewModel
import com.example.feature.backup.BackupRestoreScreen
import com.example.feature.collage.CollageStudioScreen
import com.example.feature.developer.DeveloperInfoScreen
import com.example.feature.editor.PhotoEditorScreen
import com.example.feature.forensics.IntegrityCheckerScreen
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
import com.example.feature.smart.SmartCollectionsScreen
import com.example.feature.storage.StorageManagerScreen
import com.example.feature.studio.CustomizationStudioScreen
import com.example.feature.studio.GalleryLayoutStudioScreen
import com.example.feature.studio.TextArtStudioScreen
import com.example.feature.studio.ToolsHubScreen
import com.example.feature.studio.WallpaperStudioScreen
import com.example.feature.trash.RecentlyDeletedScreen
import com.example.feature.vault.PrivateVaultScreen
import com.example.feature.vault.PrivateVaultViewModel
import com.example.feature.videos.VideoPlayerScreen
import com.example.feature.videos.VideoStudioScreen
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
@Serializable data object TextArtStudioRoute
@Serializable data object WallpaperStudioRoute
@Serializable data object DocumentScannerRoute
@Serializable data object VideoStudioRoute
@Serializable data object SmartCollectionsRoute
@Serializable data object IntegrityCheckerRoute
@Serializable data object StorageManagerRoute
@Serializable data object CustomizationStudioRoute
@Serializable data object GalleryLayoutStudioRoute
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
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current

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
    val effectiveAmoled = prefs.amoledMode || prefs.galleryLayoutId == GalleryLayoutId.AMOLED_PURE.id
    val isDarkResolved = if (effectiveAmoled) true else (activeCustomProfile?.isDark ?: themePreset.isDark)

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDarkResolved
                controller.isAppearanceLightNavigationBars = !isDarkResolved
            }
        }
    }

    Gallery56Theme(
        preset = themePreset,
        customProfile = activeCustomProfile,
        customAccentHexOverride = prefs.customAccentHexOverride,
        secondaryHexOverride = prefs.secondaryHexOverride,
        backgroundHexOverride = prefs.backgroundHexOverride,
        cardHexOverride = prefs.cardHexOverride,
        borderHexOverride = prefs.borderHexOverride,
        textHexOverride = prefs.textHexOverride,
        amoledMode = effectiveAmoled,
        cornerRadiusDp = prefs.cornerRadiusDp,
        gridSpacingDp = prefs.gridSpacingDp,
        cardElevationDp = prefs.cardElevationDp,
        blurIntensity = prefs.blurIntensity,
        fontScale = prefs.fontScale,
        typographyStyle = prefs.typographyStyle,
        thumbnailShapeStyle = prefs.thumbnailShapeStyle,
        iconStyle = prefs.iconStyle,
        reducedMotion = prefs.reducedMotion,
    ) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDest = navBackStackEntry?.destination
        val tokens = LocalGallery56Tokens.current

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
            if (prefs.hapticFeedbackEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        val ambientBackground = if (effectiveAmoled) {
            Brush.verticalGradient(listOf(Color.Black, Color.Black))
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    MaterialTheme.colorScheme.background,
                ),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientBackground),
        ) {
            val useNavigationRail = maxWidth >= 680.dp && isTopLevelRoute

            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = if (isEdgeToEdgeViewer) WindowInsets(0) else WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useNavigationRail && isTopLevelRoute) {
                        when (prefs.navigationStyle) {
                            NavigationStyle.FLOATING_ROUNDED, NavigationStyle.GLASS -> {
                                val isFloating = prefs.navigationStyle == NavigationStyle.FLOATING_ROUNDED
                                val barShape = RoundedCornerShape(if (isFloating) 30.dp else 20.dp)
                                val bgAlpha = if (prefs.navigationStyle == NavigationStyle.GLASS) 0.78f else 0.95f
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(
                                            horizontal = if (isFloating) 16.dp else 10.dp,
                                            vertical = if (isFloating) 10.dp else 6.dp,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        shape = barShape,
                                        color = tokens.elevatedSurface.copy(alpha = bgAlpha),
                                        contentColor = tokens.semantic.textPrimary,
                                        tonalElevation = if (isFloating) 8.dp else 4.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, tokens.glassBorder, barShape),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            destinations.forEach { dest ->
                                                val selected = currentDest?.hasRoute(dest.route::class) == true
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(
                                                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                            else Color.Transparent,
                                                        )
                                                        .clickable { navigateTopLevel(dest.route) }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                        .testTag(dest.testTag),
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = dest.label,
                                                        tint = if (selected) MaterialTheme.colorScheme.primary else tokens.semantic.iconSecondary,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                    if (prefs.showNavigationLabels) {
                                                        Spacer(Modifier.height(2.dp))
                                                        Text(
                                                            text = dest.label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (selected) MaterialTheme.colorScheme.primary else tokens.semantic.textSecondary,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            NavigationStyle.COMPACT_ICON_ONLY -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    contentColor = tokens.semantic.textPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding(),
                                ) {
                                    Column {
                                        HorizontalDivider(color = tokens.subtleDivider, thickness = 0.75.dp)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                        ) {
                                            destinations.forEach { dest ->
                                                val selected = currentDest?.hasRoute(dest.route::class) == true
                                                IconButton(
                                                    onClick = { navigateTopLevel(dest.route) },
                                                    modifier = Modifier.testTag(dest.testTag),
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = dest.label,
                                                        tint = if (selected) MaterialTheme.colorScheme.primary else tokens.semantic.iconSecondary,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            NavigationStyle.MINIMAL_FLAT -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    contentColor = tokens.semantic.textPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding(),
                                ) {
                                    Column {
                                        HorizontalDivider(color = tokens.subtleDivider, thickness = 1.dp)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                        ) {
                                            destinations.forEach { dest ->
                                                val selected = currentDest?.hasRoute(dest.route::class) == true
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { navigateTopLevel(dest.route) }
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                        .testTag(dest.testTag),
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                        contentDescription = dest.label,
                                                        tint = if (selected) MaterialTheme.colorScheme.primary else tokens.semantic.iconSecondary,
                                                        modifier = Modifier.size(21.dp),
                                                    )
                                                    if (prefs.showNavigationLabels) {
                                                        Text(
                                                            text = dest.label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (selected) MaterialTheme.colorScheme.primary else tokens.semantic.textSecondary,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            NavigationStyle.CLASSIC_LABELED -> {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                    contentColor = tokens.semantic.textPrimary,
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
                                            label = if (prefs.showNavigationLabels) {
                                                { Text(dest.label, maxLines = 1, softWrap = false) }
                                            } else null,
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = tokens.semantic.iconSecondary,
                                                unselectedTextColor = tokens.semantic.textSecondary,
                                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                            ),
                                            modifier = Modifier.testTag(dest.testTag),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
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
                                    onBatchRenameSelected = homeViewModel::batchRenameSelected,
                                    onAddSelectedToAlbum = homeViewModel::addSelectedToCustomAlbum,
                                    onCopySelectedToFolder = homeViewModel::copySelectedToFolder,
                                    onMoveSelectedToTrash = homeViewModel::moveSelectedToTrash,
                                    onMoveSelectedToVault = { homeViewModel.moveSelectedToVault() },
                                    onDismissStatusNote = homeViewModel::clearStatusNote,
                                    onOpenMedia = { id -> navController.navigate(MediaViewerRoute(id)) },
                                    onOpenSearch = { navigateTopLevel(SearchRoute) },
                                    onOpenAiStudio = { navigateTopLevel(AiStudioRoute) },
                                    onOpenLayoutStudio = { navController.navigate(GalleryLayoutStudioRoute) },
                                )
                            }

                            composable<AlbumsRoute> {
                                AlbumsScreen(
                                    uiState = albumsState,
                                    gridColumns = prefs.gridColumns,
                                    albumCardLarge = prefs.albumCardLarge,
                                    onSetSortMode = albumsViewModel::setSortMode,
                                    onCreateAlbum = albumsViewModel::createAlbum,
                                    onRenameAlbum = albumsViewModel::renameAlbum,
                                    onTogglePin = albumsViewModel::togglePinAlbum,
                                    onSetAlbumCover = albumsViewModel::setAlbumCover,
                                    onSetAlbumColorLabel = albumsViewModel::setAlbumColorLabel,
                                    onMoveAlbumOrder = albumsViewModel::moveAlbumOrder,
                                    onAddItemsToAlbum = albumsViewModel::addItemsToAlbum,
                                    onRemoveItemFromAlbum = albumsViewModel::removeItemFromAlbum,
                                    onDeleteAlbum = albumsViewModel::deleteAlbum,
                                    onOpenCustomAlbum = albumsViewModel::openCustomAlbum,
                                    onOpenPhysicalBucket = albumsViewModel::openPhysicalBucket,
                                    onOpenSmartAlbum = albumsViewModel::openSmartAlbum,
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
                                    pinnedToolIds = prefs.pinnedStudioTools,
                                    recentToolIds = prefs.recentStudioTools,
                                    onTogglePinTool = { id ->
                                        scope.launch { appContainer.preferencesManager.togglePinnedStudioTool(id) }
                                    },
                                    onRecordRecentTool = { id ->
                                        scope.launch { appContainer.preferencesManager.recordRecentStudioTool(id) }
                                    },
                                    onOpenAlbums = { navigateTopLevel(AlbumsRoute) },
                                    onOpenMemories = { navController.navigate(MemoriesRoute) },
                                    onOpenPrivateVault = { navController.navigate(PrivateVaultRoute) },
                                    onOpenPhotoEditor = { navController.navigate(PhotoEditorRoute(-1L)) },
                                    onOpenCollageStudio = { navController.navigate(CollageStudioRoute) },
                                    onOpenPosterStudio = { navController.navigate(PosterStudioRoute) },
                                    onOpenTextArtStudio = { navController.navigate(TextArtStudioRoute) },
                                    onOpenWallpaperStudio = { navController.navigate(WallpaperStudioRoute) },
                                    onOpenDocumentScanner = { navController.navigate(DocumentScannerRoute) },
                                    onOpenVideoStudio = { navController.navigate(VideoStudioRoute) },
                                    onOpenSmartCollections = { navController.navigate(SmartCollectionsRoute) },
                                    onOpenIntegrityChecker = { navController.navigate(IntegrityCheckerRoute) },
                                    onOpenStorageManager = { navController.navigate(StorageManagerRoute) },
                                    onOpenRecentlyDeleted = { navController.navigate(RecentlyDeletedRoute) },
                                    onOpenLayoutStudio = { navController.navigate(GalleryLayoutStudioRoute) },
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
                                    viewerBackgroundMode = prefs.viewerBackgroundMode,
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
                                    customPresets = prefs.customEditorPresets,
                                    onSaveCustomPreset = { preset ->
                                        scope.launch { appContainer.preferencesManager.saveCustomEditorPreset(preset) }
                                    },
                                    onDeleteCustomPreset = { id ->
                                        scope.launch { appContainer.preferencesManager.deleteCustomEditorPreset(id) }
                                    },
                                    onBack = { navController.popBackStack() },
                                    onSavedToMediaStore = {
                                        if (route.mediaStoreId > 0L) {
                                            scope.launch {
                                                appContainer.database.mediaIndexDao()
                                                    .markEdited(route.mediaStoreId, System.currentTimeMillis())
                                            }
                                        }
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

                            composable<TextArtStudioRoute> {
                                TextArtStudioScreen(
                                    onBack = { navController.popBackStack() },
                                    onSavedToMediaStore = {
                                        homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                    },
                                )
                            }

                            composable<WallpaperStudioRoute> {
                                WallpaperStudioScreen(
                                    allMedia = allMediaSnapshot,
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<VideoStudioRoute> {
                                VideoStudioScreen(
                                    allMedia = allMediaSnapshot,
                                    onBack = { navController.popBackStack() },
                                    onSavedToMediaStore = {
                                        homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                    },
                                )
                            }

                            composable<SmartCollectionsRoute> {
                                SmartCollectionsScreen(
                                    allMedia = allMediaSnapshot,
                                    onBatchRename = { items, prefix ->
                                        scope.launch {
                                            items.forEachIndexed { idx, item ->
                                                val ext = item.displayName.substringAfterLast('.', "jpg")
                                                val nextName = "${prefix.trim()}_${idx + 1}.$ext"
                                                appContainer.database.mediaIndexDao()
                                                    .renameDisplayName(item.mediaStoreId, nextName, nextName)
                                            }
                                        }
                                    },
                                    onOpenMedia = { id -> navController.navigate(MediaViewerRoute(id)) },
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<IntegrityCheckerRoute> {
                                IntegrityCheckerScreen(
                                    allMedia = allMediaSnapshot,
                                    onRefreshMediaStore = {
                                        homeViewModel.refreshPermissionsAndSync(triggerAi = false)
                                    },
                                    onOpenMedia = { id -> navController.navigate(MediaViewerRoute(id)) },
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<CustomizationStudioRoute> {
                                CustomizationStudioScreen(
                                    preferences = prefs,
                                    preferencesManager = appContainer.preferencesManager,
                                    onToggleSemanticDefault = { enabled ->
                                        scope.launch { appContainer.preferencesManager.setSemanticSearchEnabled(enabled) }
                                        searchViewModel.updateQuery { it.copy(useSemanticExpansion = enabled) }
                                    },
                                    onOpenLayoutStudio = { navController.navigate(GalleryLayoutStudioRoute) },
                                    onBack = { navController.popBackStack() },
                                )
                            }

                            composable<GalleryLayoutStudioRoute> {
                                GalleryLayoutStudioScreen(
                                    preferences = prefs,
                                    onApplyLayout = { layout ->
                                        scope.launch { appContainer.preferencesManager.setGalleryLayoutId(layout.id) }
                                    },
                                    onSetGridColumns = { cols ->
                                        scope.launch { appContainer.preferencesManager.setGridColumns(cols) }
                                    },
                                    onSetCornerRadiusDp = { dpVal ->
                                        scope.launch { appContainer.preferencesManager.setCornerRadiusDp(dpVal) }
                                    },
                                    onSetGridSpacingDp = { dpVal ->
                                        scope.launch { appContainer.preferencesManager.setGridSpacingDp(dpVal) }
                                    },
                                    onSetThumbnailScale = { scale ->
                                        scope.launch { appContainer.preferencesManager.setThumbnailSizeScale(scale) }
                                    },
                                    onSetThumbnailShape = { shape ->
                                        scope.launch { appContainer.preferencesManager.setThumbnailShapeStyle(shape) }
                                    },
                                    onSetShowDateGrouping = { show ->
                                        scope.launch { appContainer.preferencesManager.setShowDateGrouping(show) }
                                    },
                                    onSetAlbumCardLarge = { large ->
                                        scope.launch { appContainer.preferencesManager.setAlbumCardLarge(large) }
                                    },
                                    onResetLayoutDefaults = {
                                        scope.launch {
                                            appContainer.preferencesManager.setGalleryLayoutId(GalleryLayoutId.CLASSIC_GRID.id)
                                            appContainer.preferencesManager.setGridColumns(3)
                                            appContainer.preferencesManager.setCornerRadiusDp(14)
                                            appContainer.preferencesManager.setGridSpacingDp(8)
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
