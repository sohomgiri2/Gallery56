package com.example.feature.vault

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.VaultItemEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.core.designsystem.formatBytesReadable
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose

@Composable
fun PrivateVaultScreen(
    uiState: PrivateVaultUiState,
    onCreatePin: (String, String) -> Unit,
    onUnlockWithPin: (String) -> Unit,
    onBiometricSuccess: () -> Unit,
    onLockNow: () -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetAutoLockSeconds: (Int) -> Unit,
    onImportPickerUris: (List<android.net.Uri>) -> Unit,
    onImportLibraryItems: (List<com.example.core.database.MediaIndexEntity>) -> Unit,
    onOpenPreview: (VaultItemEntity) -> Unit,
    onClosePreview: () -> Unit,
    onExportFromVault: (VaultItemEntity) -> Unit,
    onDeletePermanently: (VaultItemEntity) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity
    val fragmentActivity = context as? FragmentActivity

    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var showSecuritySheet by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var newConfirmPin by remember { mutableStateOf("") }
    var showLibraryImport by remember { mutableStateOf(false) }
    var selectedLibraryIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(20),
    ) { uris ->
        if (uris.isNotEmpty()) onImportPickerUris(uris)
    }

    // Enforce FLAG_SECURE while vault is unlocked to block screenshots/recents thumbnails
    DisposableEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Automatic lock when app moves to background
    DisposableEffect(lifecycleOwner, uiState.preferences.vaultAutoLockSeconds) {
        var backgroundedAt = 0L
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundedAt = System.currentTimeMillis()
                    if (uiState.preferences.vaultAutoLockSeconds <= 15) {
                        onLockNow()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (backgroundedAt > 0L) {
                        val elapsed = (System.currentTimeMillis() - backgroundedAt) / 1000L
                        if (elapsed >= uiState.preferences.vaultAutoLockSeconds) {
                            onLockNow()
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val biometricSupported = remember(context) {
        val mgr = BiometricManager.from(context)
        mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun launchBiometricPrompt() {
        val host = fragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(host)
        val prompt = BiometricPrompt(
            host,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricSuccess()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Gallery56 Private Vault")
            .setSubtitle("Authenticate to access AES-256-GCM encrypted media")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(info)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("private_vault_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    onLockNow()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("PRIVATE VAULT", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "AndroidKeyStore AES-256-GCM encrypted sandbox",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (uiState.isUnlocked) {
                Row {
                    IconButton(onClick = { showSecuritySheet = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Vault security")
                    }
                    IconButton(onClick = onLockNow) {
                        Icon(Icons.Rounded.Lock, contentDescription = "Lock vault")
                    }
                }
            }
        }

        if (!uiState.isUnlocked) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (uiState.preferences.hasVaultPin) "Enter Vault PIN" else "Create Vault PIN",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (uiState.preferences.hasVaultPin) {
                            "Media inside Private Vault is encrypted with hardware-backed AES-256-GCM keys."
                        } else {
                            "Set a 4-8 digit PIN. PINs are salted and hashed with 10,000 rounds of SHA-256."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pinInput = it },
                        label = { Text("PIN (4-8 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_pin_input"),
                    )

                    if (!uiState.preferences.hasVaultPin) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirmPinInput = it },
                            label = { Text("Confirm PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_pin_confirm_input"),
                        )
                    }

                    uiState.errorNote?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall, color = StatusRose)
                    }

                    if (uiState.lockoutRemainingSeconds > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Too many attempts. Try again in ${uiState.lockoutRemainingSeconds}s.",
                            style = MaterialTheme.typography.labelLarge,
                            color = StatusRose,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            enabled = uiState.lockoutRemainingSeconds == 0,
                            onClick = {
                                if (uiState.preferences.hasVaultPin) {
                                    onUnlockWithPin(pinInput)
                                } else {
                                    onCreatePin(pinInput, confirmPinInput)
                                }
                                pinInput = ""
                                confirmPinInput = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("vault_unlock_button"),
                        ) {
                            Icon(Icons.Rounded.LockOpen, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (uiState.preferences.hasVaultPin) "Unlock" else "Set PIN & Enter")
                        }

                        if (uiState.preferences.hasVaultPin && biometricSupported && uiState.preferences.vaultBiometricEnabled) {
                            OutlinedButton(onClick = ::launchBiometricPrompt) {
                                Icon(Icons.Rounded.Fingerprint, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Biometric")
                            }
                        }
                    }
                }
            }
        } else {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricPill("${uiState.items.size} Encrypted", Icons.Rounded.Security, StatusEmerald)
                        MetricPill(formatBytesReadable(uiState.totalVaultBytes), Icons.Rounded.Shield)
                    }
                    Text(
                        text = "FLAG_SECURE ON",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusEmerald,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vault_import_picker"),
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import Files")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedLibraryIds = emptySet()
                            showLibraryImport = true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("From Library")
                    }
                }
                uiState.statusNote?.let { note ->
                    Spacer(Modifier.height(6.dp))
                    Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (uiState.items.isEmpty()) {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("Your Private Vault is empty.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Import photos or videos above. Each file is encrypted with AES-256-GCM inside app-private storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(uiState.items, key = { it.vaultId }) { item ->
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenPreview(item) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        imageVector = if (item.isVideo) Icons.Rounded.Videocam else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Column {
                                        Text(
                                            text = item.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "AES-256-GCM • ${formatBytesReadable(item.fileSizeBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = { onExportFromVault(item) }) {
                                        Icon(Icons.Rounded.DriveFileMove, contentDescription = "Export out of vault")
                                    }
                                    IconButton(onClick = { onDeletePermanently(item) }) {
                                        Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete encrypted", tint = StatusRose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val preview = uiState.previewItem
    if (preview != null) {
        AlertDialog(
            onDismissRequest = onClosePreview,
            title = { Text(preview.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val bmp = uiState.decryptedPreviewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = preview.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    } else {
                        Text(
                            text = if (preview.isVideo) {
                                "Encrypted video (${formatBytesReadable(preview.fileSizeBytes)}). Export to MediaStore to play in full player."
                            } else {
                                "Decrypted preview unavailable."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onExportFromVault(preview) }) {
                    Text("Export to Gallery")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { onDeletePermanently(preview) },
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusRose),
                    ) {
                        Text("Shred")
                    }
                    TextButton(onClick = onClosePreview) {
                        Text("Close")
                    }
                }
            },
        )
    }

    if (showSecuritySheet) {
        AlertDialog(
            onDismissRequest = { showSecuritySheet = false },
            title = { Text("Vault Security Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Biometric Unlock")
                        Switch(
                            checked = uiState.preferences.vaultBiometricEnabled,
                            onCheckedChange = onSetBiometricEnabled,
                            enabled = biometricSupported,
                        )
                    }
                    Text("Auto-Lock Timeout", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10 to "10s", 60 to "1m", 300 to "5m").forEach { (sec, label) ->
                            FilterChip(
                                selected = uiState.preferences.vaultAutoLockSeconds == sec,
                                onClick = { onSetAutoLockSeconds(sec) },
                                label = { Text(label) },
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            showSecuritySheet = false
                            newPin = ""
                            newConfirmPin = ""
                            showChangePinDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Change Vault PIN")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecuritySheet = false }) { Text("Done") }
            },
        )
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Vault PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("New PIN (4-8 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = newConfirmPin,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) newConfirmPin = it },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreatePin(newPin, newConfirmPin)
                        showChangePinDialog = false
                    },
                ) { Text("Update PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showLibraryImport) {
        AlertDialog(
            onDismissRequest = { showLibraryImport = false },
            title = { Text("Encrypt From Library (${selectedLibraryIds.size})") },
            text = {
                if (uiState.libraryPhotos.isEmpty()) {
                    Text("No indexed library media available.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(uiState.libraryPhotos, key = { it.mediaStoreId }) { item ->
                            val selected = item.mediaStoreId in selectedLibraryIds
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable {
                                        selectedLibraryIds = if (selected) {
                                            selectedLibraryIds - item.mediaStoreId
                                        } else {
                                            selectedLibraryIds + item.mediaStoreId
                                        }
                                    },
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.contentUri)
                                        .size(200)
                                        .build(),
                                    contentDescription = item.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedLibraryIds.isNotEmpty(),
                    onClick = {
                        val chosen = uiState.libraryPhotos.filter { it.mediaStoreId in selectedLibraryIds }
                        onImportLibraryItems(chosen)
                        showLibraryImport = false
                    },
                ) { Text("Encrypt Selected") }
            },
            dismissButton = {
                TextButton(onClick = { showLibraryImport = false }) { Text("Cancel") }
            },
        )
    }
}
