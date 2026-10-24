package com.selendang3.app.data

import org.junit.Assert.*
import org.junit.Test

class PlanterCalculationsTest {
    @Test fun geometricDensityAndFeetConversion() {
        assertEquals(100.0, PlanterCalculations.density(10.0, PlantingDistanceUnit.METRES, PlantingPattern.SQUARE)!!.palmsPerHectare, 1e-8)
        assertEquals(142.5556220221309, PlanterCalculations.density(9.0, PlantingDistanceUnit.METRES, PlantingPattern.TRIANGULAR)!!.palmsPerHectare, 1e-8)
        assertEquals(PlanterCalculations.density(9.144, PlantingDistanceUnit.METRES, PlantingPattern.TRIANGULAR)!!.palmsPerHectare,
            PlanterCalculations.density(30.0, PlantingDistanceUnit.FEET, PlantingPattern.TRIANGULAR)!!.palmsPerHectare, 1e-8)
    }
    @Test fun productRateUnitsAndCalibratedCoverage() {
        val result = PlanterCalculations.dose(2.0, ProductRateUnit.LITRES, 16.0, 200.0)!!
        assertEquals(160.0, result.productPerPump, 1e-8)
        assertEquals(0.08, result.hectaresPerPump, 1e-8)
        assertEquals(12.5, result.pumpsPerHectare, 1e-8)
        assertEquals(160.0, PlanterCalculations.dose(2000.0, ProductRateUnit.MILLILITRES, 16.0, 200.0)!!.productPerPump, 1e-8)
        assertEquals(75.0, PlanterCalculations.dose(1.5, ProductRateUnit.KILOGRAMS, 20.0, 400.0)!!.productPerPump, 1e-8)
        assertEquals(75.0, PlanterCalculations.dose(1500.0, ProductRateUnit.GRAMS, 20.0, 400.0)!!.productPerPump, 1e-8)
    }
    @Test fun invalidVolumesAndImpossibleLiquidMixturesAreRejected() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { value ->
            assertNull(PlanterCalculations.density(value, PlantingDistanceUnit.FEET, PlantingPattern.TRIANGULAR))
            assertNull(PlanterCalculations.dose(value, ProductRateUnit.LITRES, 16.0, 200.0))
            assertNull(PlanterCalculations.dose(2.0, ProductRateUnit.LITRES, value, 200.0))
            assertNull(PlanterCalculations.dose(2.0, ProductRateUnit.LITRES, 16.0, value))
        }
        assertNull(PlanterCalculations.dose(201.0, ProductRateUnit.LITRES, 16.0, 200.0))
        assertNull(PlanterCalculations.input("9,5"))
        assertEquals(1000.0, PlanterCalculations.input("1,000")!!, 0.0)
        assertEquals(2000.5, PlanterCalculations.input("2,000.50")!!, 0.0)
        assertEquals(0.5, PlanterCalculations.input(".5")!!, 0.0)
        listOf("", "0", "-1", "nan", "infinity", "1,00").forEach { assertNull(PlanterCalculations.input(it)) }
        assertNotEquals("0.00", PlanterCalculations.number(0.00001))
    }
    @Test fun fertilizerBagsRoundUpAndRetainBalance() {
        val full = PlanterCalculations.fertilizer(100.0, 5.0, 50.0)!!
        assertEquals(500.0, full.kilograms, 1e-8); assertEquals(0.5, full.tonnes, 1e-8)
        assertEquals(10.0, full.wholeBags, 1e-8); assertEquals(0.0, full.balanceKilograms, 1e-8)
        val partial = PlanterCalculations.fertilizer(7.0, 2.0, 5.0)!!
        assertEquals(2.8, partial.equivalentBags, 1e-8); assertEquals(3.0, partial.wholeBags, 1e-8)
        assertEquals(1.0, partial.balanceKilograms, 1e-8)
        assertNull(PlanterCalculations.fertilizer(2.5, 2.0, 50.0))
    }
    @Test fun calibrationAndWorkCostRespectTheirDenominators() {
        assertEquals(200.0, PlanterCalculations.calibration(2.0, 100.0)!!, 1e-8)
        assertEquals(200.0, PlanterCalculations.calibration(4.0, 200.0)!!, 1e-8)
        val cost = PlanterCalculations.workCost(300.0, 150.0, 50.0, 10.0, WorkCostBasis.HECTARES)!!
        assertEquals(500.0, cost.totalRM, 1e-8); assertEquals(50.0, cost.rateRM, 1e-8)
        assertEquals(0.0, PlanterCalculations.workCost(0.0, 0.0, 0.0, 10.0, WorkCostBasis.TONNES)!!.rateRM, 0.0)
        assertNull(PlanterCalculations.workCost(1.0, 0.0, 0.0, 0.5, WorkCostBasis.PALMS))
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { bad ->
            assertNull(PlanterCalculations.fertilizer(100.0, bad, 50.0))
            assertNull(PlanterCalculations.fertilizer(100.0, 5.0, bad))
            assertNull(PlanterCalculations.calibration(2.0, bad))
            assertNull(PlanterCalculations.workCost(1.0, 0.0, 0.0, bad, WorkCostBasis.HECTARES))
        }
    }
    @Test fun unitConversionRoundTripsAndDimensionalBoundaries() {
        assertEquals(2.4710538146716534, PlanterCalculations.convert(1.0, PlantationUnit.HECTARES, PlantationUnit.ACRES)!!, 1e-8)
        assertEquals(4046.8564224, PlanterCalculations.convert(1.0, PlantationUnit.ACRES, PlantationUnit.SQUARE_METRES)!!, 1e-8)
        assertEquals(0.3048, PlanterCalculations.convert(1.0, PlantationUnit.FEET, PlantationUnit.METRES)!!, 1e-8)
        assertEquals(1.0, PlanterCalculations.convert(1000.0, PlantationUnit.KILOGRAMS, PlantationUnit.TONNES)!!, 1e-8)
        assertEquals(0.0, PlanterCalculations.convert(0.0, PlantationUnit.HECTARES, PlantationUnit.ACRES)!!, 0.0)
        assertNull(PlanterCalculations.convert(1.0, PlantationUnit.METRES, PlantationUnit.KILOGRAMS))
        ConversionGroup.entries.forEach { group -> group.units.forEach { from -> group.units.forEach { to ->
            val converted = PlanterCalculations.convert(123.456, from, to)!!
            assertEquals(123.456, PlanterCalculations.convert(converted, to, from)!!, 1e-8)
        } } }
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { bad ->
            assertNull(PlanterCalculations.convert(bad, PlantationUnit.METRES, PlantationUnit.FEET))
            assertNull(PlanterCalculations.workCost(bad, 0.0, 0.0, 1.0, WorkCostBasis.HECTARES))
        }
        assertEquals(0.0, PlanterCalculations.input("0", allowZero = true)!!, 0.0)
    }
}
