package com.selendang3.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

fun cropProductionFieldCodes(update: EstatePackage?): List<String> =
    (estateFieldCodes(update) + cropHistoryFields(update).map { it.code }).distinct().sorted()

fun fieldHarvestingGroups(summary: FieldCropSummary?): List<FieldCropSummaryGroup> =
    summary?.groups.orEmpty().filter { it.title in listOf("Harvesting", "Harvester Productivity") }

fun fieldProductionPerformance(update: EstatePackage?, today: LocalDate, code: String): List<FieldCropSummaryRow> {
    val row = cropProductionTable(update, today).rows.singleOrNull { it.field == code }
    fun value(index: Int) = row?.values?.getOrNull(index) ?: "Pending"
    return listOf(
        FieldCropSummaryRow("MTD actual / MTD budget (MT/ha)", "${value(8)} / ${value(3)} · Achievement: ${value(10)} %"),
        FieldCropSummaryRow("YTD actual / YTD budget (MT/ha)", "${value(9)} / ${value(4)} · Achievement: ${value(11)} %"),
        FieldCropSummaryRow("YTD actual / annual budget (MT/ha)", "${value(9)} / ${value(5)} · Achievement: ${value(12)} %")
    )
}

data class FieldMonthlyProductionRow(val period: String, val actualMt: String, val budgetMt: String,
    val actualYield: String, val achievement: String)
data class FieldMonthlyProduction(val year: Int, val rows: List<FieldMonthlyProductionRow>)

// Monthly actuals are MTD source values, never differences of potentially incomplete YTD records.
fun fieldMonthlyProduction(update: EstatePackage?, today: LocalDate, code: String): FieldMonthlyProduction {
    val root = update?.let { JSONObject(it.json) }
    val current = root?.optJSONObject("cropProductionReport")
    fun date(report: JSONObject?) = runCatching { LocalDate.parse(report?.optString("reportDate")) }
        .getOrNull()?.takeIf { it <= today }
    val reportDate = date(current)
    val year = reportDate?.year ?: today.year
    fun objects(array: JSONArray?) = (0 until (array?.length() ?: 0)).mapNotNull { array?.optJSONObject(it) }
    fun number(obj: JSONObject?, key: String) = (obj?.opt(key) as? Number)?.toDouble()
        ?.takeIf { it.isFinite() && it >= 0 }
    fun label(value: Double?) = value?.takeIf { it.isFinite() && it >= 0 }
        ?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"
    fun divide(n: Double?, d: Double?, scale: Double = 1.0) =
        if (n != null && d != null && d > 0) n / d * scale else null
    val budget = root?.optJSONObject("cropBudget")?.takeIf { it.optInt("year") == year }
    val budgets = objects(budget?.optJSONArray("monthlyBudget"))
    val reports = listOfNotNull(current) + objects(root?.optJSONArray("cropProductionReportHistory"))
    val fields = update?.tables?.singleOrNull { it.id == "fields" }
    val key = fields?.columns?.indexOf("Field code") ?: -1
    val areaIndex = fields?.columns?.indexOf("Area (ha)") ?: -1
    val field = fields?.rows?.singleOrNull { it.getOrNull(key)?.trim() == code }
    val currentArea = field?.getOrNull(areaIndex)?.replace(",", "")?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 }
    val rows = (1..12).map { month ->
        val period = YearMonth.of(year, month)
        val candidates = reports.filter { report ->
            val cutoff = date(report)
            cutoff != null && YearMonth.from(cutoff) == period &&
                ((period == YearMonth.from(today) && report === current) ||
                    (cutoff == period.atEndOfMonth() && report.optBoolean("isCompleteMonth", false)))
        }
        val source = candidates.singleOrNull()
        val record = objects(source?.optJSONArray("fields")).filter { it.optString("field") == code }.singleOrNull()
        val actual = number(record, "sentToMillMtdMt")
        // Historical yield needs historical hectares; do not silently apply today's area.
        val area = number(record, "areaHa") ?: if (source === current) currentArea else null
        val target = budgets.filter { it.optString("field") == code && it.optInt("year") == year && it.optInt("month") == month }
            .singleOrNull()?.let { number(it, "tonnes") }
        val caption = period.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)) +
            (source?.let { " · through ${date(it)}" } ?: " · Actual pending")
        FieldMonthlyProductionRow(caption, label(actual), label(target), label(divide(actual, area)), label(divide(actual, target, 100.0)))
    }
    return FieldMonthlyProduction(year, rows)
}
