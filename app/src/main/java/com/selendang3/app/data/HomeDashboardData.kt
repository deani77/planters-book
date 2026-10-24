package com.selendang3.app.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject

data class HomeDashboardRow(val title: String, val detail: String)

fun homeDashboardRows(update: EstatePackage?, today: LocalDate): List<HomeDashboardRow> {
    val reports = OperationalReports(update, today)
    val month = YearMonth.from(today)
    val monthFormat = DateTimeFormatter.ofPattern("MMM uuuu", Locale.ENGLISH)
    fun label(period: YearMonth) = period.format(monthFormat)
    fun number(value: Double?) = value?.takeIf { it.isFinite() && it >= 0 }
        ?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"
    val yph = update?.yph?.takeIf {
        it.reportingYear == today.year && it.throughMonth in 1..12 &&
            YearMonth.of(it.reportingYear, it.throughMonth) <= month &&
            it.matureAreaHa.isFinite() && it.matureAreaHa > 0
    }
    val annualBudgetProgress = yph?.let { source ->
        val actual = source.actualYtdTonnes?.takeIf { it.isFinite() && it >= 0 } ?: return@let null
        val budget = source.fullYearBudgetTonnes?.takeIf { it.isFinite() && it > 0 } ?: return@let null
        (actual / budget * 100).takeIf { it.isFinite() }
    }
    val yieldPeriod = yph?.let { "YTD through " + label(YearMonth.of(it.reportingYear, it.throughMonth)) } ?: "YTD " + today.year
    val costs = estateFfbCosts(update)?.takeIf { it.actualPeriod.year == today.year && it.actualPeriod <= month }
    return listOf(
        HomeDashboardRow("Crop Data · mt/ha",
            label(month.minusMonths(1)) + ": " + number(reports.previousYield) + " · " + label(month) + " MTD: " + number(reports.current("sentToMillMtdMtPerHa")) + "\n" +
                yieldPeriod + ": " + number(yph?.actualYtdYph) + "\nYTD vs Year Budget: " +
                (annualBudgetProgress?.let { number(it) + "%" } ?: "Pending")),
        HomeDashboardRow("Upkeep and Manuring · %",
            "Weeding: Pending · Manuring: " + (reports.manuringProgress?.let { number(it) + "%" } ?: "Pending")),
        HomeDashboardRow("Estate Cost · Actual YTD · RM/mt",
            "Operation: " + number(costs?.groups?.firstOrNull { it.label == "Operation Cost" }?.actualYtdRmPerTonne) +
                " · GC: " + number(costs?.groups?.firstOrNull { it.label == "General Charges" }?.actualYtdRmPerTonne) +
                "\n" + (costs?.let { "YTD through " + label(it.actualPeriod) } ?: "YTD " + today.year))
    )
}

data class OperationalMetric(val label: String, val value: String, val unit: String)
data class OperationalCard(val title: String, val metrics: List<OperationalMetric>, val period: String)

