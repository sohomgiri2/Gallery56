package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.CustomThemeProfile
import com.example.ui.theme.GalleryThemePreset
import com.example.ui.theme.GalleryTypographyStyle
import com.example.ui.theme.IconStylePreference
import com.example.ui.theme.ThumbnailShapeStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.galleryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gallery56_preferences",
)

@Serializable
enum class GalleryLayoutId(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultColumns: Int,
) {
    CLASSIC_GRID("classic_grid", "Classic Grid", "Clean square grid with date grouping & toolbar", 3),
    IOS_INSPIRED("ios_inspired", "iOS Inspired", "Airy rounded cards, refined type & segmented pills", 3),
    NOTHING_MINIMAL("nothing_minimal", "Nothing Minimal", "Monochrome dot-matrix details & sharp 4dp geometry", 3),
    LIQUID_GLASS("liquid_glass", "Liquid Glass", "Translucent frosted surfaces & layered depth chrome", 3),
    CINEMATIC_GALLERY("cinematic_gallery", "Cinematic Gallery", "Full-bleed widescreen hero spotlight & filmstrip", 2),
    MASONRY_FLOW("masonry_flow", "Masonry Flow", "Dynamic staggered waterfall honoring aspect ratios", 2),
    TIMELINE_GALLERY("timeline_gallery", "Timeline Gallery", "Editorial Year / Month / Day chronological stream", 3),
    COMPACT_PRO("compact_pro", "Compact Pro", "High-density multi-column grid with format badges", 5),
    MAGAZINE_GALLERY("magazine_gallery", "Magazine Gallery", "Editorial hero frames paired with grouped stories", 2),
    SPATIAL_GALLERY("spatial_gallery", "Spatial Gallery", "Elevated 3D-inspired depth cards with wide gutters", 2),
    FOLDER_FIRST("folder_first", "Folder First", "Directory-first showcase with large folder tiles", 2),
    SMART_AI_GALLERY("smart_ai_gallery", "Smart AI Gallery", "AI category hubs (Docs, People, Shots, Geo)", 3),
    AMOLED_PURE("amoled_pure", "AMOLED Pure", "Zero-noise pitch black canvas with hairline borders", 3),
    MATERIAL_ADAPTIVE("material_adaptive", "Material Adaptive", "Modern Android M3 expressive adaptive layout", 3);

    companion object {
        fun fromId(raw: String): GalleryLayoutId =
            entries.firstOrNull { it.id == raw } ?: CLASSIC_GRID
    }
}

@Serializable
enum class NavigationStyle(val label: String) {
    FLOATING_ROUNDED("Floating Rounded"),
    MINIMAL_FLAT("Minimal Flat"),
    GLASS("Glass"),
    COMPACT_ICON_ONLY("Compact Icons"),
    CLASSIC_LABELED("Classic Labeled"),
}

@Serializable
enum class AnimationIntensity(val label: String) {
    NONE("Off"),
    SUBTLE("Subtle"),
    STANDARD("Balanced"),
    EXPRESSIVE("Expressive"),
}

@Serializable
enum class ViewerBackgroundMode(val label: String) {
    AMOLED_BLACK("AMOLED Black"),
    DARK_GRAPHITE("Dark Graphite"),
    ADAPTIVE_THEME("Adaptive Theme"),
}

@Serializable
data class SavedEditorFilterPreset(
    val id: String,
    val name: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float,
    val tint: Float = 0f,
    val vibrance: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val filterName: String = "NONE",
)

