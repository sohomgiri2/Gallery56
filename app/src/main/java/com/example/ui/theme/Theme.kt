package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
enum class GalleryTypographyStyle(val label: String) {
    MODERN_GROTESK("Modern Space"),
    EDITORIAL_SERIF("Editorial Serif"),
    TECH_MONO("Nothing Mono"),
    CLASSIC_CLEAN("Minimal Sans"),
}

@Serializable
enum class ThumbnailShapeStyle(val label: String) {
    ROUNDED("Rounded"),
    SQUIRCLE("Squircle"),
    SHARP("Sharp"),
    PILL("Circle"),
}

@Serializable
enum class IconStylePreference(val label: String) {
    ROUNDED("Rounded"),
    OUTLINED("Outlined"),
    TWO_TONE("Editorial"),
}

@Immutable
data class SemanticColors(
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val background: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val buttonPrimary: Color,
    val onButtonPrimary: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
)

@Immutable
data class Gallery56Tokens(
    val preset: GalleryThemePreset,
    val semantic: SemanticColors,
    val glassSurface: Color,
    val glassBorder: Color,
    val accentGlow: Color,
    val elevatedSurface: Color,
    val subtleDivider: Color,
    val thumbnailCornerRadius: Dp = 14.dp,
    val gridSpacing: Dp = 8.dp,
    val cardElevation: Dp = 2.dp,
    val blurIntensity: Float = 0.65f,
    val thumbnailShapeStyle: ThumbnailShapeStyle = ThumbnailShapeStyle.ROUNDED,
    val iconStyle: IconStylePreference = IconStylePreference.ROUNDED,
    val reducedMotion: Boolean = false,
    val isAmoledMode: Boolean = false,
) {
    fun thumbnailShape(): Shape = when (thumbnailShapeStyle) {
        ThumbnailShapeStyle.ROUNDED -> RoundedCornerShape(thumbnailCornerRadius)
        ThumbnailShapeStyle.SQUIRCLE -> RoundedCornerShape((thumbnailCornerRadius * 1.4f).coerceAtMost(28.dp))
        ThumbnailShapeStyle.SHARP -> RoundedCornerShape(2.dp)
        ThumbnailShapeStyle.PILL -> CircleShape
    }
}

private val defaultPreset = GalleryThemePreset.OBSIDIAN_TITANIUM
private val defaultSemantic = SemanticColors(
    textPrimary = defaultPreset.textPrimary,
    textSecondary = defaultPreset.textSecondary,
    textMuted = defaultPreset.textSecondary.copy(alpha = 0.78f),
    textDisabled = defaultPreset.textSecondary.copy(alpha = 0.48f),
    background = defaultPreset.background,
    cardBackground = defaultPreset.surfaceElevated,
    cardBorder = defaultPreset.cardBorder,
    buttonPrimary = defaultPreset.primary,
    onButtonPrimary = defaultPreset.onPrimary,
    iconPrimary = defaultPreset.textPrimary,
    iconSecondary = defaultPreset.textSecondary,
)

val LocalGallery56Tokens = staticCompositionLocalOf {
    Gallery56Tokens(
        preset = defaultPreset,
        semantic = defaultSemantic,
        glassSurface = defaultPreset.surface.copy(alpha = 0.88f),
        glassBorder = defaultPreset.cardBorder,
        accentGlow = defaultPreset.primary.copy(alpha = 0.22f),
        elevatedSurface = defaultPreset.surfaceElevated,
        subtleDivider = defaultPreset.textSecondary.copy(alpha = 0.16f),
    )
}

