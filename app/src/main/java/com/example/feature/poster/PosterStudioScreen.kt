package com.example.feature.poster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Save
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.media.MediaStoreOperations
import com.example.feature.collage.decodeSampledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class PosterTemplate(val label: String) {
    EXHIBITION("Exhibition"),
    MAGAZINE("Magazine"),
    MINIMAL_POLAROID("Polaroid"),
    CINEMA_FRAME("Cinema 21:9"),
    NEON_TELEMETRY("Telemetry"),
}

@Composable
fun PosterStudioScreen(
    allMedia: List<MediaIndexEntity>,
    onBack: () -> Unit,
    onSavedToMediaStore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<String?>(null) }
    var template by remember { mutableStateOf(PosterTemplate.EXHIBITION) }
    var titleText by remember { mutableStateOf("GALLERY56") }
    var subtitleText by remember { mutableStateOf("BEYOND IMAGINATION") }
    var stampText by remember { mutableStateOf("EDITION 01 • ARCHIVE") }
    var accentColorInt by remember { mutableIntStateOf(0xFF5CE1E6.toInt()) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var showLibraryPicker by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) selectedUri = uri.toString()
    }

    LaunchedEffect(selectedUri, template, titleText, subtitleText, stampText, accentColorInt) {
        val uriStr = selectedUri ?: return@LaunchedEffect
        previewBitmap = withContext(Dispatchers.Default) {
            renderPosterBitmap(
                context = context,
                sourceUri = Uri.parse(uriStr),
                template = template,
                title = titleText,
                subtitle = subtitleText,
                stamp = stampText,
                accentColor = accentColorInt,
                width = 600,
                height = 800,
            )
        }
    }

    val accents = listOf(
        0xFF5CE1E6.toInt(),
        0xFFF5C469.toInt(),
        0xFFFB7185.toInt(),
        0xFF34D399.toInt(),
        0xFFC084FC.toInt(),
        0xFFE2E8F0.toInt(),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("poster_studio_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("POSTER STUDIO", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Editorial typography & frame templates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    enabled = selectedUri != null && !isExporting,
                    onClick = {
                        val uriStr = selectedUri ?: return@Button
                        scope.launch {
                            isExporting = true
                            val hiRes = withContext(Dispatchers.Default) {
                                renderPosterBitmap(
                                    context = context,
                                    sourceUri = Uri.parse(uriStr),
                                    template = template,
                                    title = titleText,
                                    subtitle = subtitleText,
                                    stamp = stampText,
                                    accentColor = accentColorInt,
                                    width = 1200,
                                    height = 1600,
                                )
                            }
                            val saved = hiRes?.let {
                                try {
                                    MediaStoreOperations.saveBitmapToMediaStore(
                                        context = context,
                                        bitmap = it,
                                        fileName = "Gallery56_Poster_${System.currentTimeMillis()}",
                                        subFolder = "Gallery56_Posters",
                                    )
                                } finally {
                                    it.recycle()
                                }
                            }
                            isExporting = false
                            if (saved != null) {
                                Toast.makeText(context, "Poster saved to Pictures/Gallery56_Posters", Toast.LENGTH_SHORT).show()
                                onSavedToMediaStore()
                            }
                        }
                    },
                    modifier = Modifier.testTag("poster_export_button"),
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isExporting) "Saving..." else "Export")
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Poster preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesomeMosaic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp),
                            )
                            Text("Choose a photo to design your poster")
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Photo Picker")
                    }
                    OutlinedButton(
                        onClick = { showLibraryPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("From Library")
                    }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Poster Template", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PosterTemplate.entries.forEach { t ->
                        FilterChip(
                            selected = template == t,
                            onClick = { template = t },
                            label = { Text(t.label) },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = subtitleText,
                    onValueChange = { subtitleText = it },
                    label = { Text("Subtitle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = stampText,
                    onValueChange = { stampText = it },
                    label = { Text("Edition Stamp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text("Accent Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    accents.forEach { c ->
                        val active = accentColorInt == c
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(c))
                                .border(
                                    width = if (active) 2.5.dp else 1.dp,
                                    color = if (active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                )
                                .clickable { accentColorInt = c },
                        )
                    }
                }
            }
        }
    }

    if (showLibraryPicker) {
        val photos = remember(allMedia) { allMedia.filter { !it.isVideo }.take(80) }
        AlertDialog(
            onDismissRequest = { showLibraryPicker = false },
            title = { Text("Select Photo") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(photos, key = { it.mediaStoreId }) { item ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.contentUri)
                                .size(200)
                                .build(),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedUri = item.contentUri
                                    showLibraryPicker = false
                                },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLibraryPicker = false }) { Text("Close") }
            },
        )
    }
}

