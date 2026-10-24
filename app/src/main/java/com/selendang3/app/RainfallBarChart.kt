package com.selendang3.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selendang3.app.data.*
import java.util.Locale

@Composable
fun RainfallBarChart(records: List<AnnualRainfall>) {
    val bars = rainfallChartBars(records)
    if (bars.isEmpty()) {
        Text("Rainfall chart pending complete monthly records.", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        return
    }
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    fun color(role: RainfallHighlight): Color = when (role) {
        RainfallHighlight.HIGHEST -> Color(if (dark) 0xFF81C784 else 0xFF2E7D32)
        RainfallHighlight.LOWEST -> Color(if (dark) 0xFFFFB74D else 0xFFDB6405)
        RainfallHighlight.FULL_YEAR -> Color(if (dark) 0xFF90CAF9 else 0xFF1565C0)
        RainfallHighlight.YTD -> Color(if (dark) 0xFFB8BFC7 else 0xFF646D76)
    }
    val max = (bars.maxOfOrNull { it.totalMm } ?: 0.0).coerceAtLeast(1.0)
    val plotHeight = 170.dp
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Rainfall (mm)", style = MaterialTheme.typography.labelMedium)
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(start = 42.dp, top = 24.dp).height(plotHeight), verticalArrangement = Arrangement.SpaceBetween) {
                repeat(5) { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.width(42.dp).padding(top = 24.dp).height(plotHeight), verticalArrangement = Arrangement.SpaceBetween) {
                    (4 downTo 0).forEach { tick -> Text(String.format(Locale.US, if (max < 10) "%.2f" else "%.0f", max * (tick / 4.0)), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                records.sortedBy { it.year }.forEach { row ->
                    val bar = bars.firstOrNull { it.year == row.year }
                    val value = bar?.let { String.format(Locale.US, "%.2f", it.totalMm) } ?: "Pending"
                    val role = bar?.highlight
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {
                        contentDescription = "${row.year}, ${role?.label ?: "Pending"}, $value${if (bar != null) " millimetres" else ""}"
                    }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(194.dp), contentAlignment = Alignment.BottomCenter) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(value, fontSize = 9.sp, maxLines = 1, color = role?.let(::color) ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (role in listOf(RainfallHighlight.HIGHEST, RainfallHighlight.LOWEST)) FontWeight.Bold else FontWeight.Normal)
                                if (bar != null) Box(Modifier.width(24.dp).height(plotHeight * (bar.totalMm / max).toFloat()).background(color(bar.highlight)))
                            }
                        }
                        Text(row.year.toString(), style = MaterialTheme.typography.labelSmall)
                        Text(when (role) { RainfallHighlight.HIGHEST -> "High"; RainfallHighlight.LOWEST -> "Low"; RainfallHighlight.YTD -> "YTD"; else -> "" },
                            style = MaterialTheme.typography.labelSmall, color = role?.let(::color) ?: MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        RainfallHighlight.entries.filter { role -> bars.any { it.highlight == role } }.chunked(2).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.forEach { role ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(10.dp).background(color(role)))
                        Text(role.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

