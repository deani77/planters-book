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
        assertEquals(listOf(2, 6, 2, 2, 2), summary.groups.map { it.rows.size })
        assertEquals(listOf("A", "100.00"), summary.groups[0].rows.map { it.value })
        assertEquals(listOf("100.00", "1000.00", "1200.00", "120.00", "1200.00"), summary.groups[1].rows.take(5).map { it.value })
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
    private fun pastReport(date: String, tonnes: Any = 1000, area: Any = 50, code: String = "A") =
        JSONObject().put("reportDate", date).put("fields", JSONArray().put(JSONObject()
            .put("field", code).put("sentToMillYtdMt", tonnes).put("areaHa", area)))
    private fun previousRows(history: JSONArray, code: String = "A", date: String = "2026-10-21",
        now: LocalDate = today): List<FieldCropSummaryRow> {
        val original = fixture()
        val root = JSONObject(original.json).put("cropProductionReportHistory", history)
        root.getJSONObject("cropProductionReport").put("reportDate", date)
        return fieldCropSummary(original.copy(json = root.toString()), now, code)!!.groups[1].rows.takeLast(1)
    }
    @Test fun previousYearShowsOnlyMatchingCutoffUsingHistoricalArea() {
        val history = JSONArray().put(pastReport("2025-12-31", 1000, 50)).put(pastReport("2025-09-30", 600, 40))
        val rows = previousRows(history)
        assertEquals(listOf("15.00"), rows.map { it.value })
        assertEquals(listOf("Last Year Same-Period YTD · through 2025-09-30 (mt/ha)"), rows.map { it.label })
        assertTrue(previousRows(history, "B").all { it.value == "Pending" })
        assertTrue(previousRows(history, now = LocalDate.of(2026, 8, 1)).all { it.value == "Pending" })
        assertEquals("0.00", previousRows(JSONArray().put(pastReport("2025-09-30", 0)))[0].value)
    }
    @Test fun previousYearNeverUsesPartialYearsCurrentAreaOrAmbiguousRecords() {
        for (history in listOf(JSONArray(), JSONArray().put(pastReport("2025-12-30")),
            JSONArray().put(pastReport("2025-10-20")),
            JSONArray().put(pastReport("2025-09-30")).put(pastReport("2025-09-30")),
            JSONArray().put(pastReport("2025-09-30", area = 0)),
            JSONArray().put(pastReport("2025-09-30", area = JSONObject.NULL)),
            JSONArray().put(pastReport("2025-09-30", tonnes = -1)),
            JSONArray().put(pastReport("2025-09-30", tonnes = JSONObject.NULL)),
            JSONArray().put(pastReport("2025-09-30", area = "50")),
            JSONArray().put(pastReport("2025-09-30").also {
                it.getJSONArray("fields").put(it.getJSONArray("fields").getJSONObject(0))
            }))) {
            assertTrue(previousRows(history).all { it.value == "Pending" })
        }
    }
    @Test fun completedMonthHandlesBoundariesLeapMonthAndJanuaryRollover() {
        for (date in listOf("2026-10-01", "2026-10-31")) {
            val rows = previousRows(JSONArray().put(pastReport("2025-09-30", 600, 40)),
                date = date, now = LocalDate.of(2026, 12, 31))
            assertEquals("15.00", rows[0].value)
            assertTrue(rows[0].label.contains("2025-09-30"))
        }
        val march = previousRows(JSONArray().put(pastReport("2024-02-29")), date = "2025-03-15")
        assertEquals("20.00", march[0].value)
        assertTrue(march[0].label.contains("2024-02-29"))
        val january = previousRows(JSONArray().put(pastReport("2025-12-31", 1000))
            .put(pastReport("2024-12-31", 600)), date = "2026-01-15")
        assertEquals(listOf("12.00"), january.map { it.value })
        assertTrue(january[0].label.contains("2024-12-31"))
        assertEquals("Pending", previousRows(JSONArray().put(pastReport("2025-10-21")))[0].value)
    }
    @Test fun previousYearUsesCompletedMonthAndReportYearInsteadOfDeviceYear() {
        val rows = previousRows(JSONArray().put(pastReport("2023-01-31", 600, 40)),
            date = "2024-02-29", now = LocalDate.of(2026, 10, 24))
        assertEquals("15.00", rows[0].value)
        assertTrue(rows[0].label.contains("2023-01-31"))
        val december = previousRows(JSONArray().put(pastReport("2025-12-31")),
            date = "2026-12-31", now = LocalDate.of(2026, 12, 31))
        assertEquals(listOf("Pending"), december.map { it.value })
    }

}


