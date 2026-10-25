package com.selendang3.app.data

import android.util.Xml
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object EstateXlsx {
    private const val MAX_EXPANDED = 8 * 1024 * 1024
    private val sections = listOf("overview", "fields", "ages", "soil", "rainfall", "abw", "roads")
    private val optional = setOf("yph", "cropProductionReport", "cropProductionReportHistory", "manuringReport", "cropBudget")

    fun parse(input: InputStream): String {
        val files = linkedMapOf<String, ByteArray>()
        var expanded = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && !entry.name.startsWith("/") && !entry.name.contains("../")) { "Invalid workbook contents." }
                require(files.size < 64) { "Workbook contains too many parts." }
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = zip.read(buffer)
                    if (n < 0) break
                    expanded += n
                    require(expanded <= MAX_EXPANDED) { "Workbook expands beyond the 8 MB limit." }
                    out.write(buffer, 0, n)
                }
                files[entry.name] = out.toByteArray()
            }
        }
        val shared = files["xl/sharedStrings.xml"]?.let { readShared(it) } ?: emptyList()
        fun sheet(number: Int) = files["xl/worksheets/sheet" + number + ".xml"]?.let { readSheet(it, shared) }
            ?: throw IllegalArgumentException("Workbook is missing sheet " + number + ".")
        val meta = sheet(1).drop(1).associate { it.getOrNull(0).orEmpty().trim() to it.getOrNull(1).orEmpty().trim() }
        val revision = meta["revision"]?.toLongOrNull() ?: 0
        require(revision > 0) { "Enter a positive whole-number revision on the Metadata sheet." }
        val root = JSONObject().put("schemaVersion", 1)
            .put("estateId", meta["estateId"].orEmpty().ifBlank { "local-estate" })
            .put("revision", revision).put("reportingPeriod", meta["reportingPeriod"].orEmpty())
            .put("exportedAt", meta["exportedAt"].orEmpty().ifBlank { java.time.Instant.now().toString() })
        val tables = JSONArray()
        sections.forEachIndexed { index, section ->
            val rows = sheet(index + 2)
            require(rows.isNotEmpty()) { "The " + section + " sheet needs a header row." }
            val columns = rows.first().map(String::trim)
            require(columns.isNotEmpty() && columns.all(String::isNotBlank)) { "The " + section + " sheet has blank headings." }
            val values = JSONArray()
            rows.drop(1).filter { it.any(String::isNotBlank) }.forEach { row ->
                require(row.size <= columns.size) { "The " + section + " sheet has values outside the headings." }
                values.put(JSONArray(columns.indices.map { col ->
                    row.getOrNull(col)?.takeIf(String::isNotEmpty) ?: JSONObject.NULL
                }))
            }
            tables.put(JSONObject().put("id", section).put("columns", JSONArray(columns)).put("rows", values))
        }
        root.put("sections", tables)
        if (files.containsKey("xl/worksheets/sheet9.xml")) {
            sheet(9).drop(1).forEach { row ->
                val key = row.getOrNull(0)?.trim().orEmpty()
                val json = row.getOrNull(1)?.trim().orEmpty()
                if (key in optional && json.isNotBlank()) root.put(key, org.json.JSONTokener(json).nextValue())
            }
        }
        return root.toString()
    }

    fun writeTemplate(output: java.io.OutputStream) {
        val sheets = mutableListOf<List<List<String>>>()
        sheets += listOf(listOf("key", "value"), listOf("estateId", "local-estate"),
            listOf("revision", "1"), listOf("reportingPeriod", "Enter reporting period"),
            listOf("exportedAt", java.time.Instant.now().toString()))
        sections.forEach { sheets += listOf(listOf("Value")) }
        sheets += listOf(listOf("key", "json"), listOf("yph", ""), listOf("cropProductionReport", ""),
            listOf("cropProductionReportHistory", ""), listOf("manuringReport", ""), listOf("cropBudget", ""))
        val names = listOf("Metadata") + sections.map { it.replaceFirstChar { first -> first.uppercase() } } + "Optional JSON"
        ZipOutputStream(output).use { zip ->
            fun add(path: String, text: String) {
                zip.putNextEntry(ZipEntry(path)); zip.write(text.toByteArray(Charsets.UTF_8)); zip.closeEntry()
            }
            val overrides = (1..sheets.size).joinToString("") { i ->
                "<Override PartName=\"/xl/worksheets/sheet" + i + ".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            }
            add("[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" + overrides + "</Types>")
            add("_rels/.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            val sheetTags = names.mapIndexed { i, n -> "<sheet name=\"" + n + "\" sheetId=\"" + (i + 1) + "\" r:id=\"rId" + (i + 1) + "\"/>" }.joinToString("")
            add("xl/workbook.xml", "<?xml version=\"1.0\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>" + sheetTags + "</sheets></workbook>")
            val rels = names.mapIndexed { i, _ -> "<Relationship Id=\"rId" + (i + 1) + "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet" + (i + 1) + ".xml\"/>" }.joinToString("")
            add("xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" + rels + "</Relationships>")
            sheets.forEachIndexed { i, rows ->
                val body = rows.mapIndexed { r, row ->
                    val cells = row.mapIndexed { c, value ->
                        "<c r=\"" + columnName(c) + (r + 1) + "\" t=\"inlineStr\"><is><t>" + escape(value) + "</t></is></c>"
                    }.joinToString("")
                    "<row r=\"" + (r + 1) + "\">" + cells + "</row>"
                }.joinToString("")
                add("xl/worksheets/sheet" + (i + 1) + ".xml", "<?xml version=\"1.0\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>" + body + "</sheetData></worksheet>")
            }
        }
    }

    private fun readShared(bytes: ByteArray): List<String> = xml(bytes) { p ->
        val values = mutableListOf<String>(); val text = StringBuilder()
        var inItem = false; var inText = false
        while (p.eventType != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) { "si" -> { inItem = true; text.setLength(0) }; "t" -> if (inItem) inText = true }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (inItem && inText) text.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) { "t" -> inText = false; "si" -> { values += text.toString(); inItem = false } }
            }; p.next()
        }
        values
    }

    private fun readSheet(bytes: ByteArray, shared: List<String>): List<List<String>> = xml(bytes) { p ->
        val rows = mutableListOf<List<String>>(); var row: MutableMap<Int, String>? = null
        var col = 0; var type = ""; var inValue = false; var value = StringBuilder()
        while (p.eventType != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> row = sortedMapOf()
                    "c" -> { col = columnIndex(p.getAttributeValue(null, "r") ?: "A1"); type = p.getAttributeValue(null, "t") ?: ""; value = StringBuilder() }
                    "v", "t" -> if (row != null) inValue = true
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (inValue) value.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v", "t" -> inValue = false
                    "c" -> { val raw = value.toString(); row?.put(col, if (type == "s") shared.getOrNull(raw.toIntOrNull() ?: -1).orEmpty() else raw) }
                    "row" -> row?.let { cells -> rows += (0..(cells.keys.maxOrNull() ?: -1)).map { cells[it].orEmpty() } }
                }
            }; p.next()
        }
        rows
    }

    private fun <T> xml(data: ByteArray, block: (XmlPullParser) -> T): T {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        p.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        p.setInput(data.inputStream(), StandardCharsets.UTF_8.name()); p.nextTag()
        return block(p)
    }
    private fun columnIndex(ref: String): Int {
        var n = 0
        ref.takeWhile(Char::isLetter).forEach { n = n * 26 + (it.uppercaseChar() - 'A' + 1) }
        require(n in 1..16384) { "Invalid workbook cell reference." }
        return n - 1
    }
    private fun columnName(index: Int): String {
        var n = index + 1; val out = StringBuilder()
        while (n > 0) { val r = (n - 1) % 26; out.insert(0, ('A'.code + r).toChar()); n = (n - 1) / 26 }
        return out.toString()
    }
    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")
}
