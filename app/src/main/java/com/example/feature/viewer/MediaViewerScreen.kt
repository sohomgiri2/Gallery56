package com.example.feature.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.MediaIndexEntity
import com.example.core.datastore.ViewerBackgroundMode
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MediaThumbnailTile
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.formatBytesReadable
import com.example.core.media.MediaStoreOperations
import com.example.feature.smart.ColorAnalysisReport
import com.example.feature.smart.RgbHistogramChart
import com.example.feature.smart.SmartCollectionsEngine
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaViewerScreen(
    initialMediaId: Long,
    allMedia: List<MediaIndexEntity>,
    uiState: MediaViewerUiState,
    viewerBackgroundMode: ViewerBackgroundMode = ViewerBackgroundMode.AMOLED_BLACK,
    onActiveMediaChanged: (Long) -> Unit,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetRating: (Int) -> Unit,
    onSaveTagsAndNotes: (String, String) -> Unit,
    onAnalyzeNow: () -> Unit,
    onFindSimilar: (Long) -> Unit,
    onEditPhoto: (Long) -> Unit,
    onOpenVideoPlayer: (Long) -> Unit,
    onOpenMetadataViewer: (Long) -> Unit,
    onMoveToVault: (MediaIndexEntity) -> Unit,
    onMoveToTrash: (MediaIndexEntity) -> Unit,
    onOpenMedia: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaList = remember(allMedia, uiState.media) {
        if (allMedia.isNotEmpty()) allMedia else listOfNotNull(uiState.media)
    }
    val initialIndex = remember(mediaList, initialMediaId) {
        mediaList.indexOfFirst { it.mediaStoreId == initialMediaId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { mediaList.size.coerceAtLeast(1) })
    var currentPageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, mediaList) {
        mediaList.getOrNull(pagerState.currentPage)?.let { current ->
            if (current.mediaStoreId != uiState.media?.mediaStoreId) {
                onActiveMediaChanged(current.mediaStoreId)
            }
        }
    }

    val media = mediaList.getOrNull(pagerState.currentPage) ?: uiState.media
    var showInspectorSheet by remember { mutableStateOf(false) }
    var isSlideshowRunning by remember { mutableStateOf(false) }
    var compareTargetItem by remember { mutableStateOf<MediaIndexEntity?>(null) }
    var showComparePicker by remember { mutableStateOf(false) }

    val canvasBgColor = when (viewerBackgroundMode) {
        ViewerBackgroundMode.AMOLED_BLACK -> Color.Black
        ViewerBackgroundMode.DARK_GRAPHITE -> Color(0xFF141820)
        ViewerBackgroundMode.ADAPTIVE_THEME -> MaterialTheme.colorScheme.background
    }

    LaunchedEffect(isSlideshowRunning, mediaList.size) {
        if (isSlideshowRunning && mediaList.size > 1) {
            while (isSlideshowRunning) {
                delay(3_000L)
                val nextPage = (pagerState.currentPage + 1) % mediaList.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBgColor)
            .testTag("media_viewer_screen"),
    ) {
        if (mediaList.isEmpty() && media == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !currentPageZoomed,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val pageItem = mediaList[page]
                if (pageItem.isVideo) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(pageItem.contentUri).build(),
                            contentDescription = pageItem.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                        FilledTonalButton(
                            onClick = { onOpenVideoPlayer(pageItem.mediaStoreId) },
                            modifier = Modifier.testTag("viewer_play_video_btn"),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Open Video Player")
                        }
                    }
                } else {
                    ZoomablePhotoSurface(
                        uriString = pageItem.contentUri,
                        title = pageItem.displayName,
                        onZoomStateChanged = { zoomed ->
                            if (page == pagerState.currentPage) currentPageZoomed = zoomed
                        },
                    )
                }
            }
        }

        // Top Floating Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag("viewer_back_button"),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            if (media != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { isSlideshowRunning = !isSlideshowRunning },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSlideshowRunning) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f))
                            .testTag("viewer_slideshow_button"),
                    ) {
                        Icon(
                            imageVector = if (isSlideshowRunning) Icons.Rounded.Pause else Icons.Rounded.Slideshow,
                            contentDescription = if (isSlideshowRunning) "Pause slideshow" else "Start slideshow",
                            tint = if (isSlideshowRunning) MaterialTheme.colorScheme.onPrimary else Color.White,
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("viewer_favorite_button"),
                    ) {
                        Icon(
                            imageVector = if (media.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (media.isFavorite) StatusRose else Color.White,
                        )
                    }
                    IconButton(
                        onClick = { onOpenMetadataViewer(media.mediaStoreId) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("viewer_metadata_button"),
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "EXIF & Metadata", tint = Color.White)
                    }
                    IconButton(
                        onClick = { onFindSimilar(media.mediaStoreId) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("viewer_visual_search_button"),
                    ) {
                        Icon(Icons.Rounded.ImageSearch, contentDescription = "Find visually similar", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = media.mimeType
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(media.contentUri))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via Gallery56"))
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("viewer_share_button"),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share media", tint = Color.White)
                    }
                }
            }
        }

        // Bottom Floating Controls
        if (media != null) {
            GlassPanel(
                contentPadding = 12.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 22.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = media.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(
                            text = "${media.format} • ${media.resolutionLabel} • ${formatBytesReadable(media.fileSizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { showInspectorSheet = true },
                        modifier = Modifier.testTag("open_inspector_button"),
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI & Tags")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!media.isVideo) {
                        FilledTonalButton(
                            onClick = { onEditPhoto(media.mediaStoreId) },
                            modifier = Modifier.testTag("viewer_edit_photo_btn"),
                        ) {
                            Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit")
                        }
                        FilledTonalButton(
                            onClick = { showComparePicker = true },
                            modifier = Modifier.testTag("viewer_compare_btn"),
                        ) {
                            Icon(Icons.Rounded.Compare, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Compare")
                        }
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    val ok = MediaStoreOperations.setAsWallpaper(context, media.contentUri)
                                    Toast.makeText(
                                        context,
                                        if (ok) "Wallpaper updated" else "Unable to set wallpaper",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        ) {
                            Icon(Icons.Rounded.Wallpaper, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Wallpaper")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { onOpenVideoPlayer(media.mediaStoreId) },
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Player")
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Media URI", media.contentUri))
                            Toast.makeText(context, "Media URI copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy URI")
                    }
                    FilledTonalButton(
                        onClick = {
                            try {
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(media.contentUri), media.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(viewIntent, "Open with"))
                            } catch (_: Throwable) {
                                Toast.makeText(context, "No external app found", Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Open With")
                    }
                    FilledTonalButton(
                        onClick = {
                            onMoveToVault(media)
                            onBack()
                        },
                        modifier = Modifier.testTag("viewer_vault_btn"),
                    ) {
                        Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Vault")
                    }
                    OutlinedButton(
                        onClick = {
                            onMoveToTrash(media)
                            onBack()
                        },
                        modifier = Modifier.testTag("viewer_trash_btn"),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(16.dp), tint = StatusRose)
                        Spacer(Modifier.width(4.dp))
                        Text("Trash", color = StatusRose)
                    }
                }
            }
        }
    }

    if (showInspectorSheet && media != null) {
        var tagsInput by remember(media.userTags) { mutableStateOf(media.userTags) }
        var notesInput by remember(media.userNotes) { mutableStateOf(media.userNotes) }

        ModalBottomSheet(
            onDismissRequest = { showInspectorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("AI Intelligence & Forensics", style = MaterialTheme.typography.headlineSmall)
                    FilledTonalButton(
                        onClick = onAnalyzeNow,
                        enabled = !uiState.isAnalyzingNow,
                        modifier = Modifier.testTag("analyze_single_now_btn"),
                    ) {
                        Text(if (uiState.isAnalyzingNow) "Analyzing..." else "Run ML Kit")
                    }
                }

                Text("User Rating & Searchable Tags", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = {
                                val next = if (media.userRating == star) 0 else star
                                onSetRating(next)
                            },
                            modifier = Modifier.testTag("rate_star_$star"),
                        ) {
                            Icon(
                                imageVector = if (star <= media.userRating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "Rate $star stars",
                                tint = if (star <= media.userRating) StatusAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Custom Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("viewer_tags_input"),
                )
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Searchable Notes") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSaveTagsAndNotes(tagsInput, notesInput)
                            Toast.makeText(context, "Indexed into Search 2.0", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("save_viewer_tags_btn"),
                    ) { Text("Save & Index") }
                    OutlinedButton(
                        onClick = {
                            showInspectorSheet = false
                            onFindSimilar(media.mediaStoreId)
                        },
                    ) {
                        Icon(Icons.Rounded.ImageSearch, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Visual Search")
                    }
                }

                HorizontalDivider()

                if (media.generatedCaption.isNotBlank()) {
                    Text("Structured On-Device Description", style = MaterialTheme.typography.titleSmall)
                    Text(text = media.generatedCaption, style = MaterialTheme.typography.bodyMedium)
                }

                Text("Image Quality & Classification", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricPill("Quality ${media.qualityScore.toInt()}/100", accentColor = StatusEmerald)
                    MetricPill("Sharpness ${media.sharpnessScore.toInt()}")
                    if (media.isBlurry) MetricPill("Soft / Blurry", accentColor = StatusRose)
                    if (media.isDocument) MetricPill("Document ${(media.documentConfidence * 100).toInt()}%")
                    if (media.isScreenshot) MetricPill("Screenshot")
                    if (media.faceCount > 0) MetricPill("${media.faceCount} Face(s)")
                }

                if (uiState.aiLabels.isNotEmpty()) {
                    Text("On-Device ML Kit Labels", style = MaterialTheme.typography.titleSmall)
                    uiState.aiLabels.take(8).forEach { label ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(label.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(120.dp))
                            LinearProgressIndicator(progress = { label.confidence.coerceIn(0f, 1f) }, modifier = Modifier.weight(1f))
                            Text("${(label.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (uiState.aiObjects.isNotEmpty()) {
                    Text("Detected Objects", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.aiObjects.forEach { obj ->
                            MetricPill("${obj.category} (${(obj.confidence * 100).toInt()}%)")
                        }
                    }
                }

                if (media.ocrText.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("OCR Extracted Text", style = MaterialTheme.typography.titleSmall)
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("OCR Text", media.ocrText))
                                Toast.makeText(context, "OCR text copied", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy OCR text")
                        }
                    }
                    GlassPanel(contentPadding = 10.dp, modifier = Modifier.fillMaxWidth()) {
                        Text(text = media.ocrText, style = MaterialTheme.typography.labelMedium)
                    }
                }

                HorizontalDivider()
                Text("EXIF, GPS & Forensic Hashes", style = MaterialTheme.typography.titleSmall)
                val dateFmt = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
                Text(
                    text = buildString {
                        appendLine("Captured: ${dateFmt.format(Date(media.dateTakenMillis))}")
                        appendLine("Album / Folder: ${media.albumName} (${media.relativePath})")
                        if (media.cameraModel.isNotBlank()) {
                            appendLine("Camera: ${media.cameraMake} ${media.cameraModel} ${media.focalLength} ${media.aperture} ISO ${media.iso} ${media.exposureTime}".trim())
                        }
                        if (media.hasGps && media.latitude != null && media.longitude != null) {
                            appendLine(String.format(Locale.US, "GPS: %.5f, %.5f", media.latitude, media.longitude))
                        }
                        if (media.sha256Hash.isNotBlank()) {
                            appendLine("SHA-256: ${media.sha256Hash.take(24)}…")
                        }
                        if (media.dHash != 0L) {
                            append("dHash64: 0x${media.dHash.toULong().toString(16)}")
                        }
                    }.trim(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!media.isVideo) {
                    var colorReport by remember(media.mediaStoreId) { mutableStateOf<ColorAnalysisReport?>(null) }
                    LaunchedEffect(media.mediaStoreId) {
                        colorReport = withContext(Dispatchers.IO) {
                            try {
                                val bmp = context.contentResolver.openInputStream(Uri.parse(media.contentUri))?.use {
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
                    colorReport?.let { report ->
                        HorizontalDivider()
                        Text("RGB Color Histogram & Palette", style = MaterialTheme.typography.titleSmall)
                        RgbHistogramChart(
                            report = report,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            report.dominantColors.forEach { colorInt ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                )
                            }
                        }
                    }
                }

                if (uiState.visuallySimilar.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Visually Similar in Library", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.visuallySimilar, key = { it.mediaStoreId }) { sim ->
                            Box(modifier = Modifier.size(86.dp)) {
                                MediaThumbnailTile(
                                    media = sim,
                                    onClick = {
                                        showInspectorSheet = false
                                        onOpenMedia(sim.mediaStoreId)
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showComparePicker && media != null) {
        val candidates = remember(allMedia, media.mediaStoreId) {
            allMedia.filter { !it.isVideo && it.mediaStoreId != media.mediaStoreId }.take(60)
        }
        AlertDialog(
            onDismissRequest = { showComparePicker = false },
            title = { Text("Pick Photo to Compare Side-by-Side") },
            text = {
                if (candidates.isEmpty()) {
                    Text("No other photos available in library.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(candidates, key = { it.mediaStoreId }) { cand ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        compareTargetItem = cand
                                        showComparePicker = false
                                    },
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(cand.contentUri).size(200).build(),
                                    contentDescription = cand.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showComparePicker = false }) { Text("Close") }
            },
        )
    }

    if (compareTargetItem != null && media != null) {
        val second = compareTargetItem!!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(media.contentUri).build(),
                        contentDescription = media.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    Text(
                        text = "A: ${media.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1,
                    )
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(second.contentUri).build(),
                        contentDescription = second.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    Text(
                        text = "B: ${second.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1,
                    )
                }
            }
            IconButton(
                onClick = { compareTargetItem = null },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f)),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Exit comparison", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ZoomablePhotoSurface(
    uriString: String,
    title: String,
    onZoomStateChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                            onZoomStateChanged(false)
                        } else {
                            scale = 2.5f
                            onZoomStateChanged(true)
                        }
                    },
                )
            }
            .pointerInput(scale) {
                if (scale > 1.02f) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = nextScale
                        offset = if (nextScale > 1.02f) offset + pan else Offset.Zero
                        onZoomStateChanged(nextScale > 1.02f)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(uriString).build(),
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}
