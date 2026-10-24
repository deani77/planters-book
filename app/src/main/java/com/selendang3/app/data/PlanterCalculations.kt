package com.selendang3.app.data

import java.util.Locale
import kotlin.math.sqrt

enum class PlantingPattern(val label: String) { TRIANGULAR("Triangular"), SQUARE("Square") }
enum class PlantingDistanceUnit(val label: String) { METRES("Metres"), FEET("Feet") }
enum class ProductRateUnit(val label: String, val factor: Double, val liquid: Boolean) {
    LITRES("L/ha", 1000.0, true), MILLILITRES("mL/ha", 1.0, true),
    KILOGRAMS("kg/ha", 1000.0, false), GRAMS("g/ha", 1.0, false);
    val doseUnit: String get() = if (liquid) "mL/pump" else "g/pump"
}
data class PlantDensityResult(val metres: Double, val rowSpacingMetres: Double, val palmsPerHectare: Double)
data class PumpDoseResult(val productPerPump: Double, val hectaresPerPump: Double, val pumpsPerHectare: Double)
object PlanterCalculations {
    fun input(text: String, allowZero: Boolean = false): Double? {
        val clean = text.trim()
        if (!Regex("^([0-9]+([.][0-9]*)?|[.][0-9]+|[0-9]{1,3}(,[0-9]{3})+([.][0-9]*)?)$").matches(clean)) return null
        return clean.replace(",", "").toDoubleOrNull()?.takeIf { it.isFinite() && (if (allowZero) it >= 0 else it > 0) }
    }
    fun density(distance: Double, unit: PlantingDistanceUnit, pattern: PlantingPattern): PlantDensityResult? {
        if (!distance.isFinite() || distance <= 0) return null
        val metres = distance * if (unit == PlantingDistanceUnit.FEET) 0.3048 else 1.0
        val rowSpacing = metres * if (pattern == PlantingPattern.TRIANGULAR) sqrt(3.0) / 2.0 else 1.0
        val density = 10000.0 / (metres * rowSpacing)
        if (!listOf(metres, rowSpacing, density).all { it.isFinite() && it > 0 }) return null
        return PlantDensityResult(metres, rowSpacing, density)
    }
    fun dose(rate: Double, unit: ProductRateUnit, tankLitres: Double, sprayLitresPerHectare: Double): PumpDoseResult? {
        if (!listOf(rate, tankLitres, sprayLitresPerHectare).all { it.isFinite() && it > 0 }) return null
        val coverage = tankLitres / sprayLitresPerHectare
        val dose = rate * unit.factor * coverage
        val pumps = sprayLitresPerHectare / tankLitres
        if (!listOf(coverage, dose, pumps).all { it.isFinite() && it > 0 } || (unit.liquid && dose / 1000.0 > tankLitres)) return null
        return PumpDoseResult(dose, coverage, pumps)
    }
    fun number(value: Double): String = String.format(Locale.US, if (value < 0.01 || value >= 1e9) "%.6g" else "%.2f", value)
    fun fertilizer(palms: Double, kilogramsPerPalm: Double, bagKilograms: Double): FertilizerResult? {
        if (!listOf(palms, kilogramsPerPalm, bagKilograms).all { it.isFinite() && it > 0 } || kotlin.math.round(palms) != palms) return null
        val kg = palms * kilogramsPerPalm
        val bags = kg / bagKilograms
        val whole = kotlin.math.ceil(bags)
        val balance = kotlin.math.max(0.0, whole * bagKilograms - kg)
        if (!listOf(kg, bags, whole, balance).all { it.isFinite() } || kg <= 0 || bags <= 0) return null
        return FertilizerResult(kg, kg / 1000.0, bags, whole, balance)
    }
    fun calibration(waterLitres: Double, testAreaSquareMetres: Double): Double? {
        if (!listOf(waterLitres, testAreaSquareMetres).all { it.isFinite() && it > 0 }) return null
        return (waterLitres / testAreaSquareMetres * 10000.0).takeIf { it.isFinite() && it > 0 }
    }
    fun workCost(labourRM: Double, materialRM: Double, otherRM: Double, quantity: Double, basis: WorkCostBasis): WorkCostResult? {
        if (!listOf(labourRM, materialRM, otherRM).all { it.isFinite() && it >= 0 } || !quantity.isFinite() || quantity <= 0 ||
            (basis == WorkCostBasis.PALMS && kotlin.math.round(quantity) != quantity)) return null
        val total = labourRM + materialRM + otherRM
        val rate = total / quantity
        if (!total.isFinite() || !rate.isFinite()) return null
        return WorkCostResult(total, rate)
    }
    fun convert(value: Double, from: PlantationUnit, to: PlantationUnit): Double? {
        if (!value.isFinite() || value < 0 || from.group != to.group) return null
        return (value * (from.factor / to.factor)).takeIf { it.isFinite() }
    }
}

data class FertilizerResult(val kilograms: Double, val tonnes: Double, val equivalentBags: Double, val wholeBags: Double, val balanceKilograms: Double)
enum class WorkCostBasis(val label: String, val unit: String, val quantityLabel: String) {
    HECTARES("Hectares", "ha", "Work area (ha)"), PALMS("Palms", "palm", "Palms treated (palms)"), TONNES("Tonnes", "mt", "Crop handled (mt)")
}
data class WorkCostResult(val totalRM: Double, val rateRM: Double)
enum class ConversionGroup(val label: String) { AREA("Area"), LENGTH("Length"), MASS("Mass");
    val units: List<PlantationUnit> get() = PlantationUnit.entries.filter { it.group == this }
}
enum class PlantationUnit(val label: String, val symbol: String, val group: ConversionGroup, val factor: Double) {
    HECTARES("Hectares (ha)", "ha", ConversionGroup.AREA, 10000.0), ACRES("Acres (ac)", "ac", ConversionGroup.AREA, 4046.8564224),
    SQUARE_METRES("Square metres (m²)", "m²", ConversionGroup.AREA, 1.0), METRES("Metres (m)", "m", ConversionGroup.LENGTH, 1.0),
    FEET("Feet (ft)", "ft", ConversionGroup.LENGTH, 0.3048), KILOGRAMS("Kilograms (kg)", "kg", ConversionGroup.MASS, 1.0),
    TONNES("Tonnes (mt)", "mt", ConversionGroup.MASS, 1000.0)
}
