package com.selendang3.app.data
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
class PlatformParityTest {
    @Test fun sharedFixturePreservesIdentifiersFactsAndNumericResults() {
        val file = listOf(File("../sample-data/architecture-parity.json"), File("sample-data/architecture-parity.json")).first { it.exists() }
        val update = EstatePackages.parse(file.readText())
        assertEquals(listOf("USER-A"), estateFieldCodes(update))
        val tables = estateFieldTables(update, "USER-A")
        assertEquals(listOf("Field Overview", "fields", "ages", "abw"), tables.map { it.id })
        assertEquals(listOf("USER-A", "OLD-A", "2010-01-01"), tables[1].rows[0])
        assertEquals("0", tables.last().rows[0].last())
        val metrics = cropProductionTable(update, LocalDate.of(2026, 10, 21)).rows.single().metrics
        val summary = fieldCropSummary(update, LocalDate.of(2026, 10, 21), "USER-A")!!
        val visible = fieldCropReportGroups(summary)
        assertTrue(summary.note.contains("2026-10-20"))
        assertEquals(listOf("Field Profile", "Crop Budget and Actual", "Harvesting", "Harvester Productivity"), visible.map { it.title })
        assertTrue(visible[1].rows.any { it.label == "Actual MTD (mt)" && it.value == "20.00" })
        assertEquals(2.0, metrics.actual(CropPeriod.MTD, true)!!, 0.0)
        assertEquals(200.0 / 120.0 * 100, metrics.achievement(CropPeriod.ANNUAL)!!, 1e-9)
    }
}
