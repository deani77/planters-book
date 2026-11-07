package com.selendang3.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selendang3.app.data.*

@Composable
fun FieldDetailsScreen(update: EstatePackage?) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val codes = estateFieldCodes(update)
    val field = selected?.takeIf { it in codes }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (field == null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Field Overview", style = MaterialTheme.typography.titleLarge)
                    Text("Fields available: " + if (update == null) "Pending" else codes.size.toString())
                    Text("Planting, maturity, area, palm population, terrain, soil and average bunch weight")
                    Text("Choose a field for its basic information. Crop and harvesting records are under Crop Production.")
                }
            }
            EstatePillGrid(codes.size, maxColumns = 2) { index ->
                FilledTonalButton(colors = bookTabButtonColors(), onClick = { selected = codes[index] },
                    modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight)) { Text(codes[index]) }
            }
            if (codes.isEmpty()) Text("Import estate records through Settings → Estate Data Management to see field buttons.")
        } else {
            TextButton(onClick = { selected = null }) { Text("‹ All Fields") }
            Text(field, style = MaterialTheme.typography.titleLarge)
            estateFieldTables(update, field).forEach { table ->
                Text(estateSections.firstOrNull { it.route == table.id }?.title ?: table.id,
                    style = MaterialTheme.typography.titleMedium)
                table.rows.forEach { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            table.columns.zip(row).forEach { (label, value) -> Text("$label: ${value ?: "Pending"}") }
                        }
                    }
                }
            }
            Text("Field overview and available ABW records only. Production and upkeep are maintained in their respective tabs.")
        }
    }
}

@Composable
fun UpkeepCultivationScreen(update: EstatePackage?) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val codes = estateFieldCodes(update)
    val field = selected?.takeIf { it in codes }
    val manuring = operationalDashboardCards(update, estateRainfallToday())[3]
    val categories = listOf(
        "Weeding", "Manuring", "Pest & Disease",
        "Water Management", "Road Management", "Others"
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (field == null) {
            Text("Upkeep & Cultivation", style = MaterialTheme.typography.titleLarge)
            Text("Operations Overview", style = MaterialTheme.typography.titleMedium)
            listOf(
                "Weeding" to "Field-based progress: Pending",
                "Manuring" to "Existing estate programme progress: ${manuring.metrics[0].value} % (${manuring.period}). Field-based totals: Pending",
                "Pest & Disease" to "Active cases and treated area: Pending"
            ).forEach { (title, status) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(status)
                    }
                }
            }
            Text("Field Maintenance Records", style = MaterialTheme.typography.titleMedium)
            Text("Select a field to view its six upkeep categories. Missing activity records are marked Pending.")
            EstatePillGrid(codes.size, maxColumns = 2) { index ->
                FilledTonalButton(
                    colors = bookTabButtonColors(),
                    onClick = { selected = codes[index] },
                    modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight)
                ) { Text(codes[index]) }
            }
            if (codes.isEmpty()) Text("Import field information in Settings → Estate Data Management to see field buttons.")
        } else {
            TextButton(onClick = { selected = null }) { Text("‹ All Fields") }
            Text("$field · Upkeep & Cultivation", style = MaterialTheme.typography.titleLarge)
            categories.forEach { category ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(category, style = MaterialTheme.typography.titleMedium)
                        Text("Field activity records: Pending")
                    }
                }
            }
            Text("Field-level upkeep records are not yet available in the local data model. Estate-level manuring figures are not assigned to individual fields.")
        }
    }
}
