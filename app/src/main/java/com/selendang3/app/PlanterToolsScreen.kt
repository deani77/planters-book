package com.selendang3.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.selendang3.app.data.*

@Composable
fun PlanterToolsScreen() {
    var tool by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (tool.isEmpty()) {
            FilledTonalButton(onClick = { tool = "density" }, modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight)) { Text("Plant Density") }
            FilledTonalButton(onClick = { tool = "dose" }, modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight)) { Text("Chemical Dosage per Pump") }
            FilledTonalButton(onClick = { tool = "fertilizer" }, modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight)) { Text("Fertilizer Requirement") }
            FilledTonalButton(onClick = { tool = "calibration" }, modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight)) { Text("Nozzle Calibration") }
            FilledTonalButton(onClick = { tool = "cost" }, modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight)) { Text("Work Costing") }
            FilledTonalButton(onClick = { tool = "units" }, modifier = Modifier.fillMaxWidth().heightIn(min = EstateControlHeight)) { Text("Unit Conversion") }
            Text("Offline calculators. Inputs and results are temporary and do not change estate records. Use a decimal point; commas group thousands.", style = MaterialTheme.typography.bodySmall)
        } else {
            TextButton(onClick = { tool = "" }) { Text("‹ All Tools") }
            when (tool) {
                "density" -> DensityCalculator()
                "dose" -> PumpDosageCalculator()
                "fertilizer" -> FertilizerCalculator()
                "calibration" -> NozzleCalibrationCalculator()
                "cost" -> WorkCostCalculator()
                "units" -> PlantationUnitConverter()
            }
        }
    }
}

@Composable
private fun DensityCalculator() {
    var distance by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(PlantingDistanceUnit.METRES) }
    var pattern by rememberSaveable { mutableStateOf(PlantingPattern.TRIANGULAR) }
    Text("Plant Density", style = MaterialTheme.typography.titleLarge)
    EstatePillSelector(PlantingPattern.entries, pattern, { pattern = it }) { it.label }
    EstatePillSelector(PlantingDistanceUnit.entries, unit, { unit = it }) { it.label }
    NumberInput("Planting distance (${if (unit == PlantingDistanceUnit.METRES) "m" else "ft"})", distance) { distance = it }
    Text(if (pattern == PlantingPattern.TRIANGULAR) "Distance between neighbouring palms along each side of an equilateral triangle." else "Equal distance between palms and between rows.", style = MaterialTheme.typography.bodySmall)
    val result = PlanterCalculations.input(distance)?.let { PlanterCalculations.density(it, unit, pattern) }
    ToolResult {
        if (result != null) {
            Text("Plant density: ${PlanterCalculations.number(result.palmsPerHectare)} palms/ha", style = MaterialTheme.typography.titleMedium)
            Text("Row spacing: ${PlanterCalculations.number(result.rowSpacingMetres)} m")
            Text("Distance in metres: ${PlanterCalculations.number(result.metres)} m")
        } else Text(if (distance.isEmpty()) "Enter a planting distance to calculate." else "Enter a valid distance greater than zero.")
    }
    Text(if (pattern == PlantingPattern.TRIANGULAR) "Palms/ha = 10,000 ÷ (distance × row spacing). Triangular row spacing = distance × √3 ÷ 2." else "Palms/ha = 10,000 ÷ distance².", style = MaterialTheme.typography.bodySmall)
    Text("Distances are converted to metres. This is theoretical density; roads, drains and unplanted areas are not deducted.", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { distance = "" }) { Text("Clear input") }
}

@Composable
private fun PumpDosageCalculator() {
    var rate by rememberSaveable { mutableStateOf("") }
    var tank by rememberSaveable { mutableStateOf("") }
    var spray by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf(ProductRateUnit.LITRES) }
    Text("Chemical per Pump", style = MaterialTheme.typography.titleLarge)
    EstatePillSelector(ProductRateUnit.entries, unit, { unit = it }) { it.label }
    NumberInput("Label product rate (${unit.label})", rate) { rate = it }
    Text("Use the formulated-product label rate per treated hectare, not the active-ingredient rate.", style = MaterialTheme.typography.bodySmall)
    NumberInput("Final solution per pump (L)", tank) { tank = it }
    NumberInput("Nozzle application volume (L/ha)", spray) { spray = it }
    Text("Enter calibrated spray volume in L/ha, not nozzle flow in L/min. Pump volume is the final total solution volume.", style = MaterialTheme.typography.bodySmall)
    val values = listOf(rate, tank, spray).map { PlanterCalculations.input(it) }
    val result = if (values.all { it != null }) PlanterCalculations.dose(values[0]!!, unit, values[1]!!, values[2]!!) else null
    ToolResult {
        if (result != null) {
            Text("Chemical per pump: ${PlanterCalculations.number(result.productPerPump)} ${unit.doseUnit}", style = MaterialTheme.typography.titleMedium)
            Text("Coverage per pump: ${PlanterCalculations.number(result.hectaresPerPump)} ha")
            Text("Pumps per hectare: ${PlanterCalculations.number(result.pumpsPerHectare)} pumps/ha")
        } else Text(if (listOf(rate, tank, spray).any { it.isEmpty() }) "Enter all three values to calculate." else "Check the rate and volumes. Values must be greater than zero; liquid product cannot exceed the final tank volume.")
    }
    Text("Chemical per pump = product rate per hectare × pump solution volume ÷ nozzle application volume. Liquid results are in mL/pump; solid results are in g/pump.", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { rate = ""; tank = ""; spray = "" }) { Text("Clear inputs") }
}

@Composable
internal fun NumberInput(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}
@Composable
internal fun ToolResult(body: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Result", style = MaterialTheme.typography.titleMedium); body() }
    }
}
