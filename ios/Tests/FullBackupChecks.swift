import Foundation
let backup = FullBackup(estate: nil, previousEstate: nil, production: Data("{\"version\":2,\"rows\":[]}".utf8),
    settings: Data("{\"estateName\":\"Example\",\"extension\":{\"zero\":0}}".utf8), mapPDF: Data("%PDF-example".utf8))
let parsed = try FullBackup.decode(backup.encode())
precondition(parsed.estate == nil && parsed.mapPDF == backup.mapPDF)
let settings = try JSONSerialization.jsonObject(with: parsed.settings) as! [String: Any]
precondition((settings["extension"] as! [String: Any])["zero"] as! Int == 0)
var wrong = try JSONSerialization.jsonObject(with: backup.encode()) as! [String: Any]
wrong["version"] = 2
do { _ = try FullBackup.decode(JSONSerialization.data(withJSONObject: wrong)); fatalError("Unknown version accepted") } catch {}
