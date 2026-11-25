package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BoosterDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = ObsidianBlack,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = NeonCyan,
    secondary = NeonCyan,
    onSecondary = ObsidianBlack,
    secondaryContainer = ElevatedSlate,
    onSecondaryContainer = TextPure,
    tertiary = OptimalGreen,
    onTertiary = ObsidianBlack,
    tertiaryContainer = OptimalGreenContainer,
    onTertiaryContainer = OptimalGreen,
    error = CriticalRed,
    onError = TextPure,
    errorContainer = CriticalRedContainer,
    onErrorContainer = CriticalRed,
    background = ObsidianBlack,
    onBackground = TextPure,
    surface = CarbonSurface,
    onSurface = TextPure,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorderBright,
    outlineVariant = GlassBorder,
)

private val BoosterShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun IvcGameBoosterTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BoosterDarkColorScheme,
        typography = BoosterTypography,
        shapes = BoosterShapes,
        content = content,
    )
}
