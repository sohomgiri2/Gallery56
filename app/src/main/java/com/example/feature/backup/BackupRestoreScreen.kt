package com.example.feature.backup

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.RestorePage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import kotlinx.coroutines.launch

@Composable
fun BackupRestoreScreen(
    allMedia: List<MediaIndexEntity>,
    backupRestoreManager: BackupRestoreManager,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingPreview by remember { mutableStateOf<BackupValidationPreview?>(null) }
    var showMediaZipSelector by remember { mutableStateOf(false) }
    var selectedIdsForZip by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = backupRestoreManager.exportJsonToUri(uri)
                statusText = if (ok) "Exported Gallery56 settings & metadata JSON." else "Export failed."
                Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val preview = backupRestoreManager.parseAndValidateBackupUri(uri)
                if (preview == null) {
                    statusText = "Invalid or unreadable Gallery56 JSON backup."
                    Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                } else {
                    pendingPreview = preview
                }
            }
        }
    }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val chosen = allMedia.filter { it.mediaStoreId in selectedIdsForZip }
                val count = backupRestoreManager.exportSelectedMediaZip(uri, chosen)
                statusText = "Archived $count media file(s) into ZIP."
                Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("backup_restore_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("BACKUP & RESTORE", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "SAF offline JSON metadata backup & ZIP media archive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        statusText?.let { msg ->
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Backup, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Settings, Themes, Albums & Tags (JSON)", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Exports your theme presets, custom HEX themes, saved searches, custom albums, and photo tags/ratings/notes via Storage Access Framework. Vault PINs are never exported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            exportJsonLauncher.launch("Gallery56_Backup_${System.currentTimeMillis()}.json")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_export_json"),
                    ) {
                        Text("Export JSON")
                    }
                    OutlinedButton(
                        onClick = {
                            importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backup_import_json"),
                    ) {
                        Icon(Icons.Rounded.RestorePage, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import JSON")
                    }
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Archive, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Selected Media ZIP Archive", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Bundle selected photos or videos into a single portable .zip archive saved to any folder or USB drive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    enabled = allMedia.isNotEmpty(),
                    onClick = {
                        selectedIdsForZip = allMedia.take(10).map { it.mediaStoreId }.toSet()
                        showMediaZipSelector = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Select Media & Export ZIP")
                }
            }
        }
    }

    val preview = pendingPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { pendingPreview = null },
            title = { Text("Restore Validation Preview") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricPill(
                        label = preview.validationNote,
                        accentColor = if (preview.isValid) StatusEmerald else StatusRose,
                    )
                    Text("Custom Themes: ${preview.customThemeCount}")
                    Text("Saved Searches: ${preview.savedSearchCount}")
                    Text("Custom Albums: ${preview.albumCount}")
                    Text("Media Annotations (Tags/Ratings): ${preview.annotationCount}")
                }
            },
            confirmButton = {
                Button(
                    enabled = preview.isValid,
                    onClick = {
                        val b = preview.bundle
                        pendingPreview = null
                        scope.launch {
                            backupRestoreManager.applyBackupBundle(b)
                            statusText = "Backup restored successfully."
                            Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text("Apply Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPreview = null }) { Text("Cancel") }
            },
        )
    }

    if (showMediaZipSelector) {
        val candidates = remember(allMedia) { allMedia.take(80) }
        AlertDialog(
            onDismissRequest = { showMediaZipSelector = false },
            title = { Text("Select Media for ZIP (${selectedIdsForZip.size})") },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(candidates.size) { idx ->
                        val item = candidates[idx]
                        val checked = item.mediaStoreId in selectedIdsForZip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedIdsForZip = if (checked) {
                                        selectedIdsForZip - item.mediaStoreId
                                    } else {
                                        selectedIdsForZip + item.mediaStoreId
                                    }
                                },
                            )
                            Text(
                                text = item.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedIdsForZip.isNotEmpty(),
                    onClick = {
                        showMediaZipSelector = false
                        exportZipLauncher.launch("Gallery56_Media_${System.currentTimeMillis()}.zip")
                    },
                ) {
                    Text("Export ZIP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMediaZipSelector = false }) { Text("Cancel") }
            },
        )
    }
}
