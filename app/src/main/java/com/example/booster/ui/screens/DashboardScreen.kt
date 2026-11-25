package com.example.booster.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.booster.model.GameProfile
import com.example.booster.model.GameProfileId
import com.example.booster.model.OverlayBoostMode
import com.example.booster.model.ThermalStateLevel
import com.example.booster.ui.BoosterUiState
import com.example.booster.ui.components.CyberPanel
import com.example.booster.ui.components.StatusBadgeChip
import com.example.booster.ui.components.TelemetryArcGauge
import com.example.booster.ui.components.TelemetrySparkline
import com.example.booster.ui.components.thermalColorForTemp
import com.example.booster.ui.components.thermalSummaryColor
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OptimalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPure
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmAmber
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    state: BoosterUiState,
    onSelectProfile: (GameProfileId) -> Unit,
    onBoostGame: () -> Unit,
    onRevertSettings: () -> Unit,
    onLaunchSelectedGame: (GameProfile) -> Unit,
    onToggleFpsOverlay: (Boolean) -> Unit,
    onSelectBoostMode: (OverlayBoostMode) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val telemetry = state.telemetry
    val selectedProfile = state.selectedProfile
    val snapshot = state.snapshot

    val ramColor = when {
        telemetry.ramUsagePercent >= 88f -> WarmAmber
        else -> ElectricBlue
    }
    val cpuLoadVal = telemetry.cpuLoadPercent
    val cpuProgress = (cpuLoadVal ?: 0f) / 100f
    val tempColor = thermalColorForTemp(telemetry.batteryTempC)
    val tempProgress = telemetry.batteryTempC?.let { ((it - 20f) / 35f).coerceIn(0.05f, 1f) } ?: 0.25f

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero Core Banner + BOOST GAME primary control
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .border(
                        width = if (snapshot.isActive) 1.5.dp else 1.dp,
                        color = if (snapshot.isActive) ElectricBlue else GlassBorder,
                        shape = MaterialTheme.shapes.extraLarge,
                    ),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_booster_hero),
                    contentDescription = stringResource(R.string.cd_booster_hero),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    ObsidianBlack.copy(alpha = 0.94f),
                                    ObsidianBlack.copy(alpha = 0.78f),
                                    ObsidianBlack.copy(alpha = 0.58f),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COMMAND CENTER",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan,
                            )
                            Text(
                                text = selectedProfile.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPure,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (selectedProfile.isInstalled) {
                                    "Detected: ${selectedProfile.installedPackage}"
                                } else {
                                    "Profile Ready • Pre-configure or preview"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        StatusBadgeChip(
                            text = if (snapshot.isActive) "BOOSTED" else telemetry.thermalLevel.label,
                            accentColor = if (snapshot.isActive) OptimalGreen else thermalSummaryColor(telemetry.thermalLevel),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onBoostGame,
                            enabled = !state.isBoosting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = ObsidianBlack,
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("boost_game_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isBoosting) "OPTIMIZING..." else stringResource(R.string.action_boost_game),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        if (selectedProfile.isInstalled) {
                            OutlinedButton(
                                onClick = { onLaunchSelectedGame(selectedProfile) },
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("launch_selected_game_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_launch_game),
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = snapshot.isActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(CobaltContainer.copy(alpha = 0.85f))
                                .border(1.dp, ElectricBlue.copy(alpha = 0.45f), MaterialTheme.shapes.small)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${snapshot.activeProfileName ?: "Game"} Snapshot Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPure,
                                )
                                Text(
                                    text = "${snapshot.activeChangesCount} reversible tweaks • ${snapshot.trimmedPackagesCount} apps trimmed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            OutlinedButton(
                                onClick = onRevertSettings,
                                border = BorderStroke(1.dp, ElectricBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("revert_settings_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Revert",
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Profile Switcher Strip (Free Fire, PUBG Mobile, Minecraft)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.profiles, key = { it.id.storageKey }) { profile ->
                    val isSelected = profile.id == state.selectedProfileId
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (isSelected) CobaltContainer else CarbonSurface)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ElectricBlue else GlassBorder,
                                shape = MaterialTheme.shapes.medium,
                            )
                            .clickable { onSelectProfile(profile.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("quick_profile_${profile.id.storageKey}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = if (isSelected) NeonCyan else TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPure,
                            )
                            Text(
                                text = if (profile.isInstalled) "Installed" else "Profile Ready",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (profile.isInstalled) OptimalGreen else TextMuted,
                            )
                        }
                    }
                }
            }
        }

        // Persistent Low-Profile FPS Overlay Toggle (Accessibility Service)
        item {
            val hasAccessibility = state.permissions.hasAccessibilityOverlayPermission
            val overlayVisible = state.overlayRuntimeStatus.isOverlayVisible
            val overlayChecked = state.fpsOverlayEnabled && hasAccessibility

            CyberPanel(
                highlighted = overlayVisible,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fps_overlay_card"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (overlayVisible) CobaltContainer else ElevatedSlate,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = null,
                                tint = if (overlayVisible) NeonCyan else ElectricBlue,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "LIVE FPS OVERLAY",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPure,
                                )
                                StatusBadgeChip(
                                    text = when {
                                        overlayVisible -> "ACTIVE"
                                        hasAccessibility -> "READY"
                                        else -> "ACCESSIBILITY OFF"
                                    },
                                    accentColor = when {
                                        overlayVisible -> OptimalGreen
                                        hasAccessibility -> NeonCyan
                                        else -> WarmAmber
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when {
                                    overlayVisible && state.overlayRuntimeStatus.measuredFps > 0 ->
                                        String.format(
                                            Locale.US,
                                            "Floating pill active: %d FPS • %.1f ms (Choreographer VSync)",
                                            state.overlayRuntimeStatus.measuredFps,
                                            state.overlayRuntimeStatus.frameTimeMs,
                                        )
                                    hasAccessibility ->
                                        "Low-profile draggable VSync frame & thermal pill over active games"
                                    else ->
                                        "Requires Accessibility Service permission to draw over active games"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = overlayChecked,
                        onCheckedChange = onToggleFpsOverlay,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBlack,
                            checkedTrackColor = ElectricBlue,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = ElevatedSlate,
                        ),
                        modifier = Modifier.testTag("fps_overlay_toggle"),
                    )
                }

                if (!hasAccessibility) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(ElevatedSlate)
                            .clickable(onClick = onOpenAccessibilitySettings)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("grant_accessibility_banner"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Enable IVC SIYAM in Accessibility Settings",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "GRANT ->",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricBlue,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "OVERLAY & BOOST MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OverlayBoostMode.entries.forEach { mode ->
                        val isSelected = state.overlayBoostMode == mode
                        val accent = when (mode) {
                            OverlayBoostMode.POWER_SAVE -> OptimalGreen
                            OverlayBoostMode.BALANCED -> ElectricBlue
                            OverlayBoostMode.ADVANCE -> NeonCyan
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (isSelected) accent.copy(alpha = 0.18f) else ElevatedSlate,
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accent else GlassBorder,
                                    shape = MaterialTheme.shapes.small,
                                )
                                .clickable { onSelectBoostMode(mode) }
                                .padding(vertical = 12.dp, horizontal = 6.dp)
                                .testTag("boost_mode_${mode.storageKey}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) accent else TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.overlayBoostMode.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        // Primary 3 Hardware Gauges: RAM, CPU, Thermal
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TelemetryArcGauge(
                    title = "MEMORY",
                    valueText = "${telemetry.ramUsagePercent.roundToInt()}%",
                    subtitle = "${telemetry.availableRamMb} MB Free",
                    progress = telemetry.ramUsagePercent / 100f,
                    accentColor = ramColor,
                    modifier = Modifier.weight(1f),
                )
                TelemetryArcGauge(
                    title = "CPU LOAD",
                    valueText = cpuLoadVal?.let { "${it.roundToInt()}%" } ?: "${telemetry.cpuCores}C",
                    subtitle = telemetry.cpuLoadSource,
                    progress = if (cpuLoadVal != null) cpuProgress else 0.35f,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                )
                TelemetryArcGauge(
                    title = "THERMAL",
                    valueText = telemetry.batteryTempC?.let {
                        String.format(Locale.US, "%.1f°C", it)
                    } ?: "--°C",
                    subtitle = telemetry.systemThermalStatus,
                    progress = tempProgress,
                    accentColor = tempColor,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Live Telemetry Graphs + Honest Display & Frame Timing
        item {
            CyberPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE TELEMETRY STREAM",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPure,
                        )
                    }
                    Text(
                        text = "${telemetry.usedRamMb} / ${telemetry.totalRamMb} MB",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricBlue,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "RAM Utilization Trend",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                TelemetrySparkline(
                    values = telemetry.ramHistory,
                    lineColor = ElectricBlue,
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "CPU Activity Trend (${telemetry.cpuCores} Cores • ${telemetry.socLabel})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                TelemetrySparkline(
                    values = telemetry.cpuHistory,
                    lineColor = NeonCyan,
                )

                if (telemetry.cpuClocks.any { it.currentMhz != null }) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CORE CLOCKS (REAL SYSFS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(telemetry.cpuClocks) { core ->
                            Column(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(ElevatedSlate)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "C${core.coreIndex}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                )
                                Text(
                                    text = core.currentMhz?.let { "$it MHz" } ?: "Locked",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Honest Display Refresh Rate & Dashboard Frame Timing + Safety Card
        item {
            CyberPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DISPLAY & SYSTEM TELEMETRY",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPure,
                        )
                    }
                    StatusBadgeChip(
                        text = "${telemetry.refreshRateHz.roundToInt()} Hz PANEL",
                        accentColor = NeonCyan,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricMiniBox(
                        label = "UI Frame Time",
                        value = String.format(Locale.US, "%.1f ms", telemetry.uiFrameTimeMs),
                        hint = "Booster UI only (no fake FPS)",
                        modifier = Modifier.weight(1f),
                    )
                    MetricMiniBox(
                        label = "Storage Free",
                        value = String.format(Locale.US, "%.1f GB", telemetry.storageFreeGb),
                        hint = String.format(Locale.US, "of %.0f GB /data", telemetry.storageTotalGb),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricMiniBox(
                        label = "Battery & Power",
                        value = "${telemetry.batteryLevelPct}%${if (telemetry.isCharging) " ⚡" else ""}",
                        hint = if (telemetry.isPowerSaveMode) "Battery Saver ON" else "${telemetry.batteryVoltageMv} mV",
                        modifier = Modifier.weight(1f),
                    )
                    MetricMiniBox(
                        label = "Network",
                        value = telemetry.networkLabel,
                        hint = telemetry.abiLabel,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(ElevatedSlate)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = OptimalGreen,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Integrity Guarantee: Thermal throttling protections remain active. Third-party game FPS is never fabricated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        // Thermal Warning Notice if device is warm/hot
        if (telemetry.thermalLevel == ThermalStateLevel.HOT) {
            item {
                CyberPanel(
                    highlighted = true,
                    containerColor = Color(0xFF220A10),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Thermostat,
                            contentDescription = null,
                            tint = Color(0xFFFF3B5C),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thermal Guardrail Alert: Battery/SoC temperature is elevated. Lower screen brightness or pause charging while gaming.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPure,
                        )
                    }
                }
            }
        }

        // Audit Log
        item {
            CyberPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "RECENT OPTIMIZATION LOG",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPure,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.logs.isEmpty()) {
                    Text(
                        text = "No optimization events recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.logs.take(6).forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianBlack.copy(alpha = 0.65f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = entry.timeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadgeChip(
                                    text = entry.category,
                                    accentColor = if (entry.isReversible) ElectricBlue else OptimalGreen,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = entry.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(ElevatedSlate)
            .padding(10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPure,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
