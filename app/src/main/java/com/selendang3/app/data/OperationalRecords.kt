package com.selendang3.app.data

import java.time.LocalDate
import java.time.YearMonth

/** Domain values use explicit units; null is missing and zero is measured zero. */
private fun validMeasure(value: Double?) = value == null || (value.isFinite() && value >= 0)
private fun validField(code: String) = code.isNotBlank() && code == code.trim() && code.length <= 100 && code.none { it.isISOControl() }

data class FieldDefinition(val code: String, val plantedAreaHa: Double?, val perimeterAreaHa: Double?,
    val standingPalms: Int?, val plantingYear: Int?, val maturityYear: Int?,
    val variety: String?, val terrain: String?, val soil: String?) {
    init {
        require(validField(code) && listOf(plantedAreaHa, perimeterAreaHa).all(::validMeasure))
        require(standingPalms == null || standingPalms >= 0)
        require(listOf(plantingYear, maturityYear).all { it == null || it in 1..9999 })
        require(plantingYear == null || maturityYear == null || maturityYear >= plantingYear)
    }
}

data class MonthlyCropRecord(val fieldCode: String, val month: YearMonth, val through: LocalDate,
    val actualMt: Double?, val budgetMt: Double?, val areaHa: Double?, val complete: Boolean) {
    init {
        require(validField(fieldCode) && YearMonth.from(through) == month)
        require(!complete || through == month.atEndOfMonth())
        require(listOf(actualMt, budgetMt, areaHa).all(::validMeasure))
    }
    val actualMtPerHa: Double? get() = if (actualMt != null && areaHa != null && areaHa > 0)
        (actualMt / areaHa).takeIf(Double::isFinite) else null
    val achievementPercent: Double? get() = if (actualMt != null && budgetMt != null && budgetMt > 0)
        (actualMt / budgetMt * 100).takeIf(Double::isFinite) else null
}

enum class AbwBasis { ANNUAL, YTD }
data class AbwRecord(val fieldCode: String, val year: Int, val basis: AbwBasis,
    val through: LocalDate, val kgPerBunch: Double?) {
    init {
        require(validField(fieldCode) && year in 1..9999 && through.year == year && validMeasure(kgPerBunch))
        require(basis != AbwBasis.ANNUAL || through == LocalDate.of(year, 12, 31))
    }
}

enum class UpkeepOperation { WEEDING, MANURING, PEST_DISEASE, WATER, ROAD, OTHER }
enum class WorkUnit { HECTARES, KILOGRAMS, BAGS, KILOMETRES, HOURS, COUNT }
data class UpkeepRecord(val id: String, val date: LocalDate, val fieldCode: String?,
    val operation: UpkeepOperation, val quantity: Double?, val unit: WorkUnit,
    val bagWeightKg: Double?, val round: Int?, val notes: String) {
    init {
        require(id.isNotBlank() && (fieldCode == null || validField(fieldCode)))
        require(validMeasure(quantity) && validMeasure(bagWeightKg) && (round == null || round > 0))
        require(bagWeightKg == null || (unit == WorkUnit.BAGS && bagWeightKg > 0))
    }
    val kilograms: Double? get() = when (unit) {
        WorkUnit.KILOGRAMS -> quantity
        WorkUnit.BAGS -> if (quantity != null && bagWeightKg != null) (quantity * bagWeightKg).takeIf(Double::isFinite) else null
        else -> null
    }
}
