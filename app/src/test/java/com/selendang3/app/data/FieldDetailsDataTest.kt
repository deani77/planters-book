package com.selendang3.app.data
import org.junit.Assert.*
import org.junit.Test

class FieldDetailsDataTest {
    @Test fun sourceProfilesPreserveMissingSoilZeroHeightAndSelectedField() {
        val json = """{"fieldProfiles":[{"fieldCode":"A","reportYear":2027,
            "plantedAreaHa":10.5,"unplantedAreaHa":1.25,"divisionStandsPerHa":120,
            "divisionPalmHeightM":0,"maturity":"MATURED","blocks":[{
            "blockId":"EXAMPLE01","blockName":"01","plantedAreaHa":10.5,
            "unplantedAreaHa":1.25,"soilCode":null,"harvestingYear":2015,"sourceNote":null}]}]}"""
        val updated = input.copy(json = json)
        val tables = sourceFieldProfileTables(updated, "A")
        assertEquals(2, tables.size)
        assertEquals("0", tables[0].rows[0][3])
        assertNull(tables[1].rows[0][3])
        assertEquals("2015", tables[1].rows[0][4])
        assertTrue(sourceFieldProfileTables(updated, "B").isEmpty())
        assertTrue(sourceFieldProfileTables(null, "A").isEmpty())
        assertTrue(sourceFieldProfileTables(input.copy(json = "{}"), "A").isEmpty())
        assertEquals(input.tables, updated.tables)
    }
    @Test fun malformedProfilesDoNotCrashOrShowAmbiguousData() {
        for (json in listOf("""{"fieldProfiles":[{}]}""", """{"fieldProfiles":[null]}""",
            """{"fieldProfiles":[{"fieldCode":"A"},{"fieldCode":"A"}]}""")) {
            assertTrue(sourceFieldProfileTables(input.copy(json = json), "A").isEmpty())
        }
    }

    private val input = EstatePackage(1, "Fictional", "", listOf(
        EstateTable("fields", listOf("Field code", "Area (ha)"), listOf(listOf("A", "10"), listOf("B", "20"))),
        EstateTable("ages", listOf("Field / category", "Age / years"), listOf(listOf("A", "3 years"), listOf("B", "8 years"))),
        EstateTable("overview", listOf("Item", "Details"), listOf(listOf("Source", "A")))
    ), "")
    @Test fun fieldButtonsUseSavedCodesAndDetailsStayIsolatedToSelectedField() {
        assertEquals(listOf("A", "B"), estateFieldCodes(input))
        val tables = estateFieldTables(input, "A")
        assertEquals(listOf("fields", "ages"), tables.map { it.id })
        assertTrue(tables.all { it.rows.size == 1 && it.rows[0][0] == "A" })
        assertTrue(estateFieldTables(input, "UNKNOWN").isEmpty())
        assertTrue(estateFieldCodes(null).isEmpty())
    }
    @Test fun cropRecordsMoveToProductionWithoutChangingSavedTables() {
        val fields = EstateTable("fields", listOf("Field code", "Area (ha)", "Total palms", "Sent to mill MTD (mt)", "Harvested area MTD (ha)"),
            listOf(listOf("A", "10", "1000", "120", "200"), listOf("B", "20", "2000", "180", "300")))
        val updated = input.copy(tables = listOf(fields) + input.tables.drop(1) +
            EstateTable("abw", listOf("Field", "ABW (kg/bunch)"), listOf(listOf("A", "18"), listOf("B", "20"))))
        val basic = estateFieldTables(updated, "A")
        val crop = estateFieldCropTables(updated, "A")
        assertEquals(listOf("Field code", "Area (ha)", "Total palms"), basic.first().columns)
        assertEquals(listOf("fields", "abw"), crop.map { it.id })
        assertEquals(listOf("Field code", "Sent to mill MTD (mt)", "Harvested area MTD (ha)"), crop.first().columns)
        assertTrue(crop.all { it.rows.size == 1 && it.rows[0][0] == "A" })
        assertTrue(estateFieldCropTables(updated, "UNKNOWN").isEmpty())
        assertEquals(5, updated.tables.first().columns.size)
    }
}
