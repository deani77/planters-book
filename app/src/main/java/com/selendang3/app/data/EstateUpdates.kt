package com.selendang3.app.data

import androidx.room.withTransaction

class EstateUpdates(private val db: EstateDatabase) {
    suspend fun apply(update: EstatePackage, source: String, expectedJson: String? = null, checkExpected: Boolean = false) = db.withTransaction {
        val dao = db.estateDao()
        val previous = dao.currentSnapshot()
        require(!checkExpected || previous?.content == expectedJson) { "Saved estate changed after preview. Review the file again." }
        EstateZip.prepare(update.json, previous?.content)
        val now = System.currentTimeMillis()
        if (previous != null) dao.putSnapshot(previous.copy(slot = 2))
        dao.putSnapshot(EstateSnapshot(revision = update.revision, content = update.json,
            updatedAt = now, source = source))
    }

    suspend fun restore(expectedJson: String, backupJson: String) = db.withTransaction {
        val dao = db.estateDao()
        val current = dao.currentSnapshot() ?: error("No saved estate.")
        val backup = dao.previousSnapshot() ?: error("No previous snapshot.")
        require(current.content == expectedJson && backup.content == backupJson) { "Saved data changed. Review again." }
        EstatePackages.parse(backup.content)
        dao.putSnapshot(current.copy(slot = 2))
        dao.putSnapshot(backup.copy(slot = 1, updatedAt = System.currentTimeMillis(), source = "Restored backup"))
    }
}
