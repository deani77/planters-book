package com.selendang3.app.data

import kotlin.math.abs

data class SoilSeriesBand(val name: String, val area: Double, val share: Double,
    val suitability: String, val reference: String?, val qualification: String?)
data class EstateSoilSeries(val bands: List<SoilSeriesBand>) {
    val totalArea: Double get() = bands.sumOf { it.area }
}

fun estateSoilSeries(update: EstatePackage?): EstateSoilSeries? {
    val soil = update?.tables?.singleOrNull { it.id == "soil" } ?: return null
    val fields = update.tables.singleOrNull { it.id == "fields" } ?: return null
    val fieldAreaIndex = fields.columns.indexOf("Area (ha)")
    if (fieldAreaIndex < 0 || fields.rows.isEmpty()) return null
    val expectedArea = fields.rows.sumOf { row ->
        row.getOrNull(fieldAreaIndex)?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return null
    }
    if (!expectedArea.isFinite()) return null
    val nameIndex = soil.columns.indexOf("Type")
    val categoryIndex = soil.columns.indexOf("Category")
    val areaIndex = soil.columns.indexOf("Area (ha)")
    val shareIndex = soil.columns.indexOf("Share (%)")
    if (minOf(nameIndex, categoryIndex, areaIndex, shareIndex) < 0) return null
    val classIndex = soil.columns.indexOf("Suitability class")
    val referenceIndex = soil.columns.indexOf("Class reference")
    val qualificationIndex = soil.columns.indexOf("Class qualification")
    val rows = soil.rows.filter { it.getOrNull(categoryIndex) == "Soil series" }
    if (rows.isEmpty()) return null
    val names = mutableSetOf<String>()
    val bands = rows.map { row ->
        val name = row.getOrNull(nameIndex)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!names.add(name.lowercase(java.util.Locale.ROOT))) return null
        val area = row.getOrNull(areaIndex)?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 } ?: return null
        val share = row.getOrNull(shareIndex)?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..100.0 } ?: return null
        if (abs(area - expectedArea * share / 100) > 0.011) return null
        val reference = row.getOrNull(referenceIndex)?.takeIf { it.isNotBlank() }
        val suitability = row.getOrNull(classIndex)?.takeIf { it in listOf("I", "II", "III", "IV", "V") && reference != null }
        SoilSeriesBand(name, area, share, suitability?.let { "Class $it*" } ?: "Pending",
            reference, row.getOrNull(qualificationIndex))
    }
    if (abs(bands.sumOf { it.share } - 100) > 0.001 || abs(bands.sumOf { it.area } - expectedArea) > 0.011) return null
    return EstateSoilSeries(bands)
}
