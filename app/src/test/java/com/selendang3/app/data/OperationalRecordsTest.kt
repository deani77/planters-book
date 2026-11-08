package com.selendang3.app.data
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
class OperationalRecordsTest {
    @Test fun zeroMissingAndInvalidPeriodsStayDistinct() {
        val record = MonthlyCropRecord("NEW-A", YearMonth.of(2026, 2), LocalDate.of(2026, 2, 28), 0.0, 10.0, 5.0, true)
        assertEquals(0.0, record.actualMtPerHa!!, 0.0)
        assertNull(record.copy(areaHa = 0.0).actualMtPerHa)
        assertNull(record.copy(actualMt = null).achievementPercent)
        assertThrows(IllegalArgumentException::class.java) { record.copy(through = LocalDate.of(2026, 2, 20)) }
        assertThrows(IllegalArgumentException::class.java) { record.copy(actualMt = -1.0) }
    }
    @Test fun abwPreservesBasisAndRequiresExplicitCutoff() {
        AbwRecord("NEW-A", 2026, AbwBasis.YTD, LocalDate.of(2026, 6, 30), 18.0)
        AbwRecord("NEW-A", 2026, AbwBasis.ANNUAL, LocalDate.of(2026, 12, 31), 18.0)
        assertThrows(IllegalArgumentException::class.java) { AbwRecord("NEW-A", 2026, AbwBasis.ANNUAL, LocalDate.of(2026, 6, 30), 18.0) }
    }
    @Test fun bagsRequireWeightBeforeConversionAndEstateScopeIsExplicit() {
        val record = UpkeepRecord("1", LocalDate.of(2026, 1, 1), null, UpkeepOperation.MANURING, 2.0, WorkUnit.BAGS, null, 1, "")
        assertNull(record.kilograms)
        assertEquals(100.0, record.copy(bagWeightKg = 50.0).kilograms!!, 0.0)
    }
}
