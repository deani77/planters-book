import Foundation
import Combine

struct ProductionRemote: Codable {
    let record: ProductionDraft
    let version: Int
    let mutationId: String
}
struct ProductionEntry: Codable, Identifiable {
    var record: ProductionDraft
    var baseVersion: Int = 0
    var mutationId = UUID().uuidString
    var pending = true
    var conflict: ProductionRemote?
    var id: String { record.id }
}
private struct ProductionReplica: Codable {
    let version: Int
    let entries: [ProductionEntry]
}
@MainActor
final class ProductionStore: ObservableObject {
    static let shared = ProductionStore()
    @Published private(set) var entries: [ProductionEntry] = []
    @Published private(set) var storageError: String?
    var rows: [ProductionDraft] { entries.map(\.record) }
    private let url: URL

    init() {
        url = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("production-drafts-v1.json")
        do {
            if FileManager.default.fileExists(atPath: url.path) {
                let data = try Data(contentsOf: url)
                let root = try JSONSerialization.jsonObject(with: data) as? [String: Any]
                if root?["version"] as? Int == 1 {
                    let file = try JSONDecoder().decode(ProductionDraftFile.self, from: data)
                    try file.validate()
                    try persist(file.rows.map { ProductionEntry(record: $0) })
                } else {
                    let replica = try JSONDecoder().decode(ProductionReplica.self, from: data)
                    guard replica.version == 2, Set(replica.entries.map(\.id)).count == replica.entries.count else { throw ProductionError.invalid("Invalid stored production") }
                    try replica.entries.forEach { try $0.record.validate() }
                    entries = replica.entries
                }
            }
        } catch { storageError = "Saved production drafts could not be read. Editing is unavailable to protect your data." }
    }
    private func persist(_ updated: [ProductionEntry]) throws {
        guard storageError == nil else { throw ProductionError.invalid(storageError!) }
        let data = try JSONEncoder().encode(ProductionReplica(version: 2, entries: updated))
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        try data.write(to: url, options: .atomic)
        try FileManager.default.setAttributes([.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication], ofItemAtPath: url.path)
        entries = updated
    }
    func save(_ draft: ProductionDraft) throws {
        try draft.validate()
        let previous = entries.first { $0.id == draft.id }
        guard previous?.conflict == nil else { throw ProductionError.invalid("Resolve this record's conflict in Settings first.") }
        let updated = ProductionEntry(record: draft, baseVersion: previous?.baseVersion ?? 0)
        try persist((entries.filter { $0.id != draft.id } + [updated]).sorted { $0.id < $1.id })
    }
    func resolve(_ id: String, keepLocal: Bool) throws {
        try persist(entries.map { entry in
            guard entry.id == id, let remote = entry.conflict else { return entry }
            return keepLocal ? ProductionEntry(record: entry.record, baseVersion: remote.version) :
                ProductionEntry(record: remote.record, baseVersion: remote.version, mutationId: remote.mutationId, pending: false)
        })
    }
}
