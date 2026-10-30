package com.selendang3.app.data

import org.json.JSONObject

data class CropHistoryField(val code: String, val cutoff: String, val productionYear: Int,
    val tonnes: Double?, val hectares: Double?, val annual: Map<Int, Double?>, val areaSource: String) {
    fun yield(year: Int): Double? = if (year != productionYear) annual[year]?.takeIf { it.isFinite() && it >= 0 } else
        if (tonnes != null && tonnes.isFinite() && tonnes >= 0 && hectares != null && hectares.isFinite() && hectares > 0) (tonnes / hectares).takeIf { it.isFinite() } else null
    fun years(currentYear: Int) = (currentYear - 4)..currentYear
}
fun cropHistoryFields(update: EstatePackage?): List<CropHistoryField> = try {
    val history = update?.let { JSONObject(it.json).optJSONObject("cropHistory") }
    val fields = history?.optJSONArray("fields")
    if (fields == null) emptyList() else (0 until fields.length()).map { i ->
        val f = fields.getJSONObject(i)
        fun number(key: String): Double? = if (f.isNull(key)) null else f.getDouble(key)
        val annual = f.getJSONObject("annual")
        CropHistoryField(f.getString("fieldCode"), requireNotNull(history).getString("cutoff"), f.getInt("productionYear"),
            number("productionTonnes"), number("plantedAreaHa"), annual.keys().asSequence().associate { year ->
                year.toInt() to if (annual.isNull(year)) null else annual.getDouble(year)
            }, f.getString("areaSource"))
    }
} catch (_: Exception) { emptyList() }
