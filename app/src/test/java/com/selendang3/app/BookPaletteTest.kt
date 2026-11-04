package com.selendang3.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class BookPaletteTest {
    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance()
        val y = b.luminance()
        return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
    }

    @Test fun gradientLabelsMeetNormalTextContrast() {
        BookAccent.entries.forEach { accent ->
            // Every interpolated sRGB channel lies between the endpoint channels.
            for (step in 0..100) {
                val t = step / 100f
                val color = Color(
                    accent.start.red * (1 - t) + accent.end.red * t,
                    accent.start.green * (1 - t) + accent.end.green * t,
                    accent.start.blue * (1 - t) + accent.end.blue * t)
                assertTrue("${accent.name} gradient step $step", contrast(Color.White, color) >= 4.5f)
            }
        }
    }

    @Test fun semanticTextPairsMeetNormalTextContrast() {
        BookAccent.entries.forEach { accent ->
            listOf(false, true).forEach { dark ->
                val colors = estateColorScheme(dark, accent)
                listOf(colors.primary to colors.onPrimary,
                    colors.primaryContainer to colors.onPrimaryContainer,
                    colors.secondary to colors.onSecondary,
                    colors.secondaryContainer to colors.onSecondaryContainer,
                    colors.background to colors.onBackground,
                    colors.surface to colors.onSurface,
                    colors.surfaceVariant to colors.onSurfaceVariant).forEach { (background, text) ->
                    assertTrue("${accent.name}, dark=$dark", contrast(background, text) >= 4.5f)
                }
            }
        }
    }

    @Test fun pageTableAndNavigationTextRemainReadable() {
        listOf(false, true).forEach { dark ->
            val colors = estateColorScheme(dark)
            listOf(
                BookSlate to Color.White,
                BookSlate to Color.White.copy(alpha = 0.8f).compositeOver(BookSlate),
                BookTeal to BookInk,
                colors.surface to colors.primary,
                colors.surface to colors.secondary,
                colors.surface to colors.tertiary,
                colors.secondaryContainer to colors.onSurface,
                colors.secondaryContainer to colors.primary,
                colors.secondaryContainer to colors.onSecondaryContainer,
            ).forEach { (background, text) ->
                assertTrue("dark=$dark, background=$background, text=$text",
                    contrast(background, text) >= 4.5f)
            }
        }
    }

    @Test fun exactBackgroundAndTabLevelsInBothModes() {
        listOf(false, true).forEach { dark ->
            val colors = estateColorScheme(dark)
            assertEquals(Color(0xFF2D2D2D), colors.background)
            assertEquals(Color(0xFF2D2D2D), colors.surface)
        }
        listOf(0xFF006400, 0xFF228B22, 0xFF32CD32, 0xFF7FFF00).forEachIndexed { level, hex ->
            assertEquals(Color(hex), bookTabBackground(level))
            assertTrue("tab level $level", contrast(bookTabBackground(level), bookTabForeground(level)) >= 4.5f)
        }
    }

    @Test fun moduleAccentsMatchNavigationAndDashboard() {
        assertEquals(BookAccent.Crop, bookAccent("reports"))
        assertEquals(BookAccent.Crop, bookAccent("Crop Production"))
        assertEquals(BookAccent.Harvesting, bookAccent("Harvesting Coverage"))
        assertEquals(BookAccent.Productivity, bookAccent("Harvester Productivity"))
        assertEquals(BookAccent.Upkeep, bookAccent("upkeep"))
        assertEquals(BookAccent.Rainfall, bookAccent("rainfall"))
        assertEquals(BookAccent.Dashboard, bookAccent("home"))
    }
}
