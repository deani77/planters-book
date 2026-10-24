package com.selendang3.app.data

import java.time.YearMonth
import kotlin.math.abs

data class EstateWorkerRatios(val period: YearMonth, val immatureArea: Double, val matureArea: Double,
    val matureOperationsWorkers: Int) {
    val matureHaPerWorker: Double? get() = if (matureArea > 0 && matureOperationsWorkers > 0)
        (matureArea / matureOperationsWorkers).takeIf { it.isFinite() } else null
}

fun estateWorkerRatios(update: EstatePackage?): EstateWorkerRatios? {
    val overview = update?.tables?.singleOrNull { it.id == "overview" } ?: return null
    val item = overview.columns.indexOf("Item")
    val details = overview.columns.indexOf("Details")
    if (minOf(item, details) < 0) return null
    fun value(label: String): String? = overview.rows.singleOrNull { it.getOrNull(item) == label }?.getOrNull(details)
    fun area(label: String): Double? = value(label)?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val period = runCatching { YearMonth.parse(value("Ratios reporting period")) }.getOrNull() ?: return null
    val immature = area("Ratios immature area (ha)") ?: return null
    val mature = area("Ratios mature area (ha)") ?: return null
    val workers = value("Ratios mature operations workers")?.trim()?.toIntOrNull()?.takeIf { it in 0..1000000 } ?: return null
    val fields = update.tables.singleOrNull { it.id == "fields" } ?: return null
    val areaColumn = fields.columns.indexOf("Area (ha)")
    if (areaColumn < 0 || fields.rows.isEmpty()) return null
    val fieldArea = fields.rows.sumOf { row ->
        row.getOrNull(areaColumn)?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return null
    }
    if (!fieldArea.isFinite() || abs(mature + immature - fieldArea) > 0.011) return null
    return EstateWorkerRatios(period, immature, mature, workers)
}
