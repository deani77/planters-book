package com.selendang3.app.data

import kotlin.math.abs

data class TerrainBand(val label: String, val area: Double, val share: Double)
data class EstateTopography(val bands: List<TerrainBand>) {
    val totalArea: Double get() = bands.sumOf { it.area }
}

fun estateTopography(update: EstatePackage?): EstateTopography? {
    val soil = update?.tables?.find { it.id == "soil" } ?: return null
    val fields = update.tables.find { it.id == "fields" } ?: return null
    val fieldAreaColumn = fields.columns.indexOf("Area (ha)")
    if (fieldAreaColumn < 0 || fields.rows.isEmpty()) return null
    val fieldAreas = fields.rows.map { row ->
        row.getOrNull(fieldAreaColumn)?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return null
    }
    val expectedArea = fieldAreas.sum()
    if (!expectedArea.isFinite()) return null
    val typeColumn = soil.columns.indexOf("Type")
    val categoryColumn = soil.columns.indexOf("Category")
    val areaColumn = soil.columns.indexOf("Area (ha)").takeIf { it >= 0 }
        ?: soil.columns.indexOf("Source area (ha)")
    val shareColumn = soil.columns.indexOf("Share (%)").takeIf { it >= 0 }
        ?: soil.columns.indexOf("Source share")
    if (listOf(typeColumn, categoryColumn, areaColumn, shareColumn).any { it < 0 }) return null
    val rows = soil.rows.filter { it.getOrNull(categoryColumn) == "Terrain" }
    val names = listOf("Flat", "Hilly", "Undulating")
    if (rows.size != names.size || rows.map { it.getOrNull(typeColumn) }.toSet() != names.toSet()) return null
    fun numeric(text: String?): Double? = text?.trim()?.removePrefix("~")?.removeSuffix("ha")
        ?.removeSuffix("%")?.replace(",", "")?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() }
    val bands = names.map { name ->
        val row = rows.single { it.getOrNull(typeColumn) == name }
        val area = numeric(row.getOrNull(areaColumn)) ?: return null
        val share = numeric(row.getOrNull(shareColumn)) ?: return null
        if (area < 0 || share !in 0.0..100.0 || abs(area - expectedArea * share / 100) > 0.011) return null
        TerrainBand(name, area, share)
    }
    if (abs(bands.sumOf { it.share } - 100.0) > 0.001 || abs(bands.sumOf { it.area } - expectedArea) > 0.011) return null
    return EstateTopography(bands)
}
