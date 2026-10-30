package com.selendang3.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.selendang3.app.data.AnnualRainfall
import com.selendang3.app.data.estateAnnualRainfall
import com.selendang3.app.data.estateRainfallToday
import java.time.Month
import java.time.format.TextStyle
import com.selendang3.app.data.EstateAverageSph
import com.selendang3.app.data.estateAverageSph
import com.selendang3.app.data.EstateSoilSeries
import com.selendang3.app.data.estateSoilSeries
import com.selendang3.app.data.EstateWorkforce
import com.selendang3.app.data.estateWorkforce
import java.time.format.DateTimeFormatter
import com.selendang3.app.data.EstateWorkerRatios
import com.selendang3.app.data.estateWorkerRatios
import com.selendang3.app.data.FfbCostComparison
import com.selendang3.app.data.estateFfbCosts
import com.selendang3.app.data.estateFfbQuality
import com.selendang3.app.data.FfbQuality
import com.selendang3.app.data.estateAreaSummary
import com.selendang3.app.data.EstatePackage
import com.selendang3.app.data.EstateAgeProfile
import com.selendang3.app.data.EstateTopography
import com.selendang3.app.data.YphData
import com.selendang3.app.data.estateTopography
import com.selendang3.app.data.estateAgeProfile
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val Pending = "—"

