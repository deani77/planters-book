package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class SoilSeriesTest {
    private fun fixture(area: String = "60", rating: String? = "III", reference: String? = "Published reference") =
        EstatePackage(1, "Test", "", listOf(
            EstateTable("fields", listOf("Area (ha)"), listOf(listOf("100"))),
            EstateTable("soil", listOf("Type", "Category", "Area (ha)", "Share (%)", "Suitability class", "Class reference"), listOf(
                listOf("Example A", "Soil series", area, "60", rating, reference),
                listOf("Example B", "Soil series", "40", "40", null, null),
                listOf("Flat", "Terrain", "100", "100", null, null)
            ))
        ), "")
    @Test fun showsOnlySeriesWithReferencedClasses() {
        val result = estateSoilSeries(fixture())!!
        assertEquals(2, result.bands.size)
        assertEquals(100.0, result.totalArea, 0.0001)
        assertEquals("Class III*", result.bands.first().suitability)
        assertEquals("Pending", result.bands.last().suitability)
    }
    @Test fun oldAreaBasisRemainsPending() { assertNull(estateSoilSeries(fixture(area = "65"))) }
    @Test fun unsupportedOrUnreferencedClassesArePending() {
        assertEquals("Pending", estateSoilSeries(fixture(rating = "VI"))!!.bands.first().suitability)
        assertEquals("Pending", estateSoilSeries(fixture(reference = null))!!.bands.first().suitability)
    }
}
