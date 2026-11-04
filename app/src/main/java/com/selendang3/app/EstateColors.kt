package com.selendang3.app

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Dark, saturated endpoints keep white labels readable across the entire gradient.
enum class BookAccent(val start: Color, val end: Color, val light: Color, val tint: Color) {
    Dashboard(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFFC4B5FD), Color(0xFFEDE9FE)),
    Crop(Color(0xFF0E7490), Color(0xFF1D4ED8), Color(0xFF67E8F9), Color(0xFFCFFAFE)),
    Harvesting(Color(0xFF047857), Color(0xFF0F766E), Color(0xFF6EE7B7), Color(0xFFD1FAE5)),
    Productivity(Color(0xFF9A3412), Color(0xFFBE123C), Color(0xFFFDBA74), Color(0xFFFFEDD5)),
    Upkeep(Color(0xFF7E22CE), Color(0xFFBE185D), Color(0xFFF0ABFC), Color(0xFFFAE8FF)),
    Rainfall(Color(0xFF0369A1), Color(0xFF0E7490), Color(0xFF7DD3FC), Color(0xFFE0F2FE)),
}

fun bookAccent(key: String) = when (key) {
    "reports", "abw", "Crop Production" -> BookAccent.Crop
    "production", "Harvesting Coverage" -> BookAccent.Harvesting
    "Harvester Productivity", "Estate Cost", "tools" -> BookAccent.Productivity
    "upkeep", "Manuring", "Weeding", "Pest & Disease" -> BookAccent.Upkeep
    "rainfall" -> BookAccent.Rainfall
    else -> BookAccent.Dashboard
}

fun estateColorScheme(dark: Boolean, accent: BookAccent = BookAccent.Dashboard) = if (dark) darkColorScheme(
    primary = accent.light, onPrimary = Color(0xFF17132E),
    primaryContainer = accent.start, onPrimaryContainer = Color.White,
    secondary = accent.light, onSecondary = Color(0xFF17132E),
    secondaryContainer = Color(0xFF302B46), onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFFF9A8D4), onTertiary = Color(0xFF4A1032),
    background = Color(0xFF100E1D), onBackground = Color(0xFFF3F0FF),
    surface = Color(0xFF1C192D), onSurface = Color(0xFFF3F0FF),
    surfaceVariant = Color(0xFF302B46), onSurfaceVariant = Color(0xFFD0C9E2),
    outline = Color(0xFF9A91AE), outlineVariant = Color(0xFF49415E),
) else lightColorScheme(
    primary = accent.start, onPrimary = Color.White,
    primaryContainer = accent.tint, onPrimaryContainer = Color(0xFF211735),
    secondary = accent.end, onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE), onSecondaryContainer = Color(0xFF302050),
    tertiary = Color(0xFFBE185D), onTertiary = Color.White,
    background = Color(0xFFF7F5FF), onBackground = Color(0xFF211B35),
    surface = Color.White, onSurface = Color(0xFF211B35),
    surfaceVariant = Color(0xFFEDE9F5), onSurfaceVariant = Color(0xFF554B66),
    outline = Color(0xFF7C708F), outlineVariant = Color(0xFFD9D2E8),
)
