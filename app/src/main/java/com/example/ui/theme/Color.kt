package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

enum class GalleryThemePreset(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val isDark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
) {
    OBSIDIAN_TITANIUM(
        id = "obsidian_titanium",
        displayName = "Titanium Silver",
        subtitle = "Deep obsidian & brushed titanium cyan",
        isDark = true,
        primary = Color(0xFF5CE1E6),
        onPrimary = Color(0xFF041316),
        secondary = Color(0xFFC4CBD4),
        tertiary = Color(0xFFF5B971),
        background = Color(0xFF07090D),
        surface = Color(0xFF0F131A),
        surfaceElevated = Color(0xFF171D27),
        cardBorder = Color(0x33C4CBD4),
        textPrimary = Color(0xFFF4F7FB),
        textSecondary = Color(0xFF94A0B2),
    ),
    MIDNIGHT_BLACK(
        id = "midnight_black",
        displayName = "Midnight Black",
        subtitle = "Deep navy obsidian & glacier blue",
        isDark = true,
        primary = Color(0xFF60A5FA),
        onPrimary = Color(0xFF041126),
        secondary = Color(0xFF94A3B8),
        tertiary = Color(0xFF38BDF8),
        background = Color(0xFF04070D),
        surface = Color(0xFF0B111C),
        surfaceElevated = Color(0xFF131D2E),
        cardBorder = Color(0x3360A5FA),
        textPrimary = Color(0xFFF1F5F9),
        textSecondary = Color(0xFF94A3B8),
    ),
    PURE_AMOLED(
        id = "pure_amoled",
        displayName = "Pure AMOLED",
        subtitle = "True #000000 void with sharp silver",
        isDark = true,
        primary = Color(0xFFE2E8F0),
        onPrimary = Color(0xFF000000),
        secondary = Color(0xFF38BDF8),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF000000),
        surface = Color(0xFF0A0A0C),
        surfaceElevated = Color(0xFF141418),
        cardBorder = Color(0x2EFFFFFF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF8B95A5),
    ),
    LIQUID_GLASS(
        id = "liquid_glass",
        displayName = "Liquid Glass",
        subtitle = "Translucent frost & alpine sapphire",
        isDark = true,
        primary = Color(0xFF7DD3FC),
        onPrimary = Color(0xFF051824),
        secondary = Color(0xFFA5B4FC),
        tertiary = Color(0xFF6EE7B7),
        background = Color(0xFF09101C),
        surface = Color(0xFF111C2D),
        surfaceElevated = Color(0xFF19283E),
        cardBorder = Color(0x40BAE6FD),
        textPrimary = Color(0xFFF0F9FF),
        textSecondary = Color(0xFF93C5FD),
    ),
    NOTHING_MINIMAL(
        id = "nothing_minimal",
        displayName = "Nothing Minimal",
        subtitle = "Monochrome industrial & signal red",
        isDark = true,
        primary = Color(0xFFFF3B30),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFFD1D5DB),
        tertiary = Color(0xFF9CA3AF),
        background = Color(0xFF0B0B0C),
        surface = Color(0xFF131316),
        surfaceElevated = Color(0xFF1C1C21),
        cardBorder = Color(0x33FFFFFF),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF9CA3AF),
    ),
    CYBERPUNK_NEON(
        id = "cyberpunk_neon",
        displayName = "Cyberpunk",
        subtitle = "Electric magenta & hyper-cyan",
        isDark = true,
        primary = Color(0xFF00F5D4),
        onPrimary = Color(0xFF041411),
        secondary = Color(0xFFF72585),
        tertiary = Color(0xFFFEE440),
        background = Color(0xFF090612),
        surface = Color(0xFF130E24),
        surfaceElevated = Color(0xFF1D1636),
        cardBorder = Color(0x4500F5D4),
        textPrimary = Color(0xFFF8F7FF),
        textSecondary = Color(0xFFB8B2D8),
    ),
    ARCTIC_BLUE(
        id = "arctic_blue",
        displayName = "Arctic Blue",
        subtitle = "Ice-blue precision on slate",
        isDark = true,
        primary = Color(0xFF38BDF8),
        onPrimary = Color(0xFF031926),
        secondary = Color(0xFFBAE6FD),
        tertiary = Color(0xFF2DD4BF),
        background = Color(0xFF06111A),
        surface = Color(0xFF0C1C29),
        surfaceElevated = Color(0xFF13293B),
        cardBorder = Color(0x3838BDF8),
        textPrimary = Color(0xFFEFF8FF),
        textSecondary = Color(0xFF8FB8D6),
    ),
    COSMIC_PURPLE(
        id = "cosmic_purple",
        displayName = "Cosmic Purple",
        subtitle = "Deep nebula & ultraviolet accent",
        isDark = true,
        primary = Color(0xFFC084FC),
        onPrimary = Color(0xFF1E0836),
        secondary = Color(0xFFF472B6),
        tertiary = Color(0xFF818CF8),
        background = Color(0xFF0B0714),
        surface = Color(0xFF150F26),
        surfaceElevated = Color(0xFF20183A),
        cardBorder = Color(0x3AC084FC),
        textPrimary = Color(0xFFF5F3FF),
        textSecondary = Color(0xFFA99CC9),
    ),
    EMERALD_STUDIO(
        id = "emerald_studio",
        displayName = "Emerald",
        subtitle = "Dark carbon & vivid emerald optics",
        isDark = true,
        primary = Color(0xFF34D399),
        onPrimary = Color(0xFF031C13),
        secondary = Color(0xFFA7F3D0),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF060E0B),
        surface = Color(0xFF0C1A15),
        surfaceElevated = Color(0xFF142820),
        cardBorder = Color(0x3834D399),
        textPrimary = Color(0xFFECFDF5),
        textSecondary = Color(0xFF88B8A5),
    ),
    CRIMSON_CINEMA(
        id = "crimson_cinema",
        displayName = "Crimson",
        subtitle = "Cinema darkroom & ruby flare",
        isDark = true,
        primary = Color(0xFFFB7185),
        onPrimary = Color(0xFF29050B),
        secondary = Color(0xFFFCA5A5),
        tertiary = Color(0xFFF59E0B),
        background = Color(0xFF0D0608),
        surface = Color(0xFF180C10),
        surfaceElevated = Color(0xFF261319),
        cardBorder = Color(0x38FB7185),
        textPrimary = Color(0xFFFFF1F2),
        textSecondary = Color(0xFFC9939C),
    ),
    CHAMPAGNE_GOLD(
        id = "champagne_gold",
        displayName = "Champagne Gold",
        subtitle = "Luxury obsidian & warm brushed gold",
        isDark = true,
        primary = Color(0xFFF5C469),
        onPrimary = Color(0xFF241904),
        secondary = Color(0xFFE5D4B3),
        tertiary = Color(0xFFFB923C),
        background = Color(0xFF0B0907),
        surface = Color(0xFF16130E),
        surfaceElevated = Color(0xFF221D15),
        cardBorder = Color(0x3DF5C469),
        textPrimary = Color(0xFFFAF6EE),
        textSecondary = Color(0xFFB5A992),
    ),
    SUNSET_FLARE(
        id = "sunset_flare",
        displayName = "Sunset",
        subtitle = "Dusk violet & solar amber glow",
        isDark = true,
        primary = Color(0xFFFB923C),
        onPrimary = Color(0xFF261003),
        secondary = Color(0xFFF472B6),
        tertiary = Color(0xFFFACC15),
        background = Color(0xFF11090C),
        surface = Color(0xFF1D1016),
        surfaceElevated = Color(0xFF2B1821),
        cardBorder = Color(0x3CFB923C),
        textPrimary = Color(0xFFFFF7ED),
        textSecondary = Color(0xFFC8A09E),
    ),
    GRAPHITE_PRO(
        id = "graphite_pro",
        displayName = "Graphite",
        subtitle = "Neutral color-accurate pro editing gray",
        isDark = true,
        primary = Color(0xFFD4D4D8),
        onPrimary = Color(0xFF121215),
        secondary = Color(0xFFA1A1AA),
        tertiary = Color(0xFF60A5FA),
        background = Color(0xFF111113),
        surface = Color(0xFF1A1A1E),
        surfaceElevated = Color(0xFF25252B),
        cardBorder = Color(0x30FFFFFF),
        textPrimary = Color(0xFFFAFAFA),
        textSecondary = Color(0xFFA1A1AA),
    ),
    MINIMAL_DAYLIGHT(
        id = "minimal_daylight",
        displayName = "Minimal White",
        subtitle = "Clean daylight studio & deep cobalt",
        isDark = false,
        primary = Color(0xFF0F4C81),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF475569),
        tertiary = Color(0xFFD97706),
        background = Color(0xFFF4F6FA),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFE9EEF5),
        cardBorder = Color(0x260F172A),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
    );

    companion object {
        fun fromId(id: String): GalleryThemePreset =
            entries.firstOrNull { it.id == id } ?: OBSIDIAN_TITANIUM
    }
}

@Serializable
data class CustomThemeProfile(
    val id: String,
    val name: String,
    val isDark: Boolean = true,
    val primaryHex: String = "#5CE1E6",
    val secondaryHex: String = "#C4CBD4",
    val tertiaryHex: String = "#F5B971",
    val backgroundHex: String = "#07090D",
    val surfaceHex: String = "#0F131A",
)

fun parseHexColorOrNull(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    return try {
        when (clean.length) {
            6 -> Color(("FF$clean").toLong(16))
            8 -> Color(clean.toLong(16))
            else -> null
        }
    } catch (_: Throwable) {
        null
    }
}

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

val StatusEmerald = Color(0xFF34D399)
val StatusAmber = Color(0xFFFBBF24)
val StatusRose = Color(0xFFFB7185)
val StatusCyan = Color(0xFF38BDF8)
