import SwiftUI

struct PlanterToolsView: View {
    var body: some View {
        List {
            Section {
                NavigationLink { PlantDensityCalculatorView() } label: {
                    Label("Plant Density", systemImage: "leaf.fill")
                }
                NavigationLink { PumpDosageCalculatorView() } label: {
                    Label("Chemical Dosage per Pump", systemImage: "drop.fill")
                }
                NavigationLink { FertilizerCalculatorView() } label: { Label("Fertilizer Requirement", systemImage: "leaf.circle.fill") }
                NavigationLink { NozzleCalibrationView() } label: { Label("Nozzle Calibration", systemImage: "drop.circle.fill") }
                NavigationLink { WorkCostCalculatorView() } label: { Label("Work Costing", systemImage: "banknote.fill") }
                NavigationLink { PlantationUnitConverterView() } label: { Label("Unit Conversion", systemImage: "arrow.left.arrow.right") }
            } footer: {
                Text("Offline calculators. Inputs and results are temporary and do not change estate records. Use a decimal point; commas group thousands.")
            }
        }.estateScreen().navigationTitle("Planter Tools")
            .navigationBarTitleDisplayMode(.inline).toolbar(.visible, for: .navigationBar)
    }
}

private struct PlantDensityCalculatorView: View {
    @State private var distance = ""
    @State private var unit: PlantingDistanceUnit = .metres
    @State private var pattern: PlantingPattern = .triangular
    @FocusState private var focused: Bool
    private var result: PlantDensityResult? {
        guard let value = PlanterCalculations.input(distance) else { return nil }
        return PlanterCalculations.density(distance: value, unit: unit, pattern: pattern)
    }
    var body: some View {
        Form {
            Section("Planting layout") {
                Picker("Pattern", selection: $pattern) {
                    ForEach(PlantingPattern.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }.pickerStyle(.segmented)
                Picker("Distance unit", selection: $unit) {
                    ForEach(PlantingDistanceUnit.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }.pickerStyle(.segmented)
                TextField("Planting distance (\(unit == .metres ? "m" : "ft"))", text: $distance)
                    .keyboardType(.decimalPad).focused($focused)
                Text(pattern == .triangular ? "Distance between neighbouring palms along each side of an equilateral triangle." : "Equal distance between palms and between rows.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
            Section("Result") {
                if let result = result {
                    LabeledContent("Plant density", value: PlanterCalculations.number(result.palmsPerHectare) + " palms/ha")
                    LabeledContent("Row spacing", value: PlanterCalculations.number(result.rowSpacingMetres) + " m")
                    LabeledContent("Distance in metres", value: PlanterCalculations.number(result.metres) + " m")
                } else {
                    Text(distance.isEmpty ? "Enter a planting distance to calculate." : "Enter a valid distance greater than zero.")
                }
            }
            Section("Calculation") {
                Text(pattern == .triangular ? "Palms/ha = 10,000 ÷ (distance × row spacing). Triangular row spacing = distance × √3 ÷ 2." : "Palms/ha = 10,000 ÷ distance².")
                Text("Distances are converted to metres. This is theoretical density; roads, drains and unplanted areas are not deducted.")
            }.font(.footnote)
            Button("Clear input") { distance = ""; focused = false }
        }
        .scrollDismissesKeyboard(.interactively)
        .toolbar { ToolbarItemGroup(placement: .keyboard) { Spacer(); Button("Done") { focused = false } } }
        .estateScreen().navigationTitle("Plant Density").navigationBarTitleDisplayMode(.inline)
    }
}

private struct PumpDosageCalculatorView: View {
    @State private var rate = ""
    @State private var tank = ""
    @State private var spray = ""
    @State private var unit: ProductRateUnit = .litres
    @FocusState private var focused: Int?
    private var result: PumpDoseResult? {
        guard let r = PlanterCalculations.input(rate), let t = PlanterCalculations.input(tank), let s = PlanterCalculations.input(spray) else { return nil }
        return PlanterCalculations.dose(rate: r, unit: unit, tankLitres: t, sprayLitresPerHectare: s)
    }
    var body: some View {
        Form {
            Section("Recommended product rate") {
                Picker("Rate unit", selection: $unit) {
                    ForEach(ProductRateUnit.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }.pickerStyle(.segmented)
                TextField("Label product rate (\(unit.rawValue))", text: $rate).keyboardType(.decimalPad).focused($focused, equals: 0)
                Text("Use the formulated-product label rate per treated hectare, not the active-ingredient rate.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
            Section("Pump and nozzle") {
                TextField("Final solution per pump (L)", text: $tank).keyboardType(.decimalPad).focused($focused, equals: 1)
                TextField("Nozzle application volume (L/ha)", text: $spray).keyboardType(.decimalPad).focused($focused, equals: 2)
                Text("Enter calibrated spray volume in L/ha, not nozzle flow in L/min. Pump volume is the final total solution volume.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
            Section("Result") {
                if let result = result {
                    LabeledContent("Chemical per pump", value: PlanterCalculations.number(result.productPerPump) + " " + unit.doseUnit)
                    LabeledContent("Coverage per pump", value: PlanterCalculations.number(result.hectaresPerPump) + " ha")
                    LabeledContent("Pumps per hectare", value: PlanterCalculations.number(result.pumpsPerHectare) + " pumps/ha")
                } else {
                    Text(rate.isEmpty || tank.isEmpty || spray.isEmpty ? "Enter all three values to calculate." : "Check the rate and volumes. Values must be greater than zero; liquid product cannot exceed the final tank volume.")
                }
            }
            Section("Calculation") {
                Text("Chemical per pump = product rate per hectare × pump solution volume ÷ nozzle application volume. Liquid results are in mL/pump; solid results are in g/pump.")
            }.font(.footnote)
            Button("Clear inputs") { rate = ""; tank = ""; spray = ""; focused = nil }
        }
        .scrollDismissesKeyboard(.interactively)
        .toolbar { ToolbarItemGroup(placement: .keyboard) { Spacer(); Button("Done") { focused = nil } } }
        .estateScreen().navigationTitle("Chemical per Pump").navigationBarTitleDisplayMode(.inline)
    }
}
