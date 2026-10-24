import Foundation

func estateFixture() -> [String: Any] {
    ["schemaVersion":1,"estateId":"selendang-3","revision":1,"reportingPeriod":"Fictional source",
     "exportedAt":"2026-01-01T00:00:00Z","sections":["overview","fields","ages","soil","rainfall","abw","roads"].map {
        ["id":$0,"columns":["Value"],"rows":[[NSNull()],[0],["Example"]]] as [String:Any]
     }]
}
func encoded(_ value: [String: Any]) throws -> Data { try JSONSerialization.data(withJSONObject: value) }
let sample = try EstateReference.parse(encoded(estateFixture()))
precondition(sample.sections.count == 7 && sample.revision == 1)
precondition(sample.sections[0].rows[0][0] == nil)
precondition(sample.sections[0].rows[1][0]?.text == "0")
var wrongEstate = estateFixture(); wrongEstate["estateId"] = "other"
do { _ = try EstateReference.parse(encoded(wrongEstate)); fatalError("Wrong estate accepted") } catch {}
var duplicateSections = estateFixture(); var sections = duplicateSections["sections"] as! [[String:Any]]
sections[1]["id"] = "overview"; duplicateSections["sections"] = sections
do { _ = try EstateReference.parse(encoded(duplicateSections)); fatalError("Duplicate section accepted") } catch {}
var invalidRows = estateFixture(); sections = invalidRows["sections"] as! [[String:Any]]
sections[0]["rows"] = [["A","B"]]; invalidRows["sections"] = sections
do { _ = try EstateReference.parse(encoded(invalidRows)); fatalError("Wrong row width accepted") } catch {}
let endpoint = try EstateSyncEndpoint(address:" https://example.com/v1/estates/selendang-3/snapshot\n",token:" example-token\n")
precondition(endpoint.token == "example-token")
for address in ["http://example.com/v1/estates/selendang-3/snapshot", "https://user:password@example.com/v1/estates/selendang-3/snapshot", "https://example.com/v1/estates/other/snapshot", "https://example.com/v1/estates/selendang-3/snapshot?token=secret"] {
    do { _ = try EstateSyncEndpoint(address:address,token:"example"); fatalError("Unsafe endpoint accepted") } catch {}
}
print("Estate reference validation checks passed")
