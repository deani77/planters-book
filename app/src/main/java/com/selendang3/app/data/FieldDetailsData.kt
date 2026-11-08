package com.selendang3.app.data

import org.json.JSONObject

// Display-only source facts; they do not replace operational areas or infer palm counts.
fun sourceFieldProfileTables(update: EstatePackage?, code: String): List<EstateTable> {
    if (code !in estateFieldCodes(update)) return emptyList()
    return runCatching { sourceProfileTables(update, code) }.getOrDefault(emptyList())
}

private fun sourceProfileTables(update: EstatePackage?, code: String): List<EstateTable> {
    val root = update?.let { JSONObject(it.json) } ?: return emptyList()
    validateSourceFieldProfiles(root)
    val profiles = root.optJSONArray("fieldProfiles") ?: return emptyList()
    val matches = (0 until profiles.length()).map { profiles.getJSONObject(it) }.filter { it.optString("fieldCode") == code }
    val profile = matches.singleOrNull() ?: return emptyList()
    fun value(obj: JSONObject, key: String): String? = if (obj.isNull(key)) null else obj.get(key).toString()
    fun validNumbers(obj: JSONObject, keys: List<String>) = keys.all { key ->
        val number = obj.opt(key) as? Number
        number != null && number.toDouble().isFinite() && number.toDouble() >= 0
    }
    fun validYear(obj: JSONObject, key: String): Boolean {
        val number = obj.opt(key) as? Number ?: return false
        val value = number.toDouble()
        return value.isFinite() && value in 1.0..9999.0 && value == kotlin.math.floor(value)
    }
    require(validYear(profile, "reportYear"))
    require(validNumbers(profile, listOf("plantedAreaHa", "unplantedAreaHa", "divisionStandsPerHa", "divisionPalmHeightM")))
    val sourceBlocks = profile.getJSONArray("blocks")
    val ids = (0 until sourceBlocks.length()).map { index ->
        val block = sourceBlocks.getJSONObject(index)
        require(validNumbers(block, listOf("plantedAreaHa", "unplantedAreaHa")))
        require(validYear(block, "harvestingYear"))
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
    "Age / years", "Maturity", "Soil series", "Terrain", "Maturity year", "Age category",
    "Perimeter area (ha)", "Perimeter (ha)", "Planted area (ha)", "Standing palms/ha", "Palm variety")

private val overviewLabels = listOf(
    "Field code", "Planting year", "Maturity year", "Age / years", "Age category",
    "Perimeter area (ha)", "Planted area (ha)", "Total palms", "Standing palms/ha",
    "Palm variety", "Terrain", "Soil series"
)

private val overviewSources = mapOf(
    "Field code" to listOf("Field code"),
    "Planting year" to listOf("Planting year"),
    "Maturity year" to listOf("Maturity year"),
    "Age / years" to listOf("Age / years"),
    "Age category" to listOf("Age category"),
    "Perimeter area (ha)" to listOf("Perimeter area (ha)", "Perimeter (ha)"),
    "Planted area (ha)" to listOf("Planted area (ha)", "Area (ha)"),
    "Total palms" to listOf("Total palms"),
    "Standing palms/ha" to listOf("Standing palms/ha", "Source palms/ha"),
    "Palm variety" to listOf("Palm variety"),
    "Terrain" to listOf("Terrain"),
    "Soil series" to listOf("Soil series")
)

fun estateFieldTables(update: EstatePackage?, code: String): List<EstateTable> {
    if (code !in estateFieldCodes(update)) return emptyList()
    val fields = update?.tables?.firstOrNull { it.id == "fields" }
    val fieldCodeIndex = fields?.columns?.indexOf("Field code") ?: -1
    val row = if (fieldCodeIndex >= 0) fields?.rows?.firstOrNull {
        it.getOrNull(fieldCodeIndex)?.trim() == code
    } else null
    val values = overviewLabels.map { label ->
        overviewSources[label].orEmpty().firstNotNullOfOrNull { source ->
            val index = fields?.columns?.indexOf(source) ?: -1
            if (index < 0) null else row?.getOrNull(index)?.takeIf { it.isNotBlank() }
        }
    }
    val overview = EstateTable("Field Overview", overviewLabels, listOf(values))
    // ABW belongs to field information, not crop production. Preserve imported
    // year labels and values; never fabricate a previous-year measurement.
    // Keep legacy facts that have no slot in the ordered overview, including
    // alternate identifiers, planting dates, review notes and separate source tables.
    val represented = overviewSources.values.flatten().toSet()
    val records = fieldRecordTables(update, code, false).mapNotNull { table ->
        if (table.id != "fields") return@mapNotNull table
        val indices = table.columns.indices.filter {
            table.columns[it] == "Field code" || table.columns[it] !in represented
        }
        if (indices.size <= 1) null else table.copy(
            columns = indices.map { table.columns[it] },
            rows = table.rows.map { cells -> indices.map { cells[it] } }
        )
    }
    return listOf(overview) + records
}
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
            (!crop && table.id in listOf("abw", "ages", "soil", "roads")) -> table.columns.indices.toList()
            else -> return@mapNotNull null
        }
        if (indices.isEmpty()) null else table.copy(columns = indices.map { table.columns[it] }, rows = rows.map { row -> indices.map { row[it] } })
    }
}

// Validate optional source facts at import, before saving or presenting them.
internal fun validateSourceFieldProfiles(root: JSONObject) {
    if (root.isNull("fieldProfiles")) return
    val profiles = root.optJSONArray("fieldProfiles")
    require(profiles != null) { "fieldProfiles must be an array." }
    fun text(obj: JSONObject, key: String): String {
        val value = obj.opt(key)
        require(value is String && value.isNotBlank()) { "Invalid profile $key." }
        return value
    }
    fun year(obj: JSONObject, key: String) {
        val number = obj.opt(key) as? Number
        require(number != null && number.toDouble().isFinite() &&
            number.toDouble() in 1.0..9999.0 && number.toDouble() == kotlin.math.floor(number.toDouble())) {
            "Invalid profile $key."
        }
    }
    fun measures(obj: JSONObject, keys: List<String>) = keys.forEach { key ->
        val value = obj.opt(key) as? Number
        require(value != null && value.toDouble().isFinite() && value.toDouble() >= 0) { "Invalid profile $key." }
    }
    val codes = mutableSetOf<String>()
    for (i in 0 until profiles.length()) {
        val profile = profiles.optJSONObject(i)
        require(profile != null) { "Invalid field profile." }
        require(codes.add(text(profile, "fieldCode").trim())) { "Duplicate profile field code." }
        text(profile, "maturity")
        year(profile, "reportYear")
        measures(profile, listOf("plantedAreaHa", "unplantedAreaHa", "divisionStandsPerHa", "divisionPalmHeightM"))
        val blocks = profile.optJSONArray("blocks")
        require(blocks != null) { "Profile blocks must be an array." }
        val ids = mutableSetOf<String>()
        for (j in 0 until blocks.length()) {
            val block = blocks.optJSONObject(j)
            require(block != null) { "Invalid profile block." }
            require(ids.add(text(block, "blockId").trim())) { "Duplicate profile block identifier." }
            text(block, "blockName")
            year(block, "harvestingYear")
            measures(block, listOf("plantedAreaHa", "unplantedAreaHa"))
            for (key in listOf("soilCode", "sourceNote")) {
                require(block.isNull(key) || block.opt(key) is String) { "Invalid profile $key." }
            }
        }
    }
}
