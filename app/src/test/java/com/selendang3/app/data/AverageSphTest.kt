package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class AverageSphTest {
    private fun fixture(rows: List<List<String?>>) = EstatePackage(1, "Test", "", listOf(
        EstateTable("fields", listOf("Field code", "Area (ha)", "Total palms", "Source palms/ha"), rows)
    ), "")
    @Test fun usesTotalsRatherThanAverageFieldDensity() {
        val result = estateAverageSph(fixture(listOf(
            listOf("A", "10", "100", "10"), listOf("B", "90", "9,000", "100")
        )))!!
        assertEquals(9100L, result.totalPalms)
        assertEquals(100.0, result.totalArea, 0.0001)
        assertEquals(91.0, result.palmsPerHa, 0.0001)
    }
    @Test fun missingCountsDoNotProducePartialEstateDensity() {
        assertNull(estateAverageSph(fixture(listOf(listOf("A", "10", null, "100")))))
    }
    @Test fun duplicateFieldsAndInvalidInputsRemainPending() {
        assertNull(estateAverageSph(fixture(listOf(listOf("A", "10", "100", null), listOf("a", "20", "200", null)))))
        for (area in listOf("0", "-1", "NaN", "Infinity"))
            assertNull(estateAverageSph(fixture(listOf(listOf("A", area, "100", null)))))
        for (count in listOf("-1", "1.5", "9223372036854775808"))
            assertNull(estateAverageSph(fixture(listOf(listOf("A", "10", count, null)))))
        assertNull(estateAverageSph(fixture(emptyList())))
    }
    @Test fun zeroPalmsIsMeasuredZero() {
        assertEquals(0.0, estateAverageSph(fixture(listOf(listOf("A", "10", "0", null))))!!.palmsPerHa, 0.0)
    }
}
