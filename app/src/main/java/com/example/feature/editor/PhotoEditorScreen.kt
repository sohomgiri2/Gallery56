package com.example.feature.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.core.datastore.SavedEditorFilterPreset
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.core.media.MediaStoreOperations
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private enum class EditorPanelTab(val label: String) {
    TRANSFORM("Transform"),
    LIGHT("Light & Tone"),
    DETAIL_HSL("Detail & HSL"),
    FILTERS("Filters & FX"),
    DESIGN("Text & Draw"),
}

private val paletteColors = listOf(
    Color.White,
    Color(0xFF0A0A0C),
    Color(0xFF5CE1E6),
    Color(0xFFFBBF24),
    Color(0xFFFB7185),
    Color(0xFF34D399),
    Color(0xFFC084FC),
    Color(0xFF60A5FA),
)

@Composable
fun PhotoEditorScreen(
    initialUriString: String?,
    customPresets: List<SavedEditorFilterPreset> = emptyList(),
    onSaveCustomPreset: (SavedEditorFilterPreset) -> Unit = {},
    onDeleteCustomPreset: (String) -> Unit = {},
    onBack: () -> Unit,
    onSavedToMediaStore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGallery56Tokens.current

    var sourceUri by remember { mutableStateOf(initialUriString) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var activeTab by remember { mutableStateOf(EditorPanelTab.LIGHT) }
    var showOriginalBefore by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Canvas zoom/pan + draw mode
    var canvasZoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var drawModeEnabled by remember { mutableStateOf(false) }
    var brushColor by remember { mutableStateOf(Color(0xFF5CE1E6)) }
    var brushEraser by remember { mutableStateOf(false) }
    var brushWidth by remember { mutableFloatStateOf(0.012f) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    val activeDrawPoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    // Export dialog & custom preset dialog
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(EditorExportFormat.JPEG) }
    var exportQuality by remember { mutableFloatStateOf(95f) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    // Text layer inputs
    var newTextDraft by remember { mutableStateOf("") }
    var newTextColor by remember { mutableStateOf(Color.White) }
    var newTextFont by remember { mutableStateOf(EditorTextFontStyle.SANS) }
    var newTextAlign by remember { mutableStateOf(EditorTextAlign.CENTER) }
    var newTextOutline by remember { mutableStateOf(false) }

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
            canvasZoom = 1f
            panX = 0f
            panY = 0f
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
                while (maxDim / sample > 1280) sample *= 2
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
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("photo_editor_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Gallery56TopBar(
            title = "PHOTO EDITOR 2.0",
            subtitle = if (drawModeEnabled) "Draw Mode Active" else "Non-destructive RAW-style pipeline",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.semantic.iconPrimary)
                }
            },
            primaryActions = {
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
                    onClick = {
                        if (canvasZoom > 1.05f) {
                            canvasZoom = 1f
                            panX = 0f
                            panY = 0f
                        } else {
                            canvasZoom = 2.0f
                        }
                    },
                    modifier = Modifier.testTag("editor_zoom_toggle"),
                ) {
                    Icon(
                        Icons.Rounded.ZoomIn,
                        contentDescription = if (canvasZoom > 1.05f) "Fit" else "100% Zoom",
                        tint = if (canvasZoom > 1.05f) MaterialTheme.colorScheme.primary else tokens.semantic.iconPrimary,
                    )
                }
                Button(
                    onClick = { showExportDialog = true },
                    enabled = previewBitmap != null && !isSaving,
                    modifier = Modifier.testTag("editor_save_copy_btn"),
                ) {
                    Icon(Icons.Rounded.Save, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isSaving) "..." else "Export", maxLines = 1, softWrap = false)
                }
            },
            overflowContent = { dismiss ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Compare Before/After") },
                    leadingIcon = { Icon(Icons.Rounded.Compare, null) },
                    onClick = {
                        dismiss()
                        showOriginalBefore = !showOriginalBefore
                    },
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Save as Custom Preset") },
                    leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, null) },
                    onClick = {
                        dismiss()
                        showSavePresetDialog = true
                    },
                )
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Reset All Edits") },
                    leadingIcon = { Icon(Icons.Rounded.RestartAlt, null) },
                    onClick = {
                        dismiss()
                        pushState(PhotoEditState())
                    },
                )
            },
        )

        // Preview Canvas with Zoom/Pan & Freehand Drawing
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .clipToBounds()
                .background(tokens.semantic.cardBackground)
                .onSizeChanged { canvasSize = it }
                .pointerInput(drawModeEnabled, brushColor, brushEraser, brushWidth, currentState) {
                    if (drawModeEnabled) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activeDrawPoints.clear()
                                val nx = (offset.x / canvasSize.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                val ny = (offset.y / canvasSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                activeDrawPoints.add(nx to ny)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val nx = (change.position.x / canvasSize.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                val ny = (change.position.y / canvasSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                activeDrawPoints.add(nx to ny)
                            },
                            onDragEnd = {
                                if (activeDrawPoints.size >= 2) {
                                    val newStroke = EditorBrushStroke(
                                        points = activeDrawPoints.toList(),
                                        colorInt = brushColor.toArgb(),
                                        widthFraction = brushWidth,
                                        isEraser = brushEraser,
                                    )
                                    pushState(currentState.copy(brushStrokes = currentState.brushStrokes + newStroke))
                                }
                                activeDrawPoints.clear()
                            },
                        )
                    } else {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (canvasZoom * zoom).coerceIn(1f, 4f)
                            canvasZoom = nextScale
                            if (nextScale > 1.02f) {
                                panX += pan.x
                                panY += pan.y
                            } else {
                                panX = 0f
                                panY = 0f
                            }
                        }
                    }
                }
                .pointerInput(drawModeEnabled) {
                    if (!drawModeEnabled) {
                        detectTapGestures(
                            onPress = {
                                showOriginalBefore = true
                                tryAwaitRelease()
                                showOriginalBefore = false
                            },
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val shown = if (showOriginalBefore) originalBitmap else previewBitmap
            if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = "Photo editor preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = canvasZoom
                            scaleY = canvasZoom
                            translationX = panX
                            translationY = panY
                        },
                )
                if (showOriginalBefore) {
                    GlassPanel(
                        contentPadding = 6.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(10.dp),
                    ) {
                        Text("ORIGINAL (HOLDING COMPARE)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose a photo to edit with Photo Editor 2.0", style = MaterialTheme.typography.bodyMedium)
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

        // Editor Panel Tabs
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
                    label = { Text(tab.label, maxLines = 1, softWrap = false) },
                )
            }
        }

        // Tool Controls Area (Scrollable so nothing clips)
        GlassPanel(
            contentPadding = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 235.dp),
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (activeTab) {
                    EditorPanelTab.TRANSFORM -> {
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
                                Text("90°")
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
                            ) { Text("Flip V") }
                        }
                        Text("Aspect Ratio", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CropAspectPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = currentState.cropPreset == preset,
                                    onClick = { pushState(currentState.copy(cropPreset = preset)) },
                                    label = { Text(preset.label) },
                                )
                            }
                        }
                        AdjustmentSlider("Straighten (${currentState.straightenDegrees.toInt()}°)", currentState.straightenDegrees, -25f..25f) {
                            pushState(currentState.copy(straightenDegrees = it))
                        }
                        AdjustmentSlider("Perspective H (${String.format("%.2f", currentState.perspectiveX)})", currentState.perspectiveX, -0.25f..0.25f) {
                            pushState(currentState.copy(perspectiveX = it))
                        }
                        AdjustmentSlider("Perspective V (${String.format("%.2f", currentState.perspectiveY)})", currentState.perspectiveY, -0.25f..0.25f) {
                            pushState(currentState.copy(perspectiveY = it))
                        }
                        AdjustmentSlider("Free Crop Left (${(currentState.freeCropLeft * 100).toInt()}%)", currentState.freeCropLeft, 0f..0.35f) {
                            pushState(currentState.copy(freeCropLeft = it))
                        }
                        AdjustmentSlider("Free Crop Right (${(currentState.freeCropRight * 100).toInt()}%)", currentState.freeCropRight, 0.65f..1f) {
                            pushState(currentState.copy(freeCropRight = it))
                        }
                        Text("Resize Output Scale", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1.0f to "100%", 0.75f to "75%", 0.5f to "50%", 0.25f to "25%").forEach { (scale, label) ->
                                FilterChip(
                                    selected = kotlin.math.abs(currentState.resizeScale - scale) < 0.02f,
                                    onClick = { pushState(currentState.copy(resizeScale = scale)) },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }

                    EditorPanelTab.LIGHT -> {
                        AdjustmentSlider("Exposure (${String.format("%.2f EV", currentState.exposure)})", currentState.exposure, -1.8f..1.8f) {
                            pushState(currentState.copy(exposure = it))
                        }
                        AdjustmentSlider("Brightness (${currentState.brightness.toInt()})", currentState.brightness, -80f..80f) {
                            pushState(currentState.copy(brightness = it))
                        }
                        AdjustmentSlider("Contrast (${String.format("%.2fx", currentState.contrast)})", currentState.contrast, 0.5f..1.8f) {
                            pushState(currentState.copy(contrast = it))
                        }
                        AdjustmentSlider("Highlights (${(currentState.highlights * 100).toInt()})", currentState.highlights, -1f..1f) {
                            pushState(currentState.copy(highlights = it))
                        }
                        AdjustmentSlider("Shadows (${(currentState.shadows * 100).toInt()})", currentState.shadows, -1f..1f) {
                            pushState(currentState.copy(shadows = it))
                        }
                        AdjustmentSlider("Whites (${(currentState.whites * 100).toInt()})", currentState.whites, -1f..1f) {
                            pushState(currentState.copy(whites = it))
                        }
                        AdjustmentSlider("Blacks (${(currentState.blacks * 100).toInt()})", currentState.blacks, -1f..1f) {
                            pushState(currentState.copy(blacks = it))
                        }
                        AdjustmentSlider("Fade (${(currentState.fade * 100).toInt()}%)", currentState.fade, 0f..1f) {
                            pushState(currentState.copy(fade = it))
                        }
                    }

                    EditorPanelTab.DETAIL_HSL -> {
                        AdjustmentSlider("Saturation (${String.format("%.2fx", currentState.saturation)})", currentState.saturation, 0f..2f) {
                            pushState(currentState.copy(saturation = it))
                        }
                        AdjustmentSlider("Vibrance (${(currentState.vibrance * 100).toInt()})", currentState.vibrance, -1f..1f) {
                            pushState(currentState.copy(vibrance = it))
                        }
                        AdjustmentSlider("Temperature (${currentState.temperature.toInt()})", currentState.temperature, -45f..45f) {
                            pushState(currentState.copy(temperature = it))
                        }
                        AdjustmentSlider("Tint (${currentState.tint.toInt()})", currentState.tint, -45f..45f) {
                            pushState(currentState.copy(tint = it))
                        }
                        AdjustmentSlider("HSL Hue Shift (${currentState.hueShiftDegrees.toInt()}°)", currentState.hueShiftDegrees, -180f..180f) {
                            pushState(currentState.copy(hueShiftDegrees = it))
                        }
                        AdjustmentSlider("Sharpness (${(currentState.sharpness * 100).toInt()}%)", currentState.sharpness, 0f..1f) {
                            pushState(currentState.copy(sharpness = it))
                        }
                        AdjustmentSlider("Clarity (${(currentState.clarity * 100).toInt()}%)", currentState.clarity, 0f..1f) {
                            pushState(currentState.copy(clarity = it))
                        }
                        AdjustmentSlider("Dehaze (${(currentState.dehaze * 100).toInt()}%)", currentState.dehaze, 0f..1f) {
                            pushState(currentState.copy(dehaze = it))
                        }
                        AdjustmentSlider("Denoise (${(currentState.denoise * 100).toInt()}%)", currentState.denoise, 0f..1f) {
                            pushState(currentState.copy(denoise = it))
                        }
                        AdjustmentSlider("Vignette (${(currentState.vignette * 100).toInt()}%)", currentState.vignette, 0f..1f) {
                            pushState(currentState.copy(vignette = it))
                        }
                        AdjustmentSlider("Film Grain (${(currentState.grain * 100).toInt()}%)", currentState.grain, 0f..1f) {
                            pushState(currentState.copy(grain = it))
                        }
                        AdjustmentSlider("Split-Tone (${(currentState.splitToneBalance * 100).toInt()})", currentState.splitToneBalance, -1f..1f) {
                            pushState(currentState.copy(splitToneBalance = it))
                        }
                    }

                    EditorPanelTab.FILTERS -> {
                        if (customPresets.isNotEmpty()) {
                            Text("Your Saved Presets", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                customPresets.forEach { cp ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                val f = EditorFilterPreset.entries.firstOrNull { it.name == cp.filterName }
                                                    ?: EditorFilterPreset.NONE
                                                pushState(
                                                    currentState.copy(
                                                        brightness = cp.brightness,
                                                        contrast = cp.contrast,
                                                        saturation = cp.saturation,
                                                        temperature = cp.temperature,
                                                        tint = cp.tint,
                                                        vibrance = cp.vibrance,
                                                        vignette = cp.vignette,
                                                        grain = cp.grain,
                                                        filter = f,
                                                    ),
                                                )
                                            },
                                            label = { Text(cp.name) },
                                        )
                                        IconButton(
                                            onClick = { onDeleteCustomPreset(cp.id) },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete preset", tint = StatusRose, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Text("Creative Filters (${EditorFilterPreset.entries.size})", style = MaterialTheme.typography.labelLarge)
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

                        Text("Tone Curves", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EditorCurvesPreset.entries.forEach { curve ->
                                FilterChip(
                                    selected = currentState.curvesPreset == curve,
                                    onClick = { pushState(currentState.copy(curvesPreset = curve)) },
                                    label = { Text(curve.label) },
                                )
                            }
                        }

                        Text("Duotone Split", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EditorDuotonePreset.entries.forEach { duo ->
                                FilterChip(
                                    selected = currentState.duotone == duo,
                                    onClick = { pushState(currentState.copy(duotone = duo)) },
                                    label = { Text(duo.label) },
                                )
                            }
                        }

                        AdjustmentSlider("Blur (${(currentState.blurAmount * 100).toInt()}%)", currentState.blurAmount, 0f..1f) {
                            pushState(currentState.copy(blurAmount = it))
                        }
                        AdjustmentSlider("Glow / Bloom (${(currentState.glowAmount * 100).toInt()}%)", currentState.glowAmount, 0f..1f) {
                            pushState(currentState.copy(glowAmount = it))
                        }

                        Text("Light Leaks & Gradient Overlay", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EditorLightLeakPreset.entries.forEach { leak ->
                                FilterChip(
                                    selected = currentState.lightLeak == leak,
                                    onClick = { pushState(currentState.copy(lightLeak = leak)) },
                                    label = { Text(leak.label) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EditorGradientOverlay.entries.forEach { grad ->
                                FilterChip(
                                    selected = currentState.gradientOverlay == grad,
                                    onClick = { pushState(currentState.copy(gradientOverlay = grad)) },
                                    label = { Text(grad.label) },
                                )
                            }
                        }

                        Text("Frames & Borders", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EditorFramePreset.entries.forEach { frame ->
                                FilterChip(
                                    selected = currentState.framePreset == frame,
                                    onClick = { pushState(currentState.copy(framePreset = frame)) },
                                    label = { Text(frame.label) },
                                )
                            }
                        }
                    }

                    EditorPanelTab.DESIGN -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Brush, null, tint = MaterialTheme.colorScheme.primary)
                                Text("Freehand Brush Draw Mode", style = MaterialTheme.typography.labelLarge)
                            }
                            Switch(checked = drawModeEnabled, onCheckedChange = { drawModeEnabled = it })
                        }
                        if (drawModeEnabled) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FilterChip(
                                    selected = brushEraser,
                                    onClick = { brushEraser = !brushEraser },
                                    label = { Text(if (brushEraser) "Eraser On" else "Brush") },
                                )
                                paletteColors.forEach { c ->
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(if (brushColor == c) 2.dp else 1.dp, Color.White, CircleShape)
                                            .clickable {
                                                brushEraser = false
                                                brushColor = c
                                            },
                                    )
                                }
                                if (currentState.brushStrokes.isNotEmpty()) {
                                    TextButton(onClick = {
                                        pushState(currentState.copy(brushStrokes = currentState.brushStrokes.dropLast(1)))
                                    }) { Text("Undo Stroke") }
                                }
                            }
                            AdjustmentSlider("Brush Size", brushWidth, 0.005f..0.04f) { brushWidth = it }
                        }

                        // Text Layers Creator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = newTextDraft,
                                onValueChange = { newTextDraft = it },
                                label = { Text("Add Text Layer") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                enabled = newTextDraft.isNotBlank(),
                                onClick = {
                                    val nextLayer = EditorTextLayer(
                                        id = UUID.randomUUID().toString().take(6),
                                        text = newTextDraft.trim(),
                                        colorInt = newTextColor.toArgb(),
                                        fontStyle = newTextFont,
                                        align = newTextAlign,
                                        hasOutline = newTextOutline,
                                        yFraction = (0.2f + (currentState.textLayers.size % 4) * 0.18f).coerceAtMost(0.88f),
                                    )
                                    pushState(currentState.copy(textLayers = currentState.textLayers + nextLayer))
                                    newTextDraft = ""
                                },
                            ) {
                                Icon(Icons.Rounded.Add, null, Modifier.size(16.dp))
                                Text("Add")
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            paletteColors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(if (newTextColor == c) 2.dp else 1.dp, Color.Gray, CircleShape)
                                        .clickable { newTextColor = c },
                                )
                            }
                            EditorTextFontStyle.entries.forEach { f ->
                                FilterChip(selected = newTextFont == f, onClick = { newTextFont = f }, label = { Text(f.label) })
                            }
                            FilterChip(selected = newTextOutline, onClick = { newTextOutline = !newTextOutline }, label = { Text("Outline") })
                        }
                        if (currentState.textLayers.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                currentState.textLayers.forEach { layer ->
                                    FilterChip(
                                        selected = true,
                                        onClick = {
                                            pushState(currentState.copy(textLayers = currentState.textLayers.filterNot { it.id == layer.id }))
                                        },
                                        label = { Text("× ${layer.text}") },
                                    )
                                }
                            }
                        }

                        Text("Stickers, Shapes & Watermark", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf("", "GALLERY56", "RAW EDITION", "SHOT ON ANDROID", "⭐", "🔥", "✨", "📸").forEach { stamp ->
                                FilterChip(
                                    selected = currentState.stickerText == stamp,
                                    onClick = { pushState(currentState.copy(stickerText = stamp)) },
                                    label = { Text(if (stamp.isEmpty()) "No Sticker" else stamp) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            EditorShapeOverlay.entries.forEach { shape ->
                                FilterChip(
                                    selected = currentState.shapeOverlay == shape,
                                    onClick = { pushState(currentState.copy(shapeOverlay = shape)) },
                                    label = { Text(shape.label) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = currentState.watermarkText,
                            onValueChange = { pushState(currentState.copy(watermarkText = it)) },
                            label = { Text("Signature Watermark (e.g. © Sohom Giri)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Edited Photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Format", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditorExportFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = exportFormat == fmt,
                                onClick = { exportFormat = fmt },
                                label = { Text(fmt.label) },
                            )
                        }
                    }
                    if (exportFormat != EditorExportFormat.PNG) {
                        Text("Quality (${exportQuality.toInt()}%)", style = MaterialTheme.typography.labelLarge)
                        Slider(value = exportQuality, onValueChange = { exportQuality = it }, valueRange = 60f..100f)
                    }
                    Text(
                        text = "Saves non-destructively to Pictures/Gallery56_Edited so your original file stays untouched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.semantic.textSecondary,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val srcUri = sourceUri ?: return@Button
                        showExportDialog = false
                        scope.launch {
                            isSaving = true
                            val compressFormat = when (exportFormat) {
                                EditorExportFormat.PNG -> Bitmap.CompressFormat.PNG
                                EditorExportFormat.WEBP -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        Bitmap.CompressFormat.WEBP_LOSSY
                                    } else {
                                        @Suppress("DEPRECATION")
                                        Bitmap.CompressFormat.WEBP
                                    }
                                }
                                EditorExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                            }
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
                                        format = compressFormat,
                                        quality = exportQuality.toInt().coerceIn(60, 100),
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
                                    "Exported ${exportFormat.label} to Pictures/Gallery56_Edited",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                ) { Text("Save Copy") }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Custom Preset") },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = presetNameInput.isNotBlank(),
                    onClick = {
                        onSaveCustomPreset(
                            SavedEditorFilterPreset(
                                id = "preset_${UUID.randomUUID().toString().take(8)}",
                                name = presetNameInput.trim(),
                                brightness = currentState.brightness,
                                contrast = currentState.contrast,
                                saturation = currentState.saturation,
                                temperature = currentState.temperature,
                                tint = currentState.tint,
                                vibrance = currentState.vibrance,
                                vignette = currentState.vignette,
                                grain = currentState.grain,
                                filterName = currentState.filter.name,
                            ),
                        )
                        presetNameInput = ""
                        showSavePresetDialog = false
                        Toast.makeText(context, "Preset saved", Toast.LENGTH_SHORT).show()
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancel") }
            },
        )
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