@Serializable
data class GalleryPreferences(
    // Layout & Appearance
    val galleryLayoutId: String = GalleryLayoutId.CLASSIC_GRID.id,
    val themeId: String = GalleryThemePreset.OBSIDIAN_TITANIUM.id,
    val activeCustomThemeId: String = "",
    val customAccentHexOverride: String = "",
    val secondaryHexOverride: String = "",
    val backgroundHexOverride: String = "",
    val cardHexOverride: String = "",
    val borderHexOverride: String = "",
    val textHexOverride: String = "",
    val customThemes: List<CustomThemeProfile> = emptyList(),
    val gridColumns: Int = 3,
    val thumbnailSizeScale: Float = 1.0f,
    val cornerRadiusDp: Int = 14,
    val gridSpacingDp: Int = 8,
    val thumbnailShapeStyle: ThumbnailShapeStyle = ThumbnailShapeStyle.ROUNDED,
    val iconStyle: IconStylePreference = IconStylePreference.ROUNDED,
    val showDateGrouping: Boolean = true,
    val albumHeaderCompact: Boolean = false,
    val albumCardLarge: Boolean = false,
    // Typography & Motion
    val fontScale: Float = 1.0f,
    val typographyStyle: GalleryTypographyStyle = GalleryTypographyStyle.MODERN_GROTESK,
    val animationIntensity: AnimationIntensity = AnimationIntensity.STANDARD,
    val blurIntensity: Float = 0.65f,
    val cardElevationDp: Int = 2,
    val hapticFeedbackEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val amoledMode: Boolean = false,
    // Navigation & Gestures
    val navigationStyle: NavigationStyle = NavigationStyle.GLASS,
    val showNavigationLabels: Boolean = true,
    val doubleTapQuickFavorite: Boolean = true,
    val viewerBackgroundMode: ViewerBackgroundMode = ViewerBackgroundMode.AMOLED_BLACK,
    // AI & Search
    val faceAnalysisEnabled: Boolean = false,
    val faceGroupingEnabled: Boolean = false,
    val autoIndexOnLaunch: Boolean = true,
    val lastFullIndexAtMillis: Long = 0L,
    val semanticSearchEnabled: Boolean = true,
    // Studio Dashboard Personalization
    val pinnedStudioTools: List<String> = listOf("photo_editor", "private_vault", "collage_studio", "doc_scanner"),
    val recentStudioTools: List<String> = emptyList(),
    val customEditorPresets: List<SavedEditorFilterPreset> = emptyList(),
    // Private Vault Security Settings
    val vaultPinHash: String = "",
    val vaultPinSalt: String = "",
    val vaultBiometricEnabled: Boolean = true,
    val vaultAutoLockSeconds: Int = 60,
    val vaultFailedAttempts: Int = 0,
    val vaultLockoutUntilMillis: Long = 0L,
) {
    val hasVaultPin: Boolean get() = vaultPinHash.isNotBlank()
}

class GalleryPreferencesManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val GALLERY_LAYOUT_ID = stringPreferencesKey("gallery_layout_id")
        val THEME_ID = stringPreferencesKey("theme_id")
        val ACTIVE_CUSTOM_THEME_ID = stringPreferencesKey("active_custom_theme_id")
        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
        val SECONDARY_HEX = stringPreferencesKey("secondary_hex")
        val BACKGROUND_HEX = stringPreferencesKey("background_hex")
        val CARD_HEX = stringPreferencesKey("card_hex")
        val BORDER_HEX = stringPreferencesKey("border_hex")
        val TEXT_HEX = stringPreferencesKey("text_hex")
        val CUSTOM_THEMES_JSON = stringPreferencesKey("custom_themes_json")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val THUMBNAIL_SCALE = floatPreferencesKey("thumbnail_scale")
        val CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
        val GRID_SPACING_DP = intPreferencesKey("grid_spacing_dp")
        val THUMBNAIL_SHAPE = stringPreferencesKey("thumbnail_shape")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val SHOW_DATE_GROUPING = booleanPreferencesKey("show_date_grouping")
        val ALBUM_HEADER_COMPACT = booleanPreferencesKey("album_header_compact")
        val ALBUM_CARD_LARGE = booleanPreferencesKey("album_card_large")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val TYPOGRAPHY_STYLE = stringPreferencesKey("typography_style")
        val ANIMATION_INTENSITY = stringPreferencesKey("animation_intensity")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val CARD_ELEVATION_DP = intPreferencesKey("card_elevation_dp")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val NAV_STYLE = stringPreferencesKey("navigation_style")
        val SHOW_NAV_LABELS = booleanPreferencesKey("show_nav_labels")
        val DOUBLE_TAP_FAV = booleanPreferencesKey("double_tap_fav")
        val VIEWER_BG_MODE = stringPreferencesKey("viewer_bg_mode")
        val FACE_ANALYSIS_ENABLED = booleanPreferencesKey("face_analysis_enabled")
        val FACE_GROUPING_ENABLED = booleanPreferencesKey("face_grouping_enabled")
        val AUTO_INDEX_ON_LAUNCH = booleanPreferencesKey("auto_index_on_launch")
        val LAST_FULL_INDEX_AT = longPreferencesKey("last_full_index_at")
        val SEMANTIC_SEARCH_ENABLED = booleanPreferencesKey("semantic_search_enabled")
        val PINNED_TOOLS_JSON = stringPreferencesKey("pinned_tools_json")
        val RECENT_TOOLS_JSON = stringPreferencesKey("recent_tools_json")
        val EDITOR_PRESETS_JSON = stringPreferencesKey("editor_presets_json")
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val VAULT_PIN_SALT = stringPreferencesKey("vault_pin_salt")
        val VAULT_BIOMETRIC_ENABLED = booleanPreferencesKey("vault_biometric_enabled")
        val VAULT_AUTO_LOCK_SEC = intPreferencesKey("vault_auto_lock_sec")
        val VAULT_FAILED_ATTEMPTS = intPreferencesKey("vault_failed_attempts")
        val VAULT_LOCKOUT_UNTIL = longPreferencesKey("vault_lockout_until")
    }

    val preferences: Flow<GalleryPreferences> = context.galleryDataStore.data.map { prefs ->
        val customThemes = decodeListOrDefault<CustomThemeProfile>(prefs[Keys.CUSTOM_THEMES_JSON])
        val pinnedTools = decodeListOrDefault<String>(prefs[Keys.PINNED_TOOLS_JSON])
            .ifEmpty { listOf("photo_editor", "private_vault", "collage_studio", "doc_scanner") }
        val recentTools = decodeListOrDefault<String>(prefs[Keys.RECENT_TOOLS_JSON])
        val editorPresets = decodeListOrDefault<SavedEditorFilterPreset>(prefs[Keys.EDITOR_PRESETS_JSON])

        val navStyle = prefs[Keys.NAV_STYLE]?.let { raw ->
            when (raw) {
                "STANDARD_BAR" -> NavigationStyle.CLASSIC_LABELED
                "FLOATING_PILL" -> NavigationStyle.FLOATING_ROUNDED
                else -> NavigationStyle.entries.firstOrNull { it.name == raw }
            }
        } ?: NavigationStyle.GLASS

        val shapeStyle = prefs[Keys.THUMBNAIL_SHAPE]?.let { raw ->
            ThumbnailShapeStyle.entries.firstOrNull { it.name == raw }
        } ?: ThumbnailShapeStyle.ROUNDED

        val iconStyle = prefs[Keys.ICON_STYLE]?.let { raw ->
            IconStylePreference.entries.firstOrNull { it.name == raw }
        } ?: IconStylePreference.ROUNDED

        val typeStyle = prefs[Keys.TYPOGRAPHY_STYLE]?.let { raw ->
            GalleryTypographyStyle.entries.firstOrNull { it.name == raw }
        } ?: GalleryTypographyStyle.MODERN_GROTESK

        val animIntensity = prefs[Keys.ANIMATION_INTENSITY]?.let { raw ->
            AnimationIntensity.entries.firstOrNull { it.name == raw }
        } ?: AnimationIntensity.STANDARD

        val viewerBg = prefs[Keys.VIEWER_BG_MODE]?.let { raw ->
            ViewerBackgroundMode.entries.firstOrNull { it.name == raw }
        } ?: ViewerBackgroundMode.AMOLED_BLACK

        GalleryPreferences(
            galleryLayoutId = prefs[Keys.GALLERY_LAYOUT_ID] ?: GalleryLayoutId.CLASSIC_GRID.id,
            themeId = prefs[Keys.THEME_ID] ?: GalleryThemePreset.OBSIDIAN_TITANIUM.id,
            activeCustomThemeId = prefs[Keys.ACTIVE_CUSTOM_THEME_ID] ?: "",
            customAccentHexOverride = prefs[Keys.CUSTOM_ACCENT_HEX] ?: "",
            secondaryHexOverride = prefs[Keys.SECONDARY_HEX] ?: "",
            backgroundHexOverride = prefs[Keys.BACKGROUND_HEX] ?: "",
            cardHexOverride = prefs[Keys.CARD_HEX] ?: "",
            borderHexOverride = prefs[Keys.BORDER_HEX] ?: "",
            textHexOverride = prefs[Keys.TEXT_HEX] ?: "",
            customThemes = customThemes,
            gridColumns = (prefs[Keys.GRID_COLUMNS] ?: 3).coerceIn(2, 6),
            thumbnailSizeScale = (prefs[Keys.THUMBNAIL_SCALE] ?: 1.0f).coerceIn(0.8f, 1.4f),
            cornerRadiusDp = (prefs[Keys.CORNER_RADIUS_DP] ?: 14).coerceIn(0, 28),
            gridSpacingDp = (prefs[Keys.GRID_SPACING_DP] ?: 8).coerceIn(2, 20),
            thumbnailShapeStyle = shapeStyle,
            iconStyle = iconStyle,
            showDateGrouping = prefs[Keys.SHOW_DATE_GROUPING] ?: true,
            albumHeaderCompact = prefs[Keys.ALBUM_HEADER_COMPACT] ?: false,
            albumCardLarge = prefs[Keys.ALBUM_CARD_LARGE] ?: false,
            fontScale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.25f),
            typographyStyle = typeStyle,
            animationIntensity = animIntensity,
            blurIntensity = (prefs[Keys.BLUR_INTENSITY] ?: 0.65f).coerceIn(0f, 1f),
            cardElevationDp = (prefs[Keys.CARD_ELEVATION_DP] ?: 2).coerceIn(0, 12),
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            amoledMode = prefs[Keys.AMOLED_MODE] ?: false,
            navigationStyle = navStyle,
            showNavigationLabels = prefs[Keys.SHOW_NAV_LABELS] ?: true,
            doubleTapQuickFavorite = prefs[Keys.DOUBLE_TAP_FAV] ?: true,
            viewerBackgroundMode = viewerBg,
            faceAnalysisEnabled = prefs[Keys.FACE_ANALYSIS_ENABLED] ?: false,
            faceGroupingEnabled = prefs[Keys.FACE_GROUPING_ENABLED] ?: false,
            autoIndexOnLaunch = prefs[Keys.AUTO_INDEX_ON_LAUNCH] ?: true,
            lastFullIndexAtMillis = prefs[Keys.LAST_FULL_INDEX_AT] ?: 0L,
            semanticSearchEnabled = prefs[Keys.SEMANTIC_SEARCH_ENABLED] ?: true,
            pinnedStudioTools = pinnedTools,
            recentStudioTools = recentTools,
            customEditorPresets = editorPresets,
            vaultPinHash = prefs[Keys.VAULT_PIN_HASH] ?: "",
            vaultPinSalt = prefs[Keys.VAULT_PIN_SALT] ?: "",
            vaultBiometricEnabled = prefs[Keys.VAULT_BIOMETRIC_ENABLED] ?: true,
            vaultAutoLockSeconds = prefs[Keys.VAULT_AUTO_LOCK_SEC] ?: 60,
            vaultFailedAttempts = prefs[Keys.VAULT_FAILED_ATTEMPTS] ?: 0,
            vaultLockoutUntilMillis = prefs[Keys.VAULT_LOCKOUT_UNTIL] ?: 0L,
        )
    }

    private inline fun <reified T> decodeListOrDefault(raw: String?): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<T>>(raw)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun setGalleryLayoutId(layoutId: String) {
        context.galleryDataStore.edit { it[Keys.GALLERY_LAYOUT_ID] = layoutId }
    }

    suspend fun setThemeId(themeId: String) {
        context.galleryDataStore.edit {
            it[Keys.THEME_ID] = themeId
            it[Keys.ACTIVE_CUSTOM_THEME_ID] = ""
        }
    }

    suspend fun setColorOverrides(
        primaryHex: String? = null,
        secondaryHex: String? = null,
        backgroundHex: String? = null,
        cardHex: String? = null,
        borderHex: String? = null,
        textHex: String? = null,
    ) {
        context.galleryDataStore.edit { prefs ->
            primaryHex?.let { prefs[Keys.CUSTOM_ACCENT_HEX] = it.trim() }
            secondaryHex?.let { prefs[Keys.SECONDARY_HEX] = it.trim() }
            backgroundHex?.let { prefs[Keys.BACKGROUND_HEX] = it.trim() }
            cardHex?.let { prefs[Keys.CARD_HEX] = it.trim() }
            borderHex?.let { prefs[Keys.BORDER_HEX] = it.trim() }
            textHex?.let { prefs[Keys.TEXT_HEX] = it.trim() }
        }
    }

    suspend fun setCustomAccentHex(hex: String) = setColorOverrides(primaryHex = hex)

    suspend fun saveCustomTheme(profile: CustomThemeProfile, activate: Boolean = true) {
        context.galleryDataStore.edit { prefs ->
            val current = decodeListOrDefault<CustomThemeProfile>(prefs[Keys.CUSTOM_THEMES_JSON])
            val updated = current.filterNot { it.id == profile.id } + profile
            prefs[Keys.CUSTOM_THEMES_JSON] = json.encodeToString(updated)
            if (activate) {
                prefs[Keys.ACTIVE_CUSTOM_THEME_ID] = profile.id
                prefs[Keys.CUSTOM_ACCENT_HEX] = ""
            }
        }
    }

    suspend fun activateCustomTheme(id: String) {
        context.galleryDataStore.edit {
            it[Keys.ACTIVE_CUSTOM_THEME_ID] = id
            it[Keys.CUSTOM_ACCENT_HEX] = ""
        }
    }

    suspend fun deleteCustomTheme(id: String) {
        context.galleryDataStore.edit { prefs ->
            val current = decodeListOrDefault<CustomThemeProfile>(prefs[Keys.CUSTOM_THEMES_JSON])
            prefs[Keys.CUSTOM_THEMES_JSON] = json.encodeToString(current.filterNot { it.id == id })
            if (prefs[Keys.ACTIVE_CUSTOM_THEME_ID] == id) {
                prefs[Keys.ACTIVE_CUSTOM_THEME_ID] = ""
            }
        }
    }

    suspend fun setGridColumns(columns: Int) {
        context.galleryDataStore.edit { it[Keys.GRID_COLUMNS] = columns.coerceIn(2, 6) }
    }

    suspend fun setThumbnailSizeScale(scale: Float) {
        context.galleryDataStore.edit { it[Keys.THUMBNAIL_SCALE] = scale.coerceIn(0.8f, 1.4f) }
    }

    suspend fun setCornerRadiusDp(radius: Int) {
        context.galleryDataStore.edit { it[Keys.CORNER_RADIUS_DP] = radius.coerceIn(0, 28) }
    }

    suspend fun setGridSpacingDp(spacing: Int) {
        context.galleryDataStore.edit { it[Keys.GRID_SPACING_DP] = spacing.coerceIn(2, 20) }
    }

    suspend fun setThumbnailShapeStyle(shape: ThumbnailShapeStyle) {
        context.galleryDataStore.edit { it[Keys.THUMBNAIL_SHAPE] = shape.name }
    }

    suspend fun setIconStyle(style: IconStylePreference) {
        context.galleryDataStore.edit { it[Keys.ICON_STYLE] = style.name }
    }

    suspend fun setShowDateGrouping(show: Boolean) {
        context.galleryDataStore.edit { it[Keys.SHOW_DATE_GROUPING] = show }
    }

    suspend fun setAlbumHeaderCompact(compact: Boolean) {
        context.galleryDataStore.edit { it[Keys.ALBUM_HEADER_COMPACT] = compact }
    }

    suspend fun setAlbumCardLarge(large: Boolean) {
        context.galleryDataStore.edit { it[Keys.ALBUM_CARD_LARGE] = large }
    }

    suspend fun setFontScale(scale: Float) {
        context.galleryDataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.25f) }
    }

    suspend fun setTypographyStyle(style: GalleryTypographyStyle) {
        context.galleryDataStore.edit { it[Keys.TYPOGRAPHY_STYLE] = style.name }
    }

    suspend fun setAnimationIntensity(intensity: AnimationIntensity) {
        context.galleryDataStore.edit { it[Keys.ANIMATION_INTENSITY] = intensity.name }
    }

    suspend fun setBlurIntensity(blur: Float) {
        context.galleryDataStore.edit { it[Keys.BLUR_INTENSITY] = blur.coerceIn(0f, 1f) }
    }

    suspend fun setCardElevationDp(elevation: Int) {
        context.galleryDataStore.edit { it[Keys.CARD_ELEVATION_DP] = elevation.coerceIn(0, 12) }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.galleryDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setReducedMotion(reduced: Boolean) {
        context.galleryDataStore.edit { it[Keys.REDUCED_MOTION] = reduced }
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        context.galleryDataStore.edit { it[Keys.AMOLED_MODE] = enabled }
    }

    suspend fun setNavigationStyle(style: NavigationStyle) {
        context.galleryDataStore.edit { it[Keys.NAV_STYLE] = style.name }
    }

    suspend fun setShowNavigationLabels(show: Boolean) {
        context.galleryDataStore.edit { it[Keys.SHOW_NAV_LABELS] = show }
    }

    suspend fun setDoubleTapQuickFavorite(enabled: Boolean) {
        context.galleryDataStore.edit { it[Keys.DOUBLE_TAP_FAV] = enabled }
    }

    suspend fun setViewerBackgroundMode(mode: ViewerBackgroundMode) {
        context.galleryDataStore.edit { it[Keys.VIEWER_BG_MODE] = mode.name }
    }

    suspend fun togglePinnedStudioTool(toolId: String) {
        context.galleryDataStore.edit { prefs ->
            val current = decodeListOrDefault<String>(prefs[Keys.PINNED_TOOLS_JSON])
                .ifEmpty { listOf("photo_editor", "private_vault", "collage_studio", "doc_scanner") }
            val next = if (toolId in current) current - toolId else (listOf(toolId) + current).take(8)
            prefs[Keys.PINNED_TOOLS_JSON] = json.encodeToString(next)
        }
    }

    suspend fun recordRecentStudioTool(toolId: String) {
        context.galleryDataStore.edit { prefs ->
            val current = decodeListOrDefault<String>(prefs[Keys.RECENT_TOOLS_JSON])
            val next = (listOf(toolId) + current.filterNot { it == toolId }).take(8)
            prefs[Keys.RECENT_TOOLS_JSON] = json.encodeToString(next)
        }
    }

    suspend fun saveCustomEditorPreset(preset: SavedEditorFilterPreset) {
        context.galleryDataStore.edit { prefs ->
            val current = decodeListOrDefault<SavedEditorFilterPreset>(prefs[Keys.EDITOR_PRESETS_JSON])
            val next = (listOf(preset) + current.filterNot { it.id == preset.id }).take(24)
            prefs[Keys.EDITOR_PRESETS_JSON] = json.encodeToString(next)
        }
    }

    suspend fun deleteCustomEditorPreset(id: String) {
        context.galleryDataStore.edit { prefs ->
            val current = decodeListOrDefault<SavedEditorFilterPreset>(prefs[Keys.EDITOR_PRESETS_JSON])
            prefs[Keys.EDITOR_PRESETS_JSON] = json.encodeToString(current.filterNot { it.id == id })
        }
    }

    suspend fun setFaceAnalysisEnabled(enabled: Boolean) {
        context.galleryDataStore.edit {
            it[Keys.FACE_ANALYSIS_ENABLED] = enabled
            if (!enabled) {
                it[Keys.FACE_GROUPING_ENABLED] = false
            }
        }
    }

    suspend fun setFaceGroupingEnabled(enabled: Boolean) {
        context.galleryDataStore.edit { it[Keys.FACE_GROUPING_ENABLED] = enabled }
    }

    suspend fun setAutoIndexOnLaunch(enabled: Boolean) {
        context.galleryDataStore.edit { it[Keys.AUTO_INDEX_ON_LAUNCH] = enabled }
    }

    suspend fun setLastFullIndexAt(timestampMillis: Long) {
        context.galleryDataStore.edit { it[Keys.LAST_FULL_INDEX_AT] = timestampMillis }
    }

    suspend fun setSemanticSearchEnabled(enabled: Boolean) {
        context.galleryDataStore.edit { it[Keys.SEMANTIC_SEARCH_ENABLED] = enabled }
    }

    suspend fun setVaultPin(hash: String, salt: String) {
        context.galleryDataStore.edit {
            it[Keys.VAULT_PIN_HASH] = hash
            it[Keys.VAULT_PIN_SALT] = salt
            it[Keys.VAULT_FAILED_ATTEMPTS] = 0
            it[Keys.VAULT_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun setVaultBiometricEnabled(enabled: Boolean) {
        context.galleryDataStore.edit { it[Keys.VAULT_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setVaultAutoLockSeconds(seconds: Int) {
        context.galleryDataStore.edit { it[Keys.VAULT_AUTO_LOCK_SEC] = seconds.coerceIn(10, 600) }
    }

    suspend fun recordVaultAuthAttempt(success: Boolean, lockoutDurationMillis: Long = 30_000L) {
        context.galleryDataStore.edit { prefs ->
            if (success) {
                prefs[Keys.VAULT_FAILED_ATTEMPTS] = 0
                prefs[Keys.VAULT_LOCKOUT_UNTIL] = 0L
            } else {
                val nextAttempts = (prefs[Keys.VAULT_FAILED_ATTEMPTS] ?: 0) + 1
                prefs[Keys.VAULT_FAILED_ATTEMPTS] = nextAttempts
                if (nextAttempts >= 5) {
                    prefs[Keys.VAULT_LOCKOUT_UNTIL] = System.currentTimeMillis() + lockoutDurationMillis
                }
            }
        }
    }

    suspend fun resetAppearanceDefaults() {
        context.galleryDataStore.edit { prefs ->
            prefs[Keys.GALLERY_LAYOUT_ID] = GalleryLayoutId.CLASSIC_GRID.id
            prefs[Keys.THEME_ID] = GalleryThemePreset.OBSIDIAN_TITANIUM.id
            prefs[Keys.ACTIVE_CUSTOM_THEME_ID] = ""
            prefs[Keys.CUSTOM_ACCENT_HEX] = ""
            prefs[Keys.SECONDARY_HEX] = ""
            prefs[Keys.BACKGROUND_HEX] = ""
            prefs[Keys.CARD_HEX] = ""
            prefs[Keys.BORDER_HEX] = ""
            prefs[Keys.TEXT_HEX] = ""
            prefs[Keys.GRID_COLUMNS] = 3
            prefs[Keys.THUMBNAIL_SCALE] = 1.0f
            prefs[Keys.CORNER_RADIUS_DP] = 14
            prefs[Keys.GRID_SPACING_DP] = 8
            prefs[Keys.THUMBNAIL_SHAPE] = ThumbnailShapeStyle.ROUNDED.name
            prefs[Keys.ICON_STYLE] = IconStylePreference.ROUNDED.name
            prefs[Keys.SHOW_DATE_GROUPING] = true
            prefs[Keys.ALBUM_HEADER_COMPACT] = false
            prefs[Keys.ALBUM_CARD_LARGE] = false
            prefs[Keys.FONT_SCALE] = 1.0f
            prefs[Keys.TYPOGRAPHY_STYLE] = GalleryTypographyStyle.MODERN_GROTESK.name
            prefs[Keys.ANIMATION_INTENSITY] = AnimationIntensity.STANDARD.name
            prefs[Keys.BLUR_INTENSITY] = 0.65f
            prefs[Keys.CARD_ELEVATION_DP] = 2
            prefs[Keys.HAPTIC_FEEDBACK] = true
            prefs[Keys.REDUCED_MOTION] = false
            prefs[Keys.AMOLED_MODE] = false
            prefs[Keys.NAV_STYLE] = NavigationStyle.GLASS.name
            prefs[Keys.SHOW_NAV_LABELS] = true
        }
    }

    suspend fun applyImportedSettings(imported: GalleryPreferences) {
        context.galleryDataStore.edit { prefs ->
            prefs[Keys.GALLERY_LAYOUT_ID] = imported.galleryLayoutId
            prefs[Keys.THEME_ID] = imported.themeId
            prefs[Keys.ACTIVE_CUSTOM_THEME_ID] = imported.activeCustomThemeId
            prefs[Keys.CUSTOM_ACCENT_HEX] = imported.customAccentHexOverride
            prefs[Keys.SECONDARY_HEX] = imported.secondaryHexOverride
            prefs[Keys.BACKGROUND_HEX] = imported.backgroundHexOverride
            prefs[Keys.CARD_HEX] = imported.cardHexOverride
            prefs[Keys.BORDER_HEX] = imported.borderHexOverride
            prefs[Keys.TEXT_HEX] = imported.textHexOverride
            prefs[Keys.CUSTOM_THEMES_JSON] = json.encodeToString(imported.customThemes)
            prefs[Keys.GRID_COLUMNS] = imported.gridColumns.coerceIn(2, 6)
            prefs[Keys.THUMBNAIL_SCALE] = imported.thumbnailSizeScale.coerceIn(0.8f, 1.4f)
            prefs[Keys.CORNER_RADIUS_DP] = imported.cornerRadiusDp.coerceIn(0, 28)
            prefs[Keys.GRID_SPACING_DP] = imported.gridSpacingDp.coerceIn(2, 20)
            prefs[Keys.THUMBNAIL_SHAPE] = imported.thumbnailShapeStyle.name
            prefs[Keys.ICON_STYLE] = imported.iconStyle.name
            prefs[Keys.SHOW_DATE_GROUPING] = imported.showDateGrouping
            prefs[Keys.ALBUM_HEADER_COMPACT] = imported.albumHeaderCompact
            prefs[Keys.ALBUM_CARD_LARGE] = imported.albumCardLarge
            prefs[Keys.FONT_SCALE] = imported.fontScale.coerceIn(0.85f, 1.25f)
            prefs[Keys.TYPOGRAPHY_STYLE] = imported.typographyStyle.name
            prefs[Keys.ANIMATION_INTENSITY] = imported.animationIntensity.name
            prefs[Keys.BLUR_INTENSITY] = imported.blurIntensity.coerceIn(0f, 1f)
            prefs[Keys.CARD_ELEVATION_DP] = imported.cardElevationDp.coerceIn(0, 12)
            prefs[Keys.HAPTIC_FEEDBACK] = imported.hapticFeedbackEnabled
            prefs[Keys.REDUCED_MOTION] = imported.reducedMotion
            prefs[Keys.AMOLED_MODE] = imported.amoledMode
            prefs[Keys.NAV_STYLE] = imported.navigationStyle.name
            prefs[Keys.SHOW_NAV_LABELS] = imported.showNavigationLabels
            prefs[Keys.AUTO_INDEX_ON_LAUNCH] = imported.autoIndexOnLaunch
            prefs[Keys.SEMANTIC_SEARCH_ENABLED] = imported.semanticSearchEnabled
            prefs[Keys.PINNED_TOOLS_JSON] = json.encodeToString(imported.pinnedStudioTools)
            prefs[Keys.EDITOR_PRESETS_JSON] = json.encodeToString(imported.customEditorPresets)
        }
    }
}
