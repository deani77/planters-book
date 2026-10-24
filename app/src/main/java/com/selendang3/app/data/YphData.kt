package com.selendang3.app.data

import org.json.JSONObject

data class HistoricalYph(val year: Int, val actualYph: Double?)
data class YphData(
    val reportingYear: Int,
    val throughMonth: Int,
    val matureAreaHa: Double,
    val actualYtdTonnes: Double?,
    val budgetYtdTonnes: Double?,
    val fullYearBudgetTonnes: Double?,
    val history: List<HistoricalYph>,
) {
    val displayHistory: List<HistoricalYph> get() = (reportingYear - 4 until reportingYear).map { year ->
        history.find { it.year == year } ?: HistoricalYph(year, null)
    }
    val actualYtdYph: Double? get() = actualYtdTonnes?.div(matureAreaHa)
    val budgetYtdYph: Double? get() = budgetYtdTonnes?.div(matureAreaHa)
    val fullYearBudgetYph: Double? get() = fullYearBudgetTonnes?.div(matureAreaHa)
}

fun parseYph(value: JSONObject): YphData {
    fun integer(key: String, range: IntRange): Int {
        val number = value.get(key)
        require(number is Number && number.toDouble().isFinite() && number.toDouble() % 1.0 == 0.0 && number.toDouble() in range.first.toDouble()..range.last.toDouble()) { "Invalid YPH $key." }
        return number.toInt()
    }
    fun number(objectValue: JSONObject, key: String): Double? {
        require(objectValue.has(key)) { "Missing YPH $key." }
        if (objectValue.isNull(key)) return null
        val result = objectValue.get(key)
        require(result is Number && result.toDouble().isFinite() && result.toDouble() >= 0) { "Invalid YPH $key." }
        return result.toDouble()
    }
    val year = integer("reportingYear", 1900..2200)
    val month = integer("throughMonth", 1..12)
    val area = number(value, "matureAreaHa")
    require(area != null && area > 0) { "YPH requires a positive mature area." }
    val actual = number(value, "actualYtdTonnes")
    val budget = number(value, "budgetYtdTonnes")
    val annual = number(value, "fullYearBudgetTonnes")
    require(budget == null || annual == null || budget <= annual) { "YTD budget exceeds annual budget." }
    val records = value.getJSONArray("history")
    require(records.length() <= 50) { "Too many YPH years." }
    val history = (0 until records.length()).map { index ->
        val row = records.getJSONObject(index)
        val rowYear = row.get("year")
        require(rowYear is Number && rowYear.toDouble().isFinite() && rowYear.toDouble() % 1.0 == 0.0 && rowYear.toDouble() in 1900.0..(year - 1).toDouble()) { "Invalid historical YPH year." }
        HistoricalYph(rowYear.toInt(), number(row, "actualYph"))
    }
    require(history.map { it.year }.distinct().size == history.size) { "Duplicate YPH year." }
    return YphData(year, month, area, actual, budget, annual, history.sortedBy { it.year })
}
