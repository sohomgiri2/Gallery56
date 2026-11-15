package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Gallery56Tokens(
    val preset: GalleryThemePreset,
    val glassSurface: Color,
    val glassBorder: Color,
    val accentGlow: Color,
    val elevatedSurface: Color,
    val subtleDivider: Color,
    val thumbnailCornerRadius: Dp = 14.dp,
    val gridSpacing: Dp = 8.dp,
    val reducedMotion: Boolean = false,
)

val LocalGallery56Tokens = staticCompositionLocalOf {
    val preset = GalleryThemePreset.OBSIDIAN_TITANIUM
    Gallery56Tokens(
        preset = preset,
        glassSurface = preset.surface.copy(alpha = 0.84f),
        glassBorder = preset.cardBorder,
        accentGlow = preset.primary.copy(alpha = 0.22f),
        elevatedSurface = preset.surfaceElevated,
        subtleDivider = preset.cardBorder.copy(alpha = 0.18f),
    )
}

@Composable
fun Gallery56Theme(
    preset: GalleryThemePreset = GalleryThemePreset.OBSIDIAN_TITANIUM,
    customProfile: CustomThemeProfile? = null,
    customAccentHexOverride: String = "",
    cornerRadiusDp: Int = 14,
    gridSpacingDp: Int = 8,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = customProfile?.isDark ?: preset.isDark
    val resolvedPrimary = parseHexColorOrNull(customAccentHexOverride)
        ?: customProfile?.let { parseHexColorOrNull(it.primaryHex) }
        ?: preset.primary
    val resolvedSecondary = customProfile?.let { parseHexColorOrNull(it.secondaryHex) } ?: preset.secondary
    val resolvedTertiary = customProfile?.let { parseHexColorOrNull(it.tertiaryHex) } ?: preset.tertiary
    val resolvedBg = customProfile?.let { parseHexColorOrNull(it.backgroundHex) } ?: preset.background
    val resolvedSurface = customProfile?.let { parseHexColorOrNull(it.surfaceHex) } ?: preset.surface
    val resolvedElevated = if (isDark) {
        resolvedSurface.copy(
            red = (resolvedSurface.red + 0.04f).coerceAtMost(1f),
            green = (resolvedSurface.green + 0.04f).coerceAtMost(1f),
            blue = (resolvedSurface.blue + 0.05f).coerceAtMost(1f),
        )
    } else {
        preset.surfaceElevated
    }
    val onPrimary = if (resolvedPrimary.luminance() > 0.45f) Color(0xFF070A0F) else Color.White
    val onText = if (isDark) Color(0xFFF4F7FB) else Color(0xFF0F172A)
    val onTextSecondary = if (isDark) Color(0xFF94A0B2) else Color(0xFF475569)
    val border = resolvedPrimary.copy(alpha = 0.24f)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = resolvedPrimary,
            onPrimary = onPrimary,
            primaryContainer = resolvedPrimary.copy(alpha = 0.18f),
            onPrimaryContainer = resolvedPrimary,
            secondary = resolvedSecondary,
            onSecondary = resolvedBg,
            secondaryContainer = resolvedElevated,
            onSecondaryContainer = onText,
            tertiary = resolvedTertiary,
            onTertiary = resolvedBg,
            tertiaryContainer = resolvedTertiary.copy(alpha = 0.16f),
            onTertiaryContainer = resolvedTertiary,
            background = resolvedBg,
            onBackground = onText,
            surface = resolvedSurface,
            onSurface = onText,
            surfaceVariant = resolvedElevated,
            onSurfaceVariant = onTextSecondary,
            outline = border,
            outlineVariant = border.copy(alpha = 0.45f),
            error = StatusRose,
            onError = Color(0xFF1F0308),
        )
    } else {
        lightColorScheme(
            primary = resolvedPrimary,
            onPrimary = onPrimary,
            primaryContainer = resolvedPrimary.copy(alpha = 0.12f),
            onPrimaryContainer = resolvedPrimary,
            secondary = resolvedSecondary,
            onSecondary = Color.White,
            secondaryContainer = resolvedElevated,
            onSecondaryContainer = onText,
            tertiary = resolvedTertiary,
            onTertiary = Color.White,
            background = resolvedBg,
            onBackground = onText,
            surface = resolvedSurface,
            onSurface = onText,
            surfaceVariant = resolvedElevated,
            onSurfaceVariant = onTextSecondary,
            outline = border,
            outlineVariant = border.copy(alpha = 0.35f),
            error = Color(0xFFDC2626),
            onError = Color.White,
        )
    }

    val radius = cornerRadiusDp.coerceIn(0, 24)
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((radius * 0.55f).dp),
        small = RoundedCornerShape((radius * 0.85f).dp),
        medium = RoundedCornerShape((radius + 4).dp),
        large = RoundedCornerShape((radius + 10).dp),
        extraLarge = RoundedCornerShape((radius + 16).dp),
    )

    val tokens = Gallery56Tokens(
        preset = preset,
        glassSurface = resolvedSurface.copy(alpha = if (isDark) 0.86f else 0.94f),
        glassBorder = border,
        accentGlow = resolvedPrimary.copy(alpha = 0.22f),
        elevatedSurface = resolvedElevated,
        subtleDivider = onTextSecondary.copy(alpha = 0.14f),
        thumbnailCornerRadius = radius.dp,
        gridSpacing = gridSpacingDp.coerceIn(2, 16).dp,
        reducedMotion = reducedMotion,
    )

    CompositionLocalProvider(LocalGallery56Tokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Gallery56Typography,
            shapes = shapes,
            content = content,
        )
    }
}
