package com.selendang3.app

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalBookTabLevel = staticCompositionLocalOf { 1 }

internal fun bookTabBackground(level: Int): Color = when (level.coerceIn(0, 3)) {
    0 -> Color(0xFF006400)
    1 -> Color(0xFF228B22)
    2 -> Color(0xFF32CD32)
    else -> Color(0xFF7FFF00)
}

internal fun bookTabForeground(level: Int): Color =
    if (level <= 0) Color.White else Color.Black

@Composable
internal fun bookTabButtonColors(level: Int = LocalBookTabLevel.current) =
    ButtonDefaults.filledTonalButtonColors(
        containerColor = bookTabBackground(level),
        contentColor = bookTabForeground(level))
