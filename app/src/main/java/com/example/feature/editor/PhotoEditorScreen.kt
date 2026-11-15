package com.example.feature.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.GlassPanel
import com.example.core.media.MediaStoreOperations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class EditorPanelTab(val label: String) {
    CROP_ROTATE("Crop & Rotate"),
    ADJUST("Adjust"),
    FILTERS("Filters"),
    TEXT_STICKER("Text & Stamp"),
}

@Composable
fun PhotoEditorScreen(
    initialUriString: String?,
    onBack: () -> Unit,
    onSavedToMediaStore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf(initialUriString) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeTab by remember { mutableStateOf(EditorPanelTab.ADJUST) }
    var showOriginalBefore by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val history = remember { mutableStateListOf(PhotoEditState()) }
    var historyIdx by remember { mutableStateOf(0) }
    val currentState = history[historyIdx]

    fun pushState(next: PhotoEditState) {
        if (next == currentState) return
        while (history.size > historyIdx + 1) {
            history.removeAt(history.lastIndex)
        }
        history.add(next)
        historyIdx = history.lastIndex
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            sourceUri = uri.toString()
            history.clear()
            history.add(PhotoEditState())
            historyIdx = 0
        }
    }

    LaunchedEffect(sourceUri) {
        val uriStr = sourceUri ?: return@LaunchedEffect
        originalBitmap = withContext(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
                val maxDim = maxOf(opts.outWidth, opts.outHeight)
                var sample = 1
                while (maxDim / sample > 1440) sample *= 2
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    LaunchedEffect(originalBitmap, currentState) {
        val src = originalBitmap ?: return@LaunchedEffect
        previewBitmap = withContext(Dispatchers.Default) {
            PhotoEditorEngine.renderEditedBitmap(src, currentState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("photo_editor_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text("PHOTO EDITOR", style = MaterialTheme.typography.titleLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (historyIdx > 0) historyIdx-- },
                    enabled = historyIdx > 0,
                    modifier = Modifier.testTag("editor_undo_btn"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo")
                }
                IconButton(
                    onClick = { if (historyIdx < history.lastIndex) historyIdx++ },
                    enabled = historyIdx < history.lastIndex,
                    modifier = Modifier.testTag("editor_redo_btn"),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Redo")
                }
                IconButton(
                    onClick = { showOriginalBefore = !showOriginalBefore },
                    modifier = Modifier.testTag("editor_compare_btn"),
                ) {
                    Icon(
                        Icons.Rounded.Compare,
                        contentDescription = "Before/After comparison",
                        tint = if (showOriginalBefore) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Button(
                    onClick = {
                        val srcUri = sourceUri ?: return@Button
                        scope.launch {
                            isSaving = true
                            val savedUri = withContext(Dispatchers.IO) {
                                val fullBitmap = context.contentResolver.openInputStream(Uri.parse(srcUri))?.use {
                                    BitmapFactory.decodeStream(it)
                                } ?: previewBitmap
                                if (fullBitmap != null) {
                                    val rendered = PhotoEditorEngine.renderEditedBitmap(fullBitmap, currentState)
                                    val uri = MediaStoreOperations.saveBitmapToMediaStore(
                                        context = context,
                                        bitmap = rendered,
                                        fileName = "G56_Edit_${System.currentTimeMillis()}",
                                        subFolder = "Gallery56_Edited",
                                    )
                                    if (rendered !== fullBitmap) rendered.recycle()
                                    if (fullBitmap !== originalBitmap) fullBitmap.recycle()
                                    uri
                                } else null
                            }
                            isSaving = false
                            if (savedUri != null) {
                                onSavedToMediaStore()
                                Toast.makeText(
                                    context,
                                    "Saved non-destructive copy to Pictures/Gallery56_Edited",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = previewBitmap != null && !isSaving,
                    modifier = Modifier.testTag("editor_save_copy_btn"),
                ) {
                    Icon(Icons.Rounded.Save, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isSaving) "Saving" else "Save")
                }
            }
        }

        // Preview Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            showOriginalBefore = true
                            tryAwaitRelease()
                            showOriginalBefore = false
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val shown = if (showOriginalBefore) originalBitmap else previewBitmap
            if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = "Photo editor preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                if (showOriginalBefore) {
                    GlassPanel(
                        contentPadding = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(10.dp),
                    ) {
                        Text("ORIGINAL (BEFORE)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose a photo to edit non-destructively", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Photo")
                    }
                }
            }
        }

        // Panel Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EditorPanelTab.entries.forEach { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = { Text(tab.label) },
                )
            }
        }

        // Tool Controls
        GlassPanel(contentPadding = 12.dp, modifier = Modifier.fillMaxWidth()) {
            when (activeTab) {
                EditorPanelTab.CROP_ROTATE -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                pushState(currentState.copy(rotationDegrees = (currentState.rotationDegrees + 90) % 360))
                            },
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.RotateRight, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Rotate 90°")
                        }
                        OutlinedButton(
                            onClick = { pushState(currentState.copy(flipHorizontal = !currentState.flipHorizontal)) },
                        ) {
                            Icon(Icons.Rounded.Flip, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Flip H")
                        }
                        OutlinedButton(
                            onClick = { pushState(currentState.copy(flipVertical = !currentState.flipVertical)) },
                        ) {
                            Text("Flip V")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CropAspectPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = currentState.cropPreset == preset,
                                onClick = { pushState(currentState.copy(cropPreset = preset)) },
                                label = { Text(preset.label) },
                            )
                        }
                    }
                }

                EditorPanelTab.ADJUST -> {
                    AdjustmentSlider(
                        label = "Brightness (${currentState.brightness.toInt()})",
                        value = currentState.brightness,
                        range = -80f..80f,
                        onChange = { pushState(currentState.copy(brightness = it)) },
                    )
                    AdjustmentSlider(
                        label = "Contrast (${String.format("%.2f", currentState.contrast)}x)",
                        value = currentState.contrast,
                        range = 0.5f..1.8f,
                        onChange = { pushState(currentState.copy(contrast = it)) },
                    )
                    AdjustmentSlider(
                        label = "Saturation (${String.format("%.2f", currentState.saturation)}x)",
                        value = currentState.saturation,
                        range = 0f..2f,
                        onChange = { pushState(currentState.copy(saturation = it)) },
                    )
                    AdjustmentSlider(
                        label = "Temperature (${currentState.temperature.toInt()})",
                        value = currentState.temperature,
                        range = -40f..40f,
                        onChange = { pushState(currentState.copy(temperature = it)) },
                    )
                }

                EditorPanelTab.FILTERS -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        EditorFilterPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = currentState.filter == preset,
                                onClick = { pushState(currentState.copy(filter = preset)) },
                                label = { Text(preset.label) },
                            )
                        }
                    }
                }

                EditorPanelTab.TEXT_STICKER -> {
                    OutlinedTextField(
                        value = currentState.overlayText,
                        onValueChange = { pushState(currentState.copy(overlayText = it)) },
                        label = { Text("Overlay Caption Text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("", "GALLERY56", "RAW EDITION", "SHOT ON ANDROID", "2026").forEach { stamp ->
                            FilterChip(
                                selected = currentState.stickerText == stamp,
                                onClick = { pushState(currentState.copy(stickerText = stamp)) },
                                label = { Text(if (stamp.isEmpty()) "No Stamp" else stamp) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
