package com.example.feature.smart

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BurstMode
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MediaThumbnailTile
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.formatBytesReadable
import com.example.ui.theme.LocalGallery56Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

enum class SmartCollectionTab(val label: String) {
    RECENTLY_VIEWED("Viewed"),
    RECENTLY_EDITED("Edited"),
    SCREENSHOTS("Screenshots"),
    LARGE_FILES("Large Files"),
    VIDEOS("Videos"),
    BURSTS("Burst Groups"),
    COLOR_HISTOGRAM("Color & Histogram"),
}

data class BurstCluster(
    val id: String,
    val title: String,
    val items: List<MediaIndexEntity>,
)

data class ColorAnalysisReport(
    val redBins: IntArray,
    val greenBins: IntArray,
    val blueBins: IntArray,
    val dominantColors: List<Int>,
)

object SmartCollectionsEngine {

    /**
     * Groups sequential captures taken within [windowMillis] in the same album or with existing groupIds.
     */
    fun detectBurstGroups(items: List<MediaIndexEntity>, windowMillis: Long = 3_500L): List<BurstCluster> {
        val explicitGroups = items.filter { it.groupId.isNotBlank() }
            .groupBy { it.groupId }
            .filterValues { it.size >= 2 }
            .map { (gid, list) ->
                BurstCluster("exp_$gid", "AI Burst (${list.size} shots)", list.sortedByDescending { it.qualityScore })
            }

        val sortedPhotos = items.filter { !it.isVideo && it.groupId.isBlank() }
            .sortedBy { it.dateTakenMillis }
        val timeClusters = mutableListOf<BurstCluster>()
        var current = mutableListOf<MediaIndexEntity>()
        for (photo in sortedPhotos) {
            if (current.isEmpty()) {
                current.add(photo)
            } else {
                val prev = current.last()
                val closeInTime = abs(photo.dateTakenMillis - prev.dateTakenMillis) <= windowMillis
                val sameAlbum = photo.albumName == prev.albumName
                if (closeInTime && sameAlbum) {
                    current.add(photo)
                } else {
                    if (current.size >= 2) {
                        timeClusters.add(
                            BurstCluster(
                                id = "burst_${current.first().mediaStoreId}",
                                title = "${current.first().albumName.ifBlank { "Camera" }} Burst (${current.size})",
                                items = current.toList(),
                            ),
                        )
                    }
                    current = mutableListOf(photo)
                }
            }
        }
        if (current.size >= 2) {
            timeClusters.add(
                BurstCluster(
                    id = "burst_${current.first().mediaStoreId}",
                    title = "${current.first().albumName.ifBlank { "Camera" }} Burst (${current.size})",
                    items = current.toList(),
                ),
            )
        }
        return explicitGroups + timeClusters
    }

    fun analyzeBitmapColors(bitmap: Bitmap): ColorAnalysisReport {
        val bins = 32
        val red = IntArray(bins)
        val green = IntArray(bins)
        val blue = IntArray(bins)

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val buckets = mutableMapOf<Int, Int>()
        val step = maxOf(1, pixels.size / 12_000)
        var i = 0
        while (i < pixels.size) {
            val c = pixels[i]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            red[(r * (bins - 1)) / 255]++
            green[(g * (bins - 1)) / 255]++
            blue[(b * (bins - 1)) / 255]++

            // Quantize to 4-bit per channel for dominant palette frequency
            val qr = (r and 0xF0)
            val qg = (g and 0xF0)
            val qb = (b and 0xF0)
            val key = (0xFF shl 24) or (qr shl 16) or (qg shl 8) or qb
            buckets[key] = (buckets[key] ?: 0) + 1
            i += step
        }

        val dominant = buckets.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        return ColorAnalysisReport(
            redBins = red,
            greenBins = green,
            blueBins = blue,
            dominantColors = dominant,
        )
    }
}

