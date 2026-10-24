package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ProductionTest {
    private fun row(date: String, field: String = "PM04D", mill: Double? = 2.0, backlog: Double? = null) =
        ProductionDraft(date, field, null, mill, backlog, 3.0, 4)
    private val through = LocalDate.parse("2026-02-10")

    @Test fun aliasesPreserveOriginalFieldCodes() {
        assertEquals("PM04D", productionAliases["0256PM0401"])
        assertEquals("PM15P", productionAliases["0256PM1511"])
        assertEquals(productionFields.toSet(), productionAliases.values.toSet())
    }
    @Test fun filtersByFieldAndInclusiveReportingWindow() {
        val rows = listOf(row("2025-12-31"), row("2026-01-31"), row("2026-02-01"),
            row("2026-02-10", mill = 0.0), row("2026-02-11"), row("2026-02-05", "PM05E"))
        val month = productionSummary(rows, "PM04D", through.withDayOfMonth(1), through)
        val year = productionSummary(rows, "PM04D", through.withDayOfYear(1), through)
        assertEquals(2, month.days)
        assertEquals(2.0, month.sentToMill.value!!, 0.0001)
        assertEquals(4.0, year.sentToMill.value!!, 0.0001)
        assertEquals(8.0, month.harvesterDays.value!!, 0.0001)
    }
    @Test fun blanksStayPendingAndBacklogIsNotSummedOrCarriedForward() {
        val rows = listOf(row("2026-02-01", backlog = 6.0), row("2026-02-10", mill = null))
        val result = productionSummary(rows, "PM04D", through.withDayOfMonth(1), through)
        assertNull(result.harvested.value)
        assertEquals(1, result.sentToMill.reportedDays)
        assertEquals(2, result.sentToMill.savedDays)
        assertNull(result.backlog)
        assertEquals("2026-02-10", result.backlogDate)
        val empty = productionSummary(emptyList(), "PM04D", through.withDayOfMonth(1), through)
        assertNull(empty.sentToMill.value)
    }
    @Test fun duplicateFieldDatesAreRejected() {
        val duplicate = row("2026-02-01")
        assertThrows(IllegalArgumentException::class.java) {
            productionSummary(listOf(duplicate, duplicate), "PM04D", through.withDayOfMonth(1), through)
        }
    }
    @Test fun invalidDraftsAreRejectedButMeasuredZeroIsValid() {
        row("2026-02-01", mill = 0.0).validate()
        listOf(row("2026-02-01", mill = -1.0), row("2026-02-01", mill = Double.NaN),
            row("2026-02-01", field = "unknown"), row("2026-02-01").copy(harvesters = -1)).forEach { draft ->
            assertThrows(IllegalArgumentException::class.java) { draft.validate() }
        }
        assertThrows(java.time.format.DateTimeParseException::class.java) { row("2026-02-30").validate() }
    }
}
