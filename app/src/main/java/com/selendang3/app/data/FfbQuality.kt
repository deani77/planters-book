package com.selendang3.app.data

import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

data class FfbQuality(val rows: List<List<String>>, val note: String)
fun estateFfbQuality(update: EstatePackage?, today: LocalDate): FfbQuality {
    val rows = listOf("Ripe (%)", "Underipe (%)", "Unripe (%)", "Reject (mt)").map { mutableListOf(it, "Pending", "Pending") }
    var note = "Awaiting approved FFB quality records."
    val report = update?.let { JSONObject(it.json).optJSONObject("ffbRejectReport") }
    val date = runCatching { LocalDate.parse(report?.optString("reportDate")) }.getOrNull()?.takeIf { it <= today }
    if (date != null) {
        val totals = report?.optJSONObject("totals")
        fun number(key: String) = (totals?.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0 }
        fun label(value: Double?) = value?.let { String.format(Locale.US, "%.2f", it) } ?: "Pending"
        rows[3][1] = label(number("rejectMtdMt")?.takeIf { YearMonth.from(date) == YearMonth.from(today) })
        rows[3][2] = label(number("rejectYtdMt")?.takeIf { date.year == today.year })
        note = "Reject report: $date. Rejected FFB quantities are shown in mt. Other quality categories remain Pending."
    }
    return FfbQuality(rows, note)
}
