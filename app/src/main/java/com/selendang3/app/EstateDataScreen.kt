package com.selendang3.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.selendang3.app.data.*
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.Date

@Composable
fun EstateDataControls() {
    val context = LocalContext.current
    val db = remember(context) { EstateDatabase.get(context) }
    val snapshot by remember(db) { db.estateDao().observeSnapshot() }.collectAsStateWithLifecycle(null)
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<EstatePackage?>(null) }
    var source by remember { mutableStateOf("Local file") }
    val scope = rememberCoroutineScope()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = snapshot?.content
        if (uri != null && content != null) {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri, "wt")
                            ?: throw IllegalArgumentException("Could not create export file.")
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
                    }
                    message = "Estate data exported. Store this file privately."
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { message = "Export failed. Saved data is unchanged."
                } finally { busy = false }
            }
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            message = null
            scope.launch {
                try {
                    val update = withContext(Dispatchers.IO) {
                        val input = context.contentResolver.openInputStream(uri)
                            ?: throw IllegalArgumentException("Could not open the selected file.")
                        input.use { EstatePackages.parse(EstatePackages.read(it)) }
                    }
                    EstatePackages.requireNewer(update.revision, snapshot?.revision)
                    source = "Local file"
                    preview = update
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (failure: Exception) {
                    message = failure.message ?: "Import failed. Saved data is unchanged."
                } finally { busy = false }
            }
        }
    }
    Text("Local Estate Data", style = MaterialTheme.typography.titleLarge)
    Text(snapshot?.let { "Saved revision ${it.revision} · ${it.source}\nUpdated: ${DateFormat.getDateTimeInstance().format(Date(it.updatedAt))}" }
        ?: "No estate data imported yet.")
    Text("Import an estate JSON file from this device. Data stays on this device and works offline.",
        style = MaterialTheme.typography.bodySmall)
    Button(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Import Estate File") }
    OutlinedButton(onClick = { exporter.launch("Planters-Book-estate-r${snapshot?.revision}.json") },
        enabled = snapshot != null && !busy, modifier = Modifier.fillMaxWidth()) { Text("Export Estate Data") }
    if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Processing local data…") }
    message?.let { Text(it) }

    preview?.let { update ->
        AlertDialog(onDismissRequest = { if (!busy) preview = null },
            title = { Text("Review estate update") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text("$source · revision ${update.revision}\n${update.reportingPeriod}\nExported: ${update.exportedAt}")
                    Text("${update.tables.sumOf { it.rows.size }} rows across seven sections. This replaces all currently saved sections. Check the source and reporting period.")
                    update.tables.forEach { table -> Text("${estateSections.first { it.route == table.id }.title}: ${table.rows.size} rows") }
                    Text("An empty section will be shown as awaiting data. Missing values remain Pending.")
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { EstateUpdates(db).apply(update, source) }
                        message = "Estate data updated successfully from $source."
                        preview = null
                    } catch (cancelled: CancellationException) { throw cancelled
                    } catch (failure: Exception) {
                        message = if (failure is IllegalArgumentException) failure.message else "Could not save update. Existing data is unchanged."
                        preview = null
                    } finally { busy = false }
                }
            }) { Text("Import update") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { preview = null }) { Text("Cancel") } })
    }
}

@Composable
fun EstateSectionData(section: Section, update: EstatePackage?) {
    val table = update?.tables?.firstOrNull { it.id == section.route }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { Text(section.description, color = MaterialTheme.colorScheme.onSurface) }
        item { Text(update?.let { "${it.reportingPeriod} · revision ${it.revision}" } ?: "No estate data imported yet.", color = MaterialTheme.colorScheme.onSurface) }
        if (table == null || table.rows.isEmpty()) {
            item { Card(Modifier.fillMaxWidth()) { Text("Awaiting verified records. Import estate data in Settings.", Modifier.padding(16.dp)) } }
        } else {
            items(table.rows) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        table.columns.zip(row).forEach { (label, value) -> Text("$label: ${value ?: "Pending"}") }
                    }
                }
            }
        }
    }
}
