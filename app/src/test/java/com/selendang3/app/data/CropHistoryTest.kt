package com.selendang3.app.data
import org.junit.Assert.*
import org.junit.Test
class CropHistoryTest {
    @Test fun yieldUsesActualAreaAndPreservesMissing() {
        val f = CropHistoryField("EXAMPLE", "2026-09-30", 2026, 120.0, 10.0, mapOf(2021 to 9.0, 2022 to null), "Fictional")
        assertEquals(12.0, f.yield(2026)!!, 0.00001)
        assertNull(f.yield(2022))
        assertEquals(9.0, f.yield(2021)!!, 0.00001)
        assertEquals((2022..2026).toList(), f.years(2026).toList())
        assertEquals((2023..2027).toList(), f.years(2027).toList())
        for (area in listOf(null, 0.0, -1.0, Double.NaN)) assertNull(f.copy(hectares = area).yield(2026))
        assertNull(f.copy(tonnes = null).yield(2026))
        assertNull(f.copy(tonnes = -1.0).yield(2026))
    }
}
