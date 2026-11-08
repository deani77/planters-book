import Foundation

// New domain values complement the legacy import envelope; no existing record is rewritten.
private func recordField(_ value: String) -> Bool {
    !value.isEmpty && value == value.trimmingCharacters(in: .whitespacesAndNewlines) && value.count <= 100 && value.rangeOfCharacter(from: .controlCharacters) == nil
}
private func recordMeasure(_ value: Double?) -> Bool { value == nil || (value!.isFinite && value! >= 0) }
struct FieldDefinition {
    let code: String
    let plantedAreaHa: Double?
    let perimeterAreaHa: Double?
    let standingPalms: Int?
    let plantingYear: Int?
    let maturityYear: Int?
    let variety: String?
    let terrain: String?
    let soil: String?
    func validate() throws {
        guard recordField(code), [plantedAreaHa, perimeterAreaHa].allSatisfy(recordMeasure),
              standingPalms == nil || standingPalms! >= 0,
              [plantingYear, maturityYear].allSatisfy({ $0 == nil || (1...9999).contains($0!) }),
              plantingYear == nil || maturityYear == nil || maturityYear! >= plantingYear! else { throw ProductionError.invalid("Invalid field definition.") }
    }
}
struct MonthlyCropRecord {
    let fieldCode: String
    let year: Int
    let month: Int
    let through: String
    let actualMt: Double?
    let budgetMt: Double?
    let areaHa: Double?
    let complete: Bool
    func validate() throws {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.isLenient = false
        guard let date = formatter.date(from: through), formatter.string(from: date) == through else { throw ProductionError.invalid("Invalid crop cutoff.") }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        guard recordField(fieldCode), (1...9999).contains(year), (1...12).contains(month),
              calendar.component(.year, from: date) == year, calendar.component(.month, from: date) == month,
              !complete || calendar.component(.day, from: date) == calendar.range(of: .day, in: .month, for: date)?.count,
              [actualMt, budgetMt, areaHa].allSatisfy(recordMeasure) else { throw ProductionError.invalid("Invalid monthly crop record.") }
    }
    var actualMtPerHa: Double? { guard let actual = actualMt, let area = areaHa, area > 0 else { return nil }; let result = actual / area; return result.isFinite ? result : nil }
    var achievementPercent: Double? { guard let actual = actualMt, let budget = budgetMt, budget > 0 else { return nil }; let result = actual / budget * 100; return result.isFinite ? result : nil }
}
enum AbwBasis: String { case annual, ytd }
struct AbwRecord {
    let fieldCode: String
    let year: Int
    let basis: AbwBasis
    let through: String
    let kgPerBunch: Double?
    func validate() throws {
        try MonthlyCropRecord(fieldCode: fieldCode, year: year, month: Int(through.dropFirst(5).prefix(2)) ?? 0, through: through,
                              actualMt: kgPerBunch, budgetMt: nil, areaHa: nil, complete: false).validate()
        guard basis != .annual || through == String(format: "%04d-12-31", year) else { throw ProductionError.invalid("Annual ABW requires a year-end cutoff.") }
    }
}
enum UpkeepOperation: String { case weeding, manuring, pestDisease, water, road, other }
enum WorkUnit: String { case hectares, kilograms, bags, kilometres, hours, count }
struct UpkeepRecord {
    let id: String
    let date: String
    let fieldCode: String?
    let operation: UpkeepOperation
    let quantity: Double?
    let unit: WorkUnit
    let bagWeightKg: Double?
    let round: Int?
    let notes: String
    func validate() throws {
        try MonthlyCropRecord(fieldCode: fieldCode ?? "estate", year: Int(date.prefix(4)) ?? 0,
            month: Int(date.dropFirst(5).prefix(2)) ?? 0, through: date, actualMt: nil, budgetMt: nil, areaHa: nil, complete: false).validate()
        guard !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, [quantity, bagWeightKg].allSatisfy(recordMeasure),
              round == nil || round! > 0, bagWeightKg == nil || (unit == .bags && bagWeightKg! > 0) else { throw ProductionError.invalid("Invalid upkeep record.") }
    }
    var kilograms: Double? {
        if unit == .kilograms { return quantity }
        guard unit == .bags, let amount = quantity, let weight = bagWeightKg else { return nil }
        let result = amount * weight
        return result.isFinite ? result : nil
    }
}
