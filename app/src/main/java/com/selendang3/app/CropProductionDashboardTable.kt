package com.selendang3.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.selendang3.app.data.CropTable
import com.selendang3.app.data.CropTableRow

@Composable
fun CropProductionDashboardTable(data: CropTable) {
    var measure by rememberSaveable { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 600.dp
        val identityWidth = if (wide) 130.dp else 80.dp
        val font = if (wide) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(data.note, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Crop (mt)", "Yield (mt/ha)", "Achievement (%)").forEachIndexed { index, name ->
                    FilterChip(selected = measure == index, onClick = { measure = index }, modifier = Modifier.weight(1f),
                        label = { Text(name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) })
                }
            }
            if (data.rows.isEmpty()) Text("Field records pending. Import estate data to populate this table.")
            else Column {
                Row(Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.secondaryContainer), verticalAlignment = Alignment.CenterVertically) {
                    Text("Field\nArea (ha)", Modifier.width(identityWidth), style = font, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    listOf("This Month", "Year to Date", "Full Year").forEach { Text(it, Modifier.weight(1f), style = font, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold) }
                }
                @Composable fun row(record: CropTableRow, total: Boolean = false) {
                    Row(Modifier.fillMaxWidth().height(if (wide) 56.dp else 48.dp)
                        .background(if (total) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant), verticalAlignment = Alignment.CenterVertically) {
                        Text(record.field + "\n" + record.area, Modifier.width(identityWidth), style = font, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                        (0..2).forEach { period ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                if (measure == 2) {
                                    val value = record.values[10 + period]
                                    Text(value + if (value == "Pending") "" else " %", style = font, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("T " + record.values[(if (measure == 0) 0 else 3) + period], style = font)
                                    if (period < 2) {
                                        Text("A " + record.values[(if (measure == 0) 6 else 8) + period], style = font, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                data.rows.forEach { row(it) }
                data.total?.let { row(it, true) }
            }
            Text("T = Target · A = Actual. Choose Crop, Yield or Achievement above. Actuals are sent-to-mill crop. Full Year compares actual YTD with the annual target; no full-year actual is assumed.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
