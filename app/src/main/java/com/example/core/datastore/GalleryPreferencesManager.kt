package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.CustomThemeProfile
import com.example.ui.theme.GalleryThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.galleryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gallery56_preferences",
)

@Serializable
enum class NavigationStyle(val label: String) {
    STANDARD_BAR("Standard Bar"),
    FLOATING_PILL("Floating Glass"),
}

@Serializable
data class GalleryPreferences(
    val themeId: String = GalleryThemePreset.OBSIDIAN_TITANIUM.id,
    val activeCustomThemeId: String = "",
    val customAccentHexOverride: String = "",
    val customThemes: List<CustomThemeProfile> = emptyList(),
    val gridColumns: Int = 3,
    val cornerRadiusDp: Int = 14,
    val gridSpacingDp: Int = 8,
    val reducedMotion: Boolean = false,
    val navigationStyle: NavigationStyle = NavigationStyle.STANDARD_BAR,
    val faceAnalysisEnabled: Boolean = false,
    val faceGroupingEnabled: Boolean = false,
    val autoIndexOnLaunch: Boolean = true,
    val lastFullIndexAtMillis: Long = 0L,
    val semanticSearchEnabled: Boolean = true,
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
        val THEME_ID = stringPreferencesKey("theme_id")
        val ACTIVE_CUSTOM_THEME_ID = stringPreferencesKey("active_custom_theme_id")
        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
        val CUSTOM_THEMES_JSON = stringPreferencesKey("custom_themes_json")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
        val GRID_SPACING_DP = intPreferencesKey("grid_spacing_dp")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val NAV_STYLE = stringPreferencesKey("navigation_style")
        val FACE_ANALYSIS_ENABLED = booleanPreferencesKey("face_analysis_enabled")
        val FACE_GROUPING_ENABLED = booleanPreferencesKey("face_grouping_enabled")
        val AUTO_INDEX_ON_LAUNCH = booleanPreferencesKey("auto_index_on_launch")
        val LAST_FULL_INDEX_AT = longPreferencesKey("last_full_index_at")
        val SEMANTIC_SEARCH_ENABLED = booleanPreferencesKey("semantic_search_enabled")
        val VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val VAULT_PIN_SALT = stringPreferencesKey("vault_pin_salt")
        val VAULT_BIOMETRIC_ENABLED = booleanPreferencesKey("vault_biometric_enabled")
        val VAULT_AUTO_LOCK_SEC = intPreferencesKey("vault_auto_lock_sec")
        val VAULT_FAILED_ATTEMPTS = intPreferencesKey("vault_failed_attempts")
        val VAULT_LOCKOUT_UNTIL = longPreferencesKey("vault_lockout_until")
    }

    val preferences: Flow<GalleryPreferences> = context.galleryDataStore.data.map { prefs ->
        val customThemes = prefs[Keys.CUSTOM_THEMES_JSON]?.let { raw ->
            try {
                json.decodeFromString<List<CustomThemeProfile>>(raw)
            } catch (_: Throwable) {
                emptyList()
            }
        } ?: emptyList()

        val navStyle = prefs[Keys.NAV_STYLE]?.let { raw ->
            NavigationStyle.entries.firstOrNull { it.name == raw }
        } ?: NavigationStyle.STANDARD_BAR

        GalleryPreferences(
            themeId = prefs[Keys.THEME_ID] ?: GalleryThemePreset.OBSIDIAN_TITANIUM.id,
            activeCustomThemeId = prefs[Keys.ACTIVE_CUSTOM_THEME_ID] ?: "",
            customAccentHexOverride = prefs[Keys.CUSTOM_ACCENT_HEX] ?: "",
            customThemes = customThemes,
            gridColumns = (prefs[Keys.GRID_COLUMNS] ?: 3).coerceIn(2, 5),
            cornerRadiusDp = (prefs[Keys.CORNER_RADIUS_DP] ?: 14).coerceIn(0, 24),
            gridSpacingDp = (prefs[Keys.GRID_SPACING_DP] ?: 8).coerceIn(2, 16),
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            navigationStyle = navStyle,
            faceAnalysisEnabled = prefs[Keys.FACE_ANALYSIS_ENABLED] ?: false,
            faceGroupingEnabled = prefs[Keys.FACE_GROUPING_ENABLED] ?: false,
            autoIndexOnLaunch = prefs[Keys.AUTO_INDEX_ON_LAUNCH] ?: true,
            lastFullIndexAtMillis = prefs[Keys.LAST_FULL_INDEX_AT] ?: 0L,
            semanticSearchEnabled = prefs[Keys.SEMANTIC_SEARCH_ENABLED] ?: true,
            vaultPinHash = prefs[Keys.VAULT_PIN_HASH] ?: "",
            vaultPinSalt = prefs[Keys.VAULT_PIN_SALT] ?: "",
            vaultBiometricEnabled = prefs[Keys.VAULT_BIOMETRIC_ENABLED] ?: true,
            vaultAutoLockSeconds = prefs[Keys.VAULT_AUTO_LOCK_SEC] ?: 60,
            vaultFailedAttempts = prefs[Keys.VAULT_FAILED_ATTEMPTS] ?: 0,
            vaultLockoutUntilMillis = prefs[Keys.VAULT_LOCKOUT_UNTIL] ?: 0L,
        )
    }

    suspend fun setThemeId(themeId: String) {
        context.galleryDataStore.edit {
            it[Keys.THEME_ID] = themeId
            it[Keys.ACTIVE_CUSTOM_THEME_ID] = ""
        }
    }

    suspend fun setCustomAccentHex(hex: String) {
        context.galleryDataStore.edit { it[Keys.CUSTOM_ACCENT_HEX] = hex.trim() }
    }

    suspend fun saveCustomTheme(profile: CustomThemeProfile, activate: Boolean = true) {
        context.galleryDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_THEMES_JSON]?.let {
                try {
                    json.decodeFromString<List<CustomThemeProfile>>(it)
                } catch (_: Throwable) {
                    emptyList()
                }
            } ?: emptyList()
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
            val current = prefs[Keys.CUSTOM_THEMES_JSON]?.let {
                try {
                    json.decodeFromString<List<CustomThemeProfile>>(it)
                } catch (_: Throwable) {
                    emptyList()
                }
            } ?: emptyList()
            prefs[Keys.CUSTOM_THEMES_JSON] = json.encodeToString(current.filterNot { it.id == id })
            if (prefs[Keys.ACTIVE_CUSTOM_THEME_ID] == id) {
                prefs[Keys.ACTIVE_CUSTOM_THEME_ID] = ""
            }
        }
    }

    suspend fun setGridColumns(columns: Int) {
        context.galleryDataStore.edit { it[Keys.GRID_COLUMNS] = columns.coerceIn(2, 5) }
    }

    suspend fun setCornerRadiusDp(radius: Int) {
        context.galleryDataStore.edit { it[Keys.CORNER_RADIUS_DP] = radius.coerceIn(0, 24) }
    }

    suspend fun setGridSpacingDp(spacing: Int) {
        context.galleryDataStore.edit { it[Keys.GRID_SPACING_DP] = spacing.coerceIn(2, 16) }
    }

    suspend fun setReducedMotion(reduced: Boolean) {
        context.galleryDataStore.edit { it[Keys.REDUCED_MOTION] = reduced }
    }

    suspend fun setNavigationStyle(style: NavigationStyle) {
        context.galleryDataStore.edit { it[Keys.NAV_STYLE] = style.name }
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

    suspend fun applyImportedSettings(imported: GalleryPreferences) {
        context.galleryDataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = imported.themeId
            prefs[Keys.ACTIVE_CUSTOM_THEME_ID] = imported.activeCustomThemeId
            prefs[Keys.CUSTOM_ACCENT_HEX] = imported.customAccentHexOverride
            prefs[Keys.CUSTOM_THEMES_JSON] = json.encodeToString(imported.customThemes)
            prefs[Keys.GRID_COLUMNS] = imported.gridColumns.coerceIn(2, 5)
            prefs[Keys.CORNER_RADIUS_DP] = imported.cornerRadiusDp.coerceIn(0, 24)
            prefs[Keys.GRID_SPACING_DP] = imported.gridSpacingDp.coerceIn(2, 16)
            prefs[Keys.REDUCED_MOTION] = imported.reducedMotion
            prefs[Keys.NAV_STYLE] = imported.navigationStyle.name
            prefs[Keys.AUTO_INDEX_ON_LAUNCH] = imported.autoIndexOnLaunch
            prefs[Keys.SEMANTIC_SEARCH_ENABLED] = imported.semanticSearchEnabled
        }
    }
}
