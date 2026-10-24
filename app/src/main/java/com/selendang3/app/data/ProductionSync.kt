package com.selendang3.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection

object ProductionSync {
    private val active = AtomicBoolean(false)
    fun sync(store: ProductionDraftStore, snapshotAddress: String, token: String): String {
        check(active.compareAndSet(false, true)) { "Production sync is already running." }
        try {
            val url = URL(snapshotAddress.trim())
            require(url.protocol == "https" && url.host.isNotBlank() && url.userInfo == null && url.query == null && url.ref == null &&
                url.path == "/v1/estates/selendang-3/snapshot") { "Use the full trusted HTTPS estate snapshot address." }
            require(token.isNotBlank() && token.none { it == '\r' || it == '\n' }) { "Enter your access token." }
            val endpoint = URL(url.protocol, url.host, url.port, "/v1/estates/selendang-3/production")
            val pending = store.pending()
            val batches = pending.chunked(100).ifEmpty { listOf(emptyList()) }
            for (sent in batches) {
                val changes = JSONArray()
                sent.forEach { row -> changes.put(JSONObject().put("record",productionJson(row.record))
                    .put("baseVersion",row.baseVersion).put("mutationId",row.mutationId)) }
                val connection = endpoint.openConnection() as HttpsURLConnection
                try {
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 15000; connection.readTimeout = 30000
                    connection.requestMethod = if (sent.isEmpty()) "GET" else "POST"
                    connection.setRequestProperty("Authorization", "Bearer ${token.trim()}")
                    connection.setRequestProperty("Accept", "application/json")
                    if (sent.isNotEmpty()) {
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json")
                        val bytes = JSONObject().put("schemaVersion",1).put("estateId","selendang-3")
                            .put("changes",changes).toString().toByteArray(Charsets.UTF_8)
                        connection.setFixedLengthStreamingMode(bytes.size)
                        connection.outputStream.use { it.write(bytes) }
                    }
                    require(connection.responseCode == 200) {
                        if (connection.responseCode == 403) "This token cannot upload production. Ask the administrator for write permission."
                        else "Production sync failed (${connection.responseCode}). Local drafts are retained; retry when online."
                    }
                    val response = connection.inputStream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) { val n = input.read(buffer); if (n < 0) break
                            require(output.size() + n <= 8 * 1024 * 1024) { "Production response too large." }; output.write(buffer,0,n) }
                        JSONObject(output.toString("UTF-8"))
                    }
                    require(response.getInt("schemaVersion") == 1 && response.getString("estateId") == "selendang-3")
                    val rows = response.getJSONArray("records")
                    require(rows.length() <= 10000)
                    val revision = response.getLong("revision")
                    require(revision >= (store.entries().maxOfOrNull { it.baseVersion } ?: 0)) { "Server version moved backwards." }
                    val remote = (0 until rows.length()).map { remoteFromJson(rows.getJSONObject(it)).also { r -> require(r.version <= revision) } }
                    store.applyRemote(remote, sent)
                } finally { connection.disconnect() }
            }
            return store.status()
        } finally { active.set(false) }
    }
}
