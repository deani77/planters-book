import Foundation
import CoreFoundation
import CryptoKit
import ZIPFoundation

struct EstateImport {
    let data: Data
    let reference: EstateReference
    let expectedData: Data?
    let summary: String
    var restoring = false
}

enum EstateZip {
    static let maxArchive = 8 * 1024 * 1024
    static let reports: Set<String> = ["yph", "cropProductionReport", "cropProductionReportHistory", "manuringReport", "cropBudget", "ffbRejectReport"]
    private static let sections: Set<String> = ["overview", "fields", "ages", "soil", "rainfall", "abw", "roads"]
    private static func hash(_ data: Data) -> String { SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined() }
    private static func object(_ data: Data) throws -> [String: Any] {
        guard String(data: data, encoding: .utf8) != nil else { throw ProductionError.invalid("ZIP manifest must be UTF-8.") }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { throw ProductionError.invalid("Expected an estate JSON object.") }
        return root
    }
    private static func check(_ condition: Bool, _ message: String) throws {
        guard condition else { throw ProductionError.invalid(message) }
    }
    // JSON booleans and floating-point values must not masquerade as integer versions/revisions.
    private static func integer(_ value: Any?) -> Int? {
        guard let number = value as? NSNumber, CFGetTypeID(number) != CFBooleanGetTypeID(),
              !["f", "d"].contains(String(cString: number.objCType)) else { return nil }
        return Int(exactly: number.int64Value)
    }
    static func read(_ data: Data, current: Data?) throws -> EstateImport {
        try check(data.count <= maxArchive, "Estate file exceeds 8 MB.")
        guard data.prefix(2) == Data([80, 75]) else { return try prepare(data, current: current) }
        let archive = try Archive(data: data, accessMode: .read)
        let entries = Array(archive)
        guard entries.contains(where: { $0.path == "manifest.json" }) else {
            try check(entries.contains(where: { $0.path == "xl/workbook.xml" }), "ZIP requires manifest.json and estate.json.")
            return try prepare(EstateXlsx.readEstate(data), current: current)
        }
        try check(entries.count == 2 && Set(entries.map(\.path)) == ["manifest.json", "estate.json"], "ZIP must contain only manifest.json and estate.json, without folders or duplicates.")
        func part(_ path: String, limit: Int) throws -> Data {
            guard let entry = archive[path], entry.type == .file, entry.uncompressedSize <= limit else { throw ProductionError.invalid("Invalid or oversized ZIP entry.") }
            var result = Data()
            let crc = try archive.extract(entry) { chunk in
                try check(result.count + chunk.count <= limit, "ZIP entry exceeds size limit.")
                result.append(chunk)
            }
            try check(result.count == entry.uncompressedSize && crc == entry.checksum, "ZIP entry is corrupt.")
            return result
        }
        let manifest = try object(part("manifest.json", limit: 16 * 1024))
        let payload = try part("estate.json", limit: 2 * 1024 * 1024)
        try check(manifest["format"] as? String == "planters-book-estate" && integer(manifest["version"]) == 1, "Unsupported estate ZIP version.")
        try check(manifest["payload"] as? String == "estate.json" && manifest["sha256"] as? String == hash(payload), "ZIP payload checksum does not match.")
        let incoming = try EstateReference.parse(payload)
        let root = try object(payload)
        try check(manifest["estateId"] as? String == incoming.estateId && integer(manifest["revision"]) == incoming.revision, "Manifest estate/revision does not match payload.")
        let common: Set<String> = ["format", "version", "payload", "sha256", "estateId", "revision", "mode"]
        switch manifest["mode"] as? String {
        case "replace":
            try check(Set(manifest.keys) == common, "Unexpected replacement manifest fields.")
            return try prepare(payload, current: current)
        case "merge":
            try check(Set(manifest.keys) == common.union(["baseRevision", "sections", "reports"]), "Invalid merge manifest fields.")
            guard let current else { throw ProductionError.invalid("Merge needs an existing estate snapshot.") }
            let saved = try EstateReference.parse(current)
            try check(incoming.estateId == saved.estateId, "This file belongs to another estate.")
            try check(integer(manifest["baseRevision"]) == saved.revision, "Merge base revision does not match saved data.")
            func selection(_ key: String, allowed: Set<String>) throws -> [String] {
                guard let list = manifest[key] as? [String], Set(list).count == list.count, Set(list).isSubset(of: allowed) else { throw ProductionError.invalid("Invalid merge selection.") }
                return list
            }
            let selectedSections = try selection("sections", allowed: sections)
            let selectedReports = try selection("reports", allowed: reports)
            try check(!selectedSections.isEmpty || !selectedReports.isEmpty, "Merge has no changes.")
            try check(Set(root.keys).isSubset(of: reports.union(["schemaVersion", "estateId", "revision", "reportingPeriod", "exportedAt", "sections"])), "Unknown payload fields require a full replacement.")
            try check(selectedReports.allSatisfy { root[$0] != nil }, "Selected reports must be provided; use null to clear.")
            var merged = try object(current)
            let byID = Dictionary(uniqueKeysWithValues: (root["sections"] as! [[String: Any]]).map { ($0["id"] as! String, $0) })
            merged["sections"] = (merged["sections"] as! [[String: Any]]).map { table in
                selectedSections.contains(table["id"] as! String) ? byID[table["id"] as! String]! : table
            }
            for key in selectedReports + ["revision", "reportingPeriod", "exportedAt"] { merged[key] = root[key] }
            let prepared = try prepare(JSONSerialization.data(withJSONObject: merged, options: [.sortedKeys]), current: current)
            return EstateImport(data: prepared.data, reference: prepared.reference, expectedData: current,
                summary: "Merge replaces sections: \(selectedSections.joined(separator: ", ")); reports: \(selectedReports.joined(separator: ", ")). Other data is preserved. Empty selected tables and null selected reports clear their values. Preview shows the complete result.")
        default: throw ProductionError.invalid("ZIP mode must be replace or merge.")
        }
    }
    static func prepare(_ data: Data, current: Data?) throws -> EstateImport {
        let incoming = try EstateReference.parse(data)
        if let current {
            let saved = try EstateReference.parse(current)
            try check(incoming.estateId == saved.estateId, "This file belongs to another estate.")
            try check(incoming.revision > saved.revision, "This revision is already saved or older.")
        }
        return EstateImport(data: data, reference: incoming, expectedData: current,
            summary: "Full replacement of all estate tables and optional reports. Empty tables and omitted reports clear previous values.")
    }
    static func write(_ data: Data) throws -> Data {
        let reference = try EstateReference.parse(data)
        let manifest: [String: Any] = ["format": "planters-book-estate", "version": 1, "payload": "estate.json", "sha256": hash(data), "mode": "replace", "estateId": reference.estateId, "revision": reference.revision]
        let archive = try Archive(data: Data(), accessMode: .create)
        for (path, bytes) in [("manifest.json", try JSONSerialization.data(withJSONObject: manifest, options: [.sortedKeys])), ("estate.json", data)] {
            try archive.addEntry(with: path, type: .file, uncompressedSize: Int64(bytes.count), provider: { position, size in
                let start = Int(position)
                return bytes.subdata(in: start..<min(start + size, bytes.count))
            })
        }
        guard let result = archive.data else { throw ProductionError.invalid("Could not create ZIP.") }
        return result
    }
}
