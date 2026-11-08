package com.selendang3.app.data
import org.junit.Assert.*
import org.junit.Test
class EstateReadStateTest {
    @Test fun corruptedSavedDataRemainsRecoverableInsteadOfCrashingScreens() {
        val original = EstateSnapshot(revision = 9, content = "broken JSON", updatedAt = 1, source = "Local file")
        val state = readEstateSnapshot(original)
        assertEquals(original, state.snapshot)
        assertNull(state.update)
        assertNotNull(state.error)
        assertNull(readEstateSnapshot(null).error)
        assertNull(readEstateSnapshot(null).snapshot)
    }
}
