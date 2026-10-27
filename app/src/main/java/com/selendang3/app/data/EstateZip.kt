package com.selendang3.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** A prepared result is bound to the exact snapshot reviewed, not just its revision. */
data class EstateImport(val update: EstatePackage, val expectedJson: String?, val summary: String)

object EstateZip {
    const val MAX_ARCHIVE = 8 * 1024 * 1024
    private const val MAX_MANIFEST = 16 * 1024
    val reports = setOf("yph", "cropProductionReport", "cropProductionReportHistory", "manuringReport", "cropBudget", "ffbRejectReport")
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun bounded(input: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            require(out.size() + n <= limit) { "Estate ZIP exceeds its size limit." }
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    // java.util.zip does not expose Unix file types. Check the bounded central directory
    // as well, refusing symlinks, encryption, multi-disk and ZIP64 update containers.
    private fun validateContainer(bytes: ByteArray) {
        fun u16(offset: Int): Int {
            require(offset >= 0 && offset + 2 <= bytes.size) { "Truncated ZIP header." }
            return (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)
        }
        fun u32(offset: Int): Long = u16(offset).toLong() or (u16(offset + 2).toLong() shl 16)
        val end = (bytes.size - 22 downTo maxOf(0, bytes.size - 65557)).firstOrNull {
            u32(it) == 0x06054b50L && it + 22 + u16(it + 20) == bytes.size
        } ?: throw IllegalArgumentException("ZIP central directory is missing.")
        require(u16(end + 4) == 0 && u16(end + 6) == 0 && u16(end + 8) == 2 && u16(end + 10) == 2) { "Use a single-volume ZIP with two entries." }
        val start = u32(end + 16)
        require(start <= end && start + u32(end + 12) == end.toLong()) { "Invalid ZIP central directory." }
        var cursor = start.toInt()
        repeat(2) {
            require(u32(cursor) == 0x02014b50L) { "Invalid ZIP entry header." }
            val flags = u16(cursor + 8)
            val type = (u32(cursor + 38) ushr 16).toInt() and 0xf000
            require(flags and 1 == 0 && u16(cursor + 10) in setOf(0, 8) && type in setOf(0, 0x8000) && u16(cursor + 34) == 0) { "Use regular, unencrypted ZIP files." }
            cursor += 46 + u16(cursor + 28) + u16(cursor + 30) + u16(cursor + 32)
            require(cursor <= end) { "Truncated ZIP directory." }
        }
        require(cursor == end) { "Unexpected ZIP directory contents." }
    }

    fun read(input: InputStream, current: String?): EstateImport {
        val bytes = bounded(input, MAX_ARCHIVE)
        if (bytes.take(2) != listOf(80.toByte(), 75.toByte())) {
            return prepare(EstatePackages.read(bytes.inputStream()), current)
        }
        // ZipFile requires a complete central directory. Never extract archive paths to disk.
        val temporary = java.io.File.createTempFile("estate-import-", ".zip")
        try {
            temporary.writeBytes(bytes)
            ZipFile(temporary).use { zip ->
                val entries = zip.entries().toList()
                if (entries.none { it.name == "manifest.json" }) {
                    require(entries.any { it.name == "xl/workbook.xml" }) { "ZIP requires manifest.json and estate.json." }
                    return prepare(EstateXlsx.readEstate(bytes.inputStream()), current)
                }
                validateContainer(bytes)
                require(entries.size == 2 && entries.map { it.name }.toSet() == setOf("manifest.json", "estate.json")) { "ZIP must contain only manifest.json and estate.json, without folders or duplicate entries." }
                fun part(name: String, limit: Int): ByteArray {
                    val entry = zip.getEntry(name)
                    require(!entry.isDirectory && entry.size in 0..limit.toLong()) { "Invalid or oversized ZIP entry." }
                    val data = zip.getInputStream(entry).use { bounded(it, limit) }
                    require(data.size.toLong() == entry.size && CRC32().apply { update(data) }.value == entry.crc) { "ZIP entry is corrupt." }
                    return data
                }
                val manifest = JSONObject(EstatePackages.read(part("manifest.json", MAX_MANIFEST).inputStream()))
                val payload = part("estate.json", EstatePackages.MAX_BYTES)
                require(manifest.opt("format") == "planters-book-estate" && manifest.opt("version") == 1) { "Unsupported estate ZIP version." }
                require(manifest.opt("payload") == "estate.json" && manifest.opt("sha256") == hash(payload)) { "ZIP payload checksum does not match." }
                val text = EstatePackages.read(payload.inputStream())
                val incoming = EstatePackages.parse(text)
                val root = JSONObject(incoming.json)
                require(manifest.opt("estateId") == root.get("estateId") && manifest.opt("revision") == root.get("revision")) { "Manifest estate/revision does not match payload." }
                return when (manifest.opt("mode")) {
                    "replace" -> {
                        require(manifest.keys().asSequence().toSet() == setOf("format", "version", "payload", "sha256", "estateId", "revision", "mode")) { "Unexpected replacement manifest fields." }
                        prepare(text, current)
                    }
                    "merge" -> {
                        require(manifest.keys().asSequence().toSet() == setOf("format", "version", "payload", "sha256", "estateId", "revision", "mode", "baseRevision", "sections", "reports")) { "Invalid merge manifest fields." }
                        require(current != null) { "Merge needs an existing estate snapshot." }
                        val saved = EstatePackages.parse(current)
                        require(root.getString("estateId") == JSONObject(saved.json).getString("estateId")) { "This file belongs to another estate." }
                        require(manifest.opt("baseRevision") == JSONObject(saved.json).get("revision")) { "Merge base revision does not match saved data." }
                        fun selection(key: String, allowed: Set<String>): Set<String> {
                            val array = manifest.getJSONArray(key)
                            val values = (0 until array.length()).map { array.getString(it) }
                            require(values.distinct().size == values.size && values.all { it in allowed }) { "Invalid merge selection." }
                            return values.toSet()
                        }
                        val sections = selection("sections", EstatePackages.sectionIds)
                        val reportKeys = selection("reports", reports)
                        require(sections.isNotEmpty() || reportKeys.isNotEmpty()) { "Merge has no changes." }
                        // Reject unmapped extension fields rather than silently discard them in a merge.
                        require(root.keys().asSequence().toSet().all { it in reports || it in setOf("schemaVersion", "estateId", "revision", "reportingPeriod", "exportedAt", "sections") }) { "Unknown payload fields require a full replacement." }
                        require(reportKeys.all { root.has(it) }) { "Selected reports must be provided; use null to clear." }
                        val merged = JSONObject(saved.json)
                        val byId = root.getJSONArray("sections").let { a -> (0 until a.length()).associate { a.getJSONObject(it).getString("id") to a.getJSONObject(it) } }
                        val tables = merged.getJSONArray("sections")
                        for (i in 0 until tables.length()) {
                            val id = tables.getJSONObject(i).getString("id")
                            if (id in sections) tables.put(i, byId.getValue(id))
                        }
                        reportKeys.forEach { merged.put(it, root.get(it)) }
                        listOf("revision", "reportingPeriod", "exportedAt").forEach { merged.put(it, root.get(it)) }
                        val prepared = prepare(merged.toString(), current)
                        prepared.copy(summary = "Merge replaces sections: ${sections.joinToString().ifEmpty { "none" }}; reports: ${reportKeys.joinToString().ifEmpty { "none" }}. Other data is preserved. Empty selected tables and null selected reports clear their values. Preview shows the complete result.")
                    }
                    else -> throw IllegalArgumentException("ZIP mode must be replace or merge.")
                }
            }
        } finally { temporary.delete() }
    }

    fun prepare(text: String, current: String?): EstateImport {
        val update = EstatePackages.parse(text)
        if (current != null) {
            val saved = EstatePackages.parse(current)
            require(JSONObject(saved.json).getString("estateId") == JSONObject(update.json).getString("estateId")) { "This file belongs to another estate." }
            EstatePackages.requireNewer(update.revision, saved.revision)
        }
        return EstateImport(update, current, "Full replacement of all estate tables and optional reports. Empty tables and omitted reports clear previous values.")
    }

    fun write(text: String, output: OutputStream) {
        val update = EstatePackages.parse(text)
        val root = JSONObject(update.json)
        val bytes = text.toByteArray(Charsets.UTF_8)
        val manifest = JSONObject().put("format", "planters-book-estate").put("version", 1)
            .put("payload", "estate.json").put("sha256", hash(bytes)).put("mode", "replace")
            .put("estateId", root.getString("estateId")).put("revision", update.revision)
        ZipOutputStream(output).use { zip ->
            for ((name, data) in listOf("manifest.json" to manifest.toString().toByteArray(Charsets.UTF_8), "estate.json" to bytes)) {
                zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry()
            }
        }
    }
}
