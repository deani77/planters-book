import Foundation

enum PlantingPattern: String, CaseIterable { case triangular = "Triangular", square = "Square" }
enum PlantingDistanceUnit: String, CaseIterable { case metres = "Metres", feet = "Feet" }
enum ProductRateUnit: String, CaseIterable {
    case litres = "L/ha", millilitres = "mL/ha", kilograms = "kg/ha", grams = "g/ha"
    var liquid: Bool { self == .litres || self == .millilitres }
    var factor: Double { self == .litres || self == .kilograms ? 1000 : 1 }
    var doseUnit: String { liquid ? "mL/pump" : "g/pump" }
}
struct PlantDensityResult {
    let metres: Double
    let rowSpacingMetres: Double
    let palmsPerHectare: Double
}
struct PumpDoseResult {
    let productPerPump: Double
    let hectaresPerPump: Double
    let pumpsPerHectare: Double
}
enum PlanterCalculations {
    static func input(_ text: String, allowZero: Bool = false) -> Double? {
        let clean = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard clean.range(of: "^([0-9]+([.][0-9]*)?|[.][0-9]+|[0-9]{1,3}(,[0-9]{3})+([.][0-9]*)?)$", options: .regularExpression) != nil,
              let value = Double(clean.replacingOccurrences(of: ",", with: "")), value.isFinite, (allowZero ? value >= 0 : value > 0) else { return nil }
        return value
    }
    static func density(distance: Double, unit: PlantingDistanceUnit, pattern: PlantingPattern) -> PlantDensityResult? {
        guard distance.isFinite, distance > 0 else { return nil }
        let metres = distance * (unit == .feet ? 0.3048 : 1)
        let rowSpacing = metres * (pattern == .triangular ? sqrt(3) / 2 : 1)
        let density = 10000 / (metres * rowSpacing)
        guard metres.isFinite, rowSpacing.isFinite, rowSpacing > 0, density.isFinite, density > 0 else { return nil }
        return PlantDensityResult(metres: metres, rowSpacingMetres: rowSpacing, palmsPerHectare: density)
    }
    static func dose(rate: Double, unit: ProductRateUnit, tankLitres: Double, sprayLitresPerHectare: Double) -> PumpDoseResult? {
        guard [rate, tankLitres, sprayLitresPerHectare].allSatisfy({ $0.isFinite && $0 > 0 }) else { return nil }
        let coverage = tankLitres / sprayLitresPerHectare
        let dose = rate * unit.factor * coverage
        let pumps = sprayLitresPerHectare / tankLitres
        guard [coverage, dose, pumps].allSatisfy({ $0.isFinite && $0 > 0 }),
              !unit.liquid || dose / 1000 <= tankLitres else { return nil }
        return PumpDoseResult(productPerPump: dose, hectaresPerPump: coverage, pumpsPerHectare: pumps)
    }
    static func number(_ value: Double) -> String {
        String(format: value < 0.01 || value >= 1e9 ? "%.6g" : "%.2f", locale: Locale(identifier: "en_US_POSIX"), value)
    }
}

struct FertilizerResult {
    let kilograms: Double
    let tonnes: Double
    let equivalentBags: Double
    let wholeBags: Double
    let balanceKilograms: Double
}
enum WorkCostBasis: String, CaseIterable {
    case hectares = "Hectares", palms = "Palms", tonnes = "Tonnes"
    var unit: String { self == .hectares ? "ha" : self == .palms ? "palm" : "mt" }
    var quantityLabel: String { self == .hectares ? "Work area (ha)" : self == .palms ? "Palms treated (palms)" : "Crop handled (mt)" }
}
struct WorkCostResult { let totalRM: Double; let rateRM: Double }
enum ConversionGroup: String, CaseIterable {
    case area = "Area", length = "Length", mass = "Mass"
    var units: [PlantationUnit] { PlantationUnit.allCases.filter { $0.group == self } }
}
enum PlantationUnit: String, CaseIterable {
    case hectares = "Hectares (ha)", acres = "Acres (ac)", squareMetres = "Square metres (m²)"
    case metres = "Metres (m)", feet = "Feet (ft)", kilograms = "Kilograms (kg)", tonnes = "Tonnes (mt)"
    var group: ConversionGroup {
        switch self { case .hectares, .acres, .squareMetres: return .area; case .metres, .feet: return .length; default: return .mass }
    }
    var factor: Double {
        switch self { case .hectares: return 10000; case .acres: return 4046.8564224; case .feet: return 0.3048; case .tonnes: return 1000; default: return 1 }
    }
    var symbol: String {
        switch self { case .hectares: return "ha"; case .acres: return "ac"; case .squareMetres: return "m²"; case .metres: return "m"; case .feet: return "ft"; case .kilograms: return "kg"; case .tonnes: return "mt" }
    }
}
extension PlanterCalculations {
    static func fertilizer(palms: Double, kilogramsPerPalm: Double, bagKilograms: Double) -> FertilizerResult? {
        guard [palms, kilogramsPerPalm, bagKilograms].allSatisfy({ $0.isFinite && $0 > 0 }), palms.rounded() == palms else { return nil }
        let kilograms = palms * kilogramsPerPalm
        let bags = kilograms / bagKilograms
        let whole = ceil(bags)
        let balance = max(0, whole * bagKilograms - kilograms)
        guard [kilograms, bags, whole, balance].allSatisfy({ $0.isFinite }), kilograms > 0, bags > 0 else { return nil }
        return FertilizerResult(kilograms: kilograms, tonnes: kilograms / 1000, equivalentBags: bags, wholeBags: whole, balanceKilograms: balance)
    }
    static func calibration(waterLitres: Double, testAreaSquareMetres: Double) -> Double? {
        guard [waterLitres, testAreaSquareMetres].allSatisfy({ $0.isFinite && $0 > 0 }) else { return nil }
        let result = waterLitres / testAreaSquareMetres * 10000
        return result.isFinite && result > 0 ? result : nil
    }
    static func workCost(labourRM: Double, materialRM: Double, otherRM: Double, quantity: Double, basis: WorkCostBasis) -> WorkCostResult? {
        guard [labourRM, materialRM, otherRM].allSatisfy({ $0.isFinite && $0 >= 0 }), quantity.isFinite, quantity > 0,
              basis != .palms || quantity.rounded() == quantity else { return nil }
        let total = labourRM + materialRM + otherRM
        let rate = total / quantity
        guard total.isFinite, rate.isFinite else { return nil }
        return WorkCostResult(totalRM: total, rateRM: rate)
    }
    static func convert(_ value: Double, from: PlantationUnit, to: PlantationUnit) -> Double? {
        guard value.isFinite, value >= 0, from.group == to.group else { return nil }
        let result = value * (from.factor / to.factor)
        return result.isFinite ? result : nil
    }
}
