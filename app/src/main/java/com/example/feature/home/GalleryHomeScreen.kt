package com.example.feature.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MediaThumbnailTile
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.SectionHeader
import com.example.core.media.MediaAccessLevel
import com.example.core.media.MediaPermissionHelper
import com.example.core.media.MediaStoreOperations
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryHomeScreen(
    uiState: GalleryHomeUiState,
    onRefreshSync: () -> Unit,
    onImportPickerUris: (List<Uri>) -> Unit,
    onSelectBucket: (GalleryQuickBucket) -> Unit,
    onSelectAlbum: (String?) -> Unit,
    onSelectSortOrder: (GallerySortOrder) -> Unit,
    onCycleGridColumns: (Int) -> Unit,
    onToggleSelectionMode: (Boolean?) -> Unit,
    onToggleSelectItem: (Long) -> Unit,
    onSelectAllVisible: () -> Unit,
    onFavoriteSelected: (Boolean) -> Unit,
    onAddSelectedToAlbum: (Long) -> Unit,
    onCopySelectedToFolder: (String) -> Unit,
    onMoveSelectedToTrash: () -> Unit,
    onMoveSelectedToVault: () -> Unit,
    onDismissStatusNote: () -> Unit,
    onOpenMedia: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAiStudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGallery56Tokens.current
    val context = LocalContext.current

    var sortMenuOpen by remember { mutableStateOf(false) }
    var showAlbumPickerDialog by remember { mutableStateOf(false) }
    var showCopyFolderDialog by remember { mutableStateOf(false) }
    var copyFolderName by remember { mutableStateOf("Gallery56_Copies") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        onRefreshSync()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50),
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportPickerUris(uris)
        }
    }

    val systemDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) {
        onRefreshSync()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(uiState.preferences.gridColumns.coerceIn(2, 5)),
        modifier = modifier
            .fillMaxSize()
            .testTag("gallery_home_grid"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
        verticalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (uiState.isSelectionMode) {
                    // Contextual Multi-Select Action Bar
                    GlassPanel(
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        contentPadding = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("multi_select_bar"),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconButton(onClick = { onToggleSelectionMode(false) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Exit multi-select")
                                }
                                Text(
                                    text = "${uiState.selectedIds.size} selected",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Row {
                                IconButton(onClick = onSelectAllVisible) {
                                    Icon(Icons.Rounded.DoneAll, contentDescription = "Select all")
                                }
                                IconButton(
                                    onClick = {
                                        val chosen = uiState.items.filter { it.mediaStoreId in uiState.selectedIds }
                                        MediaStoreOperations.shareMultipleMedia(context, chosen)
                                    },
                                    enabled = uiState.selectedIds.isNotEmpty(),
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share selected")
                                }
                                IconButton(
                                    onClick = { onFavoriteSelected(true) },
                                    enabled = uiState.selectedIds.isNotEmpty(),
                                ) {
                                    Icon(Icons.Rounded.Favorite, contentDescription = "Favorite selected", tint = StatusRose)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = { showAlbumPickerDialog = true },
                                enabled = uiState.selectedIds.isNotEmpty(),
                            ) {
                                Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add to Album")
                            }
                            FilledTonalButton(
                                onClick = { showCopyFolderDialog = true },
                                enabled = uiState.selectedIds.isNotEmpty(),
                            ) {
                                Icon(Icons.Rounded.DriveFileMove, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy to Folder")
                            }
                            FilledTonalButton(
                                onClick = onMoveSelectedToVault,
                                enabled = uiState.selectedIds.isNotEmpty(),
                            ) {
                                Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Vault")
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                enabled = uiState.selectedIds.isNotEmpty(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRose),
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                } else {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GALLERY56",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "BEYOND GALLERY. BEYOND IMAGINATION.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onToggleSelectionMode(true) },
                                modifier = Modifier.testTag("enter_select_mode_btn"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SelectAll,
                                    contentDescription = "Multi-select mode",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { sortMenuOpen = true },
                                    modifier = Modifier.testTag("gallery_sort_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sort,
                                        contentDescription = "Sort gallery",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                DropdownMenu(
                                    expanded = sortMenuOpen,
                                    onDismissRequest = { sortMenuOpen = false },
                                ) {
                                    GallerySortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = { Text(order.label) },
                                            onClick = {
                                                sortMenuOpen = false
                                                onSelectSortOrder(order)
                                            },
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = {
                                    val next = when (uiState.preferences.gridColumns) {
                                        2 -> 3
                                        3 -> 4
                                        4 -> 5
                                        else -> 2
                                    }
                                    onCycleGridColumns(next)
                                },
                                modifier = Modifier.testTag("cycle_grid_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GridView,
                                    contentDescription = "Switch grid columns",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                                    )
                                },
                                modifier = Modifier.testTag("photo_picker_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = "Import via Photo Picker",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(
                                onClick = onRefreshSync,
                                modifier = Modifier.testTag("refresh_media_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Rescan MediaStore",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Quick Search 2.0 & AI Studio Entry Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlassPanel(
                            onClick = onOpenSearch,
                            contentPadding = 12.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_search_shortcut"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Search 2.0 (OCR, AI, EXIF, GPS)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        GlassPanel(
                            onClick = onOpenAiStudio,
                            contentPadding = 12.dp,
                            borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                            modifier = Modifier.testTag("home_ai_shortcut"),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "AI Hub",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                if (uiState.statusNote != null) {
                    GlassPanel(
                        contentPadding = 10.dp,
                        borderColor = StatusEmerald.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = uiState.statusNote,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onDismissStatusNote) {
                                Text("Dismiss")
                            }
                        }
                    }
                }

                if (uiState.isSyncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                if (uiState.accessLevel != MediaAccessLevel.FULL_ACCESS) {
                    PermissionStatusBanner(
                        accessLevel = uiState.accessLevel,
                        onRequestMediaPermission = {
                            permissionLauncher.launch(MediaPermissionHelper.requiredPermissionsForSdk())
                        },
                        onLaunchPhotoPicker = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                            )
                        },
                    )
                }

                // Quick Buckets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GalleryQuickBucket.entries.forEach { bucket ->
                        FilterChip(
                            selected = uiState.selectedBucket == bucket,
                            onClick = { onSelectBucket(bucket) },
                            label = { Text(bucket.label, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.testTag("bucket_chip_${bucket.name.lowercase()}"),
                        )
                    }
                }

                // Albums Row
                if (uiState.albums.isNotEmpty()) {
                    SectionHeader(
                        title = "Folders & Collections",
                        subtitle = "${uiState.albums.size} folders • ${uiState.totalCount} total items",
                        trailing = if (uiState.selectedAlbum != null) {
                            {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onSelectAlbum(null) }
                                        .padding(6.dp),
                                )
                            }
                        } else null,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.albums, key = { it.name }) { album ->
                            val isSelected = uiState.selectedAlbum == album.name
                            Column(
                                modifier = Modifier
                                    .width(104.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else tokens.glassSurface,
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else tokens.glassBorder,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable { onSelectAlbum(album.name) }
                                    .padding(6.dp)
                                    .testTag("album_card_${album.name}"),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(66.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(tokens.elevatedSurface),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (album.coverUri != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(album.coverUri)
                                                .size(220)
                                                .build(),
                                            contentDescription = album.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.FolderOpen,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${album.count} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Active view count row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${uiState.selectedAlbum ?: uiState.selectedBucket.label} • ${uiState.sortOrder.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    MetricPill(
                        label = "${uiState.items.size} items",
                        icon = Icons.Rounded.PhotoLibrary,
                        accentColor = if (uiState.accessLevel == MediaAccessLevel.FULL_ACCESS) {
                            StatusEmerald
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }

        if (uiState.items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = "No device media matched this view",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Grant MediaStore library access or import photos directly using Android's zero-permission Photo Picker.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(MediaPermissionHelper.requiredPermissionsForSdk())
                                },
                                modifier = Modifier.testTag("empty_grant_access_btn"),
                            ) {
                                Text("Grant Access")
                            }
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                                    )
                                },
                                modifier = Modifier.testTag("empty_picker_btn"),
                            ) {
                                Text("Photo Picker")
                            }
                        }
                    }
                }
            }
        } else {
            items(uiState.items, key = { it.mediaStoreId }) { item ->
                val isSelected = item.mediaStoreId in uiState.selectedIds
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (uiState.isSelectionMode) {
                                onToggleSelectItem(item.mediaStoreId)
                            } else {
                                onOpenMedia(item.mediaStoreId)
                            }
                        },
                        onLongClick = { onToggleSelectItem(item.mediaStoreId) },
                    ),
                ) {
                    MediaThumbnailTile(
                        media = item,
                        isSelected = isSelected,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                onToggleSelectItem(item.mediaStoreId)
                            } else {
                                onOpenMedia(item.mediaStoreId)
                            }
                        },
                        overlayBottomEnd = if (uiState.isSelectionMode) {
                            {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        )
                                        .padding(3.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = if (isSelected) "Selected" else "Not selected",
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        } else null,
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Add to Custom Album Dialog
    if (showAlbumPickerDialog) {
        AlertDialog(
            onDismissRequest = { showAlbumPickerDialog = false },
            title = { Text("Add ${uiState.selectedIds.size} item(s) to Album") },
            text = {
                if (uiState.customAlbums.isEmpty()) {
                    Text("No custom albums created yet. Open the Albums tab to create an album first.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.customAlbums.forEach { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tokens.elevatedSurface)
                                    .clickable {
                                        onAddSelectedToAlbum(album.albumId)
                                        showAlbumPickerDialog = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(album.name, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlbumPickerDialog = false }) { Text("Close") }
            },
        )
    }

    // Copy to Folder Dialog
    if (showCopyFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCopyFolderDialog = false },
            title = { Text("Copy Selected Media to Folder") },
            text = {
                OutlinedTextField(
                    value = copyFolderName,
                    onValueChange = { copyFolderName = it },
                    label = { Text("Target Subfolder under Pictures/Movies") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCopySelectedToFolder(copyFolderName)
                        showCopyFolderDialog = false
                    },
                ) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = { showCopyFolderDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Delete / Trash Confirmation Dialog
    if (showDeleteConfirmDialog) {
        val selectedMedia = uiState.items.filter { it.mediaStoreId in uiState.selectedIds }
        val realMediaStoreUris = selectedMedia.filter { it.mediaStoreId > 0L }.map { Uri.parse(it.contentUri) }
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete ${selectedMedia.size} item(s)?") },
            text = {
                Text(
                    "Move to Gallery56 Recently Deleted (app-managed restore cache) or permanently delete from device MediaStore with system confirmation.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onMoveSelectedToTrash()
                    },
                ) { Text("Move to Trash") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (realMediaStoreUris.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                showDeleteConfirmDialog = false
                                val sender = MediaStoreOperations.createMediaStoreDeleteIntentSender(
                                    context,
                                    realMediaStoreUris,
                                )
                                if (sender != null) {
                                    systemDeleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                } else {
                                    onMoveSelectedToTrash()
                                }
                            },
                        ) { Text("System Delete", color = StatusRose) }
                    }
                    TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
                }
            },
        )
    }
}

@Composable
private fun PermissionStatusBanner(
    accessLevel: MediaAccessLevel,
    onRequestMediaPermission: () -> Unit,
    onLaunchPhotoPicker: () -> Unit,
) {
    val isPartial = accessLevel == MediaAccessLevel.PARTIAL_ACCESS
    val accent = if (isPartial) StatusAmber else MaterialTheme.colorScheme.primary

    GlassPanel(
        borderColor = accent.copy(alpha = 0.5f),
        contentPadding = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_status_banner"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (isPartial) Icons.Rounded.Shield else Icons.Rounded.LockOpen,
                contentDescription = null,
                tint = accent,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPartial) {
                        "Partial Media Access Active (Android 14+)"
                    } else {
                        "Connect Device Media Library"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (isPartial) {
                        "Only user-selected items from MediaStore are visible. Manage selection or import additional media anytime."
                    } else {
                        "Allow MediaStore access to discover photos, videos, EXIF & GPS offline, or use the zero-permission Photo Picker."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onRequestMediaPermission,
                modifier = Modifier.testTag("banner_grant_permission_btn"),
            ) {
                Text(if (isPartial) "Manage Selection" else "Allow Library")
            }
            FilledTonalButton(
                onClick = onLaunchPhotoPicker,
                modifier = Modifier.testTag("banner_photo_picker_btn"),
            ) {
                Text("Photo Picker")
            }
        }
    }
}
