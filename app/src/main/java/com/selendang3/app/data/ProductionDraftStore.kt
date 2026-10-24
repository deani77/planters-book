package com.selendang3.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ProductionEntry(val record: ProductionDraft, val baseVersion: Long = 0,
    val mutationId: String = UUID.randomUUID().toString(), val pending: Boolean = true,
    val conflict: ProductionRemote? = null)
data class ProductionRemote(val record: ProductionDraft, val version: Long, val mutationId: String)

fun productionJson(row: ProductionDraft): JSONObject = JSONObject().put("date", row.date).put("field", row.field)
    .put("harvested", row.harvested ?: JSONObject.NULL).put("sentToMill", row.sentToMill ?: JSONObject.NULL)
    .put("backlog", row.backlog ?: JSONObject.NULL).put("hectares", row.hectares ?: JSONObject.NULL)
    .put("harvesters", row.harvesters ?: JSONObject.NULL)
fun productionFromJson(row: JSONObject): ProductionDraft {
    fun number(key: String): Double? = if (row.isNull(key)) null else row.getDouble(key)
    val workers = number("harvesters")
    require(workers == null || (workers.isFinite() && workers in 0.0..1e9 && workers % 1.0 == 0.0))
    return ProductionDraft(row.getString("date"), row.getString("field"), number("harvested"),
        number("sentToMill"), number("backlog"), number("hectares"), workers?.toInt()).also { it.validate() }
}
fun remoteFromJson(row: JSONObject): ProductionRemote = ProductionRemote(productionFromJson(row.getJSONObject("record")),
    row.getLong("version"), row.getString("mutationId")).also { require(it.version > 0) }
fun remoteJson(row: ProductionRemote): JSONObject = JSONObject().put("record", productionJson(row.record))
    .put("version", row.version).put("mutationId", row.mutationId)

/** Merge never discards an offline edit made while the HTTP request was in flight. */
fun mergeProduction(local: List<ProductionEntry>, remote: List<ProductionRemote>, sent: List<ProductionEntry>): List<ProductionEntry> {
    require(remote.map { it.record.key }.distinct().size == remote.size)
    require(local.filter { !it.pending }.all { entry -> remote.any { it.record.key == entry.record.key } }) { "Server is missing previously synced records." }
    val result = local.associateBy { it.record.key }.toMutableMap()
    for (server in remote) {
        val key = server.record.key
        val current = result[key]
        val uploaded = sent.find { it.record.key == key }
        if (current == null || !current.pending) {
            require(current == null || server.version >= current.baseVersion) { "Server production version moved backwards." }
            result[key] = ProductionEntry(server.record, server.version, server.mutationId, false)
        } else if (uploaded != null && server.mutationId == uploaded.mutationId) {
            result[key] = if (current.mutationId == uploaded.mutationId)
                ProductionEntry(server.record, server.version, server.mutationId, false)
            else current.copy(baseVersion = server.version, conflict = null)
        } else if (server.version != current.baseVersion) {
            result[key] = current.copy(conflict = server)
        }
    }
    return result.values.sortedBy { it.record.key }
}

class ProductionDraftStore(context: Context) {
    private val preferences = context.getSharedPreferences("production_drafts_v1", Context.MODE_PRIVATE)
    companion object { private val lock = Any() }
    fun entries(): List<ProductionEntry> = synchronized(lock) {
        val root = JSONObject(preferences.getString("records", null) ?: "{\"version\":2,\"rows\":[]}")
        require(root.getInt("version") in 1..2) { "Unsupported production draft format." }
        val rows = root.getJSONArray("rows")
        val result = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            if (root.getInt("version") == 1) ProductionEntry(productionFromJson(row))
            else ProductionEntry(productionFromJson(row.getJSONObject("record")), row.getLong("baseVersion"),
                row.getString("mutationId"), row.getBoolean("pending"),
                if (row.isNull("conflict")) null else remoteFromJson(row.getJSONObject("conflict")))
        }
        require(result.map { it.record.key }.distinct().size == result.size) { "Duplicate production drafts." }
        result
    }
    fun load(): List<ProductionDraft> = entries().map { it.record }
    private fun persist(rows: List<ProductionEntry>) {
        val values = JSONArray()
        rows.forEach { row -> values.put(JSONObject().put("record",productionJson(row.record))
            .put("baseVersion", row.baseVersion).put("mutationId", row.mutationId).put("pending", row.pending)
            .put("conflict", row.conflict?.let { remoteJson(it) } ?: JSONObject.NULL)) }
        check(preferences.edit().putString("records", JSONObject().put("version",2).put("rows",values).toString()).commit()) { "Could not save production data." }
    }
    fun save(draft: ProductionDraft): List<ProductionDraft> = synchronized(lock) {
        draft.validate()
        val current = entries()
        val previous = current.find { it.record.key == draft.key }
        require(previous?.conflict == null) { "Resolve this record's sync conflict in Settings first." }
        persist((current.filterNot { it.record.key == draft.key } + ProductionEntry(draft, previous?.baseVersion ?: 0)).sortedBy { it.record.key })
        load()
    }
    fun pending(): List<ProductionEntry> = synchronized(lock) {
        // Persist migration and mutation IDs before any network request, so retries are idempotent.
        val rows = entries(); persist(rows); rows.filter { it.pending && it.conflict == null }
    }
    fun applyRemote(remote: List<ProductionRemote>, sent: List<ProductionEntry>) = synchronized(lock) {
        persist(mergeProduction(entries(), remote, sent))
    }
    fun resolve(key: String, keepLocal: Boolean) = synchronized(lock) {
        persist(entries().map { row ->
            if (row.record.key != key || row.conflict == null) row else {
                val server = row.conflict
                if (keepLocal) ProductionEntry(row.record, server.version)
                else ProductionEntry(server.record, server.version, server.mutationId, false)
            }
        })
    }
    fun status(): String {
        val rows = entries()
        return "${rows.count { it.pending && it.conflict == null }} pending upload · ${rows.count { it.conflict != null }} conflicts · ${rows.count { !it.pending }} synced"
    }
}
