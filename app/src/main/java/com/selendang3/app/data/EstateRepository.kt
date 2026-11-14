package com.selendang3.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Android entry point for persisted estate records.
 *
 * The schema-v2 snapshot remains the authoritative report source. This façade
 * intentionally does not change import/restore transactions or add competing
 * field/report tables. Callers needing a validated import must continue using
 * the existing import flow until its transaction is moved here separately.
 */
class EstateRepository internal constructor(private val dao: EstateDao) {
    fun observeSnapshot(): Flow<EstateSnapshot?> = dao.observeSnapshot()
    fun observeFields(): Flow<List<EstateField>> = dao.observeFields()
    fun observeSyncState(): Flow<SyncState?> = dao.observeSync()

    suspend fun currentSnapshot(): EstateSnapshot? = dao.currentSnapshot()
    suspend fun previousSnapshot(): EstateSnapshot? = dao.previousSnapshot()

    companion object {
        @Volatile private var instance: EstateRepository? = null

        fun get(context: Context): EstateRepository = instance ?: synchronized(this) {
            instance ?: EstateRepository(
                EstateDatabase.get(context.applicationContext).estateDao()
            ).also { instance = it }
        }
    }
}
