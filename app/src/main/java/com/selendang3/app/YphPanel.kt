package com.selendang3.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.selendang3.app.data.YphData
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun YphPanel(data: YphData?) {
    if (data == null) {
        Text("Yearly yield comparison · t/ha", Modifier.padding(12.dp))
        Text("Sync estate production data to view yearly actual YPH and current-year YTD/budget.", Modifier.padding(12.dp))
        return
    }
    val actualColor = Color(0xFF397FBA)
    val budgetColor = Color(0xFFD3A21A)
    val values = data.displayHistory.map { it.actualYph } + listOf(data.actualYtdYph, data.budgetYtdYph)
    val maximum = values.filterNotNull().maxOrNull()?.takeIf { it > 0 } ?: 1.0
    fun formatted(value: Double?) = value?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"
    val cutoff = Month.of(data.throughMonth).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("YPH · tonnes/hectare (t/ha)", fontWeight = FontWeight.Bold)
        Text("5-year period: ${data.reportingYear - 4}–${data.reportingYear}", style = MaterialTheme.typography.bodySmall)
        Text("Blue: actual · Yellow: budget for the same YTD period", style = MaterialTheme.typography.bodySmall)
        data.displayHistory.forEach { year ->
            YieldBar(year.year.toString(), year.actualYph, maximum, actualColor)
        }
        Text("${data.reportingYear} YTD · Jan–$cutoff", fontWeight = FontWeight.Bold)
        YieldBar("Actual", data.actualYtdYph, maximum, actualColor)
        YieldBar("Budget", data.budgetYtdYph, maximum, budgetColor)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth()) {
            Text("Year / period", Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
            Text("Actual", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Budget", Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }
        data.displayHistory.forEach { year ->
            Row(Modifier.fillMaxWidth()) {
                Text(year.year.toString(), Modifier.weight(1.2f))
                Text(formatted(year.actualYph), Modifier.weight(1f))
                Text("—", Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Text("${data.reportingYear}\nJan–$cutoff YTD", Modifier.weight(1.2f))
            Text(formatted(data.actualYtdYph), Modifier.weight(1f))
            Text(formatted(data.budgetYtdYph), Modifier.weight(1f))
        }
        Text("${data.reportingYear} full-year budget: ${formatted(data.fullYearBudgetYph)} t/ha",
            fontWeight = FontWeight.Bold)
        Text("YTD actual and budget use the same reporting cutoff and mature area. Historical actuals sum the supplied monthly estate YPH; rounding may differ from official annual totals. Missing years remain pending.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun YieldBar(label: String, value: Double?, maximum: Double, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label + ": " + (value?.let { String.format(Locale.US, "%.2f t/ha", it) } ?: "Pending"),
            style = MaterialTheme.typography.bodySmall)
        Box(Modifier.fillMaxWidth().height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (value != null && value > 0) Box(Modifier.fillMaxWidth((value / maximum).toFloat().coerceIn(0f, 1f)).height(12.dp).background(color))
        }
    }
}
