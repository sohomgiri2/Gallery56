package com.example.feature.videos

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.core.database.MediaIndexEntity
import com.example.core.database.VideoPlaybackDao
import com.example.core.database.VideoPlaybackEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.formatBytesReadable
import com.example.core.designsystem.formatDurationShort
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerScreen(
    media: MediaIndexEntity?,
    videoPlaybackDao: VideoPlaybackDao,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(media?.durationMillis ?: 0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLandscape by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var gestureHudLabel by remember { mutableStateOf<String?>(null) }
    var brightnessFraction by remember { mutableFloatStateOf(0.5f) }

    val exoPlayer = remember(media?.contentUri) {
        ExoPlayer.Builder(context).build().apply {
            if (media != null) {
                setMediaItem(MediaItem.fromUri(media.contentUri))
                prepare()
                playWhenReady = true
            }
        }
    }

    // Resume playback from saved position
    LaunchedEffect(media?.mediaStoreId) {
        if (media != null) {
            val saved = videoPlaybackDao.getState(media.mediaStoreId)
            if (saved != null && saved.positionMillis > 1000L && saved.positionMillis < (saved.durationMillis - 2000L)) {
                exoPlayer.seekTo(saved.positionMillis)
                playbackSpeed = saved.playbackSpeed
                exoPlayer.playbackParameters = PlaybackParameters(saved.playbackSpeed)
            }
        }
    }

    // Poll playback position & persist resume state
    LaunchedEffect(exoPlayer, media?.mediaStoreId) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val d = exoPlayer.duration
            if (d > 0L) durationMs = d
            if (media != null && currentPosMs > 0L) {
                videoPlaybackDao.upsert(
                    VideoPlaybackEntity(
                        mediaStoreId = media.mediaStoreId,
                        positionMillis = currentPosMs,
                        durationMillis = durationMs,
                        playbackSpeed = playbackSpeed,
                    ),
                )
            }
            delay(600L)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            if (media != null) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration.coerceAtLeast(durationMs)
                scope.launch {
                    videoPlaybackDao.upsert(
                        VideoPlaybackEntity(
                            mediaStoreId = media.mediaStoreId,
                            positionMillis = pos,
                            durationMillis = dur,
                            playbackSpeed = playbackSpeed,
                        ),
                    )
                }
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen"),
    ) {
        if (media == null) {
            Text(
                text = "Video not found",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Left / Right vertical gesture overlays for Brightness & Volume
            Row(modifier = Modifier.fillMaxSize()) {
                // Left half -> Brightness gesture
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { gestureHudLabel = null },
                                onVerticalDrag = { _, dragAmount ->
                                    brightnessFraction = (brightnessFraction - dragAmount / 500f).coerceIn(0.05f, 1f)
                                    activity?.window?.let { win ->
                                        val lp = win.attributes
                                        lp.screenBrightness = brightnessFraction
                                        win.attributes = lp
                                    }
                                    gestureHudLabel = "Brightness ${(brightnessFraction * 100).toInt()}%"
                                },
                            )
                        },
                )
                // Right half -> Volume gesture
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { gestureHudLabel = null },
                                onVerticalDrag = { _, dragAmount ->
                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                    val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val delta = if (dragAmount < -18f) 1 else if (dragAmount > 18f) -1 else 0
                                    if (delta != 0) {
                                        val next = (curVol + delta).coerceIn(0, maxVol)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                                        gestureHudLabel = "Volume ${(next * 100 / maxVol)}%"
                                    }
                                },
                            )
                        },
                )
            }

            if (gestureHudLabel != null) {
                GlassPanel(
                    contentPadding = 12.dp,
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (gestureHudLabel!!.startsWith("Brightness")) {
                                Icons.Rounded.BrightnessHigh
                            } else {
                                Icons.Rounded.VolumeUp
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(gestureHudLabel!!, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = media.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { showSpeedMenu = !showSpeedMenu },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    ) {
                        Icon(Icons.Rounded.Speed, contentDescription = "Playback speed", tint = Color.White)
                    }
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "Video info", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            isLandscape = !isLandscape
                            activity?.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    ) {
                        Icon(Icons.Rounded.Fullscreen, contentDescription = "Toggle fullscreen orientation", tint = Color.White)
                    }
                }
            }

            // Bottom transport controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showSpeedMenu) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            FilterChip(
                                selected = playbackSpeed == spd,
                                onClick = {
                                    playbackSpeed = spd
                                    exoPlayer.playbackParameters = PlaybackParameters(spd)
                                    showSpeedMenu = false
                                },
                                label = { Text("${spd}x") },
                            )
                        }
                    }
                }

                GlassPanel(contentPadding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier.testTag("video_play_pause_btn"),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = formatDurationShort(currentPosMs),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Slider(
                            value = if (durationMs > 0L) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { frac ->
                                val seekTo = (frac * durationMs).toLong()
                                currentPosMs = seekTo
                                exoPlayer.seekTo(seekTo)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatDurationShort(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        text = "Swipe left edge: Brightness • Swipe right edge: Volume • Speed ${playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showInfoDialog && media != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Video Metadata") },
            text = {
                Text(
                    buildString {
                        appendLine("File: ${media.displayName}")
                        appendLine("Format: ${media.format} (${media.mimeType})")
                        appendLine("Resolution: ${media.resolutionLabel}")
                        appendLine("Duration: ${formatDurationShort(durationMs)}")
                        appendLine("Size: ${formatBytesReadable(media.fileSizeBytes)}")
                        append("Folder: ${media.relativePath}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Close") }
            },
        )
    }
}