@Composable
fun Gallery56Theme(
    preset: GalleryThemePreset = GalleryThemePreset.OBSIDIAN_TITANIUM,
    customProfile: CustomThemeProfile? = null,
    customAccentHexOverride: String = "",
    secondaryHexOverride: String = "",
    backgroundHexOverride: String = "",
    cardHexOverride: String = "",
    borderHexOverride: String = "",
    textHexOverride: String = "",
    amoledMode: Boolean = false,
    cornerRadiusDp: Int = 14,
    gridSpacingDp: Int = 8,
    cardElevationDp: Int = 2,
    blurIntensity: Float = 0.65f,
    fontScale: Float = 1.0f,
    typographyStyle: GalleryTypographyStyle = GalleryTypographyStyle.MODERN_GROTESK,
    thumbnailShapeStyle: ThumbnailShapeStyle = ThumbnailShapeStyle.ROUNDED,
    iconStyle: IconStylePreference = IconStylePreference.ROUNDED,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = if (amoledMode) true else (customProfile?.isDark ?: preset.isDark)

    val resolvedPrimary = parseHexColorOrNull(customAccentHexOverride)
        ?: customProfile?.let { parseHexColorOrNull(it.primaryHex) }
        ?: preset.primary
    val resolvedSecondary = parseHexColorOrNull(secondaryHexOverride)
        ?: customProfile?.let { parseHexColorOrNull(it.secondaryHex) }
        ?: preset.secondary
    val resolvedTertiary = customProfile?.let { parseHexColorOrNull(it.tertiaryHex) } ?: preset.tertiary

    val resolvedBg = if (amoledMode) {
        Color.Black
    } else {
        parseHexColorOrNull(backgroundHexOverride)
            ?: customProfile?.let { parseHexColorOrNull(it.backgroundHex) }
            ?: preset.background
    }

    val resolvedSurface = if (amoledMode) {
        Color(0xFF0A0A0D)
    } else {
        parseHexColorOrNull(cardHexOverride)
            ?: customProfile?.let { parseHexColorOrNull(it.surfaceHex) }
            ?: preset.surface
    }

    val resolvedElevated = if (amoledMode) {
        Color(0xFF141419)
    } else {
        parseHexColorOrNull(cardHexOverride)
            ?: customProfile?.let { parseHexColorOrNull(it.cardHex) }
            ?: if (isDark) {
                resolvedSurface.copy(
                    red = (resolvedSurface.red + 0.045f).coerceAtMost(1f),
                    green = (resolvedSurface.green + 0.045f).coerceAtMost(1f),
                    blue = (resolvedSurface.blue + 0.055f).coerceAtMost(1f),
                )
            } else {
                preset.surfaceElevated
            }
    }

    val rawPrimaryText = parseHexColorOrNull(textHexOverride)
        ?: customProfile?.let { parseHexColorOrNull(it.textPrimaryHex) }
        ?: if (isDark) Color(0xFFF4F7FB) else Color(0xFF0F172A)

    val onText = ensureReadableTextColor(rawPrimaryText, resolvedSurface, isDark)
    val rawSecondaryText = customProfile?.let { parseHexColorOrNull(it.textSecondaryHex) }
        ?: if (isDark) Color(0xFFA6B2C3) else Color(0xFF475569)
    val onTextSecondary = ensureReadableTextColor(rawSecondaryText, resolvedSurface, isDark)
    val onPrimary = if (resolvedPrimary.luminance() > 0.48f) Color(0xFF070A0F) else Color.White

    val resolvedBorder = parseHexColorOrNull(borderHexOverride)
        ?: customProfile?.let { parseHexColorOrNull(it.borderHex) }
        ?: resolvedPrimary.copy(alpha = if (isDark) 0.28f else 0.24f)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = resolvedPrimary,
            onPrimary = onPrimary,
            primaryContainer = resolvedPrimary.copy(alpha = 0.20f),
            onPrimaryContainer = ensureReadableTextColor(resolvedPrimary, resolvedSurface, true),
            secondary = resolvedSecondary,
            onSecondary = resolvedBg,
            secondaryContainer = resolvedElevated,
            onSecondaryContainer = onText,
            tertiary = resolvedTertiary,
            onTertiary = resolvedBg,
            tertiaryContainer = resolvedTertiary.copy(alpha = 0.18f),
            onTertiaryContainer = ensureReadableTextColor(resolvedTertiary, resolvedSurface, true),
            background = resolvedBg,
            onBackground = onText,
            surface = resolvedSurface,
            onSurface = onText,
            surfaceVariant = resolvedElevated,
            onSurfaceVariant = onTextSecondary,
            outline = resolvedBorder,
            outlineVariant = resolvedBorder.copy(alpha = 0.50f),
            error = StatusRose,
            onError = Color(0xFF1F0308),
        )
    } else {
        lightColorScheme(
            primary = resolvedPrimary,
            onPrimary = onPrimary,
            primaryContainer = resolvedPrimary.copy(alpha = 0.14f),
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
            outline = resolvedBorder,
            outlineVariant = resolvedBorder.copy(alpha = 0.40f),
            error = Color(0xFFDC2626),
            onError = Color.White,
        )
    }

    val radius = cornerRadiusDp.coerceIn(0, 28)
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((radius * 0.50f).dp),
        small = RoundedCornerShape((radius * 0.80f).dp),
        medium = RoundedCornerShape((radius + 2).dp),
        large = RoundedCornerShape((radius + 8).dp),
        extraLarge = RoundedCornerShape((radius + 14).dp),
    )

    val glassAlpha = if (amoledMode) 0.96f else (0.72f + blurIntensity.coerceIn(0f, 1f) * 0.24f)
    val semantic = SemanticColors(
        textPrimary = onText,
        textSecondary = onTextSecondary,
        textMuted = onTextSecondary.copy(alpha = 0.80f),
        textDisabled = onTextSecondary.copy(alpha = 0.45f),
        background = resolvedBg,
        cardBackground = resolvedElevated,
        cardBorder = resolvedBorder,
        buttonPrimary = resolvedPrimary,
        onButtonPrimary = onPrimary,
        iconPrimary = onText,
        iconSecondary = onTextSecondary,
    )

    val tokens = Gallery56Tokens(
        preset = preset,
        semantic = semantic,
        glassSurface = resolvedSurface.copy(alpha = glassAlpha),
        glassBorder = resolvedBorder,
        accentGlow = resolvedPrimary.copy(alpha = 0.22f),
        elevatedSurface = resolvedElevated,
        subtleDivider = onTextSecondary.copy(alpha = 0.18f),
        thumbnailCornerRadius = radius.dp,
        gridSpacing = gridSpacingDp.coerceIn(2, 20).dp,
        cardElevation = cardElevationDp.coerceIn(0, 12).dp,
        blurIntensity = blurIntensity.coerceIn(0f, 1f),
        thumbnailShapeStyle = thumbnailShapeStyle,
        iconStyle = iconStyle,
        reducedMotion = reducedMotion,
        isAmoledMode = amoledMode,
    )

    val scaledTypography = buildStyledTypography(typographyStyle, fontScale.coerceIn(0.85f, 1.25f))

    CompositionLocalProvider(
        LocalGallery56Tokens provides tokens,
        LocalContentColor provides colorScheme.onBackground,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            shapes = shapes,
            content = content,
        )
    }
}

