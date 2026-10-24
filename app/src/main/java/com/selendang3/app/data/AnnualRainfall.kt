package com.selendang3.app.data

import java.time.LocalDate
import java.time.ZoneId

data class AnnualRainfall(val year: Int, val throughMonth: Int, val totalMm: Double?)
fun estateRainfallToday(): LocalDate = LocalDate.now(ZoneId.of("Asia/Kuala_Lumpur"))

/** Five calendar years, newest first; current-month readings never enter YTD. */
fun estateAnnualRainfall(update: EstatePackage?, today: LocalDate = estateRainfallToday()): List<AnnualRainfall> {
    val table = update?.tables?.singleOrNull { it.id == "rainfall" }
    val yearColumn = table?.columns?.indexOf("Year") ?: -1
    val monthColumn = table?.columns?.indexOf("Month / total") ?: -1
    val mmColumn = table?.columns?.indexOf("Rainfall (mm)") ?: -1
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    return (today.year downTo today.year - 4).map { year ->
        val cutoff = if (year == today.year) today.monthValue - 1 else 12
        val readings = mutableMapOf<Int, Double?>()
        var duplicate = false
        if (minOf(yearColumn, monthColumn, mmColumn) >= 0) {
            table?.rows?.filter { it.getOrNull(yearColumn)?.trim()?.toIntOrNull() == year }?.forEach { row ->
                val month = months.indexOfFirst { it.equals(row.getOrNull(monthColumn)?.trim(), ignoreCase = true) } + 1
                // Printed totals are deliberately ignored to avoid double-counting.
                if (month in 1..cutoff) {
                    if (readings.containsKey(month)) duplicate = true
                    readings[month] = row.getOrNull(mmColumn)?.trim()?.replace(",", "")?.toDoubleOrNull()
                        ?.takeIf { it.isFinite() && it >= 0 }
                }
            }
        }
        val complete = cutoff > 0 && !duplicate && (1..cutoff).all { readings[it] != null }
        val total = if (complete) readings.values.filterNotNull().sum().takeIf { it.isFinite() } else null
        AnnualRainfall(year, cutoff, total)
    }
}

enum class RainfallHighlight(val label: String) {
    HIGHEST("Highest full year"), LOWEST("Lowest full year"), FULL_YEAR("Full year"), YTD("Current YTD")
}
data class RainfallChartBar(val year: Int, val totalMm: Double, val highlight: RainfallHighlight)
fun rainfallChartBars(records: List<AnnualRainfall>): List<RainfallChartBar> {
    val available = records.filter { it.totalMm?.let { value -> value.isFinite() && value >= 0 } == true }
    val full = available.filter { it.throughMonth == 12 }.map { it.totalMm!! }
    val high = full.maxOrNull(); val low = full.minOrNull()
    val distinct = full.size >= 2 && high != null && low != null && high - low > 1e-7
    return available.map { row ->
        val value = row.totalMm!!
        val highlight = when {
            row.throughMonth < 12 -> RainfallHighlight.YTD
            distinct && kotlin.math.abs(value - high!!) < 1e-7 -> RainfallHighlight.HIGHEST
            distinct && kotlin.math.abs(value - low!!) < 1e-7 -> RainfallHighlight.LOWEST
            else -> RainfallHighlight.FULL_YEAR
        }
        RainfallChartBar(row.year, value, highlight)
    }
}

