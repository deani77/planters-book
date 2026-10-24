package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HomeDashboardDataTest {
    private fun fixture(year: Int = 2026, month: Int = 9, area: Double = 1000.0) =
        EstatePackage(1, "Fictional", "", listOf(EstateTable("overview", listOf("Item", "Details"), listOf(
            listOf("FFB cost YTD reporting period", "%04d-%02d".format(year, month)),
            listOf("FFB cost budget year", year.toString()),
            listOf("FFB cost operation actual YTD (RM/tonne)", "123"),
            listOf("FFB cost operation budget year (RM/tonne)", "100"),
            listOf("FFB cost general charges actual YTD (RM/tonne)", "45"),
            listOf("FFB cost general charges budget year (RM/tonne)", "40")
        ))), "", YphData(year, month, area, 25000.0, 20000.0, 30000.0, emptyList()))
    private val today = LocalDate.of(2026, 10, 22)
    @Test fun usesActualYtdWithExplicitPeriodAndDoesNotInventMonthlyOrProgressData() {
        val rows = homeDashboardRows(fixture(), today)
        assertEquals(3, rows.size)
        assertTrue(rows[0].detail.contains("Sep 2026: Pending"))
        assertTrue(rows[0].detail.contains("Oct 2026 MTD: Pending"))
        assertTrue(rows[0].detail.contains("YTD through Sep 2026: 25.00"))
        assertTrue(rows[1].detail.contains("Weeding: Pending"))
        assertTrue(rows[1].detail.contains("Manuring: Pending"))
        assertTrue(rows[2].detail.contains("Operation: 123.00 · GC: 45.00"))
        assertFalse(rows[2].detail.contains("100.00"))
    }
    @Test fun missingFutureAndOldYearRecordsRemainPending() {
        for (source in listOf(null, fixture(2025), fixture(2026, 11))) {
            val rows = homeDashboardRows(source, today)
            assertTrue(rows[0].detail.contains("YTD 2026: Pending"))
            assertTrue(rows[2].detail.contains("Operation: Pending · GC: Pending"))
        }
        assertTrue(homeDashboardRows(fixture(area = 0.0), today)[0].detail.endsWith("Pending"))
    }
    @Test fun januaryUsesDecemberOfPreviousYearAndHidesLastYearsYtd() {
        val rows = homeDashboardRows(fixture(), LocalDate.of(2027, 1, 1))
        assertTrue(rows[0].detail.contains("Dec 2026: Pending"))
        assertTrue(rows[0].detail.contains("Jan 2027 MTD: Pending"))
        assertTrue(rows[0].detail.contains("YTD 2027: Pending"))
    }

    @Test fun annualBudgetAchievementUsesAnnualDenominatorAndHandlesMissingOrZeroBudget() {
        val base = fixture()
        assertTrue(homeDashboardRows(base, today)[0].detail.endsWith("YTD vs Year Budget: 83.33%"))
        for (budget in listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val input = base.copy(yph = base.yph!!.copy(fullYearBudgetTonnes = budget))
            assertTrue(homeDashboardRows(input, today)[0].detail.endsWith("YTD vs Year Budget: Pending"))
        }
        val zero = base.copy(yph = base.yph!!.copy(actualYtdTonnes = 0.0))
        val over = base.copy(yph = base.yph!!.copy(actualYtdTonnes = 60000.0))
        val missing = base.copy(yph = base.yph!!.copy(actualYtdTonnes = null))
        assertTrue(homeDashboardRows(zero, today)[0].detail.endsWith("YTD vs Year Budget: 0.00%"))
        assertTrue(homeDashboardRows(over, today)[0].detail.endsWith("YTD vs Year Budget: 200.00%"))
        assertTrue(homeDashboardRows(missing, today)[0].detail.endsWith("YTD vs Year Budget: Pending"))
    }

    @Test fun operationalCardsMatchSharedTitlesUnitsValuesAndPendingWork() {
        val cards = operationalDashboardCards(fixture(), today)
        assertEquals(listOf("Crop Production", "Harvesting Coverage", "Harvester Productivity",
            "Manuring", "Weeding", "Pest & Disease", "Estate Cost"), cards.map { it.title })
        assertEquals(OperationalMetric("Actual YTD", "25.00", "mt/ha"), cards[0].metrics[2])
        assertEquals(OperationalMetric("YTD / Year Budget", "83.33", "%"), cards[0].metrics[3])
        assertEquals("YTD through Sep 2026", cards[0].period)
        assertEquals(listOf("Circle", "Selective"), cards[4].metrics.map { it.label })
        assertEquals(listOf("123.00", "45.00"), cards.last().metrics.map { it.value })
        assertTrue(cards.last().metrics.all { it.unit == "RM/mt" })
        assertTrue(cards.subList(1, 6).all { it.metrics.all { metric -> metric.value == "Pending" } })
        for (input in listOf(null, fixture(2025), fixture(2026, 11))) {
            val pending = operationalDashboardCards(input, today)
            assertEquals("Pending", pending[0].metrics[2].value)
            assertEquals("Pending", pending[0].metrics[3].value)
            assertTrue(pending.last().metrics.all { it.value == "Pending" })
        }
        assertTrue(operationalDashboardCards(null, today).all { it.metrics.all { metric -> metric.unit.isNotBlank() } })
    }
    @Test fun approvedReportsPopulateSharedCardsAndKeepActualReportDates() {
        val json = """{
          "cropProductionReport":{"reportDate":"2026-10-21","totals":{"sentToMillMtdMtPerHa":1.25,"hectaresToday":100,"hectaresMtd":2000,"roundsMtd":2,"mtPerHarvesterDayMtd":2.5,"haPerHarvesterDayMtd":3}},
          "cropProductionReportHistory":[{"reportDate":"2026-09-30","isCompleteMonth":true,"totals":{"sentToMillMtdMtPerHa":2.75}}],
          "manuringReport":{"reportDate":"2026-10-22","annualProgrammeMt":1000,"appliedYtdMt":800}
        }"""
        val input = fixture().copy(json = json)
        val cards = operationalDashboardCards(input, today)
        assertEquals(listOf("2.75", "1.25"), cards[0].metrics.take(2).map { it.value })
        assertEquals(listOf("100.00", "2000.00", "2.00"), cards[1].metrics.map { it.value })
        assertEquals("Report: 2026-10-21", cards[1].period)
        assertEquals(listOf("2.50", "3.00"), cards[2].metrics.map { it.value })
        assertEquals("80.00", cards[3].metrics.single().value)
        assertTrue(homeDashboardRows(input, today)[1].detail.contains("Manuring: 80.00%"))
        val later = operationalDashboardCards(input, LocalDate.of(2026, 11, 1))
        assertEquals("Pending", later[0].metrics[1].value)
        val future = operationalDashboardCards(input, LocalDate.of(2026, 8, 1))
        assertEquals("Pending", future[3].metrics.single().value)
    }
}
