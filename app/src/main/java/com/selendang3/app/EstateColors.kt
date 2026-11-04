package com.selendang3.app

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// One slate/teal visual language across navigation, dashboards and charts.
val BookSlate = Color(0xFF4A6274)
val BookTeal = Color(0xFF79AEB2)
val BookInk = Color(0xFF142D36)
enum class BookAccent(val start: Color = BookSlate, val end: Color = BookSlate,
    val light: Color = BookTeal, val tint: Color = Color(0xFFDCEBED)) {
    Dashboard, Crop, Harvesting, Productivity, Upkeep, Rainfall
}

fun bookAccent(key: String) = when (key) {
    "reports", "abw", "Crop Production" -> BookAccent.Crop
    "production", "Harvesting Coverage" -> BookAccent.Harvesting
    "Harvester Productivity", "Estate Cost", "tools" -> BookAccent.Productivity
    "upkeep", "Manuring", "Weeding", "Pest & Disease" -> BookAccent.Upkeep
    "rainfall" -> BookAccent.Rainfall
    else -> BookAccent.Dashboard
}

@Suppress("UNUSED_PARAMETER")
fun estateColorScheme(dark: Boolean, accent: BookAccent = BookAccent.Dashboard) = if (dark) darkColorScheme(
    primary = BookTeal, onPrimary = BookInk,
    primaryContainer = Color(0xFF304B50), onPrimaryContainer = Color(0xFFE3F2F3),
    secondary = Color(0xFFB2CDD0), onSecondary = BookInk,
    secondaryContainer = Color(0xFF30444F), onSecondaryContainer = Color(0xFFE6F0F3),
    tertiary = Color(0xFFBDD9CF), onTertiary = BookInk,
    background = BookSlate, onBackground = Color.White,
    surface = Color(0xFF1D2C35), onSurface = Color(0xFFF0F5F6),
    surfaceVariant = Color(0xFF30444F), onSurfaceVariant = Color(0xFFCDDCE0),
    outline = Color(0xFF95AEB7), outlineVariant = Color(0xFF657F8B),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
) else lightColorScheme(
    primary = Color(0xFF285C61), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBED), onPrimaryContainer = BookInk,
    secondary = Color(0xFF405C6B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE7ED), onSecondaryContainer = BookInk,
    tertiary = Color(0xFF3F6658), onTertiary = Color.White,
    background = BookSlate, onBackground = Color.White,
    surface = Color(0xFFF5F8F9), onSurface = BookInk,
    surfaceVariant = Color(0xFFE0E9EC), onSurfaceVariant = Color(0xFF3E5662),
    outline = Color(0xFF617B86), outlineVariant = Color(0xFFA1B5BE),
    error = Color(0xFF9C2924), onError = Color.White,
)
