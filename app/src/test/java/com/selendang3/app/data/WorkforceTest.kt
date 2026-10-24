package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class WorkforceTest {
    private fun fixture(local: String? = "7", contractors: String? = null, extra: List<List<String?>> = emptyList()) =
        EstatePackage(1,"Test","",listOf(EstateTable("overview",listOf("Item","Details"),listOf(
            listOf("Workforce reporting period","2026-09"),listOf("Workforce staff","2"),
            listOf("Workforce local workers",local),listOf("Workforce foreign workers","3"),
            listOf("Workforce total workers","10"),listOf("Workforce contract workers",contractors)
        ) + extra)),"")
    @Test fun totalIncludesStaffOnceAndUnknownContractorsRemainPending() {
        val result = estateWorkforce(fixture())!!
        assertNull(result.totalWorkforce)
        assertEquals(9,result.period.monthValue)
        assertNull(result.contractWorkers)
    }
    @Test fun contractorsAreSeparateFromLocalWorkersAndIncludedOnceInTotal() {
        val result = estateWorkforce(fixture(contractors = "4"))!!
        assertEquals(7,result.localWorkers)
        assertEquals(3,result.foreignWorkers)
        assertEquals(16,result.totalWorkforce!!)
        assertEquals(12,estateWorkforce(fixture(contractors = "0"))!!.totalWorkforce!!)
        assertEquals(0,estateWorkforce(fixture(contractors = "0"))!!.contractWorkers!!)
    }
    @Test fun incompleteInvalidOrDuplicateCountsStayPending() {
        for (value in listOf(null,"-1","1.5","8"))
            assertNull(estateWorkforce(fixture(local = value)))
        assertNull(estateWorkforce(fixture(extra = listOf(listOf("Workforce staff","3")))))
        assertNull(estateWorkforce(null))
    }
}
