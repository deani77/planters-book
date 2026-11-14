package com.selendang3.app.data

import android.content.Context
import androidx.room.withTransaction
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow

/**
 * Android entry point for persisted estate records.
 *
 * The schema-v2 snapshot remains the authoritative report source. This façade
 * intentionally does not change import/restore transactions or add competing
 * field/report tables. Callers needing a validated import must continue using
 * the existing import flow until its transaction is moved here separately.
 */
class EstateRepository internal constructor(
    private val dao: EstateDao,
    private val database: EstateDatabase
) {
    fun observeSnapshot(): Flow<EstateSnapshot?> = dao.observeSnapshot()
    fun observeFields(): Flow<List<EstateField>> = dao.observeFields()
    fun observeSyncState(): Flow<SyncState?> = dao.observeSync()

    suspend fun currentSnapshot(): EstateSnapshot? = dao.currentSnapshot()
    suspend fun previousSnapshot(): EstateSnapshot? = dao.previousSnapshot()

    /**
     * Commit only the exact import that was previewed, atomically retaining the
     * previous snapshot. Stale previews and wrong-estate imports are rejected.
     * Existing import UI is not yet migrated to this method.
     */
    suspend fun commitReviewedImport(
        reviewed: EstateImport,
        source: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): EstateSnapshot = database.withTransaction {
        require(source.isNotBlank()) { "Import source is required." }
        val current = dao.currentSnapshot()
        check(current?.content == reviewed.expectedJson) {
            "Estate data changed after preview. Review the import again."
        }
        val incoming = EstatePackages.parse(reviewed.update.json)
        check(incoming.revision == reviewed.update.revision) {
            "Prepared import revision mismatch."
        }
        if (current != null) {
            val saved = EstatePackages.parse(current.content)
            require(JSONObject(saved.json).getString("estateId") ==
                JSONObject(incoming.json).getString("estateId")) {
                "Import belongs to another estate."
            }
            EstatePackages.requireNewer(incoming.revision, saved.revision)
            dao.putSnapshot(current.copy(slot = 2))
        }
        EstateSnapshot(
            slot = 1,
            revision = incoming.revision,
            content = incoming.json,
            updatedAt = timestampMillis,
            source = source
        ).also { dao.putSnapshot(it) }
    }

    companion object {
        @Volatile private var instance: EstateRepository? = null

        fun get(context: Context): EstateRepository = instance ?: synchronized(this) {
            instance ?: EstateRepository(
                EstateDatabase.get(context.applicationContext).estateDao(),
                EstateDatabase.get(context.applicationContext)
            ).also { instance = it }
        }
    }
}
