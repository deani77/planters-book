package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class EstateAreaSummaryTest {
    private val today = LocalDate.of(2026, 10, 24)
    private fun fixture(perimeter: String = "1200", mature: String = "1000", period: String = "2026-09") =
        EstatePackage(1, "Fictional data", "", listOf(EstateTable("overview", listOf("Item", "Details"), listOf(
            listOf("Estate area reporting period", period), listOf("Perimeter area (ha)", perimeter), listOf("Mature area (ha)", mature)))), "")
    @Test fun usesExplicitEstateAreaRecordsAndValidReportingPeriod() {
        val result = estateAreaSummary(fixture(), today)!!
        assertEquals(1200.0, result.perimeterHa, 0.0)
        assertEquals(1000.0, result.matureHa, 0.0)
        assertEquals("2026-09", result.period.toString())
        assertNull(estateAreaSummary(fixture(perimeter = "900"), today))
        assertNull(estateAreaSummary(fixture(period = "2026-11"), today))
        assertNull(estateAreaSummary(fixture(perimeter = "0"), today))
        assertNull(estateAreaSummary(null, today))
        assertEquals(0.0, estateAreaSummary(fixture(mature = "0"), today)!!.matureHa, 0.0)
    }
}

