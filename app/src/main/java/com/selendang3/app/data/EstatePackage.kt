package com.selendang3.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.time.OffsetDateTime

data class EstateTable(val id: String, val columns: List<String>, val rows: List<List<String?>>)
data class EstatePackage(val revision: Long, val reportingPeriod: String,
    val exportedAt: String, val tables: List<EstateTable>, val json: String, val yph: YphData? = null)

object EstatePackages {
    const val MAX_BYTES = 2 * 1024 * 1024
    val sectionIds = setOf("overview", "fields", "ages", "soil", "rainfall", "abw", "roads")

    fun read(input: InputStream): String {
        val bytes = input.readBytesLimited()
        return Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    }

    private fun InputStream.readBytesLimited(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_BYTES) { "Update file exceeds 2 MB." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    fun parse(text: String): EstatePackage {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Update file exceeds 2 MB." }
        val root = JSONObject(text)
        require(root.opt("schemaVersion") == 1) { "Unsupported estate update format." }
        require(root.opt("estateId") == "selendang-3") { "This update belongs to a different estate." }
        val revisionValue = root.get("revision")
        require(revisionValue is Int || revisionValue is Long) { "Revision must be a whole number." }
        val revision = (revisionValue as Number).toLong()
        require(revision > 0) { "Revision must be positive." }
        fun requiredString(key: String, max: Int): String {
            val value = root.get(key)
            require(value is String && value.isNotBlank() && value.length <= max) { "Invalid $key." }
            return value
        }
        val period = requiredString("reportingPeriod", 100)
        val exported = requiredString("exportedAt", 50)
        OffsetDateTime.parse(exported)
        val sections = root.getJSONArray("sections")
        require(sections.length() == sectionIds.size) { "Include all seven estate sections; use empty rows when pending." }
        var rowCount = 0
        val tables = (0 until sections.length()).map { index ->
            val section = sections.getJSONObject(index)
            val id = section.getString("id")
            require(id in sectionIds) { "Unknown estate section." }
            val rawColumns = section.getJSONArray("columns")
            require(rawColumns.length() in 1..20) { "Each section needs 1–20 columns." }
            val columns = (0 until rawColumns.length()).map {
                val value = rawColumns.get(it)
                require(value is String && value.isNotBlank() && value.length <= 80) { "Invalid column label." }
                value
            }
            require(columns.distinct().size == columns.size) { "Duplicate column labels." }
            val rawRows = section.getJSONArray("rows")
            rowCount += rawRows.length()
            require(rowCount <= 10000) { "Too many rows in update." }
            val rows = (0 until rawRows.length()).map { row ->
                val cells = rawRows.getJSONArray(row)
                require(cells.length() == columns.size) { "Row width does not match columns." }
                (0 until cells.length()).map { col ->
                    when (val cell = cells.get(col)) {
                        JSONObject.NULL -> null
                        is String -> cell.also { require(it.length <= 1000) { "Cell is too long." } }
                        is Number -> cell.toString().also { require(cell.toDouble().isFinite()) }
                        else -> throw IllegalArgumentException("Cells must be text, numbers or null (pending).")
                    }
                }
            }
            EstateTable(id, columns, rows)
        }
        require(tables.map { it.id }.toSet() == sectionIds) { "Duplicate or missing estate sections." }
        val yph = if (root.has("yph") && !root.isNull("yph")) parseYph(root.getJSONObject("yph")) else null
        return EstatePackage(revision, period, exported, tables, root.toString(), yph)
    }

    fun requireNewer(incoming: Long, current: Long?) {
        require(current == null || incoming > current) { "This update is already imported or older than the saved data." }
    }
}
