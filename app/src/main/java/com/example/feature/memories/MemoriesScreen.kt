package com.example.feature.memories

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MediaThumbnailTile
import com.example.core.designsystem.MetricPill
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MemoryCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val items: List<MediaIndexEntity>,
)

@Composable
fun MemoriesScreen(
    allMedia: List<MediaIndexEntity>,
    onOpenMedia: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val collections = remember(allMedia) { deriveRealMemories(allMedia) }
    var activeSlideshow by remember { mutableStateOf<MemoryCollection?>(null) }

    val slideshow = activeSlideshow
    if (slideshow != null && slideshow.items.isNotEmpty()) {
        MemorySlideshowOverlay(
            collection = slideshow,
            onOpenMedia = onOpenMedia,
            onClose = { activeSlideshow = null },
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("memories_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("MEMORIES", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Date & GPS timelines derived strictly from your real library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (collections.isEmpty()) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Text("No Memories Built Yet", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Import or grant library access to photos so Gallery56 can group real dates, years, and GPS journeys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(collections, key = { it.id }) { collection ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            MetricPill(
                                label = collection.category,
                                icon = if (collection.category.contains("GPS")) Icons.Rounded.LocationOn else Icons.Rounded.CalendarMonth,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(collection.title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = collection.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = { activeSlideshow = collection },
                            modifier = Modifier.testTag("memory_play_${collection.id}"),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(collection.items.take(12), key = { it.mediaStoreId }) { item ->
                            MediaThumbnailTile(
                                media = item,
                                onClick = { onOpenMedia(item.mediaStoreId) },
                                modifier = Modifier.size(104.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemorySlideshowOverlay(
    collection: MemoryCollection,
    onOpenMedia: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var idx by remember(collection.id) { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var intervalMs by remember { mutableLongStateOf(3000L) }

    val items = collection.items
    val current = items[idx.coerceIn(0, items.lastIndex)]

    LaunchedEffect(isPlaying, intervalMs, idx, items.size) {
        if (isPlaying && items.size > 1) {
            delay(intervalMs)
            idx = (idx + 1) % items.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("memory_slideshow_player"),
    ) {
        AnimatedContent(
            targetState = current,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "memory_slide",
        ) { slide ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(slide.contentUri)
                    .crossfade(true)
                    .build(),
                contentDescription = slide.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onOpenMedia(slide.mediaStoreId) },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(collection.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(
                    "${idx + 1} / ${items.size} • ${current.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close slideshow", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { idx = if (idx == 0) items.lastIndex else idx - 1 },
                ) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                }
                IconButton(onClick = { isPlaying = !isPlaying }) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
                IconButton(
                    onClick = { idx = (idx + 1) % items.size },
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                listOf(1500L to "1.5s", 3000L to "3s", 5000L to "5s").forEach { (ms, label) ->
                    FilterChip(
                        selected = intervalMs == ms,
                        onClick = { intervalMs = ms },
                        label = { Text(label) },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

private fun deriveRealMemories(allMedia: List<MediaIndexEntity>): List<MemoryCollection> {
    if (allMedia.isEmpty()) return emptyList()
    val result = mutableListOf<MemoryCollection>()
    val cal = Calendar.getInstance()
    val todayMonth = cal.get(Calendar.MONTH)
    val todayDay = cal.get(Calendar.DAY_OF_MONTH)
    val todayYear = cal.get(Calendar.YEAR)

    // 1. On This Day across previous years
    val onThisDay = allMedia.filter { item ->
        cal.timeInMillis = item.dateTakenMillis
        cal.get(Calendar.MONTH) == todayMonth &&
            cal.get(Calendar.DAY_OF_MONTH) == todayDay &&
            cal.get(Calendar.YEAR) < todayYear
    }
    if (onThisDay.isNotEmpty()) {
        result += MemoryCollection(
            id = "on_this_day",
            title = "On This Day",
            subtitle = "${onThisDay.size} past memories from today's date",
            category = "Anniversary",
            items = onThisDay,
        )
    }

    // 2. Geo-Tagged Journeys
    val geoMedia = allMedia.filter { it.hasGps && it.latitude != null && it.longitude != null }
    if (geoMedia.isNotEmpty()) {
        val groupedGeo = geoMedia.groupBy { item ->
            val lat = String.format(Locale.US, "%.1f", item.latitude)
            val lon = String.format(Locale.US, "%.1f", item.longitude)
            "$lat, $lon"
        }
        groupedGeo.entries.take(4).forEachIndexed { i, (coordKey, list) ->
            result += MemoryCollection(
                id = "geo_$i",
                title = "Location Cluster ($coordKey)",
                subtitle = "${list.size} geo-tagged photos",
                category = "GPS Journey",
                items = list,
            )
        }
    }

    // 3. Monthly Chronicles
    val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    allMedia
        .groupBy { monthFmt.format(Date(it.dateTakenMillis)) }
        .entries
        .take(6)
        .forEach { (monthLabel, list) ->
            result += MemoryCollection(
                id = "month_$monthLabel",
                title = monthLabel,
                subtitle = "${list.size} captured items",
                category = "Monthly Chronicle",
                items = list,
            )
        }

    // 4. Yearly Highlights
    val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())
    allMedia
        .groupBy { yearFmt.format(Date(it.dateTakenMillis)) }
        .entries
        .forEach { (yr, list) ->
            if (list.size >= 2) {
                result += MemoryCollection(
                    id = "year_$yr",
                    title = "$yr Archive",
                    subtitle = "${list.size} photos & videos from $yr",
                    category = "Year in Review",
                    items = list.sortedByDescending { it.qualityScore },
                )
            }
        }

    return result
}
