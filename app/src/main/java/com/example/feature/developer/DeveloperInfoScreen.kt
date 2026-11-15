package com.example.feature.developer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.ui.theme.StatusEmerald

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperInfoScreen(
    onBack: () -> Unit,
) {
    val openSourceLibs = listOf(
        "Jetpack Compose & Material 3 (Apache 2.0)",
        "AndroidX Room + FTS4 + KSP (Apache 2.0)",
        "AndroidX DataStore Preferences (Apache 2.0)",
        "AndroidX Media3 ExoPlayer (Apache 2.0)",
        "AndroidX Biometric & Security Crypto (Apache 2.0)",
        "AndroidX WorkManager & ExifInterface (Apache 2.0)",
        "Coil & Coil-Video (Apache 2.0)",
        "Google ML Kit On-Device Vision & Text Recognition (Google APIs TOS)",
        "Kotlinx Serialization JSON (Apache 2.0)",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("developer_info_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("DEVELOPER INFORMATION", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Gallery56 • Official Identity & Architecture",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Gallery56", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "BEYOND GALLERY. BEYOND IMAGINATION.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text("Developer: Sohom Giri", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Role: Independent Android Developer / Creative Developer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Instagram: @itz_sg_063",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MetricPill("100% Native Kotlin", Icons.Rounded.Code)
                    MetricPill("Offline-First", Icons.Rounded.Security, StatusEmerald)
                    MetricPill("Zero Cloud Uploads", Icons.Rounded.Lock, StatusEmerald)
                }
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Application Build Details", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                DetailRow("Application Name", "Gallery56")
                DetailRow("Package ID", BuildConfig.APPLICATION_ID)
                DetailRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                DetailRow("UI Engine", "Jetpack Compose Material 3")
                DetailRow("Media Storage", "Android MediaStore + Photo Picker")
                DetailRow("Vault Encryption", "AndroidKeyStore AES-256-GCM + Salted SHA-256")
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Privacy & On-Device Guarantee", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Gallery56 performs media indexing, OCR text recognition, perceptual hashing, quality scoring, face geometry clustering, and AES-256-GCM vault encryption entirely on-device. No photos, face vectors, or search queries ever leave your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("Open-Source Licenses", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                openSourceLibs.forEach { lib ->
                    Text(
                        text = "• $lib",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}
