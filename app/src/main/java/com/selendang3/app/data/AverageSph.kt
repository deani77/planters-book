package com.selendang3.app.data

data class EstateAverageSph(val totalPalms: Long, val totalArea: Double) {
    val palmsPerHa: Double get() = totalPalms / totalArea
}

/** Estate stand density is the ratio of totals, never the mean of field densities. */
fun estateAverageSph(update: EstatePackage?): EstateAverageSph? {
    val table = update?.tables?.singleOrNull { it.id == "fields" } ?: return null
    val codeIndex = table.columns.indexOf("Field code")
    val areaIndex = table.columns.indexOf("Area (ha)")
    val palmsIndex = table.columns.indexOf("Total palms")
    if (minOf(codeIndex, areaIndex, palmsIndex) < 0 || table.rows.isEmpty()) return null
    val codes = mutableSetOf<String>()
    var totalPalms = 0L
    var totalArea = 0.0
    for (row in table.rows) {
        val code = row.getOrNull(codeIndex)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!codes.add(code.uppercase(java.util.Locale.ROOT))) return null
        val area = row.getOrNull(areaIndex)?.replace(",", "")?.trim()?.toDoubleOrNull() ?: return null
        val palms = row.getOrNull(palmsIndex)?.replace(",", "")?.trim()?.toBigDecimalOrNull() ?: return null
        val count = try { palms.longValueExact() } catch (_: ArithmeticException) { return null }
        if (!area.isFinite() || area <= 0 || count < 0 || count > Long.MAX_VALUE - totalPalms) return null
        totalArea += area
        totalPalms += count
    }
    if (!totalArea.isFinite() || !(totalPalms / totalArea).isFinite()) return null
    return EstateAverageSph(totalPalms, totalArea)
}
