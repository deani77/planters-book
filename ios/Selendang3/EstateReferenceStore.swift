import Foundation
import Combine

@MainActor
final class EstateReferenceStore: ObservableObject {
    static let shared = EstateReferenceStore()
    @Published private(set) var reference: EstateReference?
    @Published private(set) var storageError: String?
    private let url: URL

    init() {
        url = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("estate-reference-v1.json")
        do {
            if FileManager.default.fileExists(atPath: url.path) {
                reference = try EstateReference.parse(Data(contentsOf: url))
            }
        } catch {
            storageError = "Saved estate data could not be read. Import a valid file to recover; the original file is retained."
        }
    }

    func apply(_ data: Data) throws {
        let update = try EstateReference.parse(data)
        guard update.revision > (reference?.revision ?? 0) else {
            throw ProductionError.invalid("This revision is already saved or older.")
        }
        let directory = url.deletingLastPathComponent()
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        if let previous = try? Data(contentsOf: url) {
            try previous.write(to: directory.appendingPathComponent("estate-reference-previous.json"), options: .atomic)
        }
        try data.write(to: url, options: .atomic)
        reference = update
        storageError = nil
    }
}
