package com.example.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.MediaIndexEntity
import com.example.core.datastore.GalleryLayoutId
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MediaThumbnailTile
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.formatBytesReadable
import com.example.ui.theme.LocalGallery56Tokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DateGroupSection(
    val dateLabel: String,
    val items: List<MediaIndexEntity>,
)

@Composable
fun GalleryLayoutRenderer(
    layoutId: GalleryLayoutId,
    uiState: GalleryHomeUiState,
    headerContent: @Composable () -> Unit,
    emptyContent: @Composable () -> Unit,
    onSelectAlbum: (String?) -> Unit,
    onSelectBucket: (GalleryQuickBucket) -> Unit,
    onToggleSelectItem: (Long) -> Unit,
    onOpenMedia: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGallery56Tokens.current
    val items = uiState.items
    val cols = uiState.preferences.gridColumns.coerceIn(2, 6)
    val spacing = tokens.gridSpacing

    val dateSections = remember(items) {
        val fmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        items.groupBy { entity ->
            if (entity.dateTakenMillis > 0L) fmt.format(Date(entity.dateTakenMillis)) else "Recent Imports"
        }.map { (label, group) -> DateGroupSection(label, group) }
    }

    when (layoutId) {
        GalleryLayoutId.MASONRY_FLOW -> {
            val masonryCols = cols.coerceIn(2, 4)
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(masonryCols),
                modifier = modifier
                    .fillMaxSize()
                    .testTag("layout_masonry_flow"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalItemSpacing = spacing,
            ) {
                item(span = StaggeredGridItemSpan.FullLine) { headerContent() }
                if (items.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) { emptyContent() }
                } else {
                    items(items, key = { it.mediaStoreId }) { item ->
                        val ratio = if (item.width > 0 && item.height > 0) {
                            (item.width.toFloat() / item.height.toFloat()).coerceIn(0.65f, 1.55f)
                        } else {
                            if (item.mediaStoreId % 2L == 0L) 0.82f else 1.2f
                        }
                        SelectableTileWrapper(
                            item = item,
                            isSelected = item.mediaStoreId in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            aspectRatio = ratio,
                            badgeText = if (item.format.isNotBlank()) item.format.uppercase(Locale.US) else null,
                            onToggleSelect = { onToggleSelectItem(item.mediaStoreId) },
                            onOpen = { onOpenMedia(item.mediaStoreId) },
                        )
                    }
                }
                item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(16.dp)) }
            }
        }

        GalleryLayoutId.CINEMATIC_GALLERY -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("layout_cinematic_gallery"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(spacing + 6.dp),
            ) {
                item { headerContent() }
                if (items.isEmpty()) {
                    item { emptyContent() }
                } else {
                    items(items, key = { it.mediaStoreId }) { item ->
                        CinematicMediaCard(
                            item = item,
                            isSelected = item.mediaStoreId in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            onToggleSelect = { onToggleSelectItem(item.mediaStoreId) },
                            onOpen = { onOpenMedia(item.mediaStoreId) },
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        GalleryLayoutId.TIMELINE_GALLERY -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("layout_timeline_gallery"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { headerContent() }
                if (items.isEmpty()) {
                    item { emptyContent() }
                } else {
                    items(dateSections, key = { it.dateLabel }) { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                                Text(
                                    text = section.dateLabel,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = tokens.semantic.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                MetricPill(label = "${section.items.size}")
                            }
                            ChunkedGridRows(
                                items = section.items,
                                columns = cols.coerceIn(2, 5),
                                spacing = spacing,
                                selectedIds = uiState.selectedIds,
                                isSelectionMode = uiState.isSelectionMode,
                                onToggleSelect = onToggleSelectItem,
                                onOpen = onOpenMedia,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        GalleryLayoutId.MAGAZINE_GALLERY -> {
            val paired = remember(items) { items.chunked(3) }
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .testTag("layout_magazine_gallery"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { headerContent() }
                if (items.isEmpty()) {
                    item { emptyContent() }
                } else {
                    itemsIndexed(paired) { _, trio ->
                        val lead = trio.first()
                        CinematicMediaCard(
                            item = lead,
                            isSelected = lead.mediaStoreId in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            onToggleSelect = { onToggleSelectItem(lead.mediaStoreId) },
                            onOpen = { onOpenMedia(lead.mediaStoreId) },
                            aspectRatio = 1.48f,
                        )
                        val followers = trio.drop(1)
                        if (followers.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing),
                            ) {
                                followers.forEach { sub ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        SelectableTileWrapper(
                                            item = sub,
                                            isSelected = sub.mediaStoreId in uiState.selectedIds,
                                            isSelectionMode = uiState.isSelectionMode,
                                            aspectRatio = 1.05f,
                                            onToggleSelect = { onToggleSelectItem(sub.mediaStoreId) },
                                            onOpen = { onOpenMedia(sub.mediaStoreId) },
                                        )
                                    }
                                }
                                if (followers.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        GalleryLayoutId.FOLDER_FIRST -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols.coerceIn(2, 5)),
                modifier = modifier
                    .fillMaxSize()
                    .testTag("layout_folder_first"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { headerContent() }
                if (uiState.albums.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            uiState.albums.take(2).forEach { album ->
                                GlassPanel(
                                    onClick = { onSelectAlbum(album.name) },
                                    contentPadding = 12.dp,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = tokens.semantic.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${album.count} items",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.semantic.textSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
                if (items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { emptyContent() }
                } else {
                    items(items, key = { it.mediaStoreId }) { item ->
                        SelectableTileWrapper(
                            item = item,
                            isSelected = item.mediaStoreId in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            onToggleSelect = { onToggleSelectItem(item.mediaStoreId) },
                            onOpen = { onOpenMedia(item.mediaStoreId) },
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }
            }
        }

        GalleryLayoutId.SMART_AI_GALLERY -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols.coerceIn(2, 5)),
                modifier = modifier
                    .fillMaxSize()
                    .testTag("layout_smart_ai"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { headerContent() }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val bestCount = items.count { it.isBestShot || it.qualityScore >= 0.72f }
                    val docsCount = items.count { it.isDocument || it.ocrText.isNotBlank() }
                    val geoCount = items.count { it.hasGps }
                    GlassPanel(contentPadding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Smart AI Clusters",
                                style = MaterialTheme.typography.titleSmall,
                                color = tokens.semantic.textPrimary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricPill(
                                label = "Best Shots ($bestCount)",
                                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onSelectBucket(GalleryQuickBucket.ALL) },
                            )
                            MetricPill(
                                label = "Docs ($docsCount)",
                                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onSelectBucket(GalleryQuickBucket.DOCUMENTS) },
                            )
                            MetricPill(
                                label = "Geo ($geoCount)",
                                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onSelectBucket(GalleryQuickBucket.GEO) },
                            )
                        }
                    }
                }
                if (items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { emptyContent() }
                } else {
                    items(items, key = { it.mediaStoreId }) { item ->
                        val topAi = item.aiLabelsText.split(',').firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
                        SelectableTileWrapper(
                            item = item,
                            isSelected = item.mediaStoreId in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            badgeText = topAi,
                            onToggleSelect = { onToggleSelectItem(item.mediaStoreId) },
                            onOpen = { onOpenMedia(item.mediaStoreId) },
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }
            }
        }

        else -> {
            // CLASSIC_GRID, IOS_INSPIRED, NOTHING_MINIMAL, LIQUID_GLASS, COMPACT_PRO,
            // SPATIAL_GALLERY, AMOLED_PURE, MATERIAL_ADAPTIVE
            val effectiveCols = when (layoutId) {
                GalleryLayoutId.COMPACT_PRO -> cols.coerceAtLeast(4).coerceAtMost(6)
                GalleryLayoutId.SPATIAL_GALLERY -> cols.coerceAtMost(2)
                else -> cols.coerceIn(2, 6)
            }
            val effectiveSpacing = when (layoutId) {
                GalleryLayoutId.COMPACT_PRO -> 4.dp
                GalleryLayoutId.SPATIAL_GALLERY -> 14.dp
                GalleryLayoutId.IOS_INSPIRED -> spacing + 2.dp
                else -> spacing
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(effectiveCols),
                modifier = modifier
                    .fillMaxSize()
                    .testTag("layout_${layoutId.id}"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(effectiveSpacing),
                verticalArrangement = Arrangement.spacedBy(effectiveSpacing),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { headerContent() }

                if (items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { emptyContent() }
                } else if (uiState.preferences.showDateGrouping &&
                    (layoutId == GalleryLayoutId.CLASSIC_GRID ||
                        layoutId == GalleryLayoutId.IOS_INSPIRED ||
                        layoutId == GalleryLayoutId.NOTHING_MINIMAL ||
                        layoutId == GalleryLayoutId.MATERIAL_ADAPTIVE)
                ) {
                    dateSections.forEach { section ->
                        item(
                            key = "hdr_${section.dateLabel}",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Text(
                                text = if (layoutId == GalleryLayoutId.NOTHING_MINIMAL) {
                                    section.dateLabel.uppercase(Locale.US)
                                } else {
                                    section.dateLabel
                                },
                                style = if (layoutId == GalleryLayoutId.NOTHING_MINIMAL) {
                                    MaterialTheme.typography.labelLarge
                                } else {
                                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                },
                                color = tokens.semantic.textSecondary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                            )
                        }
                        items(section.items, key = { it.mediaStoreId }) { item ->
                            StyledLayoutTile(
                                layoutId = layoutId,
                                item = item,
                                isSelected = item.mediaStoreId in uiState.selectedIds,
                                isSelectionMode = uiState.isSelectionMode,
                                onToggleSelect = { onToggleSelectItem(item.mediaStoreId) },
                                onOpen = { onOpenMedia(item.mediaStoreId) },
                            )
                        }
                    }
                } else {
                    items(items, key = { it.mediaStoreId }) { item ->
                        StyledLayoutTile(
                            layoutId = layoutId,
                            item = item,
                            isSelected = item.mediaStoreId in uiState.selectedIds,
                            isSelectionMode = uiState.isSelectionMode,
                            onToggleSelect = { onToggleSelectItem(item.mediaStoreId) },
                            onOpen = { onOpenMedia(item.mediaStoreId) },
                        )
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun StyledLayoutTile(
    layoutId: GalleryLayoutId,
    item: MediaIndexEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    val tokens = LocalGallery56Tokens.current
    val badge = when (layoutId) {
        GalleryLayoutId.COMPACT_PRO -> if (item.format.isNotBlank()) item.format.uppercase(Locale.US) else null
        GalleryLayoutId.NOTHING_MINIMAL -> if (item.resolutionLabel != "Unknown") item.resolutionLabel else null
        else -> null
    }

    when (layoutId) {
        GalleryLayoutId.LIQUID_GLASS -> {
            GlassPanel(
                contentPadding = 4.dp,
                shape = RoundedCornerShape((tokens.thumbnailCornerRadius + 4.dp)),
                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else tokens.glassBorder,
            ) {
                SelectableTileWrapper(
                    item = item,
                    isSelected = isSelected,
                    isSelectionMode = isSelectionMode,
                    badgeText = badge,
                    onToggleSelect = onToggleSelect,
                    onOpen = onOpen,
                )
            }
        }
        GalleryLayoutId.SPATIAL_GALLERY -> {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = tokens.elevatedSurface,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SelectableTileWrapper(
                        item = item,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        aspectRatio = 0.92f,
                        onToggleSelect = onToggleSelect,
                        onOpen = onOpen,
                    )
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.semantic.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        else -> {
            SelectableTileWrapper(
                item = item,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                badgeText = badge,
                onToggleSelect = onToggleSelect,
                onOpen = onOpen,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableTileWrapper(
    item: MediaIndexEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    aspectRatio: Float = 1f,
    badgeText: String? = null,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    Box(
        modifier = Modifier.combinedClickable(
            onClick = { if (isSelectionMode) onToggleSelect() else onOpen() },
            onLongClick = onToggleSelect,
        ),
    ) {
        MediaThumbnailTile(
            media = item,
            badgeText = badgeText,
            isSelected = isSelected,
            aspectRatio = aspectRatio,
            onClick = { if (isSelectionMode) onToggleSelect() else onOpen() },
            overlayBottomEnd = if (isSelectionMode) {
                {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CinematicMediaCard(
    item: MediaIndexEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    aspectRatio: Float = 1.72f,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    val tokens = LocalGallery56Tokens.current
    val cardShape = RoundedCornerShape(tokens.thumbnailCornerRadius.coerceAtLeast(12.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(cardShape)
            .background(tokens.elevatedSurface)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else tokens.glassBorder,
                shape = cardShape,
            )
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else onOpen() },
                onLongClick = onToggleSelect,
            ),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(item.contentUri).size(720).crossfade(true).build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))),
                )
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${item.resolutionLabel} • ${formatBytesReadable(item.fileSizeBytes)}" +
                            if (item.cameraModel.isNotBlank()) " • ${item.cameraModel}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChunkedGridRows(
    items: List<MediaIndexEntity>,
    columns: Int,
    spacing: androidx.compose.ui.unit.Dp,
    selectedIds: Set<Long>,
    isSelectionMode: Boolean,
    onToggleSelect: (Long) -> Unit,
    onOpen: (Long) -> Unit,
) {
    val rows = remember(items, columns) { items.chunked(columns) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        SelectableTileWrapper(
                            item = item,
                            isSelected = item.mediaStoreId in selectedIds,
                            isSelectionMode = isSelectionMode,
                            onToggleSelect = { onToggleSelect(item.mediaStoreId) },
                            onOpen = { onOpen(item.mediaStoreId) },
                        )
                    }
                }
                repeat(columns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
