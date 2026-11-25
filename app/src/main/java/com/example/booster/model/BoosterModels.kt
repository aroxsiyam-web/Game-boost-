package com.example.booster.model

import androidx.annotation.DrawableRes
import com.example.R

enum class ThermalStateLevel(val label: String) {
    COOL("COOL"),
    WARM("WARM"),
    HOT("THROTTLE GUARD"),
    UNKNOWN("NOMINAL"),
}

data class CpuCoreClock(
    val coreIndex: Int,
    val currentMhz: Int?,
    val maxMhz: Int?,
)

data class ThermalZoneReading(
    val name: String,
    val tempCelsius: Float,
)

data class HardwareTelemetry(
    val totalRamMb: Long = 0L,
    val usedRamMb: Long = 0L,
    val availableRamMb: Long = 0L,
    val ramUsagePercent: Float = 0f,
    val cachedRamMb: Long? = null,
    val isLowMemory: Boolean = false,
    val cpuCores: Int = 0,
    val cpuLoadPercent: Float? = null,
    val cpuLoadSource: String = "Kernel",
    val cpuClocks: List<CpuCoreClock> = emptyList(),
    val socLabel: String = "",
    val abiLabel: String = "",
    val batteryTempC: Float? = null,
    val batteryLevelPct: Int = 0,
    val isCharging: Boolean = false,
    val batteryVoltageMv: Int = 0,
    val systemThermalStatus: String = "Nominal",
    val thermalLevel: ThermalStateLevel = ThermalStateLevel.COOL,
    val thermalZones: List<ThermalZoneReading> = emptyList(),
    val isPowerSaveMode: Boolean = false,
    val storageFreeGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val refreshRateHz: Float = 60f,
    val supportedRefreshRatesHz: List<Float> = emptyList(),
    val uiFrameTimeMs: Float = 16.6f,
    val networkLabel: String = "Checking...",
    val ramHistory: List<Float> = emptyList(),
    val cpuHistory: List<Float> = emptyList(),
)

enum class GameProfileId(val storageKey: String) {
    FREE_FIRE("free_fire"),
    PUBG_MOBILE("pubg_mobile"),
    MINECRAFT("minecraft"),
}

enum class OverlayBoostMode(
    val storageKey: String,
    val displayName: String,
    val shortBadge: String,
    val summary: String,
) {
    POWER_SAVE(
        storageKey = "power_save",
        displayName = "Power Save",
        shortBadge = "PWR SAVE",
        summary = "Battery-first: trims background apps, caps brightness (if granted), and cools thermal load.",
    ),
    BALANCED(
        storageKey = "balanced",
        displayName = "Balanced",
        shortBadge = "BALANCED",
        summary = "Thermal-safe smooth play: Focus Shield DND + safe background trim + steady clocks.",
    ),
    ADVANCE(
        storageKey = "advance",
        displayName = "Advance",
        shortBadge = "ADVANCE",
        summary = "Maximum safe responsiveness: Focus Shield, background trim, keep-awake & Shizuku Game Mode.",
    ),
}

data class GameProfile(
    val id: GameProfileId,
    val name: String,
    val tagline: String,
    val packageCandidates: List<String>,
    val installedPackage: String? = null,
    val installedVersion: String? = null,
    @param:DrawableRes val artRes: Int,
    val dndFocusShield: Boolean = true,
    val trimBackgroundApps: Boolean = true,
    val keepScreenOn: Boolean = true,
    val lockBrightness: Boolean = false,
    val targetBrightnessPct: Int = 75,
    val shizukuReducedAnimScale: Boolean = false,
    val shizukuPerformanceGameMode: Boolean = false,
    val thermalGuardLimitC: Int = 42,
    val optimizationSummary: String = "",
) {
    val isInstalled: Boolean get() = installedPackage != null

    companion object {
        fun defaults(): List<GameProfile> = listOf(
            GameProfile(
                id = GameProfileId.FREE_FIRE,
                name = "Free Fire",
                tagline = "Rapid Touch Response & Memory Headroom",
                packageCandidates = listOf("com.dts.freefiremax", "com.dts.freefireth"),
                artRes = R.drawable.img_profile_freefire,
                dndFocusShield = true,
                trimBackgroundApps = true,
                keepScreenOn = true,
                lockBrightness = false,
                targetBrightnessPct = 80,
                shizukuReducedAnimScale = false,
                shizukuPerformanceGameMode = true,
                thermalGuardLimitC = 42,
                optimizationSummary = "Frees cached background apps, enables Focus Shield, keeps display awake, and requests Android Performance Game Mode via authorized Shizuku.",
            ),
            GameProfile(
                id = GameProfileId.PUBG_MOBILE,
                name = "PUBG Mobile",
                tagline = "Sustained Thermal-Safe Battleground Stability",
                packageCandidates = listOf(
                    "com.tencent.ig",
                    "com.pubg.imobile",
                    "com.rekoo.pubgm",
                    "com.vng.pubgmobile",
                ),
                artRes = R.drawable.img_profile_pubg,
                dndFocusShield = true,
                trimBackgroundApps = true,
                keepScreenOn = true,
                lockBrightness = true,
                targetBrightnessPct = 70,
                shizukuReducedAnimScale = true,
                shizukuPerformanceGameMode = true,
                thermalGuardLimitC = 41,
                optimizationSummary = "Balances sustained battery thermals (<41°C guardrail), stabilizes brightness to curb display heat, clears background memory, and trims transition animations.",
            ),
            GameProfile(
                id = GameProfileId.MINECRAFT,
                name = "Minecraft",
                tagline = "Chunk-Load RAM Reservation & Smooth Frame Pacing",
                packageCandidates = listOf("com.mojang.minecraftpe"),
                artRes = R.drawable.img_profile_minecraft,
                dndFocusShield = true,
                trimBackgroundApps = true,
                keepScreenOn = true,
                lockBrightness = false,
                targetBrightnessPct = 75,
                shizukuReducedAnimScale = false,
                shizukuPerformanceGameMode = false,
                thermalGuardLimitC = 43,
                optimizationSummary = "Reclaims background RAM prior to world/chunk loading, suppresses distracting alerts, and preserves native thermal throttling safeguards.",
            ),
        )
    }
}

