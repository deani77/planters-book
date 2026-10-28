import Foundation

func estateFixture() -> [String: Any] {
    ["schemaVersion":1,"estateId":"local-estate","revision":1,"reportingPeriod":"Fictional source",
     "exportedAt":"2026-01-01T00:00:00Z","sections":["overview","fields","ages","soil","rainfall","abw","roads"].map {
        ["id":$0,"columns":["Value"],"rows":[[NSNull()],[0],["Example"]]] as [String:Any]
     }]
}
func encoded(_ value: [String: Any]) throws -> Data { try JSONSerialization.data(withJSONObject: value) }
let sample = try EstateReference.parse(encoded(estateFixture()))
precondition(sample.sections.count == 7 && sample.revision == 1)
precondition(sample.sections[0].rows[0][0] == nil)
precondition(sample.sections[0].rows[1][0]?.text == "0")
var customEstate = estateFixture(); customEstate["estateId"] = "my-estate"
let customReference = try EstateReference.parse(encoded(customEstate))
precondition(customReference.estateId == "my-estate")
var blankEstate = estateFixture(); blankEstate["estateId"] = " "
do { _ = try EstateReference.parse(encoded(blankEstate)); fatalError("Blank estate accepted") } catch {}
var duplicateSections = estateFixture(); var sections = duplicateSections["sections"] as! [[String:Any]]
sections[1]["id"] = "overview"; duplicateSections["sections"] = sections
do { _ = try EstateReference.parse(encoded(duplicateSections)); fatalError("Duplicate section accepted") } catch {}
var invalidRows = estateFixture(); sections = invalidRows["sections"] as! [[String:Any]]
sections[0]["rows"] = [["A","B"]]; invalidRows["sections"] = sections
do { _ = try EstateReference.parse(encoded(invalidRows)); fatalError("Wrong row width accepted") } catch {}
print("Estate reference validation checks passed")

precondition(sample.fieldProfiles == nil)
var withProfiles = estateFixture()
withProfiles["fieldProfiles"] = [["fieldCode":"EXAMPLE", "reportYear":2027,
    "plantedAreaHa":10.5, "unplantedAreaHa":1.25, "divisionStandsPerHa":120,
    "divisionPalmHeightM":0, "maturity":"MATURED", "blocks":[[
        "blockId":"EXAMPLE01", "blockName":"01", "plantedAreaHa":10.5,
        "unplantedAreaHa":1.25, "soilCode":NSNull(), "harvestingYear":2015,
        "sourceNote":NSNull()]]] as [String:Any]]
let sourceProfile = try EstateReference.parse(encoded(withProfiles)).fieldProfiles!.first!
precondition(sourceProfile.divisionPalmHeightM == 0)
precondition(sourceProfile.blocks[0].soilCode == nil)
precondition(sourceProfile.blocks[0].harvestingYear == 2015)
precondition(sourceProfile.blocks[0].sourceNote == nil)
print("Optional field profiles preserve source zero, missing soil and harvesting year")

let importedProfiles = try EstateReference.parse(encoded(withProfiles))
precondition(importedProfiles.sourceFieldProfile("EXAMPLE") != nil)
precondition(importedProfiles.sourceFieldProfile("UNKNOWN") == nil)
var duplicateProfiles = withProfiles
duplicateProfiles["fieldProfiles"] = (withProfiles["fieldProfiles"] as! [[String:Any]]) + (withProfiles["fieldProfiles"] as! [[String:Any]])
let duplicateReference = try EstateReference.parse(encoded(duplicateProfiles))
precondition(duplicateReference.sourceFieldProfile("EXAMPLE") == nil)
