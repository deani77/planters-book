package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class RainfallValueTest {
    @Test fun pendingMonthIsDifferentFromMeasuredZero() {
        assertTrue(RainfallValue(null, null).pending)
        assertTrue(RainfallValue(20.0, null).pending)
        assertFalse(RainfallValue(0.0, 0).pending)
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalidRainfallIsRejected() { RainfallValue(Double.NaN, 2) }
    @Test(expected = IllegalArgumentException::class)
    fun invalidDayCountIsRejected() { RainfallValue(10.0, 32) }
}
