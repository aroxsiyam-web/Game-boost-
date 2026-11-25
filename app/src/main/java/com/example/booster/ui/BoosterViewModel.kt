package com.example.booster.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.booster.data.BackgroundAppRepository
import com.example.booster.data.GameProfileRepository
import com.example.booster.data.HardwareMonitorRepository
import com.example.booster.model.BackgroundAppItem
import com.example.booster.model.BoostLogEntry
import com.example.booster.model.DevicePermissionsState
import com.example.booster.model.GameProfile
import com.example.booster.model.GameProfileId
import com.example.booster.model.HardwareTelemetry
import com.example.booster.model.OverlayBoostMode
import com.example.booster.model.PendingConsentAction
import com.example.booster.model.ReversibleSnapshot
import com.example.booster.model.ShizukuStatus
import com.example.booster.model.ShizukuTweakType
import com.example.booster.model.TermuxCommandTemplate
import com.example.booster.model.TermuxStatus
import com.example.booster.system.DeviceTweaksController
import com.example.booster.system.FpsOverlayAccessibilityService
import com.example.booster.system.OverlayRuntimeStatus
import com.example.booster.system.ShizukuManager
import com.example.booster.system.TermuxBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BoosterUiState(
    val telemetry: HardwareTelemetry = HardwareTelemetry(),
    val profiles: List<GameProfile> = GameProfile.defaults(),
    val selectedProfileId: GameProfileId = GameProfileId.FREE_FIRE,
    val snapshot: ReversibleSnapshot = ReversibleSnapshot(),
    val backgroundApps: List<BackgroundAppItem> = emptyList(),
    val appSearchQuery: String = "",
    val shizukuStatus: ShizukuStatus = ShizukuStatus(),
    val termuxStatus: TermuxStatus = TermuxStatus(),
    val termuxTemplates: List<TermuxCommandTemplate> = emptyList(),
    val customTermuxInput: String = "uname -a && free -h",
    val customTermuxBackground: Boolean = false,
    val permissions: DevicePermissionsState = DevicePermissionsState(),
    val fpsOverlayEnabled: Boolean = false,
    val overlayBoostMode: OverlayBoostMode = OverlayBoostMode.BALANCED,
    val overlayRuntimeStatus: OverlayRuntimeStatus = OverlayRuntimeStatus(),
    val showAccessibilityPermissionDialog: Boolean = false,
    val pendingConsentAction: PendingConsentAction? = null,
    val logs: List<BoostLogEntry> = emptyList(),
    val isBoosting: Boolean = false,
) {
    val selectedProfile: GameProfile
        get() = profiles.firstOrNull { it.id == selectedProfileId } ?: profiles.first()

    val filteredApps: List<BackgroundAppItem>
        get() {
            val q = appSearchQuery.trim()
            if (q.isEmpty()) return backgroundApps
            return backgroundApps.filter {
                it.appName.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }
}

class BoosterViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val hardwareRepo = HardwareMonitorRepository(appContext)
    private val profileRepo = GameProfileRepository(appContext)
    private val backgroundAppRepo = BackgroundAppRepository(appContext)
    private val shizukuManager = ShizukuManager(appContext)
    private val termuxBridge = TermuxBridge(appContext)
    private val tweaksController = DeviceTweaksController(
        context = appContext,
        backgroundAppRepository = backgroundAppRepo,
        shizukuManager = shizukuManager,
    )

    private val _uiState = MutableStateFlow(
        BoosterUiState(termuxTemplates = termuxBridge.builtInTemplates()),
    )
    val uiState: StateFlow<BoosterUiState> = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    @Volatile
    private var latestUiFrameTimeMs: Float = 16.6f
    private var logCounter = 1L

    init {
        shizukuManager.register()
        observeDataStores()
        startTelemetryLoop()
        refreshSystemIntegrations()
        refreshBackgroundApps()
        appendLog(
            category = "Telemetry",
            message = "Real hardware telemetry initialized (thermal protections preserved)",
        )
    }

    override fun onCleared() {
        super.onCleared()
        shizukuManager.unregister()
    }

    fun reportDashboardFrameTimeMs(frameTimeMs: Float) {
        latestUiFrameTimeMs = frameTimeMs.coerceIn(1f, 200f)
    }

    private fun observeDataStores() {
        viewModelScope.launch {
            combine(
                profileRepo.profilesFlow,
                profileRepo.selectedProfileIdFlow,
                profileRepo.snapshotFlow,
                shizukuManager.status,
                profileRepo.fpsOverlayEnabledFlow,
                profileRepo.overlayBoostModeFlow,
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val profiles = values[0] as List<GameProfile>
                val selectedId = values[1] as GameProfileId
                val snapshot = values[2] as ReversibleSnapshot
                val shizuku = values[3] as ShizukuStatus
                val overlayEnabled = values[4] as Boolean
                val boostMode = values[5] as OverlayBoostMode
                Hex(profiles, selectedId, snapshot, shizuku, overlayEnabled, boostMode)
            }.collect { (profiles, selectedId, snapshot, shizuku, overlayEnabled, boostMode) ->
                _uiState.update { current ->
                    current.copy(
                        profiles = profiles,
                        selectedProfileId = selectedId,
                        snapshot = snapshot,
                        shizukuStatus = shizuku,
                        fpsOverlayEnabled = overlayEnabled,
                        overlayBoostMode = boostMode,
                    )
                }
            }
        }

        viewModelScope.launch {
            FpsOverlayAccessibilityService.runtimeStatus.collect { overlayStatus ->
                _uiState.update { current ->
                    current.copy(
                        overlayRuntimeStatus = overlayStatus,
                        permissions = current.permissions.copy(
                            hasAccessibilityOverlayPermission =
                                overlayStatus.isServiceConnected ||
                                    FpsOverlayAccessibilityService.isAccessibilityServiceEnabled(appContext),
                        ),
                    )
                }
            }
        }
    }

    private fun startTelemetryLoop() {
        viewModelScope.launch {
            while (isActive) {
                val sample = withContext(Dispatchers.IO) {
                    hardwareRepo.sample(latestUiFrameTimeMs)
                }
                _uiState.update { it.copy(telemetry = sample) }
                delay(1500L)
            }
        }
    }

    fun refreshSystemIntegrations() {
        shizukuManager.refreshStatus()
        val permissions = tweaksController.inspectPermissions()
        val termux = termuxBridge.checkStatus(_uiState.value.termuxStatus.lastDispatchedCommand)
        _uiState.update {
            it.copy(
                permissions = permissions,
                termuxStatus = termux,
            )
        }
    }

    fun refreshBackgroundApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val prevMap = _uiState.value.backgroundApps.associate { it.packageName to it.isSelected }
            val apps = backgroundAppRepo.discoverLaunchableApps(prevMap)
            _uiState.update { it.copy(backgroundApps = apps) }
        }
    }

    fun updateAppSearchQuery(query: String) {
        _uiState.update { it.copy(appSearchQuery = query) }
    }

    fun toggleBackgroundAppSelection(packageName: String) {
        _uiState.update { state ->
            state.copy(
                backgroundApps = state.backgroundApps.map { app ->
                    if (app.packageName == packageName && !app.isProtected) {
                        app.copy(isSelected = !app.isSelected)
                    } else {
                        app
                    }
                },
            )
        }
    }

    fun selectAllTrimmableApps(selectAll: Boolean) {
        _uiState.update { state ->
            state.copy(
                backgroundApps = state.backgroundApps.map { app ->
                    if (app.isProtected) app else app.copy(isSelected = selectAll)
                },
            )
        }
    }

    fun selectProfile(profileId: GameProfileId) {
        viewModelScope.launch {
            profileRepo.selectProfile(profileId)
            val profileName = _uiState.value.profiles.firstOrNull { it.id == profileId }?.name ?: profileId.name
            appendLog("Profile", "Active profile switched to $profileName")
        }
    }

    fun updateProfile(updated: GameProfile) {
        viewModelScope.launch {
            profileRepo.updateProfile(updated)
            appendLog("Profile", "Updated ${updated.name} optimization rules")
        }
    }

    fun executePrimaryBoost() {
        if (_uiState.value.isBoosting) return
        val state = _uiState.value
        val profile = state.selectedProfile
        val telemetry = state.telemetry

        viewModelScope.launch {
            _uiState.update { it.copy(isBoosting = true) }

            val selectedPkgs = state.backgroundApps
                .filter { !it.isProtected && it.isSelected }
                .map { it.packageName }

            val outcome = withContext(Dispatchers.IO) {
                tweaksController.applyBoostProfile(
                    profile = profile,
                    boostMode = state.overlayBoostMode,
                    selectedPackagesToTrim = selectedPkgs,
                    previousSnapshot = state.snapshot,
                )
            }
            profileRepo.saveSnapshot(outcome.snapshot)

            val freshTelemetry = withContext(Dispatchers.IO) {
                hardwareRepo.sample(latestUiFrameTimeMs)
            }
            _uiState.update { it.copy(telemetry = freshTelemetry, isBoosting = false) }

            outcome.messages.forEach { msg ->
                appendLog("Boost", msg, isReversible = true)
            }

            val temp = telemetry.batteryTempC
            if (temp != null && temp >= profile.thermalGuardLimitC) {
                appendLog(
                    category = "Thermal Guard",
                    message = "Device at ${String.format(Locale.US, "%.1f", temp)}°C (limit ${profile.thermalGuardLimitC}°C). Thermal protections kept active.",
                )
            }

            // If Shizuku is bound and the profile requests an optional privileged tweak, ask consent explicitly!
            val shizukuReady = state.shizukuStatus.isServiceBound
            if (shizukuReady && profile.shizukuPerformanceGameMode && profile.installedPackage != null && !outcome.snapshot.gameModeChanged) {
                promptShizukuGameMode(profile.installedPackage, profile.name)
            } else if (shizukuReady && profile.shizukuReducedAnimScale && !outcome.snapshot.animScaleChanged) {
                promptShizukuAnimationScale()
            }

            emitSnackbar("${profile.name} boost applied (${outcome.snapshot.activeChangesCount} reversible rules)")
        }
    }

    fun revertAllSettings() {
        val currentSnapshot = _uiState.value.snapshot
        if (!currentSnapshot.isActive) {
            emitSnackbar("No active boost modifications to revert")
            return
        }
        viewModelScope.launch {
            val messages = withContext(Dispatchers.IO) {
                tweaksController.revertAll(currentSnapshot)
            }
            profileRepo.clearSnapshot()
            messages.forEach { msg ->
                appendLog("Revert", msg)
            }
            emitSnackbar("Restored original pre-boost device settings")
        }
    }

    fun trimSelectedBackgroundAppsOnly() {
        val selectedPkgs = _uiState.value.backgroundApps
            .filter { !it.isProtected && it.isSelected }
            .map { it.packageName }

        if (selectedPkgs.isEmpty()) {
            emitSnackbar("Select at least one non-protected app to trim")
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                backgroundAppRepo.trimBackgroundProcessesSafely(selectedPkgs)
            }
            val fresh = withContext(Dispatchers.IO) { hardwareRepo.sample(latestUiFrameTimeMs) }
            _uiState.update { it.copy(telemetry = fresh) }
            val msg = "Safely trimmed ${result.trimmedCount} background apps (+${result.freedRamMb} MB available)"
            appendLog("Memory", msg)
            emitSnackbar(msg)
        }
    }

    fun requestShizukuAuthorization() {
        shizukuManager.requestAuthorization()
        appendLog("Shizuku", "Requested user authorization via official Shizuku dialog")
    }

    fun promptShizukuAnimationScale() {
        if (!_uiState.value.shizukuStatus.isServiceBound) {
            emitSnackbar("Authorize and bind Shizuku first")
            return
        }
        _uiState.update {
            it.copy(
                pendingConsentAction = PendingConsentAction.ShizukuSystemTweak(
                    title = "Confirm Shizuku Animation Scale",
                    summary = "Reduces system UI transition animations to 0.5x for snappier app switching. Current baseline values will be saved first.",
                    commandPreview = "settings put global window_animation_scale 0.5\n" +
                        "settings put global transition_animation_scale 0.5\n" +
                        "settings put global animator_duration_scale 0.5",
                    rollbackPreview = "Restores your exact original animation scales via REVERT SETTINGS.",
                    actionType = ShizukuTweakType.APPLY_ANIMATION_SCALE_05X,
                ),
            )
        }
    }

    fun promptShizukuRevertAnimationScale() {
        if (!_uiState.value.shizukuStatus.isServiceBound) {
            emitSnackbar("Authorize and bind Shizuku first")
            return
        }
        val snap = _uiState.value.snapshot
        val w = snap.originalWindowScale ?: "1.0"
        val t = snap.originalTransitionScale ?: "1.0"
        val a = snap.originalAnimatorScale ?: "1.0"
        _uiState.update {
            it.copy(
                pendingConsentAction = PendingConsentAction.ShizukuSystemTweak(
                    title = "Restore Original Animation Scales",
                    summary = "Restores global animation scales back to your recorded baseline.",
                    commandPreview = "settings put global window_animation_scale $w\n" +
                        "settings put global transition_animation_scale $t\n" +
                        "settings put global animator_duration_scale $a",
                    rollbackPreview = "Returns device UI animations to stock state.",
                    actionType = ShizukuTweakType.REVERT_ANIMATION_SCALE,
                ),
            )
        }
    }

    fun promptShizukuGameMode(packageName: String, gameLabel: String) {
        if (!_uiState.value.shizukuStatus.isServiceBound) {
            emitSnackbar("Authorize and bind Shizuku first")
            return
        }
        _uiState.update {
            it.copy(
                pendingConsentAction = PendingConsentAction.ShizukuSystemTweak(
                    title = "Confirm Android Game Mode",
                    summary = "Requests Android's official Performance Game Mode for $gameLabel via Shizuku (Android 12+). Thermal protections remain intact.",
                    commandPreview = "cmd game mode performance $packageName",
                    rollbackPreview = "cmd game mode standard $packageName (on Revert)",
                    actionType = ShizukuTweakType.SET_GAME_MODE_PERFORMANCE,
                    targetPackage = packageName,
                ),
            )
        }
    }

    fun promptShizukuTrimApps() {
        if (!_uiState.value.shizukuStatus.isServiceBound) {
            emitSnackbar("Authorize and bind Shizuku first")
            return
        }
        val selected = _uiState.value.backgroundApps.filter { !it.isProtected && it.isSelected }
        if (selected.isEmpty()) {
            emitSnackbar("Select at least one app in the Apps tab first")
            return
        }
        val preview = selected.take(6).joinToString("\n") { "am kill ${it.packageName}" } +
            if (selected.size > 6) "\n... (${selected.size - 6} more selected apps)" else ""
        _uiState.update {
            it.copy(
                pendingConsentAction = PendingConsentAction.ShizukuSystemTweak(
                    title = "Confirm Shizuku Background Trim",
                    summary = "Trims cached processes for ${selected.size} selected user apps using Shizuku shell privilege. Protected system & game apps are excluded.",
                    commandPreview = preview,
                    rollbackPreview = "Apps reopen normally when launched by the user.",
                    actionType = ShizukuTweakType.TRIM_SELECTED_APPS,
                ),
            )
        }
    }

    fun promptTermuxTemplate(template: TermuxCommandTemplate) {
        val argsString = template.arguments.joinToString(" ") { "\"$it\"" }
        _uiState.update {
            it.copy(
                pendingConsentAction = PendingConsentAction.TermuxExecution(
                    title = "Confirm Termux Command",
                    summary = template.description,
                    commandPreview = "PATH: ${template.executablePath}\nARGS: $argsString\nWORKDIR: ${template.workingDirectory}\nBACKGROUND: ${template.runInBackground}",
                    rollbackPreview = template.safetyNote,
                    template = template,
                ),
            )
        }
    }

    fun updateCustomTermuxInput(value: String) {
        _uiState.update { it.copy(customTermuxInput = value) }
    }

    fun updateCustomTermuxBackground(runInBg: Boolean) {
        _uiState.update { it.copy(customTermuxBackground = runInBg) }
    }

    fun promptCustomTermuxCommand() {
        val cmd = _uiState.value.customTermuxInput.trim()
        if (cmd.isEmpty()) {
            emitSnackbar("Enter a Termux command first")
            return
        }
        val template = termuxBridge.createCustomTemplate(cmd, _uiState.value.customTermuxBackground)
        promptTermuxTemplate(template)
    }

    fun dismissPendingConsent() {
        _uiState.update { it.copy(pendingConsentAction = null) }
    }

    fun confirmAndExecutePendingAction() {
        val action = _uiState.value.pendingConsentAction ?: return
        _uiState.update { it.copy(pendingConsentAction = null) }

        viewModelScope.launch {
            when (action) {
                is PendingConsentAction.ShizukuSystemTweak -> executeConfirmedShizukuAction(action)
                is PendingConsentAction.TermuxExecution -> executeConfirmedTermuxAction(action.template)
            }
        }
    }

    private suspend fun executeConfirmedShizukuAction(action: PendingConsentAction.ShizukuSystemTweak) {
        when (action.actionType) {
            ShizukuTweakType.APPLY_ANIMATION_SCALE_05X -> {
                val snap = _uiState.value.snapshot
                val (origW, origT, origA) = if (!snap.animScaleChanged) {
                    shizukuManager.getAnimationScales()
                } else {
                    Triple(snap.originalWindowScale, snap.originalTransitionScale, snap.originalAnimatorScale)
                }
                val ok = shizukuManager.setAnimationScales("0.5", "0.5", "0.5")
                if (ok) {
                    profileRepo.saveSnapshot(
                        snap.copy(
                            isActive = true,
                            activatedAtEpochMs = if (snap.activatedAtEpochMs == 0L) System.currentTimeMillis() else snap.activatedAtEpochMs,
                            animScaleChanged = true,
                            originalWindowScale = origW ?: "1.0",
                            originalTransitionScale = origT ?: "1.0",
                            originalAnimatorScale = origA ?: "1.0",
                        ),
                    )
                    appendLog("Shizuku", "Set animation scales to 0.5x (baseline ${origW ?: "1.0"} saved)", isReversible = true)
                    emitSnackbar("Animation scales set to 0.5x (reversible)")
                } else {
                    emitSnackbar("Shizuku failed to update animation scales")
                }
            }

            ShizukuTweakType.REVERT_ANIMATION_SCALE -> {
                val snap = _uiState.value.snapshot
                val w = snap.originalWindowScale ?: "1.0"
                val t = snap.originalTransitionScale ?: "1.0"
                val a = snap.originalAnimatorScale ?: "1.0"
                val ok = shizukuManager.setAnimationScales(w, t, a)
                if (ok) {
                    profileRepo.saveSnapshot(snap.copy(animScaleChanged = false))
                    appendLog("Shizuku", "Restored animation scales to $w / $t / $a")
                    emitSnackbar("Animation scales restored")
                } else {
                    emitSnackbar("Failed to restore animation scales")
                }
            }

            ShizukuTweakType.SET_GAME_MODE_PERFORMANCE -> {
                val pkg = action.targetPackage ?: return
                val result = shizukuManager.setGameMode(pkg, "performance")
                val snap = _uiState.value.snapshot
                profileRepo.saveSnapshot(
                    snap.copy(
                        isActive = true,
                        activatedAtEpochMs = if (snap.activatedAtEpochMs == 0L) System.currentTimeMillis() else snap.activatedAtEpochMs,
                        gameModeChanged = true,
                        gameModePackage = pkg,
                    ),
                )
                appendLog("Shizuku", "Game Mode ($pkg): $result", isReversible = true)
                emitSnackbar(result)
            }

            ShizukuTweakType.REVERT_GAME_MODE_STANDARD -> {
                val pkg = action.targetPackage ?: return
                val result = shizukuManager.setGameMode(pkg, "standard")
                val snap = _uiState.value.snapshot
                profileRepo.saveSnapshot(snap.copy(gameModeChanged = false, gameModePackage = null))
                appendLog("Shizuku", "Reverted Game Mode ($pkg): $result")
                emitSnackbar(result)
            }

            ShizukuTweakType.TRIM_SELECTED_APPS -> {
                val selected = _uiState.value.backgroundApps.filter { !it.isProtected && it.isSelected }
                var count = 0
                for (item in selected) {
                    shizukuManager.trimBackgroundApp(item.packageName)
                    count++
                }
                appendLog("Shizuku", "Trimmed $count background packages via Shizuku UserService")
                emitSnackbar("Shizuku trimmed $count selected apps")
            }
        }
    }

    private fun executeConfirmedTermuxAction(template: TermuxCommandTemplate) {
        val result = termuxBridge.dispatchConfirmedCommand(template)
        result.onSuccess { msg ->
            _uiState.update {
                it.copy(termuxStatus = it.termuxStatus.copy(lastDispatchedCommand = template.title))
            }
            appendLog("Termux", msg)
            emitSnackbar(msg)
        }.onFailure { err ->
            val msg = err.message ?: "Termux launch failed"
            appendLog("Termux", msg)
            emitSnackbar(msg)
        }
    }

    fun launchGame(profile: GameProfile) {
        val pkg = profile.installedPackage
        if (pkg == null) {
            emitSnackbar("${profile.name} is not installed on this device")
            return
        }
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(launchIntent)
            appendLog("Launch", "Launched ${profile.name} ($pkg)")
        } else {
            emitSnackbar("Unable to resolve launcher activity for $pkg")
        }
    }

    fun openDndPolicySettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
            .onFailure { emitSnackbar("Unable to open DND settings on this device") }
    }

    fun openWriteSettingsScreen() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${appContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
            .onFailure { emitSnackbar("Unable to open Modify System Settings") }
    }

    fun toggleFpsOverlay(enable: Boolean) {
        if (!enable) {
            viewModelScope.launch {
                profileRepo.setFpsOverlayEnabled(false)
                appendLog("Overlay", "Low-profile FPS overlay disabled")
                emitSnackbar("FPS overlay turned off")
            }
            return
        }

        val hasAccess = FpsOverlayAccessibilityService.isAccessibilityServiceEnabled(appContext)
        if (!hasAccess) {
            _uiState.update { it.copy(showAccessibilityPermissionDialog = true) }
            return
        }

        viewModelScope.launch {
            profileRepo.setFpsOverlayEnabled(true)
            appendLog("Overlay", "Persistent FPS overlay enabled (real VSync frame cadence + temp)")
            emitSnackbar("Persistent FPS overlay active")
        }
    }

    fun dismissAccessibilityPermissionDialog() {
        _uiState.update { it.copy(showAccessibilityPermissionDialog = false) }
    }

    fun confirmOpenAccessibilitySettings() {
        _uiState.update { it.copy(showAccessibilityPermissionDialog = false) }
        viewModelScope.launch {
            profileRepo.setFpsOverlayEnabled(true)
        }
        openAccessibilitySettings()
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            appContext.startActivity(intent)
            appendLog("Overlay", "Opened Accessibility Settings for FPS overlay permission")
        }.onFailure {
            emitSnackbar("Unable to open Accessibility Settings on this device")
        }
    }

    fun selectOverlayBoostMode(mode: OverlayBoostMode) {
        viewModelScope.launch {
            profileRepo.setOverlayBoostMode(mode)
            val state = _uiState.value
            if (state.snapshot.isActive) {
                val selectedPkgs = state.backgroundApps
                    .filter { !it.isProtected && it.isSelected }
                    .map { it.packageName }
                val outcome = withContext(Dispatchers.IO) {
                    tweaksController.applyBoostProfile(
                        profile = state.selectedProfile,
                        boostMode = mode,
                        selectedPackagesToTrim = selectedPkgs,
                        previousSnapshot = state.snapshot,
                    )
                }
                profileRepo.saveSnapshot(outcome.snapshot)
            }
            appendLog("Mode", "Overlay & boost mode switched to ${mode.displayName}", isReversible = true)
            emitSnackbar("Boost mode: ${mode.displayName}")
        }
    }

    fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${appContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
            .onFailure { emitSnackbar("Unable to open App Info settings") }
    }

    fun openPackageOrStore(packageName: String) {
        val launch = appContext.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(launch)
            return
        }
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(marketIntent) }
            .onFailure { emitSnackbar("Could not open $packageName") }
    }

    private fun appendLog(category: String, message: String, isReversible: Boolean = false) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = BoostLogEntry(
            id = logCounter++,
            timeLabel = time,
            category = category,
            message = message,
            isReversible = isReversible,
        )
        _uiState.update { state ->
            state.copy(logs = (listOf(entry) + state.logs).take(30))
        }
    }

    private fun emitSnackbar(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    private data class Hex<A, B, C, D, E, F>(
        val a: A,
        val b: B,
        val c: C,
        val d: D,
        val e: E,
        val f: F,
    )

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer { BoosterViewModel(application) }
        }
    }
}
