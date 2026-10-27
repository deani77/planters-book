import Foundation
import ZIPFoundation

func fixture(_ revision: Int = 1, estate: String = "fictional-estate") throws -> Data {
    let root: [String: Any] = ["schemaVersion": 1, "estateId": estate, "revision": revision,
        "reportingPeriod": "Fictional October 2026", "exportedAt": "2026-10-27T00:00:00Z",
        "sections": ["overview", "fields", "ages", "soil", "rainfall", "abw", "roads"].map { ["id": $0, "columns": ["Value"], "rows": [[0], [NSNull()]]] as [String: Any] },
        "manuringReport": ["reportDate": "2026-10-27", "appliedYtdMt": 7], "privateExtension": "retained"]
    return try JSONSerialization.data(withJSONObject: root, options: [.sortedKeys])
}
func unpack(_ data: Data) throws -> [String: Data] {
    let archive = try Archive(data: data, accessMode: .read)
    var files: [String: Data] = [:]
    for entry in archive { var bytes = Data(); _ = try archive.extract(entry) { bytes.append($0) }; files[entry.path] = bytes }
    return files
}
func pack(_ files: [String: Data]) throws -> Data {
    let archive = try Archive(data: Data(), accessMode: .create)
    for (path, bytes) in files {
        try archive.addEntry(with: path, type: path.hasSuffix("/") ? .directory : .file, uncompressedSize: Int64(bytes.count)) { position, size in
            let start = Int(position); return bytes.subdata(in: start..<min(start + size, bytes.count))
        }
    }
    return archive.data!
}
func mutated(_ data: Data, _ change: (inout [String: Any]) -> Void) throws -> Data {
    var files = try unpack(data)
    var manifest = try JSONSerialization.jsonObject(with: files["manifest.json"]!) as! [String: Any]
    change(&manifest)
    files["manifest.json"] = try JSONSerialization.data(withJSONObject: manifest)
    return try pack(files)
}
var rejections = 0
func rejects(_ data: Data, current: Data? = nil) {
    do { _ = try EstateZip.read(data, current: current); fatalError("Invalid ZIP accepted") } catch { rejections += 1 }
}
let current = try fixture()
let good = try EstateZip.write(current)
let roundTrip = try EstateZip.read(good, current: nil)
precondition(roundTrip.data == current)
precondition(roundTrip.reference.sections[0].rows[0][0]?.text == "0" && roundTrip.reference.sections[0].rows[1][0] == nil)
let legacy = try EstateZip.read(current, current: nil)
precondition(legacy.data == current)
let workbook = try EstateZip.read(EstateXlsx.template(), current: nil)
precondition(workbook.reference.sections.count == 7)
var incoming = try JSONSerialization.jsonObject(with: fixture(2)) as! [String: Any]
incoming.removeValue(forKey: "privateExtension")
incoming["manuringReport"] = NSNull()
var tables = incoming["sections"] as! [[String: Any]]
tables[0]["rows"] = [] as [Any]
incoming["sections"] = tables
let merge = try mutated(EstateZip.write(JSONSerialization.data(withJSONObject: incoming))) {
    $0["mode"] = "merge"; $0["baseRevision"] = 1; $0["sections"] = ["overview"]; $0["reports"] = ["manuringReport"]
}
let merged = try EstateZip.read(merge, current: current)
precondition(merged.reference.sections[0].rows.isEmpty && merged.reference.sections[1].rows.count == 2)
let mergedRoot = try JSONSerialization.jsonObject(with: merged.data) as! [String: Any]
precondition(mergedRoot["manuringReport"] is NSNull && mergedRoot["privateExtension"] as? String == "retained")
precondition(merged.expectedData == current)
rejects(good, current: current)
rejects(try EstateZip.write(fixture(2, estate: "other")), current: current)
rejects(merge, current: try fixture(3))
rejects(merge)
rejects(merge, current: try fixture(1, estate: "other"))
for (key, value) in [("version", 2 as Any), ("version", true), ("version", 1.5), ("sha256", "wrong"), ("estateId", "other"), ("revision", 99), ("mode", "execute"), ("payload", "../estate.json")] {
    rejects(try mutated(good) { $0[key] = value })
}
for path in ["../escape", "/absolute", "folder/", "nested.zip", "folder\\estate.json"] {
    var files = try unpack(good); files[path] = Data([1]); rejects(try pack(files))
}
// Set the first central-directory entry to a Unix symlink, then corrupt its CRC separately.
let signature: [UInt8] = [80, 75, 1, 2]
let raw = [UInt8](good)
let central = (0..<(raw.count - 46)).first { Array(raw[$0..<($0 + 4)]) == signature }!
var symlink = good
symlink[central + 5] = 3; symlink[central + 40] = 0xff; symlink[central + 41] = 0xa1
rejects(symlink)
var corrupt = good
corrupt[central + 16] ^= 1
rejects(corrupt)
rejects(Data(good.prefix(good.count / 2)))
rejects(Data(good.prefix(8)))
rejects(Data(repeating: 0, count: EstateZip.maxArchive + 1))
var bomb = try unpack(good)
bomb["estate.json"] = Data(repeating: 65, count: 2 * 1024 * 1024 + 1); rejects(try pack(bomb))
bomb["manifest.json"] = Data(repeating: 65, count: 16385); rejects(try pack(bomb))
rejects(try mutated(merge) { $0["sections"] = ["fields", "fields"] }, current: current)
rejects(try mutated(merge) { $0["sections"] = ["unknown"] }, current: current)
rejects(try mutated(merge) { $0["sections"] = []; $0["reports"] = [] }, current: current)
rejects(try mutated(merge) { $0["reports"] = ["cropBudget"] }, current: current)
rejects(try mutated(merge) { $0["baseRevision"] = 1.5 }, current: current)
rejects(try mutated(merge) { $0["baseRevision"] = true }, current: current)

// Exercise the actual disk store: preview/cancel, save, exact-base race, restore, backup failure.
try MainActor.assumeIsolated {
    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
    defer { try? FileManager.default.removeItem(at: directory) }
    let url = directory.appendingPathComponent("estate.json")
    let store = EstateReferenceStore(storageURL: url)
    try store.apply(EstateZip.read(good, current: nil))
    let preview = try EstateZip.read(merge, current: store.savedData)
    let afterPreview = try Data(contentsOf: url)
    precondition(afterPreview == current) // Preview/cancel performs no write.
    try store.apply(preview)
    precondition(store.reference?.revision == 2)
    let oldBackup = try store.previousImport()
    precondition(oldBackup.data == current)
    do { try store.apply(preview); fatalError("Stale preview saved") } catch {}
    precondition(store.savedData == merged.data)
    try store.apply(oldBackup)
    precondition(store.savedData == current)
    let reloaded = EstateReferenceStore(storageURL: url)
    precondition(reloaded.savedData == current)
    // A directory at the backup destination must abort before changing the current file.
    let backup = directory.appendingPathComponent("estate-reference-previous.json")
    try FileManager.default.removeItem(at: backup)
    try FileManager.default.createDirectory(at: backup, withIntermediateDirectories: true)
    do { try store.apply(EstateZip.read(merge, current: current)); fatalError("Backup failure ignored") } catch {}
    let afterFailure = try Data(contentsOf: url)
    precondition(afterFailure == current && store.savedData == current)
}
print("Estate ZIP round-trip, legacy formats, section/report merge, \(rejections) rejected imports, preview/cancel, backup, restore and failed-write preservation passed")
