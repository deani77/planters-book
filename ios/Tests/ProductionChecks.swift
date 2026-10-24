import Foundation

func row(_ date: String, _ mill: Double? = 2, _ backlog: Double? = nil, field: String = "PM04D") -> ProductionDraft {
    ProductionDraft(date: date, field: field, harvested: nil, sentToMill: mill, backlog: backlog, hectares: 3, harvesters: 4)
}
let records = [row("2025-12-31"), row("2026-01-31"), row("2026-02-01", 2, 6),
               row("2026-02-10", 0), row("2026-02-11"), row("2026-02-05", field: "PM05E")]
let month = productionSummary(records, field: "PM04D", from: "2026-02-01", through: "2026-02-10")
let year = productionSummary(records, field: "PM04D", from: "2026-01-01", through: "2026-02-10")
precondition(month.days == 2 && month.sentToMill.value == 2 && year.sentToMill.value == 4)
precondition(month.harvesterDays.value == 8 && month.harvested.value == nil)
precondition(month.backlog == nil && month.backlogDate == "2026-02-10")
precondition(productionAliases["0256PM0401"] == "PM04D")
try row("2026-02-01", 0).validate()
let duplicate = ProductionDraftFile(version: 1, rows: [row("2026-02-01"), row("2026-02-01")])
do { try duplicate.validate(); fatalError("Duplicate dates accepted") } catch {}
do { try row("2026-02-01", -1).validate(); fatalError("Negative value accepted") } catch {}
do { try row("2026-02-30").validate(); fatalError("Invalid date accepted") } catch {}
let encoded = try JSONEncoder().encode(ProductionDraftFile(version: 1, rows: [row("2026-02-01", 0)]))
let decoded = try JSONDecoder().decode(ProductionDraftFile.self, from: encoded)
try decoded.validate()
precondition(decoded.rows[0].sentToMill == 0 && decoded.rows[0].harvested == nil)
print("Production model checks passed")

