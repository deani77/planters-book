package com.selendang3.app.data

import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

data class CropTableRow(val field: String, val area: String, val values: List<String>)
data class CropTable(val note: String, val rows: List<CropTableRow>, val total: CropTableRow?, val reportDate: String? = null)
val cropTableGroups = listOf(
    "Target · mt" to listOf("This Month", "Year to Date", "Full Year"),
    "Target Yield · mt/ha" to listOf("This Month", "Year to Date", "Full Year"),
    "Actual · mt" to listOf("This Month", "Year to Date"),
    "Actual Yield · mt/ha" to listOf("This Month", "Year to Date"),
    "Achievement · %" to listOf("This Month", "Year to Date", "Full Year")
)
fun cropProductionTable(update: EstatePackage?, today: LocalDate): CropTable {
    val root = update?.let { JSONObject(it.json) }
    val report = root?.optJSONObject("cropProductionReport")
    val date = runCatching { LocalDate.parse(report?.optString("reportDate")) }.getOrNull()?.takeIf { it <= today }
    fun valid(value: Double?) = value?.takeIf { it.isFinite() && it >= 0 }
    fun number(row: JSONObject?, key: String): Double? = (row?.opt(key) as? Number)?.toDouble()?.let(::valid)
    fun label(value: Double?) = valid(value)?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"
    fun divide(n: Double?, d: Double?, scale: Double = 1.0): Double? =
        if (n != null && d != null && d > 0) valid(n / d * scale) else null
    fun objects(array: org.json.JSONArray?) = (0 until (array?.length() ?: 0)).mapNotNull { array?.optJSONObject(it) }
    val fields = update?.tables?.firstOrNull { it.id == "fields" }
    val codeIndex = fields?.columns?.indexOf("Field code") ?: -1
    val areaIndex = fields?.columns?.indexOf("Area (ha)") ?: -1
    val identities = if (codeIndex >= 0 && areaIndex >= 0) fields!!.rows.mapNotNull { row ->
        row[codeIndex]?.takeIf { it.isNotBlank() }?.let { it to valid(row[areaIndex]?.replace(",", "")?.toDoubleOrNull()) }
    } else emptyList()
    if (identities.map { it.first }.distinct().size != identities.size) return CropTable("Duplicate field records; review source.", emptyList(), null)
    val budget = root?.optJSONObject("cropBudget")
    val budgetEntries = objects(budget?.optJSONArray("monthlyBudget"))
    fun targets(field: String): List<Double?> {
        if (date == null || budget?.optInt("year") != date.year) return List(3) { null }
        val entries = budgetEntries.filter { it.optString("field") == field && it.optInt("year") == date.year }
        fun sum(months: List<Int>): Double? {
            var total = 0.0
            for (month in months) {
                val matches = entries.filter { it.optInt("month") == month }
                if (matches.size != 1) return null
                total += number(matches.single(), "tonnes") ?: return null
            }
            return valid(total)
        }
        return listOf(sum(listOf(date.monthValue)), sum((1..date.monthValue).toList()), sum((1..12).toList()))
    }
    fun actual(field: String): List<Double?> {
        if (date == null) return List(2) { null }
        val rows = objects(report?.optJSONArray("fields")).filter { it.optString("field") == field }
        if (rows.size != 1) return List(2) { null }
        return listOf(number(rows.single(), "sentToMillMtdMt"), number(rows.single(), "sentToMillYtdMt"))
    }
    fun row(field: String, area: Double?, target: List<Double?>, actual: List<Double?>): CropTableRow {
        val values = target + target.map { divide(it, area) } + actual + actual.map { divide(it, area) } + listOf(
            divide(actual[0], target[0], 100.0), divide(actual[1], target[1], 100.0), divide(actual[1], target[2], 100.0))
        return CropTableRow(field, label(area), values.map(::label))
    }
    fun sum(values: List<Double?>): Double? = if (values.any { it == null }) null else valid(values.filterNotNull().sum())
    val rows = identities.map { row(it.first, it.second, targets(it.first), actual(it.first)) }
    val total = if (identities.isEmpty()) null else row("TOTAL", sum(identities.map { it.second }),
        (0..2).map { index -> sum(identities.map { targets(it.first)[index] }) },
        (0..1).map { index -> sum(identities.map { actual(it.first)[index] }) })
    val note = date?.let { "${it.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))} · Actual through $it. Full Year achievement = actual YTD / annual target." }
        ?: "Import a dated crop report in Settings → Estate Data Management. Missing values remain Pending."
    return CropTable(note, rows, total, date?.toString())
}

data class FieldCropSummaryRow(val label: String, val value: String)
data class FieldCropSummaryGroup(val title: String, val rows: List<FieldCropSummaryRow>)
data class FieldCropSummary(val note: String, val crewNote: String, val groups: List<FieldCropSummaryGroup>)
fun fieldCropSummary(update: EstatePackage?, today: LocalDate, code: String): FieldCropSummary? {
    val table = cropProductionTable(update, today)
    val field = table.rows.firstOrNull { it.field == code } ?: return null
    val fields = update?.tables?.firstOrNull { it.id == "fields" }
    val key = fields?.columns?.indexOf("Field code") ?: -1
    val areaIndex = fields?.columns?.indexOf("Area (ha)") ?: -1
    val area = if (key >= 0 && areaIndex >= 0) fields!!.rows.firstOrNull { it[key] == code }?.get(areaIndex)?.replace(",", "")?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 } else null
    val report = if (table.reportDate != null) update?.let { JSONObject(it.json).optJSONObject("cropProductionReport") } else null
    val records = report?.optJSONArray("fields")
    val matches = (0 until (records?.length() ?: 0)).mapNotNull { records?.optJSONObject(it) }.filter { it.optString("field") == code }
    val source = matches.singleOrNull()
    fun label(key: String): String = (source?.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0 }?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"
    val ratio = area?.div(21.0)
    val crew = ratio?.let { String.format(Locale.US, "%.0f", kotlin.math.ceil(it)) } ?: "Pending"
    val crewNote = ratio?.let { "Field hectares ÷ 21 ha/harvester = ${String.format(Locale.US, "%.2f", it)} harvesters. Whole-worker requirement is rounded up." }
        ?: "A verified field area is required for the 21 ha/harvester ratio."
    val note = table.reportDate?.let { "Crop report: $it. Budgets use the same report month." } ?: "Crop report pending. Import estate data in Settings → Estate Data Management."
    return FieldCropSummary(note, crewNote, listOf(
        FieldCropSummaryGroup("Field Profile", listOf(FieldCropSummaryRow("Field Code", code), FieldCropSummaryRow("Hectare (ha)", field.area))),
        FieldCropSummaryGroup("Crop Budget and Actual", listOf(
            FieldCropSummaryRow("Budget This Month (mt)", field.values[0]), FieldCropSummaryRow("Budget YTD (mt)", field.values[1]),
            FieldCropSummaryRow("Year Budget (mt)", field.values[2]), FieldCropSummaryRow("Actual MTD (mt)", field.values[6]), FieldCropSummaryRow("Actual YTD (mt)", field.values[7]))),
        FieldCropSummaryGroup("Harvesting", listOf(FieldCropSummaryRow("Required Harvesters (people)", crew), FieldCropSummaryRow("Harvesting Rounds This Month (rounds)", label("roundsMtd")))),
        FieldCropSummaryGroup("Harvester Productivity", listOf(FieldCropSummaryRow("Tonnes per mandays (mt/manday)", label("mtPerHarvesterDayMtd")), FieldCropSummaryRow("Hectare per mandays (ha/manday)", label("haPerHarvesterDayMtd"))))
    ))
}

