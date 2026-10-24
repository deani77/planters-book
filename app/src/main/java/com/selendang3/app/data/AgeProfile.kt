package com.selendang3.app.data

data class AgeBand(val label: String, val area: Double)
data class EstateAgeProfile(val bands: List<AgeBand>) {
    val totalArea: Double get() = bands.sumOf { it.area }
}

// Group areas by field age, never by the older precomputed category summary.
// Incomplete or ambiguous inputs leave the panel pending rather than implying zero.
fun estateAgeProfile(update: EstatePackage?): EstateAgeProfile? {
    val fields = update?.tables?.find { it.id == "fields" } ?: return null
    val ages = update.tables.find { it.id == "ages" } ?: return null
    val fieldColumn = fields.columns.indexOf("Field code")
    val areaColumn = fields.columns.indexOf("Area (ha)")
    val ageFieldColumn = ages.columns.indexOf("Field / category")
    val ageColumn = ages.columns.indexOf("Age / years")
    if (listOf(fieldColumn, areaColumn, ageFieldColumn, ageColumn).any { it < 0 } || fields.rows.isEmpty()) return null
    val fieldAreas = linkedMapOf<String, Double>()
    for (row in fields.rows) {
        val code = row.getOrNull(fieldColumn)?.takeIf { it.isNotBlank() } ?: return null
        val area = row.getOrNull(areaColumn)?.toDoubleOrNull() ?: return null
        if (!area.isFinite() || area <= 0 || fieldAreas.put(code, area) != null) return null
    }
    val fieldAges = mutableMapOf<String, Int>()
    for (row in ages.rows) {
        val code = row.getOrNull(ageFieldColumn) ?: continue
        if (code !in fieldAreas) continue
        val text = row.getOrNull(ageColumn) ?: return null
        val age = Regex("^(\\d+)\\s*(?:years?)?$", RegexOption.IGNORE_CASE)
            .matchEntire(text.trim())?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (age < 1 || fieldAges.put(code, age) != null) return null
    }
    if (fieldAges.keys != fieldAreas.keys) return null
    var young = 0.0
    var prime = 0.0
    var old = 0.0
    for ((field, area) in fieldAreas) when (fieldAges.getValue(field)) {
        in 1..5 -> young += area
        in 6..23 -> prime += area
        else -> old += area
    }
    return EstateAgeProfile(listOf(
        AgeBand("Young · 1–5 years", young),
        AgeBand("Prime · 6–23 years", prime),
        AgeBand("Old · >23 years", old),
    ))
}
