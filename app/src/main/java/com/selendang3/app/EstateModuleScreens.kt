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
                    Text("Field Dashboard", style = MaterialTheme.typography.titleLarge)
                    Text("Fields available: " + if (update == null) "Pending" else codes.size.toString())
                    Text("Basic field profiles · area, palms, planting and age records")
                    Text("Choose a field for its basic information. Crop and harvesting records are under Crop Production.")
                }
            }
            codes.chunked(2).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    group.forEach { code ->
                        FilledTonalButton(onClick = { selected = code },
                            modifier = Modifier.weight(1f).heightIn(min = 60.dp)) { Text(code) }
                    }
                    if (group.size == 1) Spacer(Modifier.weight(1f))
                }
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
            Text("Basic records for this field only. Crop, harvesting and productivity records are under Crop Production.")
        }
    }
}

@Composable
fun UpkeepCultivationScreen(update: EstatePackage?) {
    val manuring = operationalDashboardCards(update, estateRainfallToday())[3]
    val categories = listOf(
        "Weeding" to "Estate totals: Pending",
        "Manuring" to "Estate programme progress: ${manuring.metrics[0].value} %",
        "Pest and Disease" to "Estate totals: Pending",
        "Water Management" to "Estate totals: Pending",
        "Road Management" to "Estate totals: Pending",
        "Others" to "Additional estate work: Pending",
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Upkeep and Cultivation Dashboard", style = MaterialTheme.typography.titleLarge)
                Text("Weeding progress: Pending")
                Text("Manuring progress: ${manuring.metrics[0].value} %")
                Text(manuring.period, style = MaterialTheme.typography.bodySmall)
                Text("Pest and Disease: Pending")
                Text("Water Management: Pending")
                Text("Road Management: Pending")
            }
        }
        Text("Major Upkeep Work", style = MaterialTheme.typography.titleLarge)
        categories.forEach { (category, status) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(category, style = MaterialTheme.typography.titleMedium)
                    Text(status)
                }
            }
        }
        Text("Upkeep records are shown at estate level. Entered or imported field profiles are not presented as field-level upkeep activity.")
        Text("Crop production activities are recorded under Crop Production.")
    }
}
