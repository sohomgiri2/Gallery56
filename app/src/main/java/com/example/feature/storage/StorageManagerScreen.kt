package com.example.feature.storage

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MediaThumbnailTile
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.formatBytesReadable
import com.example.core.media.TrashRepository
import com.example.core.security.VaultCryptoManager
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalCoilApi::class)
@Composable
fun StorageManagerScreen(
    allMedia: List<MediaIndexEntity>,
    vaultCryptoManager: VaultCryptoManager,
    trashRepository: TrashRepository,
    onOpenMedia: (Long) -> Unit,
    onMoveToTrash: (MediaIndexEntity) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cacheBytes by remember { mutableLongStateOf(directorySizeBytes(context.cacheDir)) }
    val vaultBytes = remember(allMedia) { vaultCryptoManager.totalVaultStorageBytes() }
    val trashBytes = remember(allMedia) { trashRepository.totalTrashBytes() }

    val photos = remember(allMedia) { allMedia.filter { !it.isVideo } }
    val videos = remember(allMedia) { allMedia.filter { it.isVideo } }
    val photosBytes = remember(photos) { photos.sumOf { it.fileSizeBytes } }
    val videosBytes = remember(videos) { videos.sumOf { it.fileSizeBytes } }
    val totalIndexedBytes = (photosBytes + videosBytes + vaultBytes + trashBytes).coerceAtLeast(1L)

    val largeMedia = remember(allMedia) {
        allMedia.filter { it.fileSizeBytes >= 3_000_000L }
            .sortedByDescending { it.fileSizeBytes }
            .ifEmpty { allMedia.sortedByDescending { it.fileSizeBytes }.take(12) }
            .take(25)
    }

    val duplicateGroups = remember(allMedia) {
        allMedia
            .filter { it.sha256Hash.isNotBlank() }
            .groupBy { it.sha256Hash }
            .values
            .filter { it.size > 1 }
            .sortedByDescending { it.sumOf { item -> item.fileSizeBytes } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("storage_manager_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("STORAGE MANAGER", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Indexed media analytics, large files & app cache",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.SdStorage, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Total Tracked Footprint: ${formatBytesReadable(photosBytes + videosBytes + vaultBytes + trashBytes)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(10.dp))

                StorageBarRow(
                    label = "Photos (${photos.size})",
                    bytes = photosBytes,
                    fraction = (photosBytes.toFloat() / totalIndexedBytes.toFloat()).coerceIn(0f, 1f),
                    icon = Icons.Rounded.PhotoLibrary,
                )
                Spacer(Modifier.height(8.dp))
                StorageBarRow(
                    label = "Videos (${videos.size})",
                    bytes = videosBytes,
                    fraction = (videosBytes.toFloat() / totalIndexedBytes.toFloat()).coerceIn(0f, 1f),
                    icon = Icons.Rounded.Videocam,
                )
                Spacer(Modifier.height(8.dp))
                StorageBarRow(
                    label = "Encrypted Vault",
                    bytes = vaultBytes,
                    fraction = (vaultBytes.toFloat() / totalIndexedBytes.toFloat()).coerceIn(0f, 1f),
                    icon = Icons.Rounded.Lock,
                )
                Spacer(Modifier.height(8.dp))
                StorageBarRow(
                    label = "Recently Deleted Trash",
                    bytes = trashBytes,
                    fraction = (trashBytes.toFloat() / totalIndexedBytes.toFloat()).coerceIn(0f, 1f),
                    icon = Icons.Rounded.DeleteOutline,
                )
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Cache & Thumbnails", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Current cache: ${formatBytesReadable(cacheBytes)} (clearing never affects your photos)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val freed = withContext(Dispatchers.IO) {
                                    val before = directorySizeBytes(context.cacheDir)
                                    Coil.imageLoader(context).memoryCache?.clear()
                                    Coil.imageLoader(context).diskCache?.clear()
                                    context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                                    val after = directorySizeBytes(context.cacheDir)
                                    cacheBytes = after
                                    (before - after).coerceAtLeast(0L)
                                }
                                Toast.makeText(
                                    context,
                                    "Cleared ${formatBytesReadable(freed)} of app cache",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        modifier = Modifier.testTag("storage_clear_cache"),
                    ) {
                        Icon(Icons.Rounded.CleaningServices, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Cache")
                    }
                }
            }
        }

        if (duplicateGroups.isNotEmpty()) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = StatusAmber)
                        Text(
                            text = "Exact SHA-256 Duplicates (${duplicateGroups.size} groups)",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    duplicateGroups.take(5).forEachIndexed { idx, group ->
                        val waste = group.drop(1).sumOf { it.fileSizeBytes }
                        Text(
                            text = "Group #${idx + 1} • ${group.size} copies • Reclaimable ${formatBytesReadable(waste)}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(group, key = { it.mediaStoreId }) { media ->
                                MediaThumbnailTile(
                                    media = media,
                                    badgeText = formatBytesReadable(media.fileSizeBytes),
                                    onClick = { onOpenMedia(media.mediaStoreId) },
                                    modifier = Modifier.size(92.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item {
            Text(
                text = "Largest Media Files (${largeMedia.size})",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (largeMedia.isEmpty()) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("No indexed media found yet.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            items(largeMedia, key = { it.mediaStoreId }) { item ->
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenMedia(item.mediaStoreId) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.contentUri)
                                .size(180)
                                .build(),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${item.resolutionLabel} • ${item.albumName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MetricPill(
                            label = formatBytesReadable(item.fileSizeBytes),
                            accentColor = if (item.fileSizeBytes > 25_000_000L) StatusAmber else StatusEmerald,
                        )
                        IconButton(onClick = { onMoveToTrash(item) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Move to trash", tint = StatusRose)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageBarRow(
    label: String,
    bytes: Long,
    fraction: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
            Text(formatBytesReadable(bytes), style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
    }
}

private fun directorySizeBytes(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    if (dir.isFile) return dir.length()
    var sum = 0L
    dir.listFiles()?.forEach { child ->
        sum += directorySizeBytes(child)
    }
    return sum
}
