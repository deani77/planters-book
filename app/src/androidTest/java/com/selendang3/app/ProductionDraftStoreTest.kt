package com.selendang3.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.selendang3.app.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionDraftStoreTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun clear() { context.getSharedPreferences("production_drafts_v1", 0).edit().clear().commit() }
    @After fun cleanup() { clear() }
    @Test fun savedDraftSurvivesNewStoreAndEditingDoesNotDuplicate() {
        val store = ProductionDraftStore(context)
        val row = ProductionDraft("2026-10-20", "PM04D", null, 2.0, null, 3.0, 4)
        store.save(row)
        store.save(row.copy(sentToMill = 5.0))
        store.save(row.copy(field = "PM05E"))
        val reloaded = ProductionDraftStore(context).load()
        assertEquals(2, reloaded.size)
        assertEquals(5.0, reloaded.first { it.field == "PM04D" }.sentToMill!!, 0.0001)
        assertNull(reloaded.first().harvested)
    }
    @Test fun unreadableSavedDataIsNotOverwritten() {
        val preferences = context.getSharedPreferences("production_drafts_v1", 0)
        preferences.edit().putString("records", "unreadable").commit()
        try {
            ProductionDraftStore(context).save(ProductionDraft("2026-10-20", "PM04D", null, 2.0, null, null, null))
            fail("Unreadable data must prevent saving")
        } catch (_: org.json.JSONException) { }
        assertEquals("unreadable", preferences.getString("records", null))
    }
}
