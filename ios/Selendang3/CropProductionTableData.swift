import Foundation

struct CropProductionTableRow: Identifiable {
    let field: String
    let area: String
    let values: [String]
    var id: String { field }
}
struct CropProductionTableData {
    let reference: EstateReference?
    let today: Date
    static let groups: [(String, [String])] = [
        ("Target · mt", ["This Month", "Year to Date", "Full Year"]),
        ("Target Yield · mt/ha", ["This Month", "Year to Date", "Full Year"]),
        ("Actual · mt", ["This Month", "Year to Date"]),
        ("Actual Yield · mt/ha", ["This Month", "Year to Date"]),
        ("Achievement · %", ["This Month", "Year to Date", "Full Year"])
    ]
    private var calendar: Calendar {
        var result = Calendar(identifier: .gregorian)
        result.timeZone = TimeZone(identifier: "Asia/Kuala_Lumpur")!
        return result
    }
    private var reportDate: Date? {
        guard let text = reference?.cropProductionReport?.reportDate else { return nil }
        let format = DateFormatter(); format.locale = Locale(identifier: "en_US_POSIX")
        format.calendar = calendar; format.timeZone = calendar.timeZone
        format.dateFormat = "yyyy-MM-dd"; format.isLenient = false
        guard let date = format.date(from: text), format.string(from: date) == text,
              date <= calendar.startOfDay(for: today) else { return nil }
        return date
    }
    var note: String {
        guard let date = reportDate else { return "Import a dated crop report in Settings → Estate Data Sync. Missing values remain Pending." }
        let format = DateFormatter(); format.locale = Locale(identifier: "en_US_POSIX")
        format.calendar = calendar; format.timeZone = calendar.timeZone; format.dateFormat = "MMMM yyyy"
        return format.string(from: date) + " · Actual through " + reference!.cropProductionReport!.reportDate +
            ". Full Year achievement = actual YTD / annual target."
    }
    private func valid(_ value: Double?) -> Double? {
        guard let value = value, value.isFinite, value >= 0 else { return nil }; return value
    }
    private func divide(_ numerator: Double?, _ denominator: Double?, scale: Double = 1) -> Double? {
        guard let n = valid(numerator), let d = valid(denominator), d > 0 else { return nil }
        return valid(n / d * scale)
    }
    private var sourceFields: [(String, Double?)] {
        guard let table = reference?.sections.first(where: { $0.id == "fields" }),
              let code = table.columns.firstIndex(of: "Field code"), let area = table.columns.firstIndex(of: "Area (ha)") else { return [] }
        let fields = table.rows.compactMap { row -> (String, Double?)? in
            guard let name = row[code]?.text, !name.isEmpty else { return nil }
            return (name, valid(Double(row[area]?.text.replacingOccurrences(of: ",", with: "") ?? "")))
        }
        guard Set(fields.map { $0.0 }).count == fields.count else { return [] }
        return fields
    }
    private func targets(_ field: String) -> [Double?] {
        guard let date = reportDate, let budget = reference?.cropBudget,
              budget.year == calendar.component(.year, from: date) else { return [nil, nil, nil] }
        let month = calendar.component(.month, from: date)
        let entries = budget.monthlyBudget.filter { $0.field == field && $0.year == budget.year }
        func sum(_ months: [Int]) -> Double? {
            var total = 0.0
            for month in months {
                let matches = entries.filter { $0.month == month }
                guard matches.count == 1, let value = valid(matches[0].tonnes) else { return nil }
                total += value
            }
            return valid(total)
        }
        return [sum([month]), sum(Array(1...month)), sum(Array(1...12))]
    }
    private func actual(_ field: String) -> [Double?] {
        guard reportDate != nil else { return [nil, nil] }
        let rows = reference?.cropProductionReport?.fields?.filter { $0.field == field } ?? []
        guard rows.count == 1 else { return [nil, nil] }
        return [valid(rows[0].sentToMillMtdMt), valid(rows[0].sentToMillYtdMt)]
    }
    private func row(_ field: String, area: Double?, target: [Double?], actual: [Double?]) -> CropProductionTableRow {
        let values = target + target.map { divide($0, area) } + actual + actual.map { divide($0, area) } +
            [divide(actual[0], target[0], scale: 100), divide(actual[1], target[1], scale: 100), divide(actual[1], target[2], scale: 100)]
        return CropProductionTableRow(field: field, area: EstateDashboardData.number(area), values: values.map(EstateDashboardData.number))
    }
    var rows: [CropProductionTableRow] {
        sourceFields.map { row($0.0, area: $0.1, target: targets($0.0), actual: actual($0.0)) }
    }
    var total: CropProductionTableRow? {
        let fields = sourceFields
        guard !fields.isEmpty else { return nil }
        func sum(_ values: [Double?]) -> Double? {
            guard values.allSatisfy({ $0 != nil }) else { return nil }
            return valid(values.compactMap { $0 }.reduce(0, +))
        }
        let target = (0..<3).map { index in sum(fields.map { targets($0.0)[index] }) }
        let actual = (0..<2).map { index in sum(fields.map { self.actual($0.0)[index] }) }
        return row("TOTAL", area: sum(fields.map { $0.1 }), target: target, actual: actual)
    }
}
