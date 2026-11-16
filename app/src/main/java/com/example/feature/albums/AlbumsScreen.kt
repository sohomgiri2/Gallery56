package com.example.feature.albums

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.CustomAlbumEntity
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MediaThumbnailTile
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.SectionHeader
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusRose
import com.example.ui.theme.parseHexColorOrNull

private val albumBadgeColors = listOf(
    "", "#5CE1E6", "#60A5FA", "#C084FC", "#F472B6", "#34D399", "#FBBF24", "#FB7185",
)

@Composable
fun AlbumsScreen(
    uiState: AlbumsUiState,
    gridColumns: Int,
    albumCardLarge: Boolean = false,
    onSetSortMode: (AlbumSortMode) -> Unit,
    onCreateAlbum: (String, String, String) -> Unit,
    onRenameAlbum: (Long, String) -> Unit,
    onTogglePin: (CustomAlbumEntity) -> Unit,
    onSetAlbumCover: (Long, String?) -> Unit,
    onSetAlbumColorLabel: (Long, String) -> Unit,
    onMoveAlbumOrder: (Long, Int) -> Unit,
    onAddItemsToAlbum: (Long, List<Long>) -> Unit,
    onRemoveItemFromAlbum: (Long, Long) -> Unit,
    onDeleteAlbum: (Long) -> Unit,
    onOpenCustomAlbum: (Long?) -> Unit,
    onOpenPhysicalBucket: (String?) -> Unit,
    onOpenSmartAlbum: (SmartAlbumType?) -> Unit,
    onOpenMedia: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tokens = LocalGallery56Tokens.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newColorHex by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<CustomAlbumEntity?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var addMediaAlbumId by remember { mutableStateOf<Long?>(null) }
    var pickedMediaIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var coverPickerAlbum by remember { mutableStateOf<CustomAlbumWithItems?>(null) }
    var colorPickerAlbum by remember { mutableStateOf<CustomAlbumEntity?>(null) }

    val activeCustom = uiState.customAlbums.firstOrNull { it.album.albumId == uiState.selectedCustomAlbumId }
    val activePhysical = uiState.physicalBuckets.firstOrNull { it.name == uiState.selectedPhysicalBucketName }
    val activeSmart = uiState.smartAlbums.firstOrNull { it.type == uiState.selectedSmartAlbumType }

    if (activeCustom != null || activePhysical != null || activeSmart != null) {
        BackHandler {
            onOpenCustomAlbum(null)
            onOpenPhysicalBucket(null)
            onOpenSmartAlbum(null)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (activeCustom != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns.coerceIn(2, 6)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
                verticalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Gallery56TopBar(
                            title = activeCustom.album.name,
                            subtitle = "${activeCustom.items.size} items • Custom Album",
                            navigationIcon = {
                                IconButton(onClick = { onOpenCustomAlbum(null) }) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back to albums",
                                        tint = tokens.semantic.iconPrimary,
                                    )
                                }
                            },
                            primaryActions = {
                                IconButton(onClick = { onTogglePin(activeCustom.album) }) {
                                    Icon(
                                        Icons.Rounded.PushPin,
                                        contentDescription = "Pin album",
                                        tint = if (activeCustom.album.isPinned) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            tokens.semantic.iconSecondary
                                        },
                                    )
                                }
                                IconButton(onClick = { colorPickerAlbum = activeCustom.album }) {
                                    Icon(
                                        Icons.Rounded.Palette,
                                        contentDescription = "Album color label",
                                        tint = parseHexColorOrNull(activeCustom.album.colorLabelHex)
                                            ?: tokens.semantic.iconPrimary,
                                    )
                                }
                            },
                            overflowContent = { dismiss ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Rename Album") },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                    onClick = {
                                        dismiss()
                                        renameTarget = activeCustom.album
                                        renameValue = activeCustom.album.name
                                    },
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Choose Cover Photo") },
                                    leadingIcon = { Icon(Icons.Rounded.Image, null) },
                                    onClick = {
                                        dismiss()
                                        coverPickerAlbum = activeCustom
                                    },
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Move Up in Order") },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                                    onClick = {
                                        dismiss()
                                        onMoveAlbumOrder(activeCustom.album.albumId, -1)
                                    },
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Move Down in Order") },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) },
                                    onClick = {
                                        dismiss()
                                        onMoveAlbumOrder(activeCustom.album.albumId, +1)
                                    },
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Delete Album", color = StatusRose) },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = StatusRose) },
                                    onClick = {
                                        dismiss()
                                        onDeleteAlbum(activeCustom.album.albumId)
                                    },
                                )
                            },
                        )

                        Button(
                            onClick = {
                                pickedMediaIds = activeCustom.items.map { it.mediaStoreId }.toSet()
                                addMediaAlbumId = activeCustom.album.albumId
                            },
                            modifier = Modifier.testTag("album_add_media_btn"),
                        ) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add / Manage Photos", maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (activeCustom.items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "This album has no photos yet. Tap 'Add / Manage Photos' above to organize items without duplicating files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.semantic.textSecondary,
                            )
                        }
                    }
                } else {
                    items(activeCustom.items, key = { it.mediaStoreId }) { item ->
                        MediaThumbnailTile(
                            media = item,
                            onClick = { onOpenMedia(item.mediaStoreId) },
                            overlayBottomEnd = {
                                IconButton(
                                    onClick = {
                                        onRemoveItemFromAlbum(activeCustom.album.albumId, item.mediaStoreId)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Remove from album",
                                        tint = StatusRose,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        } else if (activePhysical != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns.coerceIn(2, 6)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
                verticalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Gallery56TopBar(
                        title = activePhysical.name,
                        subtitle = "${activePhysical.items.size} items • ${activePhysical.relativePath}",
                        navigationIcon = {
                            IconButton(onClick = { onOpenPhysicalBucket(null) }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back to folders",
                                    tint = tokens.semantic.iconPrimary,
                                )
                            }
                        },
                    )
                }
                items(activePhysical.items, key = { it.mediaStoreId }) { item ->
                    MediaThumbnailTile(
                        media = item,
                        onClick = { onOpenMedia(item.mediaStoreId) },
                    )
                }
            }
        } else if (activeSmart != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns.coerceIn(2, 6)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                horizontalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
                verticalArrangement = Arrangement.spacedBy(tokens.gridSpacing),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Gallery56TopBar(
                        title = activeSmart.type.label,
                        subtitle = "${activeSmart.items.size} smart-filtered items",
                        navigationIcon = {
                            IconButton(onClick = { onOpenSmartAlbum(null) }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back to albums",
                                    tint = tokens.semantic.iconPrimary,
                                )
                            }
                        },
                    )
                }
                if (activeSmart.items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No items currently match ${activeSmart.type.label}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.semantic.textSecondary,
                            )
                        }
                    }
                } else {
                    items(activeSmart.items, key = { it.mediaStoreId }) { item ->
                        MediaThumbnailTile(
                            media = item,
                            onClick = { onOpenMedia(item.mediaStoreId) },
                        )
                    }
                }
            }
        } else {
            val coverHeight = if (albumCardLarge) 134.dp else 104.dp
            val albumCols = if (albumCardLarge) 1 else 2

            LazyVerticalGrid(
                columns = GridCells.Fixed(albumCols),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("albums_screen"),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Gallery56TopBar(
                        title = "ALBUMS & FOLDERS",
                        subtitle = "Custom collections, Smart Albums & folders",
                    )
                }

                // Smart Albums Bar
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.smartAlbums.forEach { group ->
                            FilterChip(
                                selected = false,
                                onClick = { onOpenSmartAlbum(group.type) },
                                label = {
                                    Text(
                                        text = "${group.type.label} (${group.items.size})",
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                },
                                modifier = Modifier.testTag("smart_album_${group.type.id}"),
                            )
                        }
                    }
                }

                // Album Sort Controls
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Sort:",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.semantic.textSecondary,
                        )
                        AlbumSortMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.sortMode == mode,
                                onClick = { onSetSortMode(mode) },
                                label = { Text(mode.label, maxLines = 1, softWrap = false) },
                            )
                        }
                    }
                }

                if (uiState.customAlbums.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(
                            title = "Custom Albums",
                            subtitle = "${uiState.customAlbums.size} persisted in Room",
                        )
                    }
                    items(uiState.customAlbums, key = { "c_${it.album.albumId}" }) { entry ->
                        val cover = entry.album.coverUri ?: entry.items.firstOrNull()?.contentUri
                        val labelColor = parseHexColorOrNull(entry.album.colorLabelHex)
                        GlassPanel(
                            onClick = { onOpenCustomAlbum(entry.album.albumId) },
                            contentPadding = 10.dp,
                            borderColor = labelColor?.copy(alpha = 0.7f) ?: tokens.semantic.cardBorder,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(coverHeight)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tokens.elevatedSurface),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (cover != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(cover).size(320).build(),
                                        contentDescription = entry.album.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.PhotoAlbum,
                                        null,
                                        tint = labelColor ?: MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp),
                                    )
                                }
                                if (labelColor != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(labelColor)
                                            .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                                    )
                                }
                                if (entry.album.isPinned) {
                                    Icon(
                                        Icons.Rounded.PushPin,
                                        contentDescription = "Pinned",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(16.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = entry.album.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = tokens.semantic.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${entry.items.size} items",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.semantic.textSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = "Device MediaStore Folders",
                        subtitle = "${uiState.physicalBuckets.size} physical directories",
                    )
                }

                if (uiState.physicalBuckets.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No device folders discovered yet. Grant MediaStore permission or import photos via Photo Picker on the Gallery tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.semantic.textSecondary,
                            )
                        }
                    }
                } else {
                    items(uiState.physicalBuckets, key = { "p_${it.name}" }) { bucket ->
                        val cover = bucket.items.firstOrNull()?.contentUri
                        GlassPanel(
                            onClick = { onOpenPhysicalBucket(bucket.name) },
                            contentPadding = 10.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(coverHeight)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tokens.elevatedSurface),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (cover != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(cover).size(320).build(),
                                        contentDescription = bucket.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = bucket.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = tokens.semantic.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                            MetricPill(label = "${bucket.items.size} items")
                        }
                    }
                }
            }

            // Compact Floating Action Button replacing wrapped header "New Album" button
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("create_album_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "New Album",
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Custom Album", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Album Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_album_name_input"),
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Color Label", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        albumBadgeColors.forEach { hex ->
                            val swatch = parseHexColorOrNull(hex) ?: MaterialTheme.colorScheme.surfaceVariant
                            val selected = newColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape,
                                    )
                                    .clickable { newColorHex = hex },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        onCreateAlbum(newName, newDesc, newColorHex)
                        newName = ""
                        newDesc = ""
                        newColorHex = ""
                        showCreateDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_album_btn"),
                ) { Text("Create", maxLines = 1, softWrap = false) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", maxLines = 1, softWrap = false) }
            },
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Album") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = renameValue.isNotBlank(),
                    onClick = {
                        onRenameAlbum(target.albumId, renameValue)
                        renameTarget = null
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    colorPickerAlbum?.let { target ->
        AlertDialog(
            onDismissRequest = { colorPickerAlbum = null },
            title = { Text("Album Color Label") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    albumBadgeColors.forEach { hex ->
                        val swatch = parseHexColorOrNull(hex) ?: MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (target.colorLabelHex == hex) 3.dp else 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                )
                                .clickable {
                                    onSetAlbumColorLabel(target.albumId, hex)
                                    colorPickerAlbum = null
                                },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { colorPickerAlbum = null }) { Text("Close") }
            },
        )
    }

    coverPickerAlbum?.let { target ->
        AlertDialog(
            onDismissRequest = { coverPickerAlbum = null },
            title = { Text("Choose Album Cover") },
            text = {
                val choices = target.items.ifEmpty { uiState.allMedia.take(24) }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(choices, key = { it.mediaStoreId }) { item ->
                        MediaThumbnailTile(
                            media = item,
                            isSelected = target.album.coverUri == item.contentUri,
                            onClick = {
                                onSetAlbumCover(target.album.albumId, item.contentUri)
                                coverPickerAlbum = null
                            },
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onSetAlbumCover(target.album.albumId, null)
                    coverPickerAlbum = null
                }) { Text("Auto Cover") }
            },
            confirmButton = {
                TextButton(onClick = { coverPickerAlbum = null }) { Text("Close") }
            },
        )
    }

    addMediaAlbumId?.let { targetAlbumId ->
        AlertDialog(
            onDismissRequest = { addMediaAlbumId = null },
            title = { Text("Select Media for Album") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(uiState.allMedia, key = { it.mediaStoreId }) { item ->
                        val checked = item.mediaStoreId in pickedMediaIds
                        Box(
                            modifier = Modifier
                                .clickable {
                                    pickedMediaIds = if (checked) {
                                        pickedMediaIds - item.mediaStoreId
                                    } else {
                                        pickedMediaIds + item.mediaStoreId
                                    }
                                },
                        ) {
                            MediaThumbnailTile(
                                media = item,
                                isSelected = checked,
                                onClick = {
                                    pickedMediaIds = if (checked) {
                                        pickedMediaIds - item.mediaStoreId
                                    } else {
                                        pickedMediaIds + item.mediaStoreId
                                    }
                                },
                            )
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                                modifier = Modifier.align(Alignment.TopStart),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddItemsToAlbum(targetAlbumId, pickedMediaIds.toList())
                        addMediaAlbumId = null
                    },
                ) { Text("Save (${pickedMediaIds.size})") }
            },
            dismissButton = {
                OutlinedButton(onClick = { addMediaAlbumId = null }) { Text("Cancel") }
            },
        )
    }
}
