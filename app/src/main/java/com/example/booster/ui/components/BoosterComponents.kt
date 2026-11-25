package com.example.booster.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.booster.model.PendingConsentAction
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CobaltContainer
import com.example.ui.theme.CriticalRed
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
fun CyberPanel(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    containerColor: Color = CarbonSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (highlighted) ElectricBlue else GlassBorder,
        label = "panel_border",
    )
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, borderColor),
        tonalElevation = if (highlighted) 4.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun StatusBadgeChip(
    text: String,
    accentColor: Color,
    containerColor: Color = accentColor.copy(alpha = 0.14f),
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TelemetryArcGauge(
    title: String,
    valueText: String,
    subtitle: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 220f),
        label = "gauge_progress",
    )

    CyberPanel(
        modifier = modifier,
        containerColor = CarbonSurface,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(102.dp)) {
                val strokeWidth = 9.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                drawArc(
                    color = ElevatedSlate,
                    startAngle = 145f,
                    sweepAngle = 250f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to accentColor.copy(alpha = 0.7f),
                        0.6f to accentColor,
                        1.0f to NeonCyan,
                    ),
                    startAngle = 145f,
                    sweepAngle = 250f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPure,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun TelemetrySparkline(
    values: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBlack.copy(alpha = 0.65f))
            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp)),
    ) {
        if (values.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val pad = 4.dp.toPx()
        val usableH = (h - pad * 2).coerceAtLeast(1f)
        val stepX = w / (values.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()

        values.forEachIndexed { idx, raw ->
            val x = idx * stepX
            val normalized = (raw / 100f).coerceIn(0f, 1f)
            val y = pad + usableH * (1f - normalized)
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (idx == values.lastIndex) {
                fillPath.lineTo(x, h)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
            ),
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
fun ConsentConfirmationDialog(
    action: PendingConsentAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isTermux = action is PendingConsentAction.TermuxExecution
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonSurface,
        titleContentColor = TextPure,
        textContentColor = TextSecondary,
        shape = MaterialTheme.shapes.large,
        icon = {
            Icon(
                imageVector = if (isTermux) Icons.Rounded.Terminal else Icons.Rounded.Security,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(30.dp),
            )
        },
        title = {
            Text(
                text = action.title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPure,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = action.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(ObsidianBlack)
                        .border(1.dp, GlassBorder, MaterialTheme.shapes.small)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "COMMAND PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = action.commandPreview,
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(CobaltContainer.copy(alpha = 0.45f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Undo,
                        contentDescription = null,
                        tint = OptimalGreen,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = action.rollbackPreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPure,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = ObsidianBlack,
                ),
                modifier = Modifier.testTag("confirm_consent_button"),
            ) {
                Text(
                    text = "Confirm & Run",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.testTag("cancel_consent_button"),
            ) {
                Text(
                    text = "Cancel",
                    color = TextSecondary,
                )
            }
        },
    )
}

@Composable
fun AccessibilityPermissionDisclosureDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarbonSurface,
        titleContentColor = TextPure,
        textContentColor = TextSecondary,
        shape = MaterialTheme.shapes.large,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Layers,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(30.dp),
            )
        },
        title = {
            Text(
                text = "Accessibility Service Required",
                style = MaterialTheme.typography.titleLarge,
                color = TextPure,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "To render a persistent, low-profile real-time frame overlay over active games, enable IVC SIYAM Game Booster in Android Accessibility Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(ObsidianBlack)
                        .border(1.dp, GlassBorder, MaterialTheme.shapes.small)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "PRIVACY & OVERLAY SCOPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue,
                    )
                    Text(
                        text = "• Uses TYPE_ACCESSIBILITY_OVERLAY to display a draggable VSync FPS & thermal pill.\n" +
                            "• Detects foreground game package transitions only.\n" +
                            "• Window text, passwords, and touch inputs are never read or collected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPure,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = ObsidianBlack,
                ),
                modifier = Modifier.testTag("open_accessibility_settings_button"),
            ) {
                Text(
                    text = "Open Settings",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.testTag("cancel_accessibility_button"),
            ) {
                Text(
                    text = "Not Now",
                    color = TextSecondary,
                )
            }
        },
    )
}

fun thermalColorForTemp(tempC: Float?): Color {
    if (tempC == null) return ElectricBlue
    return when {
        tempC >= 43f -> CriticalRed
        tempC >= 37.5f -> WarmAmber
        else -> OptimalGreen
    }
}

fun thermalSummaryColor(level: com.example.booster.model.ThermalStateLevel): Color {
    return when (level) {
        com.example.booster.model.ThermalStateLevel.COOL -> OptimalGreen
        com.example.booster.model.ThermalStateLevel.WARM -> WarmAmber
        com.example.booster.model.ThermalStateLevel.HOT -> CriticalRed
        com.example.booster.model.ThermalStateLevel.UNKNOWN -> TextMuted
    }
}
