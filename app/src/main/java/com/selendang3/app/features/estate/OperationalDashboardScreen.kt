package com.selendang3.app.features.estate

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.selendang3.app.data.EstatePackage
import com.selendang3.app.data.OperationalCard
import com.selendang3.app.data.OperationalMetric
import com.selendang3.app.data.estateRainfallToday
import com.selendang3.app.data.operationalDashboardCards
import kotlinx.coroutines.delay
import com.selendang3.app.EstateControlHeight
import com.selendang3.app.EstateModuleIcon
import com.selendang3.app.bookAccent

@Composable
fun OperationalDashboardScreen(update: EstatePackage?) {
    val today by produceState(estateRainfallToday()) {
        while (true) { value = estateRainfallToday(); delay(60_000) }
    }
    val cards = operationalDashboardCards(update, today)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OperationalSummaryCard(cards[0], "information", wide = true)
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalSummaryCard(cards[1], "production", Modifier.weight(1f).fillMaxHeight())
            OperationalSummaryCard(cards[2], "worker", Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OperationalSummaryCard(cards[3], "information", Modifier.weight(1f).fillMaxHeight())
            OperationalSummaryCard(cards[4], "upkeep", Modifier.weight(1f).fillMaxHeight())
        }
        OperationalSummaryCard(cards[5], "pest", wide = true)
        OperationalSummaryCard(cards[6], "cost", wide = true)
    }
}

@Composable
private fun OperationalSummaryCard(card: OperationalCard, symbol: String, modifier: Modifier = Modifier, wide: Boolean = false) {
    val accent = bookAccent(card.title)
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) accent.light else accent.start)) {
        Card(modifier.fillMaxWidth().heightIn(min = EstateControlHeight), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(accent.start, accent.end))).padding(14.dp), horizontalArrangement = if (wide) Arrangement.Center else Arrangement.Start,
                    verticalAlignment = Alignment.Top) {
                    EstateModuleIcon(symbol, Modifier.size(18.dp), Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(card.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (card.title == "Crop Production") {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            card.metrics.take(3).forEach { metric ->
                                OperationalMetricValue(metric, Modifier.weight(1f), centered = true, prominent = true)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(card.metrics[3].label, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(card.metrics[3].value + " " + card.metrics[3].unit,
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            card.metrics.forEach { metric ->
                                OperationalMetricValue(metric, Modifier.weight(1f), centered = true,
                                    prominent = card.title == "Estate Cost")
                            }
                        }
                    } else {
                        card.metrics.forEach { OperationalMetricValue(it) }
                    }
                    Text(card.period, modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = if (wide) TextAlign.Center else TextAlign.Start,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun OperationalMetricValue(metric: OperationalMetric, modifier: Modifier = Modifier,
    centered: Boolean = false, prominent: Boolean = false) {
    Column(modifier, horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(metric.label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(metric.value + " " + metric.unit,
            style = if (prominent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (prominent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start)
    }
}
