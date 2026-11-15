package com.example.feature.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.ViewQuilt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.SectionHeader
import com.example.ui.theme.StatusEmerald

data class StudioToolShortcut(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsHubScreen(
    totalMediaCount: Int,
    aiIndexedCount: Int,
    vaultCount: Int,
    trashCount: Int,
    onOpenAlbums: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenPrivateVault: () -> Unit,
    onOpenPhotoEditor: () -> Unit,
    onOpenCollageStudio: () -> Unit,
    onOpenPosterStudio: () -> Unit,
    onOpenDocumentScanner: () -> Unit,
    onOpenStorageManager: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenCustomizationStudio: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenAiHub: () -> Unit,
    onOpenDeveloperInfo: () -> Unit,
) {
    val creativeTools = listOf(
        StudioToolShortcut("Photo Editor", "Crop, adjust, filters & text", Icons.Rounded.Edit, "tool_photo_editor", onOpenPhotoEditor),
        StudioToolShortcut("Collage Studio", "Multi-photo grids & spacing", Icons.Rounded.ViewQuilt, "tool_collage_studio", onOpenCollageStudio),
        StudioToolShortcut("Poster Studio", "Editorial poster templates", Icons.Rounded.AutoAwesomeMosaic, "tool_poster_studio", onOpenPosterStudio),
        StudioToolShortcut("Document Scanner", "Camera scan, OCR & PDF", Icons.Rounded.DocumentScanner, "tool_doc_scanner", onOpenDocumentScanner),
    )

    val libraryAndSecurityTools = listOf(
        StudioToolShortcut("Albums Manager", "Custom & physical albums", Icons.Rounded.PhotoAlbum, "tool_albums", onOpenAlbums),
        StudioToolShortcut("Memories", "Real timelines & slideshows", Icons.Rounded.CollectionsBookmark, "tool_memories", onOpenMemories),
        StudioToolShortcut("Private Vault", "$vaultCount AES-256 encrypted", Icons.Rounded.Lock, "tool_private_vault", onOpenPrivateVault),
        StudioToolShortcut("Recently Deleted", "$trashCount cached item(s)", Icons.Rounded.DeleteSweep, "tool_recently_deleted", onOpenRecentlyDeleted),
        StudioToolShortcut("Storage Manager", "Analytics, large files & cache", Icons.Rounded.SdStorage, "tool_storage_manager", onOpenStorageManager),
        StudioToolShortcut("AI Hub", "$aiIndexedCount/$totalMediaCount indexed", Icons.Rounded.AutoAwesome, "tool_ai_hub", onOpenAiHub),
        StudioToolShortcut("Theme Studio", "14 presets, HEX & layout", Icons.Rounded.Palette, "tool_theme_studio", onOpenCustomizationStudio),
        StudioToolShortcut("Backup & Restore", "SAF JSON & ZIP archives", Icons.Rounded.Backup, "tool_backup_restore", onOpenBackupRestore),
        StudioToolShortcut("Developer Info", "Sohom Giri • @itz_sg_063", Icons.Rounded.Info, "tool_developer_info", onOpenDeveloperInfo),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_hub_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(
                title = "GALLERY56 STUDIO & TOOLS",
                subtitle = "Creative labs, AES-256 vault, storage forensics & customization",
            )
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MetricPill("$totalMediaCount Media", Icons.Rounded.GridView)
                    MetricPill("$aiIndexedCount AI Indexed", Icons.Rounded.AutoAwesome, StatusEmerald)
                    MetricPill("$vaultCount Vault", Icons.Rounded.Lock)
                    MetricPill("$trashCount Trash", Icons.Rounded.DeleteSweep)
                }
            }
        }

        item {
            Text("Creative Production Studios", style = MaterialTheme.typography.titleMedium)
        }

        item {
            ToolGrid(creativeTools)
        }

        item {
            Text("Library, Security & System", style = MaterialTheme.typography.titleMedium)
        }

        item {
            ToolGrid(libraryAndSecurityTools)
        }
    }
}

@Composable
private fun ToolGrid(tools: List<StudioToolShortcut>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tools.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { tool ->
                    GlassPanel(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(tool.testTag),
                        onClick = tool.onClick,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tool.title, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tool.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
