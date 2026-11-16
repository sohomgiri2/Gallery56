package com.example.feature.studio

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.ui.theme.LocalGallery56Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class WallpaperTarget(val label: String) {
    HOME("Home Screen"),
    LOCK("Lock Screen"),
    BOTH("Home & Lock"),
}

@Composable
fun WallpaperStudioScreen(
    allMedia: List<MediaIndexEntity>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGallery56Tokens.current

    val photos = remember(allMedia) { allMedia.filter { !it.isVideo }.take(40) }
    var selectedUri by remember { mutableStateOf(photos.firstOrNull()?.contentUri) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dimAmount by remember { mutableFloatStateOf(0.15f) }
    var blurAmount by remember { mutableFloatStateOf(0f) }
    var showClockPreview by remember { mutableStateOf(true) }
    var target by remember { mutableStateOf(WallpaperTarget.BOTH) }
    var isApplying by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) selectedUri = uri.toString()
    }

    LaunchedEffect(selectedUri, dimAmount, blurAmount) {
        val uriStr = selectedUri ?: return@LaunchedEffect
        previewBitmap = withContext(Dispatchers.IO) {
            renderWallpaperBitmap(context, Uri.parse(uriStr), dimAmount, blurAmount, maxDim = 1080)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wallpaper_studio_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Gallery56TopBar(
                title = "WALLPAPER STUDIO",
                subtitle = "Lock & Home framing, ambient dim & blur",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.semantic.iconPrimary)
                    }
                },
            )
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Wallpaper preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            "Pick a photo below to preview wallpaper",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }

                    if (showClockPreview && bmp != null) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "09:56",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Text(
                                text = "GALLERY56 LOCK PREVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Photo")
                    }
                    Button(
                        enabled = selectedUri != null && !isApplying,
                        onClick = {
                            val uriStr = selectedUri ?: return@Button
                            scope.launch {
                                isApplying = true
                                val ok = withContext(Dispatchers.IO) {
                                    val full = renderWallpaperBitmap(context, Uri.parse(uriStr), dimAmount, blurAmount, maxDim = 2160)
                                    if (full != null) {
                                        try {
                                            val wm = WallpaperManager.getInstance(context)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                val flags = when (target) {
                                                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                                                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                                                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                                }
                                                wm.setBitmap(full, null, true, flags)
                                            } else {
                                                wm.setBitmap(full)
                                            }
                                            true
                                        } catch (_: Throwable) {
                                            false
                                        } finally {
                                            full.recycle()
                                        }
                                    } else false
                                }
                                isApplying = false
                                Toast.makeText(
                                    context,
                                    if (ok) "Applied to ${target.label}" else "Unable to set wallpaper",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Wallpaper, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isApplying) "Applying..." else "Set Wallpaper")
                    }
                }
            }
        }

        if (photos.isNotEmpty()) {
            item {
                Text("Quick Library Picks", style = MaterialTheme.typography.titleSmall, color = tokens.semantic.textPrimary)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos, key = { it.mediaStoreId }) { item ->
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(item.contentUri).size(180).build(),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedUri = item.contentUri },
                        )
                    }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Target & Adjustments", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WallpaperTarget.entries.forEach { t ->
                        FilterChip(
                            selected = target == t,
                            onClick = { target = t },
                            label = { Text(t.label, maxLines = 1, softWrap = false) },
                            leadingIcon = {
                                Icon(
                                    if (t == WallpaperTarget.LOCK) Icons.Rounded.Lock else Icons.Rounded.Home,
                                    null,
                                    Modifier.size(14.dp),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Dim Overlay (${(dimAmount * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge)
                Slider(value = dimAmount, onValueChange = { dimAmount = it }, valueRange = 0f..0.75f)
                Text("Ambient Blur (${(blurAmount * 100).toInt()}%)", style = MaterialTheme.typography.labelLarge)
                Slider(value = blurAmount, onValueChange = { blurAmount = it }, valueRange = 0f..1f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Show Lock Screen Clock Mockup", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = showClockPreview, onCheckedChange = { showClockPreview = it })
                }
            }
        }
    }
}

private fun renderWallpaperBitmap(
    context: android.content.Context,
    uri: Uri,
    dimAmount: Float,
    blurAmount: Float,
    maxDim: Int,
): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (maxOf(opts.outWidth, opts.outHeight) / sample > maxDim) sample *= 2
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        var working = raw.copy(Bitmap.Config.ARGB_8888, true)
        raw.recycle()

        if (blurAmount > 0.04f) {
            val scale = (1f - blurAmount * 0.85f).coerceIn(0.08f, 0.9f)
            val sw = (working.width * scale).roundToInt().coerceAtLeast(8)
            val sh = (working.height * scale).roundToInt().coerceAtLeast(8)
            val small = Bitmap.createScaledBitmap(working, sw, sh, true)
            val up = Bitmap.createScaledBitmap(small, working.width, working.height, true)
            small.recycle()
            working.recycle()
            working = up
        }

        if (dimAmount > 0.01f) {
            val c = Canvas(working)
            val factor = (1f - dimAmount.coerceIn(0f, 0.85f))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(
                    ColorMatrix().apply { setScale(factor, factor, factor, 1f) },
                )
            }
            val copy = working.copy(Bitmap.Config.ARGB_8888, false)
            c.drawBitmap(copy, 0f, 0f, paint)
            copy.recycle()
        }
        working
    } catch (_: Throwable) {
        null
    }
}
