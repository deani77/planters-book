import Foundation

struct EstateReferenceCell: Decodable {
    let text: String
    init(from decoder: Decoder) throws {
        let value = try decoder.singleValueContainer()
        if let string = try? value.decode(String.self) {
            guard string.count <= 1000 else { throw ProductionError.invalid("Estate cell is too long.") }
            text = string
        } else {
            let number = try value.decode(Double.self)
            guard number.isFinite else { throw ProductionError.invalid("Invalid estate number.") }
            text = number.rounded() == number ? String(format: "%.0f", locale: Locale(identifier: "en_US_POSIX"), number) : String(number)
        }
    }
}
struct EstateReferenceTable: Decodable, Identifiable {
    let id: String
    let columns: [String]
    let rows: [[EstateReferenceCell?]]
    var title: String {
        ["overview":"Estate Overview", "fields":"Field Information", "ages":"Age Profile", "soil":"Soil and Topography",
         "rainfall":"Rainfall", "abw":"Average Bunch Weight", "roads":"Road Information"][id] ?? id
    }
}
struct EstateReference: Decodable {
    let schemaVersion: Int
    let estateId: String
    let revision: Int
    let reportingPeriod: String
    let exportedAt: String
    let sections: [EstateReferenceTable]
    let yph: EstateYph?
    let cropProductionReport: EstateCropReport?
    let cropProductionReportHistory: [EstateCropReport]?
    let manuringReport: EstateManuringReport?
    let cropBudget: EstateCropBudget?
    let ffbRejectReport: EstateFfbRejectReport?
    static func parse(_ data: Data) throws -> EstateReference {
        guard data.count <= 2*1024*1024 else { throw ProductionError.invalid("Estate update exceeds 2 MB.") }
        guard String(data: data, encoding: .utf8) != nil else { throw ProductionError.invalid("Estate JSON must be UTF-8.") }
        let result = try JSONDecoder().decode(EstateReference.self, from: data)
        let expected: Set<String> = ["overview","fields","ages","soil","rainfall","abw","roads"]
        guard result.schemaVersion == 1, !result.estateId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, result.estateId.count <= 100, result.revision > 0,
              !result.reportingPeriod.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, result.reportingPeriod.count <= 100,
              !result.exportedAt.isEmpty, result.exportedAt.count <= 50,
              result.sections.count == 7, Set(result.sections.map(\.id)) == expected,
              result.sections.reduce(0, { $0 + $1.rows.count }) <= 10000 else {
            throw ProductionError.invalid("Invalid estate update.")
        }
        let timestamp = ISO8601DateFormatter()
        timestamp.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let fractional = timestamp.date(from: result.exportedAt)
        timestamp.formatOptions = [.withInternetDateTime]
        guard fractional != nil || timestamp.date(from: result.exportedAt) != nil else {
            throw ProductionError.invalid("Invalid exportedAt timestamp; include a timezone.")
        }
        for table in result.sections {
            guard (1...20).contains(table.columns.count), Set(table.columns).count == table.columns.count,
                  table.columns.allSatisfy({ !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && $0.count <= 80 }),
                  table.rows.allSatisfy({ $0.count == table.columns.count }) else {
                throw ProductionError.invalid("Invalid estate table.")
            }
        }
        return result
    }
}

// Optional report extensions keep older offline snapshots compatible.
struct EstateCropReport: Decodable {
    let reportDate: String
    let isCompleteMonth: Bool?
    let fields: [EstateCropField]?
    let totals: EstateCropTotals
}
struct EstateCropTotals: Decodable {
    let sentToMillMtdMtPerHa: Double?
    let hectaresToday: Double?
    let hectaresMtd: Double?
    let roundsMtd: Double?
    let mtPerHarvesterDayMtd: Double?
    let haPerHarvesterDayMtd: Double?
}
struct EstateManuringReport: Decodable {
    let reportDate: String
    let annualProgrammeMt: Double?
    let appliedYtdMt: Double?
}

struct EstateCropField: Decodable {
    let roundsMtd: Double?
    let mtPerHarvesterDayMtd: Double?
    let haPerHarvesterDayMtd: Double?
    let field: String
    let sentToMillMtdMt: Double?
    let sentToMillYtdMt: Double?
}
struct EstateCropBudget: Decodable {
    let year: Int
    let monthlyBudget: [EstateCropBudgetMonth]
}
struct EstateCropBudgetMonth: Decodable {
    let field: String
    let year: Int
    let month: Int
    let tonnes: Double?
}


struct EstateFfbRejectReport: Decodable {
    let reportDate: String
    let totals: EstateFfbRejectTotals
}
struct EstateFfbRejectTotals: Decodable {
    let rejectMtdMt: Double?
    let rejectMtdPercent: Double?
    let rejectYtdMt: Double?
    let rejectYtdPercent: Double?
}
