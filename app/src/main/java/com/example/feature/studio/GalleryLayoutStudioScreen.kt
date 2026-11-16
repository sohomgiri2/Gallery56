package com.example.feature.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ViewQuilt
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.datastore.GalleryLayoutId
import com.example.core.datastore.GalleryPreferences
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.ThumbnailShapeStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalleryLayoutStudioScreen(
    preferences: GalleryPreferences,
    onApplyLayout: (GalleryLayoutId) -> Unit,
    onSetGridColumns: (Int) -> Unit,
    onSetCornerRadiusDp: (Int) -> Unit,
    onSetGridSpacingDp: (Int) -> Unit,
    onSetThumbnailScale: (Float) -> Unit,
    onSetThumbnailShape: (ThumbnailShapeStyle) -> Unit,
    onSetShowDateGrouping: (Boolean) -> Unit,
    onSetAlbumCardLarge: (Boolean) -> Unit,
    onResetLayoutDefaults: () -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalGallery56Tokens.current
    val activeLayout = GalleryLayoutId.fromId(preferences.galleryLayoutId)
    var previewSelected by remember(activeLayout) { mutableStateOf(activeLayout) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gallery_layout_studio_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Gallery56TopBar(
                title = "GALLERY LAYOUT STUDIO",
                subtitle = "${GalleryLayoutId.entries.size} live UI architectures • Instant apply",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = tokens.semantic.iconPrimary,
                        )
                    }
                },
                primaryActions = {
                    IconButton(onClick = {
                        onResetLayoutDefaults()
                        previewSelected = GalleryLayoutId.CLASSIC_GRID
                    }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset layout defaults")
                    }
                },
            )
        }

        // Interactive Preview Box for currently selected layout
        item {
            GlassPanel(
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preview: ${previewSelected.displayName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = tokens.semantic.textPrimary,
                        )
                        Text(
                            text = previewSelected.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.semantic.textSecondary,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    if (previewSelected == activeLayout) {
                        MetricPill(label = "ACTIVE", icon = Icons.Rounded.CheckCircle)
                    }
                }
                Spacer(Modifier.height(10.dp))
                LayoutWireframePreview(layout = previewSelected, columns = preferences.gridColumns)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onApplyLayout(previewSelected) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apply_layout_btn"),
                    ) {
                        Icon(Icons.Rounded.ViewQuilt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (previewSelected == activeLayout) "Applied" else "Apply Layout")
                    }
                    OutlinedButton(
                        onClick = {
                            onResetLayoutDefaults()
                            previewSelected = GalleryLayoutId.CLASSIC_GRID
                        },
                    ) {
                        Text("Reset")
                    }
                }
            }
        }

        // Quick Geometry Customization inside Layout Studio
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Grid & Card Geometry", style = MaterialTheme.typography.titleMedium, color = tokens.semantic.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Grid Columns (${preferences.gridColumns})", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..6).forEach { cols ->
                        FilterChip(
                            selected = preferences.gridColumns == cols,
                            onClick = { onSetGridColumns(cols) },
                            label = { Text("$cols") },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Thumbnail Scale (${String.format("%.1fx", preferences.thumbnailSizeScale)})", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = preferences.thumbnailSizeScale,
                    onValueChange = onSetThumbnailScale,
                    valueRange = 0.8f..1.4f,
                )
                Text("Grid Spacing (${preferences.gridSpacingDp} dp)", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = preferences.gridSpacingDp.toFloat(),
                    onValueChange = { onSetGridSpacingDp(it.toInt()) },
                    valueRange = 2f..20f,
                )
                Text("Corner Radius (${preferences.cornerRadiusDp} dp)", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = preferences.cornerRadiusDp.toFloat(),
                    onValueChange = { onSetCornerRadiusDp(it.toInt()) },
                    valueRange = 0f..28f,
                )
                Text("Thumbnail Shape", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThumbnailShapeStyle.entries.forEach { shape ->
                        FilterChip(
                            selected = preferences.thumbnailShapeStyle == shape,
                            onClick = { onSetThumbnailShape(shape) },
                            label = { Text(shape.label) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Date Grouping Headers", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = preferences.showDateGrouping, onCheckedChange = onSetShowDateGrouping)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Large Album Showcase Cards", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = preferences.albumCardLarge, onCheckedChange = onSetAlbumCardLarge)
                }
            }
        }

        // All 14 Selectable Layouts
        items(GalleryLayoutId.entries, key = { it.id }) { layout ->
            val isApplied = layout == activeLayout
            val isPreviewed = layout == previewSelected
            GlassPanel(
                onClick = { previewSelected = layout },
                borderColor = when {
                    isApplied -> MaterialTheme.colorScheme.primary
                    isPreviewed -> MaterialTheme.colorScheme.secondary
                    else -> tokens.semantic.cardBorder
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("layout_card_${layout.id}"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = layout.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = tokens.semantic.textPrimary,
                        )
                        Text(
                            text = layout.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.semantic.textSecondary,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    if (isApplied) {
                        MetricPill(label = "Active")
                    } else {
                        OutlinedButton(onClick = {
                            previewSelected = layout
                            onApplyLayout(layout)
                        }) {
                            Text("Apply")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun LayoutWireframePreview(layout: GalleryLayoutId, columns: Int) {
    val tokens = LocalGallery56Tokens.current
    val primary = MaterialTheme.colorScheme.primary
    val blockColor = tokens.elevatedSurface
    val accentBlock = primary.copy(alpha = 0.25f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.semantic.background)
            .border(1.dp, tokens.glassBorder, RoundedCornerShape(12.dp))
            .padding(8.dp),
    ) {
        when (layout) {
            GalleryLayoutId.CINEMATIC_GALLERY, GalleryLayoutId.MAGAZINE_GALLERY -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1.8f).fillMaxSize().clip(RoundedCornerShape(8.dp)).background(accentBlock))
                    Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(blockColor))
                        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(blockColor))
                    }
                }
            }
            GalleryLayoutId.MASONRY_FLOW -> {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1.4f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(accentBlock))
                        Box(Modifier.weight(0.8f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(blockColor))
                    }
                    Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(0.8f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(blockColor))
                        Box(Modifier.weight(1.4f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(accentBlock))
                    }
                }
            }
            else -> {
                val previewCols = if (layout == GalleryLayoutId.COMPACT_PRO) 5 else columns.coerceIn(2, 4)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth(0.45f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(accentBlock))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                        repeat(previewCols) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(if (layout == GalleryLayoutId.NOTHING_MINIMAL) 3.dp else 8.dp))
                                    .background(blockColor)
                                    .border(0.75.dp, tokens.glassBorder, RoundedCornerShape(6.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}
