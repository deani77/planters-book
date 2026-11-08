import Foundation
import CoreFoundation

/// A portable, side-effect-free envelope. Restore must validate and review before touching storage.
struct FullBackup {
    let estate: Data?
    let previousEstate: Data?
    let production: Data
    let settings: Data
    let mapPDF: Data?
    static let maximumBytes = 80 * 1024 * 1024
    static let maximumMapBytes = 50 * 1024 * 1024
    func encode() throws -> Data {
        let root: [String: Any] = ["format": "planters-book-full-backup", "version": 1,
            "estate": try estate.map { try JSONSerialization.jsonObject(with: $0) } ?? NSNull(),
            "previousEstate": try previousEstate.map { try JSONSerialization.jsonObject(with: $0) } ?? NSNull(),
            "production": try JSONSerialization.jsonObject(with: production),
            "settings": try JSONSerialization.jsonObject(with: settings),
            "mapPdfBase64": mapPDF?.base64EncodedString() ?? (NSNull() as Any)]
        let data = try JSONSerialization.data(withJSONObject: root, options: [.sortedKeys])
        _ = try FullBackup.decode(data)
        return data
    }
    static func decode(_ data: Data) throws -> FullBackup {
        guard data.count <= maximumBytes, let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              Set(root.keys) == Set(["format", "version", "estate", "previousEstate", "production", "settings", "mapPdfBase64"]),
              root["format"] as? String == "planters-book-full-backup", let version = root["version"] as? NSNumber,
              CFGetTypeID(version) != CFBooleanGetTypeID(), version.doubleValue == 1 else { throw ProductionError.invalid("Unsupported or oversized full backup.") }
        func estate(_ key: String) throws -> Data? {
            if root[key] is NSNull { return nil }
            guard let value = root[key] as? [String: Any] else { throw ProductionError.invalid("Invalid estate backup section.") }
            let bytes = try JSONSerialization.data(withJSONObject: value)
            _ = try EstateReference.parse(bytes)
            return bytes
        }
        let current = try estate("estate")
        let previous = try estate("previousEstate")
        if let current, let previous {
            guard try EstateReference.parse(current).estateId == EstateReference.parse(previous).estateId else { throw ProductionError.invalid("Backup estate identifiers differ.") }
        }
        guard let production = root["production"] as? [String: Any], let draftVersion = production["version"] as? NSNumber,
              CFGetTypeID(draftVersion) != CFBooleanGetTypeID(), draftVersion.doubleValue == 2,
              let rows = production["rows"] as? [[String: Any]], rows.count <= 10000,
              let settings = root["settings"] as? [String: Any] else { throw ProductionError.invalid("Invalid draft or settings backup.") }
        var ids = Set<String>()
        for row in rows {
            guard let record = row["record"] as? [String: Any], let version = row["baseVersion"] as? NSNumber,
                  CFGetTypeID(version) != CFBooleanGetTypeID(), version.doubleValue >= 0, version.doubleValue.rounded() == version.doubleValue,
                  let mutation = row["mutationId"] as? String, !mutation.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                  let pending = row["pending"] as? NSNumber, CFGetTypeID(pending) == CFBooleanGetTypeID() else { throw ProductionError.invalid("Invalid draft metadata.") }
            let draft = try JSONDecoder().decode(ProductionDraft.self, from: JSONSerialization.data(withJSONObject: record))
            try draft.validate()
            guard ids.insert(draft.id).inserted else { throw ProductionError.invalid("Duplicate production draft.") }
            if let conflict = row["conflict"], !(conflict is NSNull) {
                guard let remote = conflict as? [String: Any], let record = remote["record"], let version = remote["version"] as? NSNumber,
                      version.doubleValue > 0, version.doubleValue.rounded() == version.doubleValue,
                      let mutation = remote["mutationId"] as? String, !mutation.isEmpty else { throw ProductionError.invalid("Invalid draft conflict.") }
                let other = try JSONDecoder().decode(ProductionDraft.self, from: JSONSerialization.data(withJSONObject: record))
                try other.validate()
                guard other.id == draft.id else { throw ProductionError.invalid("Conflict belongs to another draft.") }
            }
        }
        let settingBytes = try JSONSerialization.data(withJSONObject: settings, options: [.sortedKeys])
        guard settingBytes.count <= 1024 * 1024 else { throw ProductionError.invalid("Settings exceed 1 MB.") }
        let map: Data?
        if root["mapPdfBase64"] is NSNull { map = nil }
        else {
            guard let encoded = root["mapPdfBase64"] as? String, encoded.count <= (maximumMapBytes + 2) / 3 * 4,
                  let bytes = Data(base64Encoded: encoded), (5...maximumMapBytes).contains(bytes.count),
                  bytes.starts(with: Data("%PDF-".utf8)) else { throw ProductionError.invalid("Backup map is not a PDF.") }
            map = bytes
        }
        return FullBackup(estate: current, previousEstate: previous,
            production: try JSONSerialization.data(withJSONObject: production, options: [.sortedKeys]), settings: settingBytes, mapPDF: map)
    }
}
