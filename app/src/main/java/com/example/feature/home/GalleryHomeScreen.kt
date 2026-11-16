package com.example.feature.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
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
import androidx.compose.material.icons.rounded.ViewQuilt
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
import com.example.core.datastore.GalleryLayoutId
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.SectionHeader
import com.example.core.media.MediaAccessLevel
import com.example.core.media.MediaPermissionHelper
import com.example.core.media.MediaStoreOperations
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose

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
    onBatchRenameSelected: (String) -> Unit,
    onAddSelectedToAlbum: (Long) -> Unit,
    onCopySelectedToFolder: (String) -> Unit,
    onMoveSelectedToTrash: () -> Unit,
    onMoveSelectedToVault: () -> Unit,
    onDismissStatusNote: () -> Unit,
    onOpenMedia: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAiStudio: () -> Unit,
    onOpenLayoutStudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGallery56Tokens.current
    val context = LocalContext.current

    var sortMenuOpen by remember { mutableStateOf(false) }
    var showAlbumPickerDialog by remember { mutableStateOf(false) }
    var showCopyFolderDialog by remember { mutableStateOf(false) }
    var copyFolderName by remember { mutableStateOf("Gallery56_Copies") }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var renamePrefix by remember { mutableStateOf("Gallery56_Photo") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val activeLayout = remember(uiState.preferences.galleryLayoutId) {
        GalleryLayoutId.fromId(uiState.preferences.galleryLayoutId)
    }

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

    GalleryLayoutRenderer(
        layoutId = activeLayout,
        uiState = uiState,
        modifier = modifier.testTag("gallery_home_grid"),
        onSelectAlbum = onSelectAlbum,
        onSelectBucket = onSelectBucket,
        onToggleSelectItem = onToggleSelectItem,
        onOpenMedia = onOpenMedia,
        headerContent = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (uiState.isSelectionMode) {
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
                                    color = tokens.semantic.textPrimary,
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
                                onClick = { showBatchRenameDialog = true },
                                enabled = uiState.selectedIds.isNotEmpty(),
                            ) {
                                Icon(Icons.Rounded.DriveFileRenameOutline, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Batch Rename")
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
                    // Single-line responsive TopAppBar preventing title wrapping
                    Gallery56TopBar(
                        title = "GALLERY56",
                        subtitle = "BEYOND GALLERY. BEYOND IMAGINATION.",
                        primaryActions = {
                            IconButton(
                                onClick = onOpenLayoutStudio,
                                modifier = Modifier.testTag("home_layout_studio_btn"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ViewQuilt,
                                    contentDescription = "Gallery Layout Studio",
                                    tint = MaterialTheme.colorScheme.primary,
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
                        },
                        overflowContent = { dismiss ->
                            DropdownMenuItem(
                                text = { Text("Multi-Select Media") },
                                leadingIcon = { Icon(Icons.Rounded.SelectAll, null) },
                                onClick = {
                                    dismiss()
                                    onToggleSelectionMode(true)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Sort: ${uiState.sortOrder.label}") },
                                leadingIcon = { Icon(Icons.Rounded.Sort, null) },
                                onClick = {
                                    dismiss()
                                    sortMenuOpen = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Grid Columns (${uiState.preferences.gridColumns})") },
                                leadingIcon = { Icon(Icons.Rounded.GridView, null) },
                                onClick = {
                                    dismiss()
                                    val next = when (uiState.preferences.gridColumns) {
                                        2 -> 3
                                        3 -> 4
                                        4 -> 5
                                        5 -> 6
                                        else -> 2
                                    }
                                    onCycleGridColumns(next)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Rescan MediaStore") },
                                leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                                onClick = {
                                    dismiss()
                                    onRefreshSync()
                                },
                            )
                        },
                    )

                    Box {
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

                    // Search 2.0 + AI Hub Entry Bar
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
                                    color = tokens.semantic.textSecondary,
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
                                    color = tokens.semantic.textPrimary,
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
                                color = tokens.semantic.textPrimary,
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

                // Quick Buckets Bar
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
                            label = {
                                Text(
                                    bucket.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            },
                            modifier = Modifier.testTag("bucket_chip_${bucket.name.lowercase()}"),
                        )
                    }
                }

                // Fixed Folder Cards with High-Contrast Semantic Typography
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
                            GlassPanel(
                                onClick = { onSelectAlbum(album.name) },
                                contentPadding = 8.dp,
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    tokens.elevatedSurface
                                },
                                borderColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    tokens.semantic.cardBorder
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .width(114.dp)
                                    .testTag("album_card_${album.name}"),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(68.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(tokens.semantic.background),
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
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = tokens.semantic.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${album.count} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.semantic.textSecondary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                // Active view count & layout pill row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${uiState.selectedAlbum ?: uiState.selectedBucket.label} • ${activeLayout.displayName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.semantic.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
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
        },
        emptyContent = {
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
                        color = tokens.semantic.textPrimary,
                    )
                    Text(
                        text = "Grant MediaStore library access or import photos directly using Android's zero-permission Photo Picker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.semantic.textSecondary,
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
        },
    )

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
                                Text(album.name, style = MaterialTheme.typography.titleSmall, color = tokens.semantic.textPrimary)
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

    if (showBatchRenameDialog) {
        AlertDialog(
            onDismissRequest = { showBatchRenameDialog = false },
            title = { Text("Batch Rename (${uiState.selectedIds.size})") },
            text = {
                OutlinedTextField(
                    value = renamePrefix,
                    onValueChange = { renamePrefix = it },
                    label = { Text("Filename Prefix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = renamePrefix.isNotBlank(),
                    onClick = {
                        onBatchRenameSelected(renamePrefix)
                        showBatchRenameDialog = false
                    },
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchRenameDialog = false }) { Text("Cancel") }
            },
        )
    }

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
    val tokens = LocalGallery56Tokens.current
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
                    color = tokens.semantic.textPrimary,
                )
                Text(
                    text = if (isPartial) {
                        "Only user-selected items from MediaStore are visible. Manage selection or import additional media anytime."
                    } else {
                        "Allow MediaStore access to discover photos, videos, EXIF & GPS offline, or use the zero-permission Photo Picker."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.semantic.textSecondary,
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
