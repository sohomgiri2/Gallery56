package com.example.feature.forensics

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.PhotoSizeSelectLarge
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.database.MediaIndexEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.formatBytesReadable
import com.example.core.media.MediaStoreOperations
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MetadataViewerScreen(
    media: MediaIndexEntity?,
    onBack: () -> Unit,
    onRefreshMediaStore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateFmt = remember { DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("metadata_viewer_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("PHOTO METADATA & FORENSICS", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = media?.displayName ?: "Unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (media == null) {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("Media metadata could not be loaded.")
                }
            }
        } else {
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.PhotoSizeSelectLarge, null, tint = MaterialTheme.colorScheme.primary)
                        Text("File & Resolution", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    MetaRow("File Name", media.fileName)
                    MetaRow("Format / MIME", "${media.format} (${media.mimeType})")
                    MetaRow("Resolution", "${media.resolutionLabel} (${String.format(Locale.US, "%.2f", media.megaPixels)} MP)")
                    MetaRow("File Size", "${formatBytesReadable(media.fileSizeBytes)} (${media.fileSizeBytes} bytes)")
                    MetaRow("Album", media.albumName)
                    MetaRow("Folder Path", media.relativePath)
                    MetaRow("Date Taken", dateFmt.format(Date(media.dateTakenMillis)))
                    MetaRow("Date Modified", dateFmt.format(Date(media.dateModifiedMillis)))
                }
            }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = MaterialTheme.colorScheme.primary)
                        Text("EXIF Optics & Exposure", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    MetaRow("Camera Make", media.cameraMake.ifBlank { "Unavailable" })
                    MetaRow("Camera Model", media.cameraModel.ifBlank { "Unavailable" })
                    MetaRow("Aperture", media.aperture.ifBlank { "Unavailable" })
                    MetaRow("ISO Sensitivity", if (media.iso > 0) "ISO ${media.iso}" else "Unavailable")
                    MetaRow("Shutter Speed", media.exposureTime.ifBlank { "Unavailable" })
                    MetaRow("Focal Length", media.focalLength.ifBlank { "Unavailable" })
                    MetaRow("Orientation Tag", media.orientation.toString())
                }
            }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (media.hasGps) Icons.Rounded.GpsFixed else Icons.Rounded.GpsOff,
                            null,
                            tint = if (media.hasGps) StatusAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("GPS & Privacy Export", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (media.hasGps && media.latitude != null && media.longitude != null) {
                        MetaRow("Latitude", String.format(Locale.US, "%.6f", media.latitude))
                        MetaRow("Longitude", String.format(Locale.US, "%.6f", media.longitude))
                    } else {
                        Text(
                            text = "No GPS coordinates embedded in this file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!media.isVideo) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val saved = MediaStoreOperations.stripGpsAndSavePrivacyCopy(context, media)
                                    if (saved != null) {
                                        onRefreshMediaStore()
                                        Toast.makeText(
                                            context,
                                            "Saved metadata-free copy to Pictures/Gallery56_CleanExport",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "Unable to export clean copy", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("strip_exif_copy_btn"),
                        ) {
                            Icon(Icons.Rounded.GpsOff, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save EXIF & GPS-Stripped Copy")
                        }
                    }
                }
            }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Fingerprint, null, tint = StatusEmerald)
                        Text("Cryptographic & Perceptual Hashes", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    MetaRow("SHA-256", media.sha256Hash.ifBlank { "Run AI Index to compute" })
                    MetaRow("64-bit dHash", if (media.dHash != 0L) "0x${media.dHash.toULong().toString(16)}" else "Not computed")
                    MetaRow("Sharpness (Laplacian)", String.format(Locale.US, "%.1f", media.sharpnessScore))
                    MetaRow("Quality Score", "${media.qualityScore.toInt()} / 100")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Note: File hashes verify integrity against exact byte modifications; metadata alone does not prove photographic authenticity.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(0.58f),
        )
    }
}
