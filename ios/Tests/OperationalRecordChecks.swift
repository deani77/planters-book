import Foundation
let crop = MonthlyCropRecord(fieldCode: "NEW-A", year: 2026, month: 2, through: "2026-02-28", actualMt: 0, budgetMt: 10, areaHa: 5, complete: true)
try crop.validate()
precondition(crop.actualMtPerHa == 0 && crop.achievementPercent == 0)
try AbwRecord(fieldCode: "NEW-A", year: 2026, basis: .ytd, through: "2026-06-30", kgPerBunch: 18).validate()
try AbwRecord(fieldCode: "NEW-A", year: 2026, basis: .annual, through: "2026-12-31", kgPerBunch: 18).validate()
do { try AbwRecord(fieldCode: "NEW-A", year: 2026, basis: .annual, through: "2026-06-30", kgPerBunch: 18).validate(); fatalError("Partial annual ABW accepted") } catch {}
let bags = UpkeepRecord(id: "1", date: "2026-01-01", fieldCode: nil, operation: .manuring, quantity: 2, unit: .bags, bagWeightKg: nil, round: 1, notes: "")
try bags.validate()
precondition(bags.kilograms == nil)
let weighted = UpkeepRecord(id: "2", date: "2026-01-01", fieldCode: nil, operation: .manuring, quantity: 2, unit: .bags, bagWeightKg: 50, round: 1, notes: "")
try weighted.validate()
precondition(weighted.kilograms == 100)
