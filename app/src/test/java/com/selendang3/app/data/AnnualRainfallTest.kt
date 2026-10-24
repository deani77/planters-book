package com.selendang3.app.data

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AnnualRainfallTest {
    private val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    private fun rows(year: Int, count: Int = 12): List<List<String?>> =
        months.take(count).map { listOf(year.toString(), it, "10") }
    private fun fixture(rows: List<List<String?>>) = EstatePackage(1, "Test", "", listOf(
        EstateTable("rainfall", listOf("Year", "Month / total", "Rainfall (mm)"), rows)
    ), "")
    private val october = LocalDate.of(2026,10,22)
    @Test fun fiveYearsNewestFirstWithCurrentMonthExcludedAndPrintedTotalsIgnored() {
        val data = fixture((2021..2026).flatMap { rows(it) } + listOf(listOf("2026","TOTAL (source)","9999")))
        val result = estateAnnualRainfall(data,october)
        assertEquals(listOf(2026,2025,2024,2023,2022),result.map { it.year })
        assertEquals(9,result.first().throughMonth)
        assertEquals(90.0,result.first().totalMm!!,0.0)
        assertEquals(120.0,result.last().totalMm!!,0.0)
    }
    @Test fun missingAndInvalidMonthsArePendingWhileConfirmedZeroCounts() {
        assertNull(estateAnnualRainfall(fixture(rows(2026,8)),october).first().totalMm)
        for (value in listOf(null,"NaN","Infinity","-1","pending")) {
            val data = rows(2026,8) + listOf(listOf("2026","Sep",value))
            assertNull(estateAnnualRainfall(fixture(data),october).first().totalMm)
        }
        val zero = rows(2026,8) + listOf(listOf("2026","Sep","0"))
        assertEquals(80.0,estateAnnualRainfall(fixture(zero),october).first().totalMm!!,0.0)
    }
    @Test fun duplicateMonthCannotInflateTotal() {
        assertNull(estateAnnualRainfall(fixture(rows(2026)+listOf(listOf("2026","Jan","10"))),october).first().totalMm)
    }
    @Test fun januaryHasNoCompletedMonthAndOldYearNeedsDecember() {
        val result = estateAnnualRainfall(fixture(rows(2026,11)),LocalDate.of(2027,1,1))
        assertEquals(listOf(2027,2026,2025,2024,2023),result.map { it.year })
        assertEquals(0,result.first().throughMonth); assertNull(result.first().totalMm)
        assertNull(result[1].totalMm)
    }
    @Test fun cutoffAdvancesByCalendarWithoutTreatingMissingMonthAsZero() {
        val data = fixture(rows(2026,9))
        assertEquals(90.0,estateAnnualRainfall(data,october).first().totalMm!!,0.0)
        assertNull(estateAnnualRainfall(data,LocalDate.of(2026,11,1)).first().totalMm)
        assertNull(estateAnnualRainfall(null,october).first().totalMm)
    }
}
