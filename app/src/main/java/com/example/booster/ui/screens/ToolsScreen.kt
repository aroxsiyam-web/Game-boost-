package com.example.booster.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.booster.model.TermuxCommandTemplate
import com.example.booster.ui.BoosterUiState
import com.example.booster.ui.components.CyberPanel
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

@Composable
fun ToolsScreen(
    state: BoosterUiState,
    onRefreshIntegrations: () -> Unit,
    onRequestShizukuAuth: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onPromptShizukuAnimScale: () -> Unit,
    onPromptShizukuRevertAnimScale: () -> Unit,
    onPromptShizukuGameMode: (String, String) -> Unit,
    onPromptTermuxTemplate: (TermuxCommandTemplate) -> Unit,
    onCustomTermuxInputChange: (String) -> Unit,
    onCustomTermuxBgChange: (Boolean) -> Unit,
    onPromptCustomTermux: () -> Unit,
    onOpenTermuxApp: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenWriteSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shizuku = state.shizukuStatus
    val termux = state.termuxStatus
    val perms = state.permissions

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SYSTEM INTEGRATIONS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPure,
                    )
                    Text(
                        text = "Authorized Shizuku API, consent-gated Termux scripts, and Android system permissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                IconButton(
                    onClick = onRefreshIntegrations,
                    modifier = Modifier.testTag("refresh_tools_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh integration status",
                        tint = ElectricBlue,
                    )
                }
            }
        }

        // Official Shizuku Integration Card
        item {
            CyberPanel(
                highlighted = shizuku.isServiceBound,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SHIZUKU API INTEGRATION",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPure,
                        )
                    }
                    StatusBadgeChip(
                        text = when {
                            shizuku.isServiceBound -> "AUTHORIZED"
                            shizuku.isBinderAlive -> "READY TO AUTH"
                            shizuku.isInstalled -> "STOPPED"
                            else -> "OPTIONAL"
                        },
                        accentColor = when {
                            shizuku.isServiceBound -> OptimalGreen
                            shizuku.isBinderAlive -> NeonCyan
                            shizuku.isInstalled -> WarmAmber
                            else -> TextMuted
                        },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = shizuku.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusInfoPill(
                        label = "Privilege",
                        value = shizuku.privilegeLabel,
                        modifier = Modifier.weight(1f),
                    )
                    StatusInfoPill(
                        label = "UserService",
                        value = if (shizuku.isServiceBound) "Bound (AIDL)" else "Idle",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onRequestShizukuAuth,
                        enabled = shizuku.isBinderAlive && !shizuku.isPermissionGranted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = ObsidianBlack,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("authorize_shizuku_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (shizuku.isPermissionGranted) "Authorized" else "Authorize Shizuku",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenShizukuApp,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (shizuku.isInstalled) "Open" else "Get App",
                            color = NeonCyan,
                        )
                    }
                }

                if (shizuku.isServiceBound) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = GlassBorder)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "REVERSIBLE SHIZUKU TWEAKS (CONFIRMATION REQUIRED)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onPromptShizukuAnimScale,
                            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_anim_05x_button"),
                        ) {
                            Text("0.5x Anim Scale", color = ElectricBlue, style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onPromptShizukuRevertAnimScale,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_anim_revert_button"),
                        ) {
                            Text("Restore 1.0x", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    val targetPkg = state.selectedProfile.installedPackage
                    if (targetPkg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onPromptShizukuGameMode(targetPkg, state.selectedProfile.name) },
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shizuku_game_mode_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Apply Game Mode (Performance) -> ${state.selectedProfile.name}",
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }

        // Optional Termux Integration Card (Always Consent-Gated)
        item {
            CyberPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TERMUX COMMAND RUNNER",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPure,
                        )
                    }
                    StatusBadgeChip(
                        text = when {
                            termux.isInstalled && termux.hasRunCommandPermission -> "READY"
                            termux.isInstalled -> "NEEDS PERMISSION"
                            else -> "NOT INSTALLED"
                        },
                        accentColor = when {
                            termux.isInstalled && termux.hasRunCommandPermission -> OptimalGreen
                            termux.isInstalled -> WarmAmber
                            else -> TextMuted
                        },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Optional Termux RUN_COMMAND integration. Every command requires explicit confirmation before dispatch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(ObsidianBlack)
                        .border(1.dp, GlassBorder, MaterialTheme.shapes.small)
                        .padding(10.dp),
                ) {
                    Text(
                        text = "TERMUX PREREQUISITE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "echo \"allow-external-apps=true\" >> ~/.termux/termux.properties",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                    )
                }

                if (!termux.isInstalled || !termux.hasRunCommandPermission) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenTermuxApp,
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = if (termux.isInstalled) "Open Termux" else "Get Termux",
                                color = NeonCyan,
                            )
                        }
                        if (termux.isInstalled && !termux.hasRunCommandPermission) {
                            OutlinedButton(
                                onClick = onOpenAppPermissions,
                                border = BorderStroke(1.dp, WarmAmber.copy(alpha = 0.7f)),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Grant Permission", color = WarmAmber)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SAFE DIAGNOSTIC SCRIPTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.termuxTemplates.forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(ElevatedSlate)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPure,
                                )
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            OutlinedButton(
                                onClick = { onPromptTermuxTemplate(template) },
                                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("termux_template_${template.id}"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Review", color = ElectricBlue, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.customTermuxInput,
                    onValueChange = onCustomTermuxInputChange,
                    label = { Text("Custom Termux Command", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = ObsidianBlack,
                        unfocusedContainerColor = ObsidianBlack,
                        focusedTextColor = NeonCyan,
                        unfocusedTextColor = NeonCyan,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_termux_input"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.customTermuxBackground,
                            onCheckedChange = onCustomTermuxBgChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = ElectricBlue,
                                uncheckedColor = TextMuted,
                                checkmarkColor = ObsidianBlack,
                            ),
                        )
                        Text(
                            text = "Run in background",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    Button(
                        onClick = onPromptCustomTermux,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = ObsidianBlack,
                        ),
                        modifier = Modifier.testTag("review_custom_termux_button"),
                    ) {
                        Text("Review & Run", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Android System Permissions Card
        item {
            CyberPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "REVERSIBLE TWEAK PERMISSIONS",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPure,
                )
                Spacer(modifier = Modifier.height(10.dp))

                PermissionRow(
                    title = "Do Not Disturb Policy Access",
                    subtitle = "Required for Focus Shield (auto-restored on Revert)",
                    granted = perms.hasDndPolicyAccess,
                    onGrantClick = onOpenDndSettings,
                    testTag = "grant_dnd_button",
                )
                Spacer(modifier = Modifier.height(8.dp))
                PermissionRow(
                    title = "Modify System Settings",
                    subtitle = "Optional for reversible screen brightness lock",
                    granted = perms.hasWriteSettingsAccess,
                    onGrantClick = onOpenWriteSettings,
                    testTag = "grant_write_settings_button",
                )
                Spacer(modifier = Modifier.height(8.dp))
                PermissionRow(
                    title = "Accessibility Service (FPS Overlay)",
                    subtitle = "Draws low-profile VSync frame & thermal pill over active games",
                    granted = perms.hasAccessibilityOverlayPermission,
                    onGrantClick = onOpenAccessibilitySettings,
                    testTag = "grant_accessibility_button",
                )
            }
        }

        // Transparency & Policy Guarantee Card
        item {
            CyberPanel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = OptimalGreen,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFETY & HONESTY POLICY",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPure,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Real Device Data Only: RAM, CPU clocks/load, battery thermal status, and display refresh rate come directly from Android APIs & kernel sysfs.\n" +
                        "• Never Fakes FPS: Only displays real panel Hz and booster UI frame timing.\n" +
                        "• Thermal Protection Intact: Never disables thermal throttling or cooling daemons.\n" +
                        "• Consent-Gated Commands: Shizuku and Termux actions always require explicit user confirmation and record rollback baselines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun StatusInfoPill(
    label: String,
    value: String,
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
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = TextPure,
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onGrantClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(ElevatedSlate)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPure,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Granted",
                    tint = OptimalGreen,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Granted", style = MaterialTheme.typography.labelMedium, color = OptimalGreen)
            }
        } else {
            OutlinedButton(
                onClick = onGrantClick,
                border = BorderStroke(1.dp, ElectricBlue),
                modifier = Modifier.testTag(testTag),
            ) {
                Text("Configure", color = ElectricBlue, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
