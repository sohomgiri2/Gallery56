package com.example.feature.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.datastore.AnimationIntensity
import com.example.core.datastore.GalleryLayoutId
import com.example.core.datastore.GalleryPreferences
import com.example.core.datastore.GalleryPreferencesManager
import com.example.core.datastore.NavigationStyle
import com.example.core.datastore.ViewerBackgroundMode
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.ui.theme.CustomThemeProfile
import com.example.ui.theme.GalleryThemePreset
import com.example.ui.theme.GalleryTypographyStyle
import com.example.ui.theme.IconStylePreference
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusRose
import com.example.ui.theme.ThumbnailShapeStyle
import com.example.ui.theme.parseHexColorOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizationStudioScreen(
    preferences: GalleryPreferences,
    preferencesManager: GalleryPreferencesManager,
    onToggleSemanticDefault: (Boolean) -> Unit,
    onOpenLayoutStudio: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGallery56Tokens.current
    val json = remember { Json { ignoreUnknownKeys = true; prettyPrint = true } }

    var primaryHexInput by remember(preferences.customAccentHexOverride) { mutableStateOf(preferences.customAccentHexOverride) }
    var secondaryHexInput by remember(preferences.secondaryHexOverride) { mutableStateOf(preferences.secondaryHexOverride) }
    var bgHexInput by remember(preferences.backgroundHexOverride) { mutableStateOf(preferences.backgroundHexOverride) }
    var cardHexInput by remember(preferences.cardHexOverride) { mutableStateOf(preferences.cardHexOverride) }
    var borderHexInput by remember(preferences.borderHexOverride) { mutableStateOf(preferences.borderHexOverride) }
    var textHexInput by remember(preferences.textHexOverride) { mutableStateOf(preferences.textHexOverride) }

    var editingCustomTheme by remember { mutableStateOf<CustomThemeProfile?>(null) }
    var showCreateCustomTheme by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }

    val quickAccents = listOf(
        "#5CE1E6", "#00F5D4", "#60A5FA", "#C084FC",
        "#F5C469", "#FB7185", "#34D399", "#FF3B30",
    )

    val activeLayout = remember(preferences.galleryLayoutId) {
        GalleryLayoutId.fromId(preferences.galleryLayoutId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customization_studio_screen"),
    ) {
        Gallery56TopBar(
            title = "PERSONALIZATION LAB",
            subtitle = "${GalleryThemePreset.entries.size} themes • 14 UI layouts • 30+ live controls",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.semantic.iconPrimary)
                }
            },
            primaryActions = {
                IconButton(
                    onClick = {
                        scope.launch {
                            preferencesManager.resetAppearanceDefaults()
                            Toast.makeText(context, "Restored default appearance", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset studio defaults", tint = tokens.semantic.iconPrimary)
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Gallery Layout Studio Shortcut
            item {
                GlassPanel(
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Gallery Interface",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = activeLayout.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = tokens.semantic.textPrimary,
                            )
                            Text(
                                text = activeLayout.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.semantic.textSecondary,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onOpenLayoutStudio,
                            modifier = Modifier.testTag("open_layout_studio_from_lab"),
                        ) {
                            Icon(Icons.Rounded.Tune, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("14 Layouts")
                        }
                    }
                }
            }

            // 2. All 22 Preset Themes
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Preset Themes (${GalleryThemePreset.entries.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { showImportJsonDialog = true }) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = "Import Theme JSON")
                            }
                            OutlinedButton(
                                onClick = {
                                    editingCustomTheme = null
                                    showCreateCustomTheme = true
                                },
                                modifier = Modifier.testTag("studio_new_custom_theme"),
                            ) {
                                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Build")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GalleryThemePreset.entries.forEach { preset ->
                            val active = preferences.activeCustomThemeId.isBlank() && preferences.themeId == preset.id
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(preset.surface)
                                    .border(
                                        width = if (active) 2.dp else 1.dp,
                                        color = if (active) preset.primary else preset.cardBorder,
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable {
                                        scope.launch { preferencesManager.setThemeId(preset.id) }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("theme_chip_${preset.id}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(preset.primary),
                                )
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = preset.textPrimary,
                                )
                                if (active) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Active",
                                        tint = preset.primary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Saved Custom Themes (Activate, Rename/Edit, Duplicate, Export JSON, Delete)
            if (preferences.customThemes.isNotEmpty()) {
                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Text("Custom Theme Profiles", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        preferences.customThemes.forEach { profile ->
                            val active = preferences.activeCustomThemeId == profile.id
                            val primaryColor = parseHexColorOrNull(profile.primaryHex) ?: MaterialTheme.colorScheme.primary
                            val surfaceColor = parseHexColorOrNull(profile.surfaceHex) ?: MaterialTheme.colorScheme.surface
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(surfaceColor)
                                    .border(
                                        width = if (active) 2.dp else 1.dp,
                                        color = if (active) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { scope.launch { preferencesManager.activateCustomTheme(profile.id) } }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor),
                                    )
                                    Column {
                                        Text(profile.name, style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            "${profile.primaryHex} • ${profile.backgroundHex}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = tokens.semantic.textSecondary,
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingCustomTheme = profile
                                            showCreateCustomTheme = true
                                        },
                                    ) {
                                        Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = "Edit custom theme")
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val copy = profile.copy(
                                                    id = "custom_${UUID.randomUUID().toString().take(8)}",
                                                    name = "${profile.name} Copy",
                                                )
                                                preferencesManager.saveCustomTheme(copy, activate = true)
                                            }
                                        },
                                    ) {
                                        Icon(Icons.Rounded.FileCopy, contentDescription = "Duplicate custom theme")
                                    }
                                    IconButton(
                                        onClick = {
                                            val encoded = json.encodeToString(profile)
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("Gallery56 Theme JSON", encoded))
                                            Toast.makeText(context, "Theme JSON copied", Toast.LENGTH_SHORT).show()
                                        },
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Export JSON")
                                    }
                                    IconButton(onClick = { scope.launch { preferencesManager.deleteCustomTheme(profile.id) } }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete custom theme", tint = StatusRose)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Full HEX Color Override Studio (6 channels)
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("Live HEX Color Overrides", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Override primary, secondary, background, card, border, or text with contrast safety.",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.semantic.textSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        quickAccents.forEach { hex ->
                            val color = parseHexColorOrNull(hex) ?: Color.Cyan
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (preferences.customAccentHexOverride.equals(hex, ignoreCase = true)) 2.5.dp else 1.dp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        primaryHexInput = hex
                                        scope.launch { preferencesManager.setColorOverrides(primaryHex = hex) }
                                    },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HexOverrideRow("Primary Accent HEX", primaryHexInput, { primaryHexInput = it }) {
                        scope.launch { preferencesManager.setColorOverrides(primaryHex = it) }
                    }
                    HexOverrideRow("Secondary Accent HEX", secondaryHexInput, { secondaryHexInput = it }) {
                        scope.launch { preferencesManager.setColorOverrides(secondaryHex = it) }
                    }
                    HexOverrideRow("Background HEX", bgHexInput, { bgHexInput = it }) {
                        scope.launch { preferencesManager.setColorOverrides(backgroundHex = it) }
                    }
                    HexOverrideRow("Card Surface HEX", cardHexInput, { cardHexInput = it }) {
                        scope.launch { preferencesManager.setColorOverrides(cardHex = it) }
                    }
                    HexOverrideRow("Border Stroke HEX", borderHexInput, { borderHexInput = it }) {
                        scope.launch { preferencesManager.setColorOverrides(borderHex = it) }
                    }
                    HexOverrideRow("Primary Text HEX", textHexInput, { textHexInput = it }) {
                        scope.launch { preferencesManager.setColorOverrides(textHex = it) }
                    }
                    OutlinedButton(
                        onClick = {
                            primaryHexInput = ""
                            secondaryHexInput = ""
                            bgHexInput = ""
                            cardHexInput = ""
                            borderHexInput = ""
                            textHexInput = ""
                            scope.launch {
                                preferencesManager.setColorOverrides("", "", "", "", "", "")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Clear All HEX Overrides") }
                }
            }

            // 5. Navigation Bar & Icon Style
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("Navigation & Iconography", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Bottom Navigation Style (5 presets)", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NavigationStyle.entries.forEach { style ->
                            FilterChip(
                                selected = preferences.navigationStyle == style,
                                onClick = { scope.launch { preferencesManager.setNavigationStyle(style) } },
                                label = { Text(style.label) },
                            )
                        }
                    }
                    StudioSwitchRow(
                        title = "Show Navigation Labels",
                        subtitle = "Display tab captions beneath bottom navigation icons",
                        checked = preferences.showNavigationLabels,
                        onCheckedChange = { scope.launch { preferencesManager.setShowNavigationLabels(it) } },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Icon Style", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconStylePreference.entries.forEach { style ->
                            FilterChip(
                                selected = preferences.iconStyle == style,
                                onClick = { scope.launch { preferencesManager.setIconStyle(style) } },
                                label = { Text(style.label) },
                            )
                        }
                    }
                }
            }

            // 6. Grid Geometry & Thumbnails
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("Grid & Thumbnail Geometry", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Grid Columns (${preferences.gridColumns})", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (2..6).forEach { cols ->
                            FilterChip(
                                selected = preferences.gridColumns == cols,
                                onClick = { scope.launch { preferencesManager.setGridColumns(cols) } },
                                label = { Text("$cols Col") },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Thumbnail Shape", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThumbnailShapeStyle.entries.forEach { shape ->
                            FilterChip(
                                selected = preferences.thumbnailShapeStyle == shape,
                                onClick = { scope.launch { preferencesManager.setThumbnailShapeStyle(shape) } },
                                label = { Text(shape.label) },
                            )
                        }
                    }
                    Text("Corner Radius (${preferences.cornerRadiusDp} dp)", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = preferences.cornerRadiusDp.toFloat(),
                        onValueChange = { scope.launch { preferencesManager.setCornerRadiusDp(it.roundToInt()) } },
                        valueRange = 0f..28f,
                    )
                    Text("Grid Spacing (${preferences.gridSpacingDp} dp)", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = preferences.gridSpacingDp.toFloat(),
                        onValueChange = { scope.launch { preferencesManager.setGridSpacingDp(it.roundToInt()) } },
                        valueRange = 2f..20f,
                    )
                    Text("Thumbnail Aspect Scale (${String.format("%.2f", preferences.thumbnailSizeScale)}x)", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = preferences.thumbnailSizeScale,
                        onValueChange = { scope.launch { preferencesManager.setThumbnailSizeScale(it) } },
                        valueRange = 0.8f..1.4f,
                    )
                    StudioSwitchRow(
                        title = "Date Section Grouping",
                        subtitle = "Show chronological date subheaders inside grids",
                        checked = preferences.showDateGrouping,
                        onCheckedChange = { scope.launch { preferencesManager.setShowDateGrouping(it) } },
                    )
                    StudioSwitchRow(
                        title = "Large Album Showcase Cards",
                        subtitle = "Display single-column widescreen album feature cards",
                        checked = preferences.albumCardLarge,
                        onCheckedChange = { scope.launch { preferencesManager.setAlbumCardLarge(it) } },
                    )
                }
            }

            // 7. Typography, Glass & Motion
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("Typography, Glass & Motion", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Typography Style", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GalleryTypographyStyle.entries.forEach { tStyle ->
                            FilterChip(
                                selected = preferences.typographyStyle == tStyle,
                                onClick = { scope.launch { preferencesManager.setTypographyStyle(tStyle) } },
                                label = { Text(tStyle.label) },
                            )
                        }
                    }
                    Text("Font Scale (${String.format("%.2f", preferences.fontScale)}x)", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = preferences.fontScale,
                        onValueChange = { scope.launch { preferencesManager.setFontScale(it) } },
                        valueRange = 0.85f..1.25f,
                    )
                    Text("Glass Blur / Translucent Opacity (${(preferences.blurIntensity * 100).roundToInt()}%)", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = preferences.blurIntensity,
                        onValueChange = { scope.launch { preferencesManager.setBlurIntensity(it) } },
                        valueRange = 0f..1f,
                    )
                    Text("Card Elevation (${preferences.cardElevationDp} dp)", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = preferences.cardElevationDp.toFloat(),
                        onValueChange = { scope.launch { preferencesManager.setCardElevationDp(it.roundToInt()) } },
                        valueRange = 0f..12f,
                    )
                    Text("Animation Intensity", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AnimationIntensity.entries.forEach { anim ->
                            FilterChip(
                                selected = preferences.animationIntensity == anim,
                                onClick = { scope.launch { preferencesManager.setAnimationIntensity(anim) } },
                                label = { Text(anim.label) },
                            )
                        }
                    }
                }
            }

            // 8. Modes, Viewer, Haptics & AI
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("Display Modes, Viewer & AI", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Full-Screen Viewer Backdrop", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ViewerBackgroundMode.entries.forEach { mode ->
                            FilterChip(
                                selected = preferences.viewerBackgroundMode == mode,
                                onClick = { scope.launch { preferencesManager.setViewerBackgroundMode(mode) } },
                                label = { Text(mode.label) },
                            )
                        }
                    }
                    StudioSwitchRow(
                        title = "AMOLED Pure Black Mode",
                        subtitle = "Force pitch-black #000000 backdrop across screens",
                        checked = preferences.amoledMode,
                        onCheckedChange = { scope.launch { preferencesManager.setAmoledMode(it) } },
                    )
                    StudioSwitchRow(
                        title = "Haptic Feedback",
                        subtitle = "Tactile vibration on navigation and editor tools",
                        checked = preferences.hapticFeedbackEnabled,
                        onCheckedChange = { scope.launch { preferencesManager.setHapticFeedback(it) } },
                    )
                    StudioSwitchRow(
                        title = "Reduced Motion",
                        subtitle = "Minimize transitions and visual motion",
                        checked = preferences.reducedMotion,
                        onCheckedChange = { scope.launch { preferencesManager.setReducedMotion(it) } },
                    )
                    StudioSwitchRow(
                        title = "Double-Tap Quick Favorite",
                        subtitle = "Heart photos quickly when double-tapping thumbnails",
                        checked = preferences.doubleTapQuickFavorite,
                        onCheckedChange = { scope.launch { preferencesManager.setDoubleTapQuickFavorite(it) } },
                    )
                    StudioSwitchRow(
                        title = "Auto-index new media on launch",
                        subtitle = "Automatically run EXIF & ML Kit indexing on device updates",
                        checked = preferences.autoIndexOnLaunch,
                        onCheckedChange = { scope.launch { preferencesManager.setAutoIndexOnLaunch(it) } },
                    )
                    StudioSwitchRow(
                        title = "Semantic Search expansion by default",
                        subtitle = "Include offline synonym/concept graph matches in Search 2.0",
                        checked = preferences.semanticSearchEnabled,
                        onCheckedChange = onToggleSemanticDefault,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                preferencesManager.resetAppearanceDefaults()
                                Toast.makeText(context, "All appearance settings reset to default", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_personalization_button"),
                    ) {
                        Icon(Icons.Rounded.RestartAlt, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset All Appearance Defaults")
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showCreateCustomTheme) {
        CreateOrEditCustomThemeDialog(
            existing = editingCustomTheme,
            onDismiss = { showCreateCustomTheme = false },
            onSave = { profile ->
                scope.launch { preferencesManager.saveCustomTheme(profile, activate = true) }
                showCreateCustomTheme = false
            },
        )
    }

    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text("Import Custom Theme JSON") },
            text = {
                OutlinedTextField(
                    value = importJsonInput,
                    onValueChange = { importJsonInput = it },
                    label = { Text("Paste CustomThemeProfile JSON") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    enabled = importJsonInput.isNotBlank(),
                    onClick = {
                        try {
                            val parsed = json.decodeFromString<CustomThemeProfile>(importJsonInput.trim())
                            val clean = parsed.copy(id = "custom_${UUID.randomUUID().toString().take(8)}")
                            scope.launch { preferencesManager.saveCustomTheme(clean, activate = true) }
                            showImportJsonDialog = false
                            importJsonInput = ""
                            Toast.makeText(context, "Imported theme ${clean.name}", Toast.LENGTH_SHORT).show()
                        } catch (_: Throwable) {
                            Toast.makeText(context, "Invalid Theme JSON", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) { Text("Import & Activate") }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HexOverrideRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onApply: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text("#RRGGBB") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            enabled = parseHexColorOrNull(value) != null,
            onClick = { onApply(value) },
        ) { Text("Apply") }
    }
}

@Composable
private fun StudioSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = LocalGallery56Tokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = tokens.semantic.textPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = tokens.semantic.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CreateOrEditCustomThemeDialog(
    existing: CustomThemeProfile?,
    onDismiss: () -> Unit,
    onSave: (CustomThemeProfile) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "My Studio Theme") }
    var isDark by remember(existing) { mutableStateOf(existing?.isDark ?: true) }
    var primaryHex by remember(existing) { mutableStateOf(existing?.primaryHex ?: "#5CE1E6") }
    var secondaryHex by remember(existing) { mutableStateOf(existing?.secondaryHex ?: "#C4CBD4") }
    var backgroundHex by remember(existing) { mutableStateOf(existing?.backgroundHex ?: "#07090D") }
    var surfaceHex by remember(existing) { mutableStateOf(existing?.surfaceHex ?: "#0F131A") }
    var cardHex by remember(existing) { mutableStateOf(existing?.cardHex ?: "#18202D") }
    var borderHex by remember(existing) { mutableStateOf(existing?.borderHex ?: "#335CE1E6") }

    val valid = name.isNotBlank() &&
        parseHexColorOrNull(primaryHex) != null &&
        parseHexColorOrNull(secondaryHex) != null &&
        parseHexColorOrNull(backgroundHex) != null &&
        parseHexColorOrNull(surfaceHex) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Build Custom Theme" else "Edit ${existing.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Theme Name") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Dark Mode Palette")
                    Switch(checked = isDark, onCheckedChange = { isDark = it })
                }
                OutlinedTextField(value = primaryHex, onValueChange = { primaryHex = it }, label = { Text("Primary Accent HEX") }, singleLine = true)
                OutlinedTextField(value = secondaryHex, onValueChange = { secondaryHex = it }, label = { Text("Secondary HEX") }, singleLine = true)
                OutlinedTextField(value = backgroundHex, onValueChange = { backgroundHex = it }, label = { Text("Background HEX") }, singleLine = true)
                OutlinedTextField(value = surfaceHex, onValueChange = { surfaceHex = it }, label = { Text("Surface HEX") }, singleLine = true)
                OutlinedTextField(value = cardHex, onValueChange = { cardHex = it }, label = { Text("Card Surface HEX") }, singleLine = true)
                OutlinedTextField(value = borderHex, onValueChange = { borderHex = it }, label = { Text("Border HEX") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        CustomThemeProfile(
                            id = existing?.id ?: "custom_${UUID.randomUUID().toString().take(8)}",
                            name = name.trim(),
                            isDark = isDark,
                            primaryHex = primaryHex.trim(),
                            secondaryHex = secondaryHex.trim(),
                            backgroundHex = backgroundHex.trim(),
                            surfaceHex = surfaceHex.trim(),
                            cardHex = cardHex.trim(),
                            borderHex = borderHex.trim(),
                        ),
                    )
                },
            ) { Text("Save & Activate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
