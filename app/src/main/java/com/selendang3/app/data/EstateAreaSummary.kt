package com.selendang3.app.data

import java.time.LocalDate
import java.time.YearMonth

data class EstateAreaSummary(val period: YearMonth, val perimeterHa: Double, val matureHa: Double)
fun estateAreaSummary(update: EstatePackage?, today: LocalDate): EstateAreaSummary? {
    val overview = update?.tables?.singleOrNull { it.id == "overview" } ?: return null
    val item = overview.columns.indexOf("Item"); val details = overview.columns.indexOf("Details")
    if (minOf(item, details) < 0) return null
    fun value(label: String) = overview.rows.singleOrNull { it.getOrNull(item) == label }?.getOrNull(details)
    fun area(label: String) = value(label)?.replace(",", "")?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val period = runCatching { YearMonth.parse(value("Estate area reporting period")) }.getOrNull() ?: return null
    if (period > YearMonth.from(today)) return null
    val perimeter = area("Perimeter area (ha)")?.takeIf { it > 0 } ?: return null
    val mature = area("Mature area (ha)") ?: return null
    if (mature > perimeter) return null
    return EstateAreaSummary(period, perimeter, mature)
}

