package com.selendang3.app.data

import java.time.LocalDate

val productionFields = listOf("PM04D", "PM05E", "PM06F", "PM08G", "PM10K", "PM11M", "PM15P")
val productionAliases = listOf("0256PM0401", "0256PM0502", "0256PM0603", "0256PM0804", "0256PM1006", "0256PM1108", "0256PM1511").zip(productionFields).toMap()

// One editable local draft per field and reporting date. No server receipt is implied.
data class ProductionDraft(
    val date: String, val field: String,
    val harvested: Double?, val sentToMill: Double?, val backlog: Double?,
    val hectares: Double?, val harvesters: Int?,
) {
    val key: String get() = "$date/${this.field}"
    fun validate() {
        require(field in productionFields) { "Choose a valid field." }
        require(LocalDate.parse(date).toString() == date) { "Use YYYY-MM-DD for the reporting date." }
        require(listOf(harvested, sentToMill, backlog, hectares).all { it == null || (it.isFinite() && it in 0.0..1e9) }) { "Values must be finite and non-negative." }
        require(harvesters == null || harvesters in 0..1000000000) { "Harvesters must be a non-negative whole number." }
        require(listOf(harvested, sentToMill, backlog, hectares, harvesters).any { it != null }) { "Enter at least one value." }
    }
}

data class ProductionTotal(val value: Double?, val reportedDays: Int, val savedDays: Int)
data class ProductionSummary(
    val days: Int, val harvested: ProductionTotal, val sentToMill: ProductionTotal,
    val hectares: ProductionTotal, val harvesterDays: ProductionTotal,
    val backlog: Double?, val backlogDate: String?,
)

fun productionSummary(records: List<ProductionDraft>, field: String, from: LocalDate, through: LocalDate): ProductionSummary {
    val rows = records.filter { it.field == field && LocalDate.parse(it.date) in from..through }.sortedBy { it.date }
    require(rows.map { it.key }.distinct().size == rows.size) { "Duplicate field/date records." }
    fun total(value: (ProductionDraft) -> Double?): ProductionTotal {
        val values = rows.mapNotNull(value)
        return ProductionTotal(values.takeIf { it.isNotEmpty() }?.sum(), values.size, rows.size)
    }
    // A missing balance on the latest saved day is pending, not the older balance.
    val latest = rows.lastOrNull()
    return ProductionSummary(rows.size, total { it.harvested }, total { it.sentToMill },
        total { it.hectares }, total { it.harvesters?.toDouble() }, latest?.backlog, latest?.date)
}
