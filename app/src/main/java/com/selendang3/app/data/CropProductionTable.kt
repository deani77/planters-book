package com.selendang3.app.data

import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

fun formatCropMetric(value: Double?): String = value?.takeIf { it.isFinite() && it >= 0 }
    ?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"

enum class CropPeriod { MTD, YTD, ANNUAL }
data class CropMetrics(val areaHa: Double?, val targetMtdMt: Double?, val targetYtdMt: Double?,
    val targetAnnualMt: Double?, val actualMtdMt: Double?, val actualYtdMt: Double?) {
    private fun divide(n: Double?, d: Double?, scale: Double = 1.0): Double? =
        if (n != null && d != null && n.isFinite() && n >= 0 && d.isFinite() && d > 0)
            (n / d * scale).takeIf(Double::isFinite) else null
    fun target(period: CropPeriod, asYield: Boolean = false): Double? {
        val tonnes = when (period) { CropPeriod.MTD -> targetMtdMt; CropPeriod.YTD -> targetYtdMt; CropPeriod.ANNUAL -> targetAnnualMt }
        return if (asYield) divide(tonnes, areaHa) else tonnes
    }
    fun actual(period: CropPeriod, asYield: Boolean = false): Double? {
        val tonnes = when (period) { CropPeriod.MTD -> actualMtdMt; CropPeriod.YTD -> actualYtdMt; CropPeriod.ANNUAL -> null }
        return if (asYield) divide(tonnes, areaHa) else tonnes
    }
    fun achievement(period: CropPeriod): Double? = divide(if (period == CropPeriod.MTD) actualMtdMt else actualYtdMt, target(period), 100.0)
}
data class CropTableRow(val field: String, val metrics: CropMetrics) {
    val area: String get() = formatCropMetric(metrics.areaHa)
    // Compatibility adapter for older consumers, including the separate PR #20.
    val values: List<String> get() = (CropPeriod.values().map { metrics.target(it) } +
        CropPeriod.values().map { metrics.target(it, true) } +
        listOf(CropPeriod.MTD, CropPeriod.YTD).map { metrics.actual(it) } +
        listOf(CropPeriod.MTD, CropPeriod.YTD).map { metrics.actual(it, true) } +
        CropPeriod.values().map { metrics.achievement(it) }).map(::formatCropMetric)
}
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
        return CropTableRow(field, CropMetrics(area, target[0], target[1], target[2], actual[0], actual[1]))
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
data class FieldCropSummaryGroup(val title: String, val rows: List<FieldCropSummaryRow>, val note: String = "")
data class FieldCropSummary(val note: String, val crewNote: String, val groups: List<FieldCropSummaryGroup>)
// Presentation contract: keep every existing production group except ABW, owned by Field Details.
fun fieldCropReportGroups(summary: FieldCropSummary): List<FieldCropSummaryGroup> =
    summary.groups.filter { it.title != "Average Bunch Weight" }

private fun fieldAbwSummary(update: EstatePackage?, today: LocalDate, code: String): FieldCropSummaryGroup {
    val table = update?.tables?.firstOrNull { it.id == "abw" }
    val columns = table?.columns.orEmpty()
    val codeIndex = columns.indexOf("Field code")
    val yearIndex = columns.indexOf("Year")
    val valueIndex = columns.indexOf("YTD ABW (kg/bunch)")
    val rows = table?.rows.orEmpty()
    fun cell(row: List<String?>, index: Int) = row.getOrNull(index)?.trim()
    fun year(row: List<String?>) = cell(row, yearIndex)?.toIntOrNull()?.takeIf { it in 1900..today.year }
    val latest = rows.mapNotNull(::year).maxOrNull() ?: today.year
    val values = listOf(latest, latest - 1).map { target ->
        val matches = rows.filter { cell(it, codeIndex) == code && year(it) == target }
        val value = matches.singleOrNull()?.let { cell(it, valueIndex)?.toDoubleOrNull() }?.takeIf { it.isFinite() && it >= 0 }
        FieldCropSummaryRow("$target YTD (kg/bunch)", value?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending")
    }
    return FieldCropSummaryGroup("Average Bunch Weight", values,
        "Cumulative/YTD source averages. Reporting cutoff unconfirmed; these figures may cover a different period from the crop report.")
}
private fun previousYearCropRows(update: EstatePackage?, reportDate: String?, code: String): List<FieldCropSummaryRow> {
    val current = reportDate?.let(LocalDate::parse)
    val cutoff = current?.minusYears(1)?.withDayOfMonth(1)?.minusDays(1)
    val history = update?.let { JSONObject(it.json).optJSONArray("cropProductionReportHistory") }
    fun yieldAt(date: LocalDate?): String {
        if (date == null) return "Pending"
        val reports = (0 until (history?.length() ?: 0)).mapNotNull { history?.optJSONObject(it) }
            .filter { it.optString("reportDate") == date.toString() }
        val fields = reports.singleOrNull()?.optJSONArray("fields")
        val matches = (0 until (fields?.length() ?: 0)).mapNotNull { fields?.optJSONObject(it) }
            .filter { it.optString("field") == code }
        val source = matches.singleOrNull() ?: return "Pending"
        fun number(key: String) = (source.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0 }
        val tonnes = number("sentToMillYtdMt") ?: return "Pending"
        val area = number("areaHa")?.takeIf { it > 0 } ?: return "Pending"
        return (tonnes / area).takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"
    }
    return listOf(
        FieldCropSummaryRow("Last Year Same-Period YTD${cutoff?.let { " · through $it" }.orEmpty()} (mt/ha)", yieldAt(cutoff)))
}

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
            FieldCropSummaryRow("Budget This Month (mt)", formatCropMetric(field.metrics.targetMtdMt)), FieldCropSummaryRow("Budget YTD (mt)", formatCropMetric(field.metrics.targetYtdMt)),
            FieldCropSummaryRow("Year Budget (mt)", formatCropMetric(field.metrics.targetAnnualMt)), FieldCropSummaryRow("Actual MTD (mt)", formatCropMetric(field.metrics.actualMtdMt)), FieldCropSummaryRow("Actual YTD (mt)", formatCropMetric(field.metrics.actualYtdMt))) + previousYearCropRows(update, table.reportDate, code),
            "Last-year yield uses each field’s historical sent-to-mill tonnes and hectares. Same-period YTD runs through the last completed month, one year earlier. Missing or ambiguous records remain Pending."),
        fieldAbwSummary(update, today, code),
        FieldCropSummaryGroup("Harvesting", listOf(FieldCropSummaryRow("Required Harvesters (people)", crew), FieldCropSummaryRow("Harvesting Rounds This Month (rounds)", label("roundsMtd")))),
        FieldCropSummaryGroup("Harvester Productivity", listOf(FieldCropSummaryRow("Tonnes per mandays (mt/manday)", label("mtPerHarvesterDayMtd")), FieldCropSummaryRow("Hectare per mandays (ha/manday)", label("haPerHarvesterDayMtd"))))
    ))
}


