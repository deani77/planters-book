package com.selendang3.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FieldProductionDataTest {
    private val today = LocalDate.of(2026, 3, 20)
    private fun report(date: String, actual: Double = 20.0, area: Double = 5.0, complete: Boolean = false) =
        JSONObject().put("reportDate", date).put("isCompleteMonth", complete).put("fields", JSONArray().put(
            JSONObject().put("field", "A").put("sentToMillMtdMt", actual).put("sentToMillYtdMt", 60)
                .put("areaHa", area).put("roundsMtd", 2).put("mtPerHarvesterDayMtd", 1.5).put("haPerHarvesterDayMtd", 3)))
    private fun fixture(): EstatePackage {
        val budget = JSONArray()
        (1..12).forEach { budget.put(JSONObject().put("field", "A").put("year", 2026).put("month", it).put("tonnes", 10)) }
        val root = JSONObject().put("cropProductionReport", report("2026-03-15"))
            .put("cropBudget", JSONObject().put("year", 2026).put("monthlyBudget", budget))
            .put("cropProductionReportHistory", JSONArray().put(report("2026-01-31", 10.0, 2.0, true)))
        return EstatePackage(1, "Example", "", listOf(EstateTable("fields", listOf("Field code", "Area (ha)"),
            listOf(listOf("A", "5")))), root.toString())
    }
    @Test fun performanceUsesExistingCalculationsAndPreservesHarvestingGroups() {
        val input = fixture()
        val rows = fieldProductionPerformance(input, today, "A")
        assertEquals(listOf("4.00 / 2.00 · Achievement: 200.00 %", "12.00 / 6.00 · Achievement: 200.00 %",
            "12.00 / 24.00 · Achievement: 50.00 %"), rows.map { it.value })
        val summary = fieldCropSummary(input, today, "A")!!
        assertTrue(summary.note.contains("2026-03-15"))
        val harvesting = fieldHarvestingGroups(summary)
        assertEquals(listOf("Harvesting", "Harvester Productivity"), harvesting.map { it.title })
        assertEquals(listOf("1.50", "3.00"), harvesting.last().rows.map { it.value })
        assertTrue(fieldProductionPerformance(input, today, "UNKNOWN").all { it.value.contains("Pending") })
    }
    @Test fun monthlyActualsUseCompletedMonthsAndHistoricalArea() {
        val input = fixture()
        val monthly = fieldMonthlyProduction(input, today, "A")
        assertEquals(2026, monthly.year)
        assertEquals(12, monthly.rows.size)
        assertEquals("10.00", monthly.rows[0].actualMt)
        assertEquals("5.00", monthly.rows[0].actualYield)
        assertEquals("Pending", monthly.rows[1].actualMt)
        assertEquals("20.00", monthly.rows[2].actualMt)
        assertEquals("4.00", monthly.rows[2].actualYield)
        assertTrue(monthly.rows[2].period.contains("2026-03-15"))
        assertEquals("Pending", monthly.rows[3].actualMt)
        assertEquals("10.00", monthly.rows[3].budgetMt)
        assertTrue(fieldMonthlyProduction(input, today, "B").rows.all { it.actualMt == "Pending" && it.budgetMt == "Pending" })
    }
    @Test fun missingAmbiguousInvalidAndPartialRecordsStayPending() {
        val base = fixture()
        for (history in listOf(
            JSONArray().put(report("2026-01-30", complete = true)),
            JSONArray().put(report("2026-01-31")),
            JSONArray().put(report("2026-01-31", complete = true)).put(report("2026-01-31", complete = true)),
            JSONArray().put(report("2026-01-31", -1.0, complete = true))
        )) {
            val input = base.copy(json = JSONObject(base.json).put("cropProductionReportHistory", history).toString())
            assertEquals("Pending", fieldMonthlyProduction(input, today, "A").rows[0].actualMt)
        }
        val historical = report("2026-01-31", 0.0, complete = true)
        historical.getJSONArray("fields").getJSONObject(0).remove("areaHa")
        val root = JSONObject(base.json).put("cropProductionReportHistory", JSONArray().put(historical))
        root.getJSONObject("cropBudget").getJSONArray("monthlyBudget").getJSONObject(0).put("tonnes", 0)
        val january = fieldMonthlyProduction(base.copy(json = root.toString()), today, "A").rows[0]
        assertEquals("0.00", january.actualMt)
        assertEquals("Pending", january.actualYield)
        assertEquals("Pending", january.achievement)
    }
    @Test fun historyOnlyFieldsRemainSelectableWithoutEstateFields() {
        val root = JSONObject(fixture().json).put("cropHistory", JSONObject().put("cutoff", "2026-03-15")
            .put("fields", JSONArray().put(JSONObject().put("fieldCode", "HISTORY-ONLY").put("productionYear", 2026)
                .put("productionTonnes", 10).put("plantedAreaHa", 2).put("annual", JSONObject()).put("areaSource", "Example"))))
        val input = fixture().copy(json = root.toString())
        assertEquals(listOf("A", "HISTORY-ONLY"), cropProductionFieldCodes(input))
        assertEquals(listOf("HISTORY-ONLY"), cropProductionFieldCodes(input.copy(tables = emptyList())))
        assertEquals(5.0, cropHistoryFields(input).single().yield(2026)!!, 0.001)
    }
}
