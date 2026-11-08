package com.selendang3.app.data

import org.json.JSONObject
import java.util.Base64

/** Portable, side-effect-free backup validation. Applying it is a separate reviewed operation. */
data class FullBackup(val estateJson: String?, val previousEstateJson: String?, val productionJson: String,
    val settingsJson: String, val mapPdf: ByteArray?) {
    fun encode(): String {
        val root = JSONObject().put("format", "planters-book-full-backup").put("version", 1)
            .put("estate", estateJson?.let(::JSONObject) ?: JSONObject.NULL)
            .put("previousEstate", previousEstateJson?.let(::JSONObject) ?: JSONObject.NULL)
            .put("production", JSONObject(productionJson)).put("settings", JSONObject(settingsJson))
            .put("mapPdfBase64", mapPdf?.let { Base64.getEncoder().encodeToString(it) } ?: JSONObject.NULL)
        return root.toString().also { decode(it) }
    }
    companion object {
        const val MAX_BYTES = 80 * 1024 * 1024
        const val MAX_MAP_BYTES = 50 * 1024 * 1024
        fun decode(text: String): FullBackup {
            require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Full backup exceeds 80 MB." }
            val root = JSONObject(text)
            val keys = setOf("format", "version", "estate", "previousEstate", "production", "settings", "mapPdfBase64")
            require(root.keys().asSequence().toSet() == keys) { "Unknown or missing backup sections." }
            require(root.getString("format") == "planters-book-full-backup" && root.get("version") == 1) { "Unsupported full backup." }
            fun estate(key: String): String? = if (root.isNull(key)) null else root.getJSONObject(key).toString().also { EstatePackages.parse(it) }
            val current = estate("estate")
            val previous = estate("previousEstate")
            if (current != null && previous != null) require(JSONObject(current).getString("estateId") == JSONObject(previous).getString("estateId")) { "Backup estate identifiers differ." }
            val production = root.getJSONObject("production")
            require(production.get("version") == 2) { "Unsupported draft backup version." }
            val rows = production.getJSONArray("rows")
            require(rows.length() <= 10000) { "Too many production drafts." }
            val identifiers = mutableSetOf<String>()
            for (index in 0 until rows.length()) {
                val entry = rows.getJSONObject(index)
                val draft = productionFromJson(entry.getJSONObject("record"))
                require(identifiers.add(draft.key)) { "Duplicate production draft." }
                require(entry.getLong("baseVersion") >= 0 && entry.getString("mutationId").isNotBlank())
                entry.getBoolean("pending")
                if (!entry.isNull("conflict")) {
                    val conflict = remoteFromJson(entry.getJSONObject("conflict"))
                    require(conflict.record.key == draft.key) { "Conflict belongs to another draft." }
                }
            }
            val settings = root.getJSONObject("settings")
            require(settings.toString().toByteArray().size <= 1024 * 1024) { "Settings exceed 1 MB." }
            val map = if (root.isNull("mapPdfBase64")) null else {
                val encoded = root.getString("mapPdfBase64")
                require(encoded.length <= (MAX_MAP_BYTES + 2) / 3 * 4)
                Base64.getDecoder().decode(encoded).also {
                    require(it.size in 5..MAX_MAP_BYTES && it.take(5).toByteArray().toString(Charsets.US_ASCII) == "%PDF-") { "Backup map is not a PDF." }
                }
            }
            return FullBackup(current, previous, production.toString(), settings.toString(), map)
        }
    }
}
