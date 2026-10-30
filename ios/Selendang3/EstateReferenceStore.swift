import Foundation
import Combine

@MainActor
final class EstateReferenceStore: ObservableObject {
    static let shared = EstateReferenceStore()
    @Published private(set) var reference: EstateReference?
    @Published private(set) var storageError: String?
    private(set) var savedData: Data?
    private let url: URL

    init(storageURL: URL? = nil) {
        url = storageURL ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("estate-reference-v1.json")
        do {
            if FileManager.default.fileExists(atPath: url.path) {
                let bytes = try Data(contentsOf: url)
                reference = try EstateReference.parse(bytes)
                savedData = bytes
            }
        } catch {
            storageError = "Saved estate data could not be read. Import a valid file to recover; the original file is retained."
        }
    }

    private var backupURL: URL { url.deletingLastPathComponent().appendingPathComponent("estate-reference-previous.json") }

    func previousImport() throws -> EstateImport {
        let data = try Data(contentsOf: backupURL)
        return EstateImport(data: data, reference: try EstateReference.parse(data), expectedData: savedData,
            summary: "Restore the previous estate snapshot. This can lower the revision. The current snapshot becomes the backup. Production drafts and profile settings are unchanged.", restoring: true)
    }

    func apply(_ data: Data) throws {
        try apply(EstateZip.prepare(data, current: savedData))
    }

    func apply(_ prepared: EstateImport) throws {
        guard savedData == prepared.expectedData else { throw ProductionError.invalid("Saved estate changed after preview. Review again.") }
        let update = try EstateReference.parse(prepared.data)
        if prepared.restoring {
            guard try Data(contentsOf: backupURL) == prepared.data else { throw ProductionError.invalid("Backup changed. Review again.") }
        } else { _ = try EstateZip.prepare(prepared.data, current: savedData) }
        let directory = url.deletingLastPathComponent()
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        // Never swallow a failed backup read/write. Retain even an unreadable original for recovery.
        if FileManager.default.fileExists(atPath: url.path) {
            let previous = try Data(contentsOf: url)
            try previous.write(to: backupURL, options: .atomic)
        }
        try prepared.data.write(to: url, options: .atomic)
        reference = update
        savedData = prepared.data
        storageError = nil
    }
}
