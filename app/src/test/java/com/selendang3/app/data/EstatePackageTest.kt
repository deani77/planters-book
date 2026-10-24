package com.selendang3.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class EstatePackageTest {
    private fun fixture(): JSONObject = JSONObject().put("schemaVersion", 1)
        .put("estateId", "local-estate").put("revision", 1)
        .put("reportingPeriod", "Fictional example")
        .put("exportedAt", "2026-10-20T00:00:00Z")
        .put("sections", JSONArray(EstatePackages.sectionIds.map { id ->
            JSONObject().put("id", id).put("columns", JSONArray(listOf("Value")))
                .put("rows", JSONArray().put(JSONArray().put(JSONObject.NULL)).put(JSONArray().put(0)))
        }))

    @Test fun missingAndMeasuredZeroRemainDistinct() {
        val update = EstatePackages.parse(fixture().toString())
        assertNull(update.tables.first().rows[0][0])
        assertEquals("0", update.tables.first().rows[1][0])
    }
    @Test(expected = IllegalArgumentException::class)
    fun blankEstateRejected() { EstatePackages.parse(fixture().put("estateId", " ").toString()) }
    @Test fun customEstateAccepted() {
        assertEquals(1L, EstatePackages.parse(fixture().put("estateId", "my-estate").toString()).revision)
    }
    @Test(expected = IllegalArgumentException::class)
    fun futureSchemaRejected() { EstatePackages.parse(fixture().put("schemaVersion", 2).toString()) }
    @Test(expected = IllegalArgumentException::class)
    fun duplicateSectionRejected() {
        val root = fixture()
        root.getJSONArray("sections").getJSONObject(1).put("id", "overview")
        EstatePackages.parse(root.toString())
    }
    @Test(expected = IllegalArgumentException::class)
    fun wrongRowWidthRejected() {
        val root = fixture()
        root.getJSONArray("sections").getJSONObject(0).put("rows", JSONArray().put(JSONArray()))
        EstatePackages.parse(root.toString())
    }
    @Test(expected = IllegalArgumentException::class)
    fun oldRevisionRejected() { EstatePackages.requireNewer(1, 2) }
    @Test(expected = IllegalArgumentException::class)
    fun duplicateRevisionRejected() { EstatePackages.requireNewer(2, 2) }
    @Test(expected = IllegalArgumentException::class)
    fun fractionalRevisionRejected() { EstatePackages.parse(fixture().put("revision", 1.5).toString()) }
    @Test(expected = IllegalArgumentException::class)
    fun oversizedFileRejectedBeforeParse() { EstatePackages.read(ByteArray(EstatePackages.MAX_BYTES + 1).inputStream()) }
}
