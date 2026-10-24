package com.selendang3.app.data

fun estateFieldCodes(update: EstatePackage?): List<String> {
    val table = update?.tables?.firstOrNull { it.id == "fields" } ?: return emptyList()
    val column = table.columns.indexOf("Field code")
    if (column < 0) return emptyList()
    return table.rows.mapNotNull { it.getOrNull(column)?.trim()?.takeIf(String::isNotEmpty) }.distinct().sorted()
}

private val basicFieldColumns = setOf("Field code", "Alternate code", "Area (ha)",
    "Source palms/ha", "Total palms", "Review status", "Planting year", "Planting date",
    "Age / years", "Maturity", "Soil series", "Terrain")

fun estateFieldTables(update: EstatePackage?, code: String): List<EstateTable> = fieldRecordTables(update, code, false)
fun estateFieldCropTables(update: EstatePackage?, code: String): List<EstateTable> = fieldRecordTables(update, code, true)

private fun fieldRecordTables(update: EstatePackage?, code: String, crop: Boolean): List<EstateTable> {
    if (code !in estateFieldCodes(update)) return emptyList()
    return update?.tables.orEmpty().mapNotNull { table ->
        val key = table.columns.indexOfFirst { it in listOf("Field code", "Field / category", "Field") }
        if (key < 0) return@mapNotNull null
        val rows = table.rows.filter { it.getOrNull(key)?.trim() == code }
        if (rows.isEmpty()) return@mapNotNull null
        val indices = when {
            table.id == "fields" -> table.columns.indices.filter {
                if (crop) it == key || table.columns[it] !in basicFieldColumns else table.columns[it] in basicFieldColumns
            }.also { if (crop && it.size <= 1) return@mapNotNull null }
            (crop && table.id == "abw") || (!crop && table.id in listOf("ages", "soil", "roads")) -> table.columns.indices.toList()
            else -> return@mapNotNull null
        }
        if (indices.isEmpty()) null else table.copy(columns = indices.map { table.columns[it] }, rows = rows.map { row -> indices.map { row[it] } })
    }
}
