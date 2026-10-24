import Foundation

func panel(_ title: String, _ values: [EstateDashboardPanel]) -> EstateDashboardPanel { values.first { $0.title == title }! }
func section(_ id: String, _ columns: [String], _ rows: [[Any]]) -> [String:Any] { ["id":id,"columns":columns,"rows":rows] }
let months = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"]
var overview: [[Any]] = [
    ["Workforce reporting period","2026-09"],["Workforce staff",2],["Workforce local workers",7],["Workforce foreign workers",3],
    ["Workforce total workers",10],["Workforce contract workers",4],["Ratios reporting period","2026-09"],
    ["Ratios immature area (ha)",0],["Ratios mature area (ha)",1000],["Ratios mature operations workers",8],
    ["FFB cost YTD reporting period","2026-09"],["FFB cost budget year",2026],
    ["FFB cost operation actual YTD (RM/tonne)",123],["FFB cost operation budget year (RM/tonne)",100],
    ["FFB cost general charges actual YTD (RM/tonne)",45],["FFB cost general charges budget year (RM/tonne)",40]
]
let rainfall: [[Any]] = months.prefix(9).map { [2026,$0,10] } + [[2026,"Oct",999],[2026,"TOTAL (source)",9999]] + months.map { [2025,$0,1] }
var sections: [[String:Any]] = [
    section("overview",["Item","Details"],overview),
    section("fields",["Field code","Area (ha)","Total palms"],[["A",100,1000],["B",900,9000]]),
    section("ages",["Field / category","Age / years"],[["A","3 years"],["B","24 years"]]),
    section("soil",["Type","Category","Area (ha)","Share (%)","Suitability class","Class reference"],[
        ["Flat","Terrain",1000,100,NSNull(),NSNull()],["Hilly","Terrain",0,0,NSNull(),NSNull()],["Undulating","Terrain",0,0,NSNull(),NSNull()],
        ["Example series","Soil series",1000,100,"III","Fictional reference"]]),
    section("rainfall",["Year","Month / total","Rainfall (mm)"],rainfall),section("abw",["Value"],[]),section("roads",["Value"],[])
]
var fixture: [String:Any] = ["schemaVersion":1,"estateId":"selendang-3","revision":1,"reportingPeriod":"Fictional data",
    "exportedAt":"2026-10-01T00:00:00Z","sections":sections,"yph":["reportingYear":2026,"throughMonth":9,"matureAreaHa":1000,
    "actualYtdTonnes":25000,"budgetYtdTonnes":20000,"fullYearBudgetTonnes":30000,
    "history":[["year":2025,"actualYph":18],["year":2024,"actualYph":15],["year":2021,"actualYph":999]]]]
let october = ISO8601DateFormatter().date(from:"2026-10-22T00:00:00Z")!
func dashboard(_ fixture: [String:Any], _ date: Date = october) throws -> [EstateDashboardPanel] {
    let data = try JSONSerialization.data(withJSONObject:fixture)
    return EstateDashboardData(reference:try EstateReference.parse(data),today:date).panels
}
let panels = try dashboard(fixture)
precondition(panel("Age Profile",panels).rows[0][1] == "100.00")
precondition(panel("Age Profile",panels).rows[2][2] == "90.00%")
precondition(panel("Average SPH",panels).rows[0][1] == "10.00")
precondition(panel("Topography",panels).rows.count == 3)
precondition(panel("Soil Type",panels).rows[0][3] == "Class III*")
precondition(panel("Rainfall",panels).rows.count == 5 && panel("Rainfall",panels).rows[0][2] == "90.00")
precondition(panel("Rainfall",panels).rows[1][2] == "12.00")
precondition(panel("Workforce",panels).rows.map { $0[1] } == ["2","7","3","4","16"])
precondition(panel("Ratios",panels).rows[1][2] == "1:125.00")
precondition(panel("YPH",panels).rows[0][0] == "2022" && panel("YPH",panels).rows.last![1] == "25.00")
precondition(panel("Kos FFB/TAN",panels).rows[0] == ["Operation Cost","123.00","100.00"])
precondition(panel("Kos FFB/TAN",panels).bars.count == 4)
let november = ISO8601DateFormatter().date(from:"2026-11-01T00:00:00Z")!
let novemberPanels = try dashboard(fixture,november)
precondition(panel("Rainfall",novemberPanels).rows[0][2] == "1089.00")
let january = ISO8601DateFormatter().date(from:"2027-01-01T00:00:00Z")!
let januaryPanels = try dashboard(fixture,january)
precondition(panel("Rainfall",januaryPanels).rows[0][2] == "Pending")
sections[4] = section("rainfall",["Year","Month / total","Rainfall (mm)"],rainfall + [[2026,"Jan",0]])
fixture["sections"] = sections
let duplicatePanels = try dashboard(fixture)
precondition(panel("Rainfall",duplicatePanels).rows[0][2] == "Pending")
overview[9] = ["Ratios mature operations workers",0]
sections[0] = section("overview",["Item","Details"],overview); fixture["sections"] = sections
let zeroWorkerPanels = try dashboard(fixture)
precondition(panel("Ratios",zeroWorkerPanels).rows[1][2] == "Pending")
print("Dashboard model checks passed")

