package com.example.feature.trash

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.TrashItemEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.formatBytesReadable
import com.example.core.media.TrashRepository
import com.example.ui.theme.StatusRose
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun RecentlyDeletedScreen(
    trashItems: List<TrashItemEntity>,
    trashRepository: TrashRepository,
    onRestoredMedia: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmEmpty by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("recently_deleted_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                    Text("RECENTLY DELETED", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "${trashItems.size} item(s) • ${formatBytesReadable(trashItems.sumOf { it.fileSizeBytes })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trashItems.isNotEmpty()) {
                OutlinedButton(
                    onClick = { confirmEmpty = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRose),
                    modifier = Modifier.testTag("trash_empty_button"),
                ) {
                    Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Empty")
                }
            }
        }

        if (trashItems.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Trash bin is empty", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Items moved to Recently Deleted stay cached safely here for 30 days until you restore or permanently erase them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(trashItems, key = { it.trashId }) { item ->
                    val ageDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - item.deletedAtMillis)
                    val remaining = (30L - ageDays).coerceAtLeast(0L)
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(item.cachedFilePath))
                                    .size(200)
                                    .build(),
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(3.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    MetricPill("${remaining}d left")
                                    MetricPill(formatBytesReadable(item.fileSizeBytes))
                                }
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val ok = trashRepository.restoreFromTrash(item)
                                        if (ok) {
                                            Toast.makeText(context, "Restored ${item.displayName}", Toast.LENGTH_SHORT).show()
                                            onRestoredMedia()
                                        }
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.RestoreFromTrash, contentDescription = "Restore")
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        trashRepository.deletePermanently(item)
                                        Toast.makeText(context, "Deleted permanently", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            ) {
                                Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete permanently", tint = StatusRose)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("Empty Recently Deleted?") },
            text = { Text("Permanently erase all ${trashItems.size} trashed item(s)? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmEmpty = false
                        scope.launch { trashRepository.emptyTrash() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRose),
                ) { Text("Delete All") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) { Text("Cancel") }
            },
        )
    }
}
