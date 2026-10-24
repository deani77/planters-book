package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class AgeProfileTest {
    private fun data(ages: List<Int>): EstatePackage = EstatePackage(1, "Fictional test", "2026-10-21T00:00:00Z", listOf(
        EstateTable("fields", listOf("Field code", "Area (ha)"), ages.indices.map { listOf("DEMO$it", "1.0") }),
        EstateTable("ages", listOf("Field / category", "Age / years"), ages.mapIndexed { i, age -> listOf("DEMO$i", "$age years") }),
    ), "")

    @Test fun categoryBoundariesUseNewRanges() {
        val profile = estateAgeProfile(data(listOf(1, 5, 6, 23, 24)))!!
        assertEquals(listOf(2.0, 2.0, 1.0), profile.bands.map { it.area })
        assertEquals(5.0, profile.totalArea, 0.0001)
    }
    @Test fun incompleteOrZeroAgeDoesNotShowFalseZeroShares() {
        assertNull(estateAgeProfile(null))
        assertNull(estateAgeProfile(data(listOf(0))))
        val missing = data(listOf(7)).let { it.copy(tables = it.tables.map { t -> if (t.id == "ages") t.copy(rows = emptyList()) else t }) }
        assertNull(estateAgeProfile(missing))
    }
    @Test fun percentagesUseHectaresRatherThanFieldCounts() {
        val initial = data(listOf(5, 6))
        val weighted = initial.copy(tables = initial.tables.map { t ->
            if (t.id == "fields") t.copy(rows = listOf(listOf("DEMO0", "1.0"), listOf("DEMO1", "3.0"))) else t
        })
        val profile = estateAgeProfile(weighted)!!
        assertEquals(25.0, profile.bands.first().area / profile.totalArea * 100, 0.0001)
    }
}