let homeReference = try EstateReference.parse(JSONSerialization.data(withJSONObject: fixture))
let home = EstateDashboardData(reference: homeReference, today: october).homeRows
precondition(home.count == 3)
precondition(home[0].note.contains("Sep 2026: Pending") && home[0].note.contains("Oct 2026 MTD: Pending"))
precondition(home[0].note.contains("YTD through Sep 2026: 25.00"))
precondition(home[1].note == "Weeding: Pending · Manuring: Pending")
precondition(home[2].note.contains("Operation: 123.00 · GC: 45.00"))
let newYearHome = EstateDashboardData(reference: homeReference, today: january).homeRows
precondition(newYearHome[0].note.contains("Dec 2026: Pending"))
precondition(newYearHome[0].note.contains("YTD 2027: Pending"))
precondition(newYearHome[2].note.contains("Operation: Pending · GC: Pending"))
let noHomeData = EstateDashboardData(reference: nil, today: october).homeRows
precondition(noHomeData[0].note.contains("YTD 2026: Pending"))
let august = ISO8601DateFormatter().date(from: "2026-08-01T00:00:00Z")!
let futureHome = EstateDashboardData(reference: homeReference, today: august).homeRows
precondition(futureHome[0].note.contains("YTD 2026: Pending"))
precondition(futureHome[2].note.contains("Operation: Pending · GC: Pending"))
print("Home summary checks passed")

precondition(home[0].note.contains("YTD vs Year Budget: 83.33%"))
precondition(!home[0].note.contains("125.00%"))
precondition(newYearHome[0].note.hasSuffix("YTD vs Year Budget: Pending"))
precondition(noHomeData[0].note.hasSuffix("YTD vs Year Budget: Pending"))
precondition(futureHome[0].note.hasSuffix("YTD vs Year Budget: Pending"))
func budgetProgressNote(actual: Any, annual: Any) throws -> String {
    var input = fixture
    var crop = input["yph"] as! [String: Any]
    crop["actualYtdTonnes"] = actual
    crop["fullYearBudgetTonnes"] = annual
    crop["budgetYtdTonnes"] = NSNull()
    input["yph"] = crop
    let reference = try EstateReference.parse(JSONSerialization.data(withJSONObject: input))
    return EstateDashboardData(reference: reference, today: october).homeRows[0].note
}
let zeroAnnual = try budgetProgressNote(actual: 25000, annual: 0)
let missingAnnual = try budgetProgressNote(actual: 25000, annual: NSNull())
let missingActual = try budgetProgressNote(actual: NSNull(), annual: 30000)
let zeroActual = try budgetProgressNote(actual: 0, annual: 30000)
let aboveAnnual = try budgetProgressNote(actual: 60000, annual: 30000)
precondition(zeroAnnual.hasSuffix("YTD vs Year Budget: Pending"))
precondition(missingAnnual.hasSuffix("YTD vs Year Budget: Pending"))
precondition(missingActual.hasSuffix("YTD vs Year Budget: Pending"))
precondition(zeroActual.hasSuffix("YTD vs Year Budget: 0.00%"))
precondition(aboveAnnual.hasSuffix("YTD vs Year Budget: 200.00%"))
print("Annual crop budget achievement checks passed")

