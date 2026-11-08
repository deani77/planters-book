import SwiftUI

private struct ToolNumberField: View {
    let title: String
    @Binding var text: String
    let index: Int
    var focused: FocusState<Int?>.Binding
    var body: some View {
        TextField(title, text: $text).keyboardType(.decimalPad).focused(focused, equals: index)
    }
}
private struct ToolKeyboard: ViewModifier {
    var focused: FocusState<Int?>.Binding
    func body(content: Content) -> some View {
        content.scrollDismissesKeyboard(.interactively)
            .toolbar { ToolbarItemGroup(placement: .keyboard) { Spacer(); Button("Done") { focused.wrappedValue = nil } } }
    }
}
struct FertilizerCalculatorView: View {
    @State private var palms = ""
    @State private var rate = ""
    @State private var bag = ""
    @FocusState private var focused: Int?
    private var result: FertilizerResult? {
        guard let p = PlanterCalculations.input(palms), let r = PlanterCalculations.input(rate), let b = PlanterCalculations.input(bag) else { return nil }
        return PlanterCalculations.fertilizer(palms: p, kilogramsPerPalm: r, bagKilograms: b)
    }
    var body: some View {
        Form {
            Section("Application inputs") {
                ToolNumberField(title: "Palm population (palms)", text: $palms, index: 0, focused: $focused)
                ToolNumberField(title: "Application rate (kg/palm)", text: $rate, index: 1, focused: $focused)
                ToolNumberField(title: "Bag weight (kg/bag)", text: $bag, index: 2, focused: $focused)
                Text("Enter your approved application rate and actual bag weight.").font(.footnote).foregroundStyle(.secondary)
            }
            Section("Requirement") {
                if let r = result {
                    LabeledContent("Total fertilizer", value: PlanterCalculations.number(r.kilograms) + " kg")
                    LabeledContent("Total fertilizer", value: PlanterCalculations.number(r.tonnes) + " mt")
                    LabeledContent("Equivalent bags", value: PlanterCalculations.number(r.equivalentBags) + " bags")
                    LabeledContent("Whole bags needed", value: String(format: "%.0f", r.wholeBags) + " bags")
                    LabeledContent("Balance after application", value: PlanterCalculations.number(r.balanceKilograms) + " kg")
                } else { Text("Enter all three values. Palms must be a whole number; rate and bag weight must be greater than zero.") }
            }
            Section("Calculation") {
                Text("Total kg = palms × kg/palm. Equivalent bags = total kg ÷ kg/bag. Whole bags round up; balance is the unused fertilizer after the calculated application.")
            }.font(.footnote)
            Button("Clear inputs") { palms = ""; rate = ""; bag = ""; focused = nil }
        }.modifier(ToolKeyboard(focused: $focused)).estateScreen().navigationTitle("Fertilizer Requirement").navigationBarTitleDisplayMode(.inline)
    }
}
struct NozzleCalibrationView: View {
    @State private var litres = ""
    @State private var area = ""
    @FocusState private var focused: Int?
    private var result: Double? {
        guard let l = PlanterCalculations.input(litres), let a = PlanterCalculations.input(area) else { return nil }
        return PlanterCalculations.calibration(waterLitres: l, testAreaSquareMetres: a)
    }
    var body: some View {
        Form {
            Section("Measured test") {
                ToolNumberField(title: "Water used on test area (L)", text: $litres, index: 0, focused: $focused)
                ToolNumberField(title: "Test area (m²)", text: $area, index: 1, focused: $focused)
                Text("Use plain water over a measured area at your normal walking speed and operating pressure.").font(.footnote).foregroundStyle(.secondary)
            }
            Section("Calibrated application volume") {
                if let r = result { LabeledContent("Spray volume", value: PlanterCalculations.number(r) + " L/ha") }
                else { Text("Enter measured water use and test area, both greater than zero.") }
                Text("Use this L/ha value in Chemical Dosage per Pump.").font(.footnote).foregroundStyle(.secondary)
            }
            Section("Calculation") { Text("L/ha = water used (L) ÷ test area (m²) × 10,000.") }.font(.footnote)
            Button("Clear inputs") { litres = ""; area = ""; focused = nil }
        }.modifier(ToolKeyboard(focused: $focused)).estateScreen().navigationTitle("Nozzle Calibration").navigationBarTitleDisplayMode(.inline)
    }
}
struct WorkCostCalculatorView: View {
    @State private var labour = "0"
    @State private var material = "0"
    @State private var other = "0"
    @State private var quantity = ""
    @State private var basis: WorkCostBasis = .hectares
    @FocusState private var focused: Int?
    private var result: WorkCostResult? {
        guard let l = PlanterCalculations.input(labour, allowZero: true), let m = PlanterCalculations.input(material, allowZero: true),
              let o = PlanterCalculations.input(other, allowZero: true), let q = PlanterCalculations.input(quantity) else { return nil }
        return PlanterCalculations.workCost(labourRM: l, materialRM: m, otherRM: o, quantity: q, basis: basis)
    }
    var body: some View {
        Form {
            Section("Cost basis") {
                Picker("Work quantity", selection: Binding(get: { basis }, set: { basis = $0; quantity = "" })) { ForEach(WorkCostBasis.allCases, id: \.self) { Text($0.rawValue).tag($0) } }.pickerStyle(.segmented)
                ToolNumberField(title: basis.quantityLabel, text: $quantity, index: 0, focused: $focused)
            }
            Section("Costs") {
                ToolNumberField(title: "Labour cost (RM)", text: $labour, index: 1, focused: $focused)
                ToolNumberField(title: "Material cost (RM)", text: $material, index: 2, focused: $focused)
                ToolNumberField(title: "Other cost (RM)", text: $other, index: 3, focused: $focused)
            }
            Section("Result") {
                if let r = result {
                    LabeledContent("Total cost", value: "RM " + PlanterCalculations.number(r.totalRM))
                    LabeledContent("Unit cost", value: "RM " + PlanterCalculations.number(r.rateRM) + "/" + basis.unit)
                } else { Text("Enter non-negative costs and a quantity greater than zero. Palm quantities must be whole numbers.") }
            }
            Section("Calculation") { Text("Total cost = labour + materials + other costs. Unit cost = total cost ÷ work quantity.") }.font(.footnote)
            Button("Clear inputs") { labour = "0"; material = "0"; other = "0"; quantity = ""; focused = nil }
        }.modifier(ToolKeyboard(focused: $focused)).estateScreen().navigationTitle("Work Costing").navigationBarTitleDisplayMode(.inline)
    }
}
struct PlantationUnitConverterView: View {
    @State private var value = ""
    @State private var group: ConversionGroup = .area
    @State private var from: PlantationUnit = .hectares
    @State private var to: PlantationUnit = .acres
    @FocusState private var focused: Int?
    private var result: Double? {
        guard let v = PlanterCalculations.input(value, allowZero: true) else { return nil }
        return PlanterCalculations.convert(v, from: from, to: to)
    }
    var body: some View {
        Form {
            Section("Conversion") {
                Picker("Category", selection: Binding(get: { group }, set: { group = $0; from = $0.units[0]; to = $0.units[1] })) {
                    ForEach(ConversionGroup.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                }.pickerStyle(.segmented)
                Picker("From", selection: $from) { ForEach(group.units, id: \.self) { Text($0.rawValue).tag($0) } }
                Picker("To", selection: $to) { ForEach(group.units, id: \.self) { Text($0.rawValue).tag($0) } }
                ToolNumberField(title: "Value (\(from.symbol))", text: $value, index: 0, focused: $focused)
                Button("Swap units") { let previous = from; from = to; to = previous }
            }
            Section("Result") {
                if let r = result { LabeledContent("Converted value", value: String(format: "%.8g", locale: Locale(identifier: "en_US_POSIX"), r) + " " + to.symbol) }
                else { Text("Enter a non-negative value to convert.") }
            }
            Section("Definitions") { Text("1 ha = 10,000 m². 1 international acre = 4,046.8564224 m². 1 ft = 0.3048 m. 1 mt = 1,000 kg.") }.font(.footnote)
            Button("Clear input") { value = ""; focused = nil }
        }.modifier(ToolKeyboard(focused: $focused)).estateScreen().navigationTitle("Unit Conversion").navigationBarTitleDisplayMode(.inline)
    }
}
