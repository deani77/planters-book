package com.selendang3.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class EstatePillsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fillsRowsAndFinalPillAndUsesEqualHeights() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp).testTag("boundary")) {
                    EstatePillSelector(listOf("Crop (mt)", "Yield (mt/ha)", "Achievement (%)"), "Crop (mt)", {}) { it }
                }
            }
        }
        val boundary = compose.onNodeWithTag("boundary").fetchSemanticsNode().boundsInRoot
        val pills = listOf("Crop (mt)", "Yield (mt/ha)", "Achievement (%)").map {
            compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot
        }
        assertEquals(boundary.left, pills[0].left, 1f)
        assertEquals(boundary.right, pills[1].right, 1f)
        assertEquals(boundary.left, pills[2].left, 1f)
        assertEquals(boundary.right, pills[2].right, 1f)
        pills.forEach { assertEquals(pills[0].height, it.height, 1f) }
    }

    @Test fun largeTextStacksWithoutTruncationAndSelectionStillWorks() {
        val selection = mutableStateOf("Short")
        var overflow = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme {
                    Box(Modifier.width(280.dp).testTag("boundary")) {
                        EstatePillGrid(2) { index ->
                            val label = if (index == 0) "Short" else "Roads and Infrastructure Management"
                            androidx.compose.material3.FilledTonalButton(onClick = { selection.value = label }) {
                                androidx.compose.material3.Text(label, onTextLayout = { if (it.hasVisualOverflow) overflow = true })
                            }
                        }
                    }
                }
            }
        }
        val first = compose.onNodeWithText("Short").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithText("Roads and Infrastructure Management").fetchSemanticsNode().boundsInRoot
        assertEquals(first.width, last.width, 1f)
        assertEquals(first.height, last.height, 1f)
        assert(last.top >= first.bottom)
        compose.onNodeWithText("Roads and Infrastructure Management").performClick()
        compose.runOnIdle {
            assertFalse(overflow)
            assertEquals("Roads and Infrastructure Management", selection.value)
        }
    }
}
