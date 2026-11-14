package com.selendang3.app

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

@Composable
fun OperationalDashboardScreen(update: EstatePackage?, compact: Boolean) {
    val today by produceState(estateRainfallToday()) {
        while (true) { value = estateRainfallToday(); delay(60_000) }
    }
    val cards = operationalDashboardCards(update, today)
    val cardGap = if (compact) 6.dp else 10.dp
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(cardGap)) {
        OperationalSummaryCard(cards[0], "information", compact, wide = true)
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(cardGap)) {
            OperationalSummaryCard(cards[1], "production", compact, Modifier.weight(1f).fillMaxHeight())
            OperationalSummaryCard(cards[2], "worker", compact, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(cardGap)) {
            OperationalSummaryCard(cards[3], "information", compact, Modifier.weight(1f).fillMaxHeight())
            OperationalSummaryCard(cards[4], "upkeep", compact, Modifier.weight(1f).fillMaxHeight())
        }
        OperationalSummaryCard(cards[5], "pest", compact, wide = true)
        OperationalSummaryCard(cards[6], "cost", compact, wide = true)
    }
}

@Composable
private fun OperationalSummaryCard(
    card: OperationalCard,
    symbol: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
) {
    val contentGap = if (compact) 4.dp else 6.dp
    val contentPadding = if (compact) 8.dp else 12.dp
    val accent = bookAccent(card.title)
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) accent.light else accent.start)) {
        Card(modifier.fillMaxWidth().heightIn(min = EstateControlHeight), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(verticalArrangement = Arrangement.spacedBy(contentGap)) {
                Row(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(accent.start, accent.end))).padding(if (compact) 8.dp else 14.dp), horizontalArrangement = if (wide) Arrangement.Center else Arrangement.Start,
                    verticalAlignment = Alignment.Top) {
                    EstateModuleIcon(symbol, Modifier.size(18.dp), Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(card.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column(Modifier.padding(horizontal = contentPadding).padding(bottom = contentPadding),
                    verticalArrangement = Arrangement.spacedBy(contentGap)) {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)) {
                            card.metrics.forEach { metric ->
                                OperationalMetricValue(metric, Modifier.weight(1f), centered = true,
                                    prominent = card.title == "Estate Cost")
                            }
                        }
                    } else {
                        card.metrics.forEach { OperationalMetricValue(it) }
                    }
                    Text(card.period, modifier = Modifier.fillMaxWidth().padding(top = if (compact) 0.dp else 3.dp),
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
