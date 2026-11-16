package com.example.feature.studio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusEmerald

enum class StudioCategory(val label: String) {
    ALL("All"),
    PHOTO("Photo Creation"),
    VIDEO("Video Lab"),
    LIBRARY("Smart Library"),
    AI("AI & Memories"),
    SECURITY("Security & Storage"),
    CUSTOMIZATION("UI & Themes"),
}

data class StudioToolShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: StudioCategory,
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
    pinnedToolIds: List<String>,
    recentToolIds: List<String>,
    onTogglePinTool: (String) -> Unit,
    onRecordRecentTool: (String) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenPrivateVault: () -> Unit,
    onOpenPhotoEditor: () -> Unit,
    onOpenCollageStudio: () -> Unit,
    onOpenPosterStudio: () -> Unit,
    onOpenTextArtStudio: () -> Unit,
    onOpenWallpaperStudio: () -> Unit,
    onOpenDocumentScanner: () -> Unit,
    onOpenVideoStudio: () -> Unit,
    onOpenSmartCollections: () -> Unit,
    onOpenIntegrityChecker: () -> Unit,
    onOpenStorageManager: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenLayoutStudio: () -> Unit,
    onOpenCustomizationStudio: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenAiHub: () -> Unit,
    onOpenDeveloperInfo: () -> Unit,
) {
    val tokens = LocalGallery56Tokens.current
    var searchQuery by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf(StudioCategory.ALL) }

    val allTools = remember(
        totalMediaCount,
        aiIndexedCount,
        vaultCount,
        trashCount,
    ) {
        listOf(
            // A. Photo Creation
            StudioToolShortcut("photo_editor", "Photo Editor 2.0", "Filters, HSL, brush, crop & FX", StudioCategory.PHOTO, Icons.Rounded.Edit, "tool_photo_editor", onOpenPhotoEditor),
            StudioToolShortcut("collage_studio", "Collage & Frames", "Grids, Freeform & Museum Frame", StudioCategory.PHOTO, Icons.Rounded.ViewQuilt, "tool_collage_studio", onOpenCollageStudio),
            StudioToolShortcut("poster_studio", "Poster Studio", "Editorial layouts & covers", StudioCategory.PHOTO, Icons.Rounded.AutoAwesomeMosaic, "tool_poster_studio", onOpenPosterStudio),
            StudioToolShortcut("text_art_studio", "Text-on-Art Studio", "Typography quote & art cards", StudioCategory.PHOTO, Icons.Rounded.TextFields, "tool_text_art_studio", onOpenTextArtStudio),
            StudioToolShortcut("wallpaper_studio", "Wallpaper Studio", "Lock/home frame, blur & dim", StudioCategory.PHOTO, Icons.Rounded.Wallpaper, "tool_wallpaper_studio", onOpenWallpaperStudio),
            StudioToolShortcut("doc_scanner", "Document Scanner", "Camera scan, OCR & PDF", StudioCategory.PHOTO, Icons.Rounded.DocumentScanner, "tool_doc_scanner", onOpenDocumentScanner),
            // B. Video Lab
            StudioToolShortcut("video_studio", "Video Studio", "Trim MP4, GIF export & frames", StudioCategory.VIDEO, Icons.Rounded.Movie, "tool_video_studio", onOpenVideoStudio),
            // C. Smart Library & Organization
            StudioToolShortcut("smart_collections", "Smart Collections", "Viewed, Edited, Bursts & Histogram", StudioCategory.LIBRARY, Icons.Rounded.SmartToy, "tool_smart_collections", onOpenSmartCollections),
            StudioToolShortcut("albums_manager", "Albums Manager", "Smart, custom & physical folders", StudioCategory.LIBRARY, Icons.Rounded.PhotoAlbum, "tool_albums", onOpenAlbums),
            // D. AI & Memories
            StudioToolShortcut("ai_hub", "AI Hub", "$aiIndexedCount/$totalMediaCount ML Kit indexed", StudioCategory.AI, Icons.Rounded.AutoAwesome, "tool_ai_hub", onOpenAiHub),
            StudioToolShortcut("memories", "Memories & Reels", "Curated stories & slideshows", StudioCategory.AI, Icons.Rounded.CollectionsBookmark, "tool_memories", onOpenMemories),
            // E. Security & Storage
            StudioToolShortcut("private_vault", "Private Vault", "$vaultCount AES-256 encrypted", StudioCategory.SECURITY, Icons.Rounded.Lock, "tool_private_vault", onOpenPrivateVault),
            StudioToolShortcut("integrity_checker", "Privacy & Integrity", "Batch GPS cleaner & SHA-256", StudioCategory.SECURITY, Icons.Rounded.VerifiedUser, "tool_integrity_checker", onOpenIntegrityChecker),
            StudioToolShortcut("storage_manager", "Storage Manager", "Analytics, large files & cache", StudioCategory.SECURITY, Icons.Rounded.SdStorage, "tool_storage_manager", onOpenStorageManager),
            StudioToolShortcut("recently_deleted", "Recently Deleted", "$trashCount cached item(s)", StudioCategory.SECURITY, Icons.Rounded.DeleteSweep, "tool_recently_deleted", onOpenRecentlyDeleted),
            StudioToolShortcut("backup_restore", "Backup & Restore", "SAF JSON & ZIP archives", StudioCategory.SECURITY, Icons.Rounded.Backup, "tool_backup_restore", onOpenBackupRestore),
            // F. UI & Themes
            StudioToolShortcut("layout_studio", "Gallery Layout Studio", "14 switchable UI architectures", StudioCategory.CUSTOMIZATION, Icons.Rounded.Tune, "tool_layout_studio", onOpenLayoutStudio),
            StudioToolShortcut("theme_studio", "Personalization Lab", "22 themes, HEX & 30 controls", StudioCategory.CUSTOMIZATION, Icons.Rounded.Palette, "tool_theme_studio", onOpenCustomizationStudio),
            StudioToolShortcut("developer_info", "Developer Profile", "Sohom Giri • @itz_sg_063", StudioCategory.CUSTOMIZATION, Icons.Rounded.Info, "tool_developer_info", onOpenDeveloperInfo),
        )
    }

    val toolById = remember(allTools) { allTools.associateBy { it.id } }
    val pinnedTools = remember(pinnedToolIds, toolById) { pinnedToolIds.mapNotNull { toolById[it] } }
    val recentTools = remember(recentToolIds, toolById) { recentToolIds.mapNotNull { toolById[it] } }

    val filteredTools = remember(allTools, searchQuery, activeCategory) {
        allTools.filter { tool ->
            val catOk = activeCategory == StudioCategory.ALL || tool.category == activeCategory
            val q = searchQuery.trim()
            val textOk = q.isBlank() ||
                tool.title.contains(q, ignoreCase = true) ||
                tool.subtitle.contains(q, ignoreCase = true) ||
                tool.category.label.contains(q, ignoreCase = true)
            catOk && textOk
        }
    }

    val groupedSections = remember(filteredTools) {
        filteredTools.groupBy { it.category }
    }

    fun launchTool(tool: StudioToolShortcut) {
        onRecordRecentTool(tool.id)
        tool.onClick()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_hub_screen"),
    ) {
        Gallery56TopBar(
            title = "GALLERY56 STUDIO",
            subtitle = "19 creative, AI, video & privacy labs",
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        placeholder = { Text("Search tools, labs, security...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_search_input"),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StudioCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = activeCategory == cat,
                                onClick = { activeCategory = cat },
                                label = { Text(cat.label) },
                            )
                        }
                    }
                }
            }

            if (searchQuery.isBlank() && activeCategory == StudioCategory.ALL) {
                if (pinnedTools.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pinned Favorites",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.semantic.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            pinnedTools.forEach { tool ->
                                GlassPanel(
                                    onClick = { launchTool(tool) },
                                    contentPadding = 12.dp,
                                    modifier = Modifier.width(156.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(tool.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = tool.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = tokens.semantic.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = tool.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.semantic.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                if (recentTools.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recently Used",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.semantic.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            recentTools.forEach { tool ->
                                FilterChip(
                                    selected = false,
                                    onClick = { launchTool(tool) },
                                    leadingIcon = { Icon(tool.icon, null, modifier = Modifier.size(16.dp)) },
                                    label = { Text(tool.title) },
                                )
                            }
                        }
                    }
                }
            }

            groupedSections.forEach { (category, tools) ->
                item(key = "sec_${category.name}") {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = tokens.semantic.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    ToolGrid(
                        tools = tools,
                        pinnedIds = pinnedToolIds.toSet(),
                        onLaunch = ::launchTool,
                        onTogglePin = onTogglePinTool,
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ToolGrid(
    tools: List<StudioToolShortcut>,
    pinnedIds: Set<String>,
    onLaunch: (StudioToolShortcut) -> Unit,
    onTogglePin: (String) -> Unit,
) {
    val tokens = LocalGallery56Tokens.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tools.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { tool ->
                    val isPinned = tool.id in pinnedIds
                    GlassPanel(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(tool.testTag),
                        onClick = { onLaunch(tool) },
                        contentPadding = 12.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            IconButton(
                                onClick = { onTogglePin(tool.id) },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PushPin,
                                    contentDescription = if (isPinned) "Unpin tool" else "Pin tool",
                                    tint = if (isPinned) MaterialTheme.colorScheme.primary else tokens.semantic.iconSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = tokens.semantic.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tool.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.semantic.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
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
