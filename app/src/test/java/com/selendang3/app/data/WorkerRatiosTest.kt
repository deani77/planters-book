package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class WorkerRatiosTest {
    private fun fixture(mature: String? = "100", immature: String? = "0", workers: String? = "8") =
        EstatePackage(1,"Test","",listOf(
            EstateTable("overview",listOf("Item","Details"),listOf(
                listOf("Ratios reporting period","2026-09"),listOf("Ratios mature area (ha)",mature),
                listOf("Ratios immature area (ha)",immature),listOf("Ratios mature operations workers",workers),
                listOf("Workforce total workers","20"),listOf("Workforce staff","3"),listOf("Workforce contract workers","2")
            )), EstateTable("fields",listOf("Area (ha)"),listOf(listOf("100")))
        ),"")
    @Test fun usesOnlyAssignedOperationsWorkersAndPreservesZeroImmatureArea() {
        val result = estateWorkerRatios(fixture())!!
        assertEquals(0.0,result.immatureArea,0.0)
        assertEquals(100.0,result.matureArea,0.0)
        assertEquals(8,result.matureOperationsWorkers)
        assertEquals(12.5,result.matureHaPerWorker!!,0.0)
        assertEquals(9,result.period.monthValue)
    }
    @Test fun zeroWorkersNeverProducesInfiniteRatio() {
        assertNull(estateWorkerRatios(fixture(workers = "0"))!!.matureHaPerWorker)
    }
    @Test fun missingInvalidOrMismatchedSourceStaysPending() {
        for (area in listOf(null,"-1","NaN","Infinity","90"))
            assertNull(estateWorkerRatios(fixture(mature = area)))
        for (workers in listOf(null,"-1","1.5"))
            assertNull(estateWorkerRatios(fixture(workers = workers)))
        assertNull(estateWorkerRatios(null))
    }
}