precondition(estateFieldCodes(homeReference) == ["A", "B"])
let fieldA = estateFieldTables(homeReference, code: "A")
precondition(Set(fieldA.map(\.id)) == Set(["fields", "ages"]))
precondition(fieldA.allSatisfy { $0.rows.count == 1 })
precondition(estateFieldTables(homeReference, code: "UNKNOWN").isEmpty)
precondition(estateFieldCodes(nil).isEmpty)
print("Field record isolation checks passed")


let cards = EstateDashboardData(reference: homeReference, today: october).operationalCards
precondition(cards.count == 7)
precondition(cards.map(\.title) == ["Crop Production", "Harvesting Coverage", "Harvester Productivity", "Manuring", "Weeding", "Pest & Disease", "Estate Cost"])
precondition(cards[0].rows[2] == ["Actual YTD", "25.00", "mt/ha"])
precondition(cards[0].rows[3] == ["YTD / Year Budget", "83.33", "%"])
precondition(cards[0].note == "YTD through Sep 2026")
precondition(cards[6].rows[0] == ["Operation Cost", "123.00", "RM/mt"])
precondition(cards[6].rows[1] == ["General Charges", "45.00", "RM/mt"])
precondition(cards[6].note == "YTD through Sep 2026")
precondition(cards[1...5].allSatisfy { $0.rows.allSatisfy { $0[1] == "Pending" } })
for date in [january, august] {
    let stale = EstateDashboardData(reference: homeReference, today: date).operationalCards
    precondition(stale[0].rows[2][1] == "Pending" && stale[0].rows[3][1] == "Pending")
    precondition(stale[6].rows[0][1] == "Pending" && stale[6].rows[1][1] == "Pending")
}
let emptyCards = EstateDashboardData(reference: nil, today: october).operationalCards
precondition(emptyCards.allSatisfy { $0.rows.allSatisfy { $0[1] == "Pending" && !$0[2].isEmpty } })
print("Grouped dashboard checks passed")

precondition(cards[4].rows.map { $0[0] } == ["Circle", "Selective"])

// Complete monthly reports are independent of sparse daily production drafts.
var reportFixture = fixture
reportFixture["cropProductionReport"] = ["reportDate":"2026-10-21", "totals":[
    "sentToMillMtdMtPerHa":1.25,"hectaresToday":100,"hectaresMtd":2000,"roundsMtd":2,
    "mtPerHarvesterDayMtd":2.5,"haPerHarvesterDayMtd":3]] as [String:Any]
reportFixture["cropProductionReportHistory"] = [["reportDate":"2026-09-30","isCompleteMonth":true,
    "totals":["sentToMillMtdMtPerHa":2.75]] as [String:Any]]
reportFixture["manuringReport"] = ["reportDate":"2026-10-22","annualProgrammeMt":1000,"appliedYtdMt":800] as [String:Any]
func reportCards(_ input: [String:Any], date: Date = october) throws -> [EstateDashboardPanel] {
    EstateDashboardData(reference: try EstateReference.parse(JSONSerialization.data(withJSONObject:input)), today:date).operationalCards
}
let populated = try reportCards(reportFixture)
precondition(populated[0].rows[0][1] == "2.75" && populated[0].rows[1][1] == "1.25")
precondition(populated[1].rows.map { $0[1] } == ["100.00","2000.00","2.00"])
precondition(populated[1].note == "Report: 2026-10-21")
precondition(populated[2].rows.map { $0[1] } == ["2.50","3.00"])
precondition(populated[3].rows[0][1] == "80.00")
precondition(populated[4].rows.allSatisfy { $0[1] == "Pending" })
let later = try reportCards(reportFixture,date:november)
precondition(later[0].rows[1][1] == "Pending" && later[1].rows.allSatisfy { $0[1] == "Pending" })
let futureReports = try reportCards(reportFixture,date:august)
precondition(futureReports[0].rows[1][1] == "Pending" && futureReports[3].rows[0][1] == "Pending")
var bad = reportFixture
bad["manuringReport"] = ["reportDate":"2026-10-22","annualProgrammeMt":0,"appliedYtdMt":800] as [String:Any]
let zeroProgramme = try reportCards(bad)
precondition(zeroProgramme[3].rows[0][1] == "Pending")
bad["cropProductionReportHistory"] = [["reportDate":"2026-09-29","isCompleteMonth":true,"totals":["sentToMillMtdMtPerHa":2.75]] as [String:Any]]
let incompleteMonth = try reportCards(bad)
precondition(incompleteMonth[0].rows[0][1] == "Pending")
bad["cropProductionReport"] = ["reportDate":"2026-10-21","totals":["hectaresMtd":-1]] as [String:Any]
let negativeArea = try reportCards(bad)
precondition(negativeArea[1].rows[1][1] == "Pending")
print("Report-backed dashboard and stale/missing-data checks passed")

