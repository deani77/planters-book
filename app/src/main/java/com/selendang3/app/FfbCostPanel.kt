package com.selendang3.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.selendang3.app.data.FfbCostComparison
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FfbCostPanel(data: FfbCostComparison?) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (data == null) {
            Text("Sync the operation and general charges rates to view the cost comparison.")
            return@Column
        }
        Text("Actual YTD · January–" + data.actualPeriod.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)),
            fontWeight = FontWeight.Bold)
        Text("Budget Year " + data.budgetYear + " · annual rate benchmark", style = MaterialTheme.typography.bodySmall)
        Text("RM/tonne · shared scale starts at zero", style = MaterialTheme.typography.bodySmall)
        val maximum = data.groups.flatMap { listOfNotNull(it.actualYtdRmPerTonne, it.budgetYearRmPerTonne) }
            .maxOrNull()?.takeIf { it > 0 } ?: 1.0
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            data.groups.forEach { group ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                        Text(group.label, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CostColumn("Budget", group.budgetYearRmPerTonne, maximum, Color(0xFFD3A21A), Modifier.weight(1f))
                        CostColumn("Actual", group.actualYtdRmPerTonne, maximum, Color(0xFF397FBA), Modifier.weight(1f))
                    }
                }
            }
        }
        Text("Operation Cost: Penjagaan + Membaja + Pungutan. General Charges: Perbelanjaan Am.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CostColumn(label: String, value: Double?, maximum: Double, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(value?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending",
            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.BottomCenter) {
            if (value != null) Box(Modifier.fillMaxWidth(0.8f)
                .height((value / maximum * 144).toFloat().coerceIn(0f, 144f).dp).background(color))
            else Text("Pending", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Center))
        }
        HorizontalDivider()
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
