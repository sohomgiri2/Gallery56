package com.example.feature.intelligence

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MediaThumbnailTile
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.SectionHeader
import com.example.feature.intelligence.engine.PhotoCluster
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald

enum class AiStudioTab(val label: String) {
    OVERVIEW("Smart Albums"),
    CLUSTERS("Groups & Best Shots"),
    QUALITY("Quality & OCR"),
    FACES("Face Organization"),
}

@Composable
fun IntelligenceScreen(
    uiState: IntelligenceUiState,
    onStartIndex: (Boolean) -> Unit,
    onScheduleBackgroundIndex: () -> Unit,
    onCancelIndex: () -> Unit,
    onToggleFaceAnalysis: (Boolean) -> Unit,
    onRenameFaceGroup: (String, String) -> Unit,
    onMergeFaceGroups: (String, String) -> Unit,
    onRemoveFaceFromGroup: (String) -> Unit,
    onDeleteFaceGroup: (String) -> Unit,
    onDeleteAllFaceData: () -> Unit,
    onFilterByFaceGroup: (String) -> Unit,
    onOpenSmartRecommendation: (String) -> Unit,
    onOpenMedia: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tokens = LocalGallery56Tokens.current
    var activeTab by remember { mutableStateOf(AiStudioTab.OVERVIEW) }
    var showModelDetailsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_intelligence_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(tokens.elevatedSurface),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ai_core_banner_1794764219096),
                    contentDescription = "Gallery56 Offline AI Engine",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.84f),
                                ),
                            ),
                        )
                        .padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MetricPill(
                            label = "OFFLINE-FIRST ON-DEVICE AI",
                            icon = Icons.Rounded.Memory,
                            accentColor = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "GALLERY56 INTELLIGENCE",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                        Text(
                            text = "${uiState.aiIndexedCount}/${uiState.totalMediaCount} indexed • ${uiState.ocrCount} OCR • ${uiState.gpsCount} GPS • Zero cloud uploads",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        // Indexing Controls Card
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Content Indexing Pipeline", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = uiState.indexingProgress.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(
                        onClick = { showModelDetailsDialog = true },
                        modifier = Modifier.testTag("open_model_status_btn"),
                    ) {
                        Icon(Icons.Rounded.Info, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Models")
                    }
                }

                if (uiState.indexingProgress.isRunning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.indexingProgress.progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (uiState.indexingProgress.isRunning) {
                        Button(
                            onClick = onCancelIndex,
                            modifier = Modifier.testTag("cancel_ai_index_btn"),
                        ) {
                            Icon(Icons.Rounded.Cancel, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause")
                        }
                    } else {
                        Button(
                            onClick = { onStartIndex(false) },
                            modifier = Modifier.testTag("run_ai_index_btn"),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Index New")
                        }
                        OutlinedButton(
                            onClick = { onStartIndex(true) },
                            modifier = Modifier.testTag("reanalyze_all_btn"),
                        ) {
                            Text("Re-Analyze All")
                        }
                        OutlinedButton(
                            onClick = onScheduleBackgroundIndex,
                            modifier = Modifier.testTag("schedule_worker_btn"),
                        ) {
                            Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Background")
                        }
                    }
                }
            }
        }

        // AI Studio Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiStudioTab.entries.forEach { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        leadingIcon = when (tab) {
                            AiStudioTab.OVERVIEW -> {
                                { Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp)) }
                            }
                            AiStudioTab.CLUSTERS -> {
                                { Icon(Icons.Rounded.Collections, null, Modifier.size(16.dp)) }
                            }
                            AiStudioTab.QUALITY -> {
                                { Icon(Icons.Rounded.DocumentScanner, null, Modifier.size(16.dp)) }
                            }
                            AiStudioTab.FACES -> {
                                { Icon(Icons.Rounded.Face, null, Modifier.size(16.dp)) }
                            }
                        },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("ai_tab_${tab.name.lowercase()}"),
                    )
                }
            }
        }

        when (activeTab) {
            AiStudioTab.OVERVIEW -> {
                item {
                    SectionHeader(
                        title = "Smart Album Recommendations",
                        subtitle = "Derived automatically from on-device OCR, ML Kit labels & EXIF",
                    )
                }
                if (uiState.summary.recommendations.isEmpty()) {
                    item {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Index device media above to generate Smart Album recommendations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(uiState.summary.recommendations, key = { it.id }) { rec ->
                        GlassPanel(
                            onClick = { onOpenSmartRecommendation(rec.filterTag) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rec.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = rec.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                MetricPill(label = "${rec.count} items")
                            }
                            if (rec.previewItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(rec.previewItems, key = { it.mediaStoreId }) { media ->
                                        Box(modifier = Modifier.size(72.dp)) {
                                            MediaThumbnailTile(
                                                media = media,
                                                onClick = { onOpenMedia(media.mediaStoreId) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AiStudioTab.CLUSTERS -> {
                if (uiState.summary.bestShots.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Best-Shot Recommendations",
                            subtitle = "Highest sharpness + balanced exposure across groups",
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.summary.bestShots, key = { it.mediaStoreId }) { shot ->
                                Box(modifier = Modifier.size(110.dp)) {
                                    MediaThumbnailTile(
                                        media = shot,
                                        badgeText = "Score ${shot.qualityScore.toInt()}",
                                        onClick = { onOpenMedia(shot.mediaStoreId) },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    ClusterSection(
                        title = "Exact Duplicates (SHA-256)",
                        icon = Icons.Rounded.FileCopy,
                        clusters = uiState.summary.duplicateClusters,
                        emptyHint = "No exact file duplicates detected among indexed items.",
                        onOpenMedia = onOpenMedia,
                    )
                }
                item {
                    ClusterSection(
                        title = "Visually Similar Photos (dHash)",
                        icon = Icons.Rounded.Collections,
                        clusters = uiState.summary.similarClusters,
                        emptyHint = "No visually similar series detected yet.",
                        onOpenMedia = onOpenMedia,
                    )
                }
                item {
                    ClusterSection(
                        title = "Temporal Photo Groups & Bursts",
                        icon = Icons.Rounded.Star,
                        clusters = uiState.summary.burstGroups,
                        emptyHint = "Capture multiple photos within 90s to form burst groups.",
                        onOpenMedia = onOpenMedia,
                    )
                }
            }

            AiStudioTab.QUALITY -> {
                item {
                    SectionHeader(
                        title = "Documents & OCR Captures",
                        subtitle = "${uiState.summary.documents.size} detected document(s)",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.summary.documents.isEmpty()) {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No documents or receipts detected yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.summary.documents, key = { it.mediaStoreId }) { doc ->
                                Box(modifier = Modifier.size(110.dp)) {
                                    MediaThumbnailTile(
                                        media = doc,
                                        badgeText = "DOC ${(doc.documentConfidence * 100).toInt()}%",
                                        onClick = { onOpenMedia(doc.mediaStoreId) },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Blurry / Soft-Focus Review",
                        subtitle = "${uiState.summary.blurryPhotos.size} photo(s) below sharpness threshold",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.summary.blurryPhotos.isEmpty()) {
                        GlassPanel(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Rounded.BlurOn, null, tint = StatusEmerald)
                                Text(
                                    text = "All indexed photos pass sharpness threshold.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.summary.blurryPhotos, key = { it.mediaStoreId }) { blurry ->
                                Box(modifier = Modifier.size(110.dp)) {
                                    MediaThumbnailTile(
                                        media = blurry,
                                        badgeText = "Sharp ${blurry.sharpnessScore.toInt()}",
                                        onClick = { onOpenMedia(blurry.mediaStoreId) },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Detected Screenshots",
                        subtitle = "${uiState.summary.screenshots.size} screenshot(s)",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.summary.screenshots.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.summary.screenshots, key = { it.mediaStoreId }) { shot ->
                                Box(modifier = Modifier.size(110.dp)) {
                                    MediaThumbnailTile(
                                        media = shot,
                                        badgeText = "SCREEN",
                                        onClick = { onOpenMedia(shot.mediaStoreId) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AiStudioTab.FACES -> {
                item {
                    FaceOrganizationSection(
                        faceAnalysisEnabled = uiState.preferences.faceAnalysisEnabled,
                        faceGroups = uiState.faceGroups,
                        faceDetections = uiState.faceDetections,
                        onToggleFaceAnalysis = onToggleFaceAnalysis,
                        onRunReindexWithFaces = { onStartIndex(true) },
                        onRenameGroup = onRenameFaceGroup,
                        onMergeGroups = onMergeFaceGroups,
                        onRemoveFaceFromGroup = onRemoveFaceFromGroup,
                        onDeleteGroup = onDeleteFaceGroup,
                        onDeleteAllFaceData = onDeleteAllFaceData,
                        onFilterByFaceGroup = onFilterByFaceGroup,
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showModelDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showModelDetailsDialog = false },
            title = { Text("On-Device AI Model Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.modelStatuses.forEach { model ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = if (model.isAvailable) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = if (model.isAvailable) StatusEmerald else StatusAmber,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(model.name, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = model.statusDetail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Offer optional AICore Play Store check if user wants to inspect Android System AICore
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.aicore"),
                            )
                            context.startActivity(intent)
                        } catch (_: Throwable) {
                            // No browser/Play Store on emulator
                        }
                    },
                ) {
                    Text("Check Optional AICore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showModelDetailsDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}

@Composable
private fun ClusterSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    clusters: List<PhotoCluster>,
    emptyHint: String,
    onOpenMedia: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = title, subtitle = "${clusters.size} group(s)")
        if (clusters.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = emptyHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            clusters.take(10).forEach { cluster ->
                GlassPanel(contentPadding = 12.dp, modifier = Modifier.fillMaxWidth()) {
                    Text(cluster.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = cluster.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cluster.items, key = { it.mediaStoreId }) { item ->
                            Box(modifier = Modifier.size(96.dp)) {
                                MediaThumbnailTile(
                                    media = item,
                                    badgeText = if (item.mediaStoreId == cluster.bestShotId) "BEST" else null,
                                    onClick = { onOpenMedia(item.mediaStoreId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