private fun buildStyledTypography(style: GalleryTypographyStyle, scale: Float): Typography {
    val displayFamily: FontFamily
    val bodyFamily: FontFamily
    when (style) {
        GalleryTypographyStyle.MODERN_GROTESK -> {
            displayFamily = DisplayFontFamily
            bodyFamily = BodyFontFamily
        }
        GalleryTypographyStyle.EDITORIAL_SERIF -> {
            displayFamily = FontFamily.Serif
            bodyFamily = BodyFontFamily
        }
        GalleryTypographyStyle.TECH_MONO -> {
            displayFamily = MonoFontFamily
            bodyFamily = MonoFontFamily
        }
        GalleryTypographyStyle.CLASSIC_CLEAN -> {
            displayFamily = FontFamily.SansSerif
            bodyFamily = FontFamily.SansSerif
        }
    }

    val base = Gallery56Typography
    return Typography(
        displaySmall = base.displaySmall.copy(fontFamily = displayFamily, fontSize = base.displaySmall.fontSize * scale),
        headlineMedium = base.headlineMedium.copy(fontFamily = displayFamily, fontSize = base.headlineMedium.fontSize * scale),
        headlineSmall = base.headlineSmall.copy(fontFamily = displayFamily, fontSize = base.headlineSmall.fontSize * scale),
        titleLarge = base.titleLarge.copy(fontFamily = displayFamily, fontSize = base.titleLarge.fontSize * scale),
        titleMedium = base.titleMedium.copy(fontFamily = bodyFamily, fontSize = base.titleMedium.fontSize * scale),
        titleSmall = base.titleSmall.copy(fontFamily = bodyFamily, fontSize = base.titleSmall.fontSize * scale),
        bodyLarge = base.bodyLarge.copy(fontFamily = bodyFamily, fontSize = base.bodyLarge.fontSize * scale),
        bodyMedium = base.bodyMedium.copy(fontFamily = bodyFamily, fontSize = base.bodyMedium.fontSize * scale),
        bodySmall = base.bodySmall.copy(fontFamily = bodyFamily, fontSize = base.bodySmall.fontSize * scale),
        labelLarge = base.labelLarge.copy(fontFamily = bodyFamily, fontSize = base.labelLarge.fontSize * scale),
        labelMedium = base.labelMedium.copy(fontFamily = MonoFontFamily, fontSize = base.labelMedium.fontSize * scale),
        labelSmall = base.labelSmall.copy(fontFamily = MonoFontFamily, fontSize = base.labelSmall.fontSize * scale),
    )
}
