import Foundation
let parityData = try Data(contentsOf: URL(fileURLWithPath: "sample-data/architecture-parity.json"))
let parityReference = try EstateReference.parse(parityData)
precondition(estateFieldCodes(parityReference) == ["USER-A"])
let parityTables = estateFieldTables(parityReference, code: "USER-A")
precondition(parityTables.map(\.id) == ["Field Overview", "fields", "ages", "abw"])
precondition(parityTables[1].rows[0].map { $0?.text } == ["USER-A", "OLD-A", "2010-01-01"])
precondition(parityTables.last!.rows[0].last!?.text == "0")
let parityDate = ISO8601DateFormatter().date(from: "2026-10-21T00:00:00Z")!
let parityMetrics = CropProductionTableData(reference: parityReference, today: parityDate).rows[0].metrics
precondition(parityMetrics.actual(.mtd, yield: true) == 2)
precondition(abs(parityMetrics.achievement(.annual)! - 200.0 / 120.0 * 100) < 1e-9)
let visibleSummary = CropProductionTableData(reference: parityReference, today: parityDate).fieldSummary(code: "USER-A")!
let visibleGroups = fieldCropReportGroups(visibleSummary)
precondition(visibleSummary.note.contains("2026-10-20"))
precondition(visibleGroups.map(\.title) == ["Field Profile", "Crop Budget and Actual", "Harvesting", "Harvester Productivity"])
precondition(visibleGroups[1].rows.contains { $0.label == "Actual MTD (mt)" && $0.value == "20.00" })
