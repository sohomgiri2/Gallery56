package com.example.feature.studio

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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.datastore.GalleryPreferences
import com.example.core.datastore.NavigationStyle
import com.example.core.designsystem.GlassPanel
import com.example.ui.theme.CustomThemeProfile
import com.example.ui.theme.GalleryThemePreset
import com.example.ui.theme.StatusRose
import com.example.ui.theme.parseHexColorOrNull
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizationStudioScreen(
    preferences: GalleryPreferences,
    onSelectPresetTheme: (String) -> Unit,
    onSetCustomAccentHex: (String) -> Unit,
    onSaveCustomTheme: (CustomThemeProfile) -> Unit,
    onActivateCustomTheme: (String) -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onSelectGridColumns: (Int) -> Unit,
    onSetCornerRadiusDp: (Int) -> Unit,
    onSetGridSpacingDp: (Int) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSetNavigationStyle: (NavigationStyle) -> Unit,
    onToggleAutoIndex: (Boolean) -> Unit,
    onToggleSemanticDefault: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    onBack: () -> Unit,
) {
    var accentHexInput by remember(preferences.customAccentHexOverride) {
        mutableStateOf(preferences.customAccentHexOverride)
    }
    var showCreateCustomTheme by remember { mutableStateOf(false) }

    val quickAccents = listOf(
        "#5CE1E6", "#00F5D4", "#60A5FA", "#C084FC",
        "#F5C469", "#FB7185", "#34D399", "#FF3B30",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customization_studio_screen"),
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
                        Text("THEME & STUDIO ENGINE", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "14 presets, custom HEX builder, geometry & motion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onResetDefaults) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset studio defaults")
                }
            }
        }

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
                    OutlinedButton(
                        onClick = { showCreateCustomTheme = true },
                        modifier = Modifier.testTag("studio_new_custom_theme"),
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Custom")
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
                                .clickable { onSelectPresetTheme(preset.id) }
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

        if (preferences.customThemes.isNotEmpty()) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("Saved Custom Themes", style = MaterialTheme.typography.titleMedium)
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
                                .clickable { onActivateCustomTheme(profile.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteCustomTheme(profile.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete custom theme", tint = StatusRose)
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Accent HEX Override", style = MaterialTheme.typography.titleMedium)
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
                                    accentHexInput = hex
                                    onSetCustomAccentHex(hex)
                                },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = accentHexInput,
                        onValueChange = { accentHexInput = it },
                        label = { Text("HEX (e.g. #00F5D4)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        enabled = parseHexColorOrNull(accentHexInput) != null,
                        onClick = { onSetCustomAccentHex(accentHexInput) },
                    ) { Text("Apply") }
                    TextButton(
                        onClick = {
                            accentHexInput = ""
                            onSetCustomAccentHex("")
                        },
                    ) { Text("Clear") }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Layout Geometry & Motion", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Text("Grid Columns (${preferences.gridColumns})", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..5).forEach { cols ->
                        FilterChip(
                            selected = preferences.gridColumns == cols,
                            onClick = { onSelectGridColumns(cols) },
                            label = { Text("$cols Col") },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Tile Corner Radius (${preferences.cornerRadiusDp} dp)", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = preferences.cornerRadiusDp.toFloat(),
                    onValueChange = { onSetCornerRadiusDp(it.toInt()) },
                    valueRange = 0f..24f,
                )

                Text("Grid Spacing (${preferences.gridSpacingDp} dp)", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = preferences.gridSpacingDp.toFloat(),
                    onValueChange = { onSetGridSpacingDp(it.toInt()) },
                    valueRange = 2f..16f,
                )

                Spacer(Modifier.height(8.dp))
                Text("Navigation Bar Style", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavigationStyle.entries.forEach { style ->
                        FilterChip(
                            selected = preferences.navigationStyle == style,
                            onClick = { onSetNavigationStyle(style) },
                            label = { Text(style.label) },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                StudioSwitchRow(
                    title = "Reduced Motion",
                    subtitle = "Minimize transitions and visual motion",
                    checked = preferences.reducedMotion,
                    onCheckedChange = onSetReducedMotion,
                )
                StudioSwitchRow(
                    title = "Auto-index new media on launch",
                    subtitle = "Automatically run EXIF & ML Kit indexing on device updates",
                    checked = preferences.autoIndexOnLaunch,
                    onCheckedChange = onToggleAutoIndex,
                )
                StudioSwitchRow(
                    title = "Semantic Search expansion by default",
                    subtitle = "Include offline synonym/concept graph matches in Search 2.0",
                    checked = preferences.semanticSearchEnabled,
                    onCheckedChange = onToggleSemanticDefault,
                )
            }
        }
    }

    if (showCreateCustomTheme) {
        CreateCustomThemeDialog(
            onDismiss = { showCreateCustomTheme = false },
            onSave = { profile ->
                onSaveCustomTheme(profile)
                showCreateCustomTheme = false
            },
        )
    }
}

@Composable
private fun StudioSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CreateCustomThemeDialog(
    onDismiss: () -> Unit,
    onSave: (CustomThemeProfile) -> Unit,
) {
    var name by remember { mutableStateOf("My Studio Theme") }
    var isDark by remember { mutableStateOf(true) }
    var primaryHex by remember { mutableStateOf("#5CE1E6") }
    var secondaryHex by remember { mutableStateOf("#C4CBD4") }
    var backgroundHex by remember { mutableStateOf("#07090D") }
    var surfaceHex by remember { mutableStateOf("#0F131A") }

    val valid = name.isNotBlank() &&
        parseHexColorOrNull(primaryHex) != null &&
        parseHexColorOrNull(secondaryHex) != null &&
        parseHexColorOrNull(backgroundHex) != null &&
        parseHexColorOrNull(surfaceHex) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Theme") },
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
                OutlinedTextField(
                    value = primaryHex,
                    onValueChange = { primaryHex = it },
                    label = { Text("Primary Accent HEX") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = secondaryHex,
                    onValueChange = { secondaryHex = it },
                    label = { Text("Secondary HEX") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = backgroundHex,
                    onValueChange = { backgroundHex = it },
                    label = { Text("Background HEX") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = surfaceHex,
                    onValueChange = { surfaceHex = it },
                    label = { Text("Surface HEX") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        CustomThemeProfile(
                            id = "custom_${UUID.randomUUID().toString().take(8)}",
                            name = name.trim(),
                            isDark = isDark,
                            primaryHex = primaryHex.trim(),
                            secondaryHex = secondaryHex.trim(),
                            backgroundHex = backgroundHex.trim(),
                            surfaceHex = surfaceHex.trim(),
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
