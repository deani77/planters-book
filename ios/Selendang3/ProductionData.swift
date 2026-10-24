import Foundation

let productionFields = ["PM04D", "PM05E", "PM06F", "PM08G", "PM10K", "PM11M", "PM15P"]
let productionAliases = Dictionary(uniqueKeysWithValues: zip(
    ["0256PM0401", "0256PM0502", "0256PM0603", "0256PM0804", "0256PM1006", "0256PM1108", "0256PM1511"], productionFields))

func productionDateKey(_ date: Date) -> String {
    let formatter = DateFormatter()
    formatter.calendar = Calendar(identifier: .gregorian)
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.timeZone = .current
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.string(from: date)
}

enum ProductionError: LocalizedError {
    case invalid(String)
    var errorDescription: String? { if case .invalid(let text) = self { return text }; return nil }
}

struct ProductionDraft: Codable, Identifiable {
    let date: String
    let field: String
    let harvested: Double?
    let sentToMill: Double?
    let backlog: Double?
    let hectares: Double?
    let harvesters: Int?
    var id: String { "\(date)/\(field)" }

    func validate() throws {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.isLenient = false
        guard productionFields.contains(field), let parsed = formatter.date(from: date), productionDateKey(parsed) == date else {
            throw ProductionError.invalid("Choose a valid field and reporting date.")
        }
        guard [harvested, sentToMill, backlog, hectares].allSatisfy({ $0 == nil || ($0!.isFinite && $0! >= 0 && $0! <= 1e9) }),
              harvesters == nil || harvesters! >= 0 && harvesters! <= 1000000000 else {
            throw ProductionError.invalid("Values must be finite and non-negative; harvesters must be a whole number.")
        }
        guard [harvested, sentToMill, backlog, hectares].contains(where: { $0 != nil }) || harvesters != nil else {
            throw ProductionError.invalid("Enter at least one value.")
        }
    }
}

struct ProductionTotal {
    let value: Double?
    let reportedDays: Int
    let savedDays: Int
    func label(_ unit: String) -> String {
        guard let value = value else { return "Pending" }
        return String(format: "%.2f", locale: Locale(identifier: "en_US_POSIX"), value) + " \(unit) · \(reportedDays)/\(savedDays) saved days reported"
    }
}

struct ProductionSummary {
    let days: Int
    let harvested: ProductionTotal
    let sentToMill: ProductionTotal
    let hectares: ProductionTotal
    let harvesterDays: ProductionTotal
    let backlog: Double?
    let backlogDate: String?
}

func productionSummary(_ records: [ProductionDraft], field: String, from: String, through: String) -> ProductionSummary {
    let rows = records.filter { $0.field == field && $0.date >= from && $0.date <= through }.sorted { $0.date < $1.date }
    func total(_ metric: (ProductionDraft) -> Double?) -> ProductionTotal {
        let values = rows.compactMap(metric)
        return ProductionTotal(value: values.isEmpty ? nil : values.reduce(0, +), reportedDays: values.count, savedDays: rows.count)
    }
    return ProductionSummary(days: rows.count, harvested: total { $0.harvested }, sentToMill: total { $0.sentToMill },
        hectares: total { $0.hectares }, harvesterDays: total { $0.harvesters.map(Double.init) }, backlog: rows.last?.backlog, backlogDate: rows.last?.date)
}

struct ProductionDraftFile: Codable {
    let version: Int
    let rows: [ProductionDraft]
    func validate() throws {
        guard version == 1 else { throw ProductionError.invalid("Unsupported production draft format.") }
        try rows.forEach { try $0.validate() }
        guard Set(rows.map(\.id)).count == rows.count else { throw ProductionError.invalid("Duplicate field/date drafts.") }
    }
}
