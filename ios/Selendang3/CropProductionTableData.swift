import Foundation

enum CropPeriod: Int, CaseIterable { case mtd, ytd, annual }
struct CropMetrics {
    let areaHa: Double?
    let targetMtdMt: Double?
    let targetYtdMt: Double?
    let targetAnnualMt: Double?
    let actualMtdMt: Double?
    let actualYtdMt: Double?
    private func divide(_ n: Double?, _ d: Double?, scale: Double = 1) -> Double? {
        guard let n, let d, n.isFinite, n >= 0, d.isFinite, d > 0 else { return nil }
        let result = n / d * scale
        return result.isFinite ? result : nil
    }
    func target(_ period: CropPeriod, yield: Bool = false) -> Double? {
        let tonnes: Double?
        switch period { case .mtd: tonnes = targetMtdMt; case .ytd: tonnes = targetYtdMt; case .annual: tonnes = targetAnnualMt }
        return yield ? divide(tonnes, areaHa) : tonnes
    }
    func actual(_ period: CropPeriod, yield: Bool = false) -> Double? {
        let tonnes: Double?
        switch period { case .mtd: tonnes = actualMtdMt; case .ytd: tonnes = actualYtdMt; case .annual: tonnes = nil }
        return yield ? divide(tonnes, areaHa) : tonnes
    }
    func achievement(_ period: CropPeriod) -> Double? { divide(period == .mtd ? actualMtdMt : actualYtdMt, target(period), scale: 100) }
}
struct CropProductionTableRow: Identifiable {
    let field: String
    let metrics: CropMetrics
    var area: String { EstateDashboardData.number(metrics.areaHa) }
    // Compatibility adapter; new presentation code uses named metrics.
    var values: [String] {
        let targets = CropPeriod.allCases.map { metrics.target($0) }
        let yields = CropPeriod.allCases.map { metrics.target($0, yield: true) }
        let actuals = [CropPeriod.mtd, .ytd].map { metrics.actual($0) }
        let actualYields = [CropPeriod.mtd, .ytd].map { metrics.actual($0, yield: true) }
        let achievements = CropPeriod.allCases.map { metrics.achievement($0) }
        return (targets + yields + actuals + actualYields + achievements).map(EstateDashboardData.number)
    }
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
        guard let date = reportDate else { return "Import a dated crop report in Settings → Estate Data Management. Missing values remain Pending." }
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
        return CropProductionTableRow(field: field, metrics: CropMetrics(areaHa: area, targetMtdMt: target[0], targetYtdMt: target[1], targetAnnualMt: target[2], actualMtdMt: actual[0], actualYtdMt: actual[1]))
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

struct FieldCropSummaryRow: Identifiable {
    let label: String
    let value: String
    var id: String { label }
}
struct FieldCropSummaryGroup: Identifiable {
    let title: String
    let rows: [FieldCropSummaryRow]
    var note: String = ""
    var id: String { title }
}
struct FieldCropSummary {
    let note: String
    let crewNote: String
    let groups: [FieldCropSummaryGroup]
}
extension CropProductionTableData {
    private func abwSummary(code: String) -> FieldCropSummaryGroup {
        let table = reference?.sections.first { $0.id == "abw" }
        let columns = table?.columns ?? []
        let codeIndex = columns.firstIndex(of: "Field code")
        let yearIndex = columns.firstIndex(of: "Year")
        let valueIndex = columns.firstIndex(of: "YTD ABW (kg/bunch)")
        let currentYear = calendar.component(.year, from: today)
        func cell(_ row: [EstateReferenceCell?], _ index: Int?) -> String? {
            guard let index = index, row.indices.contains(index) else { return nil }
            return row[index]?.text.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        func year(_ row: [EstateReferenceCell?]) -> Int? {
            guard let text = cell(row, yearIndex), let year = Int(text), (1900...currentYear).contains(year) else { return nil }
            return year
        }
        let rows = table?.rows ?? []
        let latest = rows.compactMap(year).max() ?? currentYear
        let values = [latest, latest - 1].map { target -> FieldCropSummaryRow in
            let matches = rows.filter { cell($0, codeIndex) == code && year($0) == target }
            let value = matches.count == 1 ? cell(matches[0], valueIndex).flatMap(Double.init).flatMap(valid) : nil
            return FieldCropSummaryRow(label: "\(target) YTD (kg/bunch)", value: EstateDashboardData.number(value))
        }
        return FieldCropSummaryGroup(title: "Average Bunch Weight", rows: values,
            note: "Cumulative/YTD source averages. Reporting cutoff unconfirmed; these figures may cover a different period from the crop report.")
    }
    private func previousYearCropRows(code: String) -> [FieldCropSummaryRow] {
        let previousYear = reportDate.flatMap { calendar.date(byAdding: .year, value: -1, to: $0) }
        let cutoff = previousYear.flatMap { calendar.dateInterval(of: .month, for: $0)?.start }
            .flatMap { calendar.date(byAdding: .day, value: -1, to: $0) }
        let format = DateFormatter(); format.locale = Locale(identifier: "en_US_POSIX")
        format.calendar = calendar; format.timeZone = calendar.timeZone; format.dateFormat = "yyyy-MM-dd"
        func yieldAt(_ date: Date?) -> String {
            guard let date = date else { return "Pending" }
            let reports = (reference?.cropProductionReportHistory ?? []).filter { $0.reportDate == format.string(from: date) }
            guard reports.count == 1 else { return "Pending" }
            let fields = (reports[0].fields ?? []).filter { $0.field == code }
            guard fields.count == 1 else { return "Pending" }
            return EstateDashboardData.number(divide(fields[0].sentToMillYtdMt, fields[0].areaHa))
        }
        return [
            FieldCropSummaryRow(label: "Last Year Same-Period YTD" + (cutoff.map { " · through " + format.string(from: $0) } ?? "") + " (mt/ha)", value: yieldAt(cutoff))]
    }
    func fieldSummary(code: String) -> FieldCropSummary? {
        guard let field = sourceFields.first(where: { $0.0 == code }) else { return nil }
        let values = row(code, area: field.1, target: targets(code), actual: actual(code))
        let matches = reportDate != nil ? (reference?.cropProductionReport?.fields?.filter { $0.field == code } ?? []) : []
        let source = matches.count == 1 ? matches[0] : nil
        let ratio = field.1.flatMap { valid($0 / 21) }
        let crew = ratio.map { String(format: "%.0f", locale: Locale(identifier: "en_US_POSIX"), ceil($0)) } ?? "Pending"
        let crewNote = ratio.map { "Field hectares ÷ 21 ha/harvester = " + EstateDashboardData.number($0) + " harvesters. Whole-worker requirement is rounded up." } ?? "A verified field area is required for the 21 ha/harvester ratio."
        let note = reportDate != nil ? "Crop report: " + reference!.cropProductionReport!.reportDate + ". Budgets use the same report month." : "Crop report pending. Import estate data in Settings → Estate Data Management."
        return FieldCropSummary(note: note, crewNote: crewNote, groups: [
            FieldCropSummaryGroup(title: "Field Profile", rows: [
                FieldCropSummaryRow(label: "Field Code", value: code),
                FieldCropSummaryRow(label: "Hectare (ha)", value: values.area)]),
            FieldCropSummaryGroup(title: "Crop Budget and Actual", rows: [
                FieldCropSummaryRow(label: "Budget This Month (mt)", value: EstateDashboardData.number(values.metrics.targetMtdMt)),
                FieldCropSummaryRow(label: "Budget YTD (mt)", value: EstateDashboardData.number(values.metrics.targetYtdMt)),
                FieldCropSummaryRow(label: "Year Budget (mt)", value: EstateDashboardData.number(values.metrics.targetAnnualMt)),
                FieldCropSummaryRow(label: "Actual MTD (mt)", value: EstateDashboardData.number(values.metrics.actualMtdMt)),
                FieldCropSummaryRow(label: "Actual YTD (mt)", value: EstateDashboardData.number(values.metrics.actualYtdMt))] + previousYearCropRows(code: code),
                note: "Last-year yield uses each field’s historical sent-to-mill tonnes and hectares. Same-period YTD runs through the last completed month, one year earlier. Missing or ambiguous records remain Pending."),
            abwSummary(code: code),
            FieldCropSummaryGroup(title: "Harvesting", rows: [
                FieldCropSummaryRow(label: "Required Harvesters (people)", value: crew),
                FieldCropSummaryRow(label: "Harvesting Rounds This Month (rounds)", value: EstateDashboardData.number(valid(source?.roundsMtd)))]),
            FieldCropSummaryGroup(title: "Harvester Productivity", rows: [
                FieldCropSummaryRow(label: "Tonnes per mandays (mt/manday)", value: EstateDashboardData.number(valid(source?.mtPerHarvesterDayMtd))),
                FieldCropSummaryRow(label: "Hectare per mandays (ha/manday)", value: EstateDashboardData.number(valid(source?.haPerHarvesterDayMtd)))])
        ])
    }
}



// Rendering and checks consume the same report groups; ABW belongs to Field Details.
func fieldCropReportGroups(_ summary: FieldCropSummary) -> [FieldCropSummaryGroup] {
    summary.groups.filter { $0.title != "Average Bunch Weight" }
}
