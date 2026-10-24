package com.selendang3.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.selendang3.app.data.*

@Composable
fun ProductionScreen() {
    val context = LocalContext.current
    val dao = remember(context) { EstateDatabase.get(context).estateDao() }
    val snapshot by dao.observeSnapshot().collectAsState(initial = null)
    val update = remember(snapshot?.content) { snapshot?.let { EstatePackages.parse(it.content) } }
    val today by produceState(estateRainfallToday()) {
        while (true) { value = estateRainfallToday(); kotlinx.coroutines.delay(60_000) }
    }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val codes = estateFieldCodes(update)
    val field = selected?.takeIf { it in codes }
    if (field != null) {
        FieldCropReport(update, field) { selected = null }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("VPS data · Read-only · Available offline", style = MaterialTheme.typography.bodySmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Crop Production Dashboard", style = MaterialTheme.typography.titleLarge)
                CropProductionDashboardTable(cropProductionTable(update, today))
            }
        }
        Text("Field Crop Records", style = MaterialTheme.typography.titleMedium)
        Text("Select a field to view its report. Refresh records in Settings → Estate Data Sync.", style = MaterialTheme.typography.bodySmall)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val count = if (maxWidth >= 600.dp) 4 else 3
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                codes.chunked(count).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.forEach { code ->
                            FilledTonalButton(onClick = { selected = code }, modifier = Modifier.weight(1f).heightIn(min = 44.dp)) { Text(code) }
                        }
                        repeat(count - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (codes.isEmpty()) Text("No field reports saved. Download estate data from the VPS in Settings.")
    }
}

@Composable
private fun FieldCropReport(update: EstatePackage?, code: String, back: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = back) { Text("‹ All Fields") }
        Text("$code · Crop Records", style = MaterialTheme.typography.titleLarge)
        Text("VPS report · Read-only\n${update?.reportingPeriod.orEmpty()}", style = MaterialTheme.typography.bodySmall)
        val tables = estateFieldCropTables(update, code)
        if (tables.isEmpty()) Text("Crop records pending for this field.")
        tables.forEach { table ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    table.rows.forEach { row ->
                        table.columns.zip(row).forEach { (label, value) -> Text("$label: ${value ?: "Pending"}") }
                    }
                }
            }
        }
    }
}
