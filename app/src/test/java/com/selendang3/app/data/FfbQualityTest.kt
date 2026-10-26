package com.selendang3.app.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FfbQualityTest {
    private val today = LocalDate.of(2026, 10, 26)
    private fun fixture(month: Any = 120.5, year: Any = 999): EstatePackage {
        val totals = JSONObject().put("rejectMtdMt",month).put("rejectMtdPercent",1.25)
            .put("rejectYtdMt",year).put("rejectYtdPercent",2.5)
        val root = JSONObject().put("ffbRejectReport", JSONObject().put("reportDate","2026-10-21").put("totals",totals))
        return EstatePackage(1,"Example","",emptyList(),root.toString())
    }
    @Test fun usesTonnesNotPercentagesIncludingMassOverOneHundred() {
        val quality = estateFfbQuality(fixture(),today)
        assertEquals(listOf("Reject (mt)","120.50","999.00"),quality.rows[3])
        assertTrue(quality.rows.take(3).all { it[0].endsWith("(%)") && it[1] == "Pending" && it[2] == "Pending" })
        assertEquals(listOf("Reject (mt)","Pending","999.00"),estateFfbQuality(fixture(),LocalDate.of(2026,11,1)).rows[3])
        assertEquals("Awaiting approved FFB quality records.",estateFfbQuality(fixture(),LocalDate.of(2026,8,1)).note)
    }
    @Test fun preservesZeroAndRejectsInvalidMissingOrStaleMass() {
        assertEquals(listOf("Reject (mt)","0.00","0.00"),estateFfbQuality(fixture(0,0),today).rows[3])
        assertEquals("Pending",estateFfbQuality(fixture(),LocalDate.of(2027,1,1)).rows[3][2])
        assertEquals(listOf("Reject (mt)","Pending","Pending"),estateFfbQuality(fixture(-1,JSONObject.NULL),today).rows[3])
    }
}
