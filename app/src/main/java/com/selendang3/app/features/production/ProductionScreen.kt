package com.selendang3.app.features.production

import com.selendang3.app.*

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
    val codes = cropProductionFieldCodes(update)
    val field = selected?.takeIf { it in codes }
    if (field != null) {
        FieldCropReport(update, field, today) { selected = null }
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Local data · Read-only · Available offline", style = MaterialTheme.typography.bodySmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Crop Production Dashboard", style = MaterialTheme.typography.titleLarge)
                CropProductionDashboardTable(cropProductionTable(update, today))
                Text("Yield targets: MTD vs MTD budget, YTD vs YTD budget, and YTD vs annual budget (MT/ha and achievement %). Unavailable values remain Pending.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Field Crop Records", style = MaterialTheme.typography.titleMedium)
        Text("Select a field to view its report. Import records in Settings → Estate Data Management.", style = MaterialTheme.typography.bodySmall)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val count = if (maxWidth >= 600.dp) 4 else 3
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                codes.chunked(count).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.forEach { code ->
                            FilledTonalButton(colors = bookTabButtonColors(), onClick = { selected = code }, modifier = Modifier.weight(1f).heightIn(min = 44.dp)) { Text(code) }
                        }
                        repeat(count - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (codes.isEmpty()) Text("No field reports saved. Import estate data in Settings → Estate Data Management.")
    }
}

@Composable
private fun FieldCropReport(update: EstatePackage?, code: String, today: java.time.LocalDate, back: () -> Unit) {
    val summary = fieldCropSummary(update, today, code)
    val performance = fieldProductionPerformance(update, today, code)
    val monthly = fieldMonthlyProduction(update, today, code)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = back) { Text("‹ All Fields") }
        Text("$code · Crop Production", style = MaterialTheme.typography.titleLarge)
        Text("Local report · Read-only · Available offline", style = MaterialTheme.typography.bodySmall)

        Text(summary?.note ?: cropProductionTable(update, today).note, style = MaterialTheme.typography.bodySmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Field Crop Performance", style = MaterialTheme.typography.titleMedium)
                performance.forEach { row -> Text("${row.label}: ${row.value}") }
                summary?.groups?.filter { it.title in listOf("Field Profile", "Crop Budget and Actual") }?.forEach { group ->
                    Text(group.title, style = MaterialTheme.typography.titleSmall)
                    group.rows.forEach { row -> Text("${row.label}: ${row.value}") }
                    if (group.note.isNotEmpty()) Text(group.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Monthly Crop Production", style = MaterialTheme.typography.titleMedium)
                Text("January–December · Actual MT · Budget MT · Actual MT/ha · Achievement %")
                Text("${monthly.year} · Past months require a complete month-end report. Current month is MTD through its report date.", style = MaterialTheme.typography.bodySmall)
                monthly.rows.forEach { row ->
                    Text(row.period, style = MaterialTheme.typography.titleSmall)
                    Text("Actual: ${row.actualMt} MT · Budget: ${row.budgetMt} MT · Yield: ${row.actualYield} MT/ha · Achievement: ${row.achievement} %")
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("3. Crop History", style = MaterialTheme.typography.titleMedium)
                Text("Five-year yield history (MT/ha), including the current year")
                CropHistoryPanel(update, code)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("4. Harvesting Performance", style = MaterialTheme.typography.titleMedium)
                val groups = fieldHarvestingGroups(summary)
                if (groups.isEmpty()) Text("Harvesting records: Pending")
                groups.forEach { group ->
                    Text(group.title, style = MaterialTheme.typography.titleSmall)
                    group.rows.forEach { row -> Text("${row.label}: ${row.value}") }
                    if (group.note.isNotEmpty()) Text(group.note, style = MaterialTheme.typography.bodySmall)
                }
                if (summary != null && summary.crewNote.isNotEmpty()) Text(summary.crewNote, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Average Bunch Weight is managed under Field Details, not Crop Production.")
    }
}
