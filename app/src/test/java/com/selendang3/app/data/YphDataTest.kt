package com.selendang3.app.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class YphDataTest {
    private fun fixture() = JSONObject("""{
      "reportingYear":2026,"throughMonth":9,"matureAreaHa":100,
      "actualYtdTonnes":1200,"budgetYtdTonnes":1000,"fullYearBudgetTonnes":1800,
      "history":[{"year":2025,"actualYph":15},{"year":2024,"actualYph":null}]
    }""")
    @Test fun ytdAndAnnualBudgetRemainSeparate() {
        val data = parseYph(fixture())
        assertEquals(12.0, data.actualYtdYph!!, 0.0001)
        assertEquals(10.0, data.budgetYtdYph!!, 0.0001)
        assertEquals(18.0, data.fullYearBudgetYph!!, 0.0001)
        assertEquals(9, data.throughMonth)
        assertNull(data.history.first().actualYph)
    }
    @Test fun measuredZeroIsNotPending() {
        val data = parseYph(fixture().put("actualYtdTonnes", 0))
        assertEquals(0.0, data.actualYtdYph!!, 0.0001)
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalidAreaRejected() { parseYph(fixture().put("matureAreaHa", 0)) }
    @Test(expected = IllegalArgumentException::class)
    fun wrongBudgetPeriodRejected() { parseYph(fixture().put("budgetYtdTonnes", 2000)) }
    @Test(expected = IllegalArgumentException::class)
    fun duplicateHistoryRejected() {
        val value = fixture()
        value.getJSONArray("history").getJSONObject(1).put("year", 2025)
        parseYph(value)
    }
    @Test(expected = IllegalArgumentException::class)
    fun futureMonthRejected() { parseYph(fixture().put("throughMonth", 13)) }

    @Test fun displayIncludesExactlyFourPriorYearsAndKeepsMissingYearsPending() {
        val data = parseYph(fixture())
        assertEquals(listOf(2022, 2023, 2024, 2025), data.displayHistory.map { it.year })
        assertNull(data.displayHistory.first().actualYph)
        assertEquals(15.0, data.displayHistory.last().actualYph!!, 0.0001)
    }
    @Test fun rollingWindowDropsOlderRecordsAsReportingYearChanges() {
        val data = parseYph(fixture()).copy(reportingYear = 2027,
            history = (2020..2026).map { HistoricalYph(it, 10.0) })
        assertEquals(listOf(2023, 2024, 2025, 2026), data.displayHistory.map { it.year })
    }
}
