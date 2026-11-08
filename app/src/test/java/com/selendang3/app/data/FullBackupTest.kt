package com.selendang3.app.data
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class FullBackupTest {
    @Test fun roundTripPreservesSettingsAndExplicitMissingValues() {
        val backup = FullBackup(null, null, "{\"version\":2,\"rows\":[]}", "{\"estateName\":\"Example\",\"extension\":{\"zero\":0}}", "%PDF-example".toByteArray())
        val parsed = FullBackup.decode(backup.encode())
        assertNull(parsed.estateJson)
        assertEquals(0, JSONObject(parsed.settingsJson).getJSONObject("extension").getInt("zero"))
        assertArrayEquals(backup.mapPdf, parsed.mapPdf)
        val altered = JSONObject(backup.encode()).put("version", 2).toString()
        assertThrows(IllegalArgumentException::class.java) { FullBackup.decode(altered) }
        assertThrows(IllegalArgumentException::class.java) { backup.copy(mapPdf = "not PDF".toByteArray()).encode() }
    }
}
