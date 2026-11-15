package com.example.feature.collage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.media.MediaStoreOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class CollageLayoutTemplate(val label: String, val slotCount: Int) {
    SPLIT_VERTICAL("2 Split V", 2),
    SPLIT_HORIZONTAL("2 Split H", 2),
    EDITORIAL_3("3 Editorial", 3),
    GRID_4("4 Grid", 4),
    GRID_6("6 Grid", 6),
}

@Composable
fun CollageStudioScreen(
    allMedia: List<MediaIndexEntity>,
    onBack: () -> Unit,
    onSavedToMediaStore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var template by remember { mutableStateOf(CollageLayoutTemplate.GRID_4) }
    var spacingPx by remember { mutableFloatStateOf(24f) }
    var cornerRadiusPx by remember { mutableFloatStateOf(28f) }
    var bgColor by remember { mutableIntStateOf(0xFF07090D.toInt()) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var showLibraryPicker by remember { mutableStateOf(false) }
    var pickedFromLibrary by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6),
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris.map { it.toString() }
        }
    }

    LaunchedEffect(selectedUris, template, spacingPx, cornerRadiusPx, bgColor) {
        if (selectedUris.isEmpty()) {
            previewBitmap = null
            return@LaunchedEffect
        }
        previewBitmap = withContext(Dispatchers.Default) {
            renderCollageBitmap(
                context = context,
                uris = selectedUris,
                template = template,
                outputSize = 900,
                spacing = spacingPx / 2f,
                cornerRadius = cornerRadiusPx / 2f,
                backgroundColor = bgColor,
            )
        }
    }

    val bgOptions = listOf(
        0xFF07090D.toInt(),
        0xFFFFFFFF.toInt(),
        0xFF111C2D.toInt(),
        0xFF1D1636.toInt(),
        0xFFFAF6EE.toInt(),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("collage_studio_screen"),
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
                        Text("COLLAGE STUDIO", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Multi-frame grid & editorial composition",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    enabled = selectedUris.isNotEmpty() && !isExporting,
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val hiRes = withContext(Dispatchers.Default) {
                                renderCollageBitmap(
                                    context = context,
                                    uris = selectedUris,
                                    template = template,
                                    outputSize = 1800,
                                    spacing = spacingPx,
                                    cornerRadius = cornerRadiusPx,
                                    backgroundColor = bgColor,
                                )
                            }
                            val saved = hiRes?.let {
                                try {
                                    MediaStoreOperations.saveBitmapToMediaStore(
                                        context = context,
                                        bitmap = it,
                                        fileName = "Gallery56_Collage_${System.currentTimeMillis()}",
                                        subFolder = "Gallery56_Collages",
                                    )
                                } finally {
                                    it.recycle()
                                }
                            }
                            isExporting = false
                            if (saved != null) {
                                Toast.makeText(context, "Collage saved to Pictures/Gallery56_Collages", Toast.LENGTH_SHORT).show()
                                onSavedToMediaStore()
                            } else {
                                Toast.makeText(context, "Could not export collage", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("collage_export_button"),
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
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(bgColor)),
                    contentAlignment = Alignment.Center,
                ) {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Collage preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp),
                            )
                            Text(
                                text = "Select 2 to 6 photos to compose collage",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("collage_pick_photos"),
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Photo Picker")
                    }
                    OutlinedButton(
                        onClick = {
                            pickedFromLibrary = emptySet()
                            showLibraryPicker = true
                        },
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
                Text("Layout Template", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CollageLayoutTemplate.entries.forEach { t ->
                        FilterChip(
                            selected = template == t,
                            onClick = { template = t },
                            label = { Text(t.label) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Frame Spacing (${spacingPx.toInt()} px)", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = spacingPx,
                    onValueChange = { spacingPx = it },
                    valueRange = 0f..48f,
                )

                Text("Corner Radius (${cornerRadiusPx.toInt()} px)", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = cornerRadiusPx,
                    onValueChange = { cornerRadiusPx = it },
                    valueRange = 0f..56f,
                )

                Spacer(Modifier.height(6.dp))
                Text("Backdrop Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    bgOptions.forEach { colorInt ->
                        val active = bgColor == colorInt
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(
                                    width = if (active) 2.5.dp else 1.dp,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                )
                                .clickable { bgColor = colorInt },
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
            title = { Text("Select 2-6 Library Photos (${pickedFromLibrary.size}/6)") },
            text = {
                if (photos.isEmpty()) {
                    Text("No indexed photos available yet.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(photos, key = { it.mediaStoreId }) { item ->
                            val selected = item.mediaStoreId in pickedFromLibrary
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable {
                                        pickedFromLibrary = if (selected) {
                                            pickedFromLibrary - item.mediaStoreId
                                        } else if (pickedFromLibrary.size < 6) {
                                            pickedFromLibrary + item.mediaStoreId
                                        } else {
                                            pickedFromLibrary
                                        }
                                    },
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.contentUri)
                                        .size(200)
                                        .build(),
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pickedFromLibrary.isNotEmpty(),
                    onClick = {
                        selectedUris = photos
                            .filter { it.mediaStoreId in pickedFromLibrary }
                            .map { it.contentUri }
                        showLibraryPicker = false
                    },
                ) {
                    Text("Use Selected")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLibraryPicker = false }) { Text("Cancel") }
            },
        )
    }
}

private fun renderCollageBitmap(
    context: Context,
    uris: List<String>,
    template: CollageLayoutTemplate,
    outputSize: Int,
    spacing: Float,
    cornerRadius: Float,
    backgroundColor: Int,
): Bitmap? {
    if (uris.isEmpty()) return null
    val result = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawColor(backgroundColor)

    val s = spacing
    val w = outputSize.toFloat()
    val h = outputSize.toFloat()

    val rects: List<RectF> = when (template) {
        CollageLayoutTemplate.SPLIT_VERTICAL -> {
            val colW = (w - s * 3f) / 2f
            listOf(
                RectF(s, s, s + colW, h - s),
                RectF(s * 2f + colW, s, w - s, h - s),
            )
        }
        CollageLayoutTemplate.SPLIT_HORIZONTAL -> {
            val rowH = (h - s * 3f) / 2f
            listOf(
                RectF(s, s, w - s, s + rowH),
                RectF(s, s * 2f + rowH, w - s, h - s),
            )
        }
        CollageLayoutTemplate.EDITORIAL_3 -> {
            val leftW = (w - s * 3f) * 0.56f
            val rightLeft = s * 2f + leftW
            val rightH = (h - s * 3f) / 2f
            listOf(
                RectF(s, s, s + leftW, h - s),
                RectF(rightLeft, s, w - s, s + rightH),
                RectF(rightLeft, s * 2f + rightH, w - s, h - s),
            )
        }
        CollageLayoutTemplate.GRID_4 -> {
            val cw = (w - s * 3f) / 2f
            val ch = (h - s * 3f) / 2f
            listOf(
                RectF(s, s, s + cw, s + ch),
                RectF(s * 2f + cw, s, w - s, s + ch),
                RectF(s, s * 2f + ch, s + cw, h - s),
                RectF(s * 2f + cw, s * 2f + ch, w - s, h - s),
            )
        }
        CollageLayoutTemplate.GRID_6 -> {
            val cw = (w - s * 4f) / 3f
            val ch = (h - s * 3f) / 2f
            listOf(
                RectF(s, s, s + cw, s + ch),
                RectF(s * 2f + cw, s, s * 2f + cw * 2f, s + ch),
                RectF(s * 3f + cw * 2f, s, w - s, s + ch),
                RectF(s, s * 2f + ch, s + cw, h - s),
                RectF(s * 2f + cw, s * 2f + ch, s * 2f + cw * 2f, h - s),
                RectF(s * 3f + cw * 2f, s * 2f + ch, w - s, h - s),
            )
        }
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val maxTargetPx = (outputSize / 2).coerceAtLeast(512)
    rects.forEachIndexed { index, rect ->
        val uriStr = uris[index % uris.size]
        val src = decodeSampledBitmap(context, Uri.parse(uriStr), maxTargetPx) ?: return@forEachIndexed
        try {
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)

            val scale = max(rect.width() / src.width.toFloat(), rect.height() / src.height.toFloat())
            val drawW = src.width * scale
            val drawH = src.height * scale
            val left = rect.left + (rect.width() - drawW) / 2f
            val top = rect.top + (rect.height() - drawH) / 2f
            val dst = RectF(left, top, left + drawW, top + drawH)
            canvas.drawBitmap(src, null, dst, paint)
            canvas.restore()
        } finally {
            src.recycle()
        }
    }
    return result
}

internal fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
    } catch (_: Throwable) {
        null
    }
}
