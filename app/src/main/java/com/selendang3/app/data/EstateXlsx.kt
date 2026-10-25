package com.selendang3.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import org.xmlpull.v1.XmlPullParserFactory
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object EstateXlsx {
    private const val MAX_EXPANDED = 8 * 1024 * 1024
    private val sections = listOf("overview", "fields", "ages", "soil", "rainfall", "abw", "roads")
    private val optional = setOf("yph", "cropProductionReport", "cropProductionReportHistory", "manuringReport", "cropBudget")

    /** Detect by content because document providers may return a generic MIME type. */
    fun readEstate(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_EXPANDED) { "Estate file exceeds 8 MB." }
            output.write(buffer, 0, count)
        }
        val bytes = output.toByteArray()
        return if (bytes.size >= 4 && bytes[0] == 80.toByte() && bytes[1] == 75.toByte())
            parse(bytes.inputStream()) else EstatePackages.read(bytes.inputStream())
    }

    fun parse(input: InputStream): String {
        val files = linkedMapOf<String, ByteArray>()
        var expanded = 0
        var entries = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++entries <= 128) { "Workbook contains too many parts." }
                require(!entry.name.startsWith("/") && !entry.name.split('/').contains("..") && !entry.name.contains('\\')) { "Invalid workbook contents." }
                if (entry.isDirectory) continue
                require(!files.containsKey(entry.name)) { "Duplicate workbook part." }
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
        fun part(path: String) = files[path] ?: throw IllegalArgumentException("Workbook is missing " + path + ".")
        val relationshipRows = attributes(part("xl/_rels/workbook.xml.rels"), "Relationship")
        require(relationshipRows.all { !it["Id"].isNullOrEmpty() } && relationshipRows.map { it["Id"] }.distinct().size == relationshipRows.size) { "Duplicate or invalid worksheet relationship." }
        val relationships = relationshipRows.associateBy { it["Id"] }
        val sheets = linkedMapOf<String, String>()
        for (entry in attributes(part("xl/workbook.xml"), "sheet")) {
            val name = entry["name"].orEmpty().trim().lowercase(java.util.Locale.ROOT)
            require(name.isNotBlank() && name !in sheets) { "Duplicate or invalid worksheet name." }
            val relationship = relationships[entry["id"]] ?: throw IllegalArgumentException("Missing worksheet relationship.")
            if (name in sections || name == "metadata" || name == "optional json") {
                require(relationship["TargetMode"] != "External" && relationship["Type"].orEmpty().endsWith("/worksheet")) { "Use local worksheet data." }
                val target = relationship["Target"].orEmpty()
                require(target.isNotEmpty() && !target.contains('\\') && !target.split('/').contains("..")) { "Invalid worksheet target." }
                val path = if (target.startsWith("/")) target.drop(1) else "xl/" + target.removePrefix("./")
                require(path.startsWith("xl/")) { "Invalid worksheet target." }
                sheets[name] = path
            }
        }
        val shared = files["xl/sharedStrings.xml"]?.let(::readShared) ?: emptyList()
        fun sheet(name: String) = readSheet(part(sheets[name] ?: throw IllegalArgumentException("Workbook is missing the " + name + " sheet.")), shared)
        val meta = linkedMapOf<String, String>()
        sheet("metadata").drop(1).filter { it.any(String::isNotBlank) }.forEach { row ->
            require(row.size <= 2) { "Metadata requires key and value columns." }
            val key = row.getOrNull(0).orEmpty().trim()
            require(key.isNotEmpty() && key !in meta) { "Duplicate or blank metadata key." }
            meta[key] = row.getOrNull(1).orEmpty().trim()
        }
        val revision = meta["revision"]?.toLongOrNull() ?: 0
        require(revision > 0) { "Enter a positive whole-number revision on the Metadata sheet." }
        val root = JSONObject().put("schemaVersion", 1)
            .put("estateId", meta["estateId"].orEmpty().ifBlank { "local-estate" })
            .put("revision", revision).put("reportingPeriod", meta["reportingPeriod"].orEmpty())
            .put("exportedAt", meta["exportedAt"].orEmpty().ifBlank { java.time.Instant.now().toString() })
        val tables = JSONArray()
        sections.forEach { section ->
            val rows = sheet(section)
            require(rows.isNotEmpty()) { "The " + section + " sheet needs a header row." }
            val columns = rows.first().map(String::trim)
            require(columns.isNotEmpty() && columns.all(String::isNotBlank)) { "The " + section + " sheet has blank headings." }
            val values = JSONArray()
            rows.drop(1).filter { it.any(String::isNotBlank) }.forEach { row ->
                require(row.size <= columns.size) { "The " + section + " sheet has values outside the headings." }
                values.put(JSONArray(columns.indices.map { col -> row.getOrNull(col)?.takeIf(String::isNotEmpty) ?: JSONObject.NULL }))
            }
            tables.put(JSONObject().put("id", section).put("columns", JSONArray(columns)).put("rows", values))
        }
        root.put("sections", tables)
        if ("optional json" in sheets) {
            val seen = mutableSetOf<String>()
            sheet("optional json").drop(1).filter { it.any(String::isNotBlank) }.forEach { row ->
                require(row.size <= 2) { "Optional JSON requires key and JSON columns." }
                val key = row.getOrNull(0).orEmpty().trim()
                val json = row.getOrNull(1)?.trim().orEmpty()
                require(key in optional && seen.add(key)) { "Unknown or duplicate optional report key." }
                if (json.isNotBlank()) {
                    val wrapped = JSONArray("[" + json + "]")
                    require(wrapped.length() == 1) { "Enter one JSON value per optional report." }
                    val value = wrapped.get(0)
                    require(if (key == "cropProductionReportHistory") value is JSONArray else value is JSONObject) { "Invalid optional report shape." }
                    root.put(key, value)
                }
            }
        }
        // Use the same bounds and validation as JSON imports before allowing preview.
        return EstatePackages.parse(root.toString()).json
    }

    private fun attributes(bytes: ByteArray, tag: String): List<Map<String, String>> = xml(bytes) { p ->
        val result = mutableListOf<Map<String, String>>()
        while (p.eventType != XmlPullParser.END_DOCUMENT) {
            if (p.eventType == XmlPullParser.START_TAG && p.name == tag) {
                result += (0 until p.attributeCount).associate { p.getAttributeName(it) to p.getAttributeValue(it) }
            }
            p.next()
        }
        result
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
        val rows = mutableListOf<List<String>>()
        var row: MutableMap<Int, String>? = null
        var col = 0
        var nextCol = 0
        var type = ""
        var formula = false
        var inValue = false
        var value = StringBuilder()
        while (p.eventType != XmlPullParser.END_DOCUMENT) {
            when (p.eventType) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> { row = sortedMapOf(); nextCol = 0 }
                    "c" -> {
                        col = p.getAttributeValue(null, "r")?.let(::columnIndex) ?: nextCol
                        nextCol = col + 1
                        type = p.getAttributeValue(null, "t") ?: ""
                        formula = false
                        value = StringBuilder()
                    }
                    "f" -> formula = true
                    "v", "t" -> if (row != null) inValue = true
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (inValue) value.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v", "t" -> inValue = false
                    "c" -> {
                        val raw = value.toString()
                        require(type != "e" && type != "b" && (!formula || raw.isNotEmpty())) { "Use plain values; the workbook contains an error or an uncalculated formula." }
                        val cell = if (type == "s") {
                            shared.getOrNull(raw.toIntOrNull() ?: -1) ?: throw IllegalArgumentException("Invalid shared string reference.")
                        } else raw
                        require(col < 20 || cell.isEmpty()) { "Estate tables support at most 20 columns." }
                        if (cell.isNotEmpty()) {
                            require(row?.containsKey(col) != true) { "Duplicate cell reference." }
                            row?.put(col, cell)
                        }
                    }
                    "row" -> row?.let { cells ->
                        require(rows.size <= 10000) { "Too many workbook rows." }
                        rows += (0..(cells.keys.maxOrNull() ?: -1)).map { cells[it].orEmpty() }
                    }
                }
            }
            p.next()
        }
        rows
    }

    private fun <T> xml(data: ByteArray, block: (XmlPullParser) -> T): T {
        val text = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(data)).toString()
        require(!text.contains('\u0000') && !text.contains("<!DOCTYPE", ignoreCase = true) && !text.contains("<!ENTITY", ignoreCase = true)) { "Workbook XML declarations are not supported." }
        val p = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        p.setInput(data.inputStream(), null)
        p.nextTag()
        return block(p)
    }
    private fun columnIndex(ref: String): Int {
        val letters = ref.takeWhile { it in 'A'..'Z' }
        val digits = ref.drop(letters.length)
        require(letters.length in 1..3 && digits.isNotEmpty() && digits.all { it in '0'..'9' }) { "Invalid workbook cell reference." }
        var n = 0
        letters.forEach { n = n * 26 + (it - 'A' + 1) }
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

