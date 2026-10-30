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
do { _ = try EstateReference.parse(encoded(duplicateProfiles)); fatalError("Duplicate profiles accepted") } catch {}

let validProfile = (withProfiles["fieldProfiles"] as! [[String:Any]])[0]
func assertRejectedProfiles(_ profiles: Any) throws {
    var fixture = estateFixture()
    fixture["fieldProfiles"] = profiles
    do { _ = try EstateReference.parse(encoded(fixture)); fatalError("Malformed profile accepted: \(profiles)") } catch {}
}
func changedProfile(_ change: (inout [String:Any]) -> Void) -> [[String:Any]] {
    var profile = validProfile
    change(&profile)
    return [profile]
}
try assertRejectedProfiles([validProfile, validProfile])
try assertRejectedProfiles([NSNull()])
try assertRejectedProfiles([String:Any]())
for key in ["reportYear", "blocks", "maturity", "divisionPalmHeightM"] {
    try assertRejectedProfiles(changedProfile { $0.removeValue(forKey: key) })
}
for year: Any in ["2027", 2027.5, 0, 10000, NSNull()] {
    try assertRejectedProfiles(changedProfile { $0["reportYear"] = year })
}
try assertRejectedProfiles(changedProfile { $0["divisionPalmHeightM"] = -1 })
try assertRejectedProfiles(changedProfile { $0["plantedAreaHa"] = "10" })
try assertRejectedProfiles(changedProfile { $0["blocks"] = [String:Any]() })
try assertRejectedProfiles(changedProfile { $0["blocks"] = [NSNull()] })
let validBlock = (validProfile["blocks"] as! [[String:Any]])[0]
for key in ["blockId", "blockName", "harvestingYear"] {
    var block = validBlock; block.removeValue(forKey: key)
    try assertRejectedProfiles(changedProfile { $0["blocks"] = [block] })
}
var wrongSoil = validBlock; wrongSoil["soilCode"] = 1
try assertRejectedProfiles(changedProfile { $0["blocks"] = [wrongSoil] })
try assertRejectedProfiles(changedProfile { $0["blocks"] = [validBlock, validBlock] })
for profiles: Any in [NSNull(), [[String:Any]]()] {
    var fixture = estateFixture(); fixture["fieldProfiles"] = profiles
    let parsed = try EstateReference.parse(encoded(fixture))
    precondition(parsed.sections.count == sample.sections.count)
}
let reloadedProfiles = try EstateReference.parse(encoded(withProfiles))
precondition(reloadedProfiles.fieldProfiles![0].reportYear == 2027)
precondition(reloadedProfiles.fieldProfiles![0].divisionPalmHeightM == 0)
print("Profile validation rejects malformed/duplicate facts and retains 2027, measured zero and legacy snapshots")
