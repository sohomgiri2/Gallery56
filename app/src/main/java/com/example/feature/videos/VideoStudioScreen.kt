package com.example.feature.videos

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.GifBox
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.formatDurationShort
import com.example.ui.theme.LocalGallery56Tokens
import kotlinx.coroutines.launch

@Composable
fun VideoStudioScreen(
    allMedia: List<MediaIndexEntity>,
    onBack: () -> Unit,
    onSavedToMediaStore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGallery56Tokens.current

    val videos = remember(allMedia) { allMedia.filter { it.isVideo } }
    var selectedVideo by remember { mutableStateOf(videos.firstOrNull()) }
    var customUri by remember { mutableStateOf<Uri?>(null) }

    val activeUri: Uri? = customUri ?: selectedVideo?.contentUri?.let { Uri.parse(it) }
    val durationMs = (selectedVideo?.durationMillis ?: 10_000L).coerceAtLeast(1000L)

    var trimRange by remember(activeUri) { mutableStateOf(0f..durationMs.toFloat()) }
    var frameAtMs by remember(activeUri) { mutableFloatStateOf(durationMs * 0.25f) }
    var isProcessing by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) customUri = uri
    }

    val player = remember {
        ExoPlayer.Builder(context).build()
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
    LaunchedEffect(activeUri) {
        if (activeUri != null) {
            player.setMediaItem(MediaItem.fromUri(activeUri))
            player.prepare()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("video_studio_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Gallery56TopBar(
                title = "VIDEO STUDIO",
                subtitle = "Media3 preview, MP4 trim, frame capture & GIF export",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.semantic.iconPrimary)
                    }
                },
            )
        }

        if (isProcessing) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    if (activeUri != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    this.player = player
                                    useController = true
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Movie, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Text("Select a video below or via Photo Picker", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Choose Video via Photo Picker")
                }
            }
        }

        if (videos.isNotEmpty()) {
            item {
                Text("Device Video Clips (${videos.size})", style = MaterialTheme.typography.titleSmall, color = tokens.semantic.textPrimary)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(videos.take(24), key = { it.mediaStoreId }) { item ->
                        Column(
                            modifier = Modifier
                                .width(110.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.elevatedSurface)
                                .clickable {
                                    customUri = null
                                    selectedVideo = item
                                },
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(item.contentUri).size(200).build(),
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(68.dp),
                            )
                            Text(
                                text = formatDurationShort(item.durationMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.semantic.textPrimary,
                            )
                        }
                    }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Trim Segment & Animated GIF", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Range: ${formatDurationShort(trimRange.start.toLong())} – ${formatDurationShort(trimRange.endInclusive.toLong())}",
                    style = MaterialTheme.typography.labelLarge,
                )
                RangeSlider(
                    value = trimRange,
                    onValueChange = { trimRange = it },
                    valueRange = 0f..durationMs.toFloat(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        enabled = activeUri != null && !isProcessing,
                        onClick = {
                            val uri = activeUri ?: return@Button
                            scope.launch {
                                isProcessing = true
                                val saved = VideoProcessingEngine.trimVideoToMediaStore(
                                    context = context,
                                    videoUri = uri,
                                    startMillis = trimRange.start.toLong(),
                                    endMillis = trimRange.endInclusive.toLong(),
                                )
                                isProcessing = false
                                if (saved != null) {
                                    onSavedToMediaStore()
                                    Toast.makeText(context, "Trimmed MP4 saved to Movies/Gallery56_Trimmed", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Could not trim selected track", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ContentCut, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Trim MP4")
                    }
                    FilledTonalButton(
                        enabled = activeUri != null && !isProcessing,
                        onClick = {
                            val uri = activeUri ?: return@FilledTonalButton
                            scope.launch {
                                isProcessing = true
                                val saved = VideoProcessingEngine.exportVideoClipToGif(
                                    context = context,
                                    videoUri = uri,
                                    startMillis = trimRange.start.toLong(),
                                    endMillis = trimRange.endInclusive.toLong(),
                                )
                                isProcessing = false
                                if (saved != null) {
                                    onSavedToMediaStore()
                                    Toast.makeText(context, "Saved animated GIF to Pictures/Gallery56_GIFs", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "GIF export failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.GifBox, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export GIF")
                    }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Frame Extractor", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("Timestamp: ${formatDurationShort(frameAtMs.toLong())}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = frameAtMs.coerceIn(0f, durationMs.toFloat()),
                    onValueChange = {
                        frameAtMs = it
                        player.seekTo(it.toLong())
                    },
                    valueRange = 0f..durationMs.toFloat(),
                )
                Button(
                    enabled = activeUri != null && !isProcessing,
                    onClick = {
                        val uri = activeUri ?: return@Button
                        scope.launch {
                            isProcessing = true
                            val saved = VideoProcessingEngine.extractFrameToMediaStore(
                                context = context,
                                videoUri = uri,
                                timeMillis = frameAtMs.toLong(),
                            )
                            isProcessing = false
                            if (saved != null) {
                                onSavedToMediaStore()
                                Toast.makeText(context, "Saved frame to Pictures/Gallery56_Frames", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Frame capture failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Extract High-Res Frame Still")
                }
            }
        }
    }
}