@Composable
fun SmartCollectionsScreen(
    allMedia: List<MediaIndexEntity>,
    initialTab: SmartCollectionTab = SmartCollectionTab.RECENTLY_VIEWED,
    onBatchRename: (List<MediaIndexEntity>, String) -> Unit,
    onOpenMedia: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val tokens = LocalGallery56Tokens.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(initialTab) }
    var selectedPaletteMedia by remember(allMedia) { mutableStateOf(allMedia.firstOrNull { !it.isVideo }) }
    var colorReport by remember { mutableStateOf<ColorAnalysisReport?>(null) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var renamePrefix by remember { mutableStateOf("Gallery56_Batch") }

    val filteredItems = remember(allMedia, activeTab) {
        when (activeTab) {
            SmartCollectionTab.RECENTLY_VIEWED -> allMedia.filter { it.lastViewedAtMillis > 0L }.sortedByDescending { it.lastViewedAtMillis }
            SmartCollectionTab.RECENTLY_EDITED -> allMedia.filter { it.lastEditedAtMillis > 0L }.sortedByDescending { it.lastEditedAtMillis }
            SmartCollectionTab.SCREENSHOTS -> allMedia.filter { it.isScreenshot || it.albumName.contains("screenshot", ignoreCase = true) }
            SmartCollectionTab.LARGE_FILES -> allMedia.filter { it.fileSizeBytes >= 5L * 1024L * 1024L }.sortedByDescending { it.fileSizeBytes }
            SmartCollectionTab.VIDEOS -> allMedia.filter { it.isVideo }.sortedByDescending { it.durationMillis }
            SmartCollectionTab.BURSTS, SmartCollectionTab.COLOR_HISTOGRAM -> allMedia.filter { !it.isVideo }
        }
    }

    val bursts = remember(allMedia) { SmartCollectionsEngine.detectBurstGroups(allMedia) }

    LaunchedEffect(selectedPaletteMedia, activeTab) {
        if (activeTab == SmartCollectionTab.COLOR_HISTOGRAM && selectedPaletteMedia != null) {
            val uriStr = selectedPaletteMedia!!.contentUri
            colorReport = withContext(Dispatchers.IO) {
                try {
                    val bmp = context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 4 })
                    }
                    bmp?.let {
                        val r = SmartCollectionsEngine.analyzeBitmapColors(it)
                        it.recycle()
                        r
                    }
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_collections_screen"),
    ) {
        Gallery56TopBar(
            title = "SMART COLLECTIONS & COLOR",
            subtitle = "${allMedia.size} indexed library items",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.semantic.iconPrimary)
                }
            },
            primaryActions = {
                if (filteredItems.isNotEmpty() && activeTab != SmartCollectionTab.COLOR_HISTOGRAM) {
                    IconButton(onClick = { showBatchRenameDialog = true }) {
                        Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = "Batch rename visible")
                    }
                }
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmartCollectionTab.entries.forEach { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = { Text(tab.label, maxLines = 1, softWrap = false) },
                )
            }
        }

        when (activeTab) {
            SmartCollectionTab.BURSTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (bursts.isEmpty()) {
                        item {
                            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                                Text("No rapid-fire burst groups detected yet.", color = tokens.semantic.textSecondary)
                            }
                        }
                    } else {
                        items(bursts, key = { it.id }) { cluster ->
                            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(cluster.title, style = MaterialTheme.typography.titleSmall)
                                    Icon(Icons.Rounded.BurstMode, null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    cluster.items.forEach { item ->
                                        Box(modifier = Modifier.size(86.dp)) {
                                            MediaThumbnailTile(
                                                media = item,
                                                badgeText = if (item.isBestShot) "Best" else null,
                                                onClick = { onOpenMedia(item.mediaStoreId) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SmartCollectionTab.COLOR_HISTOGRAM -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = selectedPaletteMedia?.displayName ?: "No Photo Available",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            colorReport?.let { report ->
                                Text("Dominant 5-Color Palette", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    report.dominantColors.forEach { argb ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(argb))
                                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                            )
                                            Text(
                                                text = String.format("#%06X", argb and 0xFFFFFF),
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("RGB Tone Histogram", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(6.dp))
                                RgbHistogramChart(report = report, modifier = Modifier.fillMaxWidth().height(110.dp))
                            } ?: Text("Decoding image color profile...", color = tokens.semantic.textSecondary)
                        }
                    }
                    item {
                        Text("Tap any photo below to inspect its histogram & palette", style = MaterialTheme.typography.titleSmall)
                    }
                    items(filteredItems.take(20).chunked(4)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MediaThumbnailTile(
                                        media = item,
                                        isSelected = selectedPaletteMedia?.mediaStoreId == item.mediaStoreId,
                                        onClick = { selectedPaletteMedia = item },
                                    )
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            else -> {
                if (filteredItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No items in ${activeTab.label} yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = tokens.semantic.textSecondary,
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredItems, key = { it.mediaStoreId }) { item ->
                            MediaThumbnailTile(
                                media = item,
                                badgeText = if (activeTab == SmartCollectionTab.LARGE_FILES) {
                                    formatBytesReadable(item.fileSizeBytes)
                                } else null,
                                onClick = { onOpenMedia(item.mediaStoreId) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBatchRenameDialog) {
        AlertDialog(
            onDismissRequest = { showBatchRenameDialog = false },
            title = { Text("Batch Rename (${filteredItems.size} items)") },
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
                        onBatchRename(filteredItems, renamePrefix)
                        showBatchRenameDialog = false
                        Toast.makeText(context, "Renamed ${filteredItems.size} items", Toast.LENGTH_SHORT).show()
                    },
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchRenameDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun RgbHistogramChart(report: ColorAnalysisReport, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0E14)),
    ) {
        val bins = report.redBins.size
        val maxVal = maxOf(
            report.redBins.maxOrNull() ?: 1,
            report.greenBins.maxOrNull() ?: 1,
            report.blueBins.maxOrNull() ?: 1,
        ).coerceAtLeast(1).toFloat()

        fun drawChannel(counts: IntArray, color: Color) {
            val path = Path()
            val stepX = size.width / (bins - 1).coerceAtLeast(1)
            path.moveTo(0f, size.height)
            for (i in 0 until bins) {
                val x = i * stepX
                val y = size.height - (counts[i] / maxVal) * (size.height * 0.88f)
                path.lineTo(x, y)
            }
            path.lineTo(size.width, size.height)
            path.close()
            drawPath(path, color.copy(alpha = 0.38f))
            for (i in 0 until bins - 1) {
                val x1 = i * stepX
                val y1 = size.height - (counts[i] / maxVal) * (size.height * 0.88f)
                val x2 = (i + 1) * stepX
                val y2 = size.height - (counts[i + 1] / maxVal) * (size.height * 0.88f)
                drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = 2.5f)
            }
        }

        drawChannel(report.redBins, Color(0xFFFB7185))
        drawChannel(report.greenBins, Color(0xFF34D399))
        drawChannel(report.blueBins, Color(0xFF60A5FA))
    }
}
