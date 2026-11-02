package com.selendang3.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selendang3.app.data.*
import java.util.Locale

@Composable
internal fun FertilizerCalculator() {
    var palms by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var bag by rememberSaveable { mutableStateOf("") }
    Text("Fertilizer Requirement", style = MaterialTheme.typography.titleLarge)
    NumberInput("Palm population (palms)", palms) { palms = it }
    NumberInput("Application rate (kg/palm)", rate) { rate = it }
    NumberInput("Bag weight (kg/bag)", bag) { bag = it }
    Text("Enter your approved application rate and actual bag weight.", style = MaterialTheme.typography.bodySmall)
    val values = listOf(palms, rate, bag).map { PlanterCalculations.input(it) }
    val result = if (values.all { it != null }) PlanterCalculations.fertilizer(values[0]!!, values[1]!!, values[2]!!) else null
    ToolResult {
        if (result != null) {
            Text("Total fertilizer: ${PlanterCalculations.number(result.kilograms)} kg")
            Text("Total fertilizer: ${PlanterCalculations.number(result.tonnes)} mt")
            Text("Equivalent bags: ${PlanterCalculations.number(result.equivalentBags)} bags")
            Text("Whole bags needed: ${String.format(Locale.US, "%.0f", result.wholeBags)} bags")
            Text("Balance after application: ${PlanterCalculations.number(result.balanceKilograms)} kg")
        } else Text("Enter all three values. Palms must be a whole number; rate and bag weight must be greater than zero.")
    }
    Text("Total kg = palms × kg/palm. Equivalent bags = total kg ÷ kg/bag. Whole bags round up; balance is unused fertilizer after the calculated application.", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { palms = ""; rate = ""; bag = "" }) { Text("Clear inputs") }
}

@Composable
internal fun NozzleCalibrationCalculator() {
    var litres by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    Text("Nozzle Calibration", style = MaterialTheme.typography.titleLarge)
    NumberInput("Water used on test area (L)", litres) { litres = it }
    NumberInput("Test area (m²)", area) { area = it }
    Text("Use plain water over a measured area at your normal walking speed and operating pressure.", style = MaterialTheme.typography.bodySmall)
    val l = PlanterCalculations.input(litres); val a = PlanterCalculations.input(area)
    val result = if (l != null && a != null) PlanterCalculations.calibration(l, a) else null
    ToolResult {
        if (result != null) Text("Spray volume: ${PlanterCalculations.number(result)} L/ha", style = MaterialTheme.typography.titleMedium)
        else Text("Enter measured water use and test area, both greater than zero.")
    }
    Text("L/ha = water used (L) ÷ test area (m²) × 10,000. Use this L/ha value in Chemical Dosage per Pump.", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { litres = ""; area = "" }) { Text("Clear inputs") }
}

@Composable
internal fun WorkCostCalculator() {
    var labour by rememberSaveable { mutableStateOf("0") }
    var material by rememberSaveable { mutableStateOf("0") }
    var other by rememberSaveable { mutableStateOf("0") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var basis by rememberSaveable { mutableStateOf(WorkCostBasis.HECTARES) }
    Text("Work Costing", style = MaterialTheme.typography.titleLarge)
    EstatePillSelector(WorkCostBasis.entries, basis, { basis = it; quantity = "" }) { it.label }
    NumberInput(basis.quantityLabel, quantity) { quantity = it }
    NumberInput("Labour cost (RM)", labour) { labour = it }
    NumberInput("Material cost (RM)", material) { material = it }
    NumberInput("Other cost (RM)", other) { other = it }
    val costs = listOf(labour, material, other).map { PlanterCalculations.input(it, allowZero = true) }
    val q = PlanterCalculations.input(quantity)
    val result = if (costs.all { it != null } && q != null) PlanterCalculations.workCost(costs[0]!!, costs[1]!!, costs[2]!!, q, basis) else null
    ToolResult {
        if (result != null) {
            Text("Total cost: RM ${PlanterCalculations.number(result.totalRM)}")
            Text("Unit cost: RM ${PlanterCalculations.number(result.rateRM)}/${basis.unit}")
        } else Text("Enter non-negative costs and a quantity greater than zero. Palm quantities must be whole numbers.")
    }
    Text("Total cost = labour + materials + other costs. Unit cost = total cost ÷ work quantity.", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { labour = "0"; material = "0"; other = "0"; quantity = "" }) { Text("Clear inputs") }
}

@Composable
internal fun PlantationUnitConverter() {
    var text by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf(ConversionGroup.AREA) }
    var from by rememberSaveable { mutableStateOf(PlantationUnit.HECTARES) }
    var to by rememberSaveable { mutableStateOf(PlantationUnit.ACRES) }
    Text("Unit Conversion", style = MaterialTheme.typography.titleLarge)
    EstatePillSelector(ConversionGroup.entries, group, { group = it; from = it.units[0]; to = it.units[1] }) { it.label }
    UnitSelector("From", from, group.units) { from = it }
    UnitSelector("To", to, group.units) { to = it }
    NumberInput("Value (${from.symbol})", text) { text = it }
    OutlinedButton(onClick = { val previous = from; from = to; to = previous }) { Text("Swap units") }
    val value = PlanterCalculations.input(text, allowZero = true)
    val result = value?.let { PlanterCalculations.convert(it, from, to) }
    ToolResult {
        if (result != null) Text("Converted value: ${String.format(Locale.US, "%.8g", result)} ${to.symbol}")
        else Text("Enter a non-negative value to convert.")
    }
    Text("1 ha = 10,000 m². 1 international acre = 4,046.8564224 m². 1 ft = 0.3048 m. 1 mt = 1,000 kg.", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = { text = "" }) { Text("Clear input") }
}

@Composable
private fun UnitSelector(title: String, value: PlantationUnit, units: List<PlantationUnit>, choose: (PlantationUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$title: ${value.label}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit -> DropdownMenuItem(text = { Text(unit.label) }, onClick = { choose(unit); expanded = false }) }
        }
    }
}
