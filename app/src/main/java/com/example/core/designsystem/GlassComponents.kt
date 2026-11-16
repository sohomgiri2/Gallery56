package com.example.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.database.MediaIndexEntity
import com.example.ui.theme.LocalGallery56Tokens
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusRose
import com.example.ui.theme.ensureReadableTextColor
import java.util.Locale

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: Dp = 16.dp,
    borderColor: Color = LocalGallery56Tokens.current.semantic.cardBorder,
    containerColor: Color = LocalGallery56Tokens.current.glassSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalGallery56Tokens.current
    val resolvedContentColor = ensureReadableTextColor(
        candidate = tokens.semantic.textPrimary,
        background = containerColor,
        isDarkSurface = tokens.preset.isDark || tokens.isAmoledMode,
    )
    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier,
        shape = shape,
        color = containerColor,
        contentColor = resolvedContentColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = tokens.cardElevation,
    ) {
        CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}

/**
 * Single-line responsive top bar guaranteeing GALLERY56 never wraps into two lines
 * and never overlaps status bar icons or crowds compact widths.
 */
@Composable
fun Gallery56TopBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    primaryActions: @Composable RowScope.() -> Unit = {},
    overflowContent: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGallery56Tokens.current
    var showOverflow by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        val compactWidth = maxWidth < 380.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (navigationIcon != null) {
                    navigationIcon()
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = if (compactWidth) {
                            MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        },
                        color = tokens.semantic.textPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("app_header_title"),
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.semantic.textSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                primaryActions()
                if (overflowContent != null) {
                    Box {
                        IconButton(
                            onClick = { showOverflow = true },
                            modifier = Modifier.testTag("topbar_overflow_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More actions",
                                tint = tokens.semantic.iconPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false },
                        ) {
                            overflowContent { showOverflow = false }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalGallery56Tokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = tokens.semantic.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.semantic.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accentColor.copy(alpha = 0.16f))
            .border(0.75.dp, accentColor.copy(alpha = 0.38f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MediaThumbnailTile(
    media: MediaIndexEntity,
    badgeText: String? = null,
    isSelected: Boolean = false,
    aspectRatio: Float = 1f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlayBottomEnd: (@Composable BoxScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val tokens = LocalGallery56Tokens.current
    val tileShape = tokens.thumbnailShape()

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(tileShape)
            .background(tokens.elevatedSurface)
            .border(
                width = if (isSelected) 2.5.dp else 0.75.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else tokens.glassBorder.copy(alpha = 0.55f),
                shape = tileShape,
            )
            .clickable(onClick = onClick)
            .testTag("media_tile_${media.mediaStoreId}"),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(media.contentUri)
                .crossfade(true)
                .size(400)
                .build(),
            contentDescription = media.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f)),
                    ),
                )
                .padding(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (badgeText != null) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.56f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                } else if (media.isVideo) {
                    Text(
                        text = formatDurationShort(media.durationMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.56f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (media.userRating > 0) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Rated ${media.userRating}",
                            tint = StatusAmber,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    if (media.isFavorite) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Favorite",
                            tint = StatusRose,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        if (media.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f))
                    .padding(3.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (overlayBottomEnd != null) {
            overlayBottomEnd()
        }
    }
}

fun formatDurationShort(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", mins, secs)
}

fun formatBytesReadable(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}
