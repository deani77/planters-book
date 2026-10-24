package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class TopographyTest {
    private fun data() = EstatePackage(1, "Fictional test", "2026-10-21T00:00:00Z", listOf(
        EstateTable("fields", listOf("Field code", "Area (ha)"), listOf(listOf("DEMO", "100"))),
        EstateTable("soil", listOf("Type", "Category", "Share (%)", "Area (ha)"), listOf(
            listOf("Flat", "Terrain", "15", "15"),
            listOf("Hilly", "Terrain", "10", "10"),
            listOf("Undulating", "Terrain", "75", "75"),
            listOf("Example soil", "Soil series", "100", "100"),
        )),
    ), "")
    @Test fun terrainUsesHectaresAndSharesAndExcludesSoilSeries() {
        val topography = estateTopography(data())!!
        assertEquals(listOf("Flat", "Hilly", "Undulating"), topography.bands.map { it.label })
        assertEquals(100.0, topography.totalArea, 0.0001)
        assertEquals(listOf(15.0, 10.0, 75.0), topography.bands.map { it.share })
    }
    @Test fun olderAreaBasisIsNotPresentedAsCurrent() {
        val source = data()
        val older = source.copy(tables = source.tables.map { table ->
            if (table.id == "fields") table.copy(rows = listOf(listOf("DEMO", "90"))) else table
        })
        assertNull(estateTopography(older))
    }
    @Test fun missingAndDuplicateTerrainStayPending() {
        assertNull(estateTopography(null))
        val source = data()
        assertNull(estateTopography(source.copy(tables = source.tables.map { table ->
            if (table.id == "soil") table.copy(rows = table.rows + listOf(table.rows.first())) else table
        })))
    }
}