private fun renderPosterBitmap(
    context: Context,
    sourceUri: Uri,
    template: PosterTemplate,
    title: String,
    subtitle: String,
    stamp: String,
    accentColor: Int,
    width: Int,
    height: Int,
): Bitmap? {
    val photo = decodeSampledBitmap(context, sourceUri, width) ?: return null
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val scale = width / 600f
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    try {
        when (template) {
            PosterTemplate.EXHIBITION -> {
                canvas.drawColor(Color.parseColor("#0B0E14"))
                val pad = 36f * scale
                val photoRect = RectF(pad, pad * 1.4f, width - pad, height * 0.72f)
                drawCenterCropped(canvas, photo, photoRect, 14f * scale, bitmapPaint)

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 38f * scale
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = 16f * scale
                    letterSpacing = 0.12f
                }
                val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#94A0B2")
                    textSize = 13f * scale
                    typeface = Typeface.MONOSPACE
                }
                canvas.drawText(title.uppercase(), pad, height * 0.81f, titlePaint)
                canvas.drawText(subtitle.uppercase(), pad, height * 0.86f, subPaint)
                canvas.drawText(stamp.uppercase(), pad, height * 0.93f, stampPaint)
            }
            PosterTemplate.MAGAZINE -> {
                drawCenterCropped(canvas, photo, RectF(0f, 0f, width.toFloat(), height.toFloat()), 0f, bitmapPaint)
                val gradientPaint = Paint().apply {
                    shader = LinearGradient(
                        0f, height * 0.45f, 0f, height.toFloat(),
                        Color.TRANSPARENT, Color.argb(225, 6, 8, 12),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, height * 0.45f, width.toFloat(), height.toFloat(), gradientPaint)
                val masthead = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = 54f * scale
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(title.uppercase(), width / 2f, 82f * scale, masthead)

                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 20f * scale
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.LTGRAY
                    textSize = 13f * scale
                }
                canvas.drawText(subtitle, 32f * scale, height - 64f * scale, subPaint)
                canvas.drawText(stamp, 32f * scale, height - 34f * scale, stampPaint)
            }
            PosterTemplate.MINIMAL_POLAROID -> {
                canvas.drawColor(Color.parseColor("#F5F2EB"))
                val pad = 38f * scale
                val photoRect = RectF(pad, pad, width - pad, height * 0.74f)
                drawCenterCropped(canvas, photo, photoRect, 4f * scale, bitmapPaint)
                val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#18181B")
                    textSize = 28f * scale
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                val sPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#52525B")
                    textSize = 14f * scale
                    typeface = Typeface.MONOSPACE
                }
                canvas.drawText(title, pad, height * 0.83f, tPaint)
                canvas.drawText("$subtitle  •  $stamp", pad, height * 0.89f, sPaint)
            }
            PosterTemplate.CINEMA_FRAME -> {
                canvas.drawColor(Color.BLACK)
                val photoRect = RectF(0f, height * 0.22f, width.toFloat(), height * 0.68f)
                drawCenterCropped(canvas, photo, photoRect, 0f, bitmapPaint)
                val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 30f * scale
                    letterSpacing = 0.2f
                    textAlign = Paint.Align.CENTER
                }
                val sPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = 14f * scale
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(title.uppercase(), width / 2f, height * 0.79f, tPaint)
                canvas.drawText("$subtitle  //  $stamp", width / 2f, height * 0.85f, sPaint)
            }
            PosterTemplate.NEON_TELEMETRY -> {
                canvas.drawColor(Color.parseColor("#06080C"))
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    style = Paint.Style.STROKE
                    strokeWidth = 3f * scale
                }
                val pad = 28f * scale
                val photoRect = RectF(pad, pad * 2f, width - pad, height * 0.72f)
                drawCenterCropped(canvas, photo, photoRect, 18f * scale, bitmapPaint)
                canvas.drawRoundRect(photoRect, 18f * scale, 18f * scale, borderPaint)

                val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    textSize = 32f * scale
                    typeface = Typeface.MONOSPACE
                }
                val sPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 15f * scale
                    typeface = Typeface.MONOSPACE
                }
                canvas.drawText(title.uppercase(), pad, height * 0.81f, tPaint)
                canvas.drawText(subtitle.uppercase(), pad, height * 0.86f, sPaint)
                canvas.drawText("[ $stamp ]", pad, height * 0.92f, sPaint)
            }
        }
    } finally {
        photo.recycle()
    }
    return output
}

private fun drawCenterCropped(
    canvas: Canvas,
    src: Bitmap,
    rect: RectF,
    cornerRadius: Float,
    paint: Paint,
) {
    canvas.save()
    if (cornerRadius > 0f) {
        val path = Path().apply {
            addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(path)
    } else {
        canvas.clipRect(rect)
    }
    val scale = max(rect.width() / src.width.toFloat(), rect.height() / src.height.toFloat())
    val drawW = src.width * scale
    val drawH = src.height * scale
    val left = rect.left + (rect.width() - drawW) / 2f
    val top = rect.top + (rect.height() - drawH) / 2f
    canvas.drawBitmap(src, null, RectF(left, top, left + drawW, top + drawH), paint)
    canvas.restore()
}