data class BackgroundAppItem(
    val packageName: String,
    val appName: String,
    val isProtected: Boolean,
    val protectionReason: String?,
    val isSelected: Boolean,
)

data class ReversibleSnapshot(
    val isActive: Boolean = false,
    val activeProfileId: GameProfileId? = null,
    val activeProfileName: String? = null,
    val activatedAtEpochMs: Long = 0L,
    val dndChanged: Boolean = false,
    val originalDndFilter: Int? = null,
    val brightnessChanged: Boolean = false,
    val originalBrightnessMode: Int? = null,
    val originalBrightnessValue: Int? = null,
    val keepScreenOnChanged: Boolean = false,
    val animScaleChanged: Boolean = false,
    val originalWindowScale: String? = null,
    val originalTransitionScale: String? = null,
    val originalAnimatorScale: String? = null,
    val gameModeChanged: Boolean = false,
    val gameModePackage: String? = null,
    val trimmedPackagesCount: Int = 0,
    val freedRamMb: Long = 0L,
) {
    val activeChangesCount: Int
        get() = listOf(
            dndChanged,
            brightnessChanged,
            keepScreenOnChanged,
            animScaleChanged,
            gameModeChanged,
            trimmedPackagesCount > 0,
        ).count { it }
}

data class ShizukuStatus(
    val isInstalled: Boolean = false,
    val isBinderAlive: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isServiceBound: Boolean = false,
    val privilegeUid: Int? = null,
    val shellIdentity: String? = null,
    val lastMessage: String = "Waiting for Shizuku service",
) {
    val privilegeLabel: String
        get() = when (privilegeUid) {
            0 -> "Root (UID 0)"
            2000 -> "ADB Shell (UID 2000)"
            null -> "Not Connected"
            else -> "UID $privilegeUid"
        }
}

data class TermuxCommandTemplate(
    val id: String,
    val title: String,
    val description: String,
    val executablePath: String,
    val arguments: List<String>,
    val workingDirectory: String,
    val runInBackground: Boolean,
    val safetyNote: String,
)

data class TermuxStatus(
    val isInstalled: Boolean = false,
    val hasRunCommandPermission: Boolean = false,
    val lastDispatchedCommand: String? = null,
)

sealed interface PendingConsentAction {
    val title: String
    val summary: String
    val commandPreview: String
    val rollbackPreview: String

    data class ShizukuSystemTweak(
        override val title: String,
        override val summary: String,
        override val commandPreview: String,
        override val rollbackPreview: String,
        val actionType: ShizukuTweakType,
        val targetPackage: String? = null,
    ) : PendingConsentAction

    data class TermuxExecution(
        override val title: String,
        override val summary: String,
        override val commandPreview: String,
        override val rollbackPreview: String,
        val template: TermuxCommandTemplate,
    ) : PendingConsentAction
}

enum class ShizukuTweakType {
    APPLY_ANIMATION_SCALE_05X,
    REVERT_ANIMATION_SCALE,
    SET_GAME_MODE_PERFORMANCE,
    REVERT_GAME_MODE_STANDARD,
    TRIM_SELECTED_APPS,
}

data class BoostLogEntry(
    val id: Long,
    val timeLabel: String,
    val category: String,
    val message: String,
    val isReversible: Boolean = false,
)

data class DevicePermissionsState(
    val hasDndPolicyAccess: Boolean = false,
    val hasWriteSettingsAccess: Boolean = false,
    val hasTermuxPermission: Boolean = false,
    val hasAccessibilityOverlayPermission: Boolean = false,
)
