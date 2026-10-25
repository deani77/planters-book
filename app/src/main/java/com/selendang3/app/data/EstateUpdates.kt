package com.selendang3.app.data

import androidx.room.withTransaction

class EstateUpdates(private val db: EstateDatabase) {
    suspend fun apply(update: EstatePackage, source: String) = db.withTransaction {
        val dao = db.estateDao()
        val previous = dao.currentSnapshot()
        EstatePackages.requireNewer(update.revision, previous?.revision)
        val now = System.currentTimeMillis()
        if (previous != null) dao.putSnapshot(previous.copy(slot = 2))
        dao.putSnapshot(EstateSnapshot(revision = update.revision, content = update.json,
            updatedAt = now, source = source))
    }
}
