package com.selendang3.app.data

import android.content.Context
import androidx.room.withTransaction
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Keep the original snapshot available even when its payload cannot be parsed. */
data class EstateReadState(
    val snapshot: EstateSnapshot? = null,
    val update: EstatePackage? = null,
    val error: String? = null
)

fun readEstateSnapshot(snapshot: EstateSnapshot?): EstateReadState {
    if (snapshot == null) return EstateReadState()
    return try {
        EstateReadState(snapshot = snapshot, update = EstatePackages.parse(snapshot.content))
    } catch (failure: Exception) {
        EstateReadState(
            snapshot = snapshot,
            error = "Saved estate data could not be read. Review a backup or import a valid estate file. The original data is retained."
        )
    }
}

/**
 * Android entry point for persisted estate records.
 *
 * The schema-v2 snapshot remains the authoritative report source. This façade
 * does not add competing field/report tables. Existing import/restore controls
 * retain their established transactional path until explicit UI integration.
 */
class EstateRepository internal constructor(
    private val dao: EstateDao,
    private val database: EstateDatabase
) {
    fun observeSnapshot(): Flow<EstateSnapshot?> = dao.observeSnapshot()
    fun observeReadState(): Flow<EstateReadState> = observeSnapshot().map(::readEstateSnapshot)
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
