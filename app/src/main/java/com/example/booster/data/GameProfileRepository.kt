package com.example.booster.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.booster.model.GameProfile
import com.example.booster.model.GameProfileId
import com.example.booster.model.OverlayBoostMode
import com.example.booster.model.ReversibleSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.boosterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ivc_siyam_game_booster_prefs",
)

class GameProfileRepository(private val context: Context) {

    private val dataStore = context.boosterDataStore

    val profilesFlow: Flow<List<GameProfile>> = dataStore.data.map { prefs ->
        val defaults = GameProfile.defaults()
        defaults.map { base ->
            val prefix = "profile_${base.id.storageKey}_"
            val installed = detectInstalledPackage(base.packageCandidates)
            base.copy(
                installedPackage = installed?.first,
                installedVersion = installed?.second,
                dndFocusShield = prefs[booleanPreferencesKey("${prefix}dnd")] ?: base.dndFocusShield,
                trimBackgroundApps = prefs[booleanPreferencesKey("${prefix}trim")] ?: base.trimBackgroundApps,
                keepScreenOn = prefs[booleanPreferencesKey("${prefix}keep_on")] ?: base.keepScreenOn,
                lockBrightness = prefs[booleanPreferencesKey("${prefix}lock_bright")] ?: base.lockBrightness,
                targetBrightnessPct = prefs[intPreferencesKey("${prefix}bright_pct")] ?: base.targetBrightnessPct,
                shizukuReducedAnimScale = prefs[booleanPreferencesKey("${prefix}anim_scale")] ?: base.shizukuReducedAnimScale,
                shizukuPerformanceGameMode = prefs[booleanPreferencesKey("${prefix}game_mode")] ?: base.shizukuPerformanceGameMode,
                thermalGuardLimitC = prefs[intPreferencesKey("${prefix}thermal_limit")] ?: base.thermalGuardLimitC,
            )
        }
    }

