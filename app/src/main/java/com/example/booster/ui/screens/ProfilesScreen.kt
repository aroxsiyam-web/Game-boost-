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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.booster.model.GameProfile
import com.example.booster.model.GameProfileId
import com.example.booster.ui.BoosterUiState
import com.example.booster.ui.components.StatusBadgeChip
import com.example.ui.theme.CarbonSurface
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
import kotlin.math.roundToInt

@Composable
fun ProfilesScreen(
    state: BoosterUiState,
    onSelectProfile: (GameProfileId) -> Unit,
    onUpdateProfile: (GameProfile) -> Unit,
    onBoostProfile: (GameProfileId) -> Unit,
    onLaunchGame: (GameProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedProfileId by remember { mutableStateOf<GameProfileId?>(state.selectedProfileId) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "GAME PROFILES",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPure,
                )
                Text(
                    text = "Configure reversible optimization presets for Free Fire, PUBG Mobile, and Minecraft. Thermal throttling safeguards always stay active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        items(state.profiles, key = { it.id.storageKey }) { profile ->
            val isSelected = profile.id == state.selectedProfileId
            val isExpanded = expandedProfileId == profile.id

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(CarbonSurface)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) ElectricBlue else GlassBorder,
                        shape = MaterialTheme.shapes.large,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clickable {
                            onSelectProfile(profile.id)
                            expandedProfileId = if (isExpanded) null else profile.id
                        }
                        .testTag("profile_card_${profile.id.storageKey}"),
                ) {
                    Image(
                        painter = painterResource(id = profile.artRes),
                        contentDescription = stringResource(R.string.cd_profile_art),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianBlack.copy(alpha = 0.25f),
                                        ObsidianBlack.copy(alpha = 0.92f),
                                    ),
                                ),
                            ),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        StatusBadgeChip(
                            text = if (profile.isInstalled) {
                                "INSTALLED ${profile.installedVersion ?: ""}".trim()
                            } else {
                                "NOT INSTALLED"
                            },
                            accentColor = if (profile.isInstalled) OptimalGreen else TextMuted,
                        )
                        if (isSelected) {
                            StatusBadgeChip(
                                text = "ACTIVE PROFILE",
                                accentColor = NeonCyan,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPure,
                            )
                            Text(
                                text = profile.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "Expand profile settings",
                            tint = NeonCyan,
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = profile.optimizationSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { onBoostProfile(profile.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = ObsidianBlack,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("activate_profile_${profile.id.storageKey}"),
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Rounded.Bolt else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Boost ${profile.name}",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        }

                        if (profile.isInstalled) {
                            OutlinedButton(
                                onClick = { onLaunchGame(profile) },
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("launch_profile_${profile.id.storageKey}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Launch", color = NeonCyan)
                            }
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HorizontalDivider(color = GlassBorder)

                            ProfileToggleRow(
                                title = "Focus Shield (Do Not Disturb)",
                                subtitle = "Reversible Priority DND during session (requires DND policy access)",
                                checked = profile.dndFocusShield,
                                onCheckedChange = { onUpdateProfile(profile.copy(dndFocusShield = it)) },
                            )
                            ProfileToggleRow(
                                title = "Safe Background App Trim",
                                subtitle = "Trims selected non-protected background apps via ActivityManager",
                                checked = profile.trimBackgroundApps,
                                onCheckedChange = { onUpdateProfile(profile.copy(trimBackgroundApps = it)) },
                            )
                            ProfileToggleRow(
                                title = "Keep Booster Screen Awake",
                                subtitle = "Prevents screen timeout while monitoring live telemetry",
                                checked = profile.keepScreenOn,
                                onCheckedChange = { onUpdateProfile(profile.copy(keepScreenOn = it)) },
                            )
                            ProfileToggleRow(
                                title = "Reversible Brightness Lock",
                                subtitle = "Locks display brightness to ${profile.targetBrightnessPct}% (restored on Revert)",
                                checked = profile.lockBrightness,
                                onCheckedChange = { onUpdateProfile(profile.copy(lockBrightness = it)) },
                            )

                            if (profile.lockBrightness) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(ElevatedSlate)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "Target Brightness",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                        )
                                        Text(
                                            text = "${profile.targetBrightnessPct}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = NeonCyan,
                                        )
                                    }
                                    Slider(
                                        value = profile.targetBrightnessPct.toFloat(),
                                        onValueChange = {
                                            onUpdateProfile(profile.copy(targetBrightnessPct = it.roundToInt()))
                                        },
                                        valueRange = 25f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = NeonCyan,
                                            activeTrackColor = ElectricBlue,
                                        ),
                                    )
                                }
                            }

                            ProfileToggleRow(
                                title = "Shizuku: Android Game Mode (Performance)",
                                subtitle = "Prompts confirmation to apply Android 12+ Game Mode via Shizuku",
                                checked = profile.shizukuPerformanceGameMode,
                                onCheckedChange = { onUpdateProfile(profile.copy(shizukuPerformanceGameMode = it)) },
                            )
                            ProfileToggleRow(
                                title = "Shizuku: 0.5x UI Animation Scale",
                                subtitle = "Prompts confirmation to reduce system transition scales (reversible)",
                                checked = profile.shizukuReducedAnimScale,
                                onCheckedChange = { onUpdateProfile(profile.copy(shizukuReducedAnimScale = it)) },
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(ElevatedSlate)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Thermal Alert Guardrail",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                    )
                                    Text(
                                        text = "${profile.thermalGuardLimitC}°C",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WarmAmber,
                                    )
                                }
                                Slider(
                                    value = profile.thermalGuardLimitC.toFloat(),
                                    onValueChange = {
                                        onUpdateProfile(profile.copy(thermalGuardLimitC = it.roundToInt()))
                                    },
                                    valueRange = 38f..45f,
                                    steps = 6,
                                    colors = SliderDefaults.colors(
                                        thumbColor = WarmAmber,
                                        activeTrackColor = WarmAmber,
                                    ),
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
private fun ProfileToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPure,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBlack,
                checkedTrackColor = ElectricBlue,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ElevatedSlate,
            ),
        )
    }
}
