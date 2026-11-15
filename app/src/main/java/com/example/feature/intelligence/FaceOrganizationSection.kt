package com.example.feature.intelligence

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.FaceDetectionEntity
import com.example.core.database.FaceGroupEntity
import com.example.core.designsystem.GlassPanel
import com.example.core.designsystem.MetricPill
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FaceOrganizationSection(
    faceAnalysisEnabled: Boolean,
    faceGroups: List<FaceGroupEntity>,
    faceDetections: List<FaceDetectionEntity>,
    onToggleFaceAnalysis: (Boolean) -> Unit,
    onRunReindexWithFaces: () -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onMergeGroups: (String, String) -> Unit,
    onRemoveFaceFromGroup: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onDeleteAllFaceData: () -> Unit,
    onFilterByFaceGroup: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tokens = LocalGallery56Tokens.current

    var renameTarget by remember { mutableStateOf<FaceGroupEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var editFacesGroup by remember { mutableStateOf<FaceGroupEntity?>(null) }
    var mergeSourceGroup by remember { mutableStateOf<FaceGroupEntity?>(null) }
    var confirmWipeAll by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Privacy & Opt-In Gate Card
        GlassPanel(
            borderColor = if (faceAnalysisEnabled) StatusEmerald.copy(alpha = 0.5f) else tokens.glassBorder,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("face_privacy_card"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PrivacyTip,
                        contentDescription = null,
                        tint = if (faceAnalysisEnabled) StatusEmerald else MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text("On-Device Face Analysis", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (faceAnalysisEnabled) {
                                "Active locally via ML Kit • Optional & User-Managed"
                            } else {
                                "Paused by default for maximum privacy"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = faceAnalysisEnabled,
                    onCheckedChange = onToggleFaceAnalysis,
                    modifier = Modifier.testTag("face_analysis_switch"),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Privacy Guarantee: All face detection and geometric landmark clustering runs 100% locally on your device. Gallery56 never uploads face data, never builds a cloud identity database, and never guesses real names.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (faceAnalysisEnabled) {
                    Button(
                        onClick = onRunReindexWithFaces,
                        modifier = Modifier.testTag("scan_faces_button"),
                    ) {
                        Text("Scan Faces Now")
                    }
                }
                if (faceGroups.isNotEmpty() || faceDetections.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { confirmWipeAll = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRose),
                        modifier = Modifier.testTag("delete_all_face_data_btn"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wipe Face Data")
                    }
                }
            }
        }

        if (!faceAnalysisEnabled && faceGroups.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Enable On-Device Face Analysis above to group faces locally and filter albums by person.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (faceGroups.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No face groups derived yet. Tap 'Scan Faces Now' to detect faces on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            MetricPill(
                label = "${faceGroups.size} local face groups • ${faceDetections.size} detections",
                icon = Icons.Rounded.Face,
            )

            faceGroups.forEachIndexed { index, group ->
                val displayName = group.label.ifBlank { "Person #${index + 1}" }
                var menuOpen by remember { mutableStateOf(false) }

                GlassPanel(
                    contentPadding = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("face_group_card_${group.groupId}"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(tokens.elevatedSurface)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            val cropFile = group.coverCropPath?.let { File(it) }
                            if (cropFile != null && cropFile.exists()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(cropFile)
                                        .build(),
                                    contentDescription = displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Face,
                                    contentDescription = displayName,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${group.faceCount} face detection(s) • " +
                                    if (group.isUserNamed) "User-labeled" else "Unnamed cluster",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        IconButton(
                            onClick = { onFilterByFaceGroup(group.groupId) },
                            modifier = Modifier.testTag("filter_face_group_${group.groupId}"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterAlt,
                                contentDescription = "Filter gallery by $displayName",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { menuOpen = true },
                                modifier = Modifier.testTag("edit_face_group_${group.groupId}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit $displayName",
                                )
                            }
                            DropdownMenu(
                                expanded = menuOpen,
                                onDismissRequest = { menuOpen = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename Label") },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                    onClick = {
                                        menuOpen = false
                                        renameTarget = group
                                        renameText = group.label
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit Faces in Group") },
                                    leadingIcon = { Icon(Icons.Rounded.Face, null) },
                                    onClick = {
                                        menuOpen = false
                                        editFacesGroup = group
                                    },
                                )
                                if (faceGroups.size > 1) {
                                    DropdownMenuItem(
                                        text = { Text("Merge Into Another Group") },
                                        leadingIcon = { Icon(Icons.Rounded.MergeType, null) },
                                        onClick = {
                                            menuOpen = false
                                            mergeSourceGroup = group
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Remove Group", color = StatusRose) },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteForever, null, tint = StatusRose) },
                                    onClick = {
                                        menuOpen = false
                                        onDeleteGroup(group.groupId)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Label Face Group") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Person Label (stored locally only)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameGroup(target.groupId, renameText)
                        renameTarget = null
                    },
                ) {
                    Text("Save Label")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    // Merge Dialog
    mergeSourceGroup?.let { source ->
        val candidates = faceGroups.filter { it.groupId != source.groupId }
        AlertDialog(
            onDismissRequest = { mergeSourceGroup = null },
            title = { Text("Merge Face Group Into…") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    candidates.forEach { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.elevatedSurface)
                                .clickable {
                                    onMergeGroups(source.groupId, target.groupId)
                                    mergeSourceGroup = null
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(target.label.ifBlank { "Group ${target.groupId.takeLast(4)}" })
                            Text("${target.faceCount} faces", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mergeSourceGroup = null }) { Text("Cancel") }
            },
        )
    }

    // Edit Individual Faces Dialog
    editFacesGroup?.let { group ->
        val members = faceDetections.filter { it.groupId == group.groupId }
        AlertDialog(
            onDismissRequest = { editFacesGroup = null },
            title = { Text("Edit Faces in ${group.label.ifBlank { "Group" }}") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    members.forEach { face ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val file = face.cropPath?.let { File(it) }
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tokens.elevatedSurface),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (file != null && file.exists()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(file).build(),
                                        contentDescription = "Detected face",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(Icons.Rounded.Face, null)
                                }
                            }
                            TextButton(
                                onClick = { onRemoveFaceFromGroup(face.faceId) },
                            ) {
                                Icon(Icons.Rounded.PersonRemove, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Split", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editFacesGroup = null }) { Text("Done") }
            },
        )
    }

    // Confirm Wipe All Face Data Dialog
    if (confirmWipeAll) {
        AlertDialog(
            onDismissRequest = { confirmWipeAll = false },
            title = { Text("Delete All Derived Face Data?") },
            text = {
                Text(
                    "This permanently removes all face detections, face crops, face groups, and person labels from Gallery56 and turns off face analysis. Your original photos remain untouched.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllFaceData()
                        confirmWipeAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRose),
                ) {
                    Text("Delete Face Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipeAll = false }) { Text("Cancel") }
            },
        )
    }
}
