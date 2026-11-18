package com.overcookedrecomp.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Shared visual tokens for the console dashboard. Keep UI values centralized here. */
object GhostTownColors {
    val Storm = Color(0xFF0B1118)
    val StormRaised = Color(0xFF141E29)
    val Surface = Color(0xFF182431)
    val SurfaceElevated = Color(0xFF202F3D)
    val Accent = Color(0xFF54D6C5)
    val AccentDeep = Color(0xFF1E8F85)
    val TextPrimary = Color(0xFFF3F7F8)
    val TextSecondary = Color(0xFFB4C2CA)
    val TextMuted = Color(0xFF82939E)
    val Warning = Color(0xFFFFC66D)
    val Danger = Color(0xFFFF8585)
    val FocusBorder = Color(0xFF7CE8D9)
}

object GhostTownSpace {
    const val Page = 28
    const val Section = 20
    const val CardGap = 14
    const val Small = 8
    const val TouchTarget = 48
    const val FocusDuration = 190
}

private val ConsoleScheme = darkColorScheme(
    primary = GhostTownColors.Accent,
    onPrimary = GhostTownColors.Storm,
    secondary = GhostTownColors.AccentDeep,
    background = GhostTownColors.Storm,
    onBackground = GhostTownColors.TextPrimary,
    surface = GhostTownColors.Surface,
    onSurface = GhostTownColors.TextPrimary,
    surfaceVariant = GhostTownColors.SurfaceElevated,
    onSurfaceVariant = GhostTownColors.TextSecondary,
    error = GhostTownColors.Danger
)

@Composable
fun GhostTownTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ConsoleScheme, content = content)
}
