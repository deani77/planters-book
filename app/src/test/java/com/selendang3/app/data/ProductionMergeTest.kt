package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class ProductionMergeTest {
    private val row = ProductionDraft("2026-01-01", "PM04D", null, 2.0, null, null, null)
    @Test fun acknowledgementAndRetryDoNotDuplicate() {
        val local = ProductionEntry(row, mutationId = "original-mutation-123")
        val remote = ProductionRemote(row, 1, local.mutationId)
        val synced = mergeProduction(listOf(local), listOf(remote), listOf(local))
        assertEquals(1, synced.size); assertFalse(synced.single().pending)
        assertEquals(synced, mergeProduction(synced, listOf(remote), emptyList()))
    }
    @Test fun editsDuringUploadRemainPendingOnNewBaseVersion() {
        val sent = ProductionEntry(row)
        val edited = sent.copy(record = row.copy(sentToMill = 5.0), mutationId = "new-mutation-12345")
        val result = mergeProduction(listOf(edited), listOf(ProductionRemote(row, 2, sent.mutationId)), listOf(sent)).single()
        assertTrue(result.pending); assertEquals(5.0, result.record.sentToMill!!, 0.0); assertEquals(2L,result.baseVersion)
    }
    @Test fun staleOfflineEditPreservedAsConflict() {
        val local = ProductionEntry(row, 1)
        val remote = ProductionRemote(row.copy(sentToMill = 10.0),2,"server-change-12345")
        val result = mergeProduction(listOf(local),listOf(remote),listOf(local)).single()
        assertEquals(local.record,result.record); assertEquals(remote,result.conflict); assertTrue(result.pending)
    }
    @Test fun downloadsOtherPhoneRecordsWithoutDroppingUnsyncedRows() {
        val local = ProductionEntry(row)
        val remote = ProductionRemote(row.copy(field = "PM05E"),1,"other-device-12345")
        val result = mergeProduction(listOf(local),listOf(remote),emptyList())
        assertEquals(2,result.size); assertEquals(1,result.count { it.pending })
    }
}
