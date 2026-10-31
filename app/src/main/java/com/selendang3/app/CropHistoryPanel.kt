package com.selendang3.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selendang3.app.data.*
import java.time.LocalDate
import java.util.Locale

@Composable
fun CropHistoryPanel(update: EstatePackage?) {
    val history = cropHistoryFields(update)
    val codes = (estateFieldCodes(update) + history.map { it.code }).distinct().sorted()
    var selection by remember { mutableStateOf<String?>(null) }
    val selected = selection?.takeIf { it in codes } ?: codes.firstOrNull()
    val field = history.singleOrNull { it.code == selected }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Crop History · t/ha", style = MaterialTheme.typography.titleMedium)
        EstatePillSelector(codes, selected, { selection = it }) { it.orEmpty() }
        if (field == null) Text("Crop history unavailable. Import records in Settings → Estate Data Management.") else {
            Text("Production cutoff: ${field.cutoff}. Completed years and partial YTD are not directly comparable.")
            val years = field.years(LocalDate.now().year)
            val max = years.mapNotNull { field.yield(it) }.maxOrNull()?.takeIf { it > 0 } ?: 1.0
            years.forEach { year ->
                val value = field.yield(year)
                Text("$year${if (year == field.productionYear) " YTD" else ""}: ${value?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"} t/ha")
                if (value != null) Box(Modifier.fillMaxWidth((value / max).toFloat().coerceIn(0f, 1f)).height(12.dp)
                    .background(if (year == field.productionYear) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary))
            }
            Text("Actual cumulative production: ${field.tonnes ?: "N/A"} tonnes · Planted area: ${field.hectares ?: "N/A"} ha")
            Text("YTD = actual cumulative tonnes ÷ planted ha. ${field.areaSource}")
            if (field.hectares == null) Text("Planted hectares unavailable; YTD yield is N/A.")
        }
    }
}
