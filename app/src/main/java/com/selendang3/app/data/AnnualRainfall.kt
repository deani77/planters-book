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
