package com.example.booster.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import kotlin.math.roundToInt

@Composable
fun AppsScreen(
    state: BoosterUiState,
    onSearchQueryChange: (String) -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAllTrimmable: (Boolean) -> Unit,
    onRefreshApps: () -> Unit,
    onTrimSelectedStandard: () -> Unit,
    onTrimSelectedShizuku: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = state.backgroundApps.count { !it.isProtected && it.isSelected }
    val trimmableCount = state.backgroundApps.count { !it.isProtected }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CyberPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SAFE BACKGROUND APP MANAGER",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPure,
                        )
                        Text(
                            text = "RAM: ${state.telemetry.availableRamMb} MB free of ${state.telemetry.totalRamMb} MB (${state.telemetry.ramUsagePercent.roundToInt()}% used)",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan,
                        )
                    }
                    IconButton(
                        onClick = onRefreshApps,
                        modifier = Modifier.testTag("refresh_apps_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh installed apps",
                            tint = ElectricBlue,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Core system services, Home Launcher, active game profiles, Shizuku, and Termux are automatically protected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onTrimSelectedStandard,
                        enabled = selectedCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = ObsidianBlack,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("trim_standard_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Trim ($selectedCount)",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    if (state.shizukuStatus.isServiceBound) {
                        OutlinedButton(
                            onClick = onTrimSelectedShizuku,
                            enabled = selectedCount > 0,
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("trim_shizuku_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shizuku", color = NeonCyan)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.appSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search apps or packages...", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = ElectricBlue,
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = CarbonSurface,
                        unfocusedContainerColor = CarbonSurface,
                        focusedTextColor = TextPure,
                        unfocusedTextColor = TextPure,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apps_search_field"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$selectedCount / $trimmableCount user apps selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onSelectAllTrimmable(true) },
                            border = BorderStroke(1.dp, GlassBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("Select All", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onSelectAllTrimmable(false) },
                            border = BorderStroke(1.dp, GlassBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("Clear", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (state.filteredApps.isEmpty()) {
            item {
                CyberPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No matching launchable apps found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )
                }
            }
        } else {
            items(state.filteredApps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (app.isProtected) CarbonSurface.copy(alpha = 0.7f) else ElevatedSlate)
                        .border(
                            width = 1.dp,
                            color = if (app.isSelected) ElectricBlue.copy(alpha = 0.6f) else GlassBorder,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .clickable(enabled = !app.isProtected) { onToggleApp(app.packageName) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("app_item_${app.packageName}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (app.isProtected) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Protected application",
                            tint = OptimalGreen,
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .size(20.dp),
                        )
                    } else {
                        Checkbox(
                            checked = app.isSelected,
                            onCheckedChange = { onToggleApp(app.packageName) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ElectricBlue,
                                uncheckedColor = TextMuted,
                                checkmarkColor = ObsidianBlack,
                            ),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPure,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (app.protectionReason != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadgeChip(
                            text = app.protectionReason,
                            accentColor = OptimalGreen,
                        )
                    }
                }
            }
        }
    }
}
