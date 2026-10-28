package com.selendang3.app.data

import org.json.JSONObject

// Display-only source facts; they do not replace operational areas or infer palm counts.
fun sourceFieldProfileTables(update: EstatePackage?, code: String): List<EstateTable> {
    if (code !in estateFieldCodes(update)) return emptyList()
    return runCatching { sourceProfileTables(update, code) }.getOrDefault(emptyList())
}

private fun sourceProfileTables(update: EstatePackage?, code: String): List<EstateTable> {
    val profiles = update?.let { JSONObject(it.json).optJSONArray("fieldProfiles") } ?: return emptyList()
    val matches = (0 until profiles.length()).map { profiles.getJSONObject(it) }.filter { it.optString("fieldCode") == code }
    val profile = matches.singleOrNull() ?: return emptyList()
    fun value(obj: JSONObject, key: String): String? = if (obj.isNull(key)) null else obj.get(key).toString()
    fun validNumbers(obj: JSONObject, keys: List<String>) = keys.all { key ->
        val number = obj.opt(key) as? Number
        number != null && number.toDouble().isFinite() && number.toDouble() >= 0
    }
    require(profile.getInt("reportYear") in 1..9999)
    require(validNumbers(profile, listOf("plantedAreaHa", "unplantedAreaHa", "divisionStandsPerHa", "divisionPalmHeightM")))
    val sourceBlocks = profile.getJSONArray("blocks")
    val ids = (0 until sourceBlocks.length()).map { index ->
        val block = sourceBlocks.getJSONObject(index)
        require(validNumbers(block, listOf("plantedAreaHa", "unplantedAreaHa")))
        require(block.getInt("harvestingYear") in 1..9999)
        block.getString("blockId").also { require(it.isNotBlank()) }
    }
    require(ids.distinct().size == ids.size)
    val summary = EstateTable("Source profile · ${profile.getInt("reportYear")}",
        listOf("Planted area (ha)", "Unplanted area (ha)", "Division stands/ha", "Division palm height (m)", "Maturity"),
        listOf(listOf("plantedAreaHa", "unplantedAreaHa", "divisionStandsPerHa", "divisionPalmHeightM", "maturity").map { value(profile, it) }))
    val blocks = profile.getJSONArray("blocks")
    return listOf(summary) + (0 until blocks.length()).map { index ->
        val block = blocks.getJSONObject(index)
        EstateTable("Block ${block.getString("blockName")}",
            listOf("Block identifier", "Planted area (ha)", "Unplanted area (ha)", "Soil code", "Harvesting year", "Source note"),
            listOf(listOf("blockId", "plantedAreaHa", "unplantedAreaHa", "soilCode", "harvestingYear", "sourceNote").map { value(block, it) }))
    }
}

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