fun operationalDashboardCards(update: EstatePackage?, today: LocalDate): List<OperationalCard> {
    val reports = OperationalReports(update, today)
    val month = YearMonth.from(today)
    val monthFormat = DateTimeFormatter.ofPattern("MMM uuuu", Locale.ENGLISH)
    fun label(period: YearMonth) = period.format(monthFormat)
    fun number(value: Double?) = value?.takeIf { it.isFinite() && it >= 0 }
        ?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"
    val yph = update?.yph?.takeIf {
        it.reportingYear == today.year && it.throughMonth in 1..12 &&
            YearMonth.of(it.reportingYear, it.throughMonth) <= month &&
            it.matureAreaHa.isFinite() && it.matureAreaHa > 0
    }
    val annualBudgetProgress = yph?.let { source ->
        val actual = source.actualYtdTonnes?.takeIf { it.isFinite() && it >= 0 } ?: return@let null
        val budget = source.fullYearBudgetTonnes?.takeIf { it.isFinite() && it > 0 } ?: return@let null
        (actual / budget * 100).takeIf { it.isFinite() }
    }
    val yieldPeriod = yph?.let { "YTD through " + label(YearMonth.of(it.reportingYear, it.throughMonth)) } ?: "YTD " + today.year
    val costs = estateFfbCosts(update)?.takeIf { it.actualPeriod.year == today.year && it.actualPeriod <= month }
    return listOf(
        OperationalCard("Crop Production", listOf(
            OperationalMetric(label(month.minusMonths(1)), number(reports.previousYield), "mt/ha"),
            OperationalMetric(label(month) + " MTD", number(reports.current("sentToMillMtdMtPerHa")), "mt/ha"),
            OperationalMetric("Actual YTD", number(yph?.actualYtdYph), "mt/ha"),
            OperationalMetric("YTD / Year Budget", number(annualBudgetProgress), "%")
        ), yieldPeriod + (reports.cropDate?.let { " · MTD through $it" } ?: "")),
        OperationalCard("Harvesting Coverage", listOf(
            OperationalMetric("Report day", number(reports.current("hectaresToday")), "ha"),
            OperationalMetric("MTD", number(reports.current("hectaresMtd")), "ha"),
            OperationalMetric("Completed rounds", number(reports.current("roundsMtd")), "rounds")
        ), reports.cropDate?.let { "Report: $it" } ?: "Reporting period pending"),
        OperationalCard("Harvester Productivity", listOf(
            OperationalMetric("Tonnes per mandays", number(reports.current("mtPerHarvesterDayMtd")), "mt/manday"),
            OperationalMetric("Hectare per mandays", number(reports.current("haPerHarvesterDayMtd")), "ha/manday")
        ), reports.cropDate?.let { "MTD through $it" } ?: "Reporting period pending"),
        OperationalCard("Manuring", listOf(
            OperationalMetric("Programme progress", number(reports.manuringProgress), "%")
        ), reports.manuringNote),
        OperationalCard("Weeding", listOf(
            OperationalMetric("Circle", "Pending", "%"),
            OperationalMetric("Selective", "Pending", "%")
        ), "Reporting period pending"),
        OperationalCard("Pest & Disease", listOf(
            OperationalMetric("Affected area", "Pending", "ha"),
            OperationalMetric("Incidence", "Pending", "%")
        ), "Reporting period pending"),
        OperationalCard("Estate Cost", listOf(
            OperationalMetric("Operation Cost", number(costs?.groups?.firstOrNull { it.label == "Operation Cost" }?.actualYtdRmPerTonne), "RM/mt"),
            OperationalMetric("General Charges", number(costs?.groups?.firstOrNull { it.label == "General Charges" }?.actualYtdRmPerTonne), "RM/mt")
        ), costs?.let { "YTD through " + label(it.actualPeriod) } ?: "YTD " + today.year)
    )
}

private class OperationalReports(update: EstatePackage?, private val today: LocalDate) {
    private val root = update?.json?.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() }
    private fun date(report: JSONObject?): LocalDate? {
        val text = report?.optString("reportDate") ?: return null
        return runCatching { LocalDate.parse(text) }.getOrNull()?.takeIf { it <= today && it.toString() == text }
    }
    private fun number(report: JSONObject?, key: String): Double? = (report?.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0 }
    private val crop = root?.optJSONObject("cropProductionReport")?.takeIf { date(it)?.let { d -> YearMonth.from(d) == YearMonth.from(today) } == true }
    val cropDate: LocalDate? get() = date(crop)
    fun current(key: String): Double? = number(crop?.optJSONObject("totals"), key)
    val previousYield: Double? get() {
        val end = YearMonth.from(today).minusMonths(1).atEndOfMonth()
        val history = root?.optJSONArray("cropProductionReportHistory")
        val reports = listOfNotNull(root?.optJSONObject("cropProductionReport")) +
            (0 until (history?.length() ?: 0)).mapNotNull { history?.optJSONObject(it) }
        val previous = reports.filter { it.optBoolean("isCompleteMonth", false) && date(it) == end }
        return if (previous.size == 1) number(previous.single().optJSONObject("totals"), "sentToMillMtdMtPerHa") else null
    }
    private val manuring = root?.optJSONObject("manuringReport")?.takeIf { date(it)?.year == today.year }
    val manuringProgress: Double? get() {
        val applied = number(manuring, "appliedYtdMt") ?: return null
        val programme = number(manuring, "annualProgrammeMt")?.takeIf { it > 0 } ?: return null
        return (applied / programme * 100).takeIf { it.isFinite() && it >= 0 }
    }
    val manuringNote: String get() = if (manuringProgress != null) "YTD through ${date(manuring)} · Applied mt / annual programme mt" else "Reporting period pending"
}
