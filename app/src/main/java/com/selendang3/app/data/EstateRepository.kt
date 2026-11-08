package com.selendang3.app.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Keep the raw snapshot recoverable even if a stored document cannot be parsed. */
data class EstateReadState(val snapshot: EstateSnapshot? = null, val update: EstatePackage? = null,
    val error: String? = null)
fun readEstateSnapshot(snapshot: EstateSnapshot?): EstateReadState {
    if (snapshot == null) return EstateReadState()
    return try { EstateReadState(snapshot, EstatePackages.parse(snapshot.content)) }
    catch (failure: Exception) { EstateReadState(snapshot, error = "Saved estate data could not be read. Review a backup or import a valid estate file. The original data is retained.") }
}

class EstateRepository private constructor(private val db: EstateDatabase) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val state: StateFlow<EstateReadState> = db.estateDao().observeSnapshot()
        .mapLatest { snapshot -> withContext(Dispatchers.Default) { readEstateSnapshot(snapshot) } }
        .catch { failure ->
            if (failure is CancellationException) throw failure
            emit(EstateReadState(error = "Estate storage is unavailable. Retry after reopening the app."))
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), EstateReadState())
    suspend fun previousSnapshot() = db.estateDao().previousSnapshot()
    suspend fun apply(update: EstatePackage, source: String, expectedJson: String?) =
        EstateUpdates(db).apply(update, source, expectedJson, checkExpected = true)
    suspend fun restore(expectedJson: String, backupJson: String) = EstateUpdates(db).restore(expectedJson, backupJson)
    companion object {
        @Volatile private var instance: EstateRepository? = null
        fun get(context: Context): EstateRepository = instance ?: synchronized(this) {
            instance ?: EstateRepository(EstateDatabase.get(context.applicationContext)).also { instance = it }
        }
    }
}
