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
    val codes = estateFieldCodes(update)
    val field = selected?.takeIf { it in codes }
    if (field != null) {
        FieldCropReport(update, field) { selected = null }
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
private fun FieldCropReport(update: EstatePackage?, code: String, back: () -> Unit) {
    val summary = fieldCropSummary(update, estateRainfallToday(), code)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = back) { Text("‹ All Fields") }
        Text("$code · Crop Production", style = MaterialTheme.typography.titleLarge)
        Text("Local report · Read-only · Available offline", style = MaterialTheme.typography.bodySmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Field Crop Performance", style = MaterialTheme.typography.titleMedium)
                Text("MTD actual vs budget (MT/ha) · Achievement: Pending")
                Text("YTD actual vs YTD budget (MT/ha) · Achievement: Pending")
                Text("YTD actual vs annual budget (MT/ha) · Achievement: Pending")
                Text("Performance comparisons require matching field-level budget and planted-area records.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Monthly Crop Production", style = MaterialTheme.typography.titleMedium)
                Text("January–December · Actual MT · Budget MT · Actual MT/ha · Achievement %")
                Text("Monthly field breakdown: Pending")
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
                val harvesting = summary?.groups?.firstOrNull { it.title == "Harvesting" }
                if (harvesting == null) Text("Harvested area, rounds, frequency, coverage and productivity: Pending")
                else {
                    harvesting.rows.forEach { row -> Text("${row.label}: ${row.value}") }
                    if (summary.crewNote.isNotEmpty()) Text(summary.crewNote, style = MaterialTheme.typography.bodySmall)
                }
                Text("Detailed field harvesting records will be shown when supporting data are available.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Average Bunch Weight is managed under Field Details, not Crop Production.")
    }
}
