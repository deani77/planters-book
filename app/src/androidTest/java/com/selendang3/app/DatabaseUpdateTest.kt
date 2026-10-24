package com.selendang3.app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.selendang3.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseUpdateTest {
    @Test fun rejectedImportPreservesSnapshotAndSyncState() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, EstateDatabase::class.java).build()
        val sections = org.json.JSONArray(EstatePackages.sectionIds.map {
            org.json.JSONObject().put("id", it).put("columns", org.json.JSONArray(listOf("Item")))
                .put("rows", org.json.JSONArray())
        })
        fun update(revision: Int) = EstatePackages.parse(org.json.JSONObject()
            .put("schemaVersion", 1).put("estateId", "selendang-3").put("revision", revision)
            .put("reportingPeriod", "Fictional test")
            .put("exportedAt", "2026-10-20T00:00:00Z").put("sections", sections).toString())
        try {
            val repository = EstateUpdates(db)
            repository.apply(update(1), "File import")
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sync_state").use {
                assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0))
            }
            repository.apply(update(2), "VPS")
            val before = db.estateDao().currentSnapshot()
            try {
                repository.apply(update(1), "File import")
                fail("Old revision was accepted")
            } catch (_: IllegalArgumentException) {}
            assertEquals(before, db.estateDao().currentSnapshot())
            db.openHelper.readableDatabase.query("SELECT revision FROM estate_snapshot WHERE slot = 2").use {
                assertTrue(it.moveToFirst()); assertEquals(1L, it.getLong(0))
            }
            db.openHelper.readableDatabase.query("SELECT lastSuccessEpochMillis FROM sync_state").use {
                assertTrue(it.moveToFirst()); assertTrue(it.getLong(0) > 0)
            }
        } finally { db.close() }
    }

    @Test fun v1DatabaseMigratesWithoutLosingExistingField() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, 0, null).use { sql ->
            sql.execSQL("CREATE TABLE estate_fields (code TEXT NOT NULL PRIMARY KEY, alternateCode TEXT, hectares REAL NOT NULL, palms INTEGER NOT NULL, reportingYear INTEGER NOT NULL, verified INTEGER NOT NULL)")
            sql.execSQL("CREATE TABLE sync_state (id INTEGER NOT NULL PRIMARY KEY, lastSuccessEpochMillis INTEGER)")
            sql.execSQL("INSERT INTO estate_fields VALUES ('DEMO', NULL, 1.0, 1, 2026, 0)")
            sql.version = 1
        }
        val db = Room.databaseBuilder(context, EstateDatabase::class.java, name)
            .addMigrations(EstateDatabase.MIGRATION_1_2).build()
        try {
            assertNull(db.estateDao().currentSnapshot())
            db.openHelper.readableDatabase.query("SELECT code FROM estate_fields").use {
                assertTrue(it.moveToFirst()); assertEquals("DEMO", it.getString(0))
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
