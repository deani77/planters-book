package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class FfbCostComparisonTest {
    private fun fixture(operationActual: String? = "120.50", operationBudget: String? = "110.25",
        generalActual: String? = "40.50", generalBudget: String? = "35.25", year: String = "2026") =
        EstatePackage(1,"Test","",listOf(EstateTable("overview",listOf("Item","Details"),listOf(
            listOf("FFB cost YTD reporting period","2026-09"),listOf("FFB cost budget year",year),
            listOf("FFB cost operation actual YTD (RM/tonne)",operationActual),
            listOf("FFB cost operation budget year (RM/tonne)",operationBudget),
            listOf("FFB cost general charges actual YTD (RM/tonne)",generalActual),
            listOf("FFB cost general charges budget year (RM/tonne)",generalBudget),
            listOf("FFB cost actual YTD (RM/tonne)","999.00"),
            listOf("FFB cost budget YTD (RM/tonne)","888.00")
        ))),"")
    @Test fun comparesTwoSeparateMeasuresWithExplicitDifferentPeriodBases() {
        val result = estateFfbCosts(fixture())!!
        assertEquals(2026,result.budgetYear); assertEquals(9,result.actualPeriod.monthValue)
        assertEquals(listOf("Operation Cost","General Charges"),result.groups.map { it.label })
        assertEquals(120.50,result.groups[0].actualYtdRmPerTonne!!,0.0)
        assertEquals(110.25,result.groups[0].budgetYearRmPerTonne!!,0.0)
        assertEquals(40.50,result.groups[1].actualYtdRmPerTonne!!,0.0)
        assertEquals(35.25,result.groups[1].budgetYearRmPerTonne!!,0.0)
    }
    @Test fun missingRatesNeverFallBackToOverallExpenseRates() {
        val result = estateFfbCosts(fixture(null,null,null,null))!!
        assertTrue(result.groups.all { it.actualYtdRmPerTonne == null && it.budgetYearRmPerTonne == null })
    }
    @Test fun invalidRatesStayPendingAndMeasuredZeroRemainsValid() {
        for (value in listOf(null,"-1","NaN","Infinity","Pending")) {
            val result = estateFfbCosts(fixture(value,value))!!
            assertNull(result.groups[0].actualYtdRmPerTonne); assertNull(result.groups[0].budgetYearRmPerTonne)
        }
        assertEquals(0.0,estateFfbCosts(fixture("0","0"))!!.groups[0].actualYtdRmPerTonne!!,0.0)
        assertNull(estateFfbCosts(fixture(year = "2027")))
        assertNull(estateFfbCosts(null))
    }
}
