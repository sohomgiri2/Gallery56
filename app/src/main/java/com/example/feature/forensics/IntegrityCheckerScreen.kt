package com.example.feature.forensics

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.Gallery56TopBar
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.core.media.MediaStoreOperations
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

enum class FileIntegrityStatus {
    VERIFIED_HASH,
    READABLE_UNHASHED,
    HASH_MISMATCH,
    UNREADABLE_MISSING,
}

data class IntegrityAuditResult(
    val media: MediaIndexEntity,
    val status: FileIntegrityStatus,
    val computedSha256Prefix: String,
)

object FileIntegrityEngine {

    suspend fun verifyMediaItem(context: Context, item: MediaIndexEntity): IntegrityAuditResult =
        withContext(Dispatchers.IO) {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                var bytesRead = 0L
                val opened = context.contentResolver.openInputStream(Uri.parse(item.contentUri))?.use { input ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        digest.update(buf, 0, n)
                        bytesRead += n
                    }
                    true
                } ?: false

                if (!opened || bytesRead == 0L) {
                    return@withContext IntegrityAuditResult(item, FileIntegrityStatus.UNREADABLE_MISSING, "")
                }

                val hex = digest.digest().joinToString("") { "%02x".format(it) }
                val status = when {
                    item.sha256Hash.isBlank() -> FileIntegrityStatus.READABLE_UNHASHED
                    item.sha256Hash.equals(hex, ignoreCase = true) -> FileIntegrityStatus.VERIFIED_HASH
                    else -> FileIntegrityStatus.HASH_MISMATCH
                }
                IntegrityAuditResult(item, status, hex.take(16))
            } catch (_: Throwable) {
                IntegrityAuditResult(item, FileIntegrityStatus.UNREADABLE_MISSING, "")
            }
        }
}

@Composable
fun IntegrityCheckerScreen(
    allMedia: List<MediaIndexEntity>,
    onRefreshMediaStore: () -> Unit,
    onOpenMedia: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokens = LocalGallery56Tokens.current

    val gpsExposedItems = remember(allMedia) { allMedia.filter { !it.isVideo && it.hasGps } }
    var auditResults by remember { mutableStateOf<List<IntegrityAuditResult>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var isCleaningGps by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("integrity_checker_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Gallery56TopBar(
                title = "PRIVACY & INTEGRITY LAB",
                subtitle = "Batch GPS EXIF scrubber & SHA-256 file verification",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = tokens.semantic.iconPrimary)
                    }
                },
            )
        }

        if (isScanning || isCleaningGps) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        // Batch Privacy Cleaner Card
        item {
            GlassPanel(
                borderColor = if (gpsExposedItems.isNotEmpty()) StatusAmber.copy(alpha = 0.55f) else StatusEmerald.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.GpsFixed, null, tint = if (gpsExposedItems.isNotEmpty()) StatusAmber else StatusEmerald)
                    Text("Privacy Cleaner (EXIF GPS & Camera Strip)", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (gpsExposedItems.isEmpty()) {
                        "No GPS-exposed photos found in your indexed library."
                    } else {
                        "${gpsExposedItems.size} photo(s) embed exact latitude/longitude GPS coordinates. Export zero-location clean copies safely."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.semantic.textSecondary,
                )
                if (gpsExposedItems.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        enabled = !isCleaningGps,
                        onClick = {
                            scope.launch {
                                isCleaningGps = true
                                var cleaned = 0
                                gpsExposedItems.take(25).forEach { item ->
                                    if (MediaStoreOperations.stripGpsAndSavePrivacyCopy(context, item) != null) {
                                        cleaned++
                                    }
                                }
                                isCleaningGps = false
                                onRefreshMediaStore()
                                Toast.makeText(
                                    context,
                                    "Created $cleaned GPS-stripped copy(s) in Pictures/Gallery56_CleanExport",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.CleaningServices, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Batch Export Privacy-Clean Copies (${gpsExposedItems.take(25).size})")
                    }
                }
            }
        }

        // SHA-256 Integrity Checker Card
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
                    Text("SHA-256 File Integrity Audit", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Reads media byte streams and validates SHA-256 checksums against Room index records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.semantic.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    enabled = allMedia.isNotEmpty() && !isScanning,
                    onClick = {
                        scope.launch {
                            isScanning = true
                            auditResults = allMedia.take(50).map { item ->
                                FileIntegrityEngine.verifyMediaItem(context, item)
                            }
                            isScanning = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Security, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isScanning) "Auditing Files..." else "Run Library Integrity Scan")
                }

                if (auditResults.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    val okCount = auditResults.count { it.status == FileIntegrityStatus.VERIFIED_HASH || it.status == FileIntegrityStatus.READABLE_UNHASHED }
                    val warnCount = auditResults.count { it.status == FileIntegrityStatus.HASH_MISMATCH || it.status == FileIntegrityStatus.UNREADABLE_MISSING }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill("Healthy ($okCount)", Icons.Rounded.CheckCircle, StatusEmerald)
                        if (warnCount > 0) {
                            MetricPill("Issues ($warnCount)", Icons.Rounded.Warning, StatusRose)
                        }
                    }
                }
            }
        }

        items(auditResults, key = { it.media.mediaStoreId }) { res ->
            val badgeColor = when (res.status) {
                FileIntegrityStatus.VERIFIED_HASH -> StatusEmerald
                FileIntegrityStatus.READABLE_UNHASHED -> MaterialTheme.colorScheme.primary
                FileIntegrityStatus.HASH_MISMATCH, FileIntegrityStatus.UNREADABLE_MISSING -> StatusRose
            }
            GlassPanel(
                onClick = { onOpenMedia(res.media.mediaStoreId) },
                contentPadding = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(res.media.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(
                            text = if (res.computedSha256Prefix.isNotBlank()) "SHA-256: ${res.computedSha256Prefix}…" else "Stream unreadable",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.semantic.textSecondary,
                        )
                    }
                    MetricPill(
                        label = when (res.status) {
                            FileIntegrityStatus.VERIFIED_HASH -> "Verified"
                            FileIntegrityStatus.READABLE_UNHASHED -> "Readable"
                            FileIntegrityStatus.HASH_MISMATCH -> "Modified"
                            FileIntegrityStatus.UNREADABLE_MISSING -> "Missing"
                        },
                        accentColor = badgeColor,
                    )
                }
            }
        }
    }
}
