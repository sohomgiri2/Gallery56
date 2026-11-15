package com.example.feature.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.SavedSearchEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MediaThumbnailTile
import com.example.core.designsystem.MetricPill
import com.example.feature.search.engine.SearchQuery
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusCyan
import com.example.ui.theme.StatusEmerald
import kotlin.math.roundToInt

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    gridColumns: Int,
    onUpdateQuery: ((SearchQuery) -> SearchQuery) -> Unit,
    onResetFilters: () -> Unit,
    onSaveSearch: (String) -> Unit,
    onApplySavedSearch: (SavedSearchEntity) -> Unit,
    onTogglePinSavedSearch: (SavedSearchEntity) -> Unit,
    onDeleteSavedSearch: (Long) -> Unit,
    onOpenMedia: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGallery56Tokens.current
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }

    val activeFilterCount = uiState.query.activeFilterCount()

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns.coerceIn(2, 4)),
        modifier = modifier
            .fillMaxSize()
            .testTag("search2_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "ADVANCED SEARCH 2.0",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = "Indexed Offline FTS4 • Semantic Concept Graph • Visual & EXIF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            saveName = uiState.query.buildHumanSummary().take(28)
                            showSaveDialog = true
                        },
                        modifier = Modifier.testTag("save_search_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkAdd,
                            contentDescription = "Save current search",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Main Search Bar
                OutlinedTextField(
                    value = uiState.query.textQuery,
                    onValueChange = { text -> onUpdateQuery { it.copy(textQuery = text) } },
                    placeholder = {
                        Text("Search file, OCR text, AI label, camera, tag...")
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.query.textQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onUpdateQuery { it.copy(textQuery = "") } },
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                            IconButton(
                                onClick = { showFilterSheet = true },
                                modifier = Modifier.testTag("open_filters_button"),
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (activeFilterCount > 0) {
                                            Badge { Text("$activeFilterCount") }
                                        }
                                    },
                                ) {
                                    Icon(Icons.Rounded.Tune, contentDescription = "Open Search 2.0 filters")
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_field"),
                )

                // Quick Interactive Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.query.useSemanticExpansion,
                        onClick = {
                            onUpdateQuery { it.copy(useSemanticExpansion = !it.useSemanticExpansion) }
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Semantic") },
                        modifier = Modifier.testTag("semantic_toggle_chip"),
                    )
                    FilterChip(
                        selected = uiState.query.hasOcrTextOnly,
                        onClick = { onUpdateQuery { it.copy(hasOcrTextOnly = !it.hasOcrTextOnly) } },
                        label = { Text("OCR Text") },
                    )
                    FilterChip(
                        selected = uiState.query.documentsOnly,
                        onClick = { onUpdateQuery { it.copy(documentsOnly = !it.documentsOnly) } },
                        label = { Text("Documents") },
                    )
                    FilterChip(
                        selected = uiState.query.screenshotsOnly,
                        onClick = { onUpdateQuery { it.copy(screenshotsOnly = !it.screenshotsOnly) } },
                        label = { Text("Screenshots") },
                    )
                    FilterChip(
                        selected = uiState.query.hasGpsOnly,
                        onClick = { onUpdateQuery { it.copy(hasGpsOnly = !it.hasGpsOnly) } },
                        label = { Text("GPS") },
                    )
                    FilterChip(
                        selected = uiState.query.favoritesOnly,
                        onClick = { onUpdateQuery { it.copy(favoritesOnly = !it.favoritesOnly) } },
                        label = { Text("Favorites") },
                    )
                    FilterChip(
                        selected = uiState.query.minRating >= 4,
                        onClick = {
                            onUpdateQuery { q -> q.copy(minRating = if (q.minRating >= 4) 0 else 4) }
                        },
                        label = { Text("4★+") },
                    )
                }

                // Visual Similarity Anchor Card (when visual search active)
                if (uiState.similarityAnchor != null) {
                    GlassPanel(
                        borderColor = StatusCyan.copy(alpha = 0.6f),
                        contentPadding = 12.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(uiState.similarityAnchor.contentUri)
                                    .size(160)
                                    .build(),
                                contentDescription = "Visual search anchor",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ImageSearch,
                                        contentDescription = null,
                                        tint = StatusCyan,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text("Visual Search Active", style = MaterialTheme.typography.titleSmall)
                                }
                                Text(
                                    text = "Min similarity: ${(uiState.query.similarityThreshold * 100).roundToInt()}% (dHash + Color)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Slider(
                                    value = uiState.query.similarityThreshold,
                                    onValueChange = { th -> onUpdateQuery { it.copy(similarityThreshold = th) } },
                                    valueRange = 0.55f..0.95f,
                                )
                            }
                            IconButton(
                                onClick = { onUpdateQuery { it.copy(similarToMediaId = null) } },
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear visual search")
                            }
                        }
                    }
                }

                // Saved Searches Strip
                if (uiState.savedSearches.isNotEmpty()) {
                    Text(
                        text = "Saved Searches",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.savedSearches, key = { it.id }) { saved ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tokens.glassSurface)
                                    .border(1.dp, tokens.glassBorder, RoundedCornerShape(12.dp))
                                    .clickable { onApplySavedSearch(saved) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("saved_search_${saved.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PushPin,
                                    contentDescription = "Toggle pin",
                                    tint = if (saved.isPinned) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable { onTogglePinSavedSearch(saved) },
                                )
                                Column {
                                    Text(
                                        text = saved.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = saved.summary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete saved search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onDeleteSavedSearch(saved.id) },
                                )
                            }
                        }
                    }
                }

                // Expanded Semantic Concepts + Latency Telemetry
                if (uiState.expandedTerms.isNotEmpty()) {
                    Text(
                        text = "Semantic concepts: ${uiState.expandedTerms.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetricPill(
                        label = "${uiState.results.size} matches • ${uiState.latencyMillis} ms • ${uiState.totalIndexedCount} indexed",
                        icon = Icons.Rounded.FilterList,
                        accentColor = StatusEmerald,
                    )
                    if (activeFilterCount > 0 || uiState.query.textQuery.isNotBlank()) {
                        TextButton(onClick = onResetFilters) {
                            Text("Clear All")
                        }
                    }
                }
            }
        }

        if (uiState.results.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = if (uiState.totalIndexedCount == 0) {
                                "No media indexed yet"
                            } else {
                                "No media matches these Search 2.0 criteria"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (uiState.totalIndexedCount == 0) {
                                "Sync MediaStore or pick photos on the Gallery tab, then run Offline AI Indexing."
                            } else {
                                "Try broadening filters, enabling Semantic Expansion, or resetting active constraints."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(uiState.results, key = { it.media.mediaStoreId }) { scored ->
                MediaThumbnailTile(
                    media = scored.media,
                    badgeText = scored.matchReasons.firstOrNull(),
                    onClick = { onOpenMedia(scored.media.mediaStoreId) },
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showFilterSheet) {
        SearchFilterSheet(
            query = uiState.query,
            availableAlbums = uiState.availableAlbums,
            availableFolders = uiState.availableFolders,
            availableFormats = uiState.availableFormats,
            availableFaceGroups = uiState.availableFaceGroups,
            onUpdateQuery = onUpdateQuery,
            onResetFilters = onResetFilters,
            onDismiss = { showFilterSheet = false },
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Search 2.0 Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = uiState.query.buildHumanSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_search_name_input"),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveSearch(saveName)
                        showSaveDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_search_btn"),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
