package com.example.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.database.FaceGroupEntity
import com.example.feature.search.engine.MediaTypeFilter
import com.example.feature.search.engine.OrientationFilter
import com.example.feature.search.engine.SearchQuery
import com.example.feature.search.engine.SearchScope
import com.example.feature.search.engine.SearchSortOption
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterSheet(
    query: SearchQuery,
    availableAlbums: List<String>,
    availableFolders: List<String>,
    availableFormats: List<String>,
    availableFaceGroups: List<FaceGroupEntity>,
    onUpdateQuery: ((SearchQuery) -> SearchQuery) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tagInput by remember(query.requiredTags) { mutableStateOf(query.requiredTags.joinToString(", ")) }
    var aiLabelInput by remember(query.requiredAiLabels) { mutableStateOf(query.requiredAiLabels.joinToString(", ")) }
    var latInput by remember(query.gpsCenterLat) { mutableStateOf(query.gpsCenterLat?.toString().orEmpty()) }
    var lonInput by remember(query.gpsCenterLon) { mutableStateOf(query.gpsCenterLon?.toString().orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Search 2.0 Precision Filters",
                    style = MaterialTheme.typography.headlineSmall,
                )
                OutlinedButton(
                    onClick = onResetFilters,
                    modifier = Modifier.testTag("filter_reset_button"),
                ) {
                    Text("Reset")
                }
            }

            // 1. Search Scope & Media Type
            Text("Search Scope", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchScope.entries.forEach { scope ->
                    FilterChip(
                        selected = query.scope == scope,
                        onClick = { onUpdateQuery { it.copy(scope = scope) } },
                        label = { Text(scope.label) },
                    )
                }
            }

            Text("Media Type", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaTypeFilter.entries.forEach { type ->
                    FilterChip(
                        selected = query.mediaType == type,
                        onClick = { onUpdateQuery { it.copy(mediaType = type) } },
                        label = { Text(type.label) },
                    )
                }
            }

            // 2. File Name & OCR Text
            OutlinedTextField(
                value = query.fileNameFilter,
                onValueChange = { v -> onUpdateQuery { it.copy(fileNameFilter = v) } },
                label = { Text("File Name contains") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = query.ocrQuery,
                onValueChange = { v -> onUpdateQuery { it.copy(ocrQuery = v) } },
                label = { Text("OCR Extracted Text contains") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 3. Date Range Presets
            Text("Capture Date Window", style = MaterialTheme.typography.titleSmall)
            val now = System.currentTimeMillis()
            val datePresets = listOf(
                "Any" to null,
                "24h" to (now - TimeUnit.DAYS.toMillis(1)),
                "7 Days" to (now - TimeUnit.DAYS.toMillis(7)),
                "30 Days" to (now - TimeUnit.DAYS.toMillis(30)),
                "1 Year" to (now - TimeUnit.DAYS.toMillis(365)),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                datePresets.forEach { (label, threshold) ->
                    val selected = if (threshold == null) {
                        query.dateFromMillis == null
                    } else {
                        query.dateFromMillis != null && kotlin.math.abs((query.dateFromMillis ?: 0L) - threshold) < TimeUnit.HOURS.toMillis(6)
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateQuery { it.copy(dateFromMillis = threshold) } },
                        label = { Text(label) },
                    )
                }
            }

            // 4. Albums & Folders
            if (availableAlbums.isNotEmpty()) {
                Text("Albums", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableAlbums.take(12).forEach { album ->
                        val selected = album in query.albums
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onUpdateQuery { q ->
                                    val next = if (selected) q.albums - album else q.albums + album
                                    q.copy(albums = next)
                                }
                            },
                            label = { Text(album) },
                        )
                    }
                }
            }

            if (availableFolders.isNotEmpty()) {
                Text("Folders", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableFolders.take(10).forEach { folder ->
                        val selected = folder in query.folders
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onUpdateQuery { q ->
                                    val next = if (selected) q.folders - folder else q.folders + folder
                                    q.copy(folders = next)
                                }
                            },
                            label = { Text(folder.trimEnd('/').ifBlank { "/" }) },
                        )
                    }
                }
            }

            // 5. Formats
            val formatPool = (availableFormats + listOf("JPEG", "PNG", "WebP", "HEIC", "RAW", "MP4", "GIF")).distinct()
            Text("File Format", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formatPool.forEach { fmt ->
                    val selected = fmt in query.formats
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onUpdateQuery { q ->
                                val next = if (selected) q.formats - fmt else q.formats + fmt
                                q.copy(formats = next)
                            }
                        },
                        label = { Text(fmt) },
                    )
                }
            }

            // 6. File Size & Resolution / Orientation
            Text("File Size", style = MaterialTheme.typography.titleSmall)
            val sizeOptions = listOf(
                "Any" to (null to null),
                "< 500 KB" to (null to 500_000L),
                "> 2 MB" to (2_000_000L to null),
                "> 10 MB" to (10_000_000L to null),
                "> 50 MB" to (50_000_000L to null),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sizeOptions.forEach { (label, range) ->
                    val selected = query.minFileSizeBytes == range.first && query.maxFileSizeBytes == range.second
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onUpdateQuery {
                                it.copy(minFileSizeBytes = range.first, maxFileSizeBytes = range.second)
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }

            Text("Resolution & Orientation", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Any MP", 2f to "2+ MP", 8f to "8+ MP", 12f to "12+ MP").forEach { (mp, lbl) ->
                    FilterChip(
                        selected = query.minMegaPixels == mp,
                        onClick = { onUpdateQuery { it.copy(minMegaPixels = mp) } },
                        label = { Text(lbl) },
                    )
                }
                OrientationFilter.entries.forEach { ori ->
                    FilterChip(
                        selected = query.orientation == ori,
                        onClick = { onUpdateQuery { it.copy(orientation = ori) } },
                        label = { Text(ori.label) },
                    )
                }
            }

            // 7. EXIF Camera & ISO
            HorizontalDivider()
            Text("EXIF Camera & Exposure", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = query.cameraModelQuery,
                onValueChange = { v -> onUpdateQuery { it.copy(cameraModelQuery = v) } },
                label = { Text("Camera Make / Model (e.g., Pixel, Sony, Galaxy)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = query.maxIso == 200,
                    onClick = {
                        onUpdateQuery { q ->
                            q.copy(maxIso = if (q.maxIso == 200) null else 200, minIso = null)
                        }
                    },
                    label = { Text("Low ISO <= 200") },
                )
                FilterChip(
                    selected = query.minIso == 800,
                    onClick = {
                        onUpdateQuery { q ->
                            q.copy(minIso = if (q.minIso == 800) null else 800, maxIso = null)
                        }
                    },
                    label = { Text("High ISO >= 800") },
                )
            }

            // 8. User Tags, Ratings & AI Labels
            HorizontalDivider()
            Text("User Tags, Ratings & AI Signals", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = tagInput,
                onValueChange = { raw ->
                    tagInput = raw
                    val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onUpdateQuery { it.copy(requiredTags = list) }
                },
                label = { Text("Required User Tags (comma separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = aiLabelInput,
                onValueChange = { raw ->
                    aiLabelInput = raw
                    val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onUpdateQuery { it.copy(requiredAiLabels = list) }
                },
                label = { Text("Required On-Device AI Labels (e.g., Sky, Dog, Food)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Minimum Rating: ${if (query.minRating == 0) "Any" else "${query.minRating}★+"}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..5).forEach { stars ->
                    FilterChip(
                        selected = query.minRating == stars,
                        onClick = { onUpdateQuery { it.copy(minRating = stars) } },
                        label = { Text(if (stars == 0) "Any" else "${stars}★") },
                    )
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = query.favoritesOnly,
                    onClick = { onUpdateQuery { it.copy(favoritesOnly = !it.favoritesOnly) } },
                    label = { Text("Favorites") },
                )
                FilterChip(
                    selected = query.hasOcrTextOnly,
                    onClick = { onUpdateQuery { it.copy(hasOcrTextOnly = !it.hasOcrTextOnly) } },
                    label = { Text("Has OCR Text") },
                )
                FilterChip(
                    selected = query.documentsOnly,
                    onClick = { onUpdateQuery { it.copy(documentsOnly = !it.documentsOnly) } },
                    label = { Text("Documents") },
                )
                FilterChip(
                    selected = query.screenshotsOnly,
                    onClick = { onUpdateQuery { it.copy(screenshotsOnly = !it.screenshotsOnly) } },
                    label = { Text("Screenshots") },
                )
                FilterChip(
                    selected = query.excludeBlurry,
                    onClick = { onUpdateQuery { it.copy(excludeBlurry = !it.excludeBlurry, blurryOnly = false) } },
                    label = { Text("Sharp Only") },
                )
                FilterChip(
                    selected = query.blurryOnly,
                    onClick = { onUpdateQuery { it.copy(blurryOnly = !it.blurryOnly, excludeBlurry = false) } },
                    label = { Text("Blurry Only") },
                )
            }

            // 9. GPS Coordinates & Radius
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Geo-Tagged GPS Only", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = query.hasGpsOnly,
                    onCheckedChange = { checked -> onUpdateQuery { it.copy(hasGpsOnly = checked) } },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latInput,
                    onValueChange = { raw ->
                        latInput = raw
                        onUpdateQuery { it.copy(gpsCenterLat = raw.toDoubleOrNull()) }
                    },
                    label = { Text("Lat") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = lonInput,
                    onValueChange = { raw ->
                        lonInput = raw
                        onUpdateQuery { it.copy(gpsCenterLon = raw.toDoubleOrNull()) }
                    },
                    label = { Text("Lon") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            if (query.gpsCenterLat != null && query.gpsCenterLon != null) {
                val radius = query.gpsRadiusKm ?: 25.0
                Text("GPS Search Radius: ${radius.roundToInt()} km", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = radius.toFloat(),
                    onValueChange = { r -> onUpdateQuery { it.copy(gpsRadiusKm = r.toDouble()) } },
                    valueRange = 1f..200f,
                )
            }

            // 10. Face-Based Album Filtering
            if (availableFaceGroups.isNotEmpty()) {
                HorizontalDivider()
                Text("Filter by Face Group", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableFaceGroups.forEachIndexed { idx, group ->
                        val selected = group.groupId in query.faceGroupIds
                        val label = group.label.ifBlank { "Person #${idx + 1}" }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onUpdateQuery { q ->
                                    val next = if (selected) q.faceGroupIds - group.groupId else q.faceGroupIds + group.groupId
                                    q.copy(faceGroupIds = next)
                                }
                            },
                            label = { Text("$label (${group.faceCount})") },
                        )
                    }
                }
            }

            // 11. Sort Order
            HorizontalDivider()
            Text("Sort Results By", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchSortOption.entries.forEach { opt ->
                    FilterChip(
                        selected = query.sortBy == opt,
                        onClick = { onUpdateQuery { it.copy(sortBy = opt) } },
                        label = { Text(opt.label) },
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_filters_button"),
            ) {
                Text("Show Matching Media")
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