var tableFixture = reportFixture
var tableReport = tableFixture["cropProductionReport"] as! [String:Any]
tableReport["fields"] = [["field":"A","sentToMillMtdMt":120,"sentToMillYtdMt":1200],
                         ["field":"B","sentToMillMtdMt":180,"sentToMillYtdMt":2400]]
tableFixture["cropProductionReport"] = tableReport
let budgetMonths: [[String:Any]] = ["A", "B"].flatMap { field in
    (1...12).map { month in ["field":field,"year":2026,"month":month,"tonnes":field == "A" ? 100 : 200] as [String:Any] }
}
tableFixture["cropBudget"] = ["year":2026,"monthlyBudget":budgetMonths]
func cropTable(_ input: [String:Any], date: Date = october) throws -> CropProductionTableData {
    CropProductionTableData(reference:try EstateReference.parse(JSONSerialization.data(withJSONObject:input)), today:date)
}
let table = try cropTable(tableFixture)
precondition(table.rows.count == 2 && table.rows[0].values.count == 13)
precondition(Array(table.rows[0].values.prefix(3)) == ["100.00","1000.00","1200.00"])
precondition(table.rows[0].values[6] == "120.00" && table.rows[0].values[12] == "100.00")
precondition(table.total?.values[10] == "100.00" && table.total?.values[11] == "120.00")
precondition(table.total?.values[8] == "0.30") // area-weighted, not sum of field yields
var missingTable = tableFixture
missingTable.removeValue(forKey:"cropBudget")
let missingBudgetTable = try cropTable(missingTable)
precondition(missingBudgetTable.rows[0].values[0] == "Pending")
precondition(missingBudgetTable.rows[0].values[6] == "120.00")
var incompleteBudget = tableFixture
incompleteBudget["cropBudget"] = ["year":2026,"monthlyBudget":Array(budgetMonths.dropLast())]
let incompleteTable = try cropTable(incompleteBudget)
precondition(incompleteTable.total?.values[2] == "Pending")
let futureTable = try cropTable(tableFixture,date:august)
precondition(futureTable.rows[0].values.allSatisfy { $0 == "Pending" })
precondition(!CropProductionTableData.groups.flatMap { $0.1 }.contains("Today"))
print("Field crop table totals, target periods and missing-data checks passed")

var routedFixture = tableFixture
var routedSections = routedFixture["sections"] as! [[String:Any]]
routedSections[1] = section("fields", ["Field code","Area (ha)","Total palms","Sent to mill MTD (mt)","Harvested area MTD (ha)","Crop report dates / previous month"],
    [["A",100,1000,120,200,"2026-10-21"],["B",900,9000,180,300,"2026-10-21"]])
routedSections[5] = section("abw", ["Field","Average bunch weight (kg/bunch)"], [["A",18],["B",20]])
routedFixture["sections"] = routedSections
let routedReference = try EstateReference.parse(JSONSerialization.data(withJSONObject:routedFixture))
let basicField = estateFieldTables(routedReference,code:"A")
let cropField = estateFieldCropTables(routedReference,code:"A")
precondition(basicField.map(\.id) == ["fields","ages"])
precondition(basicField[0].columns == ["Field code","Area (ha)","Total palms"])
precondition(cropField.map(\.id) == ["fields","abw"])
precondition(cropField[0].columns == ["Field code","Sent to mill MTD (mt)","Harvested area MTD (ha)","Crop report dates / previous month"])
precondition(cropField.allSatisfy { $0.rows.count == 1 && $0.rows[0][0]?.text == "A" })
precondition(estateFieldCropTables(routedReference,code:"UNKNOWN").isEmpty)
precondition(routedReference.sections[1].columns.count == 6) // Projection leaves stored records intact.
print("Basic field versus crop record routing checks passed")
