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
private struct ProductionResponse: Decodable {
    let schemaVersion: Int
    let estateId: String
    let revision: Int
    let records: [ProductionRemote]
}
private struct ProductionChange: Encodable {
    let record: ProductionDraft
    let baseVersion: Int
    let mutationId: String
}
private struct ProductionRequest: Encodable {
    let schemaVersion = 1
    let estateId = "selendang-3"
    let changes: [ProductionChange]
}
private final class ProductionNoRedirect: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }
}

@MainActor
final class ProductionStore: ObservableObject {
    static let shared = ProductionStore()
    @Published private(set) var entries: [ProductionEntry] = []
    @Published private(set) var storageError: String?
    @Published private(set) var syncing = false
    var rows: [ProductionDraft] { entries.map(\.record) }
    var status: String {
        "\(entries.filter { $0.pending && $0.conflict == nil }.count) pending upload · \(entries.filter { $0.conflict != nil }.count) conflicts · \(entries.filter { !$0.pending }.count) synced"
    }
    private let url: URL
    private static var syncActive = false

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
    private func merge(_ remote: [ProductionRemote], sent: [ProductionEntry]) throws {
        guard entries.filter({ !$0.pending }).allSatisfy({ entry in remote.contains { $0.record.id == entry.id } }) else {
            throw ProductionError.invalid("Server is missing previously synced records.")
        }
        var merged = Dictionary(uniqueKeysWithValues: entries.map { ($0.id, $0) })
        for server in remote {
            let id = server.record.id
            let current = merged[id]
            let uploaded = sent.first { $0.id == id }
            if current == nil || !current!.pending {
                guard current == nil || server.version >= current!.baseVersion else { throw ProductionError.invalid("Server version moved backwards.") }
                merged[id] = ProductionEntry(record: server.record, baseVersion: server.version, mutationId: server.mutationId, pending: false)
            } else if let uploaded = uploaded, server.mutationId == uploaded.mutationId {
                if current!.mutationId == uploaded.mutationId {
                    merged[id] = ProductionEntry(record: server.record, baseVersion: server.version, mutationId: server.mutationId, pending: false)
                } else { merged[id]!.baseVersion = server.version; merged[id]!.conflict = nil }
            } else if server.version != current!.baseVersion { merged[id]!.conflict = server }
        }
        try persist(merged.values.sorted { $0.id < $1.id })
    }
    func sync(address: String, token: String) async throws {
        guard !Self.syncActive else { throw ProductionError.invalid("Production sync is already running.") }
        guard storageError == nil else { throw ProductionError.invalid(storageError!) }
        guard var components = URLComponents(string: address.trimmingCharacters(in: .whitespacesAndNewlines)),
              components.scheme == "https", components.host != nil, components.user == nil, components.password == nil,
              components.query == nil, components.fragment == nil, components.path == "/v1/estates/selendang-3/snapshot",
              !token.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, !token.contains("\n"), !token.contains("\r") else {
            throw ProductionError.invalid("Enter the trusted HTTPS snapshot address and access token.")
        }
        components.path = "/v1/estates/selendang-3/production"
        guard let endpoint = components.url else { throw ProductionError.invalid("Invalid server address") }
        Self.syncActive = true; syncing = true
        defer { Self.syncActive = false; syncing = false }
        let session = URLSession(configuration: .ephemeral, delegate: ProductionNoRedirect(), delegateQueue: nil)
        defer { session.invalidateAndCancel() }
        let pending = entries.filter { $0.pending && $0.conflict == nil }
        let batches = pending.isEmpty ? [[]] : stride(from: 0, to: pending.count, by: 100).map { Array(pending[$0..<min($0+100,pending.count)]) }
        for sent in batches {
            var request = URLRequest(url: endpoint, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 30)
            request.setValue("Bearer " + token.trimmingCharacters(in: .whitespacesAndNewlines), forHTTPHeaderField: "Authorization")
            if !sent.isEmpty {
                request.httpMethod = "POST"
                request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                request.httpBody = try JSONEncoder().encode(ProductionRequest(changes: sent.map { ProductionChange(record: $0.record, baseVersion: $0.baseVersion, mutationId: $0.mutationId) }))
            }
            let (bytes, response) = try await session.bytes(for: request)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
                throw ProductionError.invalid("Production sync failed. Check connection and token write permission; local drafts are retained.")
            }
            var data = Data()
            for try await byte in bytes { guard data.count < 8*1024*1024 else { throw ProductionError.invalid("Production response too large") }; data.append(byte) }
            let result = try JSONDecoder().decode(ProductionResponse.self, from: data)
            guard result.schemaVersion == 1, result.estateId == "selendang-3", result.records.count <= 10000, result.revision >= (entries.map(\.baseVersion).max() ?? 0),
                  Set(result.records.map { $0.record.id }).count == result.records.count else { throw ProductionError.invalid("Invalid production response") }
            for remote in result.records {
                try remote.record.validate()
                guard remote.version > 0, remote.version <= result.revision else { throw ProductionError.invalid("Invalid server version") }
            }
            try merge(result.records, sent: sent)
        }
    }
}
