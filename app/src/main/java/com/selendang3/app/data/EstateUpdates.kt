package com.selendang3.app.data

import androidx.room.withTransaction
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class EstateUpdates(private val db: EstateDatabase) {
    suspend fun apply(update: EstatePackage, source: String) = db.withTransaction {
        val dao = db.estateDao()
        val previous = dao.currentSnapshot()
        EstatePackages.requireNewer(update.revision, previous?.revision)
        val now = System.currentTimeMillis()
        if (previous != null) dao.putSnapshot(previous.copy(slot = 2))
        dao.putSnapshot(EstateSnapshot(revision = update.revision, content = update.json,
            updatedAt = now, source = source))
        if (source == "VPS") dao.putSync(SyncState(lastSuccessEpochMillis = now))
    }
}

object EstateServer {
    fun download(address: String, accessToken: String): String {
        val url = URL(address.trim())
        require(url.protocol == "https" && url.host.isNotBlank() && url.userInfo == null &&
            url.query == null && url.ref == null) { "Use a full HTTPS update address without credentials or query parameters." }
        require(accessToken.isNotBlank() && accessToken.none { it == '\r' || it == '\n' }) { "Enter your server access token." }
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Authorization", "Bearer ${accessToken.trim()}")
            connection.setRequestProperty("Accept", "application/json")
            require(connection.responseCode == 200) { "Server did not provide an update. Check the address and access token." }
            return connection.inputStream.use { EstatePackages.read(it) }
        } finally { connection.disconnect() }
    }
}
