package com.selendang3.app.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// One slate/teal visual language across navigation, dashboards and charts.
val BookSlate = Color(0xFF2D2D2D)
val BookTeal = Color(0xFF32CD32)
val BookInk = Color(0xFF101A10)
enum class BookAccent(val start: Color = Color(0xFF006400), val end: Color = Color(0xFF006400),
    val light: Color = BookTeal, val tint: Color = Color(0xFF393939)) {
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

// The requested charcoal base and readable surfaces are identical in both modes.
@Suppress("UNUSED_PARAMETER")
fun estateColorScheme(dark: Boolean, accent: BookAccent = BookAccent.Dashboard) = darkColorScheme(
    primary = Color(0xFF7FFF00), onPrimary = BookInk,
    primaryContainer = Color(0xFF006400), onPrimaryContainer = Color.White,
    secondary = Color(0xFF32CD32), onSecondary = BookInk,
    secondaryContainer = Color(0xFF393939), onSecondaryContainer = Color.White,
    tertiary = Color(0xFFB8EFA0), onTertiary = BookInk,
    background = BookSlate, onBackground = Color.White,
    surface = BookSlate, onSurface = Color.White,
    surfaceVariant = Color(0xFF393939), onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0xFFB1B1B1), outlineVariant = Color(0xFF808080),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)
