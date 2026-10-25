package com.selendang3.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FieldCropSummaryTest {
    private val today = LocalDate.of(2026, 10, 24)
    private fun fixture(): EstatePackage {
        val budget = JSONArray()
        listOf("A", "B").forEach { code -> (1..12).forEach { month ->
            budget.put(JSONObject().put("field", code).put("year", 2026).put("month", month).put("tonnes", if (code == "A") 100 else 200))
        } }
        val root = JSONObject().put("cropBudget", JSONObject().put("year", 2026).put("monthlyBudget", budget))
            .put("cropProductionReport", JSONObject().put("reportDate", "2026-10-21").put("fields", JSONArray()
                .put(JSONObject().put("field", "A").put("sentToMillMtdMt", 120).put("sentToMillYtdMt", 1200)
                    .put("roundsMtd", 2.5).put("mtPerHarvesterDayMtd", 1.2).put("haPerHarvesterDayMtd", 3.4))
                .put(JSONObject().put("field", "B").put("sentToMillMtdMt", 180).put("sentToMillYtdMt", 2400))))
        return EstatePackage(1, "Fictional data", "", listOf(EstateTable("fields", listOf("Field code", "Area (ha)"),
            listOf(listOf("A", "100"), listOf("B", "42")))), root.toString())
    }
    @Test fun summaryUsesOrderedRequestedMetricsAndRoundsCrewUp() {
        val summary = fieldCropSummary(fixture(), today, "A")!!
        assertEquals(listOf(2, 5, 2, 2, 2), summary.groups.map { it.rows.size })
        assertEquals(listOf("A", "100.00"), summary.groups[0].rows.map { it.value })
        assertEquals(listOf("100.00", "1000.00", "1200.00", "120.00", "1200.00"), summary.groups[1].rows.map { it.value })
        assertEquals(listOf("5", "2.50"), summary.groups[3].rows.map { it.value })
        assertEquals(listOf("1.20", "3.40"), summary.groups[4].rows.map { it.value })
        assertTrue(summary.crewNote.contains("4.76 harvesters"))
        assertTrue(summary.note.contains("2026-10-21"))
        assertEquals("2", fieldCropSummary(fixture(), today, "B")!!.groups[3].rows[0].value)
    }
    @Test fun unavailableFutureAndWrongFieldValuesAreNotSubstituted() {
        assertNull(fieldCropSummary(fixture(), today, "UNKNOWN"))
        val future = fieldCropSummary(fixture(), LocalDate.of(2026, 8, 1), "A")!!
        assertTrue(future.groups[1].rows.all { it.value == "Pending" })
        assertTrue(future.groups[4].rows.all { it.value == "Pending" })
        val b = fieldCropSummary(fixture(), today, "B")!!
        assertTrue(b.groups[4].rows.all { it.value == "Pending" })
        assertEquals("180.00", b.groups[1].rows[3].value)
    }
    @Test fun abwUsesCumulativeFieldValuesAndDoesNotInventMissingData() {
        fun abw(rows: List<List<String?>>, code: String = "A"): FieldCropSummaryGroup {
            val original = fixture()
            val update = original.copy(tables = original.tables + EstateTable("abw",
                listOf("Field code", "Year", "YTD ABW (kg/bunch)"), rows))
            return fieldCropSummary(update, today, code)!!.groups[2]
        }
        val rows = listOf(listOf("A", "2026", "12.34"), listOf("A", "2025", "11.25"), listOf("B", "2026", "0"))
        val group = abw(rows)
        assertEquals("Average Bunch Weight", group.title)
        assertEquals(listOf("2026 YTD (kg/bunch)", "2025 YTD (kg/bunch)"), group.rows.map { it.label })
        assertEquals(listOf("12.34", "11.25"), group.rows.map { it.value })
        assertTrue(group.note.contains("cutoff unconfirmed"))
        assertEquals(listOf("0.00", "Pending"), abw(rows, "B").rows.map { it.value })
        assertEquals("Pending", abw(rows + listOf(listOf("A", "2026", "99"))).rows[0].value)
        assertTrue(abw(listOf(listOf("A", "2026", "-1"), listOf("A", "2025", null),
            listOf("A", "2027", "55"))).rows.all { it.value == "Pending" })
    }
}


