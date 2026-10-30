package com.selendang3.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class EstateZipTest {
    private fun fixture(revision: Int = 1, estate: String = "fictional-estate"): String = JSONObject()
        .put("schemaVersion", 1).put("estateId", estate).put("revision", revision)
        .put("reportingPeriod", "Fictional October 2026").put("exportedAt", "2026-10-27T00:00:00Z")
        .put("sections", JSONArray(EstatePackages.sectionIds.map { id -> JSONObject().put("id", id)
            .put("columns", JSONArray(listOf("Value"))).put("rows", JSONArray().put(JSONArray().put(0)).put(JSONArray().put(JSONObject.NULL))) }))
        .put("manuringReport", JSONObject().put("reportDate", "2026-10-27").put("appliedYtdMt", 7))
        .put("privateExtension", "retained").toString()
    private fun exported(text: String) = ByteArrayOutputStream().also { EstateZip.write(text, it) }.toByteArray()
    private fun unpack(bytes: ByteArray): MutableMap<String, ByteArray> {
        val files = linkedMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip -> while (true) { val entry = zip.nextEntry ?: break; files[entry.name] = zip.readBytes() } }
        return files
    }
    private fun pack(files: Map<String, ByteArray>) = ByteArrayOutputStream().also { output -> ZipOutputStream(output).use { zip ->
        files.forEach { (path, bytes) -> zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry() }
    } }.toByteArray()
    private fun rejected(bytes: ByteArray, current: String? = null) {
        try { EstateZip.read(bytes.inputStream(), current); fail("Invalid import accepted") } catch (_: Exception) { }
    }
    private fun manifest(files: MutableMap<String, ByteArray>, change: (JSONObject) -> Unit) {
        val root = JSONObject(String(files.getValue("manifest.json"), Charsets.UTF_8))
        change(root); files["manifest.json"] = root.toString().toByteArray()
    }
    private fun merge(): ByteArray {
        val root = JSONObject(fixture(2)).apply { remove("privateExtension"); put("manuringReport", JSONObject.NULL) }
        root.getJSONArray("sections").getJSONObject(0).put("rows", JSONArray())
        val files = unpack(exported(root.toString()))
        manifest(files) { it.put("mode", "merge").put("baseRevision", 1).put("sections", JSONArray(listOf("overview"))).put("reports", JSONArray(listOf("manuringReport"))) }
        return pack(files)
    }
    @Test fun exportRoundTripPreservesBytesAndUnknownFields() {
        val text = fixture()
        val result = EstateZip.read(exported(text).inputStream(), null)
        assertEquals(JSONObject(text).toString(), result.update.json)
        assertEquals("retained", JSONObject(result.update.json).getString("privateExtension"))
        assertEquals("0", result.update.tables.first().rows.first().first())
        assertNull(result.update.tables.first().rows.last().first())
        assertNull(result.expectedJson)
    }
    @Test fun plainJSONAndWorkbookRemainSupported() {
        assertEquals(1L, EstateZip.read(fixture().byteInputStream(), null).update.revision)
        val workbook = ByteArrayOutputStream().also { EstateXlsx.writeTemplate(it) }.toByteArray()
        assertEquals(7, EstateZip.read(workbook.inputStream(), null).update.tables.size)
    }
    @Test fun mergePreservesUnselectedDataAndClearsExplicitSelections() {
        val current = fixture()
        val result = EstateZip.read(merge().inputStream(), current)
        assertEquals(current, result.expectedJson)
        assertTrue(result.update.tables.first { it.id == "overview" }.rows.isEmpty())
        assertEquals(2, result.update.tables.first { it.id == "fields" }.rows.size)
        val root = JSONObject(result.update.json)
        assertTrue(root.isNull("manuringReport"))
        assertEquals("retained", root.getString("privateExtension"))
    }
    @Test fun selectedProfileAndHistoryUpdatesPreserveOtherReportsAndAllowExplicitClear() {
        val profile = JSONObject("""{"fieldCode":"EXAMPLE","reportYear":2027,"plantedAreaHa":10,"unplantedAreaHa":0,"divisionStandsPerHa":120,"divisionPalmHeightM":0,"maturity":"Mature","blocks":[]}""")
        val history = JSONObject("""{"cutoff":"2026-09-30","fields":[{"fieldCode":"EXAMPLE","annual":{"2025":12},"productionYear":2026,"productionTonnes":120,"plantedAreaHa":10,"areaSource":"Fictional"}]}""")
        val saved = JSONObject(fixture()).put("fieldProfiles", JSONArray().put(profile)).put("cropHistory", history)
        val incoming = JSONObject(fixture(2)).apply {
            remove("privateExtension"); put("fieldProfiles", JSONArray().put(JSONObject(profile.toString()).put("divisionPalmHeightM", 2)))
            put("cropHistory", JSONObject.NULL)
        }
        fun update(reports: List<String>): EstatePackage {
            val files = unpack(exported(incoming.toString()))
            manifest(files) { it.put("mode", "merge").put("baseRevision", 1).put("sections", JSONArray()).put("reports", JSONArray(reports)) }
            return EstateZip.read(pack(files).inputStream(), saved.toString()).update
        }
        val profilesOnly = update(listOf("fieldProfiles"))
        val root = JSONObject(profilesOnly.json)
        assertEquals(2, root.getJSONArray("fieldProfiles").getJSONObject(0).getInt("divisionPalmHeightM"))
        assertEquals(history.toString(), root.getJSONObject("cropHistory").toString())
        assertEquals("retained", root.getString("privateExtension"))
        assertEquals(12.0, cropHistoryFields(profilesOnly).single().yield(2026)!!, 0.0)
        val cleared = JSONObject(update(listOf("cropHistory")).json)
        assertTrue(cleared.isNull("cropHistory"))
        assertEquals(0, cleared.getJSONArray("fieldProfiles").getJSONObject(0).getInt("divisionPalmHeightM"))
    }
    @Test fun rejectsWrongEstateStaleRevisionAndMergeBase() {
        rejected(exported(fixture()), fixture())
        rejected(exported(fixture(2, "other")), fixture())
        rejected(merge(), fixture(3))
        rejected(merge())
        rejected(merge(), fixture(1, "other"))
    }
    @Test fun rejectsVersionChecksumMetadataModeAndUnsafePaths() {
        val good = exported(fixture())
        for ((key, value) in listOf("version" to 2, "sha256" to "wrong", "estateId" to "other", "revision" to 99, "mode" to "execute", "payload" to "../estate.json")) {
            val files = unpack(good); manifest(files) { it.put(key, value) }; rejected(pack(files))
        }
        for (path in listOf("../escape", "/absolute", "folder/", "nested.zip", "folder\\estate.json")) {
            val files = unpack(good); files[path] = byteArrayOf(1); rejected(pack(files))
        }
        rejected(good.copyOf(good.size / 2))
        rejected(good.copyOf(8))
        rejected(ByteArray(EstateZip.MAX_ARCHIVE + 1))
    }
    @Test fun rejectsExpandedBombAndMalformedPayload() {
        val files = unpack(exported(fixture()))
        files["estate.json"] = ByteArray(EstatePackages.MAX_BYTES + 1) { 65 }
        rejected(pack(files))
        files["manifest.json"] = ByteArray(16385) { 65 }; rejected(pack(files))
        val root = JSONObject(fixture()).put("cropBudget", "invalid")
        try { EstatePackages.parse(root.toString()); fail("Invalid optional report accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun rejectsSymlinkEntriesAndCorruptCrc() {
        val bytes = exported(fixture())
        // Mark the first central-directory file as a Unix symlink.
        val symlink = bytes.copyOf()
        val cursor = (0 until symlink.size - 46).first { i -> symlink[i] == 80.toByte() && symlink[i + 1] == 75.toByte() && symlink[i + 2] == 1.toByte() && symlink[i + 3] == 2.toByte() }
        symlink[cursor + 5] = 3
        symlink[cursor + 40] = 0xff.toByte()
        symlink[cursor + 41] = 0xa1.toByte()
        rejected(symlink)
        // Change the expected CRC in the directory; payload SHA alone must not bypass CRC.
        val corrupt = bytes.copyOf()
        corrupt[cursor + 16] = (corrupt[cursor + 16].toInt() xor 1).toByte()
        rejected(corrupt)
    }
    @Test fun rejectsInvalidMergeSelections() {
        val good = merge()
        val mutations: List<(JSONObject) -> Unit> = listOf(
            { it.put("sections", JSONArray(listOf("fields", "fields"))) },
            { it.put("sections", JSONArray(listOf("unknown"))) },
            { it.put("sections", JSONArray()).put("reports", JSONArray()) },
            { it.put("reports", JSONArray(listOf("cropBudget"))) },
            { it.put("baseRevision", 1.5) }
        )
        for (mutation in mutations) { val files = unpack(good); manifest(files, mutation); rejected(pack(files), fixture()) }
    }
}
