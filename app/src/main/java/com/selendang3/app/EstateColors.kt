package com.selendang3.app

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Shared forest palette: explicit content pairs preserve contrast in both appearances.
fun estateColorScheme(dark: Boolean) = if (dark) darkColorScheme(
    primary = Color(0xFF9EDDB9), onPrimary = Color(0xFF103823),
    primaryContainer = Color(0xFF243F31), onPrimaryContainer = Color(0xFFD9F2DF),
    secondary = Color(0xFFB2CDBB), onSecondary = Color(0xFF213A2C),
    secondaryContainer = Color(0xFF243F31), onSecondaryContainer = Color(0xFFD9F2DF),
    tertiary = Color(0xFFE7C88D), onTertiary = Color(0xFF3D2F0C),
    background = Color(0xFF101C17), onBackground = Color(0xFFE4EDE6),
    surface = Color(0xFF1A2B23), onSurface = Color(0xFFE4EDE6),
    surfaceVariant = Color(0xFF293B31), onSurfaceVariant = Color(0xFFBACBBF),
    outline = Color(0xFF809888), outlineVariant = Color(0xFF365243),
) else lightColorScheme(
    primary = Color(0xFF20543E), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9ECDD), onPrimaryContainer = Color(0xFF163D2A),
    secondary = Color(0xFF476451), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5EFE7), onSecondaryContainer = Color(0xFF203D2C),
    tertiary = Color(0xFF725B27), onTertiary = Color.White,
    background = Color(0xFFF3F6F1), onBackground = Color(0xFF192D22),
    surface = Color.White, onSurface = Color(0xFF192D22),
    surfaceVariant = Color(0xFFE5EFE7), onSurfaceVariant = Color(0xFF4D6455),
    outline = Color(0xFF728979), outlineVariant = Color(0xFFD5E2D8),
)
