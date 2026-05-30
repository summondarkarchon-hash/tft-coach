// ui/theme/Theme.kt
package com.tftcoach.advisor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TftDarkColorScheme = darkColorScheme(
    primary         = TftGold,
    onPrimary       = TftDarkBg,
    primaryContainer = TftAccentDim,
    secondary       = TftAccent,
    onSecondary     = TextPrimary,
    background      = TftDarkBg,
    surface         = TftPanelBg,
    onSurface       = TextPrimary,
    onBackground    = TextPrimary,
    outline         = OverlayBorder,
    error           = PriorityHigh
)

@Composable
fun TFTCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TftDarkColorScheme,
        typography  = TftTypography,
        content     = content
    )
}
