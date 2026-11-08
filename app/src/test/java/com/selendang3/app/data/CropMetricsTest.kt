package com.selendang3.app.data
import org.junit.Assert.*
import org.junit.Test
class CropMetricsTest {
    @Test fun namedResultsRetainPrecisionAndAnnualComparisonUsesYtd() {
        val metrics = CropMetrics(3.0, 10.0, 20.0, 40.0, 5.0, 30.0)
        assertEquals(5.0 / 3.0, metrics.actual(CropPeriod.MTD, true)!!, 1e-12)
        assertEquals(75.0, metrics.achievement(CropPeriod.ANNUAL)!!, 0.0)
        assertNull(metrics.actual(CropPeriod.ANNUAL))
        assertNull(metrics.copy(areaHa = 0.0).actual(CropPeriod.MTD, true))
        assertNull(metrics.copy(targetMtdMt = 0.0).achievement(CropPeriod.MTD))
        assertEquals("1.67", CropTableRow("A", metrics).values[8])
    }
}
