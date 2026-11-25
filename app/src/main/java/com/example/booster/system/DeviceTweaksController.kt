package com.example.booster.system

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import com.example.booster.data.BackgroundAppRepository
import com.example.booster.model.DevicePermissionsState
import com.example.booster.model.GameProfile
import com.example.booster.model.OverlayBoostMode
import com.example.booster.model.ReversibleSnapshot
import kotlin.math.roundToInt

data class BoostApplyOutcome(
    val snapshot: ReversibleSnapshot,
    val messages: List<String>,
)

class DeviceTweaksController(
    private val context: Context,
    private val backgroundAppRepository: BackgroundAppRepository,
    private val shizukuManager: ShizukuManager,
) {

    fun inspectPermissions(): DevicePermissionsState {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dndGranted = nm.isNotificationPolicyAccessGranted
        val writeSettingsGranted = Settings.System.canWrite(context)
        val termuxGranted = TermuxBridge(context).checkStatus().hasRunCommandPermission
        val accessibilityGranted = FpsOverlayAccessibilityService.isAccessibilityServiceEnabled(context)
        return DevicePermissionsState(
            hasDndPolicyAccess = dndGranted,
            hasWriteSettingsAccess = writeSettingsGranted,
            hasTermuxPermission = termuxGranted,
            hasAccessibilityOverlayPermission = accessibilityGranted,
        )
    }

    fun applyBoostProfile(
        profile: GameProfile,
        boostMode: OverlayBoostMode,
        selectedPackagesToTrim: List<String>,
        previousSnapshot: ReversibleSnapshot,
    ): BoostApplyOutcome {
        // Never stack dirty baselines: if already active, keep original values from previousSnapshot
        val messages = mutableListOf<String>()
        messages += "Applied ${boostMode.displayName} boost mode"

        var dndChanged = previousSnapshot.dndChanged
        var origDnd = previousSnapshot.originalDndFilter

        val shouldEnableDnd = profile.dndFocusShield && boostMode != OverlayBoostMode.POWER_SAVE
        if (shouldEnableDnd) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                if (!dndChanged) {
                    origDnd = nm.currentInterruptionFilter
                }
                runCatching {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    dndChanged = true
                    messages += "Focus Shield (DND Priority) enabled — original filter saved"
                }.onFailure {
                    messages += "Focus Shield skipped: ${it.message}"
                }
            } else {
                messages += "Focus Shield skipped (grant Do Not Disturb Access in Tools to enable)"
            }
        }

        var brightChanged = previousSnapshot.brightnessChanged
        var origBrightMode = previousSnapshot.originalBrightnessMode
        var origBrightVal = previousSnapshot.originalBrightnessValue

        val shouldAdjustBrightness = when (boostMode) {
            OverlayBoostMode.POWER_SAVE -> true
            OverlayBoostMode.BALANCED -> profile.lockBrightness
            OverlayBoostMode.ADVANCE -> profile.lockBrightness
        }
        val targetPct = when (boostMode) {
            OverlayBoostMode.POWER_SAVE -> profile.targetBrightnessPct.coerceAtMost(50)
            OverlayBoostMode.BALANCED -> profile.targetBrightnessPct
            OverlayBoostMode.ADVANCE -> profile.targetBrightnessPct.coerceAtLeast(75)
        }

        if (shouldAdjustBrightness) {
            if (Settings.System.canWrite(context)) {
                runCatching {
                    val cr = context.contentResolver
                    if (!brightChanged) {
                        origBrightMode = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE)
                        origBrightVal = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS)
                    }
                    Settings.System.putInt(
                        cr,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    )
                    val target255 = ((targetPct.coerceIn(15, 100) / 100f) * 255f)
                        .roundToInt()
                        .coerceIn(25, 255)
                    Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, target255)
                    brightChanged = true
                    messages += "Display brightness set to $targetPct% (${boostMode.displayName}, reversible)"
                }.onFailure {
                    messages += "System brightness unchanged: ${it.message}"
                }
            } else if (profile.lockBrightness) {
                messages += "System brightness lock skipped (grant Modify System Settings if desired)"
            }
        }

        var trimmedCount = previousSnapshot.trimmedPackagesCount
        var freedRamMb = previousSnapshot.freedRamMb

        if (profile.trimBackgroundApps && selectedPackagesToTrim.isNotEmpty()) {
            val trimResult = backgroundAppRepository.trimBackgroundProcessesSafely(selectedPackagesToTrim)
            trimmedCount = trimResult.trimmedCount
            freedRamMb = trimResult.freedRamMb
            messages += "Safely trimmed ${trimResult.trimmedCount} background apps (+${trimResult.freedRamMb} MB available)"
        }

        val keepScreenOn = profile.keepScreenOn && boostMode != OverlayBoostMode.POWER_SAVE
        if (keepScreenOn) {
            messages += "Keep-Screen-On active inside booster session"
        }

        val snapshot = previousSnapshot.copy(
            isActive = true,
            activeProfileId = profile.id,
            activeProfileName = "${profile.name} • ${boostMode.displayName}",
            activatedAtEpochMs = System.currentTimeMillis(),
            dndChanged = dndChanged,
            originalDndFilter = origDnd,
            brightnessChanged = brightChanged,
            originalBrightnessMode = origBrightMode,
            originalBrightnessValue = origBrightVal,
            keepScreenOnChanged = keepScreenOn,
            trimmedPackagesCount = trimmedCount,
            freedRamMb = freedRamMb,
        )

        return BoostApplyOutcome(snapshot = snapshot, messages = messages)
    }

    suspend fun revertAll(snapshot: ReversibleSnapshot): List<String> {
        val messages = mutableListOf<String>()

        if (snapshot.dndChanged && snapshot.originalDndFilter != null) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                runCatching {
                    nm.setInterruptionFilter(snapshot.originalDndFilter)
                    messages += "Restored original Do Not Disturb mode"
                }.onFailure {
                    messages += "Could not restore DND: ${it.message}"
                }
            }
        }

        if (snapshot.brightnessChanged && Settings.System.canWrite(context)) {
            runCatching {
                val cr = context.contentResolver
                snapshot.originalBrightnessValue?.let {
                    Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, it)
                }
                snapshot.originalBrightnessMode?.let {
                    Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, it)
                }
                messages += "Restored original screen brightness & mode"
            }.onFailure {
                messages += "Could not restore brightness: ${it.message}"
            }
        }

        if (snapshot.animScaleChanged) {
            if (shizukuManager.status.value.isServiceBound) {
                val w = snapshot.originalWindowScale ?: "1.0"
                val t = snapshot.originalTransitionScale ?: "1.0"
                val a = snapshot.originalAnimatorScale ?: "1.0"
                val ok = shizukuManager.setAnimationScales(w, t, a)
                if (ok) {
                    messages += "Restored system animation scales via Shizuku ($w/$t/$a)"
                } else {
                    messages += "Failed to revert animation scales (check Shizuku connection)"
                }
            } else {
                messages += "Animation scale revert skipped: reconnect Shizuku to restore ($snapshot.originalWindowScale)"
            }
        }

        if (snapshot.gameModeChanged && snapshot.gameModePackage != null) {
            if (shizukuManager.status.value.isServiceBound) {
                val res = shizukuManager.setGameMode(snapshot.gameModePackage, "standard")
                messages += "Reverted Game Mode for ${snapshot.gameModePackage}: $res"
            } else {
                messages += "Game Mode revert skipped: reconnect Shizuku to reset ${snapshot.gameModePackage}"
            }
        }

        if (snapshot.keepScreenOnChanged) {
            messages += "Released Keep-Screen-On flag"
        }

        if (messages.isEmpty()) {
            messages += "Cleared boost session state"
        }
        return messages
    }
}
