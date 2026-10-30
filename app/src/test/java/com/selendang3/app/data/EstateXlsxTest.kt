package com.selendang3.app.data

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class EstateXlsxTest {
    @Test fun cropHistoryObjectSurvivesWorkbookImport() {
        val files = template()
        val history = """{"cutoff":"2026-09-30","fields":[{"fieldCode":"EXAMPLE","annual":{"2025":12},"productionYear":2026,"productionTonnes":0,"plantedAreaHa":10,"areaSource":"Fictional"}]}"""
        files["xl/worksheets/sheet9.xml"] = sheet(listOf(listOf("key", "json"), listOf("cropHistory", history)))
        val fields = cropHistoryFields(parsed(files))
        assertEquals(0.0, fields.single().yield(2026)!!, 0.0)
        assertEquals(12.0, fields.single().yield(2025)!!, 0.0)
    }
    @Test fun fieldProfilesArraySurvivesWorkbookImport() {
        val files = template()
        val profiles = """[{"fieldCode":"EXAMPLE","reportYear":2027,"plantedAreaHa":1,"unplantedAreaHa":0,"divisionStandsPerHa":120,"divisionPalmHeightM":0,"maturity":"Example","blocks":[]}]"""
        files["xl/worksheets/sheet9.xml"] = sheet(listOf(listOf("key", "json"), listOf("fieldProfiles", profiles)))
        val saved = JSONObject(parsed(files).json).getJSONArray("fieldProfiles").getJSONObject(0)
        assertEquals("EXAMPLE", saved.getString("fieldCode"))
        assertEquals(0, saved.getInt("divisionPalmHeightM"))
    }

    private fun template(): MutableMap<String, ByteArray> {
        val out = ByteArrayOutputStream()
        EstateXlsx.writeTemplate(out)
        val files = linkedMapOf<String, ByteArray>()
        ZipInputStream(out.toByteArray().inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                files[entry.name] = zip.readBytes()
            }
        }
        return files
    }
    private fun pack(files: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.forEach { (path, bytes) -> zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry() }
        }
        return out.toByteArray()
    }
    private fun sheet(rows: List<List<String>>): ByteArray {
        fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val content = rows.mapIndexed { r, row ->
            "<row>" + row.mapIndexed { c, value ->
                "<c r=\"" + ('A'.code + c).toChar() + (r + 1) + "\" t=\"inlineStr\"><is><t>" + escape(value) + "</t></is></c>"
            }.joinToString("") + "</row>"
        }.joinToString("")
        return ("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>" + content + "</sheetData></worksheet>").toByteArray()
    }
    private fun parsed(files: Map<String, ByteArray>) = EstatePackages.parse(EstateXlsx.readEstate(pack(files).inputStream()))
    private fun rejects(files: Map<String, ByteArray>) {
        try { parsed(files); fail("Invalid workbook was accepted") } catch (_: Exception) {}
    }
    @Test fun generatedTemplateAndGenericFileDetectionRoundTrip() {
        val files = template()
        files["xl/"] = byteArrayOf() // valid ZIP directory entries from other editors
        val update = parsed(files)
        assertEquals(7, update.tables.size)
        assertTrue(update.tables.all { it.rows.isEmpty() })
        assertEquals(update.json, EstateXlsx.readEstate(update.json.byteInputStream()))
    }
    @Test fun followsWorksheetRelationshipsAndKeepsZeroAndMissingCells() {
        val files = template()
        files["xl/worksheets/custom-overview.xml"] = sheet(listOf(listOf("Item", "Value"), listOf("Zero", "0"), listOf("Pending", "")))
        files.remove("xl/worksheets/sheet2.xml")
        val path = "xl/_rels/workbook.xml.rels"
        files[path] = files.getValue(path).toString(Charsets.UTF_8).replace("worksheets/sheet2.xml", "worksheets/custom-overview.xml").toByteArray()
        val rows = parsed(files).tables.first { it.id == "overview" }.rows
        assertEquals(listOf("Zero", "0"), rows[0])
        assertEquals(listOf("Pending", null), rows[1])
    }
    @Test fun readsSharedStringsAndRejectsBrokenReferences() {
        val files = template()
        files["xl/sharedStrings.xml"] = "<sst><si><t>Value</t></si><si><r><t>Estate </t></r><r><t>name</t></r></si></sst>".toByteArray()
        files["xl/worksheets/sheet2.xml"] = "<worksheet><sheetData><row><c r=\"A1\" t=\"s\"><v>0</v></c></row><row><c r=\"A2\" t=\"s\"><v>1</v></c></row></sheetData></worksheet>".toByteArray()
        assertEquals("Estate name", parsed(files).tables.first().rows[0][0])
        files["xl/worksheets/sheet2.xml"] = files.getValue("xl/worksheets/sheet2.xml").toString(Charsets.UTF_8).replace("<v>1</v>", "<v>99</v>").toByteArray()
        rejects(files)
    }
    @Test fun optionalReportsAreKeptAndMalformedDataIsRejected() {
        val files = template()
        val report = "{\"reportDate\":\"2026-10-01\",\"appliedYtdMt\":10}"
        files["xl/worksheets/sheet9.xml"] = sheet(listOf(listOf("key", "json"), listOf("manuringReport", report)))
        assertEquals(10, JSONObject(parsed(files).json).getJSONObject("manuringReport").getInt("appliedYtdMt"))
        files["xl/worksheets/sheet9.xml"] = sheet(listOf(listOf("key", "json"), listOf("manuringReport", "{broken")))
        rejects(files)
    }
    @Test fun rejectReportRoundTripsThroughLocalWorkbook() {
        val files = template()
        val report = """{"reportDate":"2026-10-21","totals":{"rejectMtdMt":0,"rejectYtdMt":null},"fields":[{"field":"Example","rejectMtdMt":0}],"reviewNotes":"Fictional fixture"}"""
        files["xl/worksheets/sheet9.xml"] = sheet(listOf(listOf("key", "json"), listOf("ffbRejectReport", report)))
        val update = parsed(files)
        val saved = JSONObject(update.json).getJSONObject("ffbRejectReport")
        assertEquals(0, saved.getJSONObject("totals").getInt("rejectMtdMt"))
        assertTrue(saved.getJSONObject("totals").isNull("rejectYtdMt"))
        assertEquals("Example", saved.getJSONArray("fields").getJSONObject(0).getString("field"))
        assertEquals("Fictional fixture", saved.getString("reviewNotes"))
        assertEquals(listOf("Reject (mt)", "0.00", "Pending"), estateFfbQuality(update, java.time.LocalDate.of(2026,10,26)).rows[3])
        assertEquals(update.json, EstateXlsx.readEstate(update.json.byteInputStream()))
    }
    @Test fun rejectsUnheadedValuesAndInvalidReferencesWithoutDroppingData() {
        val files = template()
        files["xl/worksheets/sheet2.xml"] = sheet(listOf(listOf("Value"), listOf("A", "Unexpected")))
        rejects(files)
        files["xl/worksheets/sheet2.xml"] = "<worksheet><sheetData><row><c r=\"AAAAAAAAAAAA1\"><v>1</v></c></row></sheetData></worksheet>".toByteArray()
        rejects(files)
        files["xl/worksheets/sheet2.xml"] = "<!DOCTYPE worksheet [<!ENTITY x 'text'>]><worksheet/>".toByteArray()
        rejects(files)
    }
}