    val selectedProfileIdFlow: Flow<GameProfileId> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_SELECTED_PROFILE]
        GameProfileId.entries.firstOrNull { it.storageKey == raw } ?: GameProfileId.FREE_FIRE
    }

    val fpsOverlayEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_FPS_OVERLAY_ENABLED] ?: false
    }

    val overlayBoostModeFlow: Flow<OverlayBoostMode> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_OVERLAY_BOOST_MODE]
        OverlayBoostMode.entries.firstOrNull { it.storageKey == raw } ?: OverlayBoostMode.BALANCED
    }

    val snapshotFlow: Flow<ReversibleSnapshot> = dataStore.data.map { prefs ->
        val profileRaw = prefs[KEY_SNAP_PROFILE_ID]
        ReversibleSnapshot(
            isActive = prefs[KEY_SNAP_ACTIVE] ?: false,
            activeProfileId = GameProfileId.entries.firstOrNull { it.storageKey == profileRaw },
            activeProfileName = prefs[KEY_SNAP_PROFILE_NAME],
            activatedAtEpochMs = prefs[KEY_SNAP_TIMESTAMP] ?: 0L,
            dndChanged = prefs[KEY_SNAP_DND_CHANGED] ?: false,
            originalDndFilter = prefs[KEY_SNAP_ORIG_DND],
            brightnessChanged = prefs[KEY_SNAP_BRIGHT_CHANGED] ?: false,
            originalBrightnessMode = prefs[KEY_SNAP_ORIG_BRIGHT_MODE],
            originalBrightnessValue = prefs[KEY_SNAP_ORIG_BRIGHT_VAL],
            keepScreenOnChanged = prefs[KEY_SNAP_KEEP_ON] ?: false,
            animScaleChanged = prefs[KEY_SNAP_ANIM_CHANGED] ?: false,
            originalWindowScale = prefs[KEY_SNAP_ORIG_WIN_SCALE],
            originalTransitionScale = prefs[KEY_SNAP_ORIG_TRANS_SCALE],
            originalAnimatorScale = prefs[KEY_SNAP_ORIG_ANIM_SCALE],
            gameModeChanged = prefs[KEY_SNAP_GAMEMODE_CHANGED] ?: false,
            gameModePackage = prefs[KEY_SNAP_GAMEMODE_PKG],
            trimmedPackagesCount = prefs[KEY_SNAP_TRIMMED_COUNT] ?: 0,
            freedRamMb = prefs[KEY_SNAP_FREED_RAM_MB] ?: 0L,
        )
    }

    suspend fun selectProfile(profileId: GameProfileId) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_PROFILE] = profileId.storageKey
        }
    }

    suspend fun setFpsOverlayEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_FPS_OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setOverlayBoostMode(mode: OverlayBoostMode) {
        dataStore.edit { prefs ->
            prefs[KEY_OVERLAY_BOOST_MODE] = mode.storageKey
        }
    }

    suspend fun updateProfile(profile: GameProfile) {
        val prefix = "profile_${profile.id.storageKey}_"
        dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("${prefix}dnd")] = profile.dndFocusShield
            prefs[booleanPreferencesKey("${prefix}trim")] = profile.trimBackgroundApps
            prefs[booleanPreferencesKey("${prefix}keep_on")] = profile.keepScreenOn
            prefs[booleanPreferencesKey("${prefix}lock_bright")] = profile.lockBrightness
            prefs[intPreferencesKey("${prefix}bright_pct")] = profile.targetBrightnessPct
            prefs[booleanPreferencesKey("${prefix}anim_scale")] = profile.shizukuReducedAnimScale
            prefs[booleanPreferencesKey("${prefix}game_mode")] = profile.shizukuPerformanceGameMode
            prefs[intPreferencesKey("${prefix}thermal_limit")] = profile.thermalGuardLimitC
        }
    }

    suspend fun saveSnapshot(snapshot: ReversibleSnapshot) {
        dataStore.edit { prefs ->
            prefs[KEY_SNAP_ACTIVE] = snapshot.isActive
            if (snapshot.activeProfileId != null) {
                prefs[KEY_SNAP_PROFILE_ID] = snapshot.activeProfileId.storageKey
            } else {
                prefs.remove(KEY_SNAP_PROFILE_ID)
            }
            if (snapshot.activeProfileName != null) {
                prefs[KEY_SNAP_PROFILE_NAME] = snapshot.activeProfileName
            } else {
                prefs.remove(KEY_SNAP_PROFILE_NAME)
            }
            prefs[KEY_SNAP_TIMESTAMP] = snapshot.activatedAtEpochMs
            prefs[KEY_SNAP_DND_CHANGED] = snapshot.dndChanged
            snapshot.originalDndFilter?.let { prefs[KEY_SNAP_ORIG_DND] = it } ?: prefs.remove(KEY_SNAP_ORIG_DND)
            prefs[KEY_SNAP_BRIGHT_CHANGED] = snapshot.brightnessChanged
            snapshot.originalBrightnessMode?.let { prefs[KEY_SNAP_ORIG_BRIGHT_MODE] = it }
                ?: prefs.remove(KEY_SNAP_ORIG_BRIGHT_MODE)
            snapshot.originalBrightnessValue?.let { prefs[KEY_SNAP_ORIG_BRIGHT_VAL] = it }
                ?: prefs.remove(KEY_SNAP_ORIG_BRIGHT_VAL)
            prefs[KEY_SNAP_KEEP_ON] = snapshot.keepScreenOnChanged
            prefs[KEY_SNAP_ANIM_CHANGED] = snapshot.animScaleChanged
            snapshot.originalWindowScale?.let { prefs[KEY_SNAP_ORIG_WIN_SCALE] = it }
                ?: prefs.remove(KEY_SNAP_ORIG_WIN_SCALE)
            snapshot.originalTransitionScale?.let { prefs[KEY_SNAP_ORIG_TRANS_SCALE] = it }
                ?: prefs.remove(KEY_SNAP_ORIG_TRANS_SCALE)
            snapshot.originalAnimatorScale?.let { prefs[KEY_SNAP_ORIG_ANIM_SCALE] = it }
                ?: prefs.remove(KEY_SNAP_ORIG_ANIM_SCALE)
            prefs[KEY_SNAP_GAMEMODE_CHANGED] = snapshot.gameModeChanged
            snapshot.gameModePackage?.let { prefs[KEY_SNAP_GAMEMODE_PKG] = it }
                ?: prefs.remove(KEY_SNAP_GAMEMODE_PKG)
            prefs[KEY_SNAP_TRIMMED_COUNT] = snapshot.trimmedPackagesCount
            prefs[KEY_SNAP_FREED_RAM_MB] = snapshot.freedRamMb
        }
    }

    suspend fun clearSnapshot() {
        saveSnapshot(ReversibleSnapshot(isActive = false))
    }

    private fun detectInstalledPackage(candidates: List<String>): Pair<String, String>? {
        val pm = context.packageManager
        for (pkg in candidates) {
            val info = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
            }.getOrNull()
            if (info != null) {
                return pkg to (info.versionName ?: "Installed")
            }
        }
        return null
    }

    companion object {
        private val KEY_SELECTED_PROFILE = stringPreferencesKey("selected_profile_id")
        private val KEY_SNAP_ACTIVE = booleanPreferencesKey("snap_active")
        private val KEY_SNAP_PROFILE_ID = stringPreferencesKey("snap_profile_id")
        private val KEY_SNAP_PROFILE_NAME = stringPreferencesKey("snap_profile_name")
        private val KEY_SNAP_TIMESTAMP = longPreferencesKey("snap_timestamp")
        private val KEY_SNAP_DND_CHANGED = booleanPreferencesKey("snap_dnd_changed")
        private val KEY_SNAP_ORIG_DND = intPreferencesKey("snap_orig_dnd")
        private val KEY_SNAP_BRIGHT_CHANGED = booleanPreferencesKey("snap_bright_changed")
        private val KEY_SNAP_ORIG_BRIGHT_MODE = intPreferencesKey("snap_orig_bright_mode")
        private val KEY_SNAP_ORIG_BRIGHT_VAL = intPreferencesKey("snap_orig_bright_val")
        private val KEY_SNAP_KEEP_ON = booleanPreferencesKey("snap_keep_on")
        private val KEY_SNAP_ANIM_CHANGED = booleanPreferencesKey("snap_anim_changed")
        private val KEY_SNAP_ORIG_WIN_SCALE = stringPreferencesKey("snap_orig_win_scale")
        private val KEY_SNAP_ORIG_TRANS_SCALE = stringPreferencesKey("snap_orig_trans_scale")
        private val KEY_SNAP_ORIG_ANIM_SCALE = stringPreferencesKey("snap_orig_anim_scale")
        private val KEY_SNAP_GAMEMODE_CHANGED = booleanPreferencesKey("snap_gamemode_changed")
        private val KEY_SNAP_GAMEMODE_PKG = stringPreferencesKey("snap_gamemode_pkg")
        private val KEY_SNAP_TRIMMED_COUNT = intPreferencesKey("snap_trimmed_count")
        private val KEY_SNAP_FREED_RAM_MB = longPreferencesKey("snap_freed_ram_mb")
        private val KEY_FPS_OVERLAY_ENABLED = booleanPreferencesKey("fps_overlay_enabled")
        private val KEY_OVERLAY_BOOST_MODE = stringPreferencesKey("overlay_boost_mode")
    }
}