/** Estate metrics are calculated from the user’s imported records. */
@Composable
fun EstateDashboard(estateUpdate: EstatePackage? = null) {
    val estateName = rememberEstateName()
    val ageProfile = remember(estateUpdate) { estateAgeProfile(estateUpdate) }
    val topography = remember(estateUpdate) { estateTopography(estateUpdate) }
    val averageSph = remember(estateUpdate) { estateAverageSph(estateUpdate) }
    val soilSeries = remember(estateUpdate) { estateSoilSeries(estateUpdate) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val today by produceState(estateRainfallToday(), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { value = estateRainfallToday(); delay(60_000) }
        }
    }
    val quality = remember(estateUpdate, today) { estateFfbQuality(estateUpdate, today) }
    val estateArea = remember(estateUpdate, today) { estateAreaSummary(estateUpdate, today) }
    val rainfall = remember(estateUpdate, today) { estateAnnualRainfall(estateUpdate, today) }
    val workforce = remember(estateUpdate) { estateWorkforce(estateUpdate) }
    val workerRatios = remember(estateUpdate) { estateWorkerRatios(estateUpdate) }
    val ffbCost = remember(estateUpdate) { estateFfbCosts(estateUpdate) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
            BoxWithConstraints {
                val wide = maxWidth >= 840.dp
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("ESTATE DETAILS · $estateName", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge)
                    DashboardPanel("Estate Area") {
                        DashboardTable(listOf("Measure", "Area (ha)"), listOf(
                            listOf("Perimeter Hectare", estateArea?.let { String.format(Locale.US, "%,.2f", it.perimeterHa).trim() } ?: Pending),
                            listOf("Mature Hectare", estateArea?.let { String.format(Locale.US, "%,.2f", it.matureHa).trim() } ?: Pending)
                        ), minimumWidth = 320)
                        Text(estateArea?.let { "Reporting period: ${it.period}" } ?: "Awaiting verified estate area records.",
                            Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(estateUpdate?.reportingPeriod ?: "Reporting period: pending", style = MaterialTheme.typography.bodyMedium)
                    Text("Age Profile, Topography, YPH, Average SPH, Soil Type, Rainfall, Workforce, Ratios and FFB cost use locally saved estate records. Other panels may still be pending. — means unavailable.",
                        style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ProfilePanels(ageProfile, topography, estateUpdate?.yph, averageSph, soilSeries)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                PeopleAndRainfallPanels(rainfall, workforce, workerRatios)
                            }
                            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ProductionPanels(ffbCost, quality)
                            }
                        }
                    } else {
                        ProfilePanels(ageProfile, topography, estateUpdate?.yph, averageSph, soilSeries)
                        PeopleAndRainfallPanels(rainfall, workforce, workerRatios)
                        ProductionPanels(ffbCost, quality)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun ProfilePanels(ageProfile: EstateAgeProfile?, topography: EstateTopography?, yph: YphData?, averageSph: EstateAverageSph?, soilSeries: EstateSoilSeries?) {
    DashboardPanel("Age Profile") {
        if (ageProfile == null) {
            ChartPending("Palm age distribution")
            Text("Complete field ages and areas are required.", Modifier.padding(12.dp))
            DashboardTable(listOf("Age category", "Area (ha)", "Share"), listOf(
                listOf("Young · 1–5 years", Pending, Pending),
                listOf("Prime · 6–23 years", Pending, Pending),
                listOf("Old · >23 years", Pending, Pending),
            ))
            MetricRow("Total area (ha)")
        } else {
            val colors = listOf(Color(0xFF49ACC3), Color(0xFF4B8C42), Color(0xFFA46436))
            Canvas(Modifier.size(160.dp).align(Alignment.CenterHorizontally).padding(12.dp)) {
                var angle = -90f
                ageProfile.bands.forEachIndexed { index, band ->
                    val sweep = (band.area / ageProfile.totalArea * 360).toFloat()
                    if (sweep > 0f) drawArc(colors[index], angle, sweep, true)
                    angle += sweep
                }
            }
            DashboardTable(listOf("Age category", "Area (ha)", "Share"),
                ageProfile.bands.map { band -> listOf(band.label,
                    String.format(Locale.US, "%,.2f", band.area),
                    String.format(Locale.US, "%.2f%%", band.area / ageProfile.totalArea * 100)) })
            Text("Total area: " + String.format(Locale.US, "%,.2f ha", ageProfile.totalArea),
                Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            Text("Area-weighted from supplied field ages.",
                Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
    DashboardPanel("Topography") {
        if (topography == null) {
            ChartPending("Terrain distribution")
            DashboardTable(listOf("Terrain", "Area (ha)", "Share"),
                listOf("Flat", "Hilly", "Undulating").map { listOf(it, Pending, Pending) })
            Text("Import complete terrain records using the current estate area.",
                Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        } else {
            val colors = listOf(Color(0xFF49ACC3), Color(0xFFA46436), Color(0xFF4B8C42))
            Canvas(Modifier.size(160.dp).align(Alignment.CenterHorizontally).padding(12.dp)) {
                var angle = -90f
                topography.bands.forEachIndexed { index, band ->
                    val sweep = (band.share * 3.6).toFloat()
                    if (sweep > 0f) drawArc(colors[index], angle, sweep, true)
                    angle += sweep
                }
            }
            DashboardTable(listOf("Terrain", "Area (ha)", "Share"),
                topography.bands.map { band -> listOf(band.label,
                    String.format(Locale.US, "%,.2f", band.area),
                    String.format(Locale.US, "%.2f%%", band.share)) })
            Text("Total area: " + String.format(Locale.US, "%,.2f ha", topography.totalArea),
                Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            Text("Source percentages applied to current field area. Rounded hectares reconcile to the total.",
                Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
    DashboardPanel("YPH") {
        YphPanel(yph)
    }
    DashboardPanel("Average SPH") {
        if (averageSph == null) {
            MetricRow("Palms/ha")
            Text("Complete field palm counts and areas are required.", Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall)
        } else {
            Text(String.format(Locale.US, "%,.2f palms/ha", averageSph.palmsPerHa),
                Modifier.padding(12.dp), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            DashboardTable(listOf("Measure", "Total"), listOf(
                listOf("Oil palm trees · all fields", String.format(Locale.US, "%,d", averageSph.totalPalms)),
                listOf("Estate area (ha)", String.format(Locale.US, "%,.2f", averageSph.totalArea)),
            ))
            Text("Total oil palm trees ÷ total estate hectares.",
                Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
    DashboardPanel("Soil Type") {
        if (soilSeries == null) {
            Text("Import soil series records using the current estate area.", Modifier.padding(12.dp))
        } else {
            DashboardTable(listOf("Soil series", "Area (ha)", "Share", "Reference class"),
                soilSeries.bands.map { listOf(it.name, String.format(Locale.US, "%,.2f", it.area),
                    String.format(Locale.US, "%.2f%%", it.share), it.suitability) }, minimumWidth = 440)
            Text("Total area: " + String.format(Locale.US, "%,.2f ha", soilSeries.totalArea),
                Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            Text("* Agricultural suitability Class I–V. Published reference ratings; estate verification pending.",
                Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            soilSeries.bands.mapNotNull { it.qualification }.distinct().forEach {
                Text(it, Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall)
            }
            soilSeries.bands.mapNotNull { it.reference }.distinct().forEach {
                Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PeopleAndRainfallPanels(rainfall: List<AnnualRainfall>, workforce: EstateWorkforce?, workerRatios: EstateWorkerRatios?) {
    DashboardPanel("Rainfall") {
        RainfallBarChart(rainfall)
        DashboardTable(listOf("Year", "Period", "Rainfall (mm)"), rainfall.mapIndexed { index, row ->
            val period = if (index > 0) "Full year" else if (row.throughMonth == 0) "YTD · no completed month"
                else "YTD through " + Month.of(row.throughMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            listOf(row.year.toString(), period,
                row.totalMm?.let { String.format(Locale.US, "%,.2f", it) } ?: "Pending")
        })
        Text("Five years; table newest first. Current-year YTD ends at the last completed month (Malaysia time). High/low colours compare completed full years. Tied extremes share a colour; equal totals have no distinct extreme. Missing data remains Pending.",
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
    }
    DashboardPanel("Workforce") {
        Text(workforce?.period?.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH))
            ?: "Reporting month: Pending", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        DashboardTable(listOf("Category", "People"), listOf(
            listOf("Staff", workforce?.staff?.toString() ?: "Pending"),
            listOf("Local Workers", workforce?.localWorkers?.toString() ?: "Pending"),
            listOf("Foreign Workers", workforce?.foreignWorkers?.toString() ?: "Pending"),
            listOf("Contract Workers", workforce?.contractWorkers?.toString() ?: "Pending"),
            listOf("Total Workforce", workforce?.totalWorkforce?.toString() ?: "Pending"),
        ))
    }
    DashboardPanel("Ratios") {
        DashboardTable(listOf("Maturity", "Area (ha)", "Workers:ha"), listOf(
            listOf("Immature", workerRatios?.immatureArea?.let { String.format(Locale.US, "%,.2f", it) } ?: "Pending",
                if (workerRatios?.immatureArea == 0.0) Pending else "Pending"),
            listOf("Mature", workerRatios?.matureArea?.let { String.format(Locale.US, "%,.2f", it) } ?: "Pending",
                if (workerRatios?.matureArea == 0.0) Pending else
                    workerRatios?.matureHaPerWorker?.let { String.format(Locale.US, "1:%.2f", it) } ?: "Pending"),
        ))
        workerRatios?.let {
            Text("Based on " + it.matureOperationsWorkers + " mature-area operations workers only · " +
                it.period.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)),
                Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProductionPanels(ffbCost: FfbCostComparison?, quality: FfbQuality) {
    DashboardPanel("Kos FFB/TAN") { FfbCostPanel(ffbCost) }
    DashboardPanel("FFB Quality") {
        DashboardTable(listOf("Category", "This Month", "YTD"), quality.rows)
        Text(quality.note, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
    DashboardPanel("OER (%)") {
        DashboardTable(listOf("Period", "KPG (%)", "KPA (%)"), listOf(listOf("YTD", Pending, Pending)), minimumWidth = 320)
        Text("KPG = actual OER; KPA = budget OER. Awaiting approved OER records.", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
    DashboardPanel("Oil Yield (t/ha)") {
        DashboardTable(listOf("Period", "Actual (t/ha)", "Budget (t/ha)"), listOf(listOf("YTD", Pending, Pending)), minimumWidth = 320)
        Text("Oil tonnes per hectare. Awaiting approved oil yield records.", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }

}

@Composable
private fun DashboardPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))) {
        Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 10.dp))
        content()
    }
}

@Composable
private fun ChartPending(label: String) {
    Column(Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(12.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("Awaiting estate data", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MetricRow(label: String) {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f))
        Text(Pending, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DashboardTable(headers: List<String>, rows: List<List<String>>, minimumWidth: Int = 0) {
    // Only wide tables scroll horizontally. The page retains a single vertical scroll position.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val tableWidth = maxOf(maxWidth, minimumWidth.dp)
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(tableWidth).padding(horizontal = 8.dp)) {
                (listOf(headers) + rows).forEachIndexed { index, cells ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cells.forEachIndexed { column, cell ->
                            Text(cell, modifier = Modifier.weight(if (column == 0) 1.5f else 1f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}


